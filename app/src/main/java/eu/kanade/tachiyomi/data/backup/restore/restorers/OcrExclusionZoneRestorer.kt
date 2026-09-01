package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupOcrExclusionZone
import eu.kanade.tachiyomi.data.backup.models.exclusionScope
import logcat.LogPriority
import mihon.domain.ocr.interactor.AddOcrExclusionZone
import mihon.domain.ocr.interactor.GetOcrExclusionZones
import mihon.domain.ocr.model.OcrExclusionZone
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Restores OCR exclusion zones. Zones referencing manga missing from the local
 * database are skipped (mangaId FK would fail); duplicates (same manga, scope,
 * chapter, page and rectangle) are not re-inserted.
 */
class OcrExclusionZoneRestorer(
    private val getOcrExclusionZones: GetOcrExclusionZones = Injekt.get(),
    private val addOcrExclusionZone: AddOcrExclusionZone = Injekt.get(),
) {

    suspend fun restoreZones(zones: List<BackupOcrExclusionZone>) {
        val existing = try {
            getOcrExclusionZones.awaitAll()
        } catch (e: Exception) {
            emptyList<OcrExclusionZone>()
        }
        for (zone in zones) {
            val scope = zone.exclusionScope ?: continue
            if (existing.any { it.duplicates(zone) }) continue
            try {
                addOcrExclusionZone.await(
                    mangaId = zone.mangaId,
                    sourceId = zone.sourceId,
                    chapterId = zone.chapterId,
                    pageIndex = zone.pageIndex,
                    scope = scope,
                    leftNorm = zone.leftNorm,
                    topNorm = zone.topNorm,
                    rightNorm = zone.rightNorm,
                    bottomNorm = zone.bottomNorm,
                )
            } catch (e: Exception) {
                // FK violations (restored manga/chapter missing) skip that zone.
                logcat(LogPriority.WARN, e) {
                    "Skipped OCR exclusion zone for missing manga=${zone.mangaId}"
                }
            }
        }
    }

    private fun OcrExclusionZone.duplicates(other: BackupOcrExclusionZone): Boolean =
        mangaId == other.mangaId &&
            chapterId == other.chapterId &&
            pageIndex == other.pageIndex &&
            boundingBox.left == other.leftNorm &&
            boundingBox.top == other.topNorm &&
            boundingBox.right == other.rightNorm &&
            boundingBox.bottom == other.bottomNorm
}
