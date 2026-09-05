package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val topSmallPaddingValues = PaddingValues(top = MaterialTheme.padding.small)

const val DISABLED_ALPHA = .38f
const val SECONDARY_ALPHA = .78f

// Standard alpha for tonal pill containers (counts, chips) over background.
const val PILL_ALPHA = 0.12f

// Neutral placeholder for cover/icon images while loading.
val CoverPlaceholderColor = Color(0x1F888888)

class Padding {

    val extraLarge = 32.dp

    val large = 24.dp

    val medium = 16.dp

    val small = 8.dp

    val extraSmall = 4.dp
}

val MaterialTheme.padding: Padding
    get() = Padding()
