package eu.kanade.presentation.more.settings.screen

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.setting.readaloud.ReadAloudSettingsScreenModel
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import mihon.domain.tts.engine.TtsVoiceInfo
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Locale
import kotlin.math.roundToInt

object SettingsReadAloudScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_read_aloud

    @Composable
    override fun getPreferences(): List<Preference> {
        val screenModel = rememberScreenModel { ReadAloudSettingsScreenModel() }
        val state by screenModel.state.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(Unit) { screenModel.load() }

        if (state.isLoading) {
            return listOf(
                Preference.PreferenceItem.CustomPreference(title = stringResource(MR.strings.loading)) {
                    CircularProgressIndicator()
                },
            )
        }

        if (state.loadFailed) {
            return listOf(
                Preference.PreferenceItem.InfoPreference(
                    title = stringResource(MR.strings.tts_voices_unavailable),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.action_retry),
                    onClick = { screenModel.load() },
                ),
            )
        }

        val allTags = state.voices.map { it.languageTag }.distinct().sorted()
        val selectedLanguagePrefix = state.selectedLanguageTag.substringBefore('-')
        val localeTags = allTags.filter { it.startsWith(selectedLanguagePrefix) }

        val voiceCandidates = when {
            state.selectedLanguageTag.isEmpty() -> state.voices
            state.selectedLanguageTag.contains('-') -> state.voices.filter {
                it.languageTag == state.selectedLanguageTag
            }
            else -> state.voices.filter { it.languageTag.startsWith(selectedLanguagePrefix) }
        }

        val engineEntries = buildMap {
            put("", stringResource(MR.strings.tts_default_system_engine))
            state.engines.forEach { put(it.packageName, it.label) }
        }.toImmutableMap()

        val languageEntries = buildMap {
            put("", stringResource(MR.strings.tts_default_system_engine))
            allTags.forEach { put(it, localeDisplayName(it)) }
        }.toImmutableMap()

        val localeEntries = buildMap {
            put("", stringResource(MR.strings.tts_default_system_engine))
            localeTags.forEach { put(it, localeDisplayName(it)) }
        }.toImmutableMap()

        val voiceEntries = voiceCandidates
            .associate { it.name to voiceLabel(it) }
            .toImmutableMap()

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.tts_section_text_to_speech),
                preferenceItems = listOf(
                    Preference.PreferenceItem.BasicListPreference(
                        value = state.selectedEnginePackage,
                        entries = engineEntries,
                        title = stringResource(MR.strings.tts_engine),
                        subtitleProvider = { _, e ->
                            e[state.selectedEnginePackage] ?: stringResource(MR.strings.tts_default_system_engine)
                        },
                        onValueChanged = { screenModel.selectEngine(it) },
                    ),
                    Preference.PreferenceItem.BasicListPreference(
                        value = state.selectedLanguageTag,
                        entries = languageEntries,
                        title = stringResource(MR.strings.tts_language),
                        subtitleProvider = { v, e -> e[v] ?: v },
                        onValueChanged = { screenModel.selectLanguage(it) },
                    ),
                    Preference.PreferenceItem.BasicListPreference(
                        value = state.selectedLanguageTag,
                        entries = localeEntries,
                        title = stringResource(MR.strings.tts_locale),
                        enabled = state.selectedLanguageTag.isNotEmpty(),
                        subtitleProvider = { v, e -> e[v] ?: v },
                        onValueChanged = { screenModel.selectLanguage(it) },
                    ),
                    Preference.PreferenceItem.BasicListPreference(
                        value = state.selectedVoiceName,
                        entries = voiceEntries,
                        title = stringResource(MR.strings.tts_voice),
                        searchable = true,
                        subtitleProvider = { v, e -> e[v] ?: v },
                        onValueChanged = { screenModel.selectVoice(it) },
                    ),
                ).toImmutableList(),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.tts_section_voice_calibration),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SliderPreference(
                        value = (state.rate * 100).roundToInt(),
                        title = stringResource(MR.strings.pref_tts_speech_rate),
                        valueString = "${(state.rate * 100).roundToInt()}%",
                        valueRange = 50..200,
                        onValueChanged = { screenModel.setRate(it / 100f) },
                    ),
                    Preference.PreferenceItem.SliderPreference(
                        value = (state.pitch * 100).roundToInt(),
                        title = stringResource(MR.strings.pref_tts_pitch),
                        valueString = "${(state.pitch * 100).roundToInt()}%",
                        valueRange = 50..200,
                        onValueChanged = { screenModel.setPitch(it / 100f) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.tts_preview_voice),
                        subtitle = stringResource(MR.strings.tts_play_sample),
                        widget = if (state.isPreviewPlaying) {
                            {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            if (state.isPreviewPlaying) screenModel.stopPreview() else screenModel.preview()
                        },
                    ),
                ).toImmutableList(),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.tts_section_advanced),
                preferenceItems = listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.tts_engine_information),
                        subtitle = engineInfoSummary(state),
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        title = stringResource(MR.strings.tts_available_voices, state.voices.size),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.tts_reset_voice_config),
                        onClick = {
                            screenModel.resetVoiceConfig()
                            context.toast(MR.strings.tts_config_reset)
                        },
                    ),
                ).toImmutableList(),
            ),
        )
    }

    private fun localeDisplayName(tag: String): String {
        val name = Locale.forLanguageTag(tag).getDisplayName(Locale.getDefault())
        return name.takeIf { it.isNotBlank() } ?: tag
    }

    private fun voiceLabel(voice: TtsVoiceInfo): String = buildString {
        append(voice.name)
        if (voice.quality >= 400) append(" · high quality")
        if (voice.latency <= 200) append(" · low latency")
        if (voice.networkRequired) append(" · network")
    }

    @Composable
    private fun engineInfoSummary(state: ReadAloudSettingsScreenModel.ReadAloudSettingsState): String {
        val engine = state.engines.firstOrNull { it.packageName == state.selectedEnginePackage }
        val label = engine?.label
            ?: state.engines.firstOrNull { it.isSystemDefault }?.label
            ?: stringResource(MR.strings.tts_default_system_engine)
        return stringResource(MR.strings.tts_available_voices, state.voices.size).let { "$label · $it" }
    }
}
