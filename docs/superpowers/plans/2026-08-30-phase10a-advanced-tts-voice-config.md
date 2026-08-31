# Phase 10A — Advanced System TTS Voice Configuration: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the verified Phase 1–9 Read-Aloud pipeline with engine/voice/language discovery + selection, rate/pitch calibration in a dedicated settings screen, voice preview, persistent prefs, and reader→settings navigation — without regressing TEXT→SEGMENT→SPEAK→ADVANCE.

**Architecture:** Extend `TtsEngine` with discovery + engine-selection contracts; `AndroidTtsEngine` applies engine/voice/language via cached-field + re-apply-on-initialize pattern (mirrors existing rate/pitch fields). New `TtsVoicePreferences` (:domain) persists selections; pure `resolveVoiceSelection` implements the fallback chain (Voice → Language → SystemDefault). New `SettingsReadAloudScreen` (SearchableSettings) + `ReadAloudSettingsScreenModel` (StateScreenModel) host discovery, pickers, preview, reset. Reader "Read aloud" tab stays minimal, swaps pitch for an "Advanced voice settings" row that deep-links to the new screen via `MainActivity` intent handoff + new `Destination.ReadAloud`. Preview uses the SINGLE shared engine (QUEUE_FLUSH semantics); no second `TextToSpeech` ever. Rate/pitch remain the existing single global pair (`TtsPreferences`) — per-voice overrides deferred to Phase 10B per spec PART 6's YAGNI allowance.

**Tech Stack:** Kotlin, Compose, Voyager, Injekt, moko-resources i18n, `android.speech.tts` (framework only — zero new deps, zero new permissions).

**Spec:** Phase 10A task spec (session message) + `docs/phase.md` Phase 10 backlog + `docs/prd.md`.

## Global Constraints

- Zero new dependencies; zero new permissions (rules.md §5/§6).
- `AndroidTtsEngine` remains the ONLY `TextToSpeech` touchpoint (rules.md §6).
- Existing `TtsPreferences` keys/behavior unchanged (regression guard for controller L148-149 live collectors).
- Fallback: Selected Voice → Selected Locale/Language → System Default → safe error state (spec PART 9). Never crash on removed engine/voice/locale.
- Preview never conflicts with reader narration (spec PART 7): single engine + QUEUE_FLUSH; preview stops previous utterance first; reader narration always wins by flush; interrupted narration pauses honestly (existing controller behavior).
- Reader settings tab stays minimal (spec PART 5); full config in main app settings (spec PART 4).
- i18n: base `strings.xml` only, snake_case (rules.md §4).
- No `app.yomihon.*` packages (AGENTS.md). UI composables in `eu.kanade.presentation.*`, screen models in `eu.kanade.tachiyomi.ui.*`.
- Gates via devcontainer image `vsc-yomihon-e24e3bd7e46d5060e88796634a865cb501faf4766a48662dbc474a380427c674:latest`, `-Xmx4g`; record results in `docs/memory.md`.
- Domain tests: JUnit5 + Kotest `shouldBe` (precedent `TtsAdvancePolicyTest.kt`).
- No DB changes → no `.sqm`, no `verifySqlDelightMigration`.
- Reader dialog ColorFilter dim-hack (`ReaderSettingsDialog.kt:49`, `currentPage == 2`) untouched — no tab reordering.
- Controller/VM untouched (architectural rule): engine re-applies config on every `initialize()`; controller already calls `initialize()` on every resume/step/rebuild (`TtsPlaybackController.kt:179,296,569`), so mid-session pref changes apply at next pause/resume — no controller edits needed.
- Future-provider extensibility (spec PARTS 11/12): interface extension only; document cloud-TTS + expressive-speech requirements as Phase 10B notes in docs (Task 7). No OpenAI/cloud TTS in this change set; no fake emotional speech.

---

### Task 1: Domain — `TtsVoicePreferences` + pure `resolveVoiceSelection` fallback logic

**Files:**
- Create: `domain/src/main/java/mihon/domain/tts/service/TtsVoicePreferences.kt`
- Test: `domain/src/test/java/mihon/domain/tts/service/TtsVoicePreferencesTest.kt`

**Interfaces:**
- Consumes: `tachiyomi.core.common.preference.PreferenceStore` (already in :domain; precedent `TtsPreferences.kt:3`).
- Produces (Tasks 2, 3, 5 consume):
  - `class TtsVoicePreferences(preferenceStore)` with `ttsEnginePackage(): Preference<String>` (key `pref_tts_engine_package`, default `""`), `ttsVoiceName(): Preference<String>` (`pref_tts_voice_name`, `""`), `ttsLanguageTag(): Preference<String>` (`pref_tts_language_tag`, `""`), and `reset()`.
  - `sealed interface TtsVoiceSelection { data object SystemDefault; data class Voice(enginePackage, voiceName, languageTag); data class Language(enginePackage, languageTag) }`
  - `fun resolveVoiceSelection(selectedEnginePackage: String, selectedVoiceName: String, selectedLanguageTag: String, availableEnginePackages: Set<String>, availableVoiceNames: Set<String>, availableLanguageTags: Set<String>): TtsVoiceSelection`

**Fallback rules (exact):**
1. Voice non-empty AND in `availableVoiceNames` → `Voice(selectedEnginePackage, selectedVoiceName, selectedLanguageTag)`.
2. Else language tag non-empty AND in `availableLanguageTags` → `Language(selectedEnginePackage, selectedLanguageTag)`.
3. Else → `SystemDefault`.

- [ ] **Step 1: Write failing test**

`domain/src/test/java/mihon/domain/tts/service/TtsVoicePreferencesTest.kt` — 5 tests:

```kotlin
package mihon.domain.tts.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TtsVoicePreferencesTest {

    @Test
    fun `empty selections mean system default`() {
        resolveVoiceSelection(
            selectedEnginePackage = "",
            selectedVoiceName = "",
            selectedLanguageTag = "",
            availableEnginePackages = setOf("com.google.android.tts"),
            availableVoiceNames = setOf("en-us-x-i-local"),
            availableLanguageTags = setOf("en-US"),
        ) shouldBe TtsVoiceSelection.SystemDefault
    }

    @Test
    fun `selected voice still available is used`() {
        resolveVoiceSelection(
            selectedEnginePackage = "com.google.android.tts",
            selectedVoiceName = "en-us-x-i-local",
            selectedLanguageTag = "en-US",
            availableEnginePackages = setOf("com.google.android.tts"),
            availableVoiceNames = setOf("en-us-x-i-local", "en-gb-x-i-local"),
            availableLanguageTags = setOf("en-US", "en-GB"),
        ) shouldBe TtsVoiceSelection.Voice(
            enginePackage = "com.google.android.tts",
            voiceName = "en-us-x-i-local",
            languageTag = "en-US",
        )
    }

    @Test
    fun `removed voice falls back to language`() {
        resolveVoiceSelection(
            selectedEnginePackage = "com.google.android.tts",
            selectedVoiceName = "en-us-x-removed",
            selectedLanguageTag = "en-US",
            availableEnginePackages = setOf("com.google.android.tts"),
            availableVoiceNames = setOf("en-gb-x-i-local"),
            availableLanguageTags = setOf("en-US", "en-GB"),
        ) shouldBe TtsVoiceSelection.Language(
            enginePackage = "com.google.android.tts",
            languageTag = "en-US",
        )
    }

    @Test
    fun `removed voice and language falls back to system default`() {
        resolveVoiceSelection(
            selectedEnginePackage = "com.google.android.tts",
            selectedVoiceName = "en-us-x-removed",
            selectedLanguageTag = "en-IN",
            availableEnginePackages = setOf("com.google.android.tts"),
            availableVoiceNames = setOf("en-gb-x-i-local"),
            availableLanguageTags = setOf("en-US", "en-GB"),
        ) shouldBe TtsVoiceSelection.SystemDefault
    }

    @Test
    fun `uninstalled engine with no other selection falls back to system default`() {
        resolveVoiceSelection(
            selectedEnginePackage = "com.samsung.tts",
            selectedVoiceName = "",
            selectedLanguageTag = "",
            availableEnginePackages = setOf("com.google.android.tts"),
            availableVoiceNames = setOf("en-gb-x-i-local"),
            availableLanguageTags = setOf("en-US", "en-GB"),
        ) shouldBe TtsVoiceSelection.SystemDefault
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run (devcontainer): `./gradlew :domain:testDebugUnitTest --tests "mihon.domain.tts.service.TtsVoicePreferencesTest"`
Expected: FAIL — unresolved reference `resolveVoiceSelection`.

- [ ] **Step 3: Write minimal implementation**

`domain/src/main/java/mihon/domain/tts/service/TtsVoicePreferences.kt`:

```kotlin
package mihon.domain.tts.service

import tachiyomi.core.common.preference.PreferenceStore

class TtsVoicePreferences(private val preferenceStore: PreferenceStore) {

    fun ttsEnginePackage() = preferenceStore.getString("pref_tts_engine_package", "")

    fun ttsVoiceName() = preferenceStore.getString("pref_tts_voice_name", "")

    fun ttsLanguageTag() = preferenceStore.getString("pref_tts_language_tag", "")

    fun reset() {
        ttsEnginePackage().set("")
        ttsVoiceName().set("")
        ttsLanguageTag().set("")
    }
}

sealed interface TtsVoiceSelection {
    data object SystemDefault : TtsVoiceSelection
    data class Voice(val enginePackage: String, val voiceName: String, val languageTag: String) : TtsVoiceSelection
    data class Language(val enginePackage: String, val languageTag: String) : TtsVoiceSelection
}

