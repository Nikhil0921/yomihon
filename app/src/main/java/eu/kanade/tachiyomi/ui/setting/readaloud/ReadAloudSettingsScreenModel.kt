package eu.kanade.tachiyomi.ui.setting.readaloud

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.domain.tts.engine.TtsEngine
import mihon.domain.tts.engine.TtsEngineInfo
import mihon.domain.tts.engine.TtsVoiceInfo
import mihon.domain.tts.service.TtsPreferences
import mihon.domain.tts.service.TtsVoicePreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ReadAloudSettingsScreenModel(
    private val engine: TtsEngine = Injekt.get(),
    private val voicePreferences: TtsVoicePreferences = Injekt.get(),
    private val ttsPreferences: TtsPreferences = Injekt.get(),
) : StateScreenModel<ReadAloudSettingsScreenModel.ReadAloudSettingsState>(ReadAloudSettingsState()) {

    @Immutable
    data class ReadAloudSettingsState(
        val isLoading: Boolean = true,
        val loadFailed: Boolean = false,
        val engines: List<TtsEngineInfo> = emptyList(),
        val voices: List<TtsVoiceInfo> = emptyList(),
        val selectedEnginePackage: String = "",
        val selectedVoiceName: String = "",
        val selectedLanguageTag: String = "",
        val rate: Float = 1f,
        val pitch: Float = 1f,
        val isPreviewPlaying: Boolean = false,
    )

    fun load() {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true, loadFailed = false) }
            val ok = engine.initialize()
            if (!ok) {
                mutableState.update { it.copy(isLoading = false, loadFailed = true) }
                return@launch
            }
            val engines = engine.getEngines()
            val voices = engine.getVoices()
            mutableState.update {
                it.copy(
                    isLoading = false,
                    engines = engines,
                    voices = voices,
                    selectedEnginePackage = voicePreferences.ttsEnginePackage().get(),
                    selectedVoiceName = voicePreferences.ttsVoiceName().get(),
                    selectedLanguageTag = voicePreferences.ttsLanguageTag().get(),
                    rate = ttsPreferences.ttsSpeechRate().get(),
                    pitch = ttsPreferences.ttsPitch().get(),
                )
            }
        }
    }

    fun selectEngine(pkg: String) {
        if (pkg.isNotEmpty() && state.value.engines.none { it.packageName == pkg }) return
        voicePreferences.ttsEnginePackage().set(pkg)
        voicePreferences.ttsVoiceName().set("")
        voicePreferences.ttsLanguageTag().set("")
        engine.setEnginePackage(pkg)
        load()
    }

    fun selectLanguage(tag: String) {
        voicePreferences.ttsLanguageTag().set(tag)
        voicePreferences.ttsVoiceName().set("")
        mutableState.update { it.copy(selectedLanguageTag = tag, selectedVoiceName = "") }
    }

    fun selectVoice(name: String) {
        voicePreferences.ttsVoiceName().set(name)
        mutableState.update { it.copy(selectedVoiceName = name) }
        screenModelScope.launch { engine.initialize() }
    }

    fun setRate(rate: Float) {
        ttsPreferences.ttsSpeechRate().set(rate)
        mutableState.update { it.copy(rate = rate) }
    }

    fun setPitch(pitch: Float) {
        ttsPreferences.ttsPitch().set(pitch)
        mutableState.update { it.copy(pitch = pitch) }
    }

    fun preview() {
        if (mutableState.value.isPreviewPlaying) return
        screenModelScope.launch {
            try {
                engine.stop()
                engine.initialize()
                engine.setSpeechRate(mutableState.value.rate)
                engine.setPitch(mutableState.value.pitch)
                engine.acquireFocus()
                mutableState.update { it.copy(isPreviewPlaying = true) }
                engine.speak(PREVIEW_UTTERANCE_ID, PREVIEW_TEXT)
            } finally {
                mutableState.update { it.copy(isPreviewPlaying = false) }
                engine.abandonFocus()
                engine.stop()
            }
        }
    }

    fun stopPreview() {
        engine.stop()
    }

    fun resetVoiceConfig() {
        voicePreferences.reset()
        engine.setEnginePackage("")
        load()
    }

    private companion object {
        const val PREVIEW_UTTERANCE_ID = "tts-preview"
        const val PREVIEW_TEXT = "Hello. This is a preview of the selected reading voice."
    }
}
