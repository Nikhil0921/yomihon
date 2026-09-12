package eu.kanade.tachiyomi.ui.recent

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource

data class RecentTabContent(
    val titleRes: StringResource,
    val content: @Composable (
        contentPadding: PaddingValues,
        snackbarHostState: SnackbarHostState,
        pagerState: PagerState,
    ) -> Unit,
)
