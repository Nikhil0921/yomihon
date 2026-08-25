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
 * - CJK terminals always split. An ASCII period splits only as a single dot
 *   before whitespace/end of region, so ellipses ("...") stay glued and
 *   decimals ("3.14") never split.
 */
private val TERMINALS = "。！？!?‼⁇⁉⁈".toSet()

fun List<OcrRegion>.toTtsSentences(): List<TtsSentence> =
    flatMap { region -> region.toSentences() }

private fun OcrRegion.toSentences(): List<TtsSentence> {
    if (text.isBlank()) return emptyList()
    val slices = buildList {
        val current = StringBuilder()
        for ((index, char) in text.withIndex()) {
            current.append(char)
            if (char.isTerminalAt(text, index)) {
                add(current.toString())
                current.clear()
            }
        }
        if (current.isNotBlank()) add(current.toString())
    }
    return slices.mapNotNull { slice ->
        slice.trim().takeIf { it.isNotBlank() }?.let {
            TtsSentence(it, order, boundingBox, textOrientation)
        }
    }
}

private fun Char.isTerminalAt(text: String, index: Int): Boolean {
    if (this in TERMINALS) return true
    if (this != '.') return false
    if (index > 0 && text[index - 1] == '.') return false
    val next = text.getOrNull(index + 1) ?: return true
    return next.isWhitespace()
}