fun resolveVoiceSelection(
    selectedEnginePackage: String,
    selectedVoiceName: String,
    selectedLanguageTag: String,
    availableEnginePackages: Set<String>,
    availableVoiceNames: Set<String>,
    availableLanguageTags: Set<String>,
): TtsVoiceSelection {
    val voiceValid = selectedVoiceName.isNotEmpty() && selectedVoiceName in availableVoiceNames
    val languageValid = selectedLanguageTag.isNotEmpty() && selectedLanguageTag in availableLanguageTags
    return when {
        voiceValid -> TtsVoiceSelection.Voice(selectedEnginePackage, selectedVoiceName, selectedLanguageTag)
        languageValid -> TtsVoiceSelection.Language(selectedEnginePackage, selectedLanguageTag)
        else -> TtsVoiceSelection.SystemDefault
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :domain:testDebugUnitTest --tests "mihon.domain.tts.service.TtsVoicePreferencesTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add domain/src/main/java/mihon/domain/tts/service/TtsVoicePreferences.kt \
        domain/src/test/java/mihon/domain/tts/service/TtsVoicePreferencesTest.kt
git commit -m "Add TtsVoicePreferences and voice-selection fallback resolver"
```

---

### Task 2: Domain — extend `TtsEngine` interface with discovery + selection contracts

**Files:**
- Modify: `domain/src/main/java/mihon/domain/tts/engine/TtsEngine.kt`

**Interfaces:**
- Produces (Tasks 3, 5 consume) — append inside `interface TtsEngine`:

```kotlin
    /**
     * Installed TTS engines on this device. Empty when the engine is not
     * initialized or discovery is unavailable.
     */
    suspend fun getEngines(): List<TtsEngineInfo>

    /**
     * Voices available from the active engine. Empty when the engine is not
     * initialized or exposes no voices.
     */
    suspend fun getVoices(): List<TtsVoiceInfo>

    /**
     * Selects the TTS engine used by the next [initialize]. Empty string
     * means system default engine. If the engine is currently initialized
     * with a different package, implementations must release it so the next
     * [initialize] rebuilds with the new engine.
     */
    fun setEnginePackage(pkg: String)
```

File-bottom models (all-`val`, no defaults):

```kotlin
data class TtsEngineInfo(
    val packageName: String,
    val label: String,
    val isSystemDefault: Boolean,
)

data class TtsVoiceInfo(
    val name: String,
    val languageTag: String,
    val displayName: String,
    val quality: Int,
    val latency: Int,
    val features: List<String>,
    val networkRequired: Boolean,
)
```

Keep existing members unchanged. Do NOT reintroduce `japaneseAvailable` (removed in product pivot).

- [ ] **Step 1: Add members + models**
- [ ] **Step 2: Compile** — `./gradlew :domain:compileDebugKotlin` (expect SUCCESS; :app intentionally broken until Task 3, so don't run :app gates yet)
- [ ] **Step 3: Commit** — `git commit -m "Extend TtsEngine with engine/voice discovery and selection contracts"` (only `TtsEngine.kt` staged)

---

### Task 3: App — `AndroidTtsEngine` implementation + initialize re-apply + DI wiring

**Files:**
- Modify: `app/src/main/java/eu/kanade/tachiyomi/data/tts/AndroidTtsEngine.kt`
- Modify: `app/src/main/java/eu/kanade/domain/DomainModule.kt` (L285)
- Modify: `app/src/main/java/eu/kanade/tachiyomi/di/PreferenceModule.kt` (after L63)

**Interfaces:**
- Consumes: Task 1 (`TtsVoicePreferences`, `TtsVoiceSelection`, `resolveVoiceSelection`), Task 2 contracts.
- Produces (Task 5 consumes): `AndroidTtsEngine` satisfying the extended interface; Injekt bindings `TtsVoicePreferences` + engine-with-prefs.

**Implementation (edits to current 175-line file):**

1. Constructor: `class AndroidTtsEngine(private val context: Context, private val voicePreferences: TtsVoicePreferences) : TtsEngine`
2. Cached fields beside existing rate/pitch (L29-31):
   ```kotlin
   private var enginePackage: String = voicePreferences.ttsEnginePackage().get()
   private var voiceName: String = voicePreferences.ttsVoiceName().get()
   private var languageTag: String = voicePreferences.ttsLanguageTag().get()
   ```
3. `setEnginePackage(pkg)`: `if (pkg == enginePackage) return; enginePackage = pkg; if (tts.get()?.let { it.engine?.packageName } != pkg) shutdown()` — shutdown releases the old instance so next `initialize()` rebuilds with new engine. (Get current engine package via `tts.get()?.defaultEngine?.packageName`? NO — current instance's engine: keep a `private var activeEnginePackage: String = ""` set at initialize; compare against it.)
4. `initialize()` — three changes:
   - Creation (L39-41): `val ttsToCreate = if (enginePackage.isNotEmpty()) TextToSpeech(context.applicationContext, enginePackage, initListener) else TextToSpeech(context.applicationContext, initListener)` (two constructors; both public API, minSdk 26 safe).
   - Idempotent early-return path (L36): instead of bare `return@withLock true`, first re-apply config from prefs (see below) — this makes every subsequent `initialize()` (controller calls it on resume/step/rebuild) pick up pref changes made in settings mid-session.
   - After readiness success (post L79), apply voice/language via a private `applyVoiceConfig(engine: TextToSpeech): Unit` that:
     - Re-reads `voicePreferences` (fresh values into cached fields).
     - Builds availability sets: `availableVoiceNames = engine.voices.orEmpty().map { it.name }.toSet()`, `availableLanguageTags = engine.voices.orEmpty().map { it.locale.toLanguageTag() }.toSet()`, `availableEnginePackages = engine.engines.orEmpty().map { it.packageName }.toSet()`.
     - Calls `resolveVoiceSelection(...)` and applies:
       - `Voice` → find `engine.voices.firstOrNull { it.name == voiceName }` → `engine.setVoice(v)`; log DEBUG "TTS voice applied name=X" (no text content; name is metadata, acceptable per rules.md §7).
       - `Language` → `engine.setLanguage(Locale.forLanguageTag(languageTag))` — if result is `LANG_MISSING_DATA` or `LANG_NOT_SUPPORTED`, log DEBUG and leave default (never fail initialize).
       - `SystemDefault` → no-op (engine default).
     - `setVoice`/`setLanguage` failures NEVER fail `initialize()` — always return true once the TTS connection itself succeeded (spec PART 9).
   - Apply existing `setSpeechRate(speechRate)`/`setPitch(pitch)` as today (L65-66).
5. `getEngines()`:
   ```kotlin
   override suspend fun getEngines(): List<TtsEngineInfo> = withContext(Dispatchers.Main) {
       val engine = tts.get() ?: return@withContext emptyList()
       val defaultPkg = engine.defaultEngine?.packageName
       engine.engines.orEmpty().map {
           TtsEngineInfo(
               packageName = it.packageName,
               label = it.label ?: it.name,
               isSystemDefault = it.packageName == defaultPkg,
           )
       }
   }
   ```
6. `getVoices()`:
   ```kotlin
   override suspend fun getVoices(): List<TtsVoiceInfo> = withContext(Dispatchers.Main) {
       val engine = tts.get() ?: return@withContext emptyList()
       engine.voices.orEmpty()
           .map {
               TtsVoiceInfo(
                   name = it.name,
                   languageTag = it.locale.toLanguageTag(),
                   displayName = it.name,
                   quality = it.quality,
                   latency = it.latency,
                   features = it.features.orEmpty().toList(),
                   networkRequired = it.isNetworkConnectionRequired,
               )
           }
   }
   ```
7. speak/stop/shutdown/focus paths: UNTOUCHED (regression guard). `pendingUtterances`/`onFocusEvent` logic unchanged. Preview needs no engine change — screen model (Task 5) calls `engine.stop()` then `engine.speak("tts-preview", SAMPLE_TEXT)`; QUEUE_FLUSH (L93) guarantees previous preview/narration is flushed; reader narration resumes authority on its next speak.
8. DI wiring (same task so :app compiles):
   - `PreferenceModule.kt` after L63: `addSingletonFactory { TtsVoicePreferences(get()) }`
   - `DomainModule.kt` L285 → `addSingletonFactory<TtsEngine> { AndroidTtsEngine(get<Application>(), get<TtsVoicePreferences>()) }`

- [ ] **Step 1: Implement engine + DI edits**
- [ ] **Step 2: Compile** — `./gradlew :app:compileDebugKotlin` (expect SUCCESS)
- [ ] **Step 3: Full gates** — `./gradlew spotlessCheck testDebugUnitTest` (expect green; no controller/VM changes so existing behavior suites unaffected)
- [ ] **Step 4: Commit** — `git add` the three files; `git commit -m "Apply persisted TTS voice selection in AndroidTtsEngine with safe fallback"`

---

### Task 4: App — main settings screen "Read aloud & voice" (screen model + screen + i18n)

**Files:**
- Create: `app/src/main/java/eu/kanade/tachiyomi/ui/setting/readaloud/ReadAloudSettingsScreenModel.kt`
- Create: `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsReadAloudScreen.kt`
- Modify: `i18n/src/commonMain/moko-resources/base/strings.xml` (near L1263 block)
- Modify: `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsSearchScreen.kt` (L288+ `settingScreens` list — add `SettingsReadAloudScreen`)

**Interfaces:**
- Consumes: Task 1 + 2 + 3 outputs; `Preference.PreferenceItem.*` (`Preference.kt`); Anki pattern for stateful screen (`SettingsAnkiScreen.kt:41-55`).
- Produces (Task 5 consumes): `SettingsReadAloudScreen` object + MR keys + screen-model API (`load/selectEngine/selectLanguage/selectVoice/preview/stopPreview/setRate/setPitch/resetVoiceConfig`).

**Screen model:**

```kotlin
package eu.kanade.tachiyomi.ui.setting.readaloud

// StateScreenModel — Dictionary/Anki precedent
class ReadAloudSettingsScreenModel(
    private val engine: TtsEngine = Injekt.get(),
    private val voicePreferences: TtsVoicePreferences = Injekt.get(),
    private val ttsPreferences: TtsPreferences = Injekt.get(),
) : StateScreenModel<ReadAloudSettingsState>(ReadAloudSettingsState()) {

    @Immutable
    data class ReadAloudSettingsState(
        val isLoading: Boolean = true,
        val engines: List<TtsEngineInfo> = emptyList(),
        val voices: List<TtsVoiceInfo> = emptyList(),
        val selectedEnginePackage: String = "",
        val selectedVoiceName: String = "",
        val selectedLanguageTag: String = "",
        val rate: Float = 1f,
        val pitch: Float = 1f,
        val isPreviewPlaying: Boolean = false,
        val loadFailed: Boolean = false,
    )

    fun load() {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true, loadFailed = false) }
            val ok = engine.initialize()
            if (!ok) {
                mutableState.update { it.copy(isLoading = false, loadFailed = true) }
                return@launch
            }
            val engines = engine.getEngines()
            val voices = engine.getVoices()
            // effective selection per resolveVoiceSelection; stale prefs left as-is
            mutableState.update {
                it.copy(
                    isLoading = false,
                    engines = engines,
                    voices = voices,
                    selectedEnginePackage = voicePreferences.ttsEnginePackage().get(),
                    selectedVoiceName = voicePreferences.ttsVoiceName().get(),
                    selectedLanguageTag = voicePreferences.ttsLanguageTag().get(),
                    rate = ttsPreferences.ttsSpeechRate().get(),
                    pitch = ttsPreferences.ttsPitch().get(),
                )
            }
        }
    }

    fun selectEngine(pkg: String) {
        voicePreferences.ttsEnginePackage().set(pkg)
        voicePreferences.ttsVoiceName().set("")   // voices belong to engines; reset stale voice
        voicePreferences.ttsLanguageTag().set("") // and stale language
        engine.setEnginePackage(pkg)
        load()
    }

    fun selectLanguage(tag: String) {
        voicePreferences.ttsLanguageTag().set(tag)
        voicePreferences.ttsVoiceName().set("")   // language change invalidates old voice
        mutableState.update { it.copy(selectedLanguageTag = tag, selectedVoiceName = "") }
    }

    fun selectVoice(name: String) {
        voicePreferences.ttsVoiceName().set(name)
        mutableState.update { it.copy(selectedVoiceName = name) }
        screenModelScope.launch { engine.initialize() } // re-apply path applies new voice
    }

    fun setRate(v: Float) { ttsPreferences.ttsSpeechRate().set(v); mutableState.update { it.copy(rate = v) } }
    fun setPitch(v: Float) { ttsPreferences.ttsPitch().set(v); mutableState.update { it.copy(pitch = v) } }

    fun preview() {
        screenModelScope.launch {
            engine.stop() // stop previous preview (spec PART 7.4)
            engine.initialize() // re-apply current prefs (rate/pitch/voice)
            engine.acquireFocus()
            mutableState.update { it.copy(isPreviewPlaying = true) }
            engine.speak("tts-preview", PREVIEW_TEXT)
            mutableState.update { it.copy(isPreviewPlaying = false) }
            engine.abandonFocus()
        }
    }

    fun stopPreview() { engine.stop() } // speak() completes false → coroutine above resets flag

    fun resetVoiceConfig() {
        voicePreferences.reset()
        engine.setEnginePackage("")
        load()
    }

    // Preview text hardcoded (content, not label — no i18n per spec example)
    private companion object { const val PREVIEW_TEXT = "Hello. This is a preview of the selected reading voice." }
}
```

Note: engine.acquireFocus/abandonFocus are existing interface members (TtsEngine.kt:27-29) — preview reuses them so focus behavior stays correct (spec check 17).

**Screen (SearchableSettings, Anki pattern):**

```kotlin
object SettingsReadAloudScreen : SearchableSettings {
    @ReadOnlyComposable @Composable
    override fun getTitleRes() = MR.strings.pref_category_read_aloud

