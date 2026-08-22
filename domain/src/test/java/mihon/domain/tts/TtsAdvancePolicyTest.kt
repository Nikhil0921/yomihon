package mihon.domain.tts

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class TtsAdvancePolicyTest {

    private fun compute(
        pageIndex: Int = 0,
        totalPages: Int = 10,
        pageHasText: Boolean = true,
        autoTurn: Boolean = true,
        autoNextChapter: Boolean = false,
        nextChapterExists: Boolean = false,
    ) = TtsAdvancePolicy.computeAdvance(
        currentPageIndex = pageIndex,
        totalPages = totalPages,
        pageHasText = pageHasText,
        autoTurn = autoTurn,
        autoNextChapter = autoNextChapter,
        nextChapterExists = nextChapterExists,
    )

    @Test
    fun `mid-chapter page advances when auto turn is on`() {
        compute(pageIndex = 3) shouldBe TtsAdvanceAction.NextPage
    }

    @Test
    fun `mid-chapter page pauses at page end when auto turn is off`() {
        compute(pageIndex = 3, autoTurn = false) shouldBe TtsAdvanceAction.PauseAtPageEnd
    }

    @Test
    fun `last page finishes when auto next chapter is off`() {
        compute(pageIndex = 9) shouldBe TtsAdvanceAction.Finish
    }

    @Test
    fun `last page advances to next chapter when enabled and available`() {
        compute(pageIndex = 9, autoNextChapter = true, nextChapterExists = true) shouldBe
            TtsAdvanceAction.NextChapter
    }

    @Test
    fun `next chapter is taken at last page regardless of auto turn`() {
        compute(pageIndex = 9, autoTurn = false, autoNextChapter = true, nextChapterExists = true) shouldBe
            TtsAdvanceAction.NextChapter
    }

    @Test
    fun `last page finishes when no next chapter exists even if auto next chapter is on`() {
        compute(pageIndex = 9, autoNextChapter = true, nextChapterExists = false) shouldBe
            TtsAdvanceAction.Finish
    }

    @Test
    fun `empty page is skipped even when auto turn is off`() {
        compute(pageIndex = 3, pageHasText = false, autoTurn = false) shouldBe TtsAdvanceAction.NextPage
    }

    @Test
    fun `empty last page advances to next chapter when enabled and available`() {
        compute(pageIndex = 9, pageHasText = false, autoNextChapter = true, nextChapterExists = true) shouldBe
            TtsAdvanceAction.NextChapter
    }

    @Test
    fun `empty last page finishes otherwise`() {
        compute(pageIndex = 9, pageHasText = false) shouldBe TtsAdvanceAction.Finish
        compute(pageIndex = 9, pageHasText = false, autoNextChapter = true) shouldBe
            TtsAdvanceAction.Finish
    }

    @Test
    fun `empty chapter finishes immediately`() {
        compute(pageIndex = 0, totalPages = 0, pageHasText = false) shouldBe TtsAdvanceAction.Finish
    }
}
