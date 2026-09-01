package mihon.domain.ocr.model

/**
 * Pure exclusion-zone matching for OCR regions. Zones use normalized coordinates
 * so the same rule applies across resolutions and long-strip pages. A region
 * overlapping any enabled zone (matching the page scope) is excluded.
 */
fun List<OcrRegion>.applyExclusions(
    zones: List<OcrExclusionZone>,
    pageIndex: Int,
): List<OcrRegion> {
    if (zones.isEmpty()) return this
    val active = zones.filter { it.enabled && zoneMatchesPage(it, pageIndex) }
    if (active.isEmpty()) return this
    return filter { region ->
        active.none { zone -> overlaps(region.boundingBox, zone.boundingBox) }
    }
}

private fun zoneMatchesPage(zone: OcrExclusionZone, pageIndex: Int): Boolean = when (zone.scope) {
    OcrExclusionScope.PAGE -> zone.pageIndex == pageIndex
    OcrExclusionScope.CHAPTER,
    OcrExclusionScope.MANGA,
    OcrExclusionScope.SOURCE,
    -> true
}

private fun overlaps(a: OcrBoundingBox, b: OcrBoundingBox): Boolean =
    a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom
