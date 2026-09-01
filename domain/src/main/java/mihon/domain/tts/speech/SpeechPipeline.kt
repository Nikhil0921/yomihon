package mihon.domain.tts.speech

import mihon.domain.ocr.model.OcrRegion
import mihon.domain.tts.TtsSentence
import mihon.domain.tts.toTtsSentences

/**
 * Speech-only pipeline over original OCR regions:
 * classify → type/script filter → text cleanup → sentence segmentation.
 * Original OCR data is immutable; this is the only place speech-specific
 * decisions live. Cleanup runs BEFORE segmentation so punctuation runs like
 * "WHAT?!?!?!" normalize to "WHAT?!" first, while meaningful punctuation
 * ("I don't know...") survives untouched. Punctuation-only sentence fragments
 * left over from terminal splitting (e.g. the "!" of "?!") are dropped.
 */
object SpeechPipeline {

    fun toSpeakableSentences(
        regions: List<OcrRegion>,
        classificationConfig: SpeechClassificationConfig,
        filterConfig: SpeechRegionFilterConfig,
        cleanupOptions: SpeechCleanupOptions,
    ): List<TtsSentence> =
        SpeechRegionFilter
            .filterRegions(regions, classificationConfig, filterConfig)
            .mapNotNull { region ->
                SpeechCleaner.cleanRegionForSpeech(region.text, cleanupOptions)
                    ?.let { region.copy(text = it) }
            }
            .toTtsSentences()
            .filter { it.text.any { c -> c.isLetterOrDigit() } }
}
