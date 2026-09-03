# Yomihon — Project Memory (AI State File)

> LIVING DOCUMENT. Every coding agent MUST read this file before substantial work
> and MUST update it after meaningful implementation work (protocol at bottom).
> Companion documents: `prd.md` (WHAT) · `architecture.md` (HOW) · `rules.md`
> (MUST/MUST NOT) · `phase.md` (WHEN) · `design.md` (LOOK/FEEL).

---

## Current project state

```text
Project:        Yomihon fork (v0.5.1, vc27) — Android manga reader + OCR/language tooling
Repo state:     branch main @ b8858545a + UNCOMMITTED multi-phase feature set
                (2026-09-01, see "[COMPLETED 2026-09-01 — Phases A–I]" block).
                v0.5.1 RELEASE PUBLISHED 2026-08-31 (tag v0.5.1, 5 ABI APKs).
Untracked:      .opencode/ + .device-pass/ (gitignored), .codegraph/ (index,
                gitignored)
Primary goal:   Multi-phase roadmap: intelligent speech cleanup, region
                classification, OCR exclusion zones, voice profiles, 3x rate,
                dictionary nav cleanup, Feed feature
Current phase:  2026-09-01 session — Phases A–I implemented, all gates green
                (spotlessCheck + testDebugUnitTest + verifySqlDelightMigration
                + :app:assembleDebug), UNCOMMITTED, device pass NOT run.
Current status: New DB migration 18.sqm verified; TTS regression surface
                untouched (speak/advance/pause paths intact, pipeline only
                filters regions before segmentation).
```

## Current objective

Stabilize the read-aloud pipeline end-to-end for ENGLISH content per the
2026-08-25 product decision: OCR must find actual dialogue (incl. long
webtoon strips), text must be spoken completely without silent skips, and
page/chapter progression must advance exactly once with user navigation
authoritative. Device verification of prd.md §3.4(3)–(5) DONE (script steps
1–15 executed + user-confirmed 2026-08-28). Phase 9 COMPLETE 2026-08-29:
leakcanary sign-off (0 application leaks after two reader TTS sessions on
0.4.0-8241), memory profile captured (meminfo-profile.log: stable), and
battery measurement done (battery-sample.log + batterystats-app.txt:
TTS+OCR session ~365mA avg device drain incl. screen, app total 115mAh
attributed over 1h on-battery window, ZERO post-exit background drain —
app reaped+frozen, no wakelocks after reader exit).

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

```text
[COMPLETED 2026-09-02 — OCR exclusion device-test regression fixes + auto-detect, UNCOMMITTED]

Device test (user, build from 1b810ccde audit set) found: toggle no-op in BOTH
management UIs, WORD/PHRASE rules not excluded on fresh OCR pages. Evidence
phase: 2 parallel explore agents traced full pipelines against source (no fresh
device logs existed; newest .device-pass log = 2026-08-31). Root causes proven
in code, DISTINCT for the two bugs:

Issue A (toggle dead) — ROOT CAUSE: positional-argument swap in
OcrExclusionZoneRepositoryImpl.setEnabled. .sq writes "SET enabled=:enabled
WHERE _id=:id" → SQLDelight generates (enabled, id); repo called (id, enabled)
positionally, both Long — silent swap. OFF → "WHERE _id=0" = 0 rows (no
autoincrement row has id 0); ON → "SET enabled=<rowId> WHERE _id=1" = corrupts
row 1, never touches tapped row. DELETE worked because deleteZone is
single-param (unswappable). Toggle wiring (both UIs → interactor → repo) was
fully correct end-to-end. FIX: named args at call site (compile-checked
forever). NOTE: device DB may have row _id=1 re-enabled by the ON-tap side
effect — user should re-toggle row 1 to intended state.

Issue B (WORD/PHRASE no-op on fresh pages) — wiring verified CORRECT: rules
re-queried per page (TtsPlaybackController.acquireSentences:496 via
awaitForSpeech), zonesForSpeech SQL includes global WORD/PHRASE, matcher has no
rect requirement for them, cache stores RAW regions + playback-time filtering
(cached pages DO get new exclusions on next acquire). Failure was MATCH
SEMANTICS only, 3 causes all in OcrExclusionMatcher:
  1. WORD needle never tokenized: "K-manga.com" contains -/. → pure
     isLetterOrDigit tokens can never equal it = impossible rule; "keymanga"
     missed OCR variants ("Key Manga").
  2. PHRASE collapsed whitespace to single space but kept it: OCR emits
     "Discord. gg / AsuraScans" → needle "discord.gg/asurascans" (0 spaces)
     never substring-matches.
  3. JP-mixed lines are full-width-converted by TextPostprocessor (half→full
     ASCII mapping); matcher lowercase() does not fold width → ｋｅｙｍａｎｇａ ≠
     keymanga.
FIX (OcrExclusionMatcher rework): all text comparisons NFKC-normalize (folds
full-width→half). WORD = rule-token CONCATENATION must equal concat of a
consecutive run of region tokens (single-token "ion" still ≠ "combination";
"K-manga.com" ≡ [k,manga,com] runs; "KeyManga" ≡ "Key Manga"). PHRASE =
NFKC-fold + lowercase + strip ALL whitespace + substring (URL/domain noise-
proof). URL-like input therefore behaves predictably in EITHER type (documented
choice: robust matching in both, no UI forced-conversion).

Manage-sheet visibility landmine (agent-found): reader "Manage exclusion zones"
used zonesForManga WHERE manga_id=:id → global WORD/PHRASE rows (manga_id=0)
invisible there = user thinks rule missing. FIX: zonesForManga now
manga_id=:mangaId OR source_id=:sourceId OR match_type IN (WORD,PHRASE);
subscribeForManga(mangaId, sourceId) signature ripple (repo+interactor+VM).
Settings screen unchanged (subscribeAll already).

Diagnostics (rules §7 — no text logged): acquireSentences now logs
"TTS page=N exclusion rules=X types={WORD=1,...} excluded=A/B" per page when
rules exist.

Issue D (auto-detect selection text) — implemented per spec, cached-first:
captureExclusionZoneSelection resolves normalized rect → VM
detectExclusionZoneText (cached OcrPageResult regions intersecting selection
via boxesOverlap, sorted by order, joined \n — NO scan) → miss → targeted crop
OCR (Bitmap.createBitmap of selection rect only, existing ocrProcessor.getText
HIGH-priority queue path, no full-page scan, bitmap recycled in VM finally) →
Dialog.ExclusionZoneScope gains detectedText → ExclusionZoneScopeDialog
pre-fills match text field (multiline, editable, clearable; cancel+re-select =
re-detect). User confirms before any rule is saved (dialog flow unchanged).
rejoin: detection runs while dialog opens? NO — detection runs BEFORE dialog
opens (launchIO), dialog opens with result; null detection = empty field,
manual entry.

Tests: OcrExclusionMatcherTest extended 14→20 cases: keymanga all-cases +
trailing punct, punctuated rule tokenized ("K-manga.com"), separator variants
("KeyManga"≡"Key Manga"), consecutive-run requirement, full-width fold WORD +
PHRASE, URL-like OCR spacing noise ×3 variants, (existing ion/combination
standalone-token case kept).

GATES GREEN 2026-09-02 (devcontainer JDK17, -Xmx4g, both volumes):
  spotlessCheck + testDebugUnitTest + verifySqlDelightMigration BUILD
  SUCCESSFUL 3m39s (matcher 20/20); :app:assembleDebug BUILD SUCCESSFUL 3m20s.
  No DB schema change (query-only edit to zonesForManga — verifySqlDelight
  Migration still run + green).

Files changed (10): OcrExclusionMatcher.kt, OcrExclusionMatcherTest.kt,
  OcrExclusionZoneRepositoryImpl.kt, ocr_exclusion_zones.sq (zonesForManga
  query), OcrExclusionZoneInteractors.kt, OcrExclusionZoneRepository.kt,
  ReaderViewModel.kt (subscribeForManga call + detect/crop fns + dialog field),
  ReaderActivity.kt (capture flow + dialog args), OcrExclusionZoneDialogs.kt
  (pre-fill), TtsPlaybackController.kt (diagnostics log).
```

```text
[COMPLETED 2026-09-03 — OCR exclusion regression #2: diagnostic-first fix set, UNCOMMITTED]

Device evidence FIRST (on-device capture .device-pass/ocr-excl-snapshot1.log,
PID 20038, 11:00–11:37, build 0.5.1-8254):
- 11:01:36 "OCR(legacy) Runtime: recognizeText total time: 5715 ms" during
  exclusion auto-detect → crop OCR ran the JP-vocab LegacyOcrEngine on an
  English selection (device DB rule _id=14 = COMBINED with saved JP garbage
  text — user-confirmed garbage insert). 36 tokens of kana/kanji output.
- 11:03:05 "TTS page=0 exclusion rules=2 types={PHRASE=1, COMBINED=1}
  excluded=0/16" → exclusion MISS with rules loaded (matcher semantics).
- 11:03:35 ReaderActivity destroy → LeakCanary "Found 2 objects retained,
  app is not visible" → 11:06 heap dump (12.3s freeze, all threads stopped)
  → analysis 131.5s @ ~100% CPU on WorkManager thread. Leak trace:
  WebtoonTransitionHolder retained 184.7MB → ReaderChapter.stateFlow slot →
  ScopeCoroutine collect → holder.itemView LinearLayout.mContext → destroyed
  ReaderActivity (97.7kB, 2691 objects). Root: holder's stateJob (own
  MainScope) cancelled ONLY in recycle(); RecyclerView never recycles holders
  on activity destroy → collector stayed registered in the chapter's StateFlow
  slot. PagerTransitionHolder has onDetachedFromWindow cancel; webtoon one
  didn't. Same family as 2026-08-30 WebtoonPageHolder 49.9MB flag.
- Device DB dump (python sqlite3): 3 rules; enabled values all clean 0/1
  (B-RC1 corrupt-enabled residue from old toggle bug NOT present on device;
  zonesForSpeech enabled=1 predicate safe here). Rule 10 COMBINED disabled
  (toggle fix works), rule 13 PHRASE enabled, rule 14 = the JP-garbage rule.

3 parallel explore subagents (A auto-detect pipeline, B exclusion/TTS, C perf)
+ orchestrator source verification. CONFIRMED root causes and fixes:

Fix 1 (A-RC1, garbage crops): OcrRepositoryImpl.recognizeText called
  recognizeWithFallback(selectedEngineType()) — LEGACY default pref routed
  arbitrary English crops into LegacyOcrEngine (JP vocab, non-uniform 224×224
  stretch). Scan path NEVER hits LEGACY directly (detection stub throws →
  Glens redirect) but recognizeText did. FIX: LEGACY/FAST text-recognition
  redirects to GLENS in recognizeText (mirrors scanLocalOrFallback), with
  ponytail: note to drop when a real DetOcrEngine lands. Manual long-press OCR
  selection path (processOcrRegion) gets the same correction for free.

Fix 2 (A-RC2, outside-selection leaks): detectExclusionZoneText filtered
  cached regions by boxesOverlap (ANY intersection ≥1px) → neighboring bubble
  text leaked into detected field. FIX: new pure fn boxMostlyInside(a, b,
  minCoverage=0.5f) in SpeechPipeline.kt (intersection ≥50% of REGION area);
  detectExclusionZoneText uses it. Selection-inside-huge-region now EXCLUDED
  (documented decision: merged-bubble box mostly outside selection = leak).
  BoxMostlyInsideTest 7 cases (contain/graze/disjoint/40%/75%/huge/degenerate).

Fix 3 (B-RC3, phrase punctuation asymmetry): phraseMatches kept punctuation —
  rule "discord gg" could never match OCR "discord.gg"; single ・ survived.
  FIX: phraseMatches first tries whitespace-stripped substring (as before),
  then falls back to TOKEN-CONCAT containment (normalizedTokens joined, both
  sides) — separator/punctuation tolerant in BOTH directions. New matcher
  tests 20→27: space-rule vs punct-OCR, punct-rule vs space-OCR, ・ separator,
  cross-region phrase NOT excluded (pins documented v1 per-region semantics),
  punctuation-only rule never matches, "Dis\ncord" word concat-run pinned.
  NOT fixed (documented): phrase split across TWO regions stays un-excluded
  (per-region matcher by design, v1); mid-page rule additions apply next page.

Fix 4 (C-RC1, 184.7MB leak): WebtoonTransitionHolder gained detach() =
  stateJob?.cancel(); WebtoonAdapter.onViewDetachedFromWindow calls it
  (RecyclerView.ViewHolder has no onDetachedFromWindow). detach also fires on
  mid-session recycling — same semantics as recycle(), next bind relaunches.

Fix 5 (C-A, detect job pile-up): captureExclusionZoneSelection now
  single-flight — exclusionDetectJob?.cancel() before relaunch (lifecycleScope
  cancels on destroy); obsolete detections no longer queue 5.7s OCRs behind
  each other. Sub-10px degenerate crops skip OCR entirely (honest empty field
  beats noise). Lazy-bitmap-open REJECTED during implementation: normalization
  needs file dims before cache lookup (chicken-egg with region decode) — not
  worth a bounds-only decoder helper.

Fix 5' (C-RC2, LeakCanary Toast config) DROPPED after API verification:
  IgnoredReferenceMatcher/referenceMatchers only filter leak-trace paths,
  do NOT prevent dumps of retained instances; AppWatcher exposes no
  per-class watcher filter. The REAL retained object was the (now-fixed)
  holder leak; Toast/PopupLayout dumps remain debug-build noise = Known
  issue #13 (unchanged). Debug sourceSet + manifest override reverted.

GATES GREEN 2026-09-03 (devcontainer JDK17, -Xmx4g, both volumes):
  spotlessCheck 35s; testDebugUnitTest + verifySqlDelightMigration BUILD
  SUCCESSFUL 2m36s (193 domain tests, matcher 27/27, BoxMostlyInside 7/7,
  one pre-existing test expectation of mine corrected before green);
  :app:assembleDebug BUILD SUCCESSFUL 2m44s. NO DB schema change.
  APK 0.5.1-8255 installed on SM_M066B 12:54; on-device verify capture
  /sdcard/ocr-excl-verify.log running (PID 24098).

Device verification checklist (Phase 7, pending user): A/B English selections,
C noisy area, D WORD excl, E PHRASE excl spacing/punct variant, F cached,
G fresh, H rapid re-select + rule CRUD, I long session lag watch,
J no playback regressions.

Files changed (7): OcrRepositoryImpl.kt (recognizeText redirect),
  SpeechPipeline.kt (+boxMostlyInside), ReaderViewModel.kt (filter + import),
  OcrExclusionMatcher.kt (phraseMatches token-concat), ReaderActivity.kt
  (single-flight detect + min-crop guard + Job import),
  WebtoonTransitionHolder.kt (+detach), WebtoonAdapter.kt
  (+onViewDetachedFromWindow), OcrExclusionMatcherTest.kt (+7),
  BoxMostlyInsideTest.kt (new, 7).
```

