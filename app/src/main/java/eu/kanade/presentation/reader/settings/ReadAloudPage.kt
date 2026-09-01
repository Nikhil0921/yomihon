package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import kotlin.math.roundToInt

@Composable
internal fun ColumnScope.ReadAloudPage(
    screenModel: ReaderSettingsScreenModel,
    onOpenVoiceSettings: () -> Unit,
    onAddExclusionZone: () -> Unit,
    onManageExclusionZones: () -> Unit,
) {
    val ttsPreferences = screenModel.ttsPreferences

    val speechRate by ttsPreferences.ttsSpeechRate().collectAsState()

    SliderItem(
        label = stringResource(MR.strings.pref_tts_speech_rate),
        value = (speechRate * 100).roundToInt(),
        valueRange = 50..300,
        valueString = "${(speechRate * 100).roundToInt()}%",
        onChange = { ttsPreferences.ttsSpeechRate().set(it / 100f) },
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

    // Speech cleanup: what the read-aloud engine says and how it says it.
    // See SpeechCleaner / SpeechRegionFilter in :domain for the logic.
    HeadingItem(stringResource(MR.strings.pref_tts_speech_cleanup_section))
    CheckboxItem(
        label = stringResource(MR.strings.pref_tts_skip_punctuation_only),
        pref = ttsPreferences.ttsSkipPunctuationOnly(),
    )
    CheckboxItem(
        label = stringResource(MR.strings.pref_tts_skip_ocr_garbage),
        pref = ttsPreferences.ttsSkipOcrGarbage(),
    )
    CheckboxItem(
        label = stringResource(MR.strings.pref_tts_normalize_punctuation),
        pref = ttsPreferences.ttsNormalizePunctuation(),
    )
    CheckboxItem(
        label = stringResource(MR.strings.pref_tts_ellipsis_to_pause),
        pref = ttsPreferences.ttsEllipsisToPause(),
    )

    HeadingItem(stringResource(MR.strings.pref_tts_spoken_content_section))
    CheckboxItem(
        label = stringResource(MR.strings.pref_tts_speak_sfx),
        pref = ttsPreferences.ttsSpeakSoundEffects(),
    )
    CheckboxItem(
        label = stringResource(MR.strings.pref_tts_speak_expressions),
        pref = ttsPreferences.ttsSpeakExpressions(),
    )
    CheckboxItem(
        label = stringResource(MR.strings.pref_tts_skip_foreign_script),
        pref = ttsPreferences.ttsSkipForeignScript(),
    )

    HeadingItem(stringResource(MR.strings.ocr_exclusion_section))
    CheckboxItem(
        label = stringResource(MR.strings.ocr_exclusion_enabled),
        pref = ttsPreferences.ttsOcrExclusionsEnabled(),
    )
    TextPreferenceWidget(
        title = stringResource(MR.strings.ocr_exclusion_add_zone),
        onPreferenceClick = onAddExclusionZone,
    )
    TextPreferenceWidget(
        title = stringResource(MR.strings.ocr_exclusion_manage_zones),
        onPreferenceClick = onManageExclusionZones,
    )

    TextPreferenceWidget(
        title = stringResource(MR.strings.tts_advanced_voice_settings),
        onPreferenceClick = onOpenVoiceSettings,
    )
}
