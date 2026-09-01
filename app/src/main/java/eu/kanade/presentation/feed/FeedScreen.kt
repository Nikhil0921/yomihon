package eu.kanade.presentation.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.feed.model.FeedItem
import eu.kanade.domain.feed.model.FeedListing
import eu.kanade.presentation.library.components.MangaComfortableGridItem
import eu.kanade.tachiyomi.ui.feed.FeedScreenModel
import eu.kanade.tachiyomi.ui.feed.FeedSectionResult
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun FeedScreen(
    state: FeedScreenModel.State,
    onMangaClick: (Long) -> Unit,
    onAddFeedClick: () -> Unit,
    onDeleteFeedClick: (FeedItem) -> Unit,
    onToggleFeed: (FeedItem, Boolean) -> Unit,
    onMoveFeedUp: (FeedItem) -> Unit,
    onMoveFeedDown: (FeedItem) -> Unit,
    onAddFeedConfirm: (Long, FeedListing) -> Unit,
    onDismissAddDialog: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(MR.strings.label_feed)) },
                actions = {
                    IconButton(onClick = onAddFeedClick) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(MR.strings.feed_add),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingScreen(Modifier.padding(padding))
            return@Scaffold
        }
        if (state.feeds.isEmpty()) {
            FeedEmptyScreen(Modifier.padding(padding), onAddFeedClick)
            return@Scaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(96.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.feeds.forEach { feed ->
                item(span = {
                    GridItemSpan(maxLineSpan)
                }) { FeedHeader(feed, state, onDeleteFeedClick, onToggleFeed, onMoveFeedUp, onMoveFeedDown) }
                when (val section = state.sections[feed]) {
                    is FeedSectionResult.Success -> items(section.mangas) { manga ->
                        MangaComfortableGridItem(
                            coverData = manga.asMangaCover(),
                            title = manga.title,
                            onClick = { onMangaClick(manga.id) },
                            onLongClick = { },
                        )
                    }
                    else -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(
                                when (section) {
                                    is FeedSectionResult.Loading -> MR.strings.loading
                                    else -> MR.strings.unknown_error
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddFeedDialog(
            sources = state.sources,
            onConfirm = onAddFeedConfirm,
            onDismiss = onDismissAddDialog,
        )
    }
}

@Composable
private fun FeedHeader(
    feed: FeedItem,
    state: FeedScreenModel.State,
    onDeleteFeedClick: (FeedItem) -> Unit,
    onToggleFeed: (FeedItem, Boolean) -> Unit,
    onMoveFeedUp: (FeedItem) -> Unit,
    onMoveFeedDown: (FeedItem) -> Unit,
) {
    val source = state.sources.firstOrNull { it.id == feed.sourceId }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = source?.visualName ?: stringResource(MR.strings.feed_source_unavailable),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        if (feed.listing == FeedListing.LATEST) MR.strings.latest else MR.strings.popular,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = { onMoveFeedUp(feed) }) {
                Icon(Icons.Outlined.ArrowUpward, contentDescription = stringResource(MR.strings.feed_move_up))
            }
            IconButton(onClick = { onMoveFeedDown(feed) }) {
                Icon(Icons.Outlined.ArrowDownward, contentDescription = stringResource(MR.strings.feed_move_down))
            }
            Switch(checked = feed.enabled, onCheckedChange = { onToggleFeed(feed, it) })
            IconButton(onClick = { onDeleteFeedClick(feed) }) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(MR.strings.action_delete))
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun FeedEmptyScreen(modifier: Modifier, onAddFeedClick: () -> Unit) {
    Column(modifier = modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(MR.strings.feed_empty),
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(onClick = onAddFeedClick) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text(stringResource(MR.strings.feed_add))
        }
    }
}

@Composable
private fun AddFeedDialog(
    sources: List<Source>,
    onConfirm: (Long, FeedListing) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedSource by remember { mutableStateOf<Source?>(null) }
    var selectedListing by remember { mutableStateOf(FeedListing.LATEST) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.feed_add)) },
        text = {
            Column {
                Text(stringResource(MR.strings.feed_select_source), style = MaterialTheme.typography.titleSmall)
                sources.forEach { source ->
                    ListItem(
                        headlineContent = { Text(source.visualName) },
                        supportingContent = { Text(source.lang) },
                        trailingContent = if (selectedSource?.id == source.id) {
                            { Icon(Icons.Outlined.Check, contentDescription = null) }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSource = source
                                if (!source.supportsLatest) selectedListing = FeedListing.POPULAR
                            },
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(stringResource(MR.strings.feed_select_listing), style = MaterialTheme.typography.titleSmall)
                Row {
                    TextButton(
                        onClick = { selectedListing = FeedListing.POPULAR },
                    ) {
                        Text(
                            stringResource(MR.strings.popular) +
                                if (selectedListing == FeedListing.POPULAR) " ✓" else "",
                        )
                    }
                    val latestSupported = selectedSource?.supportsLatest ?: true
                    TextButton(
                        onClick = { if (latestSupported) selectedListing = FeedListing.LATEST },
                        enabled = latestSupported,
                    ) {
                        Text(
                            stringResource(MR.strings.latest) +
                                if (selectedListing == FeedListing.LATEST) " ✓" else "",
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedSource?.let { onConfirm(it.id, selectedListing) } },
                enabled = selectedSource != null,
            ) {
                Text(stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}