```text
[COMPLETED 2026-09-03 — P0 ZONE exclusion reliability fix (RC1/RC2/RC3), UNCOMMITTED]

User report: ZONE exclusion intermittent on device (WORD/PHRASE fine). 6
parallel explore agents + orchestrator verification + fresh device-log
evidence (ocr-excl-verify2.log, 63 exclusion decisions). Root causes:

RC1 (CONFIRMED, primary): zones drawn with CHAPTER/MANGA/SOURCE scope were
  FORCED to match_type=COMBINED (rect AND phraseMatches) by
  saveExclusionZone — user zones all became text-dependent rules whose text
  half broke on Glens re-clustering/noise (excluded=0/22 dominant; device
  logs show ONLY types={COMBINED=*}, never a pure ZONE rule). Fix:
  - Matcher ZONE semantics: all scopes pure-rect, PAGE-ANCHORED —
    matchesRegionScope: ZONE requires pageIndex!=null && (PAGE/CHAPTER:
    own chapterId; MANGA: mangaId; SOURCE: sourceId); matchesRegion rect
    still requires zone.pageIndex == context.pageIndex. CHAPTER-scope pure
    zone ≡ PAGE (documented; rect drawn on one page). Legacy rows
    (page_index NULL) dormant via pageIndex null-gate.
  - saveExclusionZone: matchType derived from text presence ONLY — blank
    text = pure ZONE for ANY scope; non-blank = COMBINED (opt-in).
  - ExclusionZoneScopeDialog: PAGE saves immediately (pure zone); wider
    scopes show OPTIONAL text field (supportingText "leave empty to always
    exclude"); Save no longer disabled on blank. Legacy detection now
    pageIndex==null (was scope!=PAGE — new wider-scope ZONE rules would
    have been mislabeled legacy).
  - SettingsOcrExclusionsScreen RuleRow: scope label shown for all
    non-PAGE scopes; rect shown for pageIndex!=null; legacy = null pageIndex.
RC2 (CONFIRMED conditional): zone normalized against DISPLAYED bitmap,
  OCR bboxes against ORIGINAL image — divergent exactly on dual-page
  split (x halved+offset) / rotateToFit (x/y transposed) / webtoon
  splitAndMerge pages → wrong rect, page-dependent = "intermittent".
  cropBorders + splitTallImages verified SAFE (both paths share base).
  Fix: captureExclusionZoneSelection now bounds-decodes the ORIGINAL
  page stream (page.stream, inJustDecodeBounds) and REJECTS zone creation
  with a clear error when displayed dims != original dims (honest failure
  beats silently-wrong rect). Full inverse-transform mapping deferred
  (documented follow-up if users need zones on split pages).
RC3 (diagnostics): controller acquireSentences now logs per-rule
  structural detail after the summary line: "OCR-ZONE rule=<id>
  type=<t> scope=<s> page=<pi> chapter=<cid> manga=<mid> rect=[l,t,r,b]
  enabled=<b>" — no text content (rules §7). Proves rule/scope/rect per
  page on next device pass.

CONFIRMED NON-CAUSES (agents + logs): all OCR engines emit normalized
  0..1 full-image bboxes (Glens single+tiled, OwOcr; Legacy/Fast never
  emit bboxes, redirected to Glens); cache roundtrip bit-exact; rules
  re-queried fresh per acquire (awaitAsList, no Flow race); matcher pure;
  same (chapter,page) always stable excluded A/B in logs (all A/B changes
  = chapter switches). ZonesForSpeech SQL unchanged (enabled=1 +
  text-rules-or-scope-match) — still correct for new ZONE semantics.

Tests: OcrExclusionMatcherTest 28→35: chapter-scope anchoring,
  manga/source same-page-index, null-pageIndex dormant, rect edge
  touching vs overlap vs spanning, degenerate rect, multi-zone/page.
  All prior WORD/PHRASE/COMBINED cases unchanged+green (COMBINED stays
  rect+text, opt-in).

GATES GREEN 2026-09-03 (devcontainer JDK17, -Xmx4g, both volumes):
  spotlessApply+spotlessCheck; :domain:OcrExclusionMatcherTest 35/35;
  full testDebugUnitTest + verifySqlDelightMigration BUILD SUCCESSFUL
  3m3s; :app:assembleDebug BUILD SUCCESSFUL (arm64 APK 12:42).
  NO DB schema change (no migration needed).

Files changed (7): OcrExclusionMatcher.kt, OcrExclusionMatcherTest.kt,
  ReaderViewModel.kt (saveExclusionZone), OcrExclusionZoneDialogs.kt
  (dialog + isLegacyZone), SettingsOcrExclusionsScreen.kt (RuleRow),
  ReaderActivity.kt (original-bounds guard + BitmapFactory import),
  TtsPlaybackController.kt (OCR-ZONE per-rule log), i18n base
  strings.xml (ocr_exclusion_match_text_optional; label de-required).

Device verification PENDING user (repeat matrix): fresh vs cached OCR,
replay, exit/reopen, toggle off/on, delete, multi-zone, page-edge zone,
webtoon top/middle/bottom, CHAPTER/MANGA/SOURCE pure-zone (blank text),
COMBINED opt-in, repeated same-scenario runs (detect intermittency),
"OCR-ZONE rule=" lines confirm rule content. Existing device DB rules
(match_type=COMBINED from old save path) keep working as COMBINED;
re-create as blank-text zone rules for pure-rect behavior.
```

```text
[COMPLETED 2026-09-03 — verify2 log deep-analysis (read-only, no code change)]

Analyzed .device-pass/ocr-excl-verify2.log (79MB; PID 24098 12:54–13:11,
PID 8957 13:26–13:39+ after user relaunch, build 0.5.1-8255) +
ocr-excl-verify.log (= truncated prefix of verify2, ends 13:06, NO new data;
do not re-analyze) + ocr-excl-snapshot1.log (PID 20038, build 8254).
Findings (full report in session; structural facts only, no OCR text):

EXCLUSION PIPELINE: HEALTHY — no nondeterminism found.
- All "A/B changes across acquisitions" cases resolve to CHAPTER switches:
  page=0 with B=16/22/18/25/8/24 = chapters 2145/2188/2187/2186/1717/2184
  respectively. Same (chapter,page) re-acquired → A and B ALWAYS identical,
  cache vs fresh (dispatch textHashes also identical).
- No page ever completed a fresh scan twice (single-flight + cache hold).
  Near-miss: ch2184 p0 scan 13:30:25 abandoned (user left reader, VRI
  destructor 13:30:48) before completion — second attempt 13:32:10 was
  cache MISS → fresh → 2/24. No A/B contradiction.
- Matcher fixes verified stable: snapshot1 ch2152 p19 = 1/13 FRESH then ×8
  CACHE 1/13; ch2145 p0 = 0/16 across 1h47m + app restart (cache stable).
- ZONE pure type: NEVER appears in any log. Types seen: PHRASE/COMBINED/WORD
  only. ("types={[100]=...}" lines = Samsung CpEventLog telephony noise.)
- "dedup dropped": ZERO events in all three logs (only MR2SystemProvider /
  fb4a substring noise).
- Rule CRUD invisible (no repo logging) but inferable: rules count path
  2→{COMB=2}→1→3→4→5 with dialog opens (WindowManager addView) at
  13:29:45–57 (before first rules=4 line) and 13:36:44–58 (before first
  rules=5 line). Count bumps land exactly after dialog windows. Rules
  re-queried per acquire confirmed live (mid-session adds take effect).

REAL DEFECTS FOUND (unrelated to matcher, ranked):
1. NEW BUG — NetworkOnMainThreadException ×25: TtsPlaybackController.scanOnDemand
   → OcrPageSourceResolver.resolveRemotePages → HttpSource.getPageList via
   awaitSingle ON MAIN THREAD. Kills next-page prefetch scans in ~35ms each
   (ch2188 p1/p2 ×10 each; ch2184 p1..p7). "TTS prefetch scan failed page=N
   (best-effort)". FIX CANDIDATE: move page-list resolve off main (IO
   dispatcher) — root-cause fix at resolver/scanOnDemand level.
2. Glens HTTP 502 ×4 (13:05:35 / 13:34:58 / 13:38:26 / 13:38:33), server-side,
   NO client retry. FALLBACK INCONSISTENCY: recognizeText path falls back to
   fast engine (13:05 → FastOcrEngine 2245ms OK); scan path does NOT — 502
   during scan = page gets NO OCR at all (TTS on-demand scan failed).
3. DetectionUnavailable redirect stacks ×22 (detection model absent on
   device — expected, but 21-frame W-stack per scan = log spam; benign).

Noise ruled out: SQLiteLog (10) LOCK error 3850 ×10 = transient lock
contention, self-recovered, not exclusion-related. 24098 death 13:26:06 =
normal LMK cached-app reaping (cch CAC), no crash/ANR.

Device clock = host + ~5h29m (log timestamps 12:54+ ↔ file mtime ~07:30).
```

