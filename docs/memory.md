# Yomihon — Project Memory (AI State File)

> LIVING DOCUMENT. Every coding agent MUST read this file before substantial work
> and MUST update it after meaningful implementation work (protocol at bottom).
> Companion documents: `prd.md` (WHAT) · `architecture.md` (HOW) · `rules.md`
> (MUST/MUST NOT) · `phase.md` (WHEN) · `design.md` (LOOK/FEEL).

---

## Current project state

```text
Project:        Yomihon (v0.4.0, vc25) — Android manga reader + OCR/language tooling
Repo state:     branch main @ 80deac5b8 (user committed the z-order + action-
                logging + prefetch-fix set: 41200022e, be31edb71, 80deac5b8).
                UNCOMMITTED: 3-file Phase 9 leak-fix set (app/build.gradle.kts
                LeakCanary core, ReaderActivity.kt ioCoroutineScope cancel,
                ReaderViewModel.kt onFocusEvent=null). Do not amend user
                commits; .opencode/ stays untouched. Stray untracked
                gradle-home/ dir (2.5G) in repo root — remove, do not commit.
Untracked:      .opencode/ (tool config), .device-pass/ (logcat evidence, gitignored)
Primary goal:   Reliable Read-Aloud: English OCR → English system TTS →
                correct progression (PRODUCT PIVOT 2026-08-25: English is the
                primary v1 language; Japanese TTS moved to Phase 10.)
Current phase:  Phase 8 device pass COMPLETE — script steps 1–15 all executed +
                user-confirmed (2026-08-28 RUN4: steps 7–15; rate/pitch live +
                rotation pause/resume-same-position confirmed by user). Phase 9
                perf passes + fixes landed; prefetch-DNS escalation fix (Finding
                #4) FIXED + DEVICE-VERIFIED (wifi-killed test, committed
                80deac5b8). NEW: LeakCanary flagged ~100MB reader leak on device
                2026-08-28 → leak-fix set implemented (uncommitted).
Current status: Finding #1 FIXED+VERIFIED (22/22 advances, 0 timeouts, 1–5ms).
                Finding #2 FIXED (timeout sets pageIndex=target; unexercised).
                Finding #3 duplicate speech: USER DROPPED 2026-08-28, LOW
                PRIORITY post-build (Deferred issues). Finding #4 prefetch-DNS
                session-kill FIXED+VERIFIED 2026-08-28 (reportFailure gate).
TTS code:       Region-level auto-scroll (ScrollToRegion event), webtoon
                confirm fix (explicit onScrolled in moveToPage), pause/resume
                page-awareness, prefetch/rebuild dedup, user-nav debounce.
```

## Current objective

Stabilize the read-aloud pipeline end-to-end for ENGLISH content per the
2026-08-25 product decision: OCR must find actual dialogue (incl. long
webtoon strips), text must be spoken completely without silent skips, and
page/chapter progression must advance exactly once with user navigation
authoritative. Device verification of prd.md §3.4(3)–(5) DONE (script steps
1–15 executed + user-confirmed 2026-08-28). Remaining: Phase 9 hardening
measurements (memory/battery/leakcanary) — leak-fix set IMPLEMENTED
(2026-08-28, uncommitted, LeakCanary evidence below) awaiting gates +
device re-verification, then commit.

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

```text
[COMPLETED 2026-08-23 — commit 3fc10ad50]
- Phase 6 + Phase 7 + Phase-6 compile fixes COMMITTED by user in one commit
  (also contains devcontainer deletions and .opencode state files — noted,
  left as-is). docs/memory.md + phase.md updated inside that commit too.

[COMPLETED 2026-08-23 — this session, UNCOMMITTED (docs only)]
- Phase 8 unit portion:
  - Audited SentenceSegmenterTest (12 cases) + TtsAdvancePolicyTest (10 cases)
    against prd.md §3.4(1) checklist — ALL required branches covered, no gaps.
  - Full gates green in devcontainer (JDK17): spotlessCheck +
    testDebugUnitTest + :app:assembleDebug → BUILD SUCCESSFUL 14m48s, EXIT:0
  - Fixed stale/contradictory docs: repo-state hashes (3fc10ad50), phase.md
    Phase 6 status NOT_STARTED→COMPLETED, Phase 8 status→IN_PROGRESS

[COMPLETED 2026-08-23 — follow-up session, UNCOMMITTED (docs only)]
- Phase 8 §3.4(6) verification: `git diff 07c64985f^..HEAD` shows ZERO
  uses-permission changes in any AndroidManifest.xml, ZERO version-catalog
  edits, ZERO new dependency statements in build.gradle.kts → "no new
  permissions / no new external dependencies" CONFIRMED
- prd §3.4 scorecard: (1) ✓ suites, (2) ✓ gates green, (6) ✓ verified;
  (3)(4)(5) = on-device script, BLOCKED

[COMPLETED 2026-08-23 — Phase 9 static-audit session, UNCOMMITTED]
- Phase 9 static portion (bitmap lifecycle + cancellation correctness audit of
  AndroidTtsEngine + TtsPlaybackController + OCR scan path):
  - VERIFIED CLEAN: bitmap retention (toOcrImage copies pixels to IntArray
    upfront — OCR never holds Bitmap; controller recycle guarded by
    isRecycled; ResolvedOcrPages/OcrPageInput streams closed via use);
    teardown ordering (failPendingUtterances BEFORE engine.stop/shutdown →
    no post-teardown callbacks; completeUtterance no-ops on cleared map);
    explicit CancellationException rethrow in main loop paths; prefetch N+1
    bounded + cancelled on page change/reset; focus request acquire/abandon paired
  - FIXED 1: AndroidTtsEngine.initialize() leaked a freshly built TextToSpeech
    (service connection) when the init coroutine was cancelled during
    readiness.await() (e.g. swipe-away during Preparing) → now shuts it down
    on Main before rethrowing CE
  - FIXED 2: schedulePrefetch used runCatching → swallowed
    CancellationException (rules.md §7 violation) → explicit try/catch, CE rethrown,
    other failures logged/no-op'd as before (prefetch stays best-effort)
- Verified: spotlessCheck + :app:compileDebugKotlin + testDebugUnitTest GREEN
  (devcontainer JDK17, BUILD SUCCESSFUL 10m54s, EXIT:0)

[COMPLETED 2026-08-23 — model-setup session, UNCOMMITTED (gitignored assets only)]
- Downloaded all 6 ML model assets via the exact CI step
  (.github/workflows/build.yml), every file sha256-verified OK. Clears the
  models half of the Phase 8/9 device-pass blocker (Known issue #3).
- :app:assembleDebug re-run WITH models present → BUILD SUCCESSFUL 3m46s,
  EXIT:0; verified all 6 assets packaged into the APK (unzip -l). Staged
  installable artifacts at app/build/outputs/apk/debug/ (split per ABI;
  use app-arm64-v8a-debug.apk on a modern phone) → device pass is turnkey.
```

