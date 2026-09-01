package mihon.data.ocr

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mihon.domain.ocr.model.OcrBoundingBox
import mihon.domain.ocr.model.OcrExclusionScope
import mihon.domain.ocr.model.OcrExclusionZone
import mihon.domain.ocr.repository.OcrExclusionZoneRepository
import tachiyomi.data.Database

class OcrExclusionZoneRepositoryImpl(
    private val database: Database,
) : OcrExclusionZoneRepository {

    override suspend fun getAll(): List<OcrExclusionZone> =
        withContext(Dispatchers.IO) {
            database.ocr_exclusion_zonesQueries.getAllZones(::zoneMapper).awaitAsList()
        }

    override fun subscribeZonesForManga(mangaId: Long): Flow<List<OcrExclusionZone>> =
        database.ocr_exclusion_zonesQueries
            .zonesForManga(mangaId, ::zoneMapper)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override fun subscribeZonesForSource(sourceId: Long): Flow<List<OcrExclusionZone>> =
        database.ocr_exclusion_zonesQueries
            .zonesForSource(sourceId, ::zoneMapper)
            .asFlow()
            .mapToList(Dispatchers.IO)

    override suspend fun getZonesForChapter(chapterId: Long): List<OcrExclusionZone> =
        withContext(Dispatchers.IO) {
            database.ocr_exclusion_zonesQueries.zonesForChapter(chapterId, ::zoneMapper).awaitAsList()
        }

    override suspend fun insert(
        mangaId: Long,
        sourceId: Long,
        chapterId: Long?,
        pageIndex: Int?,
        scope: OcrExclusionScope,
        leftNorm: Float,
        topNorm: Float,
        rightNorm: Float,
        bottomNorm: Float,
    ): Long = withContext(Dispatchers.IO) {
        database.ocr_exclusion_zonesQueries.transaction {
            database.ocr_exclusion_zonesQueries.insertZone(
                mangaId = mangaId,
                sourceId = sourceId,
                chapterId = chapterId,
                pageIndex = pageIndex?.toLong(),
                scope = scope.name,
                leftNorm = leftNorm.toDouble(),
                topNorm = topNorm.toDouble(),
                rightNorm = rightNorm.toDouble(),
                bottomNorm = bottomNorm.toDouble(),
                enabled = 1L,
                createdAt = System.currentTimeMillis(),
            )
        }
        database.ocr_exclusion_zonesQueries.selectLastInsertedRowId().awaitAsOne()
    }

    override suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            database.ocr_exclusion_zonesQueries.deleteZone(id)
        }
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            database.ocr_exclusion_zonesQueries.setEnabled(id, if (enabled) 1L else 0L)
        }
    }

    private fun zoneMapper(
        _id: Long,
        mangaId: Long,
        sourceId: Long,
        chapterId: Long?,
        pageIndex: Long?,
        scope: String,
        leftNorm: Double,
        topNorm: Double,
        rightNorm: Double,
        bottomNorm: Double,
        enabled: Long,
        createdAt: Long,
    ): OcrExclusionZone = OcrExclusionZone(
        id = _id,
        mangaId = mangaId,
        sourceId = sourceId,
        chapterId = chapterId,
        pageIndex = pageIndex?.toInt(),
        scope = OcrExclusionScope.valueOf(scope),
        boundingBox = OcrBoundingBox(
            left = leftNorm.toFloat(),
            top = topNorm.toFloat(),
            right = rightNorm.toFloat(),
            bottom = bottomNorm.toFloat(),
        ),
        enabled = enabled != 0L,
        createdAt = createdAt,
    )
}
