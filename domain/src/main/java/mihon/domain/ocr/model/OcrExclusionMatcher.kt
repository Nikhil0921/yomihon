package mihon.domain.ocr.model

import java.text.Normalizer

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
 * - WORD: region excluded when the rule's token concatenation equals the
 *   concatenation of a consecutive run of region word tokens. Tokenization is
 *   identical on both sides, so "K-manga.com" matches tokens k, manga, com and
 *   camelCase/OCR separator variants ("KeyManga" ≡ "Key Manga") match too,
 *   while "ion" never matches "combination" (token boundaries enforced).
 * - PHRASE: region excluded when the rule text appears as a substring after
 *   Unicode-folded, case-insensitive, whitespace-stripped comparison —
 *   tolerant of OCR spacing noise around punctuation ("Discord. gg / x").
 * - COMBINED: rectangle + phrase must both match within the rule's scope.
 *
 * All text comparisons NFKC-normalize (folds full-width ｋｅｙ → key) so
 * JP-mixed OCR lines converted to full-width still match half-width rules.
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

/**
 * WORD: the rule's token concatenation must equal the concatenation of some
 * consecutive run of region tokens. Single-token rules therefore behave as
 * standalone-word match ("ion" never matches "combination"), while separator
 * variants match across rule/region ("KeyManga" ≡ "Key Manga" ≡ [k, manga, com]
 * runs of "K-manga.com").
 */
private fun wordMatches(ruleText: String?, regionText: String): Boolean {
    val needleTokens = ruleText.normalizedTokens() ?: return false
    if (needleTokens.isEmpty()) return false
    val regionTokens = regionText.normalizedTokens() ?: return false
    if (regionTokens.isEmpty()) return false
    val needleConcat = needleTokens.joinToString("")
    return (1..regionTokens.size).any { size ->
        regionTokens.windowed(size).any { run -> run.joinToString("") == needleConcat }
    }
}

/** NFKC-fold, then split on non-letter/digit runs; tokens keep their case-fold. */
private fun String?.normalizedTokens(): List<String>? {
    if (this == null) return null
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFKC).lowercase()
    val tokens = ArrayList<String>()
    val current = StringBuilder()
    for (ch in normalized) {
        if (ch.isLetterOrDigit()) {
            current.append(ch)
        } else if (current.isNotEmpty()) {
            tokens.add(current.toString())
            current.clear()
        }
    }
    if (current.isNotEmpty()) tokens.add(current.toString())
    return tokens
}

/** NFKC-fold, lowercase, strip ALL whitespace — OCR spacing noise-proof. */
private fun normalizeForPhrase(text: String?): String? {
    if (text == null) return null
    return Normalizer.normalize(text, Normalizer.Form.NFKC)
        .filterNot { it.isWhitespace() }
        .lowercase()
}

private fun phraseMatches(ruleText: String?, regionText: String): Boolean {
    val needle = normalizeForPhrase(ruleText) ?: return false
    if (needle.isEmpty()) return false
    val haystack = normalizeForPhrase(regionText) ?: return false
    return haystack.contains(needle)
}

private fun overlaps(a: OcrBoundingBox, b: OcrBoundingBox): Boolean =
    a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom
