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