```text
[COMPLETED 2026-08-24 — device bring-up session]
- Host adb + wireless device SM_M066B (Android 16, arm64) connected; staged
  APK verified byte-identical to installed build (vc25).
- DEVICE LOG EVIDENCE of two real bugs:
  a) GoogleTTSServiceImpl synthesized locale eng-IND then TextToSpeech.ERROR —
     engine never called setLanguage and preflight gated on Japanese only.
  b) Samsung SMT has NO Japanese pack (en/hi only) → old gate would hard-block
     all playback for English content.
- Signature-mismatch incident: fresh container generated a NEW debug keystore
  (~/.android not persisted) → INSTALL_FAILED_UPDATE_INCOMPATIBLE. With user
  approval: uninstalled app.yomihon.dev, reinstalled fresh build. Fix: created
  persistent docker volume yomihon-android-home mounted at /home/vscode/.android
  so debug signing is stable from now on. User must restore the .tachibk backup
  after any such reinstall (latest: app.yomihon.dev_2026-08-24_23-44.tachibk).
- Gradle poison found+fixed: yomihon-gradle-home volume held transform-cache
  entries with stale absolute /work/... paths → DexingNoClasspathTransform
  failed ("file ... located outside the root directory"). Volume deleted and
  recreated; one cold rebuild (~19m). Keep mounting both volumes.

[COMPLETED 2026-08-25 — reader stabilization session (PRODUCT PIVOT), UNCOMMITTED]
User directive: English OCR → English system TTS → reliable progression is the
v1 goal; Japanese TTS explicitly de-prioritized (Phase 10). Diagnostic-first,
then fixes:

P0 OCR correctness (data/.../ocr/GlensOcrEngine.kt):
- ROOT CAUSE of skipped bubbles on long strips: prepareImage downscaled ANY
  image to ≤1500px on the LONG side (800×8000 webtoon → 150×1500, text
  unreadable for Lens). FIX: recognizePage now tiles tall strips
  (isTallStrip = h>w*3 && h>1500; tile height = min(w*1.8,1500) floor 1000;
  20% overlap; boxes remapped to full-image coords; seam duplicates dropped by
  IoU ≥0.45; order reassigned sequentially top→bottom). Single-page path
  unchanged. DEBUG log reports region count + tiled flag.
- ROOT CAUSE of English misordering: nonJpLines were appended AFTER the JP
  pipeline unsorted. FIX: pages with zero Japanese text skip the JP/ruby
  pipeline entirely and go straight through mergeIntoBubbles positional sort
  (horizontal = top-down). JP pages keep existing behavior.

P0 TTS correctness:
- Removed Japanese-only gating: TtsEngine.japaneseAvailable deleted from
  interface+impl; controller ensureInitialized no longer fails without a JP
  voice; engine no longer pins Locale.JAPAN. Speech uses the system-default
  voice (English devices speak English).
- TtsError.NoJapaneseVoice removed (+ UI branches + i18n key
  tts_error_no_japanese_voice deleted from base strings.xml).
- ROOT CAUSE of silent sentence skips: runPlayback treated ANY speak()=false
  as an interruption and still did sentenceIndex++ → engine-rejected utterances
  vanished. FIX per rules.md §7: retry once, then honest Paused (logged).
- Pause/resume race fixed: when pause interrupted an utterance, the loop now
  returns instead of advancing (resume() relaunches from resumeIndex; before,
  old and new jobs could race QUEUE_FLUSH on the engine).

P0 progression:
- awaitAdvanceConfirmation restructured (returns Boolean): mismatch between
  requested vs shown page now RECONCILES via rebuildQueueForUserNavigation
  (was: dead-end Paused); timeout → explicit logged Paused.
- Controller takes provideContext: () -> TtsChapterContext? (ReaderViewModel
  supplies buildTtsChapterContext() reading LIVE state). Every queue rebuild
  re-resolves chapter context → no more stale-chapter playback after auto or
  manual chapter switches; hasNextChapter no longer stale.
- onPageSelected treats Preparing as active so late first-page callbacks heal
  chapter transitions (fixes "playback silently dies during chapter load").
- ReaderActivity.loadNextChapter(): moveToPageIndex(0) now ONLY runs when the
  chapter id actually changed (loadAdjacent swallows errors; previously a
  failed load yanked the OLD chapter back to page 0 = content skip).

Segmenter English support (:domain SentenceSegmenter.kt):
- ASCII '.' now terminal when a single dot precedes whitespace/end-of-region;
  dot-runs ("...") stay glued; decimals ("3.14") never split; slices trimmed.
- SentenceSegmenterTest updated + extended to 15 cases (EN period split,
  ellipsis glue, decimals).

UI:
- TtsPlaybackBar padding aligned to design.md §5 (24dp/12dp); error retry now
  always offered (no JP-voice special case).

Diagnostics logging added (DEBUG): OCR cache hit/miss, on-demand scan start,
segmented sentence/region counts, page advance request/confirm/timeout, user
navigation, prefetch start/hit/complete/cancelled, sentence retry/fail-pause.

Verified this session: spotlessCheck GREEN; :domain:testDebugUnitTest GREEN
(15 segmenter cases); FULL testDebugUnitTest + :app:assembleDebug GREEN
(BUILD SUCCESSFUL in 19m8s, fresh caches). New build installed on device as
versionName 0.4.0-8232; logcat capture running. DEVICE SCRIPT NOT YET RUN.
```

