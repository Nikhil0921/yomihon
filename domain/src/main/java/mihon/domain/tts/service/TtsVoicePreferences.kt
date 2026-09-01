package mihon.domain.tts.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import tachiyomi.core.common.preference.PreferenceStore
import java.util.UUID

@Serializable
data class TtsVoiceProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val enginePackage: String = "",
    val voiceName: String = "",
    val languageTag: String = "",
    val rate: Float = 1f,
    val pitch: Float = 1f,
)

class TtsVoicePreferences(private val preferenceStore: PreferenceStore) {

    fun ttsEnginePackage() = preferenceStore.getString("pref_tts_engine_package", "")

    fun ttsVoiceName() = preferenceStore.getString("pref_tts_voice_name", "")

    fun ttsLanguageTag() = preferenceStore.getString("pref_tts_language_tag", "")

    fun ttsVoiceProfiles() = preferenceStore.getObjectFromString(
        "pref_tts_voice_profiles",
        emptyList(),
        { profiles -> profileJson.encodeToString(ListSerializer(TtsVoiceProfile.serializer()), profiles) },
        { raw ->
            runCatching { profileJson.decodeFromString(ListSerializer(TtsVoiceProfile.serializer()), raw) }
                .getOrDefault(emptyList())
        },
    )

    fun ttsActiveVoiceProfileId() = preferenceStore.getString("pref_tts_active_voice_profile", "")

    fun reset() {
        ttsEnginePackage().set("")
        ttsVoiceName().set("")
        ttsLanguageTag().set("")
    }

    private companion object {
        val profileJson = Json { ignoreUnknownKeys = true }
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
