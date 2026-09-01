package eu.kanade.tachiyomi.ui.feed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Feed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.feed.FeedScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object FeedTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 5u,
            title = stringResource(MR.strings.label_feed),
            icon = rememberVectorPainter(Icons.Outlined.Feed),
        )

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { FeedScreenModel() }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        FeedScreen(
            state = state,
            onMangaClick = { mangaId -> navigator.push(MangaScreen(mangaId, true)) },
            onAddFeedClick = { screenModel.showAddDialog() },
            onDeleteFeedClick = { screenModel.deleteFeed(it) },
            onToggleFeed = { feed, enabled -> screenModel.setFeedEnabled(feed, enabled) },
            onMoveFeedUp = { screenModel.moveFeedUp(it) },
            onMoveFeedDown = { screenModel.moveFeedDown(it) },
            onAddFeedConfirm = { sourceId, listing -> screenModel.addFeed(sourceId, listing) },
            onDismissAddDialog = { screenModel.dismissAddDialog() },
        )

        val context = LocalContext.current
        androidx.compose.runtime.LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
