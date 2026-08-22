package mihon.domain.tts

import mihon.domain.ocr.model.OcrBoundingBox
import mihon.domain.ocr.model.OcrRegion
import mihon.domain.ocr.model.OcrTextOrientation

data class TtsSentence(
    val text: String,
    val regionOrder: Int,
    val boundingBox: OcrBoundingBox,
    val textOrientation: OcrTextOrientation,
)

/**
 * Splits OCR regions into speakable sentences.
 *
 * - Regions are consumed in stored list order; never re-sorted here
 *   (must match tap-highlight behavior).
 * - A region boundary is a hard boundary: sentences never merge across regions.
 * - Within a region, splits only on terminal punctuation (kept attached to the
 *   preceding sentence); a trailing remainder becomes the final fragment.
 * - ASCII dots are deliberately NOT terminal (`...` is an ellipsis).
 */
private val TERMINALS = "。！？!?‼⁇⁉⁈".toSet()

fun List<OcrRegion>.toTtsSentences(): List<TtsSentence> =
    flatMap { region -> region.toSentences() }

private fun OcrRegion.toSentences(): List<TtsSentence> {
    if (text.isBlank()) return emptyList()
    val slices = buildList {
        val current = StringBuilder()
        for (char in text) {
            current.append(char)
            if (char in TERMINALS) {
                add(current.toString())
                current.clear()
            }
        }
        if (current.isNotBlank()) add(current.toString())
    }
    return slices.map { slice ->
        TtsSentence(slice, order, boundingBox, textOrientation)
    }
}
