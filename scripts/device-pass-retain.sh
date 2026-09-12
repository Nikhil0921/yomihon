#!/usr/bin/env bash
# .device-pass retention manager.
# Default: dry-run. Destructive requires --apply --confirm.
# Classification priority (per docs/memory.md protocol):
#   1. Explicit gate/result metadata (.device-pass/GATE)
#   2. Current verification state (open file descriptors / recent writes)
#   3. Explicit pass/fail result (gate status)
#   4. Protected/pinned artifacts (PINNED file, docs/memory.md citations)
#   5. Failure markers as secondary signal only
#   6. Age/retention policy
# Ambiguous classification => KEEP. Never auto-delete ambiguous artifacts.

set -euo pipefail

ROOT="${HOME}/Project/yomihon/.device-pass"
DRY_RUN=1
APPLY=0
CONFIRM=0
VERBOSE=0
RET_FAIL_DAYS=90
RET_PASS_DAYS=30
RET_LOGCAT_DAYS=7
RET_SHOT_DAYS=14
MAX_BYTES=""
NOW_EPOCH="$(date +%s)"
LOG_FILE=""
declare -a REMOVED_FILES=()
declare -i REMOVED_BYTES=0

usage() {
    cat <<'EOF'
Usage: scripts/device-pass-retain.sh [--dry-run] [--apply --confirm] [--verbose]
                                     [--root DIR] [--retention-pass-days N]
                                     [--retention-fail-days N]
                                     [--retention-logcat-days N]
                                     [--retention-shot-days N]
                                     [--max-bytes N] [--log FILE]

Modes:
  --dry-run (default)  report what WOULD be removed; deletes nothing
  --apply              destructive mode; requires --confirm
  --confirm            explicit confirmation token for --apply

Retention defaults:
  current verification      never deleted
  failed verification       90 days
  successful gate logs      30 days
  raw logcat/debug logs     7 days (after superseding pass)
  screenshots              14 days
  ambiguous artifacts      never auto-deleted (kept)
EOF
}

die() { echo "ERROR: $*" >&2; exit 1; }
log() { [[ "$VERBOSE" -eq 1 ]] && echo "[retain] $*" || true; }

while [[ $# -gt 0 ]]; do
    case "$1" in
        --dry-run) DRY_RUN=1 ;;
        --apply) APPLY=1; DRY_RUN=0 ;;
        --confirm) CONFIRM=1 ;;
        --verbose) VERBOSE=1 ;;
        --root) ROOT="$2"; shift ;;
        --retention-pass-days) RET_PASS_DAYS="$2"; shift ;;
        --retention-fail-days) RET_FAIL_DAYS="$2"; shift ;;
        --retention-logcat-days) RET_LOGCAT_DAYS="$2"; shift ;;
        --retention-shot-days) RET_SHOT_DAYS="$2"; shift ;;
        --max-bytes) MAX_BYTES="$2"; shift ;;
        --log) LOG_FILE="$2"; shift ;;
        -h|--help) usage; exit 0 ;;
        *) die "unknown argument: $1 (see --help)" ;;
    esac
    shift
done

[[ "$APPLY" -eq 1 && "$CONFIRM" -ne 1 ]] && die "--apply requires --confirm"
[[ -d "$ROOT" ]] || die "root not a directory: $ROOT"

# Realpath guard: never operate outside .device-pass
REAL_ROOT="$(cd "$ROOT" && pwd -P)"
[[ "$(basename "$REAL_ROOT")" == ".device-pass" ]] || die "root must be a .device-pass directory: $REAL_ROOT"

echo "Mode: $([[ $APPLY -eq 1 ]] && echo APPLY || echo DRY-RUN)"
echo "Root: $REAL_ROOT"

# --- classification inputs -------------------------------------------------
GATE_FILE="$REAL_ROOT/GATE"
PINNED_FILE="$REAL_ROOT/PINNED"
MEMORY="${HOME}/Project/yomihon/docs/memory.md"
GATE_NAME=""; GATE_RESULT=""; GATE_DATE=""
if [[ -f "$GATE_FILE" ]]; then
    GATE_NAME="$(sed -n 's/^name=//p' "$GATE_FILE" | head -n 1)"
    GATE_RESULT="$(sed -n 's/^result=//p' "$GATE_FILE" | head -n 1)"
    GATE_DATE="$(sed -n 's/^date=//p' "$GATE_FILE" | head -n 1)"
    echo "Gate metadata: name=${GATE_NAME:-?} result=${GATE_RESULT:-?} date=${GATE_DATE:-?}"
else
    echo "Gate metadata: none (GATE file absent; ambiguity => keep)"
fi

