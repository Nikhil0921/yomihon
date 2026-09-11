package eu.kanade.tachiyomi.ui.reader.setting

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReaderBottomBarActionTest {

    private val default = ReaderBottomBarAction.DEFAULT_ORDER

    @Test
    fun `default order preserved`() {
        assertEquals(
            listOf(
                ReaderBottomBarAction.READING_MODE,
                ReaderBottomBarAction.ORIENTATION,
                ReaderBottomBarAction.CROP_BORDERS,
                ReaderBottomBarAction.OCR,
                ReaderBottomBarAction.READ_ALOUD,
                ReaderBottomBarAction.SETTINGS,
            ),
            default,
        )
    }

    @Test
    fun `empty stored state falls back to defaults`() {
        assertEquals(default, ReaderBottomBarAction.fromStoredIds(listOf("")))
        assertEquals(default, ReaderBottomBarAction.fromStoredIds(emptyList()))
    }

    @Test
    fun `custom order round-trips through serialization`() {
        val custom = listOf(
            ReaderBottomBarAction.READ_ALOUD,
            ReaderBottomBarAction.OCR,
            ReaderBottomBarAction.READING_MODE,
            ReaderBottomBarAction.ORIENTATION,
            ReaderBottomBarAction.CROP_BORDERS,
            ReaderBottomBarAction.SETTINGS,
        )
        val restored = ReaderBottomBarAction.fromStoredIds(
            ReaderBottomBarAction.serialize(custom).split(','),
        )
        assertEquals(custom, restored)
    }

    @Test
    fun `reorder changes displayed order`() {
        val stored = listOf("OCR", "READING_MODE", "ORIENTATION", "CROP_BORDERS", "READ_ALOUD")
        assertEquals(
            listOf(
                ReaderBottomBarAction.OCR,
                ReaderBottomBarAction.READING_MODE,
                ReaderBottomBarAction.ORIENTATION,
                ReaderBottomBarAction.CROP_BORDERS,
                ReaderBottomBarAction.READ_ALOUD,
                ReaderBottomBarAction.SETTINGS,
            ),
            ReaderBottomBarAction.fromStoredIds(stored),
        )
    }

    @Test
    fun `hidden action remains in stored ordering`() {
        // Visibility is separate from ordering: stored order keeps all actions.
        val stored = listOf("OCR", "READING_MODE", "ORIENTATION", "CROP_BORDERS", "READ_ALOUD")
        val order = ReaderBottomBarAction.fromStoredIds(stored)
        assertEquals(6, order.size)
        assertEquals(ReaderBottomBarAction.OCR, order.first())
    }

    @Test
    fun `hidden to visible restores stored position`() {
        // Position comes from the order pref only; toggling visibility never
        // rewrites it, so restore lands exactly where the user left it.
        val stored = listOf("READ_ALOUD", "OCR", "READING_MODE", "ORIENTATION", "CROP_BORDERS")
        val before = ReaderBottomBarAction.fromStoredIds(stored)
        val visibleBefore = before.filterNot { it == ReaderBottomBarAction.OCR }
        val visibleAfter = ReaderBottomBarAction.fromStoredIds(stored).filterNot {
            it == ReaderBottomBarAction.SETTINGS
        }
        assertEquals(
            visibleBefore.indexOf(ReaderBottomBarAction.READ_ALOUD),
            visibleAfter.indexOf(ReaderBottomBarAction.READ_ALOUD),
        )
    }

    @Test
    fun `missing actions appended deterministically in default order`() {
        // New action introduced after v1: stored string lacks CROP_BORDERS.
        val stored = listOf("OCR", "READ_ALOUD", "READING_MODE", "ORIENTATION")
        assertEquals(
            listOf(
                ReaderBottomBarAction.OCR,
                ReaderBottomBarAction.READ_ALOUD,
                ReaderBottomBarAction.READING_MODE,
                ReaderBottomBarAction.ORIENTATION,
                ReaderBottomBarAction.CROP_BORDERS,
                ReaderBottomBarAction.SETTINGS,
            ),
            ReaderBottomBarAction.fromStoredIds(stored),
        )
    }

    @Test
    fun `unknown stored ids ignored without crash`() {
        assertEquals(
            default,
            ReaderBottomBarAction.fromStoredIds(listOf("BOGUS", "ANOTHER")),
        )
        assertEquals(
            listOf(
                ReaderBottomBarAction.OCR,
                ReaderBottomBarAction.READING_MODE,
                ReaderBottomBarAction.ORIENTATION,
                ReaderBottomBarAction.CROP_BORDERS,
                ReaderBottomBarAction.READ_ALOUD,
                ReaderBottomBarAction.SETTINGS,
            ),
            ReaderBottomBarAction.fromStoredIds(
                listOf("BOGUS", "OCR", "READING_MODE", "ORIENTATION", "CROP_BORDERS", "READ_ALOUD"),
            ),
        )
    }

    @Test
    fun `duplicate stored ids deduped`() {
        assertEquals(
            listOf(
                ReaderBottomBarAction.OCR,
                ReaderBottomBarAction.READING_MODE,
                ReaderBottomBarAction.ORIENTATION,
                ReaderBottomBarAction.CROP_BORDERS,
                ReaderBottomBarAction.READ_ALOUD,
                ReaderBottomBarAction.SETTINGS,
            ),
            ReaderBottomBarAction.fromStoredIds(
                listOf("OCR", "OCR", "READING_MODE", "ORIENTATION", "CROP_BORDERS", "READ_ALOUD"),
            ),
        )
    }

    @Test
    fun `settings always last even if stored elsewhere`() {
        val order = ReaderBottomBarAction.fromStoredIds(
            listOf("SETTINGS", "OCR", "READING_MODE", "ORIENTATION", "CROP_BORDERS", "READ_ALOUD"),
        )
        assertEquals(ReaderBottomBarAction.SETTINGS, order.last())
        assertEquals(1, order.count { it == ReaderBottomBarAction.SETTINGS })
    }

    @Test
    fun `reset restores default order`() {
        val stored = listOf("READ_ALOUD", "OCR", "READING_MODE", "ORIENTATION", "CROP_BORDERS")
        val reset = ReaderBottomBarAction.fromStoredIds(
            ReaderBottomBarAction.serialize(default).split(','),
        )
        assertEquals(default, reset)
        assertNotDefault(stored)
    }

    private fun assertNotDefault(stored: List<String>) {
        val order = ReaderBottomBarAction.fromStoredIds(stored)
        assertEquals(ReaderBottomBarAction.READ_ALOUD, order.first())
    }

    @Test
    fun `visibility and ordering compose`() {
        val stored = listOf("READ_ALOUD", "OCR", "READING_MODE", "ORIENTATION", "CROP_BORDERS")
        val ocrVisible = false
        val readAloudVisible = false
        val displayed = ReaderBottomBarAction.fromStoredIds(stored)
            .filterNot { it == ReaderBottomBarAction.SETTINGS }
            .filter {
                when (it) {
                    ReaderBottomBarAction.OCR -> ocrVisible
                    ReaderBottomBarAction.READ_ALOUD -> readAloudVisible
                    else -> true
                }
            }
        assertEquals(
            listOf(
                ReaderBottomBarAction.READING_MODE,
                ReaderBottomBarAction.ORIENTATION,
                ReaderBottomBarAction.CROP_BORDERS,
            ),
            displayed,
        )
    }

    @Test
    fun `never produces empty toolbar`() {
        // Both optional actions hidden still leaves the four fixed actions.
        val displayed = default.filterNot { it == ReaderBottomBarAction.SETTINGS }
        assertEquals(5, displayed.size)
    }
}
