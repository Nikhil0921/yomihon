package mihon.domain.tts.speech

import io.kotest.matchers.shouldBe
import mihon.domain.ocr.model.OcrBoundingBox
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class BoxMostlyInsideTest {

    private fun box(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) = OcrBoundingBox(left, top, right, bottom)

    @Test
    fun `fully contained region is included`() {
        val region = box(0.2f, 0.2f, 0.4f, 0.4f)
        val selection = box(0.1f, 0.1f, 0.9f, 0.9f)
        boxMostlyInside(region, selection) shouldBe true
    }

    @Test
    fun `edge-grazing neighbor is excluded`() {
        // Region overlaps the selection by a sliver (1px in practice) — its full
        // text must NOT leak into the detected text.
        val region = box(0.0f, 0.0f, 0.11f, 0.2f)
        val selection = box(0.1f, 0.1f, 0.9f, 0.9f)
        boxMostlyInside(region, selection) shouldBe false
    }

    @Test
    fun `disjoint region is excluded`() {
        val region = box(0.5f, 0.5f, 0.8f, 0.8f)
        val selection = box(0.1f, 0.1f, 0.4f, 0.4f)
        boxMostlyInside(region, selection) shouldBe false
    }

    @Test
    fun `region 40 percent inside is excluded at default threshold`() {
        // Region [0,0.2]x[0,0.1] area 0.02; selection starts x=0.08:
        // intersection 0.12*0.1=0.012 → 60%... use 0.05: intersection 0.15*0.1
        // = 0.015 → 75% included. To get clearly-below-threshold: selection
        // x=0.13 → intersection 0.07*0.1 = 0.007 → 35% excluded.
        val region = box(0.0f, 0.0f, 0.2f, 0.1f)
        val selection = box(0.13f, 0.0f, 0.9f, 0.9f)
        boxMostlyInside(region, selection) shouldBe false
    }

    @Test
    fun `region mostly inside is included`() {
        // Region [0,0.08]x[0,0.1] inside selection starting at x=0.02:
        // intersection 0.06x0.1=0.006; region area 0.08x0.1=0.008 → 75%.
        val region = box(0.0f, 0.0f, 0.08f, 0.1f)
        val selection = box(0.02f, 0.0f, 0.9f, 0.9f)
        boxMostlyInside(region, selection) shouldBe true
    }

    @Test
    fun `selection fully inside a huge region is excluded`() {
        // A merged bubble box far wider than the user's tight selection covers
        // mostly OUT-of-selection text; majority semantics keep it out of the
        // detected text (its intersection is only a small fraction of it).
        val region = box(0.0f, 0.0f, 1.0f, 1.0f)
        val selection = box(0.4f, 0.4f, 0.6f, 0.6f)
        boxMostlyInside(region, selection) shouldBe false
    }

    @Test
    fun `degenerate zero-area region is excluded`() {
        val region = box(0.2f, 0.2f, 0.2f, 0.2f)
        val selection = box(0.1f, 0.1f, 0.9f, 0.9f)
        boxMostlyInside(region, selection) shouldBe false
    }
}
