package eu.kanade.presentation.reader

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class ReaderArtworkToneTest {

    // Fallback: null tone (failed/unavailable/disabled/neutral) keeps chrome unchanged.
    @Test
    fun `null tone leaves base color untouched`() {
        val base = Color(0.2f, 0.4f, 0.6f)
        assertEquals(base, base.withReaderTone(null))
    }

    @Test
    fun `blend moves color by fixed small ratio`() {
        val base = Color(0f, 0f, 0f)
        val tone = Color(1f, 0f, 0f)
        val blended = base.withReaderTone(tone)
        // Compose colors are quantized to 8-bit sRGB; allow one step.
        assertTrue(abs(blended.red - READER_TONE_BLEND_RATIO) <= 1f / 255f)
        assertEquals(0f, blended.green, 1e-6f)
        assertEquals(0f, blended.blue, 1e-6f)
        assertTrue(READER_TONE_BLEND_RATIO <= 0.25f, "blend must stay subtle")
    }

    @Test
    fun `blend never produces invalid channel values`() {
        val base = Color(0.9f, 0.9f, 0.9f)
        val tone = Color(0.05f, 0.95f, 0.5f)
        val blended = base.withReaderTone(tone)
        listOf(blended.red, blended.green, blended.blue).forEach {
            assertTrue(it in 0f..1f)
        }
    }

    // No crash / no tone when artwork information is unavailable.
    @Test
    fun `empty pixels produce no average`() {
        assertNull(averageArgb(intArrayOf()))
    }

    @Test
    fun `average computes channel means with opaque alpha`() {
        val avg = averageArgb(intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt()))
        assertNotNull(avg)
        assertEquals(0xFF, (avg!! shr 24) and 0xFF)
        // 255/2 truncated to Int = 127.
        assertEquals(127, (avg shr 16) and 0xFF)
        assertEquals(127, (avg shr 8) and 0xFF)
        assertEquals(127, avg and 0xFF)
    }

    // Light/dark/AMOLED safety: near-black and near-white pages produce no tint.
    @Test
    fun `near-black page produces no tone`() {
        assertNull(normalizeReaderTone(0xFF0A0A0A.toInt()))
    }

    @Test
    fun `near-white page produces no tone`() {
        assertNull(normalizeReaderTone(0xFFF7F7F7.toInt()))
    }

    @Test
    fun `desaturated gray page produces no tone`() {
        assertNull(normalizeReaderTone(0xFF808080.toInt()))
    }

    @Test
    fun `saturated mid-luminance page produces a tone`() {
        // Strong warm tone: bright red-orange paper.
        val tone = normalizeReaderTone(0xFFD98030.toInt())
        assertNotNull(tone)
        assertTrue(tone!!.red > tone.blue)
        // Chroma gain amplifies the warm channel spread beyond the raw average.
        assertTrue(tone.red - tone.blue > 0.2f)
    }

    @Test
    fun `dark saturated page produces no tone (lineart safety)`() {
        // Saturated but very dark — must stay neutral for dark-mode legibility.
        assertNull(normalizeReaderTone(0xFF300038.toInt()))
    }
}