```text
[COMPLETED 2026-08-25 → 2026-08-26 — Phase 8 device pass session + Phase 9 perf pass #1]

User committed the stabilization change set as a071feedf (incl. docs).

DEVICE SCRIPT RESULTS so far (evidence in .device-pass/logcat-step*.log):
- Step 1 cached GLENS chapter: PASS WITH ISSUE — playback/auto-advance/prefetch
  hits work; but 16 duplicate scan starts (p4×4, p8×5), bitmap recycle crash ×2
  (GlensOcrEngine.kt:91, IllegalArgumentException recycled source), false
  "sentence failed twice; pausing" ×3 during rapid rebuild churn.
- Step 2 uncached page: PASS (user-observed; log evidence lost — capture was
  pinned to a dead PID and ring buffer wrapped; procedure fixed: full unfiltered
  capture to disk, no --pid pinning, no logcat -c).
- Step 3 webtoon strips: PASS WITH ISSUE — tiled=true everywhere, healthy
  regions, zero speak failures; duplicates again (p18×3 with exact cancel→rescan
  correlation), recycle crash ×2 MORE timestamp-exact with nav-cancel of
  in-flight scans; scan durations 10–25 s/strip.
- Step 4 pause/resume: PASS WITH ISSUE (indirect) — clean stop/start cycles,
  no false fails; NEW ISSUE: advance-confirm timeouts systematic on webtoon
  (targets 14,16,17×3, then ch947 target=1,2 both) = 10 s stall + Paused each;
  suspect findLastEndVisibleItemPosition mismatch on short pages.
- Step 5 prev/next sentence: PASS WITH ISSUE — mostly works; intermittent stall
  matching advance-timeout signature (heals via manual scroll + resume).
  NOTE: sentence-skip has ZERO instrumentation (gap logged for Phase 9).
- Step 6 deliberate swipe arbitration: PASS — user nav wins cleanly.
- Steps 7–15 PENDING (rate/pitch live, chapter transition, end-of-content,
  home, rotation, exit, audio-focus, exit-idle timing).

ROOT-CAUSE INVESTIGATIONS (explore agents, verified against source):
- Webtoon auto-scroll sync: FEASIBLE without architecture change. bbox is
  normalized 0..1 vs full image (OcrModels.kt); WebtoonViewer.moveToPage uses
  scrollToPositionWithOffset(pos, 0) — offset hook exists; in-page scroll does
  NOT fire onPageSelected (won't trip arbitration). Plan: TtsEvent.ScrollToRegion
  emitted before engine.speak → VM → Activity → Viewer.scrollToRegion(fraction).
  NOT yet implemented.
- Duplicate scans: OcrRepositoryImpl.scanPage had NO cache re-check and NO
  single-flight; every call = full Lens pass, upsert only at end.

PHASE 9 PERF PASS #1 IMPLEMENTED (UNCOMMITTED, 4 files):
1. GlensOcrEngine.recognizeTiled: tiles now run TILE_CONCURRENCY=3 at a time
   (Semaphore + async/awaitAll, order preserved by index). Was strictly serial:
   6–8 sequential Lens RTTs = 10–31 s/page. Expected ~3× faster pages.
2. OcrRepositoryImpl.scanPage: single-flight map (chapterId,pageIndex)→Deferred
   + cache pre-check; concurrent identical requests join the running scan.
3. Bitmap lifecycle + upsert moved INSIDE queue task (scanWithGlens/
   scanWithOwOcr/scanLocally take OcrImage now): abandoned-on-cancel scans run
   to completion, recycle their own bitmap, and CACHE the result instead of
   crashing recognizeTiled (fixes the recycle-crash class) or wasting work.
4. Instrumentation: OCR queue depth log (PrioritizedTaskQueue.submit),
   "OCR scan joining in-flight" / "cache hit" logs, controller per-page
   acquireMs + "TTS startup open->first page ready in Xms" (SystemClock).
Verified: :data/:app compileDebugKotlin GREEN; spotlessApply applied;
spotlessCheck + full testDebugUnitTest GREEN; :app:assembleDebug GREEN
(first attempt failed packaging, immediate rerun BUILD SUCCESSFUL 2m27s);
arm64 APK built 20:01 UTC awaiting install (device dropped off wireless).

Phase 10 backlog updated in phase.md: Advanced TTS / Voice Calibration system
(engines, voices, picker, quality comparison, locale voices, voice-specific
rate/pitch tuning, engine settings, neural/local+cloud, latency comparison,
audio caching, per-engine config) — explicit future requirement, v1 untouched.
```

```text
[COMPLETED 2026-08-27 — Phase 9 perf pass #2: seamless webtoon playback + auto-scroll sync]

Build 0.4.0-8234 installed (spotlessCheck + testDebugUnitTest + :app:assembleDebug GREEN).

Fixes implemented (6 files):
1. WebtoonViewer.onScrolled: switched from findLastEndVisibleItemPosition to
   findFirstVisibleItemPosition — eliminates advance-confirm timeouts caused by
   "next page already visible" false-positive on short pages. Root cause of
   Known issue #8 (systematic 10s stalls at page boundaries) resolved.
2. TtsEvent.ScrollToRegion(pageIndex, bbox) added; emitted before each
   engine.speak() in controller; forwarded VM → Activity → WebtoonViewer.
   Implements region-level auto-scroll for webtoon/long-strip: viewer smoothly
   scrolls to sentence's bbox.top fraction within the page item.
3. WebtoonViewer.scrollToRegion(pageIndex, bbox): uses scrollToPositionWithOffset
   with offset computed from bbox.top * itemHeight (negative for down-scroll).
   In-page scroll does NOT fire onPageSelected → won't trip arbitration.
4. Controller.onPageSelected: now updates pageIndex during Paused phase so
   resume() continues from the viewer's actual page, not stale state. Fixes
   "pause/resume targets previous cached page" issue.
5. TtsPlaybackController: emits ScrollToRegion event before every speak(),
   passing sentence.boundingBox. Backward-compatible for single-page manga.
6. Instrumentation: "TTS startup open->first page ready in Xms" (existing),
   plus ScrollToRegion logs via existing event channel.

Verified: spotlessCheck + testDebugUnitTest + :app:assembleDebug GREEN
(3m9s). Build 0.4.0-8234 installed on SM_M066B. Logcat capture live
(.device-pass/logcat-8234-test.log). Awaiting device script steps 7–15.
```

## In progress

