# Yomihon Read-Aloud TTS — Implementation Plan

## Context

Yomihon (Mihon fork) already OCRs manga pages end-to-end: engines in `mihon.data.ocr` (GLENS/OWOCR/Legacy/Fast), reading-order bubble models in `mihon.domain.ocr.model.OcrModels`, per-page SQLDelight cache (`ocr_cache.sq`), background scanning (`OcrScanManager`/`OcrScanJob`), and a tap-to-lookup overlay in the reader. What's missing is speech: this feature reads a page's OCR text aloud in Japanese reading order with sentence controls, then auto-advances pages/chapters. **Zero TTS/audio-focus/MediaSession code exists today** (only `DictionaryAudioPlayerImpl`, a fire-and-forget MediaPlayer). No new Gradle dependencies are required — everything is framework (`android.speech.tts.TextToSpeech`, `android.media.AudioManager`) on top of existing repo plumbing.

**Confirmed scope decisions** (user-approved): playback is **reader-bound** (pauses on leaving reader / screen off — no foreground service, no MediaSession, no new permissions) and the current sentence is visualized **in a mini-bar only** (no on-image bbox highlight in v1).

---

## Architecture

### Pipeline (data flow)

```
[▶ tapped] → ReaderViewModel.startTts()
  → TtsPlaybackController.start()          (viewModelScope)
      ├─ AndroidTtsEngine.initialize()     (main thread, ≤5s timeout, CompletableDeferred from OnInitListener)
      ├─ preflight isLanguageAvailable(JAPANESE)  → else Error(NoJapaneseVoice)
      ├─ requestAudioFocus(AUDIOFOCUS_GAIN)
      └─ per page N (loop):
           1. TEXT   GetCachedPageOcr.await(chapterId, N)          ← hits ocr_cache.sq
              miss → OcrPageSourceResolver.resolve(manga, chapter)  (AutoCloseable, IO)
                     → getPageInput(N).openBitmap() → Bitmap.toOcrImage()
                     → WithOcrScanSession { ScanPageOcr.await(...) }  ← also persists into cache
                     → recycle bitmap in finally
           2. SEGMENT List<OcrRegion> → SentenceSegmenter.toTtsSentences()   (pure, :domain)
           3. SPEAK   engine.speak("pN-sI", sentence.text)  (suspends until onDone/onError;
                       UtteranceProgressListener completes a keyed CompletableDeferred)
           4. ADVANCE TtsAdvancePolicy.computeAdvance(...) → NextPage | NextChapter | Finish | PauseAtPageEnd
                NextPage    → Event.TtsAdvancePage(N+1) → Activity.moveToPageIndex(N+1)
                              controller records expectedChapterId/pageIndex, enters LoadingPage;
                              confirmed by forwarded onPageSelected
                NextChapter → Event.TtsAdvanceChapter → Activity.loadNextChapter()
                Finish      → state=Finished, abandonFocus
           [prefetch] when page N starts speaking, launch ensure-OCR job for N+1 (same chapter);
                      cancel on any page change
```

### Key design rules

- **Ordering**: consume `OcrRegions` in list order (`region.order`). GLENS orders vertical-bubbles right-to-left/top-to-bottom then horizontal top-to-bottom and strips furigana geometrically (`GlensOcrEngine.filterRuby`). Do **not** re-sort in the segmenter (would diverge from tap-highlight behavior). Known gap: Legacy/Fast local path (`OcrRepositoryImpl.scanLocally`) uses detection order without RTL sort — document it; don't paper over it.
- **Normalization**: nothing new needed. Engines postprocess via `TextPostprocessor` (whitespace collapse between non-JP chars, half→full width); `flattenOcrTextForQuery`/`normalizeOcrTextForDisplay` exist in `OcrModels.kt`. Note: `…` arrives expanded to `"..."`, so `.`/`...` are **not** terminal punctuation.
- **Segmentation**: never merge across regions (bubble boundary = hard boundary). Split within a region on terminal punct `。！？!?‼⁇⁉⁈` keeping punct attached; trailing remainder = final fragment; skip blank regions.
- **Navigation arbitration**: controller never touches `Viewer`. It emits events; user swipes mid-playback produce an `onPageSelected` mismatch → rebuild queue for the new page and keep playing (user wins, no bouncing). Advance-confirm timeout ~10 s → Paused + toast.
- **End of content**: last page done AND `viewerChapters.nextChapter == null` → Finished. (`moveToPageIndex` indexes real `chapter.pages`; transition/InsertPage adapter items are not in that list, so index math is safe.)
- **Caching**: none beyond the existing OCR cache (system-TTS latency is tens of ms; revisit only with a cloud engine).

