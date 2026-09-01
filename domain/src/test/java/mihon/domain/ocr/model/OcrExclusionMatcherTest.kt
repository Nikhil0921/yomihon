package mihon.domain.ocr.model

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class OcrExclusionMatcherTest {

    private val context = ExclusionMatchContext(mangaId = 1, sourceId = 1, chapterId = 1, pageIndex = 0)

    private fun context(pageIndex: Int) = context.copy(pageIndex = pageIndex)

    private fun region(
        order: Int,
        left: Float = 0f,
        top: Float = 0f,
        right: Float = 0.2f,
        bottom: Float = 0.2f,
        text: String = "text $order",
    ) = OcrRegion(
        order = order,
        text = text,
        boundingBox = OcrBoundingBox(left, top, right, bottom),
        textOrientation = OcrTextOrientation.Horizontal,
    )

    private fun zone(
        scope: OcrExclusionScope = OcrExclusionScope.PAGE,
        left: Float = 0f,
        top: Float = 0f,
        right: Float = 1f,
        bottom: Float = 1f,
        pageIndex: Int? = null,
        chapterId: Long? = 1,
        mangaId: Long = 1,
        sourceId: Long = 1,
        enabled: Boolean = true,
        matchType: OcrExclusionMatchType = OcrExclusionMatchType.ZONE,
        matchText: String? = null,
    ) = OcrExclusionZone(
        id = 1,
        mangaId = mangaId,
        sourceId = sourceId,
        chapterId = chapterId,
        pageIndex = pageIndex,
        scope = scope,
        boundingBox = OcrBoundingBox(left, top, right, bottom),
        enabled = enabled,
        createdAt = 0,
        matchType = matchType,
        matchText = matchText,
    )

    @Test
    fun `page zone excludes overlapping region on matching page only`() {
        val regions = listOf(region(0), region(1, 0.5f, 0.5f, 0.9f, 0.9f))
        val zones = listOf(zone(pageIndex = 0, top = 0f, bottom = 0.3f))
        regions.applyExclusions(zones, context(0)).map { it.order } shouldContainExactly listOf(1)
        regions.applyExclusions(zones, context(4)).map { it.order } shouldContainExactly listOf(0, 1)
    }

    @Test
    fun `page zone requires chapter match too`() {
        val regions = listOf(region(0))
        val zones = listOf(zone(pageIndex = 0))
        regions.applyExclusions(zones, context.copy(chapterId = 99)).size shouldBe 1
    }

    @Test
    fun `non-overlapping regions survive`() {
        val regions = listOf(region(0))
        val zones = listOf(zone(left = 0.5f, top = 0.5f, right = 0.9f, bottom = 0.9f))
        regions.applyExclusions(zones, context).size shouldBe 1
    }

    @Test
    fun `word rule excludes only standalone token`() {
        val regions = listOf(region(0, text = "combination"), region(1, text = "an ion appears"))
        val zones = listOf(zone(matchType = OcrExclusionMatchType.WORD, matchText = "ion"))
        regions.applyExclusions(zones, context).map { it.order } shouldContainExactly listOf(0)
    }

    @Test
    fun `word rule is case-insensitive`() {
        val regions = listOf(region(0, text = "Join DISCORD"))
        val zones = listOf(zone(matchType = OcrExclusionMatchType.WORD, matchText = "Discord"))
        regions.applyExclusions(zones, context).size shouldBe 0
    }

    @Test
    fun `word rule matches unicode letter tokens`() {
        val regions = listOf(region(0, text = "こんにちは world"))
        val zones = listOf(zone(matchType = OcrExclusionMatchType.WORD, matchText = "こんにちは"))
        regions.applyExclusions(zones, context).size shouldBe 0
    }

    @Test
    fun `phrase rule matches case and whitespace tolerant`() {
        val regions = listOf(
            region(0, text = "Join  our   Discord!"),
            region(1, text = "Join our Patreon"),
        )
        val zones = listOf(zone(matchType = OcrExclusionMatchType.PHRASE, matchText = "join our discord"))
        regions.applyExclusions(zones, context).map { it.order } shouldContainExactly listOf(1)
    }

    @Test
    fun `phrase rule matches substring of region text`() {
        val regions = listOf(
            region(0, text = "Support us on Patreon — every bit helps"),
            region(1, text = "Support Patreon"),
        )
        val zones = listOf(zone(matchType = OcrExclusionMatchType.PHRASE, matchText = "Support us on Patreon"))
        regions.applyExclusions(zones, context).map { it.order } shouldContainExactly listOf(1)
    }

    @Test
    fun `combined chapter rule needs rect and text on any page of chapter`() {
        val zones = listOf(
            zone(
                scope = OcrExclusionScope.CHAPTER,
                top = 0f,
                bottom = 0.3f,
                matchType = OcrExclusionMatchType.COMBINED,
                matchText = "discord",
            ),
        )
        val matching = listOf(region(0, text = "Join our Discord"))
        matching.applyExclusions(zones, context(0)).size shouldBe 0
        matching.applyExclusions(zones, context(9)).size shouldBe 0
        val noRect = listOf(region(0, text = "Join our Discord", left = 0.5f, top = 0.5f, right = 0.9f, bottom = 0.9f))
        noRect.applyExclusions(zones, context(0)).size shouldBe 1
        val noText = listOf(region(0, text = "Hello there"))
        noText.applyExclusions(zones, context(0)).size shouldBe 1
        val otherChapter = listOf(region(0, text = "Join our Discord"))
        otherChapter.applyExclusions(zones, context.copy(chapterId = 99)).size shouldBe 1
    }

    @Test
    fun `combined manga and source rules match their scope`() {
        val regions = listOf(region(0, text = "Join our Discord"))
        val mangaZone = listOf(
            zone(
                scope = OcrExclusionScope.MANGA,
                chapterId = null,
                mangaId = 1,
                matchType = OcrExclusionMatchType.COMBINED,
                matchText = "discord",
            ),
        )
        regions.applyExclusions(mangaZone, context).size shouldBe 0
        regions.applyExclusions(mangaZone, context.copy(mangaId = 42)).size shouldBe 1

        val sourceZone = listOf(
            zone(
                scope = OcrExclusionScope.SOURCE,
                chapterId = null,
                sourceId = 1,
                matchType = OcrExclusionMatchType.COMBINED,
                matchText = "discord",
            ),
        )
        regions.applyExclusions(sourceZone, context).size shouldBe 0
        regions.applyExclusions(sourceZone, context.copy(sourceId = 42)).size shouldBe 1
    }

    @Test
    fun `legacy zone rules wider than page are dormant`() {
        val regions = listOf(region(0))
        OcrExclusionScope.entries
            .filterNot { it == OcrExclusionScope.PAGE }
            .forEach { scope ->
                val zones = listOf(zone(scope = scope, chapterId = if (scope == OcrExclusionScope.CHAPTER) 1 else null))
                regions.applyExclusions(zones, context(7)).size shouldBe 1
            }
    }

    @Test
    fun `disabled zone never excludes`() {
        val regions = listOf(region(0))
        val zones = listOf(zone(pageIndex = 0, enabled = false))
        regions.applyExclusions(zones, context).size shouldBe 1
    }

    @Test
    fun `multiple rules any-match excludes`() {
        val regions = listOf(region(0, text = "Join our Discord"), region(1, text = "Hello"))
        val zones = listOf(
            zone(pageIndex = 0, left = 0.5f, top = 0.5f, right = 0.9f, bottom = 0.9f),
            zone(matchType = OcrExclusionMatchType.PHRASE, matchText = "join our discord"),
        )
        regions.applyExclusions(zones, context).map { it.order } shouldContainExactly listOf(1)
    }

    @Test
    fun `normalized coords work across resolutions`() {
        val regions = listOf(region(0, left = 0.01f, top = 0.01f, right = 0.19f, bottom = 0.19f))
        val zones = listOf(zone(pageIndex = 0, left = 0f, top = 0f, right = 0.2f, bottom = 0.2f))
        regions.applyExclusions(zones, context).size shouldBe 0
    }
}
