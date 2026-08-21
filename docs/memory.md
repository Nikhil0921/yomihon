# Yomihon — Project Memory (AI State File)

> LIVING DOCUMENT. Every coding agent MUST read this file before substantial work
> and MUST update it after meaningful implementation work (protocol at bottom).
> Companion documents: `prd.md` (WHAT) · `architecture.md` (HOW) · `rules.md`
> (MUST/MUST NOT) · `phase.md` (WHEN) · `design.md` (LOOK/FEEL).

---

## Current project state

```text
Project:        Yomihon (v0.4.0, vc25) — Android manga reader + OCR/language tooling
Repo state:     branch main @ b915c9780 ("Release v0.4.0"), up to date with origin/main
Untracked:      AGENTS.md, architect.md, architect-2.md  (user planning files — DO NOT touch)
Primary goal:   Production-quality OCR-integrated Read-Aloud TTS
Current phase:  Phase 1 (TTS foundation) — NOT_STARTED
Current status: Phase 0 (repository analysis + documentation system) COMPLETED
TTS code:       NONE exists yet (verified repo-wide; only DictionaryAudioPlayerImpl MediaPlayer)
```

## Current objective

Implement the approved Read-Aloud TTS feature per root `architect.md` /
`architect-2.md` scope decisions, starting with **Phase 1**: `TtsEngine`
interface, `TtsAdvancePolicy`, `TtsPreferences` in `:domain` + Injekt bindings.
Follow `docs/phase.md` order exactly.

## Completed work

```text
[COMPLETED 2026-08-21]
- Full repository audit (modules, Gradle, packages, reader, OCR, data/DI,
  network, sources/extensions, settings, testing, i18n, theme/design system)
- Verified ZERO TTS/audio-focus/MediaSession code exists (only
  DictionaryAudioPlayerImpl fire-and-forget MediaPlayer)
- Confirmed TTS scope decisions from architect.md/architect-2.md are consistent
  with the audited architecture
- Created docs system: prd.md, architecture.md, rules.md, phase.md, design.md,
  memory.md (this file). No source files modified.
```

## In progress

```text
[IN_PROGRESS]
Feature: none (between phases)

Next up (Phase 1):
- domain/src/main/java/mihon/domain/tts/engine/TtsEngine.kt      (new)
- domain/src/main/java/mihon/domain/tts/TtsAdvancePolicy.kt      (new)
- domain/src/main/java/mihon/domain/tts/service/TtsPreferences.kt(new)
- app/src/main/java/eu/kanade/domain/DomainModule.kt             (engine binding ~l.282 area)
- app/src/main/java/eu/kanade/tachiyomi/di/PreferenceModule.kt   (prefs binding ~l.59 area)
```

## Blocked

```text
None. (Earlier blocker "2 product-fork decisions pending user input" recorded in
.remember/now.md was RESOLVED — decisions are baked into architect.md/architect-2.md:
reader-bound playback, mini-bar visualization.)
```

## Current files being modified

```text
Current working files:
- docs/memory.md (this file — always current)
(none in source; Phase 1 list above becomes the working set when started.
Remove entries here when work finishes to avoid overlapping agents.)
```

## Recently changed files

```text
2026-08-21  docs/prd.md            created — product requirements incl. verified feature inventory + TTS reqs
2026-08-21  docs/architecture.md   created — actual architecture + planned TTS pipeline/file map
2026-08-21  docs/rules.md          created — engineering rulebook for AI agents
2026-08-21  docs/phase.md          created — Phase 0..10 roadmap with statuses
2026-08-21  docs/design.md         created — design language audit + TTS UI spec
2026-08-21  docs/memory.md         created — this file
No existing repository files were modified.
```

## Architecture decisions

```text
Decision:
TTS = Android system TextToSpeech behind framework-free TtsEngine interface
(:domain); AndroidTtsEngine impl isolated in :app data/tts.

Reason:
Offline-capable baseline, zero new dependencies, future engines (cloud/neural)
become drop-in Injekt binding swaps; mirrors DictionaryAudioPlayer
interface-in-domain precedent.

Decision:
Playback is reader-bound (pause on Activity.onStop; no FGS/MediaSession/
permissions); current sentence visualized in mini-bar only (no on-image bbox
highlight in v1).

Reason:
User-approved product fork decisions (architect.md Context); targetSdk 36 makes
background audio require FGS+MediaSession surface that v1 deliberately avoids.

Decision:
Advance/navigation logic as pure functions in :domain (SentenceSegmenter,
TtsAdvancePolicy) unit-tested with JUnit5+Kotest; controller orchestration stays
thin and untested-by-design (no Robolectric in repo).

Reason:
Matches house testing stack and existing pure-domain precedents
(ChapterRecognition, SentenceParser, PrioritizedTaskQueueTest).

Decision:
Controller emits Events handled by ReaderActivity's existing eventFlow collector
(moveToPageIndex/loadNextChapter); it never touches Viewer directly.

Reason:
Reuses established VM→Activity pattern; user swipes mid-playback win via
onPageSelected arbitration without fighting the viewer.
```

## Rejected approaches

```text
Rejected:
Direct TextToSpeech calls from ReaderScreen/composables.
Reason: lifecycle coupling, untestable, blocks future engines.

Rejected:
Foreground service / MediaSession background playback in v1.
Reason: scope decision; adds permissions/notification surface; revisit in Phase 10.

Rejected:
On-image bbox highlighting of spoken region in v1.
Reason: scope decision; mini-bar text feedback suffices; reuse
ReaderOcrOverlayRenderer approach if ever added.

Rejected:
New audio caching layer for TTS output.
Reason: system-TTS latency is tens of ms; OCR cache already covers text;
revisit only with high-latency cloud engines.

Rejected:
Re-sorting OcrRegions inside the segmenter to "fix" local-engine ordering.
Reason: would diverge from tap-highlight behavior; ordering gap must stay visible
(see Known issues).
```

