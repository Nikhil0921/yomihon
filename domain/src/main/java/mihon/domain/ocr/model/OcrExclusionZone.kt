package mihon.domain.ocr.model

/**
 * A user-created rectangular area excluded from OCR processing and Read-Aloud,
 * stored in normalized page coordinates (0..1) so the same rule works across
 * image resolutions, screen sizes and long-strip pages.
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
)

enum class OcrExclusionScope {
    PAGE,
    CHAPTER,
    MANGA,
    SOURCE,
}
