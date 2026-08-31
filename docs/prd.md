# Yomihon — Product Requirements Document

> Status: living document. Describes WHAT the product must provide.
> Technical HOW lives in `docs/architecture.md`; engineering rules in `docs/rules.md`;
> roadmap in `docs/phase.md`; visual spec in `docs/design.md`; actual progress in `docs/memory.md`.
> Source of truth for all claims is the repository itself.

---

## 1. Product overview

### 1.1 What Yomihon is

Yomihon is a free, open-source (Apache-2.0) **Android manga / manhwa / manhua reader**
("Full-Featured Reader for Android"). It is a community fork of
[Mihon](https://github.com/mihonapp/mihon) (Tachiyomi lineage), enhanced with
**on-device OCR and language-learning tooling**: text recognition on manga pages,
Yomitan-style dictionary lookups, and one-click Anki card creation.

- Application ID: `app.yomihon` (code namespaces remain `eu.kanade.tachiyomi.*`,
  `mihon.*`, `tachiyomi.*` — see `docs/rules.md`).
- Requires Android 8.0+ (`minSdk 26`); `targetSdk 36`, `compileSdk 37`.
- Current release: v0.5.0 (version code 26).

### 1.2 Who it is for

1. **Manga/webtoon readers** who want a configurable local/online reader with
   library management, downloads, tracking, and categories.
2. **Language learners reading Japanese/Korean/Chinese material**, who need OCR,
   dictionary lookup, furigana-aware processing, and Anki export while reading.

### 1.3 Primary use cases

| Use case | How it works today |
|---|---|
| Discover & read manga from online sources | Browse tab → extensions provide catalogue sources → browse/search → add to library |
| Read downloaded/local content | Local source (folders/CBZ/EPUB) or DownloadManager output |
| Organize a library | Categories, sorting, filters, scheduled library updates |
| Look up unknown words on a page | Tap-to-lookup on cached OCR results, or long-press/drag region selection → OCR → dictionary popup/sheet |
| Create Anki cards while reading | Term-group export from the OCR result overlay |
| Background chapter scanning | OcrScanJob/OcrScanManager queue scans whole chapters into the OCR cache |
| Experimental panel-by-panel reading | Panel detection model drives pager navigation on tall pages |

### 1.4 Core user experience

- Bottom-navigation app shell (Library / History / Updates / Browse / More) built
  with Jetpack Compose + Voyager screens; light/dark/AMOLED themes with 13 color
  schemes plus Android 12+ dynamic color (Monet).
- A highly configurable reader: LTR/RTL/vertical pagers, webtoon (continuous and
  paged), multiple tap zones/navigation kinds, crop borders, color filters,
  brightness overlay, dual-page split, panel navigation.
- Reading progress, history, and tracking sync (MyAnimeList, AniList, Kitsu,
  MangaUpdates, Shikimori, Bangumi, Hikka, Komga/Kavita/Suwayomi as sources-side).

### 1.5 Current capabilities (verified in repository)

See §2 — every item there was verified against actual source files.

### 1.6 Planned capabilities

The single major planned feature is **Read-Aloud TTS**: reading a page's OCR text
aloud in correct manga reading order, with sentence-level controls and automatic
page/chapter progression. Full requirements in §3; implementation plan approved at
the scope level in root `architect.md` / `architect-2.md`. No other feature is
currently planned by this documentation system.

---

## 2. Existing functionality (verified)

Everything below exists in the repository today. Do not re-implement any of it;
extend it (see `docs/rules.md`).

### 2.1 Reading

- **Reader** (`app/.../ui/reader/`): `ReaderActivity` + `ReaderViewModel`
  (single `StateFlow<State>` + `Channel<Event>` to the activity).
- **Viewers**: `L2RPagerViewer`, `R2LPagerViewer`, `VerticalPagerViewer`,
  `WebtoonViewer` (continuous + paged); `ReadingMode.toViewer()` factory.
- **Page loading**: `ChapterLoader` dispatches to `HttpPageLoader` (priority queue,
  ChapterCache-first, preload of next 4 pages), `DownloadPageLoader`,
  `ArchivePageLoader`, `EpubPageLoader`, `DirectoryPageLoader`.
- **Navigation**: page/chapter movement via `moveToPageIndex`, `loadNextChapter`,
  `loadPreviousChapter`; adjacent-chapter preloading; process-death restore via
  `SavedStateHandle`.
- **Experimental panel-by-panel reading** driven by the panel-detection model
  (`PanelDetectionRepositoryImpl`, `data/src/main/assets/panel_detector/model.tflite`).

### 2.2 Sources & extensions

- **Source API** (`:source-api`): `Source`, `CatalogueSource`, `HttpSource`,
  `ParsedHttpSource`, filters, `SManga`/`SChapter`/`Page` models.
- **Local source** (`:source-local`): folders, CBZ/ZIP/RAR archives, EPUB.
- **Extension system** (`app/.../extension/`): `ExtensionManager`,
  `ExtensionLoader` (child-first classloader over installed APKs),
  PackageInstaller/Shizuku installers, remote extension repository via
  `ExtensionApi` + `ExtensionStoreRepository`. Stub sources persisted for missing
  extensions.

### 2.3 OCR (major differentiator)

- **Engines** (`data/src/main/java/mihon/data/ocr/`):
  - `LegacyOcrEngine` — on-device LiteRT encoder/decoder (manga-ocr style),
    assets under `app/src/main/assets/ocr/` (gitignored; fetched in CI).
  - `FastOcrEngine` — on-device TFLite, assets under `app/src/main/assets/ocr_fast/`.
  - `GlensOcrEngine` — network "Google Lens"-style engine producing ordered
    regions with vertical-text detection and furigana filtering.
  - `OwOcrEngine` — user-configured self-hosted WebSocket endpoint.
  - Fallback chain between engines (`pref_use_fallback_models`); per-engine
    serialization via `OcrEngineLocks` + `PrioritizedTaskQueue` (HIGH/NORMAL).
- **Domain models** (`domain/.../mihon/domain/ocr/model/OcrModels.kt`):
  `OcrRegion(order, text, boundingBox, textOrientation)`; normalization helpers
  `flattenOcrTextForQuery`, `normalizeOcrTextForDisplay`; `TextPostprocessor`
  collapses whitespace, expands `…`→`...`, half→full width.
- **Cache**: separate SQLDelight DB `ocr_cache.db` (`ocr_pages`, `ocr_regions`),
  wrapped by `OcrCacheStore`; latest-model-wins per page; regions sorted by
  `region_order`.
- **Interactors**: `ScanPageOcr`, `GetCachedPageOcr`, `WithOcrScanSession`,
  `OcrProcessor`, cache clear/size queries.
- **Background scanning**: `OcrScanManager` (persisted queue) + `OcrScanJob`
  (WorkManager foreground DATA_SYNC) + `OcrChapterScanner` (resolves page bitmaps
  via `OcrPageSourceResolver`: downloads first, then local, then remote fetch).
- **Reader integration**: bottom-bar OCR button and long-press enter selection
  mode; drag-select region → `processOcrRegion` → dictionary result dialog;
  tap-to-lookup on cached results (`ActiveOcrOverlaySession`, in-view bubble
  rendering with WCAG-contrast colors).

### 2.4 Dictionary & language learning

- Yomitan-style dictionary import/storage (7 tables in the main database:
  `dictionaries`, `dictionary_terms`, `dictionary_kanji`, tags/meta…),
  deinflectors (Japanese/English), term search backends, glossary HTML conversion.
- **Dictionary audio**: `DictionaryAudioPlayer` (+Impl, fire-and-forget
  `MediaPlayer`) plays pronunciation clips fetched by `DictionaryAudioRepository`
  (jpod101/Wiktionary, SHA-256-keyed cache). This is the **only audio code that
  exists today** — there is no TTS anywhere in the codebase.
- **Anki export**: `AnkiDroidRepository`, `AddDictionaryCard`, note dedup search.

### 2.5 Library, history, updates, downloads, backup

- Library with categories, display flags, sort modes; `LibraryUpdateJob`
  scheduled updates; metadata update job.
- History with resume points; Updates view (SQL view over chapters/mangas).
- `DownloadManager`/`Downloader` with queue, `DownloadCache`, page-list building.
- Backup create/restore (protobuf models), automatic backups to StorageManager dirs.
- Upcoming screen + Glance home-screen widget (`:presentation-widget`).

### 2.6 Settings & preferences

- `PreferenceStore` (SharedPreferences-backed, Flow-exposing) with per-feature
  preference classes registered in `PreferenceModule`: Reader, Library, Downloads,
  Track, Backup, Security, Privacy, UI, Source, Network, Storage, Base,
  **OcrPreferences**, Dictionary, AnkiDroid, Updates.
- Settings screens under `ui/setting/*` (appearance, library, reader, downloads,
  tracking, security, advanced…) and reader-specific settings dialog with tabs
  (Reading mode / General / Color filter).

### 2.7 Other platform features

- Tracking services (11 trackers), deep links, WebView screens, migration flow,
  crash screen, about/libraries, updater (flag-gated `-Penable-updater`),
  telemetry (flag-gated `-Pinclude-telemetry`, Firebase or compile-time noop).

---

## 3. TTS requirements (planned feature: "Read Aloud")

**Goal:** read a page's OCR text aloud in correct manga reading order with
sentence-level playback controls, then automatically advance pages/chapters so a
reader can listen hands-free.

**Approved scope decisions** (from root `architect.md` / `architect-2.md`, binding):

1. Playback is **reader-bound**: pauses when leaving the reader / screen off /
   activity stop. **No foreground service, no MediaSession, no new permissions.**
2. The current sentence is visualized **in a mini-bar only** (no on-image bbox
   highlight in v1).
3. Initial engine = **Android native `android.speech.tts.TextToSpeech`** behind an
   abstraction. Repository analysis confirms no better existing mechanism
   (the only audio code is the unrelated one-shot `DictionaryAudioPlayer`).
4. **Zero new Gradle dependencies** — framework APIs only (`TextToSpeech`,
   `AudioManager`/`AudioFocusRequest`; minSdk 26 covers both).

### 3.1 Functional requirements

**F1 — Text acquisition**
- F1.1 Use cached OCR results first (`GetCachedPageOcr`).
- F1.2 On miss, resolve the page bitmap through the existing
  `OcrPageSourceResolver` pipeline and run `ScanPageOcr` (which also persists to
  the OCR cache). Recycle bitmaps deterministically.
- F1.3 Prefetch OCR for page N+1 while page N speaks; cancel prefetch on page change.

**F2 — Sentence extraction & ordering**
- F2.1 Consume `OcrRegion`s in stored list order (`region.order`). Never re-sort
  in the segmenter (must match tap-highlight behavior).
- F2.2 Bubble boundary = hard boundary: never merge sentences across regions.
- F2.3 Split within a region only on terminal punctuation
  `。！？!?‼⁇⁉⁈`, keeping punctuation attached; trailing remainder becomes the
  final fragment; skip blank regions.
- F2.4 `.`/`...` are NOT terminal (engines expand `…` to `...`).
- F2.5 Reuse existing normalization (`TextPostprocessor`, `flattenOcrTextForQuery`);
  do not duplicate cleanup logic.

**F3 — Playback controls**
- Play, pause, resume/toggle, stop, next sentence, previous sentence.
- Automatic progression: next page at last sentence (configurable auto page turn);
  next chapter at last page (configurable, default off); clean Finished state at
  end of content.
- Manual navigation wins: if the user swipes mid-playback, rebuild the queue for
  the new page and keep playing (no bouncing back).

**F4 — Voice/language/rate/pitch**
- Language availability preflight (Japanese required for JP content) with an
  actionable error if no voice is installed.
- Speech rate (0.5–2.0, default 1.0) and pitch (default 1.0) settings applied live.
- Voice selection follows the system TTS default in v1; explicit voice picker
  landed in Phase 10A (see §6).

**F5 — Reader integration**
- Entry point(s) consistent with existing OCR entry points (bottom-bar icon
  patterned after the OCR button).
- Mini playback bar modeled on `OcrLoadingIndicator` (bottom pill, slide-up
  animation) showing current sentence text, position x/y, prev/play-pause/next/stop.
- Keep-screen-on honored while playing (preference, default true).
- State surfaced through `ReaderViewModel.State.ttsState` so existing Compose
  plumbing works unchanged.

**F6 — Lifecycle safety**
- Pause on `Activity.onStop` (home, screen off, app switch); stay paused on return.
- Survive rotation/config change (ViewModel-scoped).
- Stop + engine shutdown on reader finish/`onCleared()`; no leaks, no callbacks
  after teardown.
- Audio focus: gain on start, pause on transient loss, auto-resume on regain,
  stay paused on permanent loss; abandon on stop/pause.

**F7 — Error handling**
- Engine init failure, missing language, utterance errors (retry once then pause),
  empty pages (skip), end-of-content — each maps to a defined state or user-facing
  message. TTS failures must never crash the reader.

**F8 — Extensibility**
- All speech goes through a framework-free `TtsEngine` interface in `:domain`;
  the Android implementation stays isolated in `:app`. Future engines (local
  neural, cloud) plug in without touching reader logic.

### 3.2 Non-functional requirements

| Area | Requirement |
|---|---|
| Performance | Cached-page start latency < ~500 ms to first utterance; uncached pages show visible Preparing/LoadingPage states instead of freezing; prefetch hides legacy-model latency |
| Reliability | No crash paths from TTS/OCR failures; bounded queues (one page segment list at a time); deterministic bitmap recycling |
| Maintainability | Decision logic (segmentation, advancement) pure and unit-tested in `:domain`; orchestration thin |
| Accessibility | Controls ≥48 dp touch targets, content descriptions, non-color state indicators, scalable text via sp |
| Offline capability | Fully functional offline with local OCR models + system TTS voices; GLENS-dependent flows degrade with existing connection errors |
| Privacy | No new data leaves the device (system TTS is on-device unless the user's chosen engine says otherwise); no logging of spoken content beyond debug |
| Battery | No background execution after leaving reader; no wake locks beyond existing keep-screen-on flag; no polling loops |
| Memory | No bitmap retention across suspension points; no audio caching in v1 (system TTS latency is tens of ms) |
| Storage | Zero new persistent storage beyond existing OCR cache |
| Compatibility | minSdk 26 APIs only (`AudioFocusRequest` OK); behavior verified on phone portrait/landscape and tablet layouts |
| Security | No new permissions, no exported components, no secrets |

### 3.3 Constraints

- **Platform**: Android 8.0+; targetSdk 36 background-execution rules forbid
  silent background audio without a service — hence reader-bound scope.
- **Existing architecture**: Injekt DI, Voyager + Compose presentation layer,
  `StateFlow` state + `Channel<Event>` VM→Activity pattern, interactors in
  `:domain`, implementations in `:data`/`:app`. TTS must follow these patterns.
- **Dependencies**: none added for v1 (framework-only).
- **Compatibility**: must not change existing OCR/reader behavior; feature is
  additive and independently toggleable (off until started).
- **Licensing**: Apache-2.0 project; no GPL-incompatible dependencies (none added).
- **Repository conventions**: strings via moko-resources base `strings.xml`
  (snake_case keys); ML assets gitignored; CI order spotless → unit tests →
  SQLDelight migration check → assembleRelease.

### 3.4 Success criteria (measurable)

TTS v1 is production-ready when ALL of the following hold:

1. `./gradlew :domain:test` passes including new `SentenceSegmenterTest` and
   `TtsAdvancePolicyTest` covering: multi-sentence bubbles, remainder fragments,
   `.`/`...` non-terminal handling, blank-region skipping, `‼⁇⁉⁈` glyphs,
   half-width `!?`, order/bbox preservation, and every advance-policy branch
   (autoTurn off, autoNextChapter off/on, last page ±next chapter, empty page/chapter).
2. `./gradlew spotlessCheck` and `./gradlew testDebugUnitTest` pass;
   `./gradlew :app:assembleDebug` builds.
3. On-device verification script passes: play a GLENS-scanned chapter
   (cached-first path), an uncached page (scan-on-demand path), and a webtoon
   chapter; verify play/pause/resume, prev/next sentence, swipe-away arbitration
   (user navigation wins), page turn at last sentence, chapter transition,
   end-of-content stop, missing-Japanese-voice error, rate/pitch changes take effect.
4. Lifecycle matrix verified: onStop pauses; rotation continues; finish() shuts
   down the engine with no leaked callbacks (no `Exception` in logcat after exit);
   audio-focus transient loss pauses and regain resumes.
5. Leaving the reader stops all audio within ~1 s; battery shows no background
   CPU use after exit.
6. No new permissions requested; APK diff adds no new external dependencies.

---

## 4. Functional requirements summary (by category)

- **Existing (do not regress)**: everything in §2. Any change to reader/OCR
  behavior requires explicit justification in `docs/memory.md` before proceeding.
- **TTS (v1)**: F1–F8 above.
- **Future (explicitly out of v1 scope)**:
  - Foreground-service background playback + MediaSession + notification controls.
  - Explicit voice picker UI — **landed in Phase 10A** (see §6); per-language
    voice memory still future.
  - Cloud TTS providers, local neural TTS engines (new `TtsEngine` impls).
  - On-image bbox highlighting of the currently spoken region.
  - Audio caching for high-latency cloud engines.
  - Porting Glens-style reading-order sort into the local Legacy/Fast scan path
    (known gap documented in `docs/architecture.md` §OCR caveats).

---

## 5. Non-goals for v1 TTS

- Background playback outside the reader (deliberate product decision).
- Karaoke-style word highlighting on images.
- Translation or dubbing.
- Recording/exporting audio.

---

## 6. Phase 10A — Advanced system TTS voice configuration

**Status: IN_PROGRESS** (code complete, gates green 2026-08-30; device
verification pending — not marked complete until the on-device pass runs).

**Goal:** let users choose WHICH system TTS engine, voice, and language reads
their manga, calibrate rate/pitch, and audition the result with a preview —
while reader playback behavior stays identical to v1.

### 6.1 Capabilities (implemented, Tasks 1–5)

- **Engine discovery & selection**: installed engine list with labels +
  system-default marker; persisted `pref_tts_engine_package` ("" = system
  default); switching engines rebuilds the TTS instance on next initialize.
- **Language / locale selection**: `pref_tts_language_tag` full-tag pref driving
  two pickers (language = all distinct tags; locale = region variants filtered
  to the selected language's prefix; locale row enabled only once a language is
  chosen). Selecting a language clears the pinned voice.
- **Voice selection**: per-engine voice list (`pref_tts_voice_name`), filtered
  to the selected language; selecting a voice live re-applies it via an
  initialize call on the running instance.
- **Voice calibration**: rate + pitch sliders (50–200 %) reusing the single
  global `pref_tts_speech_rate` / `pref_tts_pitch` pair from v1 — one pair for
  all voices (deliberate; see 6.4).
- **Preview**: plays a hardcoded sample ("Hello. This is a preview of the
  selected reading voice.") with the CURRENT rate/pitch/voice; a second tap
  stops it.
- **Persistence + fallback chain**: three string prefs, default "" (= system
  default voice). Pure `resolveVoiceSelection` fallback: selected voice still
  valid → Voice; else selected language valid → Language; else SystemDefault
  (engine's own default voice). Stale pref values (uninstalled engine, removed
  voice) render their raw persisted value in the picker, never "null".
- **Reset**: one action clears all three prefs + resets the engine package +
  reloads; confirmation toast.
- **Entry points**: Settings root row "Read aloud & voice" (after Reader,
  VolumeUp icon); reader quick-settings Read aloud tab keeps playback toggles
  minimal (rate slider + auto page turn/auto next chapter/keep-screen-on) and
  links to the full screen via a deep-link (`SHORTCUT_VOICE_SETTINGS` intent →
  `SettingsScreen(Destination.ReadAloud)`); screen registered in settings
  search.

### 6.2 User flow

Settings → Read aloud & voice → (screen auto-initializes the TTS engine;
failure shows a retry row) → pick engine → pick language → pick locale → pick
voice → adjust rate/pitch → Preview row to audition → open reader → Read
aloud; mid-session preference changes apply at the next pause/resume or
playback step (engine re-reads fresh prefs on every initialize).

### 6.3 Preview policy (approved resolution)

- ONE shared engine instance for preview and reader narration; preview stops
  any previous utterance first, then acquires focus and speaks the sample.
- Reader narration always wins: the controller speaks with `QUEUE_FLUSH`, so
  starting narration interrupts an in-flight preview.
- An interrupted narration (e.g. preview fired during playback) pauses
  honestly via the existing controller state machine — no fake "still
  playing" state.
- Preview applies the current rate/pitch/voice from screen state so it
  auditions exactly what playback will sound like.

### 6.4 Deferred to Phase 10B (backlog)

- Per-voice rate/pitch profiles (rate/pitch stay one global pair; voice-
  specific overrides only if a future need fits cleanly).
- Cloud/neural TTS providers — requires credentials, billing/cost, network,
  privacy, streaming/downloaded-audio, caching, and latency-management
  design; new `TtsEngine` impls via Injekt swap, reader untouched.
- Local neural engines; downloadable AI voices.
- Expressive narration (emotion/prosody): needs provider SSML/prosody/emotion
  controls — pitch/rate sliders are NOT expressive narration; never fake
  emotion via random pitch.
