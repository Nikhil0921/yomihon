#!/usr/bin/env bash
# Capture device screenshot for UI visual QA (before/after redesign compares).
# Usage: ./scripts/screenshot.sh [name]   -> ui-captures/<timestamp>_<name>.png
# Requires: wireless adb connected (./scripts/adb-wireless connect)

set -euo pipefail

NAME="${1:-capture}"
# sanitize name to filename-safe chars
NAME="$(printf '%s' "$NAME" | tr -c '[:alnum:]-_' '_')"
DIR="$(git rev-parse --show-toplevel 2>/dev/null || pwd)/ui-captures"
mkdir -p "$DIR"
FILE="$DIR/$(date +%Y%m%d_%H%M%S)_${NAME}.png"

adb exec-out screencap -p > "$FILE"
echo "saved $FILE"
