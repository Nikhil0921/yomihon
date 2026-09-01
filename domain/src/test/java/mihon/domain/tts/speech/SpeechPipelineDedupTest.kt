package mihon.domain.tts.speech

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.domain.ocr.model.OcrBoundingBox
import mihon.domain.ocr.model.OcrRegion
import mihon.domain.ocr.model.OcrTextOrientation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class SpeechPipelineDedupTest {

    private val classificationConfig = SpeechClassificationConfig()
    private val filterConfig = SpeechRegionFilterConfig()
    private val cleanup = SpeechCleanupOptions()

    private fun region(
        order: Int,
        text: String,
        left: Float = 0.1f,
        top: Float = 0.1f,
        width: Float = 0.2f,
        height: Float = 0.1f,
    ) = OcrRegion(
        order = order,
        text = text,
        boundingBox = OcrBoundingBox(left, top, left + width, top + height),
        textOrientation = OcrTextOrientation.Horizontal,
    )

    @Test
    fun `duplicate text overlapping box drops later region`() {
        val kept = SpeechPipeline.dedupeOverlappingDuplicates(
            listOf(
                region(0, "Hello there."),
                region(1, "Hello there.", left = 0.15f),
            ),
        )
        kept.map { it.order } shouldContainExactly listOf(0)
    }

    @Test
    fun `duplicate text disjoint boxes both kept`() {
        val kept = SpeechPipeline.dedupeOverlappingDuplicates(
            listOf(
                region(0, "Hello there."),
                region(1, "Hello there.", left = 0.5f),
            ),
        )
        kept.map { it.order } shouldContainExactly listOf(0, 1)
    }

    @Test
    fun `different text overlapping boxes both kept`() {
        val kept = SpeechPipeline.dedupeOverlappingDuplicates(
            listOf(
                region(0, "Hello there."),
                region(1, "Goodbye now.", left = 0.15f),
            ),
        )
        kept.map { it.order } shouldContainExactly listOf(0, 1)
    }

    @Test
    fun `case-insensitive whitespace-normalized duplicate dropped`() {
        val kept = SpeechPipeline.dedupeOverlappingDuplicates(
            listOf(
                region(0, "Hello  there."),
                region(1, "hello there.", left = 0.15f),
            ),
        )
        kept.map { it.order } shouldContainExactly listOf(0)
    }

    @Test
    fun `triple duplicate keeps only first`() {
        val kept = SpeechPipeline.dedupeOverlappingDuplicates(
            listOf(
                region(0, "Hello there."),
                region(1, "Hello there.", left = 0.15f),
                region(2, "hello   there.", left = 0.12f),
            ),
        )
        kept.map { it.order } shouldContainExactly listOf(0)
    }

    @Test
    fun `empty region list unchanged`() {
        SpeechPipeline.dedupeOverlappingDuplicates(emptyList()) shouldContainExactly emptyList()
    }

    @Test
    fun `pipeline speaks duplicated regions as one sentence`() {
        val sentences = SpeechPipeline.toSpeakableSentences(
            listOf(
                region(0, "Hello there."),
                region(1, "Hello there.", left = 0.15f),
            ),
            classificationConfig,
            filterConfig,
            cleanup,
        )
        sentences.map { it.text } shouldContainExactly listOf("Hello there.")
    }

    @Test
    fun `boundingBoxIoU identical boxes is one`() {
        val box = OcrBoundingBox(0.1f, 0.1f, 0.3f, 0.2f)
        boundingBoxIoU(box, box) shouldBe 1f
    }

    @Test
    fun `boundingBoxIoU disjoint boxes is zero`() {
        val a = OcrBoundingBox(0.0f, 0.0f, 0.2f, 0.2f)
        val b = OcrBoundingBox(0.5f, 0.5f, 0.7f, 0.7f)
        boundingBoxIoU(a, b) shouldBe 0f
    }
}