## In progress

```text
P0 followup: ZONE-exclusion "prefill regression" FIX — code done, device
verify pending. Device logs (excl3-final.log, 20:10-20:26) proved all 6
new zone-drag rules (34,36,37,38,39,46) saved type=COMBINED: dialog
pre-filled matchText with OCR-detected text (RC1 resurrected via UI);
user tapped Save w/o clearing → rect+text conjunct broke on OCR text
variance again (rule 38 covers 94% of page 15 yet excluded=0/4).
FIX: prefill removed; dialog matchText starts EMPTY; blank→ZONE, typed→
COMBINED (opt-in as designed). Dead chain deleted: ExclusionZoneScopeDialog
detectedText param, Dialog.ExclusionZoneScope.detectedText field,
openExclusionZoneScopeDialog param, detectExclusionZoneText +
ocrExclusionCropText fns, ReaderActivity detection block (crop OCR
fallback + selectionChapterId/selectionBox), boxMostlyInside import.
Gates GREEN (docker, correct volume mounts — android-home mounts at
/home/vscode/.android NOT /opt/android-sdk, memory line 172): spotless+
compile 5m12s, spotlessCheck+testDebugUnitTest+assembleDebug 3m14s.
APK installed 16:17. Fresh capture /sdcard/zone-prefill-fix.log running.
NOTE: old COMBINED rules 34-46 from bad session still in device DB —
must be deleted/re-created blank for pure-zone behavior. PHRASE/WORD
rules confirmed working in same log (3/19 etc.) — "phrase broken" cases
were COMBINED-by-prefill zones, not phrase rules.
```

Feature: Phase 10A advanced system TTS voice configuration — COMPLETE
           (device-verified 2026-08-31), ALL UNCOMMITTED awaiting user commit.
           Tasks 1-6 (domain prefs/resolver, TtsEngine contracts,
           AndroidTtsEngine re-apply design, Read aloud & voice screen,
           root entry + reader deep-link, docs) + Task 7 device pass
           (USER CONFIRMED all manual checks PASS 2026-08-31) + voice-picker
           SEARCH feature (user-requested follow-up: BasicListPreference
           searchable flag → ListPreferenceWidget OutlinedTextField filter,
           voice picker only; gates green, APK 0.5.0-8250 installed).
           Evidence: .device-pass/tts-10a-test.log (571,082 lines, PIDs
           14214/30339): advances 17→18 confirmed 1-2ms, pause/stop clean,
           preview audio-focus request/abandon paired (08:51:53→56 cycle),
           "TTS default voice restored name=en-IN-language" ×4 (SystemDefault
           re-apply path device-proven), 0 FATAL exceptions, 0 sentence
           failures, single TTS engine connection per session. Device engine
           reality: Google Speech (com.google.android.tts) = only real
           synthesis engine; SamsungTTS present as download-provider only
           (no voices installed) — picker correctly lists real engines only.
           Deferred to 10B: per-voice rate/pitch profiles, cloud/neural
           providers, local neural engines, downloadable AI voices,
           expressive speech.
