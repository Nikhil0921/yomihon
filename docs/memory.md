# Yomihon — Project Memory (AI State File)

> LIVING DOCUMENT. Every coding agent MUST read this file before substantial work
> and MUST update it after meaningful implementation work (protocol at bottom).
> Companion documents: `prd.md` (WHAT) · `architecture.md` (HOW) · `rules.md`
> (MUST/MUST NOT) · `phase.md` (WHEN) · `design.md` (LOOK/FEEL).

---

## Current project state

```text
Project:        Yomihon (v0.4.0, vc25) — Android manga reader + OCR/language tooling
Repo state:     branch main @ a36c3cc83 (Phases 1–6 committed across 07c64985f,
                ac1614a5e, a36c3cc83 "checkpoint before WSL recovery"; checkpoint
                commit contained compile breaks — fixed this session, see below)
Untracked:      .opencode/ (tool config); devcontainer files deleted in worktree
                (user env recovery — DO NOT touch/restore)
Primary goal:   Production-quality OCR-integrated Read-Aloud TTS
Current phase:  Phase 7 (settings) COMPLETED (code + CI gates green)
Current status: Phases 0–7 COMPLETED. Next: Phase 8 (testing/device pass).
TTS code:       TtsEngine, TtsAdvancePolicy (+tests), TtsPreferences, SentenceSegmenter
                (+12 tests) in :domain; AndroidTtsEngine + TtsPlaybackController in :app;
                ReadAloud settings tab; DI bindings done.
                spotlessCheck + testDebugUnitTest green (this session, JDK17 container).
```

## Current objective

Implement the Read-Aloud TTS feature per root `architect.md` /
`architect-2.md` scope decisions. Phases 1–7 landed. Next: **Phase 8** —
testing: full `testDebugUnitTest` (done continuously), device pass of the
prd.md §3.4(3) script, record results. Follow `docs/phase.md` order exactly.

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

[COMPLETED 2026-08-22 — commit 07c64985f]
- Phase 1: TtsEngine interface, TtsAdvancePolicy (+ TtsAdvancePolicyTest),
  TtsPreferences in :domain; DomainModule + PreferenceModule bindings
- Phase 2: AndroidTtsEngine (:app data/tts) — TextToSpeech + AudioFocusRequest,
  CompletableDeferred bridging
- Verified: spotlessCheck + :domain:testDebugUnitTest green (JDK17 container;
  CI toolchain is JDK 21 per .github/.java-version)

[COMPLETED 2026-08-22 — this session, UNCOMMITTED]
- Phase 4: SentenceSegmenter.toTtsSentences() + TtsSentence model (:domain);
  SentenceSegmenterTest (12 cases: multi-sentence, remainder, `.`/`...`
  non-terminal, blank skip, ‼⁇⁉⁈ glyphs, half-width !?, order/bbox
  preservation, no cross-region merge, consecutive terminals) — TDD RED→GREEN
- Phase 3+5: TtsPlaybackController (:app ui/reader/tts/) — cached-first
  acquisition (GetCachedPageOcr → miss via OcrPageSourceResolver +
  WithOcrScanSession + ScanPageOcr, bitmap recycle in finally), N+1 prefetch
  with cancellation on page change, TEXT→SEGMENT→SPEAK→ADVANCE loop,
  TtsPhase state machine exposed as StateFlow<TtsPlaybackState>, advance
  confirm via CompletableDeferred + 10 s timeout → Paused, onPageSelected
  arbitration (user navigation wins), next/prev sentence within page,
  audio-focus loss pauses, live rate/pitch pref collection, events channel
  (AdvancePage/AdvanceChapter/Failed) for host wiring
- Verified: spotlessCheck + testDebugUnitTest + :app:assembleDebug green

[COMPLETED 2026-08-23 — commit ac1614a5e + a36c3cc83]
- Phase 6 committed (ReaderViewModel/ReaderActivity wiring, TtsPlaybackBar,
  ReaderBottomBar icon, onStop pause, keep-screen-on) via "checkpoint before
  WSL recovery" — but that checkpoint contained COMPILE BREAKS (see Fixed)

[COMPLETED 2026-08-23 — this session, UNCOMMITTED]
- Phase 7: ReadAloud settings tab
  - ReaderSettingsScreenModel: `ttsPreferences` (Injekt default)
  - ReadAloudPage.kt: rate/pitch sliders (50–200%, pref ×100 mapping via
    roundToInt), auto-page-turn / auto-next-chapter / keep-screen-on checkboxes
    (CheckboxItem(pref) overloads)
  - ReaderSettingsDialog: 4th tab "Read aloud" appended as page 3; ColorFilter
    dim-hack index (`== 2`) intentionally untouched
  - i18n base strings.xml: 5 snake_case keys (pref_tts_speech_rate, pitch,
    auto_page_turn, auto_next_chapter, keep_screen_on); reuses action_read_aloud
  - ReaderActivity: added ttsKeepScreenOn().changes() collector → live
    updateKeepScreenOn() (was only reader-pref + phase-change driven)
