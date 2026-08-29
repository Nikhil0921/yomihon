# Yomihon — Implementation Phases

> Roadmap for the Read-Aloud TTS feature and the documentation system that
> governs it. Statuses use: `NOT_STARTED | IN_PROGRESS | BLOCKED | COMPLETED | NEEDS_REVIEW`.
> A phase is COMPLETED only when its verification steps have actually been run
> and recorded in `docs/memory.md`.

Current phase pointer: **Phase 8 device pass COMPLETE — script steps 1–15 ALL
EXECUTED + USER-CONFIRMED (2026-08-28 RUN4 build 0.4.0-8238: steps 7–15 PASS;
user confirmed step 7 rate/pitch live + step 11 rotation pause→resume-same-OCR-
line). Chapter transition, end-of-content, home-during-playback, exit reader,
audio-focus (PermanentLoss→pause→manual resume same sentence), exit-idle 550ms,
4/4 advance confirms 1–2ms / 0 timeouts, 6 next-sentence steps clean,
ScrollToRegion on-device verified, 3 clean pause/resume cycles. FINDING #4 (P1)
FIXED 2026-08-28: background prefetch scan failure (transient DNS) escalated
via scanOnDemand.fail() and killed a healthy paused session → reportFailure
param gates failure reporting (prefetch passes false, logs best-effort).
Uncommitted set: z-order fix + action logging + prefetch fix (all gates green);
prefetch fix not yet device-verified. Phase 9 perf passes #1+#2, P0/P1,
advance-confirm/stale-page fixes previously LANDED+VERIFIED (run2/run3: 22/22
advances 1–5ms / 0 timeouts). Finding #3 duplicate speech USER DROPPED
2026-08-28 — LOW PRIORITY post-build (memory.md Deferred issues)**.
PRODUCT PIVOT 2026-08-25: English is the primary v1 Read-Aloud language;
Japanese TTS moved to Phase 10.

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

- **Status**: COMPLETED (commit 07c64985f; 2026-08-25: Japanese preflight
  REMOVED per product pivot — engine speaks system-default voice; device check
  bundled with Phase 8 device pass)
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

- **Status**: COMPLETED (SentenceSegmenterTest green; 2026-08-25 extended to
  English rules: ASCII '.' terminal before whitespace/EOL, dot-runs glued,
  decimals safe; 15 cases)
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

- **Status**: COMPLETED (2026-08-28: device script steps 1–15 all executed +
  user-confirmed; all suites green. History: 2026-08-25: stabilization change
  set landed — Glens
  strip tiling + EN ordering, JP gate removal, segmenter EN rules, progression
  fixes; spotlessCheck + testDebugUnitTest + :app:assembleDebug GREEN.
  2026-08-26: device script steps 1–6 executed (3 PASS / 3 PASS-WITH-ISSUE,
  evidence in .device-pass/); Phase 9 perf pass #1 landed (parallel tiles,
  scan single-flight, task-owned bitmap+upsert) — build 0.4.0-8233 installed.
  2026-08-27: Phase 9 perf pass #2 landed — webtoon advance-confirm fix
  (findFirstVisibleItemPosition), region-level auto-scroll (ScrollToRegion
  event), pause/resume page-awareness — build 0.4.0-8234 installed. All gates
  green. 2026-08-28: FRESH logcat analysis COMPLETE — logcat-8234-phase9.log
  (7,793 lines, PID 27363). Results: 3 chapters tested (206, 205, 1),
  9/9 page advances confirmed, 0 recycle crashes, 0 sentence failures,
  0 timeouts. Known issues #8 + #10 RESOLVED. Two new issues found:
   P0 prefetch spam loop, P1 rapid-swipe mass OCR — both FIXED 2026-08-28.
   Build 0.4.0-8236 run2/run3 verified Finding #1/#2; Finding #3 deferred LOW
   PRIORITY post-build. 2026-08-28 RUN4 (build 0.4.0-8238): script steps 7–15
   EXECUTED — chapter transition, end-of-content, home-during-playback, exit
   reader, audio-focus, exit-idle all PASS; 4/4 advance confirms 1–2ms / 0
   timeouts; ScrollToRegion on-device verified. USER CONFIRMED: step 7
   rate/pitch live PASS + step 11 rotation PASS (pause → resume same OCR
   line/position). FINDING #4 (P1) prefetch-DNS escalation FIXED 2026-08-28
   (reportFailure gate). Steps 1–15 all executed + confirmed — phase complete.
   The "missing-JP-voice" branch of the old script is OBSOLETE after the pivot.)