```

Done: leak-fix set COMMITTED (5c7d2cc2c): ReaderViewModel.onCleared
      ttsEngine.onFocusEvent = null (detaches dead controller from singleton
      engine — the LeakCanary-flagged ~100MB chain: TTS service native root →
      AndroidTtsEngine → onFocusEvent → dead controller → ViewModel → destroyed
      Activity); ReaderActivity DisposableEffect onDispose cancels
      settingsScreenModel.ioCoroutineScope (both composition blocks);
      LeakCanary core debugImplementation re-enabled. Known issue #5 → RESOLVED
      (old "bounded, self-healing" assessment wrong — LeakCanary showed ~100MB).
      Memory profile captured (meminfo-profile.log: Java 8–25MB / Native 8–28MB
      / TOTAL ~160–267MB, stable). Gates green 2026-08-29. Debug APK 0.4.0-8241
      (built from 5c7d2cc2c) installed on SM_M066B 2026-08-29.
      NOTE: user docs commit fd52a613a accidentally reverted memory.md/phase.md
      to pre-leak-fix state and broke ReaderActivity import order; docs
      restored + imports re-fixed (spotlessApply) this session (2026-08-29).

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

P0 + P1 FIXES LANDED (2026-08-28, COMMITTED as 872c55397 + 8cb320e7c, all gates green):
  - P0 prefetch spam: TtsPlaybackController.rebuildQueueForUserNavigation now
    dedups on lastRebuildPageIndex (skip if same page already handled);
    schedulePrefetch guards on prefetchPageIndex (skip if same page already
    in flight); both trackers reset in resetSession + on provideContext()==null.
  - P1 rapid-swipe mass OCR: ReaderViewModel.onPageSelected debounces the TTS
    call by 250ms (TTS_PAGE_SELECTED_DEBOUNCE_MS) for user navigation; advance
    confirmations (hasPendingAdvance) still land immediately, never debounced.
  - ScrollToRegion: added logcat in WebtoonViewer.scrollToRegion (success +
    view-not-laid-out paths) so next device run confirms auto-scroll fires.

MINI-PLAYER Z-ORDER FIX (2026-08-28, COMMITTED as 41200022e, all gates green):
  - Root cause: in the inner ContentOverlay Box (ReaderActivity.kt),
    TtsPlaybackBar was the LAST child — declared after `when (state.dialog)` —
    so it drew ON TOP of the inline OcrResultOverlay (scrim + popup/sheet),
    while window-based dialogs (AdaptiveSheet) always covered it: inconsistent
    z-order. FIX: moved TtsPlaybackBar before the dialog block so all
    dialogs/overlays render above the pill. OcrLoadingIndicator position
    unchanged (pre-existing behavior). User committed prior fix set as
    fff9583f0 + 872c55397 + 8cb320e7c.

CONTROLLER ACTION LOGGING (2026-08-28, COMMITTED as 41200022e/be31edb71, all gates green):
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

LEAKCANARY PASS — FINDING #5 ~100MB READER LEAK (2026-08-28, fix COMMITTED
  2026-08-29 as 5c7d2cc2c):
  Evidence: .device-pass/logcat-leakcanary.log (37M; LeakCanary permission
  prompt 20:33:01, leak-launcher badge present = leak detected; TTS session
  PID 31288 same log). LeakCanary (previously commented out upstream) enabled
  via debugImplementation(libs.leakCanary.core) flagged the reader screen
  retaining ~100MB after exit. Root cause: system TTS service keeps a native
  GC root to the TextToSpeech callback → AndroidTtsEngine singleton →
  onFocusEvent lambda → dead TtsPlaybackController → ReaderViewModel →
  destroyed ReaderActivity. FIX (3 files, COMMITTED 5c7d2cc2c):
  - ReaderViewModel.onCleared: ttsEngine.onFocusEvent = null after
    stop()/shutdown() — detaches controller from singleton engine; next
    reader session re-registers in controller init. Supersedes the old Known
    issue #5 "bounded, self-healing" assessment: LeakCanary showed ~100MB
    retained, NOT a small bounded object graph.
  - ReaderActivity: DisposableEffect { onDispose { settingsScreenModel
    .ioCoroutineScope.cancel() } } (both composition blocks) — the settings
    screen model's io scope outlived the Activity otherwise.
  - app/build.gradle.kts: LeakCanary core re-enabled for debug builds
    (Phase 9 leak verification; upstream had it commented out).
  Known issue #5 → RESOLVED by this fix. GATES GREEN 2026-08-29
  (spotlessCheck + :app:compileDebugKotlin + testDebugUnitTest, BUILD
  SUCCESSFUL 2m56s). Memory profile captured (meminfo-profile.log: Java
  8–25MB / Native 8–28MB / TOTAL ~160–267MB, stable, no leak spike).
  Debug APK 0.4.0-8241 installed on SM_M066B 2026-08-29. Device re-verify
  (leak gone) DONE 2026-08-29 — see LEAK-FIX DEVICE VERIFICATION below.

LEAK-FIX DEVICE VERIFICATION (2026-08-29, build 0.4.0-8241, PID 31179):
  Evidence: .device-pass/leak-full.log (33.6M full ring dump, 18:15–19:40).
  RESULT: FIX VERIFIED — LeakCanary heap dump (user-requested 19:38:32,
  analyzed 19:40:16, 95s) reports **0 APPLICATION LEAKS** after TWO full
  reader TTS sessions + exits (18:55 webtoon ch248 + 19:29–19:31 ch248
  re-run). Heap total 41.8MB, 79 bitmaps 7.5MB, large bitmaps 0.
  - Session 1 (18:55): rapid user nav pages 5↔6 fired only 3 on-demand
    scans in 1.2s (250ms debounce + single-flight working as designed),
    prefetch cache hits, stop(Playing)→stop(Idle) clean.
  - Session 2 (19:29): startup open→first page ready 3764ms (cache hit);
    advances 8→9 confirmed 1–6ms, 0 timeouts; page acquire 5–15ms cache
    hits; prefetch hits; stop clean.
  - No sentence failures, no advance timeouts, no Error phases.
  - Noise only: transient GLens HTTP 502 (known server-side, retried OK);
    SQLiteLog POSIX 3850 warnings (system-side, not TTS).
  Finding #5 device-verified CLOSED. Phase 9 leakcanary sign-off DONE.

BATTERY MEASUREMENT — PHASE 9 FINAL ITEM (2026-08-29, build 0.4.0-8241):
  Method: batterystats --reset + on-device 10s sampler (level/current/charge
  counter → battery-sample.log, 5726 lines) + on-device full logcat
  (battery-tts-session.log, 65.6MB) + post-hoc `dumpsys batterystats
  app.yomihon.dev` (batterystats-app.txt). Charger unplugged for whole
  measurement window (powered 20:03, unplugged 20:10, replugged 21:04).
  Realistic use: 23 min hands-off-ish session (PID 31179): villain-to-kill
  ch2→1 auto-advance, then ch246→ch242, rapid sentence stepping, 5 chapter
  advances, 58/58 page advances confirmed 1–3ms, 0 timeouts, 94 on-demand
  Glens scans (fresh chapters, 97 cache hits elsewhere), 0 sentence
  failures. Session ENDED at 20:31:50 by transient GLens HTTP 502 (known
  server-side; scan-fail → honest Error = correct by design). Short second
  session 20:45 (PID 32549, cached ch244, ~35s, sentence stepping + clean
  stop 20:45:51, engine disconnect <1s).
  RESULTS (sampler, device-wide current incl. screen+modem):
  - Active TTS+OCR session (20:08:39–20:31:50): avg ~365mA, median ~319mA
    device-wide. Battery 5000mAh nominal → ~13h continuous playback at that
    screen-on draw; screen is the dominant consumer (Samsung attributes
    84.5mAh of app's 115mAh to screen).
  - Post-exit, app alive background (20:31–20:44): avg ~222mA
    device-wide — dominated by OTHER apps/screen (user browsing); app's own
    logcat TTS/OCR activity = ZERO after 20:31:50.
  - App killed by LMK 20:44:50, cached-frozen 20:49:28. Frozen-idle
    (20:46–21:04): median device draw -14.8mA floor (idle floor), app frozen
    = no app CPU.
  - batterystats attribution (u0a435 = app.yomihon.dev, full 1h34m
    on-battery window): TOTAL 115mAh (fg 25.4mAh CPU / 24m51s foreground,
    bg 4.18mAh / 12m26s, cached 1.16mAh / 23m39s); screen-off/doze drain
    cpu:bg=4.02mAh+cached=1.01mAh ≈ 5mAh over 41m screen-off — that
    includes LMK reaping + freeze accounting, i.e. no runaway background
    work; TOTAL partial wakelock for uid = 0 (only WindowManager screen
    wakelock 19m = reader visible, as designed with keep-screen-on).
    App CPU total: 10m51s usr + 2m19s krn across both PIDs.
  - Keep-screen-on worked as designed: 19m2s full wakelock exactly while
    reader visible during playback.
  VERDICT: PASS. No post-exit battery drain (TTS engine disconnect <1s,
    zero app log activity 20:32–20:44, no wakelocks, cached-frozen within
    ~4 min of exit, screen-off CPU cost ≈ noise).
  LIMITATIONS (honest): (a) sampler measures DEVICE current, not app
    rail — app-specific numbers come from batterystats estimates
    (Samsung-model-based, ±); app fg CPU cost ≈ 25mAh for ~25 min
    foreground ≈ 1mAh/min with screen+TTS+OCR+network. (b) Active phone
    call 3m40s + other apps (YouTube-class audio app u0a222 94mAh,
    u0a334 206mAh) shared the same on-battery window — session avg current
    includes their draw where overlapping. (c) GLens 502 ended the long
    session at 23 min instead of user-planned ~30 min; coverage still
    spans 3 chapters + 94 real network OCR scans. (d) No A/B vs
    TTS-off baseline run recorded (would need second unplugged pass);
    relative cost: app fg 25.4mAh CPU vs screen 84.5mAh in same window →
    app compute is minority of app-session cost, screen dominates —
    expected for a reader.
  PHASE 9 COMPLETE. Remaining: commit small uncommitted set (import order
  + docs) when user approves.

[COMPLETED 2026-08-30 — Reader TTS mini-player UI polish, UNCOMMITTED (3 files)]
- Redesigned TtsPlaybackBar full-width bottom bar → floating pill:
  wrap→fixed width (fillMaxWidth(0.92f) + widthIn(max=560dp)), 28dp rounded
  corners, surfaceColorAtElevation(3).copy(0.9/0.95 alpha), shadow 6dp,
  text slots weight(1f) so icon row anchors (no width jump per sentence).
- Layout-aware positioning (no hardcoded offsets): ReaderAppBars gains
  onBottomTrayHeightChanged callback — onGloballyPositioned on bottom
  AnimatedVisibility reports real tray height (incl. nav-bar insets; 0 when
  hidden); ReaderActivity computes clearance = max(trayPx, navBarPx,
  cutoutPx) + 12dp margin; pill rides above bottom tray when menu visible,
  falls back to insets when hidden. Rotations/config changes re-measured.
- Perf: position animated via animateIntAsState → Modifier.offset{} lambda =
  PLACEMENT-phase only (zero re-measure per frame), 150ms tween (was
  animateDpAsState→padding 200ms = measure-phase churn = choppy).
  Root cause of "slow/choppy" was structural (padding invalidation), not
  jank — confirmed via logcat (no skip attributable to pill).
- Gates GREEN 2026-08-30: spotlessCheck + testDebugUnitTest + :app:assembleDebug
  (devcontainer JDK17, -Xmx4g). Installed SM_M06B arm64 APK.
  User device-tested ALL scenarios PASS (manga+webtoon, portrait+landscape,
  rotation, phase changes incl. Preparing/Playing/Paused/Error).
- Final logcat verification pass (.device-pass/ui-polish-final.log, PID 11054,
  23:53–23:57 session): 0 TTS errors/timeouts, 0 crashes, 0 Compose errors.
  All Choreographer frame-skips attributed, NONE from pill code:
  319f+68f+39f+34f = LeakCanary heap-dump cycle (hprof 74MB 5.2s + explicit
  GCs, debug-build tooling, absent in release); 53f = dropdown popup attach;
  51f = reading-mode viewer switch (webtoon→L2R pager recreation + toast,
  known upstream cost); 37f = background GC freeing 36MB (normal churn).
  TTS advances 1–6ms confirms, OCR cache hits 5–25ms, prefetch clean.
- LeakCanary same-session analysis: flagged 1 APPLICATION LEAK signature
  (WebtoonPageHolder ~49.9MB, d385fca8…) — WEBTOON page holder retained
  via mapLatest flow chain while reader ACTIVE (Activity mDestroyed=false,
  view attached). NOT the TTS pill; NOT reader-exit retention (past passes
  showed 0 application leaks after exit). Likely live-page bitmap cache
  held by still-active viewer page recycling lag; separate investigation
  item, not a regression of this change set. No release impact.
- KNOWN ISSUE #13 (NEW, LOW): LeakCanary Toast-watching noise (retained
  Toast FrameLayout) triggers debug heap dumps mid-session; consider
  excluding android.widget.Toast in LeakCanary config if it bothers future
  device passes.

[COMPLETED 2026-08-30 — Phase 10A Task 1, UNCOMMITTED (2 files)]
- TtsVoicePreferences (:domain mihon.domain.tts.service) — engine package /
  voice name / language tag string prefs (keys pref_tts_engine_package /
  pref_tts_voice_name / pref_tts_language_tag, default "") + reset();
  TtsVoiceSelection sealed interface (SystemDefault / Voice / Language) +
  pure resolveVoiceSelection fallback: valid voice → Voice, else valid
  language → Language, else SystemDefault. Consumes PreferenceStore
  (precedent TtsPreferences.kt). Tasks 2/3/5 consume this.
- TDD: TtsVoicePreferencesTest 5 cases — RED (unresolved reference,
  compile fail as brief expected) → GREEN 5/5. Full :domain:testDebugUnitTest
  green (all suites). spotlessCheck green (devcontainer JDK17, -Xmx4g).
  NOTE: brief's bare docker cmd lacks -v yomihon-gradle-home/-v yomihon-
  android-home mounts — wrapper re-downloads Gradle 9.6.1 each run and
  times out; mount both volumes (memory 2026-08-24 directive stands).

[COMPLETED 2026-08-30 — Phase 10A Task 3, UNCOMMITTED (3 files)]
- AndroidTtsEngine implements extended TtsEngine interface (:app):
  constructor gains TtsVoicePreferences; cached enginePackage/voiceName/
  languageTag fields + activeEnginePackage (set at initialize); engine-
  package-aware TextToSpeech creation (3-arg ctor (context, listener, pkg)
  when pkg non-empty, else 2-arg); idempotent initialize path re-applies
  voice config from fresh prefs BEFORE returning true (mid-session pref
  changes land at next pause/resume with ZERO controller changes —
  controller/ReaderViewModel untouched); applyVoiceConfig uses
  resolveVoiceSelection (Voice → setVoice + DEBUG log; Language →
  setLanguage + LANG_MISSING_DATA/LANG_NOT_SUPPORTED fallback log;
  SystemDefault → no-op) — NEVER fails initialize; setEnginePackage
  compares against activeEnginePackage, shutdown releases instance when
  mismatch; getEngines/getVoices mapped to TtsEngineInfo/TtsVoiceInfo.
- applyVoiceConfig re-reads ONLY voiceName/languageTag — NOT enginePackage
  (deviation from brief §4: re-reading enginePackage would cache the new
  value and make Task 5's setEnginePackage(newPkg) early-return, so a live
  instance built with the OLD engine would never rebuild. activeEngine-
  Package tracks creation-time package for the compare).
- BRIEF API CORRECTIONS (brief assumed wrong SDK signatures): 3-arg
  TextToSpeech ctor is (context, OnInitListener, enginePackage) — NOT
  (context, pkg, listener); engine.defaultEngine returns String (package
  name) — NOT EngineInfo; EngineInfo has name (pkg) + label: String (non-
  null) — no packageName field. Verified via javap android-36 android.jar.
- DI: PreferenceModule +TtsVoicePreferences factory (after TtsPreferences);
  DomainModule AndroidTtsEngine(get<Application>(), get<TtsVoicePreferences>()).
- Regression guard: speak/stop/shutdown/audio-focus/pendingUtterances/
  onFocusEvent paths byte-identical (verified via git diff — none appear).
- Gates GREEN 2026-08-30 (docker devcontainer JDK17, -Xmx4g, both volumes):
  :app:compileDebugKotlin BUILD SUCCESSFUL 4m19s; spotlessCheck BUILD
  SUCCESSFUL 35s; full testDebugUnitTest BUILD SUCCESSFUL 2m40s.
  NO commit (per ruling).

[COMPLETED 2026-08-30 — Phase 10A Task 4, UNCOMMITTED (4 files)]
- ReadAloudSettingsScreenModel (:app ui/setting/readaloud): StateScreenModel
  over ReadAloudSettingsState (@Immutable; isLoading/loadFailed/engines/
  voices/selectedEnginePackage/selectedVoiceName/selectedLanguageTag/rate/
  pitch/isPreviewPlaying). load() = engine.initialize() FIRST (empty lists
  guard), false → loadFailed state; true → getEngines/getVoices + pref
  reads into state. selectEngine guards pkg against loaded engines, writes
  all three voice prefs (voice+language reset ""), engine.setEnginePackage,
  reload. selectLanguage writes languageTag + clears voice pref (no engine
  call — applied at next initialize). selectVoice writes voice pref +
  engine.initialize() relaunch (re-apply on live instance). setRate/setPitch
  write global TtsPreferences pair (controller collects live). preview() in
  screenModelScope try/finally: stop → initialize → acquireFocus → speak
  (hardcoded PREVIEW_TEXT, no i18n per ruling) → finally resets
  isPreviewPlaying + abandonFocus + stop (scope-cancel safe). stopPreview()
  = engine.stop() (speak returns → finally resets flag). resetVoiceConfig()
  = voicePreferences.reset() + setEnginePackage("") + reload.
- SettingsReadAloudScreen (SearchableSettings object, Anki pattern):
  loading → CustomPreference spinner; loadFailed → InfoPreference
  (tts_voices_unavailable) + retry TextPreference; 3 PreferenceGroups:
  Text to speech (engine picker + Language + Locale + Voice pickers),
  Voice calibration (rate/pitch SliderPreferences 50..200% reusing
  pref_tts_speech_rate/pitch keys + preview TextPreference with small
  CircularProgressIndicator widget while isPreviewPlaying), Advanced
  (engine info TextPreference no onClick, Available voices InfoPreference
  %d, Reset voice configuration + toast). Helpers private in screen file:
  localeDisplayName (Locale.forLanguageTag + getDisplayName, blank→tag),
  voiceLabel (name + " · high quality" q>=400 / " · low latency" l<=200 /
  " · network" — API facts only), engineInfoSummary (label · voice count).
- LANGUAGE/LOCALE RULING implemented (brief's binding resolution): ONE
  persisted pref (ttsLanguageTag full-tag semantics). Language row: entries
  = "" (Default system engine) + ALL distinct full tags sorted. Locale row:
  "" + full tags filtered to selectedLanguageTag prefix, enabled only when
  a language is selected; both rows write ttsLanguageTag via selectLanguage.
  Bare-language entries exist only if engine reports them under bare tags.
  Voice picker filter: exact tag match when selected tag contains '-',
  prefix match for bare tag, all voices when empty. Stale-pref values not
  in entries render raw tag/name via subtitleProvider { v, e -> e[v] ?: v }
  (engine picker falls back to Default system engine label) — never "null".
- i18n: 17 keys added to base strings.xml TTS block (pref_category_read_
  aloud, pref_read_aloud_summary, tts_section_text_to_speech/
  voice_calibration/advanced, tts_engine/voice/language/locale,
  tts_default_system_engine, tts_engine_information, tts_available_voices
  (%d), tts_preview_voice, tts_play_sample, tts_reset_voice_config,
  tts_config_reset, tts_voices_unavailable). pref_read_aloud_summary
  consumed by Task 5 (main-screen entry wiring is Task 5 scope).
- SettingsSearchScreen settingScreens list: SettingsReadAloudScreen added
  (after SettingsBrowseScreen).
- Gates GREEN 2026-08-30 (docker devcontainer JDK17, -Xmx4g, both volumes):
  :app:compileDebugKotlin BUILD SUCCESSFUL 2m57s (+2m25s re-run after
  review fixes); spotlessCheck GREEN (one ktlint line-length fix via
  spotlessApply); full testDebugUnitTest BUILD SUCCESSFUL (all suites).
  NO commit (per ruling).
- [FIX ROUND 1 2026-08-30 — review findings, UNCOMMITTED, same 2 code files]
  Finding 1 (preview ignores rate/pitch): preview() now calls
  engine.setSpeechRate(mutableState.value.rate) + setPitch(mutableState.value
  .pitch) between initialize() and acquireFocus() — values read INSIDE launch
  (latest state at execution time). Root cause: initialize() existing-engine
  path only re-applies voice/language; engine rate/pitch fields ctor-time 1f.
  Finding 2 (stale voice on reset/selectLanguage→default): applyVoiceConfig
  SystemDefault branch no-op → now restores engine.defaultVoice via setVoice
  (null defaultVoice → DEBUG log + leave, language NOT touched — SystemDefault
  is engine's choice not device locale). Language branch unchanged — framework
  doc: setLanguage sets default voice for that language (self-clearing).
  Minor: preview() early-returns if isPreviewPlaying (double-click guard).
  No AndroidTtsEngine ctor change (no second prefs dep). Gates GREEN:
  compileDebugKotlin 2m28s / spotlessCheck 39s first-try / testDebugUnitTest
  2m45s. Details: task-4-report.md "Fix round 1".
```

