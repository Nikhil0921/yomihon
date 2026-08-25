# Yomihon — Project Memory (AI State File)

> LIVING DOCUMENT. Every coding agent MUST read this file before substantial work
> and MUST update it after meaningful implementation work (protocol at bottom).
> Companion documents: `prd.md` (WHAT) · `architecture.md` (HOW) · `rules.md`
> (MUST/MUST NOT) · `phase.md` (WHEN) · `design.md` (LOOK/FEEL).

---

## Current project state

```text
Project:        Yomihon (v0.4.0, vc25) — Android manga reader + OCR/language tooling
Repo state:     branch main @ 91dc9e7e7 + UNCOMMITTED Phase-8/9 stabilization
                change set (11 files — see Recently changed). Do not amend user
                commits; .opencode/ stays untouched.
Untracked:      .opencode/ (tool config)
Primary goal:   Reliable Read-Aloud: English OCR → English system TTS →
                correct progression (PRODUCT PIVOT 2026-08-25: English is the
                primary v1 language; Japanese TTS moved to Phase 10. The old
                Japanese-voice gate was REMOVED.)
Current phase:  Phase 8 device pass IN_PROGRESS on real hardware (SM_M066B,
                Android 16, arm64, wireless adb working); Phase 9 static DONE +
                reader-stabilization fixes landed this session.
Current status: All code for Phases 0–7 + Phase 8 unit + Phase 9 static +
                stabilization P0s COMPLETE and GREEN (spotlessCheck,
                testDebugUnitTest, :app:assembleDebug). Device script of
                prd.md §3.4(3)–(5) is the ONLY remaining item.
TTS code:       Same layout as before; segmenter now 15 tests incl. English
                period rules; controller takes provideContext() lambda;
                NoJapaneseVoice error no longer exists.
```

## Current objective

Stabilize the read-aloud pipeline end-to-end for ENGLISH content per the
2026-08-25 product decision: OCR must find actual dialogue (incl. long
webtoon strips), text must be spoken completely without silent skips, and
page/chapter progression must advance exactly once with user navigation
authoritative. Device verification of prd.md §3.4(3)–(5) pending.

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

## In progress

```text
Feature: Phase 8 device pass — English-content verification of the stabilized
         build (0.4.0-8232) on SM_M066B.

Done: all code + gates (see 2026-08-25 entry).

Remaining on device:
- prd §3.4(3): play GLENS-scanned chapter, uncached page scan, webtoon strip
  (tiling), play/pause/resume, prev/next sentence, swipe arbitration,
  auto page turn ×1, chapter transition, end-of-content stop, rate/pitch live.
  NOTE: "missing-Japanese-voice error" branch is OBSOLETE (English primary).
- prd §3.4(4) lifecycle matrix: onStop pause, rotation continues, clean exit,
  audio-focus transient loss.
- prd §3.4(5): exit-to-idle <1s; no background CPU after exit.
- Phase 9 measurements: memory profile, leakcanary, battery.

User must restore backup after the reinstall:
Settings → Backup/restore → app.yomihon.dev_2026-08-24_23-44.tachibk
```

## Blocked