- **Objective**: complete the test story.
- **Tasks**: ensure segmenter+policy suites comprehensive; run full
  `testDebugUnitTest`; device pass of the prd.md §3.4(3) script (cached path,
  uncached path, webtoon strip incl. tiling, arbitration, end-of-content,
  rate/pitch); record results in memory.md.
- **Dependencies**: Phases 1–7 + 2026-08-25 stabilization fixes.
- **Completion criteria**: all suites green + device checklist recorded.
- **Tests required**: as listed.

## Phase 9 — Performance/stability hardening

- **Status**: IN_PROGRESS (static audit DONE 2026-08-23; reader-stabilization
  pass DONE 2026-08-25; perf pass #1 DONE 2026-08-26 [tile parallelism ×3,
  scan single-flight, task-owned bitmap+upsert — kills duplicate scans and
  recycle crashes, ~3× faster pages expected]; perf pass #2 DONE 2026-08-27
  [webtoon confirm fix: findFirstVisibleItemPosition, region-level auto-scroll
  via TtsEvent.ScrollToRegion, pause/resume page-awareness] — all gates green,
  build 0.4.0-8234 installed. 2026-08-28: FRESH logcat analysis COMPLETE.
  Phase 9 fixes VERIFIED: findFirstVisibleItemPosition ✓, single-flight ✓,
  task-owned bitmap ✓, pause/resume page-awareness ✓, tile parallelism ✓.
  Known issues #8 + #10 RESOLVED. Two new issues found: P0 prefetch spam
  loop, P1 rapid-swipe mass OCR. 2026-08-28: P0 + P1 FIXED (rebuild dedup +
  prefetch guard in TtsPlaybackController; 250ms debounce in
   ReaderViewModel.onPageSelected, advance confirmations exempt) + ScrollToRegion
   logcat instrumentation added. All gates green. 2026-08-28: build 0.4.0-8236
   device run2/run3 verified advance-confirm/stale-page fixes; hands-off duplicate
   speech not reproduced and USER DROPPED it as LOW PRIORITY post-build. TTS-DBG
   removed. REMAINING: device script steps 7–15, memory/battery/leakcanary
   profiles, exit-to-idle timing. Mini-player z-order fix DONE 2026-08-28
   (UNCOMMITTED, ReaderActivity.kt — TtsPlaybackBar moved before dialog block).
   Controller action-level logging DONE 2026-08-28 (UNCOMMITTED,
   TtsPlaybackController.kt — pause/resume/stop/stepBy/focus-loss; closes Known
   issue #9, makes script steps 4/5 verifiable).)
- **Objective**: production quality under stress.
- **Tasks**: bitmap lifecycle audit (no retention across suspension points);
  cancellation correctness (swipe-away, chapter switch mid-scan); memory profile
  during long sessions; battery check after exit (no background CPU);
  latency masked by Preparing/LoadingPage states + prefetch.
- **Dependencies**: Phase 8.
- **Completion criteria**: no leaks in leakcanary runs; exit-to-idle < ~1 s audio
  stop; documented measurements in memory.md.
- **Tests required**: repeat device matrix + `:app:assembleDebug` release build.

## Phase 10 — Future engines (backlog)

- **Status**: NOT_STARTED (explicitly out of v1)
- **Candidates**: Japanese/multi-language TTS voice selection (revisit the
  language-preflight removed on 2026-08-25 as an explicit opt-in, not a gate);
  cloud TTS provider(s); local neural TTS; explicit voice picker;
  background playback via FGS+MediaSession; on-image bbox highlight; audio
  caching for high-latency engines; porting Glens ordering into local Legacy/Fast
  scans; cross-tile text merge for seam bubbles (Known issue #6).
- **Advanced TTS / Voice Calibration system** (explicit requirement added
  2026-08-25): dedicated Read-Aloud configuration beyond the current basic
  rate/pitch controls. Must investigate and calibrate: multiple TTS engines;
  multiple voice models; voice selection/picker; voice quality comparison;
  language/locale-specific voices; voice-specific rate tuning; voice-specific
  pitch tuning; engine-specific settings; neural/local TTS models where
  appropriate; cloud TTS providers where appropriate; higher-quality narration
  voices; latency comparison between engines; audio caching for high-latency
  engines; per-engine/per-voice configuration where technically appropriate.
  Revisit ONLY after Phase 8/9 device verification + performance work complete.
  v1 scope unchanged — do not pull any of this forward.
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
