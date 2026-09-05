package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.theme.header

/**
 * The in-list section header. All list-style sections (More tab groups,
 * Sources/Feed/History/Updates headers) converge on this style: one rank
 * below screen titles, `Typography.header` (bodyMedium/onSurfaceVariant/
 * semibold).
 */
@Composable
fun ListGroupHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        style = MaterialTheme.typography.header,
    )
}