```text
[COMPLETED 2026-08-30 — Phase 10A Task 5, UNCOMMITTED (8 files)]
- Settings root entry: SettingsMainScreen.getItems gains Item after Reader
  (pref_category_read_aloud / pref_read_aloud_summary / Icons.AutoMirrored
  .Outlined.VolumeUp → SettingsReadAloudScreen object). AutoMirrored icon
  resolved at compile — fallback unused.
- Deep-link plumbing: Constants.SHORTCUT_VOICE_SETTINGS =
  "eu.kanade.tachiyomi.SHOW_VOICE_SETTINGS"; SettingsScreen.Destination
  ReadAloud id 4 (0-3 taken) + BOTH when branches (phone SettingsMainScreen
  fallback / tablet SettingsAppearanceScreen fallback) →
  SettingsReadAloudScreen; MainActivity.handleIntentAction new case beside
  ACTION_APPLICATION_PREFERENCES: popUntilRoot + push(SettingsScreen(
  Destination.ReadAloud)) → null.
- Reader tab minimal per spec PART 5: ReadAloudPage pitch SliderItem
  REMOVED (relocates to main settings Voice Calibration; rate slider + 3
  checkboxes kept); nav row TextPreferenceWidget(tts_advanced_voice_
  settings) at bottom; signature + onOpenVoiceSettings callback.
  ReaderSettingsDialog param after onHideMenus; tab order + dim-hack
  currentPage==2 untouched. ReaderActivity BOTH dialog call sites
  (phone L463 block + tablet L886 block): closeDialog() + startActivity
  MainActivity action=SHORTCUT_VOICE_SETTINGS FLAG_ACTIVITY_CLEAR_TOP
  (openMangaScreen precedent; no new imports). TtsPlaybackController +
  ReaderViewModel ZERO-DIFF confirmed.
- i18n: tts_advanced_voice_settings "Advanced voice settings" appended to
  TTS block.
- Gates GREEN 2026-08-30 (docker devcontainer JDK17, -Xmx4g, both volumes):
  :app:compileDebugKotlin BUILD SUCCESSFUL 3m36s; spotlessCheck BUILD
  SUCCESSFUL 46s first-try; full testDebugUnitTest BUILD SUCCESSFUL 2m57s
  (all suites incl. TtsVoicePreferencesTest 5/5). NO commit (per ruling).
  Full flow live: Settings root → Read aloud & voice ↔ reader quick
  settings → deep-link. Report: .superpowers/sdd/2026-08-30-phase10a-
  advanced-tts-voice-config/task-5-report.md.
```

```text
[COMPLETED 2026-08-30 — Phase 10A Task 6 (docs), UNCOMMITTED (5 docs files)]
- Documented the Phase 10A implementation across the docs system (Task 7
  device verification still PENDING — nothing marked complete):
  - prd.md: new §6 Phase 10A section — capabilities (engine/language/locale/
    voice pickers, calibration, preview, persistence, fallback chain, reset,
    entry points), user flow, preview policy (single shared engine; preview
    stops previous utterance; reader narration wins by QUEUE_FLUSH; honest
    pause), §6.4 Phase 10B deferred list (per-voice rate/pitch profiles,
    cloud/neural providers w/ credentials+cost+privacy+streaming+caching+
    latency, local neural engines, downloadable AI voices, expressive speech
    needs SSML/prosody/emotion — pitch/rate not expressive, no fake emotion).
    §4 future list annotated: voice picker landed in 10A.
  - architecture.md: §5 TtsEngine 10A contracts (getEngines/getVoices/
    setEnginePackage + models); AndroidTtsEngine config pipeline (construction
    seed, activeEnginePackage compare+shutdown on engine switch, initialize
    re-applies FRESH prefs every call → zero controller changes for mid-
    session pref pickup; applyVoiceConfig via resolveVoiceSelection, never
    fails initialize); preview single-engine policy; future-provider Injekt
    swap note. §7 tables: +3 new 10A files, integration-point list expanded.
  - design.md: new §13 — 3 preference groups (Text to speech / Voice
    calibration / Advanced), loading spinner (Anki pattern), failure+retry
    state, voice metadata labels API-facts-only (quality≥400 high quality,
    latency≤200 low latency, network marker), Locale.getDisplayName entries,
    stale-pref raw-value subtitleProvider, reader tab minimal + nav row,
    pitch relocated.
  - phase.md: Phase 10A entry IN_PROGRESS (code complete, gates green,
    device verification PENDING — completion gated on Task 7 + commit); old
    Phase 10 block → Phase 10B with 10A-landed/10B-remaining note; pointer
    block + dependency graph updated.
  - memory.md: this record.
- Gates: docs-only change — no Gradle run needed (spotless does not cover
  docs/*.md; no code touched). No commit/stage (per ruling).
- Phase 10A overall state: Tasks 1–5 code + Task 6 docs ALL UNCOMMITTED;
  gates green per task reports (spotlessCheck / testDebugUnitTest /
  :app:compileDebugKotlin; verifySqlDelightMigration NOT needed — no DB
  change). Task 7 device verification NOT run.
```

```text
[COMPLETED 2026-08-31 — Phase 10A Task 7 (device pass) + voice-picker search, UNCOMMITTED]
- DEVICE VERIFICATION PASS (build 0.5.0-8250, SM_M066B, USER CONFIRMED):
  Read-Aloud playback, language/locale selection, voice selection, voice
  preview, speech rate, and all other tested functionality working. Manual
  checks PASS per user report 2026-08-31.
- Evidence collected: .device-pass/tts-10a-test.log (571,082 lines, PIDs
  14214/30339, two sessions 06:37 + 08:51): page advances 17→18 confirmed
  1–2ms with 0 timeouts; pause/stop cycles clean (phase=Paused/Idle);
  preview audio-focus request/abandon PAIRED (08:51:53 request → 08:51:56
  abandon); "TTS default voice restored name=en-IN-language" ×4 — the
  SystemDefault re-apply branch (Task 4 fix round 1) proven on-device;
  0 FATAL exceptions; 0 sentence failures; single "Connected to TTS engine"
  per session (no duplicate TextToSpeech instances).
- Device engine reality (user observation + log): only default system engine
  + Google Speech Recognition & Synthesis appear in the picker; SamsungTTS
  present on device as a download-provider only (log: "Provider found [4]
  voices" but no synthesis service voices installed). Picker correctly lists
  only real installed engines — dynamic discovery working as designed.
- FOLLOW-UP FEATURE (user request, same session): voice-picker SEARCH —
  hundreds of voices made scrolling painful. Implementation (3 files + 1
  call-site):
  - Preference.kt: BasicListPreference gains `searchable: Boolean = false`.
  - PreferenceItem.kt: passes searchable through to ListPreferenceWidget.
  - ListPreferenceWidget.kt: when searchable, OutlinedTextField (placeholder
    = existing action_search MR key — zero new i18n) filters entries by
    case-insensitive contains on entry labels; remember(searchQuery, entries)
    memoizes the filtered map; list auto-shrinks as user types.
  - SettingsReadAloudScreen.kt: voice picker passes searchable = true;
    engine/language/locale pickers unchanged (small lists don't need it).
  - Gates GREEN: spotlessCheck + :app:compileDebugKotlin + testDebugUnitTest
    + :app:assembleDebug (3m51s / 3m28s, devcontainer JDK17 -Xmx4g). APK
    0.5.0-8250 reinstalled on SM_M066B.
- PHASE 10A COMPLETE. All work UNCOMMITTED awaiting user commit.
```

```text
[COMPLETED 2026-09-01 — Speed-adaptive Read-Aloud OCR prefetch, UNCOMMITTED]
- Problem: hardcoded N+1 prefetch had zero margin at high speech rates.
  Measured: Glens scan p50 8.1s / p90 16.7s / max 31s vs page speech time
  ~10-17s at 3x. Cache hits 9ms (irrelevant).
- TtsPlaybackController.kt (only code file):
  - prefetchPageIndex → prefetchPages: IntRange? (pages covered by
    current/last prefetch job).
  - prefetchDepth(): rate <1.5x → 1, 1.5-2.5x → 2, ≥2.5x → 3
    (MAX_PREFETCH_DEPTH=3 const). Depth read at schedule time — mid-session
    rate changes pick up naturally.
  - schedulePrefetch: targetPages = start until min(start+depth, totalPages)
    (auto-clamped at chapter end); same-pages+active-job skip guard; ONE
    coroutine iterating pages SEQUENTIALLY (per-page cache check via
    getCachedPageOcr → scanOnDemand reportFailure=false; CE rethrow; other
    exceptions logged/no-op). No new parallelism — respects serialized
    PrioritizedTaskQueue, single in-flight repo scan.
  - Mid-page escalation: sentence loop at sentenceIndex == size/2 with
    rate ≥ 2f calls schedulePrefetch(page+1) — no-op via guards when
    already covered. No new state.
  - Cancellation: rebuildQueueForUserNavigation + resetSession now also
    null prefetchPages (old code left stale prefetchPageIndex — was safe
    via isActive check but inconsistent).
  - Prefetch start log now includes pages range + rate.
  - resumeIndex semantics, dispatch-counter ids, exclusion awaitForSpeech
    untouched.
- rules.md §6: prefetch line updated to 3-page bounded, speed-aware depth.
- Gates NOT run (per task instruction). ktlint 120-col verified by awk.
- Device verify pending: 3x playback of Glens-style tall strips should show
  "TTS prefetch start pages=N..N+2 rate=3.0" with no mid-playback OCR
  LoadingPage gaps.
```