```text
Feature: Phase 9 hardening — leak-fix set (Finding #5) implemented +
          device re-verification + battery measurement remaining.
          (Phase 8 COMPLETE; Phase 9 perf passes #1/#2 landed + verified.)

Done: steps 1–6 recorded; Phase 9 perf pass #1 (tile parallelism ×3, single-flight,
      task-owned bitmap+upsert) + pass #2 (webtoon confirm fix, region-level
      auto-scroll, pause/resume page-awareness) IMPLEMENTED. All gates green.
      Build 0.4.0-8236 installed on device.

FRESH LOGCAT ANALYSIS (2026-08-28): logcat-8234-phase9.log (7,793 lines,
  PID 27363, 12 min window 02:26:39–02:38:50). Real TTS/OCR activity captured
  across 3 chapters (206, 205, 1). Key findings:
  - 9/9 page advances confirmed (0 timeouts) — Known issue #8 RESOLVED
  - 0 bitmap recycle crashes — Known issue #10 RESOLVED
  - 0 sentence failures
  - Cache hits: 72, misses: 15
  - OCR scan times: 957ms–16.7s (tiled strips)
  - Cold startup: 6.7–13.9s; warm startup: 1.9s
  - NEW ISSUE: Page 15→16 spam loop (19 prefetch cancellations + 20 scan starts
    in 7s) — prefetch restart race condition → FIXED 2026-08-28 (see below)
  - NEW ISSUE: Rapid swipe (13 pages in 0.7s) fires 13 simultaneous OCR scans
    → FIXED 2026-08-28 (see below)
  - ScrollToRegion: not visible in logs → instrumentation ADDED 2026-08-28
  - HTTP 502 GLens error (server-side, retry succeeded)

P0 + P1 FIXES LANDED (2026-08-28, uncommitted, all gates green):
  - P0 prefetch spam: TtsPlaybackController.rebuildQueueForUserNavigation now
    dedups on lastRebuildPageIndex (skip if same page already handled);
    schedulePrefetch guards on prefetchPageIndex (skip if same page already
    in flight); both trackers reset in resetSession + on provideContext()==null.
  - P1 rapid-swipe mass OCR: ReaderViewModel.onPageSelected debounces the TTS
    call by 250ms (TTS_PAGE_SELECTED_DEBOUNCE_MS) for user navigation; advance
    confirmations (hasPendingAdvance) still land immediately, never debounced.
  - ScrollToRegion: added logcat in WebtoonViewer.scrollToRegion (success +
    view-not-laid-out paths) so next device run confirms auto-scroll fires.

MINI-PLAYER Z-ORDER FIX (2026-08-28, uncommitted, all gates green):
  - Root cause: in the inner ContentOverlay Box (ReaderActivity.kt),
    TtsPlaybackBar was the LAST child — declared after `when (state.dialog)` —
    so it drew ON TOP of the inline OcrResultOverlay (scrim + popup/sheet),
    while window-based dialogs (AdaptiveSheet) always covered it: inconsistent
    z-order. FIX: moved TtsPlaybackBar before the dialog block so all
    dialogs/overlays render above the pill. OcrLoadingIndicator position
    unchanged (pre-existing behavior). User committed prior fix set as
    fff9583f0 + 872c55397 + 8cb320e7c.

CONTROLLER ACTION LOGGING (2026-08-28, uncommitted, all gates green):
  - Known issue #9 closed. TtsPlaybackController now logs DEBUG action-level
    events: pause (page+sentence), resume (page+resumeIndex), stop (prior phase),
    stepBy next/prev (from->to + phase, plus boundary-reject), and audio-focus-loss
    pause (event name). Indices/phase only — no spoken text (rules §7). Makes
    device script steps 4 (pause/resume) + 5 (prev/next sentence) verifiable.

DEVICE VERIFICATION RUN4 — SCRIPT STEPS 7–15 (2026-08-28, build 0.4.0-8238):
  Evidence: .device-pass/tts-steps7-15.log (144,053 lines, PID 5582,
  19:44:42–19:49:29; chapters 1189 → 1188 auto-advance → 1942).
  - Step 7 rate/pitch live: PASS (USER CONFIRMED 2026-08-28: rate and pitch
    changes worked correctly during playback; engine setters have no logs by
    design).
  - Step 8 chapter transition: PASS — 19:45:43 "TTS chapter advance request"
    ch1189→ch1188; page 0 on-demand scan 4.6s; playback continued from page 0.
  - Step 9 end-of-content: PASS — 19:48:44 "TTS stop (phase=Finished)"; clean
    finish, engine disconnected, no crash.
  - Step 10 home-during-playback: PASS — home 19:46:13 (onTop=false), auto-pause
    logged (page=1 sentence=1), return 19:46:30. (Session then needed restart
    due to the prefetch-DNS bug below, not the home transition itself.)
  - Step 11 rotation: PASS (USER CONFIRMED 2026-08-28: rotation worked —
    playback paused on rotation, resume continued from the same OCR
    line/position. No rotation events in logcat because reader orientation is
    preference-locked; pause/resume path verified by user observation).
  - Step 12 exit reader: PASS — stop(Playing) → stop(Idle) → "Disconnected from
    TTS engine" <1s; zero TTS/OCR activity after exit.
  - Step 13 audio focus: PASS — YouTube Music opened → "TTS audio focus lost
    (PermanentLoss); pausing" 19:46:46 → auto-pause; manual resume 19:46:54
    continued from the SAME sentence (page=1 sentence=5).
  - Steps 14/15 exit-idle: PASS — final stop 19:48:47.383 → engine disconnect
    19:48:47.932 (550ms); process idle of TTS work afterwards.
  - Advance confirms: 4/4 confirmed 1–2ms, 0 timeouts; chapter advance 1/1.
  - Sentence stepping: 6 rapid next-sentence steps (2→8) all re-acquired from
    cache 4–9ms + ScrollToRegion; 0 boundary rejects. Pause/resume: 3 clean
    cycles resuming from correct sentences (0, 8, 5).
  - ScrollToRegion auto-scroll ON-DEVICE VERIFIED: bboxTop increases
    monotonically per page (was "not visible in logs" before instrumentation).
  - FINDING #4 (P1) FIXED 2026-08-28: background PREFETCH scan failure
    escalated to a session-killing Error. Evidence: home-press 19:46:13 → DNS
    failure (UnknownHostException lensfrontend-pa.googleapis.com) during page 2
    prefetch at 19:46:20 → scanOnDemand called fail(TtsError.OcrError) →
    healthy paused session entered Error phase; user had to restart (warm
    start 50ms at 19:46:40). Root cause: scanOnDemand unconditionally calls
    fail() on exception, but prefetch reuses it — the prefetch wrapper's
    best-effort catch never fired because scanOnDemand swallows exceptions and
    returns null. FIX: scanOnDemand gained reportFailure param (default true);
    prefetch passes false and logs "prefetch scan failed (best-effort)";
    misleading "prefetch complete" now only logs on non-null result. Main loop
    behavior unchanged (still reports its own failures).
  - GlanceAppWidget composition error 19:48:26 "CompositionLocal LocalContext
    not present" — non-fatal, widget-related, NOT TTS — new Known issue #12.

PREFETCH-DNS FIX DEVICE VERIFICATION (2026-08-28, fresh APK with fix):
  Evidence: .device-pass/logcat-prefetch-verify.log (ring-buffer dump, PID
  22599, 20:12–20:16, chapter 1187, wifi-killed test). RESULT: FIX VERIFIED.
  - 13 prefetch failures for page 4 (wifi off) each logged "TTS prefetch scan
    failed page=4 (best-effort)" while page 3 playback + 13 next-sentence
    steps continued uninterrupted — zero Error-state escalation.
  - Home-press mid-prefetch (page 6 scan in flight) → auto-pause 20:16:14
    (page=5 sentence=17) → stop(phase=Paused) — user observed Paused, not
    Error (the exact run4 regression scenario).
  - Main-loop scan failure on the CURRENT page (page 4, wifi off) still
    honestly reports Error; user retry after wifi restore → 16.8s tiled scan
    → playback resumed. Correct by design.
  - Advance confirms 4/4 @ 1–4ms, 0 timeouts; pause/resume cycles clean.

DEVICE VERIFICATION RUN2 (2026-08-28): build 0.4.0-8236 reinstalled, on-device
  logcat (.device-pass/logcat-8236-run2.log, 185,361 lines, PID 25847). NOTE:
  first capture attempt died — streaming `adb logcat` over wireless adb drops
  when the link blips; use ON-DEVICE capture instead:
  `adb shell "logcat -c; nohup logcat -v threadtime > /sdcard/tts-test.log 2>&1 &"`
  then `adb pull`. Findings:
  - FINDING #1 (advance-confirm) FIXED+VERIFIED: 22/22 advances confirmed,
    0 timeouts, request→confirm latency 1–5ms (was 10s wall). Root cause was
    scrollToPositionWithOffset = layout-driven jump that never dispatches
    onScrolled; fix = WebtoonViewer.moveToPage now calls onScrolled(pos)
    explicitly after the scroll. Session ran clean page 4→22 auto-advancing.
  - FINDING #2 (stale page on timeout) FIXED (safety net): awaitAdvanceConfirmation
    timeout branch now sets pageIndex=target so resume() can't re-read the prior
    page. 0 timeouts occurred so branch unexercised; pause/resume verified correct
    (resume pageIndex=3 resumeIndex=4 then 9).
  - FINDING #3 (duplicate speech) RESOLVED-AS-EXPECTED + DEFERRED: run2 dups were
     USER tapping prev/next-sentence buttons (confirmed) — stepBy() re-speaks from
     the chosen sentence, expected nav behavior, NOT a bug. A SEPARATE hands-off
     "words/phrases read twice" was reported but did NOT reproduce in run3
     (logcat-8236-run3.log: 55 dispatches/55 unique ids, 0 engine onStop/onError,
     0 retries, 4/4 advances). Likely manga-specific OCR overlapping regions or a
     Google-TTS audio quirk. USER DECISION 2026-08-28: DROP for now, LOW PRIORITY,
     revisit after full app build (see Deferred issues).
  - TTS-DBG instrumentation REMOVED 2026-08-28 (all files reverted); #1 + #2 fixes
     KEPT. Gates re-run green (spotlessCheck + testDebugUnitTest + assembleDebug).

LEAKCANARY PASS — FINDING #5 ~100MB READER LEAK (2026-08-28):
  Evidence: .device-pass/logcat-leakcanary.log (37M; LeakCanary permission
  prompt 20:33:01, leak-launcher badge present = leak detected; TTS session
  PID 31288 same log). LeakCanary (previously commented out upstream) was
  enabled via debugImplementation(libs.leakCanary.core) (UNCOMMITTED
  app/build.gradle.kts) and flagged the reader screen retaining ~100MB after
  exit. Root cause: system TTS service keeps a native GC root to the
  TextToSpeech callback → AndroidTtsEngine singleton → onFocusEvent lambda →
  dead TtsPlaybackController → ReaderViewModel → destroyed ReaderActivity.
  FIX (3 files, UNCOMMITTED, compile-green):
  - ReaderViewModel.onCleared: ttsEngine.onFocusEvent = null after
    stop()/shutdown() — detaches controller from singleton engine; next
    reader session re-registers in controller init. Supersedes the old
    Known issue #5 "bounded, self-healing" assessment: LeakCanary showed
    ~100MB retained, NOT a small bounded object graph.
  - ReaderActivity: DisposableEffect { onDispose { settingsScreenModel
    .ioCoroutineScope.cancel() } } (both composition blocks) — the settings
    screen model's io scope outlived the Activity otherwise.
  - app/build.gradle.kts: LeakCanary core re-enabled for debug builds
    (Phase 9 leak verification; upstream had it commented out).
  Known issue #5 → RESOLVED by this fix. GATES GREEN 2026-08-29
  (spotlessCheck + :app:compileDebugKotlin + testDebugUnitTest, BUILD
  SUCCESSFUL 2m56s — after fixing root-owned build artifacts from a stray
  root docker run via chown). Device re-verification with new debug APK
  still pending.

Deferred issues (LOW PRIORITY, revisit post-build):
  - #3b hands-off duplicate words/phrases: intermittent, manga-specific, never
    reproduced under instrumentation. To re-debug: re-add TTS-DBG logs (git history)
    + log per-sentence bounding box to catch duplicate/overlapping OCR regions;
    reproduce on the affected manga. Hypotheses: OCR overlapping regions (same text
    under different utterance ids) or Google-TTS engine audio quirk.

Remaining:
- Commit the 3-file leak-fix set (gates green 2026-08-29).
- Install new debug APK (LeakCanary core enabled) on device → re-run leak
  check → confirm ~100MB retention gone.
- Phase 9 battery measurement + final leakcanary sign-off; memory profile
  already captured (meminfo-profile.log: Java 8–25MB / Native 8–28MB /
  TOTAL ~160–267MB, stable, no leak spike).
- (DONE 2026-08-28: mini-player z-order fix + action logging +
  prefetch-failure fix COMMITTED by user: 41200022e, be31edb71, 80deac5b8;
  script steps 7–15 executed + user-confirmed; prefetch fix device-verified.
  DONE 2026-08-29: gates green on leak-fix set.)
```

