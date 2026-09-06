package eu.kanade.tachiyomi.ui.feed

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.asState
import eu.kanade.domain.feed.model.FeedItem
import eu.kanade.domain.feed.model.FeedListing
import eu.kanade.domain.feed.service.FeedPreferences
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

sealed interface FeedSectionResult {
    data object Loading : FeedSectionResult
    data class Success(
        val mangas: List<Manga>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
        val error: String? = null,
    ) : FeedSectionResult
    data class Error(val message: String?) : FeedSectionResult
}

class FeedScreenModel(
    private val sourceManager: SourceManager = Injekt.get(),
    private val getEnabledSources: GetEnabledSources = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val feedPreferences: FeedPreferences = Injekt.get(),
) : StateScreenModel<FeedScreenModel.State>(State()) {

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val feeds: List<FeedItem> = emptyList(),
        val sections: Map<FeedItem, FeedSectionResult> = emptyMap(),
        val sources: List<tachiyomi.domain.source.model.Source> = emptyList(),
        val showAddDialog: Boolean = false,
        val selectedSourceId: Long? = null,
        val listingOverride: FeedListing? = null,
        val showSourceSelector: Boolean = true,
        val showListingSelector: Boolean = true,
        val defaultListing: FeedListing? = null,
    ) {
        val visibleFeeds: List<FeedItem>
            get() = feeds.filter { feed ->
                (selectedSourceId == null || feed.sourceId == selectedSourceId) &&
                    (listingOverride == null || feed.listing == listingOverride)
            }
    }

    private var sectionJobs: MutableMap<FeedItem, Job> = mutableMapOf()

    val gridColumns = feedPreferences.gridColumns().asState(screenModelScope)

    init {
        screenModelScope.launch {
            launch {
                feedPreferences.feeds().changes().collect { feeds ->
                    mutableState.update { it.copy(feeds = feeds) }
                    loadSections(feeds)
                }
            }
            launch {
                combine(
                    feedPreferences.showSourceSelector().changes(),
                    feedPreferences.showListingSelector().changes(),
                    feedPreferences.defaultListing().changes(),
                ) { showSource, showListing, defaultListing ->
                    Triple(showSource, showListing, defaultListing)
                }.collect { (showSource, showListing, defaultListing) ->
                    mutableState.update {
                        it.copy(
                            showSourceSelector = showSource,
                            showListingSelector = showListing,
                            defaultListing = defaultListing,
                            listingOverride = it.listingOverride ?: defaultListing,
                        )
                    }
                }
            }
            launch {
                val sources = getEnabledSources.subscribe()
                sources.collect { list ->
                    mutableState.update {
                        it.copy(isLoading = false, sources = list.filterNot { s -> s.isStub })
                    }
                }
            }
        }
    }

    private fun loadSections(feeds: List<FeedItem>) {
        val enabled = feeds.filter { it.enabled }
        // Keep sections for feeds that still exist; drop the rest.
        mutableState.update { state ->
            state.copy(
                sections = enabled.associateWith { state.sections[it] ?: FeedSectionResult.Loading },
            )
        }
        enabled.forEach { feed ->
            if (state.value.sections[feed] is FeedSectionResult.Success) return@forEach
            sectionJobs.remove(feed)?.cancel()
            sectionJobs[feed] = screenModelScope.launch {
                val result = fetchSection(feed, page = 1)
                mutableState.update { it.copy(sections = it.sections + (feed to result)) }
            }
        }
    }

    /**
     * Explicitly loads the next page for one feed/listing. State-controlled:
     * no prefetch, no auto-infinite scroll.
     */
    fun loadMore(feed: FeedItem) {
        val section = state.value.sections[feed] as? FeedSectionResult.Success ?: return
        if (section.isLoadingMore || !section.hasMore) return
        sectionJobs.remove(feed)?.cancel()
        sectionJobs[feed] = screenModelScope.launch {
            mutableState.update {
                it.copy(sections = it.sections + (feed to section.copy(isLoadingMore = true)))
            }
            val nextPageNumber = section.mangas.size / PAGE_SIZE + 2
            val result = fetchSection(feed, page = nextPageNumber, appendTo = section)
            mutableState.update { it.copy(sections = it.sections + (feed to result)) }
        }
    }

    private suspend fun fetchSection(
        feed: FeedItem,
        page: Int,
        appendTo: FeedSectionResult.Success? = null,
    ): FeedSectionResult {
        val source = sourceManager.get(feed.sourceId) as? CatalogueSource
            ?: return FeedSectionResult.Error(null)
        return try {
            val pageResult = when (feed.listing) {
                FeedListing.POPULAR -> source.getPopularManga(page)
                FeedListing.LATEST -> source.getLatestUpdates(page)
            }
            val freshMangas = pageResult.mangas
                .map { it.toDomainManga(source.id) }
                .let { networkToLocalManga(it) }
            val mangas = if (appendTo == null) {
                freshMangas.distinctBy { it.url }
            } else {
                (appendTo.mangas + freshMangas).distinctBy { it.url }
            }
            FeedSectionResult.Success(
                mangas = mangas,
                hasMore = pageResult.hasNextPage,
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logcat(LogPriority.DEBUG) { "Feed fetch failed source=${feed.sourceId} page=$page" }
            if (appendTo != null) {
                appendTo.copy(isLoadingMore = false)
            } else {
                FeedSectionResult.Error(e.message)
            }
        }
    }

    fun showAddDialog() {
        mutableState.update { it.copy(showAddDialog = true) }
    }

    fun dismissAddDialog() {
        mutableState.update { it.copy(showAddDialog = false) }
    }

    fun selectSource(sourceId: Long?) {
        mutableState.update { it.copy(selectedSourceId = sourceId) }
    }

    fun selectListing(listing: FeedListing?) {
        mutableState.update { it.copy(listingOverride = listing) }
    }

    fun setDefaultListing(listing: FeedListing?) {
        feedPreferences.defaultListing().set(listing)
        mutableState.update { it.copy(listingOverride = listing) }
    }

    fun toggleSourceSelector(show: Boolean) {
        feedPreferences.showSourceSelector().set(show)
    }

    fun toggleListingSelector(show: Boolean) {
        feedPreferences.showListingSelector().set(show)
    }

    fun setGridColumns(columns: Int) {
        feedPreferences.gridColumns().set(columns)
    }

    fun retry(feed: FeedItem) {
        sectionJobs.remove(feed)?.cancel()
        mutableState.update { it.copy(sections = it.sections + (feed to FeedSectionResult.Loading)) }
        sectionJobs[feed] = screenModelScope.launch {
            val result = fetchSection(feed, page = 1)
            mutableState.update { it.copy(sections = it.sections + (feed to result)) }
        }
    }

    fun addFeed(sourceId: Long, listing: FeedListing) {
        val feeds = feedPreferences.feeds().get()
        if (feeds.any { it.sourceId == sourceId && it.listing == listing }) return
        feedPreferences.feeds().set(feeds + FeedItem(sourceId, listing))
        mutableState.update { it.copy(showAddDialog = false) }
    }

    fun deleteFeed(feed: FeedItem) {
        feedPreferences.feeds().set(feedPreferences.feeds().get() - feed)
    }

    fun setFeedEnabled(feed: FeedItem, enabled: Boolean) {
        val feeds = feedPreferences.feeds().get()
        val index = feeds.indexOfFirst { it == feed.copy(enabled = true) || it == feed }
        if (index >= 0) {
            feedPreferences.feeds().set(feeds.toMutableList().also { it[index] = feed.copy(enabled = enabled) })
        }
    }

    fun moveFeedUp(feed: FeedItem) = moveFeed(feed, -1)

    fun moveFeedDown(feed: FeedItem) = moveFeed(feed, 1)

    private fun moveFeed(feed: FeedItem, delta: Int) {
        val feeds = feedPreferences.feeds().get().toMutableList()
        val index = feeds.indexOfFirst { it.sourceId == feed.sourceId && it.listing == feed.listing }
        val target = index + delta
        if (index < 0 || target !in feeds.indices) return
        val item = feeds.removeAt(index)
        feeds.add(target, item)
        feedPreferences.feeds().set(feeds)
    }

    private companion object {
        // ponytail: page-size assumption for append page math; per-feed server sizes vary,
        // swap for a stored page counter if a source returns uneven pages.
        const val PAGE_SIZE = 20
    }
}