### State model

```
// in TtsPlaybackController (app module)
enum TtsPhase { Idle, Preparing, LoadingPage, Playing, Paused, Finished, Error }
data class TtsPlaybackState(phase, pageIndex=-1, sentenceIndex=0, sentenceCount=0, currentSentenceText: String? = null, error: TtsError? = null)
val state: StateFlow<TtsPlaybackState>
```
`ReaderViewModel.State` gains `val ttsState: TtsPlaybackState = Idle`; VM collects controller flow into `mutableState.update { copy(ttsState = …) }` so all existing Compose plumbing works unchanged.

### Threading

- **Main**: all `TextToSpeech` calls (needs a Looper), callbacks (only touch deferreds), StateFlow recomposition, viewer movement via events handled in activity.
- **IO**: bitmap decode, resolver, `ScanPageOcr`/`GetCachedPageOcr` (repository internally serializes engines via `PrioritizedTaskQueue` + per-engine mutexes — don't add another lock layer).
- Controller body on `viewModelScope`; never hold bitmaps across suspension points except inside one acquire/finally-recycle block.

### Lifecycle matrix

| Event | Playing | Paused/Idle |
|---|---|---|
| Activity onStop (home/screen-off/app switch) | **pause** (+abandon focus) | – |
| onResume after stop | stays paused (manual restart) | – |
| Rotation/config change | continues (VM survives) | – |
| finish()/onDestroy + onCleared | `stopTts()` → engine.shutdown() in finally | same |
| Process death | idle restore (reading position restored normally) | – |
| Chapter switch mid-play | LoadingPage → sync on onPageSelected; timeout → Paused | – |
| Audio focus transient loss / regain | pause / auto-resume | – |
| Permanent focus loss | paused, manual resume | – |

---

## New files

| Path | Contents |
|---|---|
| `domain/src/main/java/mihon/domain/tts/engine/TtsEngine.kt` | Framework-free interface: `initialize(): Boolean`, `japaneseAvailable: Boolean`, `suspend speak(utteranceId, text): Boolean`, `setSpeechRate/setPitch(Float)`, `acquireFocus()/abandonFocus()` + focus-loss callback hook, `stop()`, `shutdown()`. Mirrors `DictionaryAudioPlayer` interface-in-domain precedent |
| `domain/src/main/java/mihon/domain/tts/SentenceSegmenter.kt` | `data class TtsSentence(text, regionOrder, boundingBox, textOrientation)` + `fun List<OcrRegion>.toTtsSentences(): List<TtsSentence>` (pure) |
| `domain/src/main/java/mihon/domain/tts/TtsAdvancePolicy.kt` | `sealed/enum AdvanceAction` + pure `computeAdvance(currentPageIndex, totalPages, pageHasText, autoTurn, autoNextChapter, nextChapterExists)` — every navigation decision lives here so it's unit-testable |
| `domain/src/main/java/mihon/domain/tts/service/TtsPreferences.kt` | `class TtsPreferences(preferenceStore)` mirroring `OcrPreferences`: `ttsSpeechRate()` Float 0.5–2.0 def 1.0, `ttsPitch()` Float def 1.0, `ttsAutoPageTurn()` Boolean def true, `ttsAutoNextChapter()` Boolean def false, `ttsKeepScreenOn()` Boolean def true (`PreferenceStore.getFloat` exists) |
| `domain/src/test/java/mihon/domain/tts/SentenceSegmenterTest.kt` | Multi-sentence bubble; remainder w/o terminal punct; `.`/`…`-runs NOT terminal; blank region skipped; `‼⁇⁉` single-glyph terminals; half-width `!?`; order+bbox preservation |
| `domain/src/test/java/mihon/domain/tts/TtsAdvancePolicyTest.kt` | autoTurn off→PauseAtPageEnd; autoNext off→Finish at chapter end; last page+next→NextChapter; last page+none→Finish; empty page→skip; empty chapter→Finished |
| `app/src/main/java/eu/kanade/tachiyomi/data/tts/AndroidTtsEngine.kt` | `class AndroidTtsEngine(context: Application) : TtsEngine` — sibling of `DictionaryAudioPlayerImpl`. Construct `TextToSpeech` on main, `UtteranceProgressListener.onDone/onError` complete keyed `CompletableDeferred`s; `stop()` fails pending deferreds then `tts.stop()`; `AudioManager.requestAudioFocus`/`AudioFocusRequest` (minSdk 26-safe) |
| `app/src/main/java/eu/kanade/tachiyomi/ui/reader/tts/TtsPlaybackController.kt` | Orchestration per the flow above; constructor takes scope, `TtsEngine`, `TtsPreferences`, `GetCachedPageOcr`, `ScanPageOcr`, `WithOcrScanSession`, resolver factory, and advance-callbacks `(pageIdx)->Unit`/`() -> Unit`; exposes `start(page)/pause/resume/toggle/nextSentence/prevSentence/stop/onPageSelected(chapterId,pageIndex)/onFocusLost(...)` |
| `app/src/main/java/eu/kanade/presentation/reader/TtsPlaybackBar.kt` | Bottom pill modeled on `OcrLoadingIndicator` (AnimatedVisibility slide-up, `Alignment.BottomCenter`): current sentence text large, "x/y", prev/play-pause/next/stop IconButtons; error state shows retry |
| `app/src/main/java/eu/kanade/presentation/reader/settings/ReadAloudSettingsPage.kt` | Settings tab content using existing `CheckboxItem`/`SliderItem` specs |

## Modified files

1. `app/src/main/java/eu/kanade/domain/DomainModule.kt` (~line 282) — `addSingletonFactory<TtsEngine> { AndroidTtsEngine(get<Application>()) }`.
2. `app/src/main/java/eu/kanade/tachiyomi/di/PreferenceModule.kt` (~line 59) — `addSingletonFactory { TtsPreferences(get()) }`.
3. `app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt`:
   - lazy-create controller on first `startTts(currentPage)` (inject engine/prefs/interactors via `Injekt.get()` defaults);
   - `State.ttsState` field + collect controller flow into `mutableState`;
   - forward `page` from `onPageSelected()` (~line 458) to controller;
   - new Events in sealed `Event`: `TtsAdvancePage(index)`, `TtsAdvanceChapter`, `TtsError(msgRes)`, `TtsNoTextFound`;
   - `stopTts()` from `onActivityFinish()` (~line 277) and `onCleared()` (scope cancellation triggers shutdown in controller's `finally`).
4. `app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt`:
   - handle new events in existing `eventFlow.collectLatest` block (~line 326): `TtsAdvancePage→moveToPageIndex(it)` (~1111), `TtsAdvanceChapter→loadNextChapter()` (~1122), others → toast;
   - render `TtsPlaybackBar` in `ContentOverlay` beside `OcrLoadingIndicator` (~line 885);
   - `override fun onStop()` → `viewModel.pauseTts()`;
   - keep-screen-on: effective flag = pref OR (`ttsKeepScreenOn && phase==Playing`) in the existing collection (~1597), reusing `setKeepScreenOn` (~1657).
5. `app/src/main/java/eu/kanade/presentation/reader/settings/ReaderSettingsDialog.kt` — append 4th tab "Read aloud" to `tabTitles` + `when(page)` branch.
6. `app/src/main/java/eu/kanade/presentation/reader/appbars/ReaderBottomBar.kt` + `ReaderAppBars.kt` call sites — optional entry icon `Icons.Outlined.RecordVoiceOver` with `onClickTts` threaded exactly like `onClickOcr`.
7. `i18n/src/commonMain/moko-resources/base/strings.xml` — snake_case keys: `action_tts_read_aloud`, `tts_play`, `tts_pause`, `tts_previous_sentence`, `tts_next_sentence`, `tts_stop`, `tts_preparing`, `pref_tts_speed`, `pref_tts_pitch`, `pref_tts_auto_page_turn`, `pref_tts_auto_next_chapter`, `pref_tts_keep_screen_on`, `tts_error_no_japanese_voice`, `tts_error_init_failed`, `tts_finished`, `tts_no_text_found`.

**Reused unchanged (no duplication)**: `OcrRepository`/SQLDelight cache via interactors, `OcrPageSourceResolver`/`OcrPageInput`/`OcrPageBitmapDecoder`, `Bitmap.toOcrImage()`, `OcrModels` + normalization free functions, `PrioritizedTaskQueue` serialization, `WithOcrScanSession`, `Viewer.moveToPage` via existing activity methods, `Notifications` infra (nothing added), Injekt modules, preference-store plumbing. `DictionaryAudioPlayer` left as-is (term-audio is unrelated one-shot clip playback).

## Dependencies

**None.** Framework-only (`android.speech.tts`, `android.media.AudioManager`); minSdk 26 covers `AudioFocusRequest`. No Gradle/manifest changes (no service, no permission).

## Testing strategy

- Pure JUnit5+Kotest tests (house stack, e.g. `PrioritizedTaskQueueTest`) for `SentenceSegmenterTest` and `TtsAdvancePolicyTest` — these carry all decision logic.
- Untested by design (framework-thin): `AndroidTtsEngine`, controller orchestration (decisions delegated to tested policy), composables, activity event wiring. No Robolectric exists in the repo; don't introduce it.
- Manual verification: emulator/device with Japanese TTS data installed → play a GLENS-scanned chapter (cached-first path), uncached page (scan-on-demand path), webtoon mode, swipe-away-mid-speech arbitration, end-of-content stop, missing-Japanese-voice preflight error, rate/pitch settings.

## Risks / compatibility

- **Local Legacy/Fast OCR ordering**: detection-order output means vertical manga reads out of order with those models. Document; recommend GLENS for read-aloud. Don't silently reorder (would diverge from tap behavior). Optional follow-up: port Glens-style ordering into `scanLocally`.
- **targetSdk 36**: nothing added → zero foreground-service policy surface. Background playback later = `FOREGROUND_SERVICE_MEDIA_PLAYBACK` + service + MediaSession + notification channel (-95x range; -9xx taken by OCR) — nothing here blocks it.
- **Dual-page split / InsertPage**: worst case a one-tick stall while the insert half displays; acceptable.
- **Device TTS variance**: some devices ship no Japanese voice → explicit preflight + actionable error instead of gibberish.
- **Legacy-model OCR latency** (seconds/page): cached-first + prefetch of N+1 + visible Preparing/LoadingPage phases hide most of it.

## Verification

1. `./gradlew :domain:test` — segmenter + policy suites green.
2. `./gradlew :app:assembleDebug` — compiles.
3. On-device script: open a chapter → tap ▶ in bottom bar or mini-bar → speech starts from first bubble; verify pause/resume, prev/next sentence, swipe-away arbitration, page-turn at last sentence, chapter transition, end-of-content stop; repeat in webtoon mode; toggle each new pref; confirm screen stays awake while playing with keep-screen-on pref off.
