package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import kotlin.math.roundToInt

@Composable
internal fun ColumnScope.ReadAloudPage(screenModel: ReaderSettingsScreenModel) {
    val ttsPreferences = screenModel.ttsPreferences

    val speechRate by ttsPreferences.ttsSpeechRate().collectAsState()
    val pitch by ttsPreferences.ttsPitch().collectAsState()

    SliderItem(
        label = stringResource(MR.strings.pref_tts_speech_rate),
        value = (speechRate * 100).roundToInt(),
        valueRange = 50..200,
        valueString = "${(speechRate * 100).roundToInt()}%",
        onChange = { ttsPreferences.ttsSpeechRate().set(it / 100f) },
    )
    SliderItem(
        label = stringResource(MR.strings.pref_tts_pitch),
        value = (pitch * 100).roundToInt(),
        valueRange = 50..200,
        valueString = "${(pitch * 100).roundToInt()}%",
        onChange = { ttsPreferences.ttsPitch().set(it / 100f) },
    )

    CheckboxItem(
        label = stringResource(MR.strings.pref_tts_auto_page_turn),
        pref = ttsPreferences.ttsAutoPageTurn(),
    )
    CheckboxItem(
        label = stringResource(MR.strings.pref_tts_auto_next_chapter),
        pref = ttsPreferences.ttsAutoNextChapter(),
    )
    CheckboxItem(
        label = stringResource(MR.strings.pref_tts_keep_screen_on),
        pref = ttsPreferences.ttsKeepScreenOn(),
    )
}