    @Composable
    override fun getPreferences(): List<Preference> {
        val screenModel = rememberScreenModel { ReadAloudSettingsScreenModel() }
        val state by screenModel.state.collectAsState()
        LaunchedEffect(Unit) { screenModel.load() }

        if (state.isLoading) {
            return listOf(CustomPreference spinner — MR.strings.loading) // Anki L49-55 pattern
        }
        if (state.loadFailed) {
            return listOf(InfoPreference(MR.strings.tts_voices_unavailable via stringResource), retry TextPreference → screenModel.load())
        }

        val languageTags = state.voices.map { it.languageTag }.distinct().sorted()
        val languages = languageTags.map { it.substringBefore('-') }.distinct().sorted()

        return listOf(
            PreferenceGroup(title = stringResource(MR.strings.tts_section_text_to_speech), preferenceItems = listOf(
                BasicListPreference(
                    value = state.selectedEnginePackage,
                    entries = buildMap {
                        put("", stringResource(MR.strings.tts_default_system_engine))
                        state.engines.forEach { put(it.packageName, it.label + if (it.isSystemDefault) " ★" else "") }
                    },
                    title = stringResource(MR.strings.tts_engine),
                    onValueChanged = { screenModel.selectEngine(it) },
                ),
                BasicListPreference(
                    value = state.selectedLanguageTag,
                    entries = languageTags.associateWith { tag -> localeDisplayName(tag) },
                    title = stringResource(MR.strings.tts_language),
                    onValueChanged = { screenModel.selectLanguage(it) },
                ),
                BasicListPreference(
                    value = state.selectedVoiceName,
                    entries = state.voices.associate { it.name to voiceLabel(it) },
                    title = stringResource(MR.strings.tts_voice),
                    onValueChanged = { screenModel.selectVoice(it) },
                ),
            )),
            PreferenceGroup(title = stringResource(MR.strings.tts_section_voice_calibration), preferenceItems = listOf(
                SliderPreference(
                    value = (state.rate * 100).roundToInt(),
                    valueRange = 50..200,
                    valueString = "${(state.rate * 100).roundToInt()}%",
                    title = stringResource(MR.strings.pref_tts_speech_rate), // REUSED key
                    onValueChanged = { screenModel.setRate(it / 100f) },
                ),
                SliderPreference(... pitch, pref_tts_pitch REUSED ...),
                TextPreference(
                    title = stringResource(MR.strings.tts_preview_voice),
                    subtitle = stringResource(MR.strings.tts_play_sample),
                    widget = if (state.isPreviewPlaying) { { CircularProgressIndicator(small) } } else null,
                    onClick = { if (state.isPreviewPlaying) screenModel.stopPreview() else screenModel.preview() },
                ),
            )),
            PreferenceGroup(title = stringResource(MR.strings.tts_section_advanced), preferenceItems = listOf(
                TextPreference(
                    title = stringResource(MR.strings.tts_engine_information),
                    subtitle = engineInfoSummary(state), // "Google Speech Services · default · 12 voices"
                ),
                InfoPreference(title = stringResource(MR.strings.tts_available_voices, state.voices.size)),
                TextPreference(
                    title = stringResource(MR.strings.tts_reset_voice_config),
                    onClick = { screenModel.resetVoiceConfig(); context.toast(MR.strings.tts_config_reset) },
                ),
            )),
        )
    }
}
```

**Helper functions (in screen file, private):**
- `localeDisplayName(tag: String): String` = `Locale.forLanguageTag(tag).getDisplayName(Locale.getDefault())` — shows "English (India)" for en-IN per device locale. Fallback to tag if blank.
- `voiceLabel(v: TtsVoiceInfo): String` = `v.name` + quality/latency markers appended from `android.speech.tts.Voice` constants (`QUALITY_VERY_HIGH`=400 etc.): map `quality >= 400 → " · high quality"`, `latency <= 200 → " · low latency"`, `networkRequired → " · network"`. Only API-derived facts (spec PART 3: do not invent).
- `engineInfoSummary(state)`: selected engine label + " (default)" if flagged + voice count. Show "System default" when no engine selected.

Locale vs language distinction (spec PART 2): language picker lists distinct language prefixes ("en"); locale picker lists full tags ("en-IN"). Filter voices for the selected language: `state.voices.filter { it.languageTag.startsWith(selectedLanguagePrefix) }` when building voice entries, and locale entries only for tags matching selected language. If no language selected, voice list shows all (safe default).

Executor note: exact `BasicListPreference`/`TextPreference`/`SliderPreference`/`InfoPreference` constructor params from `Preference.kt:24-99,162-179` — `BasicListPreference(value: String, entries: Map<String,String>, title, subtitle="%s", subtitleProvider, enabled, onValueChanged)`; widget param only on TextPreference.

**i18n keys (base strings.xml, snake_case, block near L1263):**

```xml
<string name="pref_category_read_aloud">Read aloud and voice</string>
<string name="pref_read_aloud_summary">Text-to-speech engine, voices and preview</string>
<string name="tts_section_text_to_speech">Text to speech</string>
<string name="tts_section_voice_calibration">Voice calibration</string>
<string name="tts_section_advanced">Advanced</string>
<string name="tts_engine">TTS engine</string>
<string name="tts_voice">Voice</string>
<string name="tts_language">Language</string>
<string name="tts_default_system_engine">Default system engine</string>
<string name="tts_engine_information">Engine information</string>
<string name="tts_available_voices">Available voices: %d</string>
<string name="tts_preview_voice">Preview voice</string>
<string name="tts_play_sample">Play sample</string>
<string name="tts_reset_voice_config">Reset voice configuration</string>
<string name="tts_config_reset">Voice configuration reset</string>
<string name="tts_voices_unavailable">No voices found. Install or enable a text-to-speech engine.</string>
```

(16 keys. Locale picker reuses `tts_language`? NO — spec needs "Locale" label distinct: add `<string name="tts_locale">Locale</string>` — 17 keys total. Reused keys: `pref_tts_speech_rate`, `pref_tts_pitch`, `loading`, `action_retry`.)

- [ ] **Step 1: Screen model + screen + i18n + search registration**
- [ ] **Step 2: Compile** — `./gradlew :app:compileDebugKotlin`
- [ ] **Step 3: spotlessApply then spotlessCheck**
- [ ] **Step 4: Commit** — `git commit -m "Add Read aloud & voice settings screen with engine/voice pickers and preview"` (4 files)

---

### Task 5: App — settings root entry + reader deep-link + minimal reader tab

**Files:**
- Modify: `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsMainScreen.kt` (getItems L179+)
- Modify: `core/common/src/main/kotlin/tachiyomi/core/common/Constants.kt` (SHORTCUT block L15-21)
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/main/MainActivity.kt` (handleIntentAction, L515+)
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/setting/SettingsScreen.kt` (Destination + both `when` branches L38-44, L60-66)
- Modify: `app/src/main/java/eu/kanade/presentation/reader/settings/ReaderSettingsDialog.kt` (param + pass-through)
- Modify: `app/src/main/java/eu/kanade/presentation/reader/settings/ReadAloudPage.kt` (remove pitch slider; add nav row)
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt` (BOTH dialog call sites L462-467 phone + L876-881 tablet)
- Modify: `i18n/src/commonMain/moko-resources/base/strings.xml` (1 key)