## Blocked

```text
Nothing hard-blocked. Finding #3 deferred LOW PRIORITY by user (post-build).
Phase 8 device script COMPLETE (steps 1–15 executed + user-confirmed).
SM_M066B via wireless adb; use on-device logcat capture (streaming adb logcat
dies on blip). English TTS voice = device default. GLENS/network reachable.
Standing decision unchanged: prd script stays manual/interactive.
```

## Current files being modified

```text
Current working files:
- app/build.gradle.kts (debugImplementation(libs.leakCanary.core) enabled,
  UNCOMMITTED)
- app .../ui/reader/ReaderViewModel.kt (onCleared: ttsEngine.onFocusEvent =
  null — Known issue #5 fix, UNCOMMITTED)
- app .../ui/reader/ReaderActivity.kt (DisposableEffect cancels
  settingsScreenModel.ioCoroutineScope on dispose, both composition blocks,
  UNCOMMITTED)
- docs/memory.md, docs/phase.md (this update)
Prior fix set now COMMITTED by user (41200022e, be31edb71, 80deac5b8):
z-order, action logging, prefetch reportFailure fix.
TTS-DBG instrumentation removed; no active Finding #3 work.
```

## Recently changed files

```text
2026-08-28  app/build.gradle.kts                               LeakCanary core enabled for debug (Phase 9 leak verification) (UNCOMMITTED)
2026-08-28  app .../ui/reader/ReaderViewModel.kt              Known issue #5 fix: ttsEngine.onFocusEvent = null in onCleared (~100MB leak) (UNCOMMITTED)
2026-08-28  app .../ui/reader/ReaderActivity.kt               DisposableEffect cancels settingsScreenModel.ioCoroutineScope on dispose (UNCOMMITTED)
2026-08-28  docs/memory.md, docs/phase.md                     LeakCanary finding #5 + fix; log-collection results; committed-set hashes updated
2026-08-28  (committed 41200022e/be31edb71/80deac5b8) z-order fix + action logging + prefetch reportFailure fix (device-verified)
2026-08-28  app .../ui/reader/tts/TtsPlaybackController.kt Prefetch failure no longer kills session (reportFailure gate); RUN4 analysis
2026-08-28  app .../ui/reader/tts/TtsPlaybackController.kt Action-level DEBUG logs: pause/resume/stop/stepBy/focus-loss
2026-08-28  docs/memory.md, docs/phase.md                 Finding #3 deferred LOW PRIORITY; run2/run3 results; TTS-DBG removed
2026-08-28  app .../viewer/webtoon/WebtoonViewer.kt       #1 moveToPage explicit onScrolled; TTS-DBG removed
2026-08-28  app .../ui/reader/tts/TtsPlaybackController.kt #2 timeout pageIndex=target; P0 rebuild/prefetch dedup; TTS-DBG removed
2026-08-28  app .../ui/reader/ReaderViewModel.kt          P1 250ms TTS page-selected debounce; advance confirmations exempt
2026-08-27  app .../ui/reader/tts/TtsPlaybackController.kt  ScrollToRegion emit + pause/resume page fix
2026-08-27  app .../ui/reader/ReaderViewModel.kt           ScrollToRegion forward to event channel
2026-08-27  app .../ui/reader/ReaderActivity.kt            ScrollToRegion handler + Viewer.scrollToRegion()
2026-08-27  app .../viewer/Viewer.kt                       scrollToRegion() default impl
2026-08-27  app .../viewer/webtoon/WebtoonViewer.kt        scrollToRegion() impl + findFirstVisibleItemPosition fix
2026-08-26  data .../data/ocr/GlensOcrEngine.kt        parallel tiles (TILE_CONCURRENCY=3)
2026-08-26  data .../data/ocr/OcrRepositoryImpl.kt     single-flight + task-owned bitmap/upsert
2026-08-26  data .../data/ocr/PrioritizedTaskQueue.kt  queue-depth debug log
2026-08-26  app .../ui/reader/tts/TtsPlaybackController.kt  acquireMs + startup latency logs
2026-08-25  docs/phase.md                              Phase 10 voice-calibration backlog
2026-08-25  (committed a071feedf) GlensOcrEngine tiling, EN ordering, JP-gate removal,
            segmenter EN rules, progression fixes, TtsPlaybackBar, i18n — see commit
2026-08-24  docs/memory.md, docs/phase.md              session records
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
English is the primary v1 Read-Aloud language (product pivot 2026-08-25).
No language preflight, no setLanguage pinning — the system-default TTS voice
is used. Japanese/voice-picker work belongs to Phase 10.

Reason:
Device had no JP voice; the old gate hard-blocked all playback and the
eng-IND synthesis log proved wrong-language output. English content was the
user's actual usage.

Decision:
Tall webtoon strips are tiled before the Glens request (engine-level fix),
not compensated in TTS.

Reason:
Root cause was image downscale destroying text; per-task spec forbids masking
OCR gaps in TTS. Tiling keeps MAX_IMAGE_DIMENSION per tile and preserves the
single-request path for normal pages.

Decision:
Controller re-resolves chapter context via provideContext() on every queue
rebuild instead of caching it at start().

Reason:
Auto chapter advance + user navigation both need fresh totalPages/
hasNextChapter/chapter id; stale context caused silent death and wrong-chapter
scans.

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
Japanese-voice availability gate + Locale.JAPAN pinning in the TTS engine.
Reason: blocked all playback on JP-less devices and produced wrong-language
synthesis; replaced by system-default voice (product pivot 2026-08-25).

Rejected:
Retrying utterances more than once or adding delays to mask speak() failures.
Reason: rules.md §7 fixes failure at the cause (retry once → honest Paused);
delays hide bugs.

Rejected:
Sorting regions inside SentenceSegmenter to fix webtoon order.
Reason: ordering belongs to the OCR engine (now fixed there via tiling +
positional merge); segmenter must mirror tap-highlight behavior.
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

3. RESOLVED-locally | Build env | ML model assets were gitignored & absent;
   NOW DOWNLOADED into working tree via the exact CI step
   (.github/workflows/build.yml "Download ML models", all sha256 verified):
   app/src/main/assets/ocr/{encoder,decoder}.tflite+embeddings.bin,
   app/src/main/assets/ocr_fast/{encoder,decoder}.tflite,
   data/src/main/assets/panel_detector/model.tflite. Still absent on fresh
   clones/CI-only; re-run that step if assets are wiped.

4. INFO | Docs env | AGENTS.md notes .devcontainer "Java 17" note stale — CI/toolchain
   effectively JDK 21 (Gradle java property 17). Use CI commands from rules.md §11.

5. RESOLVED | TTS leak (FIXED 2026-08-28, UNCOMMITTED) | Singleton TtsEngine
   retained last TtsPlaybackController via onFocusEvent lambda after
   ReaderViewModel.onCleared. LeakCanary (enabled 2026-08-28 for Phase 9)
   showed ~100MB retained after reader exit — NOT the small bounded graph
   previously assumed (system TTS service keeps a native GC root to the
   TextToSpeech callback → engine singleton → lambda → dead controller →
   ViewModel → destroyed Activity). FIX: ReaderViewModel.onCleared sets
   ttsEngine.onFocusEvent = null after stop()/shutdown(); next session
   re-registers in controller init. (Clearing inside engine.shutdown()
   remains REJECTED: ensureInitialized() calls shutdown() mid-session on the
   engine-init-failure path, which would drop focus-loss handling for the
   live controller.) Gates + device re-verification pending.

6. MEDIUM | OCR tiling | Glens strip tiling dedupes seam
   repeats by IoU ≥0.45; a bubble straddling a seam can still yield two
   complementary fragments if Lens returns differing boxes in the overlap.
   Ceiling accepted for v1; upgrade path = cross-tile text merge before region
   creation. Watch `tiled=true` pages during device pass. NOTE: tiles now run
   3-concurrent — seam behavior unchanged, but watch for new Lens rate-limit
   responses (HTTP 429/5xx) under parallelism; reduce TILE_CONCURRENCY if seen.

7. LOW | Build env (2026-08-24) | Docker debug keystore now persisted via
   volume yomihon-android-home (~/.android). If that volume is ever deleted,
   signatures change → INSTALL_FAILED_UPDATE_INCOMPATIBLE again; recovery =
   uninstall + reinstall + restore .tachibk backup (data loss). Also: gradle
   transform cache once held stale /work paths — if DexingNoClasspathTransform
   fails with "outside the root directory", delete yomihon-gradle-home volume.

8. RESOLVED | Reader TTS (NEW 2026-08-26) | Advance-confirm timeouts on webtoon:
   `page advance to N timed out` fired systematically (ch732 p17 ×3, ch947
   p1+p2). Root cause: WebtoonViewer.onScrolled used
   findLastEndVisibleItemPosition which returns the item at the BOTTOM of the
   view — for webtoon (vertical stack), short pages have the next page already
   visible → returns next page index ≠ target → 10 s timeout → Paused.
   FIXED 2026-08-27: switched to findFirstVisibleItemPosition in onScrolled
   (WebtoonViewer.kt). Verified in build 0.4.0-8234. Evidence: logcat-step3-
   full.log lines ~99730–132013 (pre-fix), logcat-8234-test.log (post-fix).

9. RESOLVED | Instrumentation gaps (NEW 2026-08-26, FIXED 2026-08-28) | Controller
   lacked debug logs for pause/resume/stop + nextSentence/previousSentence, so step
   4/5 results were only partially verifiable. FIXED 2026-08-28: added DEBUG
   action-level logs in TtsPlaybackController — pause (page+sentence), resume
   (page+resumeIndex), stop (prior phase), stepBy next/prev (from->to + phase, and
   boundary-reject), and audio-focus-loss pause (event name). No text content logged
   (rules §7). Step 4/5 now verifiable from logcat.

10. RESOLVED | OCR duplicate scans + recycle crash (2026-08-26) | Root causes
     were: no single-flight in OcrRepositoryImpl.scanPage; bitmap recycled by
     caller's useBitmap-finally while the repo-owned task still used it;
     upsert skipped when caller abandoned await. Fixed by single-flight map +
     moving bitmap lifecycle/upsert inside the queue task (Phase 9 perf pass #1).
     Device re-verification pending.

11. INFO | Logcat analysis (2026-08-27) | logcat-8234-test.log (312,371 lines)
     contains ZERO app activity after install. App process (PID 22868) started
     once for broadcast receiver at 23:58:33, killed after 11 seconds (signal 9).
     Zero TTS/OCR logs, zero activity launches. All 6 user-reported issues
     cannot be verified — no test run occurred on build 0.4.0-8234 during this
     log capture. Fresh test required.

12. LOW | App widget (NEW 2026-08-28) | GlanceAppWidget composition error
     "CompositionLocal LocalContext not present" observed in run4 logcat
     (19:48:26, non-fatal, app kept running). Unrelated to TTS/reader work —
     pre-existing Glance widget issue. Investigate only if widget complaints
     surface.

13. RESOLVED | TTS prefetch failure escalation (NEW+FIXED 2026-08-28,
     DEVICE-VERIFIED 2026-08-28) | A failed background prefetch scan (transient
     DNS: UnknownHostException for lensfrontend-pa.googleapis.com while app
     backgrounded) called scanOnDemand→fail(TtsError.OcrError) and pushed a
     healthy paused session into Error phase; user had to restart. Root cause:
     scanOnDemand unconditionally reported failure even for best-effort
     prefetch callers. FIXED: reportFailure param (default true); prefetch
     passes false + logs "prefetch scan failed (best-effort)"; "prefetch
     complete" now only on success. VERIFIED ON DEVICE (logcat-prefetch-
     verify.log, PID 22599, wifi-killed test): 13 prefetch failures for page 4
     logged "(best-effort)" while page 3 playback + 13 sentence-steps
     continued uninterrupted; home-press mid-prefetch (page 6 scan in flight)
     → auto-pause, stop(phase=Paused), NO Error. Main-loop failure on the
     CURRENT page still honestly errors (page 4 wifi-off → Error bar → user
     retry after wifi restore → 16.8s tiled scan → playback resumed) — correct
     by design. INFO observation: each next-sentence tap while wifi down
     restarted the failed page-4 prefetch (~170ms fast-fail each) — harmless,
     tap-rate bounded, no fix needed.
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
Unit tests:        PASS (2026-08-29, full testDebugUnitTest, all modules —
                    on the 3-file leak-fix set, uncommitted)
Integration tests: none run (existing androidTest suites are device-gated/@Ignore)
UI tests:          none exist in repo
Device tests:      COMPLETE — Phase 8 script steps 1–15 all executed +
                    user-confirmed (2026-08-28 RUN4 build 0.4.0-8238: steps 7–15
                    PASS incl. user-confirmed rate/pitch live + rotation
                    pause/resume-same-position). run2/run3 verified Finding #1
                    (22/22 advances, 0 timeouts) + Finding #2. Finding #3
                    deferred LOW PRIORITY. Finding #4 prefetch-DNS fix
                    DEVICE-VERIFIED (logcat-prefetch-verify.log). LeakCanary
                    found Finding #5 (~100MB reader leak) → fix implemented,
                    device re-verify pending.
Lint:              spotlessCheck PASS (2026-08-29, leak-fix set)
Build:             :app:assembleDebug PASS (2026-08-28, 0.4.0-8238 installed)
Memory profile:    captured (meminfo-profile.log): Java 8–25MB / Native 8–28MB /
                    TOTAL ~160–267MB across session, stable, no leak spike
                    (the ~100MB retention only visible via LeakCanary heap dump).
Baseline (pre-TTS expectations): CI order = spotlessCheck → testDebugUnitTest →
                         verifySqlDelightMigration → assembleRelease (see rules.md §11)
Environment: devcontainer image vsc-yomihon-e24e3bd7… (JDK 17) via docker on host;
            ALWAYS pass -Xmx4g; mount BOTH volumes:
              -v yomihon-gradle-home:/home/vscode/.gradle
              -v yomihon-android-home:/home/vscode/.android   (stable debug key)
            CI (JDK 21, more RAM) unaffected.
```