- FIXED pre-existing Phase-6 compile breaks from WSL-recovery checkpoint:
  - ReaderViewModel Event.TtsError param self-shadowed sibling nested class
    (Kotlin scoping: nested classifiers shadow imports) → FQ type in data class
  - ReaderViewModel l.371 passed db Chapter where TtsChapterContext expects
    domain Chapter → toDomainChapter()!! (house precedent l.640/l.1168)
- Verified: spotlessCheck + :app:compileDebugKotlin + testDebugUnitTest green
```

## In progress

```text
[COMPLETED]
Feature: Phase 7 — ReadAloud settings tab

Completed:
- Read aloud tab in ReaderSettingsDialog (page 3, appended after ColorFilter)
- Rate/pitch sliders write pref ×100; controller already collects rate/pitch
  live, auto* prefs read at decision time → live by construction
- keep-screen-on toggle now live via new ReaderActivity collector

Next:
- Phase 8: device test pass (prd.md §3.4(3) script) + record in this file;
  needs ML models or GLENS-scanned chapter locally (Known issue #3)
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
2026-08-23  app .../presentation/reader/settings/ReadAloudPage.kt        created (Phase 7)
2026-08-23  app .../presentation/reader/settings/ReaderSettingsDialog.kt 4th tab
2026-08-23  app .../ui/reader/setting/ReaderSettingsScreenModel.kt       ttsPreferences
2026-08-23  app .../ui/reader/ReaderActivity.kt   ttsKeepScreenOn collector (+ Phase 6 files fixed)
2026-08-23  app .../ui/reader/ReaderViewModel.kt  TtsError FQN + toDomainChapter!! fixes
2026-08-23  i18n .../base/strings.xml             5 pref_tts_* keys
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

Decision:
Settings tab appended LAST in ReaderSettingsDialog (page 3) so ColorFilter's
dim-amount hack (`pagerState.currentPage == 2`) keeps pointing at ColorFilter.

Reason:
Smallest diff; reordering tabs would silently move the special no-dim behavior.
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
Unit tests:        PASS (this session, full testDebugUnitTest)
Integration tests: none run (existing androidTest suites are device-gated/@Ignore)
UI tests:          none exist in repo
Device tests:      pending — Phase 8 (needs ML models or GLENS-scanned chapter)
Lint:              spotlessCheck PASS (this session)
Build:             :app:compileDebugKotlin PASS (this session; assembleDebug not
                   re-run this session, compile task covers the changed code)
Baseline (pre-TTS expectations): CI order = spotlessCheck → testDebugUnitTest →
                        verifySqlDelightMigration → assembleRelease (see rules.md §11)
Previous verified build:
Date:     2026-08-22
Command:  ./gradlew spotlessCheck testDebugUnitTest :app:assembleDebug
Result:   ALL GREEN (predates WSL-recovery checkpoint; that checkpoint broke
          :app compile — fixed 2026-08-23, see Completed work)
Environment: devcontainer image vsc-yomihon (JDK 17) run via docker on WSL2 host;
            NOTE: packaging OOMs with repo default -Xmx2560m in 7.4 GiB container —
            run with GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g"
            or build on CI/host with more RAM. No repo file changed for this.
            CI (JDK 21, more RAM) unaffected.
```

## Last verified build

```text
Date:     2026-08-23 (this session)
Command:  ./gradlew spotlessCheck :app:compileDebugKotlin   (docker devcontainer, JDK 17)
Result:   BUILD SUCCESSFUL
```

## Last verified test

```text
Date:     2026-08-23 (this session)
Command:  ./gradlew testDebugUnitTest                    (docker devcontainer, JDK 17)
Result:   BUILD SUCCESSFUL (all modules)
```

---

## Agent handoff

```text
Last agent:                 ox-alpha (Phase 7 + Phase-6 break-fix session)
Date:                       2026-08-23
Task completed:             Phase 7 (ReadAloud settings tab: sliders/checkboxes,
                            i18n keys, live keep-screen-on collector) · Fixed
                            Phase-6 compile breaks introduced by WSL-recovery
                            checkpoint commit a36c3cc83 (Event.TtsError
                            self-shadowing; db-vs-domain Chapter mismatch).
Current task:               none (awaiting direction)
Next recommended task:      Phase 8 (docs/phase.md): device test pass of prd.md
                            §3.4(3) script — needs ML models downloaded or a
                            GLENS-scanned chapter on device. Then Phase 9.
Files safe to modify:       docs/* ; app reader/tts/settings files ;
                            i18n base strings.xml (snake_case keys only)
Files currently worked on:  none
Known risks:                ML models absent locally (device verification needs
                            GLENS scan or model download); ReaderActivity dual-
                            composition quirk (#2) when adding overlays; do not
                            touch deleted devcontainer files or untracked
                            AGENTS.md/architect*.md; build env OOM note above;
                            Kotlin scoping gotcha: nested classifiers shadow
                            same-named imports inside sealed interfaces.
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