# Active-verification check: any process holding a file under root open (write)
# or any file modified in the last ACTIVE_WINDOW minutes.
ACTIVE_WINDOW_MIN=30
ACTIVE_CUTOFF_EPOCH=$(( NOW_EPOCH - ACTIVE_WINDOW_MIN * 60 ))
ACTIVE=0
while IFS= read -r -d '' f; do
    if lsof -t -- "$f" >/dev/null 2>&1; then
        echo "ACTIVE: file held open by process: $f"
        ACTIVE=1
    fi
done < <(find -P "$REAL_ROOT" -type f -print0 2>/dev/null)

NEWEST_MTIME=$(find -P "$REAL_ROOT" -type f -printf '%T@\n' 2>/dev/null | sort -nr | head -n 1)
if [[ -n "$NEWEST_MTIME" ]] && awk -v n="$NEWEST_MTIME" -v c="$ACTIVE_CUTOFF_EPOCH" 'BEGIN{exit !(n>=c)}'; then
    echo "ACTIVE: recent write within ${ACTIVE_WINDOW_MIN}min (newest mtime epoch $NEWEST_MTIME)"
    ACTIVE=1
fi

if [[ "$ACTIVE" -eq 1 ]]; then
    echo "RESULT: verification appears ACTIVE — nothing deleted."
    exit 0
fi

# --- docs citation scan (memory.md, phase.md, roadmap) --------------------
cited_in_docs() {
    local f="$1" base
    base="$(basename "$f")"
    grep -qF "$base" "$MEMORY" 2>/dev/null && return 0
    grep -qF "$base" "${HOME}/Project/yomihon/docs/phase.md" 2>/dev/null && return 0
    grep -qF "$base" "${HOME}/Project/yomihon/docs/implementation-roadmap.md" 2>/dev/null && return 0
    return 1
}

is_pinned() {
    local f="$1" base
    base="$(basename "$f")"
    [[ -f "$PINNED_FILE" ]] && grep -qxF "$base" "$PINNED_FILE" && return 0
    return 1
}

# Primary failure classifier: gate metadata. Secondary: markers.
classify() {
    local f="$1" base
    base="$(basename "$f")"
    if [[ -n "$GATE_NAME" && "$base" == "$GATE_NAME"* ]]; then
        if [[ "$GATE_RESULT" == "fail" ]]; then echo "current-failed"
        else echo "current"; fi
        return
    fi
    # current-series default: logs newer than gate date and matching gate stem
    if [[ -n "$GATE_NAME" ]] && [[ "$base" == "${GATE_NAME%%-*}"* ]]; then
        [[ "$GATE_RESULT" == "fail" ]] && { echo "current-failed"; return; }
    fi
    # explicit pass/fail marker files written by verification commands (future)
    if [[ -f "${f%.log}.result" ]]; then
        grep -qi "fail" "${f%.log}.result" 2>/dev/null && { echo "failed"; return; }
        grep -qi "pass" "${f%.log}.result" 2>/dev/null && { echo "pass"; return; }
    fi
    # protected
    is_pinned "$f" && { echo "protected"; return; }
    cited_in_docs "$f" && { echo "evidence"; return; }
    # secondary signal: failure markers in filename/content
    case "$base" in
        *fail*|*crash*|*leak*) echo "failed-secondary"; return ;;
    esac
    # type-based classes
    case "$base" in
        logcat-*|*logcat*) echo "raw-logcat" ;;
        *.png|*.jpg) echo "screenshot" ;;
        *verify*|*smoke*|*stabilize*|*batch*) echo "pass-historical" ;;
        *) echo "ambiguous" ;;
    esac
}

retention_days_for() {
    case "$1" in
        current|current-failed|protected|evidence|ambiguous|failed-secondary) echo -1 ;;
        failed) echo "$RET_FAIL_DAYS" ;;
        pass|pass-historical) echo "$RET_PASS_DAYS" ;;
        raw-logcat) echo "$RET_LOGCAT_DAYS" ;;
        screenshot) echo "$RET_SHOT_DAYS" ;;
        *) echo -1 ;;
    esac
}

header() { printf '%-58s %10s %6s %-17s %-42s %s\n' FILE SIZE_MB AGE_D CLASS REASON WOULD_DELETE; }

header() { printf '%-58s %10s %6s %-17s %-42s %s\n' FILE SIZE_MB AGE_D CLASS REASON WOULD_DELETE; }

printf '%-58s %10s %6s %-17s %-42s %s\n' FILE SIZE_MB AGE_D CLASS REASON WOULD_DELETE | tr -s ' '
echo "------------------------------------------------------------------------------------------------------------------------------"

declare -i TOTAL_CANDIDATE_BYTES=0
declare -i FILE_COUNT=0
declare -i KEEP_COUNT=0