**Interfaces:**
- Consumes: Task 4 `SettingsReadAloudScreen`; intent-handoff precedent (`ReaderActivity.openMangaScreen` L1113-1123; `MainActivity.handleIntentAction` L540-544).
- Produces: full flow Settings root → Read aloud & voice ↔ reader quick settings → deep-link.

**Steps:**

1. **Settings root row** (getItems, after Reader item L192-197):
   ```kotlin
   Item(
       titleRes = MR.strings.pref_category_read_aloud,
       subtitleRes = MR.strings.pref_read_aloud_summary,
       icon = Icons.AutoMirrored.Outlined.VolumeUp,
       screen = SettingsReadAloudScreen,
   )
   ```
   Icon: `androidx.compose.material.icons.automirrored.outlined.VolumeUp` (same AutoMirrored import pattern as `ChromeReaderMode` at L11 — if unresolved at compile, drop to `Icons.Outlined.VolumeUp`).
2. **Deep-link plumbing:**
   - `Constants.kt`: `const val SHORTCUT_VOICE_SETTINGS = "eu.kanade.tachiyomi.SHOW_VOICE_SETTINGS"` (beside L15-21).
   - `SettingsScreen.kt`: `Destination.ReadAloud` with unique id `4` (existing: About=0, DataAndStorage=1, Dictionary=3, Tracking=2 — 4 unused); add `Destination.ReadAloud.id -> SettingsReadAloudScreen` to BOTH `when` branches (phone L38-44 + tablet L60-66; import `SettingsReadAloudScreen`).
   - `MainActivity.handleIntentAction`: new case beside ACTION_APPLICATION_PREFERENCES (L540-544):
     ```kotlin
     Constants.SHORTCUT_VOICE_SETTINGS -> {
         navigator.popUntilRoot()
         navigator.push(SettingsScreen(SettingsScreen.Destination.ReadAloud))
         null
     }
     ```
3. **Reader tab (spec PART 5):**
   - `ReadAloudPage.kt`: REMOVE pitch `SliderItem` (moves to main settings Voice Calibration — spec's recommended reader list omits it). Keep rate slider, 3 checkboxes. Add nav row using `TextPreferenceWidget` (verified self-contained: only presentation-core deps + PrefsHorizontalPadding from same widget package, all importable into reader dialog):
     ```kotlin
     TextPreferenceWidget(
         title = stringResource(MR.strings.tts_advanced_voice_settings),
         onPreferenceClick = onOpenVoiceSettings,
     )
     ```
   - `ReaderSettingsDialog.kt`: add param `onOpenVoiceSettings: () -> Unit` (after onHideMenus), pass to `ReadAloudPage(screenModel, onOpenVoiceSettings)`.
   - `ReaderActivity.kt` BOTH call sites:
     ```kotlin
     onOpenVoiceSettings = {
         viewModel.closeDialog()
         startActivity(Intent(this@ReaderActivity, MainActivity::class.java).apply {
             action = Constants.SHORTCUT_VOICE_SETTINGS
             addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
         })
     }
     ```
4. **i18n:** `<string name="tts_advanced_voice_settings">Advanced voice settings</string>`

- [ ] **Step 1: All edits above**
- [ ] **Step 2: Compile + gates** — `./gradlew :app:compileDebugKotlin spotlessCheck testDebugUnitTest`
- [ ] **Step 3: Commit** — `git commit -m "Add Read aloud & voice settings entry, reader deep-link and minimal reader tab"`

---

### Task 6: Documentation updates

**Files:**
- Modify: `docs/prd.md`, `docs/architecture.md`, `docs/design.md`, `docs/phase.md`, `docs/memory.md`

**Content per spec PART 14 + PARTS 11/12:**

- **prd.md**: Phase 10A feature section — capability list (engine/voice/language discovery, pickers, calibration, preview, persistence, fallback chain), user flow (Settings → Read aloud & voice → engine/language/voice → calibration → preview → reader playback), preview policy (preview stops previous utterance; reader narration wins by flush; interrupted narration pauses honestly), Phase 10B candidates (per-voice rate/pitch profiles, cloud/neural providers w/ credentials+cost+privacy+streaming+caching+latency, local neural engines, downloadable AI voices; expressive speech needs provider SSML/prosody/emotion controls — pitch/rate alone are not expressive narration, no fake emotion via random pitch).
- **architecture.md**: TtsEngine new contracts (getEngines/getVoices/setEnginePackage + TtsEngineInfo/TtsVoiceInfo); AndroidTtsEngine config pipeline (prefs read at construction, re-read at every initialize → controller's existing resume/step initialize calls pick up pref changes with zero controller changes); fallback application inside initialize via resolveVoiceSelection (never fails init on voice/locale apply failure); preview single-engine policy; future-provider extensibility notes (new impls = Injekt binding swap; playback controller untouched).
- **design.md**: screen structure (3 groups: Text to speech / Voice calibration / Advanced), reader tab minimal list + nav row, loading spinner (Anki pattern), error state (InfoPreference + retry), voice metadata labels from API facts only (quality/latency/network markers), locale display names via `Locale.getDisplayName`.
- **phase.md**: Phase 10A IN_PROGRESS → status block (device-verification gated completion; Task 7 fills COMPLETED after device pass).
- **memory.md**: session record — audit summary, files changed, gate commands + results, decisions (merged initialize re-apply design; per-voice rate/pitch deferred 10B; preview policy; deep-link over root-settings), device-test placeholder to fill in Task 7.

- [ ] **Step 1: Write doc updates**
- [ ] **Step 2: Commit** — `git commit -m "Document Phase 10A advanced voice configuration"`

---

### Task 7: Device verification (spec PART 13) + final status

**Files:** evidence to `.device-pass/` (gitignored); final `docs/memory.md` + `docs/phase.md` status update.

**Steps:**
- Build + install: devcontainer `./gradlew :app:assembleDebug` → install arm64 APK on SM_M066B via wireless adb (persistent volumes yomihon-gradle-home + yomihon-android-home per memory.md).
- On-device logcat capture (established pattern: `adb shell "logcat -c; nohup logcat -v threadtime > /sdcard/tts-test.log 2>&1 &"` then pull).
- Manual script — spec PART 13's 17 checks:
  1. Default TTS still works (reader playback, no selection) — regression baseline.
  2. Voice picker loads device voices.
  3. Language selection works.
  4. Locale selection works.
  5. Voice selection works.
  6. Speech rate applies live in reader.
  7. Pitch applies live in reader (now via main settings; pause/resume in reader applies it).
  8. Preview uses selected engine/voice/rate/pitch.
  9. Settings persist after app restart.
  10. Invalid/uninstalled voice falls back safely (select engine/voice, restart app, no crash + honest fallback; simulate uninstall via pref reset if needed).
  11. Reader TTS playback functional end-to-end.
  12. Pause/resume works.
  13. Auto-scroll (webtoon) works.
  14. Auto page progression works.
  15. No memory leaks (LeakCanary visible, no new APPLICATION LEAK signatures beyond known noise #13).
  16. No duplicate TextToSpeech instances (single "Connected to TTS engine" per session in logcat).
  17. Audio focus correct (YouTube Music during preview and narration — preview acquires/abandons; narration pause/resume same as Phase 9).
- Record results in `docs/memory.md`; mark Phase 10A COMPLETED in `docs/phase.md` ONLY after all 17 pass.

- [ ] **Step 1: Build + install + run script**
- [ ] **Step 2: Record results + status updates + final commit**

---

## Self-Review

**Spec coverage:** engine discovery/selection ✓T3/T4; voice discovery/selection ✓T3/T4 (PART 3 metadata from API facts only); language/locale ✓T4 (PART 2 dynamic lists, graceful fallback); installed voice inspection ✓T4 Advanced group; per-voice config → global rate/pitch only, per-voice profiles deferred per PART 6's explicit YAGNI allowance ✓ (documented T6); rate/pitch calibration ✓T4 sliders (existing global prefs); preview ✓T4 (PART 7: stop-previous, selected config, single engine, focus handling); persistence ✓T1/T3 (PART 8: validate-at-apply, update-on-next-init); fallback ✓T1/T3 (PART 9 chain); UI/UX ✓T4/T5 (PART 10: hierarchy, summaries, dynamic lists, loading states, no jargon); cloud TTS excluded, extensibility documented ✓T6 (PART 11); no fake expressive speech ✓T6 (PART 12); build+tests ✓T3/T4/T5 gates; device testing ✓T7 (PART 13); docs ✓T6 (PART 14).

**Placeholder scan:** none — all code blocks complete; T4 screen/sketch blocks name exact components + params from audited sources; executor decisions (icon fallback, dialog row component) have explicit choices + fallbacks.

**Type consistency:** `TtsVoiceSelection` variants identical T1→T3; `TtsEngineInfo`/`TtsVoiceInfo` fields match T2 defs and T3/T4 usage; pref keys `pref_tts_engine_package`/`pref_tts_voice_name`/`pref_tts_language_tag` consistent T1→T3→T4; `SHORTCUT_VOICE_SETTINGS` consistent T5 steps; `Destination.ReadAloud` id 4 unique (0,1,2,3 taken).

**Regression guards:** controller/VM zero-diff; existing TtsPreferences untouched; reader tab keeps rate + 3 checkboxes (pitch relocation per spec PART 5 recommended list); dim-hack index untouched (read aloud stays tab 3, appended last); QUEUE_FLUSH preview semantics reuse existing speak path; engine singleton + shutdown-on-engine-switch reuses Phase 9 leak-fix lifecycle (onFocusEvent detach unchanged).
