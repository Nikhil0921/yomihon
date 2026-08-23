# Yomihon — Implementation Phases

> Roadmap for the Read-Aloud TTS feature and the documentation system that
> governs it. Statuses use: `NOT_STARTED | IN_PROGRESS | BLOCKED | COMPLETED | NEEDS_REVIEW`.
> A phase is COMPLETED only when its verification steps have actually been run
> and recorded in `docs/memory.md`.

Current phase pointer: **Phase 8 IN_PROGRESS (unit portion green; device pass
blocked on hardware/models) → then Phase 9**.

---

## Phase 0 — Repository analysis & documentation system

- **Status**: COMPLETED (this change set)
- **Objective**: map the actual repository; establish the AI documentation/memory
  system so future agents don't restart analysis.
- **Tasks**: repository audit; architecture mapping; reader/OCR/data/DI/network/
  testing inventory; TTS scope confirmation against root `architect.md` /
  `architect-2.md`; creation of `docs/{prd,architecture,rules,phase,design,memory}.md`.
- **Dependencies**: none.
- **Files affected**: `docs/*` only (new).
- **Completion criteria**: six documents exist, cross-checked, no contradictions,
  all claims verified against source.
- **Tests required**: none (documentation only).
- **Verification performed**: full source inspection recorded in `docs/memory.md`
  §Last verified build/test (no code changed).

---

## Phase 1 — TTS foundation

- **Status**: COMPLETED (commit 07c64985f; spotlessCheck + :domain tests green)
- **Objective**: framework-free contracts + pure decision logic in `:domain`,
  wired via Injekt.
- **Tasks**:
  - `domain/src/main/java/mihon/domain/tts/engine/TtsEngine.kt` (initialize,
    language availability, suspend `speak(utteranceId, text)`, rate/pitch setters,
    focus hooks, stop, shutdown).
  - `domain/src/main/java/mihon/domain/tts/TtsAdvancePolicy.kt` (pure advance fn).
  - `domain/src/main/java/mihon/domain/tts/service/TtsPreferences.kt`
    (rate 0.5–2.0 def 1.0, pitch def 1.0, auto page turn def true,
    auto next chapter def false, keep screen on def true).
  - DI bindings: `DomainModule` engine factory, `PreferenceModule` prefs singleton.
- **Dependencies**: Phase 0.
- **Files/modules affected**: `:domain`, `app/.../di/PreferenceModule.kt`,
  `app/.../eu/kanade/domain/DomainModule.kt`.
- **Completion criteria**: interfaces compile; bindings resolve (`:app:assembleDebug`).
- **Tests required**: none yet beyond compile (policy tests land with Phase 4 files
  or here if written together).

## Phase 2 — Android TTS engine

- **Status**: COMPLETED (commit 07c64985f; device check pending — bundled with Phase 8 device pass)
- **Objective**: working system-engine implementation behind the abstraction.
- **Tasks**: `app/.../data/tts/AndroidTtsEngine.kt`; main-thread construction;
  `CompletableDeferred` bridging of `OnInitListener`/`UtteranceProgressListener`;
  `isLanguageAvailable(JAPANESE)` preflight; rate/pitch application;
  `AudioFocusRequest` gain/transient/permanent handling; stop fails pending
  deferreds; shutdown releases resources.
- **Dependencies**: Phase 1.
- **Files affected**: new engine file only.
- **Completion criteria**: engine initializes/shuts down cleanly on device;
  speak() suspends until done/error; focus transitions correct.
- **Tests required**: manual device check (framework-thin by design); no Robolectric.

## Phase 3 — OCR integration

- **Status**: COMPLETED (acquisition + prefetch inside TtsPlaybackController;
  device verification deferred to Phase 8 script)
- **Objective**: feed pages to the pipeline from cache or on-demand scan.
- **Tasks**: cached-first `GetCachedPageOcr`; miss path via
  `OcrPageSourceResolver` + `WithOcrScanSession` + `ScanPageOcr`; bitmap
  acquire/recycle discipline; N+1 prefetch job with cancellation on page change.
- **Dependencies**: Phase 1 (controller skeleton may start in parallel).
- **Files affected**: controller file (`ui/reader/tts/TtsPlaybackController.kt`).
- **Completion criteria**: text acquisition works for cached GLENS chapters and
  uncached pages without leaking bitmaps.
- **Tests required**: covered indirectly by policy tests; manual verification path
  defined in prd.md §3.4(3).

## Phase 4 — Sentence processing

- **Status**: COMPLETED (SentenceSegmenter + SentenceSegmenterTest green)
- **Objective**: pure segmentation honoring manga reading order.
- **Tasks**: `SentenceSegmenter.toTtsSentences()`; terminal punct
  `。！？!?‼⁇⁉⁈` with punctuation attached; remainder fragment; blank-region skip;
  no cross-region merges; reuse existing normalization (no re-cleanup);
  `TtsSentence(text, regionOrder, boundingBox, textOrientation)`.
- **Dependencies**: Phase 1 (models), independent of Phase 2/3.
- **Files affected**: `:domain` segmenter + tests.
- **Completion criteria**: `SentenceSegmenterTest` green covering all cases listed
  in prd.md §3.4(1).
- **Tests required**: JUnit5+Kotest suite in `domain/src/test/java/mihon/domain/tts/`.

## Phase 5 — TTS queue & playback control

