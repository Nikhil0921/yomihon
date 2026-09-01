package mihon.domain.ocr.repository

import kotlinx.coroutines.flow.Flow
import mihon.domain.ocr.model.OcrExclusionScope
import mihon.domain.ocr.model.OcrExclusionZone

interface OcrExclusionZoneRepository {

    suspend fun getAll(): List<OcrExclusionZone>

    fun subscribeZonesForManga(mangaId: Long): Flow<List<OcrExclusionZone>>

    fun subscribeZonesForSource(sourceId: Long): Flow<List<OcrExclusionZone>>

    suspend fun getZonesForChapter(chapterId: Long): List<OcrExclusionZone>

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
    ): Long

    suspend fun delete(id: Long)

    suspend fun setEnabled(id: Long, enabled: Boolean)
}
