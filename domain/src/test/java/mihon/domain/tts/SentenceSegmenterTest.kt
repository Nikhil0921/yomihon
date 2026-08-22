package mihon.domain.tts

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.domain.ocr.model.OcrBoundingBox
import mihon.domain.ocr.model.OcrRegion
import mihon.domain.ocr.model.OcrTextOrientation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class SentenceSegmenterTest {

    private fun region(
        order: Int,
        text: String,
        left: Float = 0f,
        top: Float = 0f,
    ) = OcrRegion(
        order = order,
        text = text,
        boundingBox = OcrBoundingBox(left, top, left + 10f, top + 10f),
        textOrientation = OcrTextOrientation.Vertical,
    )

    private fun segment(vararg regions: OcrRegion) = regions.toList().toTtsSentences()

    @Test
    fun `splits multi-sentence region keeping punctuation attached`() {
        segment(region(0, "おはよう。今日も暑いね。")) shouldContainExactly listOf(
            TtsSentence("おはよう。", 0, region(0, "").boundingBox, OcrTextOrientation.Vertical),
            TtsSentence("今日も暑いね。", 0, region(0, "").boundingBox, OcrTextOrientation.Vertical),
        )
    }

    @Test
    fun `trailing text without terminal punctuation becomes final fragment`() {
        segment(region(0, "行くぞ！待って")) shouldContainExactly listOf(
            TtsSentence("行くぞ！", 0, region(0, "").boundingBox, OcrTextOrientation.Vertical),
            TtsSentence("待って", 0, region(0, "").boundingBox, OcrTextOrientation.Vertical),
        )
    }

    @Test
    fun `region without any terminal punctuation stays whole`() {
        segment(region(0, "まだだ")) shouldContainExactly listOf(
            TtsSentence("まだだ", 0, region(0, "").boundingBox, OcrTextOrientation.Vertical),
        )
    }

    @Test
    fun `ascii dots are not terminal`() {
        segment(region(0, "えっと...そうだね。")) shouldContainExactly listOf(
            TtsSentence("えっと...そうだね。", 0, region(0, "").boundingBox, OcrTextOrientation.Vertical),
        )
    }

    @Test
    fun `half-width exclamation and question marks are terminal`() {
        segment(region(0, "やめろ!え?")) shouldContainExactly listOf(
            TtsSentence("やめろ!", 0, region(0, "").boundingBox, OcrTextOrientation.Vertical),
            TtsSentence("え?", 0, region(0, "").boundingBox, OcrTextOrientation.Vertical),
        )
    }

    @Test
    fun `combined punctuation glyphs are terminal`() {
        segment(region(0, "なんだ‼ほんとか⁇まさか⁉嘘だろ⁈")) shouldContainExactly listOf(
            TtsSentence("なんだ‼", 0, region(0, "").boundingBox, OcrTextOrientation.Vertical),
            TtsSentence("ほんとか⁇", 0, region(0, "").boundingBox, OcrTextOrientation.Vertical),
            TtsSentence("まさか⁉", 0, region(0, "").boundingBox, OcrTextOrientation.Vertical),
            TtsSentence("嘘だろ⁈", 0, region(0, "").boundingBox, OcrTextOrientation.Vertical),
        )
    }

    @Test
    fun `blank regions are skipped`() {
        segment(region(0, "  "), region(1, "あ。")) shouldContainExactly listOf(
            TtsSentence("あ。", 1, region(1, "").boundingBox, OcrTextOrientation.Vertical),
        )
    }

    @Test
    fun `empty input yields no sentences`() {
        segment() shouldContainExactly emptyList()
    }

    @Test
    fun `regions never merge across boundaries`() {
        val sentences = segment(region(3, "まだ"), region(7, "終わり。"))
        sentences.map { it.text } shouldContainExactly listOf("まだ", "終わり。")
        sentences.map { it.regionOrder } shouldContainExactly listOf(3, 7)
    }

    @Test
    fun `stored region list order is consumed as-is without re-sorting`() {
        val sentences = segment(region(5, "い。"), region(2, "ろ。"))
        sentences.map { it.regionOrder } shouldContainExactly listOf(5, 2)
    }

    @Test
    fun `sentence carries its region bbox and orientation`() {
        val r = OcrRegion(
            order = 4,
            text = "位置。",
            boundingBox = OcrBoundingBox(0.2f, 0.3f, 0.5f, 0.9f),
            textOrientation = OcrTextOrientation.Horizontal,
        )
        val sentence = listOf(r).toTtsSentences().single()
        sentence.boundingBox shouldBe r.boundingBox
        sentence.textOrientation shouldBe OcrTextOrientation.Horizontal
        sentence.regionOrder shouldBe 4
    }

    @Test
    fun `consecutive terminals split into separate sentences`() {
        segment(region(0, "そうか。!")) shouldContainExactly listOf(
            TtsSentence("そうか。", 0, region(0, "").boundingBox, OcrTextOrientation.Vertical),
            TtsSentence("!", 0, region(0, "").boundingBox, OcrTextOrientation.Vertical),
        )
    }
}