- **Status**: COMPLETED (controller loop, TtsPhase state machine, confirm-timeout
  arbitration, policy reuse; device pass deferred to Phase 8)
- **Objective**: play/pause/resume/stop/next/prev sentence + automatic progression.
- **Tasks**: `TtsAdvancePolicyTest` suite; controller loop
  (TEXT→SEGMENT→SPEAK→ADVANCE); state machine
  `TtsPhase { Idle, Preparing, LoadingPage, Playing, Paused, Finished, Error }`;
  `TtsPlaybackState` exposed as StateFlow; user-navigation arbitration
  (`onPageSelected` mismatch rebuilds queue; ~10 s advance-confirm timeout → Paused);
  end-of-content detection (`viewerChapters.nextChapter == null`).
- **Dependencies**: Phases 2–4.
- **Files affected**: controller + `TtsAdvancePolicy` tests.
- **Completion criteria**: all policy branches tested; controller drives a full
  chapter on device including page turn and chapter transition.
- **Tests required**: `TtsAdvancePolicyTest` (all branches from prd.md §3.4(1)).

## Phase 6 — Reader integration

- **Status**: COMPLETED (code + compile verified 2026-08-23, commit 3fc10ad50;
  device verification deferred to Phase 8 pass)
- **Objective**: surface the feature in the reader UI safely.
- **Tasks**: `ReaderViewModel.State.ttsState` + controller lifecycle wiring
  (lazy start, stop in `onActivityFinish`/`onCleared`); new Events
  (`TtsAdvancePage`, `TtsAdvanceChapter`, `TtsError`, `TtsNoTextFound`) handled in
  ReaderActivity event collector; `TtsPlaybackBar` rendered beside
  `OcrLoadingIndicator`; entry icon in `ReaderBottomBar` threaded like `onClickOcr`;
  `onStop` pause; keep-screen-on combination logic.
- **Dependencies**: Phase 5.
- **Files affected**: `ReaderViewModel.kt`, `ReaderActivity.kt`,
  `presentation/reader/*`, `ReaderBottomBar.kt`.
- **Completion criteria**: lifecycle matrix from root architect.md verified on device.
- **Tests required**: unit tests still green; manual matrix run recorded.

## Phase 7 — Settings

- **Status**: COMPLETED (2026-08-23; device verification deferred to Phase 8 pass)
- **Objective**: user control over speech behavior.
- **Tasks**: "Read aloud" tab in `ReaderSettingsDialog` using existing
  `CheckboxItem`/`SliderItem` specs; rate/pitch sliders, auto page turn,
  auto next chapter, keep-screen-on checkboxes; i18n snake_case keys added to base
  `strings.xml` only.
- **Dependencies**: Phases 2 & 6.
- **Files affected**: settings page file, `ReaderSettingsDialog.kt`, i18n base.
- **Completion criteria**: every pref takes effect live on device.
- **Tests required**: spotless; manual toggling script.

## Phase 8 — Testing

- **Status**: IN_PROGRESS (2026-08-23: suites audited vs prd §3.4(1) — no gaps;
  full `testDebugUnitTest` + `spotlessCheck` + `:app:assembleDebug` GREEN.
  REMAINING: on-device script §3.4(3)–(5) — BLOCKED: no adb device attached,
  ML models absent locally)
- **Objective**: complete the test story.
- **Tasks**: ensure segmenter+policy suites comprehensive; run full
  `testDebugUnitTest`; device pass of the prd.md §3.4(3) script (cached path,
  uncached path, webtoon, arbitration, end-of-content, missing-JP-voice error,
  rate/pitch); record results in memory.md.
- **Dependencies**: Phases 1–7.
- **Completion criteria**: all suites green + device checklist recorded.
- **Tests required**: as listed.

## Phase 9 — Performance/stability hardening

- **Status**: IN_PROGRESS (2026-08-23: static audit DONE — bitmap lifecycle +
  cancellation correctness verified, 2 fixes landed [engine init-cancel leak,
  prefetch CE swallow]; gates green. REMAINING device items bundled with the
  Phase 8 device pass: memory/battery profiles, leakcanary, exit-to-idle timing)
- **Objective**: production quality under stress.
- **Tasks**: bitmap lifecycle audit (no retention across suspension points);
  cancellation correctness (swipe-away, chapter switch mid-scan); memory profile
  during long sessions; battery check after exit (no background CPU);
  legacy-model latency masked by Preparing/LoadingPage states + prefetch.
- **Dependencies**: Phase 8.
- **Completion criteria**: no leaks in leakcanary runs; exit-to-idle < ~1 s audio
  stop; documented measurements in memory.md.
- **Tests required**: repeat device matrix + `:app:assembleDebug` release build.

## Phase 10 — Future engines (backlog)

- **Status**: NOT_STARTED (explicitly out of v1)
- **Candidates**: cloud TTS provider(s); local neural TTS; explicit voice picker;
  background playback via FGS+MediaSession; on-image bbox highlight; audio caching
  for high-latency engines; porting Glens ordering into local Legacy/Fast scans.
- **Rule**: each requires a PRD update + architecture review BEFORE coding
  (rules.md §10). None may regress v1 behavior.

---

## Dependency graph

```text
P0 ──> P1 ──> P2 ──┐
        │          ├──> P5 ──> P6 ──> P7 ──> P8 ──> P9
        └──> P3 ───┤
        └──> P4 ───┘
P10 (backlog, gated)
```

Update statuses in this file AND `docs/memory.md` whenever work happens.
