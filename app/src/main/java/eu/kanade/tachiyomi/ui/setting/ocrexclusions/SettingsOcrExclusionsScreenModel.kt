package eu.kanade.tachiyomi.ui.setting.ocrexclusions

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.domain.ocr.interactor.AddOcrExclusionZone
import mihon.domain.ocr.interactor.DeleteOcrExclusionZone
import mihon.domain.ocr.interactor.GetOcrExclusionZones
import mihon.domain.ocr.interactor.SetOcrExclusionZoneEnabled
import mihon.domain.ocr.model.OcrExclusionMatchType
import mihon.domain.ocr.model.OcrExclusionScope
import mihon.domain.ocr.model.OcrExclusionZone
import tachiyomi.core.common.util.lang.launchNonCancellable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsOcrExclusionsScreenModel(
    private val getOcrExclusionZones: GetOcrExclusionZones = Injekt.get(),
    private val addOcrExclusionZone: AddOcrExclusionZone = Injekt.get(),
    private val deleteOcrExclusionZone: DeleteOcrExclusionZone = Injekt.get(),
    private val setOcrExclusionZoneEnabled: SetOcrExclusionZoneEnabled = Injekt.get(),
) : StateScreenModel<SettingsOcrExclusionsScreenModel.State>(State()) {

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val zones: List<OcrExclusionZone> = emptyList(),
    )

    init {
        screenModelScope.launch {
            getOcrExclusionZones.subscribeAll().collect { zones ->
                mutableState.update { it.copy(zones = zones, isLoading = false) }
            }
        }
    }

    /** Adds a global text rule (word or phrase); text rules carry no manga context. */
    fun addTextRule(matchType: OcrExclusionMatchType, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        screenModelScope.launchNonCancellable {
            addOcrExclusionZone.await(
                mangaId = 0L,
                sourceId = 0L,
                chapterId = null,
                pageIndex = null,
                scope = OcrExclusionScope.PAGE,
                leftNorm = 0f,
                topNorm = 0f,
                rightNorm = 0f,
                bottomNorm = 0f,
                matchType = matchType,
                matchText = trimmed,
            )
        }
    }

    fun delete(id: Long) {
        screenModelScope.launchNonCancellable { deleteOcrExclusionZone.await(id) }
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        screenModelScope.launchNonCancellable { setOcrExclusionZoneEnabled.await(id, enabled) }
    }
}
