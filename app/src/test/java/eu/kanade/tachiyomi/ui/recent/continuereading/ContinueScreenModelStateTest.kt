package eu.kanade.tachiyomi.ui.recent.continuereading

import eu.kanade.tachiyomi.ui.recent.continuereading.ContinueScreenModel.ContinueItem
import eu.kanade.tachiyomi.ui.recent.continuereading.ContinueScreenModel.State
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga

/**
 * Batch 6: Continue sort/filter must derive the displayed list from the
 * current raw items + current sort/filter — never destructively filter the
 * stored list, never sort from stale pre-update state.
 */
class ContinueScreenModelStateTest {

    private fun manga(id: Long, title: String) = Manga.create().copy(id = id, title = title)

    private fun libraryManga(id: Long, title: String, lastRead: Long) = LibraryManga(
        manga = manga(id, title),
        categories = emptyList(),
        totalChapters = 10,
        readCount = 5,
        bookmarkCount = 0,
        latestUpload = 0,
        chapterFetchedAt = 0,
        lastRead = lastRead,
    )

    private fun stateOf(
        vararg items: Pair<Long, String>, // id to title
        downloaded: Set<Long> = emptySet(),
        lastReadBy: (Long) -> Long = { it },
        sort: ContinueSort = ContinueSort.LAST_READ,
        downloadedOnly: Boolean = false,
    ): State {
        val raw = items.map { (id, title) ->
            ContinueItem(libraryManga(id, title, lastReadBy(id)), Chapter.create().copy(id = id + 100, mangaId = id))
        }
        return State(
            rawItems = raw,
            sort = sort,
            downloadedOnly = downloadedOnly,
            isItemDownloaded = { it.manga.id in downloaded },
        )
    }

    @Test
    fun `alphabetical sort changes ordering`() {
        // lastRead order: 3 (c) > 2 (b) > 1 (a); alpha order: a, b, c.
        val state = stateOf(
            3L to "Charlie",
            1L to "alpha",
            2L to "Bravo",
            sort = ContinueSort.ALPHA,
        )
        assertEquals(listOf(1L, 2L, 3L), state.items.map { it.manga.id })
    }

    @Test
    fun `last read sort is default and descending`() {
        val state = stateOf(
            1L to "a",
            2L to "b",
            3L to "c",
            lastReadBy = { it * 10 },
        )
        assertEquals(listOf(3L, 2L, 1L), state.items.map { it.manga.id })
    }

    @Test
    fun `downloaded only filters the raw list`() {
        val state = stateOf(
            1L to "a",
            2L to "b",
            3L to "c",
            downloaded = setOf(2L),
            downloadedOnly = true,
        )
        assertEquals(listOf(2L), state.items.map { it.manga.id })
    }

    @Test
    fun `turning downloaded only off restores the raw list`() {
        val on = stateOf(
            1L to "a",
            2L to "b",
            3L to "c",
            downloaded = setOf(2L),
            downloadedOnly = true,
        )
        val off = on.copy(downloadedOnly = false)
        // Raw list survives the filter round-trip (LAST_READ sort = lastRead
        // desc; default lastReadBy maps id -> itself).
        assertEquals(listOf(3L, 2L, 1L), off.items.map { it.manga.id })
        assertEquals(listOf(1L, 2L, 3L), off.rawItems.map { it.manga.id })
    }

    @Test
    fun `sort applies to filtered result while downloaded only is enabled`() {
        // Downloaded: 1 (alpha) + 3 (Beta). Alpha sort over filtered set.
        val state = stateOf(
            3L to "Beta",
            1L to "alpha",
            2L to "b",
            downloaded = setOf(1L, 3L),
            downloadedOnly = true,
            sort = ContinueSort.ALPHA,
        )
        assertEquals(listOf(1L, 3L), state.items.map { it.manga.id })
    }

    @Test
    fun `changing downloaded only preserves the selected sort`() {
        val withFilter = stateOf(
            1L to "a",
            2L to "b",
            3L to "c",
            downloaded = setOf(2L),
            downloadedOnly = true,
            sort = ContinueSort.ALPHA,
        )
        val toggledOff = withFilter.copy(downloadedOnly = false)
        // Sort selection survives the filter toggle.
        assertEquals(ContinueSort.ALPHA, toggledOff.sort)
        assertEquals(listOf(1L, 2L, 3L), toggledOff.items.map { it.manga.id })
    }

    @Test
    fun `applyContinueFilters composes from latest values not stale state`() {
        // Direct pure-fn check: filters use the PASSED sort/downloadedOnly,
        // never captured stale state.
        val items = listOf(
            ContinueItem(libraryManga(1, "a", lastRead = 30), null),
            ContinueItem(libraryManga(2, "b", lastRead = 10), null),
            ContinueItem(libraryManga(3, "c", lastRead = 20), null),
        )
        val downloadedOnly = applyContinueFilters(
            items = items,
            sort = ContinueSort.LAST_READ,
            downloadedOnly = true,
            isDownloaded = { it.manga.id == 3L },
        )
        assertEquals(listOf(3L), downloadedOnly.map { it.manga.id })

        val alphaAll = applyContinueFilters(
            items = items,
            sort = ContinueSort.ALPHA,
            downloadedOnly = false,
            isDownloaded = { false },
        )
        assertEquals(listOf(1L, 2L, 3L), alphaAll.map { it.manga.id })
    }
}
