# Yomihon TTS (read-aloud) implementation plan

Full plan in final agent response. Summary of decisions:

- D1: Reader-bound playback only. No FGS, no MediaSession. Pause on Activity.onStop.
- D2: `TtsEngine` interface in :domain, `AndroidTtsEngine` in :app (data/tts), bound in DomainModule (which lives in :app). CompletableDeferred bridging, no callbackFlow.
- D3: Pure sentence segmenter in :domain operating on List<OcrRegion>, never merging across regions. Terminal punct: 。！？!?‼⁇⁉⁈ only (region text already has … expanded to "..." by TextPostprocessor).
- D4: TtsPlaybackController owned by ReaderViewModel (viewModelScope). State merged into ReaderViewModel.State.ttsState. Advances issued via new Events handled by existing activity moveToPageIndex/loadNextChapter. Expected-page matching to distinguish self-advance vs user nav. OCR: cached-first, then OcrPageSourceResolver full-page decode -> ScanPageOcr (caches). Prefetch next page OCR.
- D5: Mini-bar highlight (TtsPlaybackBar, OcrLoadingIndicator template), no on-image overlay in v1.
- D6: No TTS cache.
- D7: TtsPreferences (rate, pitch, auto page turn, auto next chapter, keep screen on) + new ReaderSettingsDialog tab.
- D8: Preflight JP voice check, utterance error retry-once-then-pause, empty page skip, no process-death restore, audio focus in engine.

New files: domain tts engine/segmenter/policy/prefs + tests, app AndroidTtsEngine, app TtsPlaybackController, presentation TtsPlaybackBar, settings ReadAloudSettingsPage.
Modified: DomainModule, PreferenceModule, ReaderViewModel, ReaderActivity, ReaderSettingsDialog, ReaderBottomBar (entry icon), i18n strings.xml.
Zero new dependencies (android.speech.tts is framework).