while IFS= read -r -d '' f; do
    # symlink guard
    [[ -L "$f" ]] && { log "skip symlink: $f"; continue; }
    [[ ! -f "$f" ]] && continue
    # never leave root
    REAL_F="$(cd "$(dirname "$f")" && pwd -P)/$(basename "$f")"
    [[ "$REAL_F" == "$REAL_ROOT"/* ]] || { log "skip outside-root: $f"; continue; }
    # skip control/marker files
    case "$(basename "$f")" in
        GATE|PINNED|*.result) continue ;;
    esac

    CLASS="$(classify "$f")"
    RET="$(retention_days_for "$CLASS")"
    SIZE="$(stat -c%s "$f")"
    MTIME="$(stat -c%Y "$f")"
    AGE_DAYS=$(( (NOW_EPOCH - MTIME) / 86400 ))
    REASON=""
    WOULD="KEEP"

    if [[ "$RET" -eq -1 ]]; then
        REASON="no-retention-class"
    elif [[ "$AGE_DAYS" -lt "$RET" ]]; then
        REASON="age ${AGE_DAYS}d < retention ${RET}d"
    else
        REASON="age ${AGE_DAYS}d >= retention ${RET}d"
        WOULD="DELETE"
        TOTAL_CANDIDATE_BYTES+=SIZE
    fi

    printf '%-58s %10d %6d %-17s %-42s %s\n' \
        "${f#"$REAL_ROOT"/}" $(( SIZE / 1024 / 1024 )) "$AGE_DAYS" "$CLASS" "$REASON" "$WOULD"
    FILE_COUNT+=1
    [[ "$WOULD" == "KEEP" ]] && KEEP_COUNT+=1
done < <(find -P "$REAL_ROOT" -type f -print0 2>/dev/null)

echo "------------------------------------------------------------------------------------------------------------------------------"
echo "Files: $FILE_COUNT  keep: $KEEP_COUNT  delete-candidates: $(( FILE_COUNT - KEEP_COUNT ))"
echo "Candidate bytes: $TOTAL_CANDIDATE_BYTES ($(( TOTAL_CANDIDATE_BYTES / 1024 / 1024 )) MB)"

if [[ "$APPLY" -ne 1 ]]; then
    echo "RESULT: dry-run complete. Nothing deleted."
    exit 0
fi

# --- apply ------------------------------------------------------------------
if [[ "$TOTAL_CANDIDATE_BYTES" -eq 0 ]]; then
    echo "RESULT: nothing to delete."
    exit 0
fi

echo "WARNING: --apply will DELETE the DELETE candidates listed above."
read -r -p "Type DELETE to proceed: " ANSWER
[[ "$ANSWER" == "DELETE" ]] || die "aborted by user"

if [[ -n "$MAX_BYTES" ]]; then
    [[ "$TOTAL_CANDIDATE_BYTES" -le "$MAX_BYTES" ]] || die "candidate bytes exceed --max-bytes $MAX_BYTES; abort"
fi

for f in "${REMOVED_FILES[@]:-}"; do :; done  # no-op placeholder for array guard

while IFS= read -r -d '' f; do
    [[ -L "$f" || ! -f "$f" ]] && continue
    REAL_F="$(cd "$(dirname "$f")" && pwd -P)/$(basename "$f")"
    [[ "$REAL_F" == "$REAL_ROOT"/* ]] || continue
    case "$(basename "$f")" in GATE|PINNED|*.result) continue ;; esac
    CLASS="$(classify "$f")"
    RET="$(retention_days_for "$CLASS")"
    [[ "$RET" -eq -1 ]] && continue
    MTIME="$(stat -c%Y "$f")"
    AGE_DAYS=$(( (NOW_EPOCH - MTIME) / 86400 ))
    [[ "$AGE_DAYS" -lt "$RET" ]] && continue
    SIZE="$(stat -c%s "$f")"
    if rm -- "$f"; then
        REMOVED_FILES+=("$f")
        REMOVED_BYTES+=SIZE
        echo "REMOVED: $f ($SIZE bytes)"
        [[ -n "$LOG_FILE" ]] && printf '%s\t%s\t%s\n' "$(date -Is)" "$f" "$SIZE" >> "$LOG_FILE"
    else
        echo "FAILED to remove: $f" >&2
    fi
done < <(find -P "$REAL_ROOT" -type f -print0 2>/dev/null)

echo "RESULT: removed ${#REMOVED_FILES[@]} files, recovered $REMOVED_BYTES bytes ($(( REMOVED_BYTES / 1024 / 1024 )) MB)"
[[ -n "$LOG_FILE" ]] && echo "Removal log: $LOG_FILE"
