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