## Known issues

```text
1. MEDIUM | OCR ordering | Local Legacy/Fast full-page scan path uses raw
   detection index as region order (hardcoded Horizontal orientation) — vertical
   manga would read out of order. Mitigation today: detection engine stub always
   throws (UnavailableDetOcrEngine TODO in OcrRepositoryImpl.detectionEngine())
   so scans redirect to Glens when fallbacks enabled. Action: document
   (done in architecture.md §4); optional follow-up = port Glens ordering into
   scanLocally. Do NOT silently reorder in segmenter.

2. LOW | ReaderActivity | ContentOverlay internally re-calls
   binding.composeOverlay.setComposeContent creating two parallel Compose
   rendering blocks (outer setComposeOverlay ~l.371 vs inner ~l.613).
   Impact: TTS bar/dialog additions must go to the correct block (inner
   ContentOverlay, beside OcrLoadingIndicator ~l.882). Do not refactor casually.

3. INFO | Build env | ML model assets are gitignored & absent from fresh clones
   (app assets ocr/, ocr_fast/, data panel_detector/model.tflite). Local OCR +
   panel features silently degrade; CI downloads pinned artifacts. TTS cached-
   path testing needs a GLENS-scanned chapter or downloaded models first.

4. INFO | Docs env | AGENTS.md notes .devcontainer "Java 17" note stale — CI/toolchain
   effectively JDK 21 (Gradle java property 17). Use CI commands from rules.md §11.
```

## Technical debt

```text
- UnavailableDetOcrEngine stub (TODO upstream) — see Known issues #1.
- Dual Compose composition blocks in ReaderActivity — see #2.
- No unit tests for repositories/download/network/UI layers (house-wide, pre-existing).
- androidTest OcrRepositoryImplTest is @Ignore'd (needs device+models).
(Deliberately NOT adding new debt for TTS v1: policy logic must be tested.)
```

## Dependencies

```text
No dependency changes made. Standing decision: TTS v1 adds ZERO dependencies
(framework android.speech.tts + android.media.AudioManager; minSdk 26 covers
AudioFocusRequest). Key existing versions recorded in architecture.md §1/§14
(Kotlin 2.4.0, AGP 9.2.1, Compose BOM 2026.06.01, SQLDelight 2.3.2, OkHttp 5.4.0,
Injekt 91edab2317, JUnit5 6.1.1/Kotest 6.2.2/MockK 1.14.11).
```

## Testing status

```text
Unit tests:        NOT RUN this session (docs-only change; nothing to test yet)
Integration tests: none run (existing androidTest suites are device-gated/@Ignore)
UI tests:          none exist in repo
Device tests:      not applicable yet
Lint:              spotlessCheck NOT RUN this session (markdown-only change)
Build:             NOT RUN this session (markdown-only change)
Baseline (pre-TTS expectations): CI order = spotlessCheck → testDebugUnitTest →
verifySqlDelightMigration → assembleRelease (see rules.md §11)
```

## Last verified build

```text
Date:     2026-08-21 (this session)
Command:  none executed — documentation-only change set
Result:   n/a (do not claim success without running; first code change MUST run
          ./gradlew :app:assembleDebug and record it here)
Environment: linux, branch main @ b915c9780, JDK per repo config (CI: 21)
```

## Last verified test

```text
Date:     2026-08-21 (this session)
Command:  none executed — documentation-only change set
Result:   n/a (first code change MUST run ./gradlew testDebugUnitTest and record)
Environment: same as above
```

---

## Agent handoff

```text
Last agent:                 ox-alpha (documentation-system session)
Date:                       2026-08-21
Task completed:             Phase 0 — full repo audit + docs/{prd,architecture,rules,
                            phase,design,memory}.md created; zero source changes
Current task:               none (handoff point)
Next recommended task:      Phase 1 (docs/phase.md): create TtsEngine interface,
                            TtsAdvancePolicy, TtsPreferences in :domain; wire
                            DomainModule/PreferenceModule bindings; then
                            ./gradlew :app:assembleDebug + spotlessCheck and
                            update THIS file (statuses, working files, build/test)
Files safe to modify:       docs/* ; new files under domain/src/{main,test}/java/mihon/domain/tts/;
                            app DI modules (DomainModule.kt, PreferenceModule.kt) for bindings
Files currently worked on:  none
Known risks:                ML models absent locally (device verification needs GLENS
                            scan or model download); ReaderActivity dual-composition
                            quirk (#2) when adding overlays; do not touch untracked
                            AGENTS.md/architect*.md
```

---

## MEMORY UPDATE PROTOCOL (mandatory)

**Before modifying code:** read this file → check Current objective / phase /
working files / known issues / decisions / rejected approaches.

**After modifying code:** update Completed work · In progress · Changed files ·
Decisions/rejections · Issues/blockers · Testing status · Last verified
build/test · Agent handoff. Skip updates for trivial formatting-only changes.

**Anti-spaghetti checklist before any new class/utility/dependency/refactor:**
existing class? existing utility? SDK/platform solution? why is current
architecture insufficient? is each module touch required? does it reduce or move
complexity? where does it belong per architecture.md? If unclear — inspect the
repo before writing code.

**Session startup sequence:** memory.md → rules.md → relevant architecture.md
section → current phase.md status → git status → task-relevant files → confirm
architecture → smallest appropriate change → test → update memory.md → report
exactly what changed. Do not redo full analysis unless architecture changed,
docs are stale, or reality contradicts them.