```text
[COMPLETED 2026-09-01 — Phases A–I multi-feature roadmap, UNCOMMITTED]

Phase A — Intelligent speech cleanup (domain mihon.domain.tts.speech):
- SpeechCleaner: punctuation-only skip (post-normalization so "WHAT?!?!?!" →
  "WHAT?!" kept), conservative OCR-garbage detector (<40% letters over ≥4
  chars drops symbol soup; never drops emphatic dialogue), excessive punct
  normalization (runs ≥3 → 2 chars incl. full-width ！！？？), whitespace
  collapse (bubble \n → space). SpeechCleanupOptions has per-feature toggles.
- Tests: SpeechCleanerTest 9 cases.

Phase B — Expression/language filtering:
- SpeechRegionFilterConfig: speak-sfx/expressions/decorative/unknown toggles +
  skipForeignScript + speechScript (LATIN/CJK — script-based not
  language-name-based, stays multilingual).

Phase C — Region classification:
- SpeechRegionClassifier heuristics: blank/symbol-only → DECORATIVE; CJK
  script on Latin page → DECORATIVE; wide-thin terminal-less → NARRATION;
  short uppercase + emphasis → SOUND_EFFECT; uppercase interjection regex →
  EXPRESSION; else DIALOGUE. OcrRegion untouched (no schema/confidence
  exists — classifier designed for future metadata swap-in).
- Tests: SpeechRegionClassifierTest 7 cases + SpeechRegionFilterTest +
  SpeechPipelineTest.

Pipeline wiring (TtsPlaybackController.acquireSentences):
- result.regions → SpeechPipeline.toSpeakableSentences (classify → filter →
  clean → segment). Cleanup BEFORE segmentation (punct runs normalize
  first); post-segment punct-only slices dropped. Original OCR data
  immutable (dictionary/tap-overlay/search see full regions).

Phase D — OCR exclusion zones:
- DB: 18.sqm + ocr_exclusion_zones.sq (manga_id FK CASCADE, chapter_id FK
  CASCADE, source_id, page_index, scope TEXT, normalized REAL rect, enabled,
  created_at). verifySqlDelightMigration GREEN.
- Domain: OcrExclusionZone model, OcrExclusionScope {PAGE,CHAPTER,MANGA,
  SOURCE}, repository + interactors (Get/Add/Delete/SetEnabled +
  subscribeForManga/Source, awaitForChapter/awaitAll).
- Matching: OcrExclusionMatcher (pure overlap test on normalized coords;
  PAGE scope matches pageIndex only; enabled filter at query + matcher).
  Applied ONLY in TtsPlaybackController (speech layer) — tap/dictionary
  OCR intentionally unaffected (spec: exclusion vs speech-cleanup distinct).
- Selection UI: reuse drag-select infra (SelectionAction.SaveExclusionZone);
  captureExclusionZoneSelection resolves captures → bitmap dims → normalized
  rect → Dialog.ExclusionZoneScope (page/chapter/manga/source choice) →
  VM saveExclusionZone (launchNonCancellable). Manage sheet
  (OcrExclusionZonesSheet) lists zones w/ enable toggle + delete.
  Entry: Reader settings → Read aloud tab → "OCR & text recognition"
  (enable toggle + add zone + manage). Pref gate
  pref_tts_ocr_exclusions_enabled (default true) controls lookup.
- Tests: OcrExclusionMatcherTest 5 cases (overlap, page-scope, cross-scope,
  normalized-coords scaling).

Phase G — Backup/restore:
- Backup model +BackupOcrExclusionZone list @ProtoNumber(107) (additive proto
  — old backups compatible, unknown fields survive).
- Creator: OcrExclusionZonesBackupCreator gated on options.appSettings.
  Restore: OcrExclusionZoneRestorer — dedupe (same manga/chapter/page/rect),
  skip zones w/ invalid scope name, per-zone try/catch (FK miss → logged skip,
  no restore abort). All new prefs (pref_tts_*, pref_dictionary_reader_*,
  pref_feed_items, pref_tts_voice_profiles) auto-included via
  PreferenceBackupCreator (plain keys, no __PRIVATE_ prefix).

Phase E — Voice profiles:
- TtsVoiceProfile @Serializable {id,name,enginePackage,voiceName,languageTag,
  rate,pitch}; stored as JSON in one pref (pref_tts_voice_profiles) via
  getObjectFromString; active id pref (pref_tts_active_voice_profile).
- ReadAloudSettingsScreenModel: saveVoiceProfile (snapshot current config),
  deleteVoiceProfile, applyVoiceProfile (writes component prefs + rate/pitch
  + engine.setEnginePackage + reload). UI: "Voice profiles" group in
  SettingsReadAloudScreen — rows w/ apply + delete icon, save row w/ name
  dialog (default name "N% · voice").

Phase F — 3x speech rate:
- Rate slider ranges 50..300 (reader tab + main settings). Engine-side no
  clamp (Android TTS accepts float; framework clamps internally per engine).
- TtsPlaybackBar: speed chip ("1x") + DropdownMenu (0.5/0.75/1/1.25/1.5/
  1.75/2/2.5/3) → writes ttsSpeechRate pref → controller live collector
  applies to engine (same path as Phase 10A rate/pitch).

Phase H — Dictionary navigation cleanup:
- AUDIT RESULT: bottom-nav DictionaryTab (word lookup) vs More→Dictionaries
  (SettingsDictionaryScreen: import/manager/popup-style) = DIFFERENT
  features, no merge. Lookup tab moved: DictionaryTab DELETED →
  DictionaryLookupScreen (Voyager Screen, same DictionarySearchScreenModel
  content) opened from More tab "Dictionary" row (MenuBook icon) above the
  "Dictionaries" manager row.
- Dictionary interaction settings (SettingsDictionaryScreen, "Dictionary
  style" group): reader tap lookup toggle (pref_dictionary_reader_tap_lookup,
  default ON — gates shouldHandleCachedOcrRegionTaps) + auto-search toggle
  (pref_dictionary_reader_auto_search, default ON — gates OcrResultOverlay
  initial LaunchedEffect search; off = popup opens with query filled, user
  searches manually). Manual long-press OCR lookup unaffected.

Phase I — Feed:
- FeedTab replaces DictionaryTab in bottom nav (HomeScreen TABS + Tab.Feed;
  Dictionary Tab sealed member replaced). Label "Feed", Icons.Outlined.Feed.
- Model: FeedItem @Serializable {sourceId, listing POPULAR/LATEST, enabled};
  FeedPreferences (JSON pref pref_feed_items — auto-backed-up).
- FeedScreenModel: GetEnabledSources (stub-filtered) for picker; per enabled
  feed fetch page 1 via source.getPopularManga/getLatestUpdates(1) →
  toDomainManga → NetworkToLocalManga (GlobalSearch fan-out precedent,
  async per feed, per-feed result state). Feeds reactive via pref changes().
- FeedScreen: LazyVerticalGrid, per-feed header (source name, listing label,
  up/down reorder, enable switch, delete) + MangaComfortableGridItem grid
  → MangaScreen(id, true). Add dialog: source list (supportsLatest gates
  Latest listing) + listing choice. Empty state w/ add CTA.

GATES GREEN 2026-09-01 (devcontainer JDK17, -Xmx4g, both volumes):
  spotlessApply → spotlessCheck + testDebugUnitTest +
  verifySqlDelightMigration + :app:assembleDebug ALL BUILD SUCCESSFUL.
  NOT run: device pass, release build, restore round-trip test.

Follow-ups (deliberately deferred, ponytail):
- Feed: no prefetch/paging (page 1 per feed only); no per-feed refresh
  control; source-uninstalled state shows error text.
- Exclusion zone rect visualization on-page (currently list-only coords).
- Speech classifier ML upgrade path = swap classify() internals only.
```

```text
[COMPLETED 2026-09-01 — OCR exclusion system redesign, UNCOMMITTED]

Rule model (single ocr_exclusion_zones table, preserved):
- match_type ∈ {ZONE, WORD, PHRASE, COMBINED} + match_text + rule_name columns.
- ZONE: pure rect, scope PAGE only for new rules (chapterId+pageIndex match).
  Legacy CHAPTER/MANGA/SOURCE ZONE rows (page_index NULL): DORMANT in matcher
  (rect was drawn on one page; blind application was the bug), visible+deletable
  with "legacy" hint in manage UI. Data preserved.
- WORD: global standalone-token match (Unicode isLetterOrDigit tokens,
  case-insensitive; "ion" ≠ "combination").
- PHRASE: global normalized substring (case-insensitive, \s+→space collapse).
- COMBINED: rect overlap AND phrase match within scope (PAGE=chapter+page,
  CHAPTER=any page of chapter, MANGA, SOURCE). Location+text = safe wide scope.

Migration 19.sqm (verifySqlDelightMigration NOT yet run — user runs it):
- Full table rebuild: drops manga_id FK (global WORD/PHRASE rules need
  manga_id=0 which would violate old FK), keeps chapter_id FK CASCADE,
  adds match_text/match_type/rule_name in same column order as .sq.
- UPDATE chapter_id=NULL for legacy MANGA/SOURCE rows (chapter-delete cascade
  used to destroy them — bug 3 fix). .sq mirror-updated identically.

Matcher (OcrExclusionMatcher.kt): new ExclusionMatchContext(mangaId, sourceId,
chapterId, pageIndex) API; applyExclusions(zones, context). PAGE zone now also
requires chapterId match (was page-only). Enabled check at matcher + query.

Repository/interactors: +subscribeAll, +getZonesForSpeech(mangaId, sourceId,
chapterId) = enabled AND (text rules OR manga/chapter/source match); insert
gains matchType/matchText/ruleName/enabled (enabled was hardcoded 1 — restorer
bug 5 fix). Controller acquireSentences → awaitForSpeech + context call.

Reader save flow (bug 2/3 fix): scope dialog now offers PAGE (pure zone) or
CHAPTER/MANGA/SOURCE (COMBINED; required non-blank match text, Save disabled
while blank; AlertDialog + OutlinedTextField, same Dialog.ExclusionZoneScope
entry so both ReaderActivity when-branches unchanged). saveExclusionZone:
MANGA/SOURCE → chapter_id NULL (no cascade); pageIndex always stored.

New Settings → OCR exclusions screen (SettingsOcrExclusionsScreen, plain
Voyager Screen per DictionaryScreen pattern — not searchable): words/phrases/
zones sections, add-word/add-phrase AlertDialogs, per-row switch+delete,
rule hint, legacy marker. ScreenModel: SettingsOcrExclusionsScreenModel
(StateScreenModel over subscribeAll). Entry: SettingsMainScreen row after
Read aloud (Icons.Outlined.Block) + Destination.OcrExclusions id 5 + both
SettingsScreen when branches. Reader manage sheet stays manga-scoped
(subscribeForManga) + type labels + legacy hint.

Backup/restore: BackupOcrExclusionZone +@ProtoNumber(11/12/13) matchType
(default "ZONE")/matchText/ruleName — old backups decode. Restorer: dedupe
now compares scope+matchType+matchText too (scope-omission bug fixed),
unknown matchType skipped, enabled round-trips via new insert param.

i18n (base only): 17 new keys (ocr_exclusions_screen_title, _summary,
_type_zone/word/phrase/combined/_legacy, _section_words/phrases/zones,
_add_word/_add_phrase, _match_text_label, _rule_hint, _invalid_text, _empty).

Tests: OcrExclusionMatcherTest rewritten — 13 cases (page-zone page+chapter
match, other-page survive, non-overlap, WORD standalone/case/unicode,
PHRASE whitespace/case/substring, COMBINED chapter any-page + rect/text
required + wrong scope kept, COMBINED manga/source, legacy dormant all
scopes, disabled, any-match, normalized coords).

GATES NOT RUN (host gradle forbidden this session; user runs gates later).
```

