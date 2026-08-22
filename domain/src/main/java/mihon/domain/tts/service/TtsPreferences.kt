package mihon.domain.tts.service

import tachiyomi.core.common.preference.PreferenceStore

class TtsPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun ttsSpeechRate() = preferenceStore.getFloat("pref_tts_speech_rate", 1f)

    fun ttsPitch() = preferenceStore.getFloat("pref_tts_pitch", 1f)

    fun ttsAutoPageTurn() = preferenceStore.getBoolean("pref_tts_auto_page_turn", true)

    fun ttsAutoNextChapter() = preferenceStore.getBoolean("pref_tts_auto_next_chapter", false)

    fun ttsKeepScreenOn() = preferenceStore.getBoolean("pref_tts_keep_screen_on", true)
}
