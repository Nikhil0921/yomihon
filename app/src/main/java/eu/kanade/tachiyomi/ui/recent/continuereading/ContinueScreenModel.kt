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
    )

    init {
        screenModelScope.launch {
            getLibraryManga.subscribe()
                .collectLatest { mangas ->
                    val resumable = mangas
                        .filter { it.unreadCount > 0 && it.hasStarted }
                        .sortedByDescending { it.lastRead }
                    val items = resumable.map { m ->
                        ContinueItem(m, getNextUnreadChapter(m.manga))
                    }
                    mutableState.update { it.copy(isLoading = false, items = items) }
                }
        }
    }

    private suspend fun getNextUnreadChapter(manga: Manga): Chapter? {
        val chapters = getChaptersByMangaId.await(manga.id, applyScanlatorFilter = true)
        return chapters.getNextUnread(manga, downloadManager)
    }
}