```text
Nothing hard-blocked. Device pass needs the USER driving the phone screen:
SM_M066B connected via wireless adb (port rotates when Wireless debugging is
toggled → reconnect with fresh IP:port or USB). English TTS voice = device
default (Google TTS en-IN present). GLENS/network reachable on device.
Standing decision unchanged: prd script stays manual/interactive.
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
2026-08-25  data .../data/ocr/GlensOcrEngine.kt            strip tiling + EN ordering (P0)
2026-08-25  domain .../domain/tts/engine/TtsEngine.kt      japaneseAvailable removed
2026-08-25  app .../data/tts/AndroidTtsEngine.kt           JP gate/locale removed
2026-08-25  app .../ui/reader/tts/TtsPlaybackController.kt progression+retry+logging
2026-08-25  app .../ui/reader/ReaderViewModel.kt           buildTtsChapterContext provider
2026-08-25  app .../ui/reader/ReaderActivity.kt            loadNextChapter guard + error branch
2026-08-25  app .../presentation/reader/TtsPlaybackBar.kt  padding + retry-always
2026-08-25  domain .../domain/tts/SentenceSegmenter.kt     English '.' rules + trim
2026-08-25  domain/src/test/.../SentenceSegmenterTest.kt   15 cases
2026-08-25  i18n .../base/strings.xml                      tts_error_no_japanese_voice removed
2026-08-24  docs/memory.md, docs/phase.md                  session records
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

5. LOW | TTS | Singleton TtsEngine retains last TtsPlaybackController via
   onFocusEvent lambda after ReaderViewModel.onCleared until next reader session
   re-registers (new controller init) — bounded, self-healing retention of a
   small object graph with dead (cancelled) scope. Clearing it inside
   engine.shutdown() was REJECTED: ensureInitialized() calls shutdown() on the
   engine-init-failure path mid-session, which would permanently drop focus-loss
   handling for that controller. Revisit if leakcanary (Phase 9 device pass)
   flags more than the controller itself.

6. MEDIUM | OCR tiling (NEW 2026-08-25) | Glens strip tiling dedupes seam
   repeats by IoU ≥0.45; a bubble straddling a seam can still yield two
   complementary fragments if Lens returns differing boxes in the overlap.
   Ceiling accepted for v1; upgrade path = cross-tile text merge before region
   creation. Watch `tiled=true` pages during device pass.

7. LOW | Build env (2026-08-24) | Docker debug keystore now persisted via
   volume yomihon-android-home (~/.android). If that volume is ever deleted,
   signatures change → INSTALL_FAILED_UPDATE_INCOMPATIBLE again; recovery =
   uninstall + reinstall + restore .tachibk backup (data loss). Also: gradle
   transform cache once held stale /work paths — if DexingNoClasspathTransform
   fails with "outside the root directory", delete yomihon-gradle-home volume.
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
Unit tests:        PASS (2026-08-25, full testDebugUnitTest, all modules;
                   SentenceSegmenterTest now 15 cases)
Integration tests: none run (existing androidTest suites are device-gated/@Ignore)
UI tests:          none exist in repo
Device tests:      PENDING — build 0.4.0-8232 installed on SM_M066B; prd
                   §3.4(3)–(5) script to be executed (English content)
Lint:              spotlessCheck PASS (2026-08-25)
Build:             :app:assembleDebug PASS (2026-08-25, full run 19m8s cold)
Baseline (pre-TTS expectations): CI order = spotlessCheck → testDebugUnitTest →
                        verifySqlDelightMigration → assembleRelease (see rules.md §11)
Previous verified build:
Date:     2026-08-23
Command:  ./gradlew spotlessCheck testDebugUnitTest :app:assembleDebug
Result:   ALL GREEN (predates stabilization change set)
Environment: devcontainer image vsc-yomihon (JDK 17) run via docker on host;
            ALWAYS pass -Xmx4g (repo-default 2560m OOMs in the 7.4 GiB container).
            Mount BOTH persistent volumes:
              -v yomihon-gradle-home:/home/vscode/.gradle
              -v yomihon-android-home:/home/vscode/.android   (stable debug key)
            CI (JDK 21, more RAM) unaffected.
```

## Last verified build

```text
Date:     2026-08-25 (this session)
Command:  ./gradlew testDebugUnitTest :app:assembleDebug
          (docker devcontainer, JDK 17, -Xmx4g, both volumes mounted;
          gradle-home volume recreated fresh after cache poisoning)
Result:   BUILD SUCCESSFUL in 19m8s — EXIT:0. APK installed to device
          (adb install Success), versionName 0.4.0-8232.
Earlier same session: spotlessCheck + :domain:testDebugUnitTest GREEN.
```

## Last verified test

```text
Date:     2026-08-25 (this session)
Command:  ./gradlew testDebugUnitTest                    (docker devcontainer, JDK 17)
Result:   BUILD SUCCESSFUL (all modules; segmenter 15 cases incl. EN rules)
```

---

## Agent handoff

```text
Last agent:                 ox-alpha (reader-stabilization session, product pivot)
Date:                       2026-08-25
Task completed:             English-primary pivot implemented: Glens strip
                            tiling + EN ordering (P0), JP gate/locale removal
                            (P0), segmenter EN period rules + tests (P0),
                            progression fixes — reconcile-on-mismatch,
                            retry-once-then-Pause, fresh-context provider,
                            chapter-load guard (P0); debug logging; bar polish.
                            Gates green; build 0.4.0-8232 installed on device.
Current task:               Phase 8 device pass on SM_M066B (English content).
Next recommended task:      Run prd §3.4(3)–(5) script with user driving the
                            phone (restore .tachibk backup first!), then Phase 9
                            measurements (memory/battery/leakcanary/exit-to-idle).
                            Then commit the stabilization change set.
Files safe to modify:       docs/* ; app reader/tts/settings files ;
                            i18n base strings.xml (snake_case keys only)
Files currently worked on:  the 11-file uncommitted change set listed in
                            Recently changed files (all compile-green)
Known risks:                device pass may surface OCR-quality follow-ups
                            (tiling seam duplicates, Known issue #6);
                            ReaderActivity dual-composition quirk (#2) when
                            touching overlays; do not touch user's AGENTS.md or
                            .opencode/; always -Xmx4g + both docker volumes;
                            adb wireless port rotates — reconnect via fresh
                            IP:port.
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
