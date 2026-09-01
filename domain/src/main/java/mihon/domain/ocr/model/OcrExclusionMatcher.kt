package mihon.domain.ocr.model

/** Chapter/page identity the matcher needs to decide which zones apply where. */
data class ExclusionMatchContext(
    val mangaId: Long,
    val sourceId: Long,
    val chapterId: Long,
    val pageIndex: Int,
)

/**
 * Pure exclusion-rule matching for OCR regions.
 *
 * - ZONE: rectangle on a specific page (scope PAGE). Legacy rows with wider
 *   scopes are dormant: their rectangle was drawn on one page only, so applying
 *   it to every page in scope would suppress unrelated text.
 * - WORD: region excluded when any standalone word token equals the rule text.
 * - PHRASE: region excluded when the rule text appears as a case-insensitive,
 *   whitespace-tolerant substring of the region text.
 * - COMBINED: rectangle + phrase must both match within the rule's scope.
 */
fun List<OcrRegion>.applyExclusions(
    zones: List<OcrExclusionZone>,
    context: ExclusionMatchContext,
): List<OcrRegion> {
    if (zones.isEmpty()) return this
    val active = zones.filter { it.enabled && it.matchesRegionScope(context) }
    if (active.isEmpty()) return this
    return filter { region ->
        active.none { zone -> matchesRegion(zone, region, context) }
    }
}

private fun OcrExclusionZone.matchesRegionScope(context: ExclusionMatchContext): Boolean = when (matchType) {
    OcrExclusionMatchType.ZONE -> scope == OcrExclusionScope.PAGE && chapterId == context.chapterId
    OcrExclusionMatchType.WORD, OcrExclusionMatchType.PHRASE -> true
    OcrExclusionMatchType.COMBINED -> when (scope) {
        OcrExclusionScope.PAGE ->
            chapterId == context.chapterId &&
                pageIndex == context.pageIndex
        OcrExclusionScope.CHAPTER -> chapterId == context.chapterId
        OcrExclusionScope.MANGA -> mangaId == context.mangaId
        OcrExclusionScope.SOURCE -> sourceId == context.sourceId
    }
}

private fun matchesRegion(
    zone: OcrExclusionZone,
    region: OcrRegion,
    context: ExclusionMatchContext,
): Boolean = when (zone.matchType) {
    OcrExclusionMatchType.ZONE ->
        zone.pageIndex == context.pageIndex &&
            overlaps(region.boundingBox, zone.boundingBox)
    OcrExclusionMatchType.WORD -> wordMatches(zone.matchText, region.text)
    OcrExclusionMatchType.PHRASE -> phraseMatches(zone.matchText, region.text)
    OcrExclusionMatchType.COMBINED ->
        overlaps(region.boundingBox, zone.boundingBox) &&
            phraseMatches(zone.matchText, region.text)
}

private fun wordMatches(ruleText: String?, regionText: String): Boolean {
    val needle = ruleText?.trim()?.lowercase() ?: return false
    if (needle.isEmpty()) return false
    return regionText.tokens().any { it == needle }
}

private fun CharSequence.tokens(): List<String> {
    val tokens = ArrayList<String>()
    val current = StringBuilder()
    forEach { ch ->
        if (ch.isLetterOrDigit()) {
            current.append(ch)
        } else if (current.isNotEmpty()) {
            tokens.add(current.toString().lowercase())
            current.clear()
        }
    }
    if (current.isNotEmpty()) tokens.add(current.toString().lowercase())
    return tokens
}

private fun phraseMatches(ruleText: String?, regionText: String): Boolean {
    val needle = normalizeForPhrase(ruleText) ?: return false
    if (needle.isEmpty()) return false
    val haystack = normalizeForPhrase(regionText) ?: return false
    return haystack.contains(needle)
}

private fun normalizeForPhrase(text: String?): String? {
    if (text == null) return null
    return text.replace(Regex("\\s+"), " ").trim().lowercase()
}

private fun overlaps(a: OcrBoundingBox, b: OcrBoundingBox): Boolean =
    a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom
