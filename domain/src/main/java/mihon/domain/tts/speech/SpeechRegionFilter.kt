package mihon.domain.tts.speech

import mihon.domain.ocr.model.OcrRegion

/**
 * Which region types Read-Aloud speaks, and how to treat regions whose script
 * differs from the TTS speech language. Sensible defaults: dialogue +
 * narration + unknown spoken; SFX/expressions/decorative skipped.
 */
data class SpeechRegionFilterConfig(
    val speakDialogue: Boolean = true,
    val speakNarration: Boolean = true,
    val speakSoundEffects: Boolean = false,
    val speakExpressions: Boolean = false,
    val speakDecorative: Boolean = false,
    val speakUnknown: Boolean = true,
    /** Skip regions whose dominant script differs from the TTS language script. */
    val skipForeignScript: Boolean = true,
    /** Speech-language script: LATIN for English v1 (product pivot). */
    val speechScript: SpeechScript = SpeechScript.LATIN,
)

/** Region-type/script gate of the speech pipeline; text cleanup lives in [SpeechCleaner]. */
object SpeechRegionFilter {

    /** Regions consumed in stored order; output order preserved. */
    fun filterRegions(
        regions: List<OcrRegion>,
        classificationConfig: SpeechClassificationConfig,
        config: SpeechRegionFilterConfig,
    ): List<OcrRegion> = regions.filter { region ->
        val text = region.text.trim()
        if (text.isBlank()) return@filter false

        val type = SpeechRegionClassifier.classify(region, classificationConfig)
        val typeAllowed = when (type) {
            SpeechRegionType.DIALOGUE -> config.speakDialogue
            SpeechRegionType.NARRATION -> config.speakNarration
            SpeechRegionType.SOUND_EFFECT -> config.speakSoundEffects
            SpeechRegionType.EXPRESSION -> config.speakExpressions
            SpeechRegionType.DECORATIVE -> config.speakDecorative
            SpeechRegionType.UNKNOWN -> config.speakUnknown
        }
        if (!typeAllowed) return@filter false
        if (config.skipForeignScript && type != SpeechRegionType.DECORATIVE) {
            val script = dominantScript(text.filter { it.isLetter() })
            script == config.speechScript
        } else {
            true
        }
    }
}
