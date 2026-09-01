package eu.kanade.tachiyomi.ui.feed

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.feed.model.FeedItem
import eu.kanade.domain.feed.model.FeedListing
import eu.kanade.domain.feed.service.FeedPreferences
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.domain.manga.model.toDomainManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

sealed interface FeedSectionResult {
    data object Loading : FeedSectionResult
    data class Success(val mangas: List<Manga>) : FeedSectionResult
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
    )

    init {
        screenModelScope.launch {
            feedPreferences.feeds().changes().collect { feeds ->
                mutableState.update { it.copy(feeds = feeds) }
                loadSections(feeds)
            }
        }
        screenModelScope.launch {
            val sources = getEnabledSources.subscribe()
            sources.collect { list ->
                mutableState.update {
                    it.copy(isLoading = false, sources = list.filterNot { s -> s.isStub })
                }
            }
        }
    }

    private fun loadSections(feeds: List<FeedItem>) {
        screenModelScope.launch {
            val enabled = feeds.filter { it.enabled }
            mutableState.update { state ->
                state.copy(
                    sections = enabled.associateWith { state.sections[it] ?: FeedSectionResult.Loading },
                )
            }
            enabled.map { feed ->
                async {
                    val result = fetchSection(feed)
                    mutableState.update { it.copy(sections = it.sections + (feed to result)) }
                }
            }.awaitAll()
        }
    }

    private suspend fun fetchSection(feed: FeedItem): FeedSectionResult {
        val source = sourceManager.get(feed.sourceId) as? CatalogueSource
            ?: return FeedSectionResult.Error(null)
        return try {
            val page = when (feed.listing) {
                FeedListing.POPULAR -> source.getPopularManga(1)
                FeedListing.LATEST -> source.getLatestUpdates(1)
            }
            val mangas = page.mangas
                .map { it.toDomainManga(source.id) }
                .distinctBy { it.url }
                .let { networkToLocalManga(it) }
            FeedSectionResult.Success(mangas)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            FeedSectionResult.Error(e.message)
        }
    }

    fun showAddDialog() {
        mutableState.update { it.copy(showAddDialog = true) }
    }

    fun dismissAddDialog() {
        mutableState.update { it.copy(showAddDialog = false) }
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
}
