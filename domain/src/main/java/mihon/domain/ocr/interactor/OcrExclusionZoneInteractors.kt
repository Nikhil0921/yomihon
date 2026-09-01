package mihon.domain.ocr.interactor

import kotlinx.coroutines.flow.Flow
import mihon.domain.ocr.model.OcrExclusionScope
import mihon.domain.ocr.model.OcrExclusionZone
import mihon.domain.ocr.repository.OcrExclusionZoneRepository

class GetOcrExclusionZones(
    private val repository: OcrExclusionZoneRepository,
) {
    fun subscribeForManga(mangaId: Long): Flow<List<OcrExclusionZone>> =
        repository.subscribeZonesForManga(mangaId)

    fun subscribeForSource(sourceId: Long): Flow<List<OcrExclusionZone>> =
        repository.subscribeZonesForSource(sourceId)

    suspend fun awaitForChapter(chapterId: Long): List<OcrExclusionZone> =
        repository.getZonesForChapter(chapterId)

    suspend fun awaitAll(): List<OcrExclusionZone> = repository.getAll()
}

class AddOcrExclusionZone(
    private val repository: OcrExclusionZoneRepository,
) {
    suspend fun await(
        mangaId: Long,
        sourceId: Long,
        chapterId: Long?,
        pageIndex: Int?,
        scope: OcrExclusionScope,
        leftNorm: Float,
        topNorm: Float,
        rightNorm: Float,
        bottomNorm: Float,
    ): Long = repository.insert(
        mangaId, sourceId, chapterId, pageIndex, scope,
        leftNorm, topNorm, rightNorm, bottomNorm,
    )
}

class DeleteOcrExclusionZone(
    private val repository: OcrExclusionZoneRepository,
) {
    suspend fun await(id: Long) = repository.delete(id)
}

class SetOcrExclusionZoneEnabled(
    private val repository: OcrExclusionZoneRepository,
) {
    suspend fun await(id: Long, enabled: Boolean) = repository.setEnabled(id, enabled)
}
