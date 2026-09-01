package mihon.domain.tts.speech

import mihon.domain.ocr.model.OcrBoundingBox
import mihon.domain.ocr.model.OcrRegion
import mihon.domain.tts.TtsSentence
import mihon.domain.tts.toTtsSentences

/**
 * Speech-only pipeline over original OCR regions:
 * dedup → classify → type/script filter → text cleanup → sentence segmentation.
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
        dedupeOverlappingDuplicates(
            SpeechRegionFilter
                .filterRegions(regions, classificationConfig, filterConfig),
        )
            .mapNotNull { region ->
                SpeechCleaner.cleanRegionForSpeech(region.text, cleanupOptions)
                    ?.let { region.copy(text = it) }
            }
            .toTtsSentences()
            .filter { it.text.any { c -> c.isLetterOrDigit() } }

    /**
     * Drops later regions whose normalized text exactly duplicates an earlier
     * region AND whose box overlaps it. Duplicate-text regions in disjoint
     * boxes (same word in two different bubbles) are legitimate and survive.
     */
    fun dedupeOverlappingDuplicates(regions: List<OcrRegion>): List<OcrRegion> {
        if (regions.size < 2) return regions
        val kept = ArrayList<OcrRegion>(regions.size)
        outer@ for (region in regions) {
            for (earlier in kept) {
                if (duplicateTextKey(earlier) == duplicateTextKey(region) &&
                    boxesOverlap(earlier.boundingBox, region.boundingBox)
                ) {
                    continue@outer
                }
            }
            kept.add(region)
        }
        return kept
    }

    private val duplicateTextWhitespace = Regex("\\s+")

    private fun duplicateTextKey(region: OcrRegion): String =
        region.text.trim().replace(duplicateTextWhitespace, " ").lowercase()
}

/** Strict AABB overlap test on normalized coordinates. */
fun boxesOverlap(a: OcrBoundingBox, b: OcrBoundingBox): Boolean =
    a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom

/**
 * Intersection-over-union of two normalized boxes; 0 when they do not overlap.
 */
fun boundingBoxIoU(a: OcrBoundingBox, b: OcrBoundingBox): Float {
    val intersectLeft = maxOf(a.left, b.left)
    val intersectTop = maxOf(a.top, b.top)
    val intersectRight = minOf(a.right, b.right)
    val intersectBottom = minOf(a.bottom, b.bottom)
    val intersection = (intersectRight - intersectLeft).coerceAtLeast(0f) *
        (intersectBottom - intersectTop).coerceAtLeast(0f)
    if (intersection <= 0f) return 0f
    val union = a.width * a.height + b.width * b.height - intersection
    if (union <= 0f) return 0f
    return intersection / union
}
