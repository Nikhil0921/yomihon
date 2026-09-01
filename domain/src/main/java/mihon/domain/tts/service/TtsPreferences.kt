package mihon.domain.tts.service

import mihon.domain.tts.speech.SpeechCleanupOptions
import mihon.domain.tts.speech.SpeechRegionFilterConfig
import tachiyomi.core.common.preference.PreferenceStore

class TtsPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun ttsSpeechRate() = preferenceStore.getFloat("pref_tts_speech_rate", 1f)

    fun ttsPitch() = preferenceStore.getFloat("pref_tts_pitch", 1f)

    fun ttsAutoPageTurn() = preferenceStore.getBoolean("pref_tts_auto_page_turn", true)

    fun ttsAutoNextChapter() = preferenceStore.getBoolean("pref_tts_auto_next_chapter", false)

    fun ttsKeepScreenOn() = preferenceStore.getBoolean("pref_tts_keep_screen_on", true)

    // --- Speech cleanup (Phase A) ---

    fun ttsSkipPunctuationOnly() = preferenceStore.getBoolean("pref_tts_skip_punctuation_only", true)

    fun ttsSkipOcrGarbage() = preferenceStore.getBoolean("pref_tts_skip_ocr_garbage", true)

    fun ttsNormalizePunctuation() = preferenceStore.getBoolean("pref_tts_normalize_punctuation", true)

    fun ttsEllipsisToPause() = preferenceStore.getBoolean("pref_tts_ellipsis_to_pause", true)

    // --- Region classification / spoken types (Phases B & C) ---

    fun ttsOcrExclusionsEnabled() = preferenceStore.getBoolean("pref_tts_ocr_exclusions_enabled", true)

    fun ttsSpeakSoundEffects() = preferenceStore.getBoolean("pref_tts_speak_sfx", false)

    fun ttsSpeakExpressions() = preferenceStore.getBoolean("pref_tts_speak_expressions", false)

    fun ttsSkipForeignScript() = preferenceStore.getBoolean("pref_tts_skip_foreign_script", true)

    /** Speech-language script hint for foreign-script filtering; "LATIN" or "CJK". */
    fun ttsSpeechScript() = preferenceStore.getString("pref_tts_speech_script", "LATIN")

    fun speechCleanupOptions(): SpeechCleanupOptions = SpeechCleanupOptions(
        skipPunctuationOnly = ttsSkipPunctuationOnly().get(),
        skipOcrGarbage = ttsSkipOcrGarbage().get(),
        normalizeExcessivePunctuation = ttsNormalizePunctuation().get(),
        ellipsisToPause = ttsEllipsisToPause().get(),
    )

    fun speechRegionFilterConfig(): SpeechRegionFilterConfig {
        val script = when (ttsSpeechScript().get()) {
            "CJK" -> mihon.domain.tts.speech.SpeechScript.CJK
            else -> mihon.domain.tts.speech.SpeechScript.LATIN
        }
        return SpeechRegionFilterConfig(
            speakSoundEffects = ttsSpeakSoundEffects().get(),
            speakExpressions = ttsSpeakExpressions().get(),
            skipForeignScript = ttsSkipForeignScript().get(),
            speechScript = script,
        )
    }
}