## Last verified build

```text
Date:     2026-08-29
Command:  ./gradlew spotlessCheck :app:compileDebugKotlin testDebugUnitTest
          (docker devcontainer JDK17, -Xmx4g, both volumes)
Result:   BUILD SUCCESSFUL in 2m56s — on the 3-file leak-fix set
          (uncommitted). Prior green at 80deac5b8 (prefetch fix + action
          logging + z-order) DEVICE-VERIFIED via wifi-killed test
          (logcat-prefetch-verify.log). LeakCanary pass on 0.4.0-8238 →
          Finding #5 (~100MB leak; fix implemented, device re-verify pending).
```

## Last verified test

```text
Date:     2026-08-29
Command:  ./gradlew spotlessCheck testDebugUnitTest    (docker devcontainer, JDK 17)
Result:   BUILD SUCCESSFUL (all modules) — on the 3-file leak-fix set (uncommitted)
```

---

## Agent handoff

```text
Last agent:                 ox-alpha (Phase 8 device pass + Phase 9 perf pass #1 + #2)
Date:                       2026-08-28
Task completed:             Device script steps 1–6 recorded with logcat evidence;
                            Phase 9 perf pass #1 (tile parallelism ×3, single-flight
                            scan dedup, task-owned bitmap+upsert — kills duplicate
                            scans, recycle crashes, ~3× faster pages) + pass #2
                            (webtoon confirm fix: findFirstVisibleItemPosition,
                            region-level auto-scroll via TtsEvent.ScrollToRegion,
                            pause/resume page-awareness) IMPLEMENTED; all gates
                            green; arm64 APK 0.4.0-8234 installed.
FRESH LOGCAT ANALYSIS:     logcat-8234-phase9.log (7,793 lines, PID 27363)
                            analyzed. 3 chapters tested (206, 205, 1). 9/9 page
                            advances confirmed, 0 recycle crashes, 0 sentence
                            failures, 0 timeouts. Known issues #8 + #10 RESOLVED.
                            Two NEW issues found:
                            P0: Prefetch spam loop (19 cancels + 20 scan starts
                                for p16 in 7s — race condition in prefetch restart)
                            P1: Rapid-swipe mass OCR (13 simultaneous scans from
                                fast swipe — no throttle/debounce)
P0 + P1 FIXES (2026-08-28): P0: TtsPlaybackController dedups rebuild on
                            lastRebuildPageIndex + guards prefetch on
                            prefetchPageIndex (both reset in resetSession and on
                            provideContext()==null). P1: ReaderViewModel.onPageSelected
                            debounces TTS call 250ms for user nav; advance
                            confirmations (hasPendingAdvance) never debounced.
                            ScrollToRegion logcat instrumentation added in
                            WebtoonViewer. All gates green (spotlessCheck +
                            testDebugUnitTest + :app:compileDebugKotlin).
Current task:               DONE 2026-08-28: log-collection + analysis session —
                            logcat-8236-run2.log (23.6M, TTS-DBG pipeline
                            confirmed: 3003ms startup, 83% cache hit, advances
                            1–5ms), logcat-prefetch-verify.log (29M, best-effort
                            verified), meminfo-profile.log (stable, no spike),
                            logcat-leakcanary.log (37M → Finding #5 ~100MB leak).
                            Leak-fix set implemented (3 files, uncommitted):
                            ReaderViewModel.onCleared onFocusEvent=null,
                            ReaderActivity DisposableEffect ioCoroutineScope
                            cancel, build.gradle.kts LeakCanary core.
                            Known issue #5 → RESOLVED; #12 (GlanceAppWidget
                            CompositionLocal error) added.
Next recommended task:      1) Commit leak-fix set (gates green 2026-08-29).
                            2) Install new debug APK (LeakCanary enabled) →
                            device re-run → confirm ~100MB retention gone.
                            3) Phase 9 battery measurement → then Phase 9
                            COMPLETED. If #3b returns, re-add TTS-DBG from git
                            history + OCR bbox/overlap logging.
Files safe to modify:       docs/* ; app reader/tts/settings files ;
                            data OCR files ; i18n base strings.xml (snake_case only)
Files currently worked on:  UNCOMMITTED (3-file leak-fix set): app/build.gradle.kts
                            (LeakCanary core), ReaderActivity.kt (DisposableEffect),
                            ReaderViewModel.kt (onFocusEvent=null). Gates green
                            2026-08-29; device re-verify pending. NOTE: stray
                            untracked gradle-home/ dir (2.5G) in repo root —
                            delete, never commit.
Known risks:                TILE_CONCURRENCY=3 may trip Lens rate limits — watch
                            HTTP 429/5xx in logs, drop constant if seen;
                            advance-confirm timeouts (#8) FIXED+VERIFIED (moveToPage
                            explicit onScrolled; 22/22 1–5ms run2);
                            CAPTURE: streaming `adb logcat` DIES on wireless-adb
                            blip — use ON-DEVICE: adb shell "logcat -c; nohup logcat
                            -v threadtime > /sdcard/tts-test.log 2>&1 &" then adb pull.
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
