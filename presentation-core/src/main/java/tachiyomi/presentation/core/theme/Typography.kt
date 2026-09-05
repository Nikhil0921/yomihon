package tachiyomi.presentation.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography.header: TextStyle
    @Composable
    get() = bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )

// Grid/list item title: smaller than titleSmall for dense grids, kept readable at font scale.
val Typography.itemTitle: TextStyle
    get() = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp,
    )
