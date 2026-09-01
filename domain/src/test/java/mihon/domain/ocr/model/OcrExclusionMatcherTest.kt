package mihon.domain.ocr.model

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class OcrExclusionMatcherTest {

    private fun region(order: Int, left: Float, top: Float, right: Float, bottom: Float) = OcrRegion(
        order = order,
        text = "text $order",
        boundingBox = OcrBoundingBox(left, top, right, bottom),
        textOrientation = OcrTextOrientation.Horizontal,
    )

    private fun zone(
        scope: OcrExclusionScope,
        left: Float = 0f,
        top: Float = 0f,
        right: Float = 1f,
        bottom: Float = 1f,
        pageIndex: Int? = null,
    ) = OcrExclusionZone(
        id = 1,
        mangaId = 1,
        sourceId = 1,
        chapterId = 1,
        pageIndex = pageIndex,
        scope = scope,
        boundingBox = OcrBoundingBox(left, top, right, bottom),
        enabled = true,
        createdAt = 0,
    )

    @Test
    fun `overlapping region is excluded`() {
        val regions = listOf(region(0, 0f, 0f, 0.2f, 0.2f), region(1, 0.5f, 0.5f, 0.9f, 0.9f))
        val zones = listOf(zone(OcrExclusionScope.CHAPTER, top = 0f, bottom = 0.3f))
        regions.applyExclusions(zones, pageIndex = 0).map { it.order } shouldContainExactly listOf(1)
    }

    @Test
    fun `non-overlapping regions survive`() {
        val regions = listOf(region(0, 0f, 0f, 0.2f, 0.2f))
        val zones = listOf(zone(OcrExclusionScope.CHAPTER, left = 0.5f, top = 0.5f, right = 0.9f, bottom = 0.9f))
        regions.applyExclusions(zones, pageIndex = 0).size shouldBe 1
    }

    @Test
    fun `page scope matches only that page`() {
        val regions = listOf(region(0, 0f, 0f, 0.2f, 0.2f))
        val zones = listOf(zone(OcrExclusionScope.PAGE, pageIndex = 3))
        regions.applyExclusions(zones, pageIndex = 3).size shouldBe 0
        regions.applyExclusions(zones, pageIndex = 4).size shouldBe 1
    }

    @Test
    fun `chapter manga source scopes match any page`() {
        val regions = listOf(region(0, 0f, 0f, 0.2f, 0.2f))
        OcrExclusionScope.entries
            .filterNot { it == OcrExclusionScope.PAGE }
            .forEach { scope ->
                regions.applyExclusions(listOf(zone(scope)), pageIndex = 7).size shouldBe 0
            }
    }

    @Test
    fun `normalized coords work across resolutions`() {
        // Same normalized rect excludes the same relative area regardless of pixel size.
        val regions = listOf(region(0, 0.01f, 0.01f, 0.19f, 0.19f))
        val zones = listOf(zone(OcrExclusionScope.SOURCE, left = 0f, top = 0f, right = 0.2f, bottom = 0.2f))
        regions.applyExclusions(zones, pageIndex = 0).size shouldBe 0
    }
}
