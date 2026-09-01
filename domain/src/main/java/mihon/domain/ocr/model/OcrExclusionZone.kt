package mihon.domain.ocr.model

/**
 * A user-created rule that keeps OCR regions out of Read-Aloud. Geometric rules
 * store a rectangle in normalized page coordinates (0..1) so the same rule works
 * across image resolutions, screen sizes and long-strip pages; text rules store
 * the word or phrase to match against region text.
 */
data class OcrExclusionZone(
    val id: Long,
    val mangaId: Long,
    val sourceId: Long,
    val chapterId: Long?,
    val pageIndex: Int?,
    val scope: OcrExclusionScope,
    val boundingBox: OcrBoundingBox,
    val enabled: Boolean,
    val createdAt: Long,
    val matchType: OcrExclusionMatchType = OcrExclusionMatchType.ZONE,
    val matchText: String? = null,
    val ruleName: String? = null,
)

enum class OcrExclusionScope {
    PAGE,
    CHAPTER,
    MANGA,
    SOURCE,
}

enum class OcrExclusionMatchType {
    ZONE,
    WORD,
    PHRASE,
    COMBINED,
}
