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
