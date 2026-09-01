package mihon.data.ocr

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mihon.domain.ocr.model.OcrBoundingBox
import mihon.domain.ocr.model.OcrExclusionMatchType
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

    override fun subscribeAll(): Flow<List<OcrExclusionZone>> =
        database.ocr_exclusion_zonesQueries
            .getAllZones(::zoneMapper)
            .asFlow()
            .mapToList(Dispatchers.IO)

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

    override suspend fun getZonesForSpeech(
        mangaId: Long,
        sourceId: Long,
        chapterId: Long,
    ): List<OcrExclusionZone> = withContext(Dispatchers.IO) {
        database.ocr_exclusion_zonesQueries
            .zonesForSpeech(mangaId, sourceId, chapterId, ::zoneMapper)
            .awaitAsList()
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
        matchType: OcrExclusionMatchType,
        matchText: String?,
        ruleName: String?,
        enabled: Boolean,
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
                enabled = if (enabled) 1L else 0L,
                createdAt = System.currentTimeMillis(),
                matchText = matchText,
                matchType = matchType.name,
                ruleName = ruleName,
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
        matchText: String?,
        matchType: String,
        ruleName: String?,
    ): OcrExclusionZone = OcrExclusionZone(
        id = _id,
        mangaId = mangaId,
        sourceId = sourceId,
        chapterId = chapterId,
        pageIndex = pageIndex?.toInt(),
        scope = runCatching { OcrExclusionScope.valueOf(scope) }.getOrDefault(OcrExclusionScope.PAGE),
        boundingBox = OcrBoundingBox(
            left = leftNorm.toFloat(),
            top = topNorm.toFloat(),
            right = rightNorm.toFloat(),
            bottom = bottomNorm.toFloat(),
        ),
        enabled = enabled != 0L,
        createdAt = createdAt,
        matchType = runCatching { OcrExclusionMatchType.valueOf(matchType) }.getOrDefault(OcrExclusionMatchType.ZONE),
        matchText = matchText,
        ruleName = ruleName,
    )
}
