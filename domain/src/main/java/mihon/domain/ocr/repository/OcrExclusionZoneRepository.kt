package mihon.domain.ocr.repository

import kotlinx.coroutines.flow.Flow
import mihon.domain.ocr.model.OcrExclusionMatchType
import mihon.domain.ocr.model.OcrExclusionScope
import mihon.domain.ocr.model.OcrExclusionZone

interface OcrExclusionZoneRepository {

    suspend fun getAll(): List<OcrExclusionZone>

    fun subscribeAll(): Flow<List<OcrExclusionZone>>

    fun subscribeZonesForManga(mangaId: Long, sourceId: Long): Flow<List<OcrExclusionZone>>

    fun subscribeZonesForSource(sourceId: Long): Flow<List<OcrExclusionZone>>

    suspend fun getZonesForChapter(chapterId: Long): List<OcrExclusionZone>

    /** All enabled rules that could affect the given chapter (text rules are global). */
    suspend fun getZonesForSpeech(mangaId: Long, sourceId: Long, chapterId: Long): List<OcrExclusionZone>

    suspend fun insert(
        mangaId: Long,
        sourceId: Long,
        chapterId: Long?,
        pageIndex: Int?,
        scope: OcrExclusionScope,
        leftNorm: Float,
        topNorm: Float,
        rightNorm: Float,
        bottomNorm: Float,
        matchType: OcrExclusionMatchType = OcrExclusionMatchType.ZONE,
        matchText: String? = null,
        ruleName: String? = null,
        enabled: Boolean = true,
    ): Long

    suspend fun delete(id: Long)

    suspend fun setEnabled(id: Long, enabled: Boolean)
}
