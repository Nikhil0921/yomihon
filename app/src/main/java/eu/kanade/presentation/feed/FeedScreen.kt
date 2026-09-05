package eu.kanade.presentation.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import eu.kanade.domain.feed.model.FeedItem
import eu.kanade.domain.feed.model.FeedListing
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.library.components.CommonMangaItemDefaults
import eu.kanade.presentation.library.components.MangaComfortableGridItem
import eu.kanade.tachiyomi.ui.feed.FeedScreenModel
import eu.kanade.tachiyomi.ui.feed.FeedSectionResult
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header

@Composable
fun FeedScreen(
    state: FeedScreenModel.State,
    onMangaClick: (Long) -> Unit,
    onAddFeedClick: () -> Unit,
    onManageFeedsClick: () -> Unit,
    onAddFeedConfirm: (Long, FeedListing) -> Unit,
    onSelectSource: (Long?) -> Unit,
    onSelectListing: (FeedListing?) -> Unit,
    onDismissAddDialog: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppBar(
                title = stringResource(MR.strings.label_feed),
                actions = {
                    AppBarActions(
                        listOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.feed_manage),
                                icon = Icons.Outlined.Tune,
                                onClick = onManageFeedsClick,
                            ),
                            AppBar.Action(
                                title = stringResource(MR.strings.feed_add),
                                icon = Icons.Outlined.Add,
                                onClick = onAddFeedClick,
                            ),
                        ),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingScreen(Modifier.padding(padding))
            return@Scaffold
        }
        if (state.feeds.isEmpty()) {
            EmptyScreen(
                stringRes = MR.strings.feed_empty,
                modifier = Modifier.padding(padding),
                actions = listOf(
                    EmptyScreenAction(
                        stringRes = MR.strings.feed_add,
                        icon = Icons.Outlined.Add,
                        onClick = onAddFeedClick,
                    ),
                ),
            )
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            FeedFilterBar(
                state = state,
                onSelectSource = onSelectSource,
                onSelectListing = onSelectListing,
            )
            val bottom = padding.calculateBottomPadding()
            LazyVerticalGrid(
                columns = GridCells.Adaptive(96.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = bottom),
                verticalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridVerticalSpacer),
                horizontalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridHorizontalSpacer),
            ) {
                state.visibleFeeds.forEach { feed ->
                    item(span = { GridItemSpan(maxLineSpan) }, contentType = { "feed_header" }) {
                        FeedHeader(feed, state)
                    }
                    when (val section = state.sections[feed]) {
                        is FeedSectionResult.Success -> items(section.mangas) { manga ->
                            MangaComfortableGridItem(
                                coverData = manga.asMangaCover(),
                                title = manga.title,
                                onClick = { onMangaClick(manga.id) },
                                onLongClick = { },
                            )
                        }
                        is FeedSectionResult.Loading -> item(
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = { "feed_section_loading" },
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.padding.medium),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        else -> item(
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = { "feed_section_error" },
                        ) {
                            Text(
                                text = (section as? FeedSectionResult.Error)?.message
                                    ?: stringResource(MR.strings.unknown_error),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(MaterialTheme.padding.medium),
                            )
                        }
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
private fun FeedFilterBar(
    state: FeedScreenModel.State,
    onSelectSource: (Long?) -> Unit,
    onSelectListing: (FeedListing?) -> Unit,
) {
    val enabledFeeds = state.feeds.filter { it.enabled }
    val feedSourceIds = enabledFeeds.map { it.sourceId }.distinct()
    val feedSources = feedSourceIds.mapNotNull { id -> state.sources.firstOrNull { it.id == id } }
    if (feedSources.size < 2 && state.listingOverride == null) return

    Column {
        if (feedSources.size >= 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.padding.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                FilterChip(
                    selected = state.selectedSourceId == null,
                    onClick = { onSelectSource(null) },
                    label = { Text(stringResource(MR.strings.feed_all_sources)) },
                )
                feedSources.forEach { source ->
                    FilterChip(
                        selected = state.selectedSourceId == source.id,
                        onClick = { onSelectSource(source.id) },
                        label = { Text(source.visualName) },
                    )
                }
            }
        }
        if (enabledFeeds.map { it.listing }.distinct().size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                FilterChip(
                    selected = state.listingOverride == null,
                    onClick = { onSelectListing(null) },
                    label = { Text(stringResource(MR.strings.all)) },
                )
                FilterChip(
                    selected = state.listingOverride == FeedListing.POPULAR,
                    onClick = { onSelectListing(FeedListing.POPULAR) },
                    label = { Text(stringResource(MR.strings.popular)) },
                )
                FilterChip(
                    selected = state.listingOverride == FeedListing.LATEST,
                    onClick = { onSelectListing(FeedListing.LATEST) },
                    label = { Text(stringResource(MR.strings.latest)) },
                )
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun FeedHeader(feed: FeedItem, state: FeedScreenModel.State) {
    val source = state.sources.firstOrNull { it.id == feed.sourceId }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = source?.visualName ?: stringResource(MR.strings.feed_source_unavailable),
                    style = MaterialTheme.typography.header,
                )
                Text(
                    text = stringResource(
                        if (feed.listing == FeedListing.LATEST) MR.strings.latest else MR.strings.popular,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        HorizontalDivider()
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
                Text(stringResource(MR.strings.feed_select_source), style = MaterialTheme.typography.header)
                sources.forEach { source ->
                    val selected = selectedSource?.id == source.id
                    ListItem(
                        headlineContent = { Text(source.visualName) },
                        supportingContent = { Text(source.lang) },
                        trailingContent = {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    selectedSource = source
                                    if (!source.supportsLatest) selectedListing = FeedListing.POPULAR
                                },
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSource = source
                                if (!source.supportsLatest) selectedListing = FeedListing.POPULAR
                            },
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.padding.small))
                Text(stringResource(MR.strings.feed_select_listing), style = MaterialTheme.typography.header)
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                    FilterChip(
                        selected = selectedListing == FeedListing.POPULAR,
                        onClick = { selectedListing = FeedListing.POPULAR },
                        label = { Text(stringResource(MR.strings.popular)) },
                    )
                    val latestSupported = selectedSource?.supportsLatest ?: true
                    FilterChip(
                        selected = selectedListing == FeedListing.LATEST,
                        onClick = { if (latestSupported) selectedListing = FeedListing.LATEST },
                        enabled = latestSupported,
                        label = { Text(stringResource(MR.strings.latest)) },
                    )
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
