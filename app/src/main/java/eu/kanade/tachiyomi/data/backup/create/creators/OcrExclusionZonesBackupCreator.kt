package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.models.BackupOcrExclusionZone
import eu.kanade.tachiyomi.data.backup.models.backupOcrExclusionZoneMapper
import mihon.domain.ocr.interactor.GetOcrExclusionZones
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class OcrExclusionZonesBackupCreator(
    private val getOcrExclusionZones: GetOcrExclusionZones = Injekt.get(),
) {

    suspend operator fun invoke(): List<BackupOcrExclusionZone> {
        return getOcrExclusionZones.awaitAll().map(backupOcrExclusionZoneMapper)
    }
}
