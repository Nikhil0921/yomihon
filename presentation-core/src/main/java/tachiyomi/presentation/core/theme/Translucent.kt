package tachiyomi.presentation.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Translucent ("Mica-like") surface treatment. When enabled, chrome
 * surfaces (nav pill, sheets, bars) use translucent container colors;
 * when disabled, normal opaque Material surfaces.
 */
val LocalTranslucentSurfaces = staticCompositionLocalOf { false }

// Container alpha for translucent chrome. Pre-blended with the theme
// background (see [asChromeContainer]) so content behind stays legible
// while the surface still reads as see-through.
private const val TRANSLUCENT_CONTAINER_ALPHA = 0.82f

// Alpha for reader floating chrome. No pre-blend: the backdrop there is
// the manga artwork itself, so see-through shows content.
private const val FLOATING_CHROME_ALPHA = 0.85f

/**
 * Chrome container color for the current mode: translucent (pre-blended
 * with background) when [LocalTranslucentSurfaces] is enabled, the
 * passed color untouched otherwise.
 */
@androidx.compose.runtime.Composable
fun Color.asChromeContainer(): Color {
    if (!LocalTranslucentSurfaces.current) return this
    val background = MaterialTheme.colorScheme.background
    val a = TRANSLUCENT_CONTAINER_ALPHA
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
        alpha = 1f,
    )
}

/**
 * Frosted reader chrome role: surfaces floating directly over the manga
 * artwork (reader bars, pill, indicators). Real translucency — the artwork
 * behind is the frost. Callers must pass a scrim-adequate container color
 * and onSurfaceVariant+ content colors. Never nest frosted surfaces.
 * Returns this (opaque) when translucent surfaces are disabled.
 */
@androidx.compose.runtime.Composable
fun Color.asFloatingChrome(): Color {
    if (!LocalTranslucentSurfaces.current) return this
    return copy(alpha = FLOATING_CHROME_ALPHA)
}

/**
 * Frosted modal role: sheets/modals floating above content (not artwork).
 * Alias of [asChromeContainer] semantics — opaque pre-blend with the theme
 * background, no real see-through.
 */
@androidx.compose.runtime.Composable
fun Color.asFrostedModal(): Color = asChromeContainer()
