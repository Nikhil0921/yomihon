package eu.kanade.presentation.reader

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import logcat.logcat
import tachiyomi.decoder.ImageDecoder
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

// Artwork-reactive reader chrome: a subtle tint derived from the current
// manga page that blends into the floating-chrome container color before
// the frost role is applied. Material color tokens remain authoritative;
// the artwork tone only nudges the container at a fixed small ratio.
//
// Deliberate guards keep this nearly invisible on neutral artwork: plain
// black/white/gray pages (most manga) produce no tint at all, so busy and
// untoned pages leave the existing chrome unchanged. Only pages with a
// strong, mid-luminance dominant tone shift the chrome slightly.

/** How much the artwork tone may shift the chrome container color. */
internal const val READER_TONE_BLEND_RATIO = 0.20f

/** Pages darker than this luma produce no tint (lineart/near-black paper). */
private const val MIN_TONE_LUMA = 0.18f

/** Pages brighter than this luma produce no tint (near-white paper). */
private const val MAX_TONE_LUMA = 0.82f

/** Gray/desaturated averages produce no tint. */
private const val MIN_TONE_SATURATION = 0.12f

/** Largest bitmap side used for tone sampling (sampleSize rounds to this). */
private const val TONE_SAMPLE_TARGET_PX = 48

/**
 * Blends [tone] into this color at [READER_TONE_BLEND_RATIO]. A null tone
 * (analysis failed, unavailable, disabled, or neutral artwork) returns this
 * color untouched — the fallback is the existing chrome, always.
 */
fun Color.withReaderTone(tone: Color?): Color {
    if (tone == null) return this
    val t = READER_TONE_BLEND_RATIO
    return Color(
        red = red + (tone.red - red) * t,
        green = green + (tone.green - green) * t,
        blue = blue + (tone.blue - blue) * t,
        alpha = alpha,
    )
}

/**
 * Mean of packed ARGB pixels, alpha forced opaque. Returns null for empty
 * input (no artwork information → chrome unchanged).
 */
internal fun averageArgb(pixels: IntArray): Int? {
    if (pixels.isEmpty()) return null
    var r = 0L
    var g = 0L
    var b = 0L
    for (p in pixels) {
        r += (p shr 16 and 0xFF).toLong()
        g += (p shr 8 and 0xFF).toLong()
        b += (p and 0xFF).toLong()
    }
    val n = pixels.size.toLong()
    return 0xFF shl 24 or ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt()
}

/**
 * Normalizes an average page color into a chrome tone, or null when the
 * page is neutral: too dark, too bright, or too desaturated. Channels are
 * kept as-is; the small blend ratio guarantees subtlety.
 */
internal fun normalizeReaderTone(argb: Int): Color? {
    val r = (argb shr 16 and 0xFF) / 255f
    val g = (argb shr 8 and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    if (maxC <= 0f) return null
    if ((maxC - minC) / maxC < MIN_TONE_SATURATION) return null
    val luma = 0.299f * r + 0.587f * g + 0.114f * b
    if (luma < MIN_TONE_LUMA || luma > MAX_TONE_LUMA) return null
    // Amplify chroma around the page's own luma so the tone stays legible
    // after the small blend into a near-black AMOLED chrome base.
    val boosted = luma + (r - luma) * TONE_CHROMA_GAIN
    val boostedG = luma + (g - luma) * TONE_CHROMA_GAIN
    val boostedB = luma + (b - luma) * TONE_CHROMA_GAIN
    return Color(
        boosted.coerceIn(0f, 1f),
        boostedG.coerceIn(0f, 1f),
        boostedB.coerceIn(0f, 1f),
    )
}

/** Chroma amplification applied to accepted tones (1 = as sampled). */
private const val TONE_CHROMA_GAIN = 2.5f

/**
 * Samples a reader page stream into a chrome tone. Decodes a tiny
 * (≤ [TONE_SAMPLE_TARGET_PX]-ish) software bitmap with the existing
 * [ImageDecoder], averages it, and normalizes. Any failure returns null —
 * the caller must treat null as "leave the chrome unchanged". Runs on an
 * IO dispatcher; never on the main thread.
 */
internal fun sampleReaderPageTone(streamFn: (() -> InputStream)?): Color? {
    if (streamFn == null) {
        logcat(priority = LogPriority.DEBUG, tag = "ReaderArtworkTone") { "tone sample: stream null at sample time" }
        return null
    }
    return try {
        streamFn().use { input ->
            val decoder = ImageDecoder.newInstance(input) ?: run {
                logcat(priority = LogPriority.DEBUG, tag = "ReaderArtworkTone") { "tone sample: decoder null" }
                return null
            }
            try {
                val width = decoder.width
                val height = decoder.height
                if (width <= 0 || height <= 0) {
                    logcat(priority = LogPriority.DEBUG, tag = "ReaderArtworkTone") { "tone sample: bad dims" }
                    return null
                }
                var sampleSize = 1
                while (max(width, height) / sampleSize > TONE_SAMPLE_TARGET_PX) {
                    sampleSize *= 2
                }
                val bitmap = decoder.decode(sampleSize = sampleSize) ?: run {
                    logcat(priority = LogPriority.DEBUG, tag = "ReaderArtworkTone") { "tone sample: decode null" }
                    return null
                }
                try {
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    val avg = averageArgb(pixels)
                    val tone = avg?.let(::normalizeReaderTone)
                    logcat(priority = LogPriority.DEBUG, tag = "ReaderArtworkTone") {
                        "tone sample: ${bitmap.width}x${bitmap.height} avg=${avg ?: 0} tone=${tone ?: Color.Unspecified}"
                    }
                    tone
                } finally {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            } finally {
                decoder.recycle()
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logcat(priority = LogPriority.DEBUG, tag = "ReaderArtworkTone") {
            "Reader artwork tone sampling failed: ${e.javaClass.simpleName}"
        }
        null
    }
}
