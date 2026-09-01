package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import mihon.domain.ocr.model.OcrExclusionMatchType
import mihon.domain.ocr.model.OcrExclusionScope
import mihon.domain.ocr.model.OcrExclusionZone

@Serializable
class BackupOcrExclusionZone(
    @ProtoNumber(1) var mangaId: Long,
    @ProtoNumber(2) var sourceId: Long,
    @ProtoNumber(3) var chapterId: Long?,
    @ProtoNumber(4) var pageIndex: Int?,
    @ProtoNumber(5) var scope: String,
    @ProtoNumber(6) var leftNorm: Float,
    @ProtoNumber(7) var topNorm: Float,
    @ProtoNumber(8) var rightNorm: Float,
    @ProtoNumber(9) var bottomNorm: Float,
    @ProtoNumber(10) var enabled: Boolean,
    @ProtoNumber(11) var matchType: String = "ZONE",
    @ProtoNumber(12) var matchText: String? = null,
    @ProtoNumber(13) var ruleName: String? = null,
)

val backupOcrExclusionZoneMapper = { zone: OcrExclusionZone ->
    BackupOcrExclusionZone(
        mangaId = zone.mangaId,
        sourceId = zone.sourceId,
        chapterId = zone.chapterId,
        pageIndex = zone.pageIndex,
        scope = zone.scope.name,
        leftNorm = zone.boundingBox.left,
        topNorm = zone.boundingBox.top,
        rightNorm = zone.boundingBox.right,
        bottomNorm = zone.boundingBox.bottom,
        enabled = zone.enabled,
        matchType = zone.matchType.name,
        matchText = zone.matchText,
        ruleName = zone.ruleName,
    )
}

val BackupOcrExclusionZone.exclusionScope: OcrExclusionScope?
    get() = runCatching { OcrExclusionScope.valueOf(scope) }.getOrNull()

val BackupOcrExclusionZone.exclusionMatchType: OcrExclusionMatchType?
    get() = runCatching { OcrExclusionMatchType.valueOf(matchType) }.getOrNull()