```text
[COMPLETED 2026-09-01 — TTS duplicate-speech fix set (4 root causes), UNCOMMITTED]

Root causes treated as established facts (user-confirmed investigation):
OCR seam duplicates surviving IoU<0.45 + resumeIndex race + utterance-id
collision + old-job stop() flushing new utterance.

1. Domain text-dedup (mihon.domain.tts.speech/SpeechPipeline.kt):
   - dedupeOverlappingDuplicates(regions) inserted BEFORE segmentation in
     toSpeakableSentences: drops later regions whose normalized text
     (trim + \s+→space + lowercase) EXACTLY duplicates an earlier kept region
     AND strictly overlaps its bbox (AABB, boxesOverlap helper). Duplicate
     text in disjoint bubbles survives (legitimate). Hoisted key regex.
   - New pure fns: boxesOverlap(a,b), boundingBoxIoU(a,b) (normalized floats).
   - Benefits ALL engines; GlensOcrEngine untouched (per plan).
2. resumeIndex race (TtsPlaybackController.runPlayback):
   - resumeIndex=sentenceIndex stays ONLY at pre-dispatch (pause DURING
     speech re-speaks current sentence = desired).
   - After spoke==true: resumeIndex=sentenceIndex+1 BEFORE suspension points
     → pause between utterances resumes at NEXT (never re-speaks completed).
   - sentenceIndex>=sentences.size on (re)entry now advances the page instead
     of resetting to 0 (fixes pause-in-advance-window re-speaking whole page).
   - stepBy resumeIndex=newIndex kept consistent.
3. Utterance-id collision: utteranceId now appends monotonic per-controller
   AtomicLong dispatch counter ("p${page}_s${sentence}_c${n}") — unique per
   dispatch, page/sentence still traceable. Engine-side finally now removes
   pendingUtterances entry only when identity matches (===) ITS completion —
   stale zombie dispatch can't unhook a newer dispatch's callback.
4. Zombie stop() flush: resume() now calls engine.stop() after
   playbackJob?.cancel() when phase was Playing/LoadingPage (old job could
   have in-flight utterance). rebuildQueueForUserNavigation + stepBy already
   called engine.stop(); pause() calls its own. engine.stop() idempotent.
5. Traceability logs (no text content, rules §7): "TTS dispatch id=...
   textLen=... textHash=..." at speak dispatch; "TTS page=N dedup dropped=X
   regions" (only when X>0) in acquireSentences (dedup applied there once,
     pipeline stays dedup-free at call site).
6. Tests: SpeechPipelineDedupTest 9 cases (dup+overlap→later dropped /
   dup+disjoint→kept / different-text+overlap→kept / case+whitespace dup→
   dropped / triple→first kept / empty unchanged / pipeline single-sentence
   / IoU identical=1 / IoU disjoint=0). 9/9 GREEN.

DEVIATIONS from brief (all flagged, user-WIP interactions):
- Brief's line numbers were stale: on-disk controller had evolved (phase A–I
  + exclusion redesign landed uncommitted). Verified every hunk against
  CURRENT source before editing; exclusion code now uses
  ExclusionMatchContext/awaitForSpeech — acquireSentences dedup log
  adapted to that shape.
- User's uncommitted exclusion-zone WIP had a COMPILE BREAK blocking all
  gates: OcrExclusionMatcher.kt:30 called zone.matchesRegion(...) but fn
  is top-level matchesRegion(zone,...). Fixed (one word, no semantic
  change). Also fixed: ReaderViewModel.kt:1055 (text:String? null-check
  → isNullOrEmpty guard), SettingsOcrExclusionsScreenModel.kt (missing
  kotlinx.coroutines.launch import).
- User WIP spotless violations (continuation indents) hand-applied per
  ktlint's exact output: OcrExclusionMatcher.kt (2 hunks),
  ReaderActivity.kt (ExclusionZoneScopeDialog arg indent).
- User WIP SpeechCleaner tests had 2 WRONG expectations contradicting their
  own impl/docs: "..." with ellipsisToPause=false → null (their own
  doc: punctuation-only skip drops standalone runs), and pipeline slice
  "I don't know," (segmenter trims slices by design, memory 2026-08-25).
  Expectations corrected; no production code changed for these two.
- Resumed resumeIndex=size fallthrough: NOT in brief; required to avoid
  re-speaking whole page when pause lands between last utterance and page
  advance. advanceFromPolicy path preserves auto-turn arbitration.

GATES GREEN 2026-09-01 (devcontainer JDK17, -Xmx4g, both volumes):
  spotlessCheck + testDebugUnitTest BUILD SUCCESSFUL in 2m 30s (173 domain
  tests + all suites; DedupTest 9/9). :domain:compileReleaseKotlin +
  :app:compileDebugKotlin BUILD SUCCESSFUL 4m 7s. verifySqlDelightMigration
  NOT needed (no DB change). NOT run: assembleDebug, device pass.

Files edited: SpeechPipeline.kt; SpeechPipelineDedupTest.kt (new);
  TtsPlaybackController.kt; AndroidTtsEngine.kt;
  OcrExclusionMatcher.kt (WIP compile+ktlint fix);
  ReaderViewModel.kt (WIP null-check fix);
  SettingsOcrExclusionsScreenModel.kt (WIP import fix);
  ReaderActivity.kt (WIP ktlint indent);
  SpeechCleanerTest.kt + SpeechRegionFilterTest.kt (WIP expectation fix).
Deferred issue #3b (hands-off duplicate speech) → addressed by this set;
device re-verify on affected manga still pending.
```

```text
[COMPLETED 2026-09-01 — Reader UI polish set (Issues 5/6/8), UNCOMMITTED]

Feature 1 (Issue 5) — Stop button during Preparing/LoadingPage:
- TtsPlaybackBar.kt Preparing/LoadingPage branch: status Text gained
  Modifier.weight(1f) + trailing Stop IconButton (Icons.Outlined.Stop, reuses
  tts_action_stop key, same pattern as PlaybackContent/ErrorContent stop) wired
  to existing onStop. UI-only; ReaderActivity onStop wiring untouched.

Feature 2 (Issue 6) — OCR drag-select toggle:
- ReaderPreferences: ocrTextSelectionEnabled ("reader_ocr_text_selection",
  default true, Controls region beside longTapOcr).
- ReaderBottomBar: showOcrButton param (default true) wraps DocumentScanner
  IconButton. Chain: ReaderActivity (both call sites, collectAsState) →
  ReaderAppBars (param + forward) → ReaderBottomBar.
- Long-press OCR path (longTapOcr pref) untouched. enterOcrMode NOT gated —
  exclusion-zone flow shares enterSelectionMode with its own SelectionAction.

Feature 3 (Issue 8) — Read Aloud button toggle (same pattern):
- ReaderPreferences: readAloudButtonEnabled ("reader_read_aloud_button",
  default true). ReaderBottomBar showReadAloudButton param wraps
  RecordVoiceOver IconButton. Wired identically at both call sites.
- Settings: SettingsReaderScreen getActionsGroup gains both SwitchPreferences
  after longTapOcr row. Core actions (mode/orientation/crop/settings) always
  visible.

i18n (base only): pref_reader_ocr_text_selection, pref_reader_read_aloud_button
(2 new keys; stop reuses existing tts_action_stop).

GATES NOT RUN (host gradle forbidden this session; user runs gates later).
Files: TtsPlaybackBar.kt, ReaderBottomBar.kt, ReaderAppBars.kt,
ReaderActivity.kt, ReaderPreferences.kt, SettingsReaderScreen.kt,
i18n base strings.xml.
```

```text
[COMPLETED 2026-09-01 — POST-DEVICE-TEST AUDIT SET (session: repeated speech +
exclusion redesign + adaptive prefetch + stop-during-prepare + reader prefs +
toolbar toggles + ellipsis pause), UNCOMMITTED — ALL GATES GREEN]

Session flow: evidence collection (5 parallel explore agents, full audit of
TTS pipeline/OCR exclusions/prefetch/minibar/toolbar/settings/backup) →
root-cause report → implementation via 4 parallel agents + orchestrator edits
→ verification. No fresh device logs existed for the reported test pass
(newest .device-pass log = tts-10a-test.log 2026-08-31); evidence = source +
prior logs. Root causes established in code, not guessed.

Issue 2 (repeated speech) — 4 root causes, all fixed:
- OCR seam/text duplicates (primary): domain text-dedup (see dedup block
  below) + traceability logs.
- resumeIndex race: fixed (see dedup block).
- utterance-id collision: unique per-dispatch ids (see dedup block).
- zombie stop() flush: resume() engine.stop() guard (see dedup block).

Issue 1 (ellipsis "dot dot dot"): SpeechCleaner ellipsisToPause option —
dot-runs (2+) and "…" → ", " spoken pause, toggleable
(pref_tts_ellipsis_to_pause default true, checkbox in Read aloud reader tab,
key pref_tts_ellipsis_to_pause). SpeechCleanerTest +2 cases; SpeechRegionFilterTest
pipeline expectation updated ("I don't know," — segmenter trims slices).

Issue 3 (OCR exclusion redesign): full redesign (see exclusion-redesign
block above) — ZONE/WORD/PHRASE/COMBINED match types, 19.sqm table rebuild
(drops manga_id FK, adds match_text/match_type/rule_name, legacy MANGA/
SOURCE chapter_id→NULL), matcher ExclusionMatchContext API, scope dialog
combined-rule text requirement, Settings→OCR exclusions screen, backup
proto 11/12/13 + restorer dedupe/enabled fixes.

Issue 4 (high-speed prefetch): speed-adaptive depth (see prefetch block).

Issue 5 (stop during Preparing/LoadingPage): TtsPlaybackBar spinner branch
gains trailing Stop IconButton (Icons.Outlined.Stop, tts_action_stop) —
stop() was fully functional controller-side; pure UI gap.

Issue 6 (OCR drag-select toggle): reader_ocr_text_selection pref (default
true) → showOcrButton param chain ReaderActivity→ReaderAppBars→
ReaderBottomBar; SettingsReaderScreen Actions group SwitchPreference.
Long-press path (longTapOcr) untouched — separate mechanism.

Issue 7 (dictionary popup toggle): ALREADY EXISTING —
pref_dictionary_reader_tap_lookup gates shouldHandleCachedOcrRegionTaps
(PHASE H, Settings→Dictionary). Regression-verified only, no change needed.

Issue 8 (toolbar customization): reader_read_aloud_button pref (default
true) → showReadAloudButton chain, same pattern as Issue 6. Core actions
(mode/orientation/crop/settings) always visible. No drag-drop ordering.

Issue 9 (dictionary in More): untouched per directive.
Issue 10 (Feed): untouched per directive.

GATES GREEN 2026-09-01 (devcontainer JDK17, -Xmx4g, both volumes, run by
ORCHESTRATOR directly, all four):
  spotlessCheck BUILD SUCCESSFUL 42s;
  testDebugUnitTest + verifySqlDelightMigration BUILD SUCCESSFUL 4m9s
    (19.sqm validated vs .sq schema; new suites green: OcrExclusionMatcherTest
    14/14, SpeechCleanerTest 11/11, SpeechPipelineDedupTest 9/9);
  :app:assembleDebug BUILD SUCCESSFUL 3m52s.
NOT run: device pass (user), assembleRelease.

Device-pass checklist for user (from audit task spec):
1. Speech cleanup incl. "..." pause + dialogue preservation.
2. Repeated-speech: multiple chapters/pages, hands-off; look for
   "TTS dispatch id=... textHash=" + "dedup dropped=" logs; zero repeats.
3. OCR exclusions: page zone; chapter/manga/source COMBINED (text required);
   word "ion" vs "combination"; phrase; legacy dormant rows visible.
4. High-speed 1x/2x/2.5x/3x: "TTS prefetch start pages=N..N+2 rate=3.0";
   stalls bounded; no LoadingPage gaps at 3x on cached pages.
5. Stop during Preparing/LoadingPage: bar now shows Stop; cancel clean;
   next session starts fresh.
6. OCR selection toggle OFF → button gone.
7. Dictionary popup: toggle OFF → no popups/no "No dictionary is enabled".
8. Toolbar customization: toggles hide/show optional buttons.
9. Backup/restore round-trip: voice profile + word + phrase + zone +
   combined rule all survive; disabled rules stay disabled.
```

