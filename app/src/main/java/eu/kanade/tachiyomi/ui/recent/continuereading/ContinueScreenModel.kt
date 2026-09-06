package eu.kanade.tachiyomi.ui.recent.continuereading

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.util.chapter.getNextUnread
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

enum class ContinueSort(val labelRes: dev.icerock.moko.resources.StringResource) {
    LAST_READ(tachiyomi.i18n.MR.strings.action_sort_last_read),
    ALPHA(tachiyomi.i18n.MR.strings.action_sort_alpha),
}

class ContinueScreenModel(
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
    private val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
) : StateScreenModel<ContinueScreenModel.State>(State()) {

    @Immutable
    data class ContinueItem(
        val manga: LibraryManga,
        val nextChapter: Chapter?,
    )

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val items: List<ContinueItem> = emptyList(),
        val sort: ContinueSort = ContinueSort.LAST_READ,
        val downloadedOnly: Boolean = false,
    )

    init {
        screenModelScope.launch {
            getLibraryManga.subscribe()
                .collectLatest { mangas ->
                    val resumable = mangas
                        // Started reading (real reading history) and still has
                        // something left to read.
                        .filter { it.unreadCount > 0 && it.hasStarted }
                    val items = resumable.map { m ->
                        ContinueItem(m, getNextUnreadChapter(m.manga))
                    }
                    mutableState.update { it.copy(isLoading = false, items = applyFilters(items)) }
                }
        }
    }

    fun setSort(sort: ContinueSort) {
        mutableState.update { it.copy(sort = sort, items = applyFilters(it.items)) }
    }

    fun setDownloadedOnly(enabled: Boolean) {
        mutableState.update { it.copy(downloadedOnly = enabled, items = applyFilters(it.items)) }
    }

    private fun applyFilters(items: List<ContinueItem>): List<ContinueItem> {
        var result = items
        if (state.value.downloadedOnly) {
            result = result.filter { item ->
                item.nextChapter?.let { isDownloaded(it, item.manga.manga) } == true
            }
        }
        return when (state.value.sort) {
            ContinueSort.LAST_READ -> result.sortedByDescending { it.manga.lastRead }
            ContinueSort.ALPHA -> result.sortedBy { it.manga.manga.title.lowercase() }
        }
    }

    private fun isDownloaded(chapter: Chapter, manga: Manga): Boolean {
        return downloadManager.isChapterDownloaded(
            chapterName = chapter.name,
            chapterScanlator = chapter.scanlator,
            chapterUrl = chapter.url,
            mangaTitle = manga.title,
            sourceId = manga.source,
        )
    }

    private suspend fun getNextUnreadChapter(manga: Manga): Chapter? {
        val chapters = getChaptersByMangaId.await(manga.id, applyScanlatorFilter = true)
        return chapters.getNextUnread(manga, downloadManager)
    }
}