Deferred issues (LOW PRIORITY, revisit post-build):
  - #3b hands-off duplicate words/phrases: intermittent, manga-specific, never
    reproduced under instrumentation. ADDRESSED 2026-09-01 by the duplicate-
    speech fix set (domain text-dedup + resumeIndex race + unique utterance
    ids + zombie-stop guard); device re-verify on the affected manga is the
    remaining confirmation step.

Remaining:
- User review + commit: 2026-09-01 Phases A–I change set is COMMITTED
  (c70e32252); the 2026-09-01 POST-DEVICE-TEST AUDIT SET (this session)
  is UNCOMMITTED awaiting user commit.
- Device pass of the new features (speech cleanup behavior, exclusion-zone
  selection on manga+webtoon, speed popup, profiles, Feed tab, dictionary
  More-tab relocation) + this session's audit checklist (9 items, listed
  in the audit-set block above).
- (DONE 2026-08-29: Phase 9 COMPLETE — leak fix device-verified 0 APPLICATION
  LEAKS; battery measurement done: no post-exit drain, app fg CPU ~1mAh/min,
  screen dominates; evidence battery-sample.log + battery-tts-session.log +
  batterystats-app.txt.)
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
- Phase 10A set UNCOMMITTED (Tasks 1-6): TtsVoicePreferences (+test),
  TtsEngine, AndroidTtsEngine, DomainModule, PreferenceModule,
  SettingsReadAloudScreen (+screen model dir), SettingsSearchScreen,
  SettingsMainScreen, Constants.kt, SettingsScreen.kt, MainActivity.kt,
  ReaderSettingsDialog.kt, ReadAloudPage.kt, ReaderActivity.kt (both
  dialog call sites), i18n base strings.xml (18 TTS keys), docs/* (Task 6
  documentation updates: prd/architecture/design/phase/memory)
- docs/memory.md (this session record)

All fix sets prior to Phase 10A COMMITTED: fff9583f0/872c55397/8cb320e7c
  (P0/P1+scroll), 41200022e (z-order), be31edb71 (action logging +
  reportFailure), 80deac5b8 (prefetch-DNS docs), 5c7d2cc2c (leak fix +
  LeakCanary core).
```

## Recently changed files

```text
2026-08-30  docs/prd.md                                  Phase 10A T6: new §6 (capabilities,
                                                        user flow, preview policy, fallback
                                                        chain, 10B deferred list)
2026-08-30  docs/architecture.md                        Phase 10A T6: §5 TtsEngine contracts +
                                                        AndroidTtsEngine config pipeline +
                                                        preview policy; §7 file tables
2026-08-30  docs/design.md                              Phase 10A T6: new §13 settings screen
                                                        spec (groups, states, metadata labels)
2026-08-30  docs/phase.md                               Phase 10A T6: 10A entry IN_PROGRESS,
                                                        Phase 10 → 10B backlog rework
2026-08-30  app .../settings/screen/SettingsMainScreen.kt    Phase 10A T5: Read aloud row
            (AutoMirrored VolumeUp) after Reader item
2026-08-30  core/common/.../Constants.kt                    Phase 10A T5: SHORTCUT_VOICE_SETTINGS
2026-08-30  app .../ui/setting/SettingsScreen.kt            Phase 10A T5: Destination.ReadAloud
            id 4 + both when branches
2026-08-30  app .../ui/main/MainActivity.kt                 Phase 10A T5: handleIntentAction
            voice-settings deep-link case
2026-08-30  app .../reader/settings/ReadAloudPage.kt        Phase 10A T5: pitch slider removed,
            advanced-voice-settings nav row added
2026-08-30  app .../reader/settings/ReaderSettingsDialog.kt  Phase 10A T5: onOpenVoiceSettings
            param + pass-through
2026-08-30  app .../ui/reader/ReaderActivity.kt             Phase 10A T5: both dialog call
            sites deep-link to MainActivity
2026-08-30  i18n base strings.xml                            Phase 10A T5: tts_advanced_voice_
            settings key (T4's 17 keys also uncommitted)
2026-08-30  app .../settings/screen/SettingsReadAloudScreen.kt + Phase 10A T4 (Task 4 report)
            app .../ui/setting/readaloud/*
2026-08-30  domain + app TtsVoicePreferences/TtsEngine/AndroidTtsEngine Phase 10A T1/T3
2026-08-29  docs/memory.md, docs/phase.md             Accuracy restoration: fd52a613a docs
            commit had reverted memory/phase to pre-leak-fix state (stale
            repo hash, prefetch-fix marked unverified, Finding #5 records
            deleted) — restored + updated for committed reality
2026-08-29  app .../ui/reader/ReaderActivity.kt       Import order re-fixed (spotlessApply):
            fd52a613a had moved ioCoroutineScope + kotlinx.coroutines.cancel
            imports to non-ktlint positions → spotlessCheck failed
2026-08-29  (committed 5c7d2cc2c) LeakCanary fix: ReaderViewModel.onCleared
            onFocusEvent=null; ReaderActivity ioCoroutineScope cancel on dispose;
            build.gradle.kts LeakCanary core debug builds
2026-08-28  app .../ui/reader/tts/TtsPlaybackController.kt Prefetch failure no longer kills session (reportFailure gate); RUN4 analysis
2026-08-28  app .../ui/reader/tts/TtsPlaybackController.kt Action-level DEBUG logs: pause/resume/stop/stepBy/focus-loss (COMMITTED be31edb71)
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

 5. RESOLVED | TTS leak (2026-08-28 LeakCanary pass, fixed 2026-08-29) | Old
    assessment "bounded, self-healing small retention" was WRONG: LeakCanary
    showed ~100MB retained after reader exit (TTS service native GC root →
    AndroidTtsEngine singleton → onFocusEvent lambda → dead controller →
    ReaderViewModel → destroyed ReaderActivity). FIXED in 5c7d2cc2c:
    ReaderViewModel.onCleared sets ttsEngine.onFocusEvent = null AFTER
    stop()/shutdown() (engine-init-failure mid-session path NOT affected —
    clearing happens only on ViewModel teardown, not engine.shutdown());
    next session re-registers in controller init. Plus ReaderActivity cancels
    settingsScreenModel.ioCoroutineScope on dispose. DEVICE-VERIFIED 2026-08-29
    (build 0.4.0-8241, leak-full.log): LeakCanary 0 APPLICATION LEAKS after two
    reader TTS sessions + exits. CLOSED.

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
Unit tests:        PASS (2026-09-03, full testDebugUnitTest — OCR exclusion
                    regression-fix set #2; OcrExclusionMatcherTest 27/27 incl.
                    token-concat PHRASE both directions, ・ separator,
                    cross-region pin; BoxMostlyInsideTest 7/7; all prior
                    suites green)
Integration tests: none run (existing androidTest suites are device-gated/@Ignore)
UI tests:          none exist in repo
Device tests:      Phase 8 script COMPLETE (steps 1–15 executed +
                      user-confirmed 2026-08-28). Phase 9 device items
                      COMPLETE (leak re-verify 0 leaks; battery PASS).
                      Phase 10A Task 7 COMPLETE (user-confirmed 2026-08-31).
                      PENDING: post-device-test audit set device pass
                      (9-item checklist) + regression-fix #2 A–J checklist
                      (memory.md 2026-09-03 block).
Lint:              spotlessCheck PASS (2026-09-03, regression-fix #2)
Build:             :app:assembleDebug PASS (2026-09-03, 0.5.1-8255 installed
                    on SM_M066B 12:54)
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
Date:     2026-09-03 (OCR exclusion regression-fix set #2, run by orchestrator)
Command:  ./gradlew spotlessCheck testDebugUnitTest verifySqlDelightMigration
          :app:assembleDebug (docker devcontainer JDK17, -Xmx4g, both volumes)
Result:   ALL GREEN — spotlessCheck 35s; testDebugUnitTest + verifySqlDelight-
          Migration BUILD SUCCESSFUL 2m36s; :app:assembleDebug BUILD
          SUCCESSFUL 2m44s. Change set: recognizeText LEGACY/FAST→Glens
          redirect, boxMostlyInside selection filter, PHRASE token-concat
          matching, WebtoonTransitionHolder detach-cancel leak fix,
          single-flight exclusion detect + min-crop guard. UNCOMMITTED;
          device verification A–J pending (APK 0.5.1-8255 installed).
```

## Last verified test

```text
Date:     2026-09-03 (OCR exclusion regression-fix set #2)
Command:  ./gradlew testDebugUnitTest (docker devcontainer, JDK 17, -Xmx4g,
          both volumes)
Result:   BUILD SUCCESSFUL — full suite green incl. extended
          OcrExclusionMatcherTest 27/27 (NEW: phrase space-rule vs punct-OCR,
          punct-rule vs space-OCR, ・ separator, cross-region phrase pin,
          punctuation-only rule, Dis\ncord concat-run pin; all prior 20 kept
          green) + BoxMostlyInsideTest 7/7 (new file).
```

---

## Agent handoff

```text
Last agent:                 opencode (2026-09-03 — OCR exclusion regression
                            #2: diagnostic-first investigation + fixes)
Date:                       2026-09-03
Task completed:             Phase 0 fresh device evidence (on-device logcat,
                            LegacyOcrEngine crop proof, exclusion-miss log,
                            LeakCanary 12.3s freeze + 131.5s analysis, device
                            DB dump via run-as+python sqlite3). 3 parallel
                            explore subagents + orchestrator verification.
                            5 evidence-confirmed root causes fixed: crop-OCR
                            engine asymmetry (LEGACY JP model → Glens
                            redirect), any-overlap selection leak
                            (boxMostlyInside 50%), PHRASE punctuation
                            asymmetry (token-concat fallback), 184.7MB
                            WebtoonTransitionHolder leak (detach-cancel),
                            detect pile-up (single-flight + min-crop guard).
                            LeakCanary-config approach investigated and
                            DROPPED (API cannot prevent dumps; real leak
                            fixed instead). Tests matcher 20→27 +
                            BoxMostlyInsideTest 7. All 4 gates green.
                            APK 0.5.1-8255 installed 12:54.
Current task:               Verify-log analysis DONE (see verify2 block in
                            Completed work — pipeline healthy, 2 new defects:
                            main-thread page-list fetch, Glens-502 scan-path
                            fallback gap). Pending: user review + commit of
                            regression #2 fix set.
Next recommended task:      Analyze verify log after user run: expect OCR(glens)
                            on crop fallback (never OCR(legacy)), exclusion
                            rules=X excluded>0 for word/phrase pages, 0
                            LeakCanary dumps after reader exit (holder leak
                            gone), no detect pile-up on rapid re-select.
                            Then user review + commit.
Files safe to modify:       docs/* ; app reader/tts/settings/viewer ; data OCR;
                            i18n base strings.xml
Known risks:                PHRASE spanning two OCR regions stays un-excluded
                            (documented v1 semantics, pinned by test); mid-
                            session rule adds apply from next page acquire;
                            sub-10px selection crops return empty detect
                            field by design; Known issue #13 LeakCanary
                            Toast/Popup debug noise unchanged (ignored-
                            matcher API cannot suppress dumps).
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
