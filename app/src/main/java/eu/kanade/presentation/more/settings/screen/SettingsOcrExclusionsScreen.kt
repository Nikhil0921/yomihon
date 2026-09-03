package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.tachiyomi.ui.setting.ocrexclusions.SettingsOcrExclusionsScreenModel
import eu.kanade.tachiyomi.util.system.toast
import mihon.domain.ocr.model.OcrExclusionMatchType
import mihon.domain.ocr.model.OcrExclusionScope
import mihon.domain.ocr.model.OcrExclusionZone
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

object SettingsOcrExclusionsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val backPress = LocalBackPress.current
        val screenModel = rememberScreenModel { SettingsOcrExclusionsScreenModel() }
        val state by screenModel.state.collectAsState()
        val context = LocalContext.current
        var addDialogType by remember { mutableStateOf<OcrExclusionMatchType?>(null) }
        val dialogType = addDialogType

        if (dialogType != null) {
            AddTextRuleDialog(
                matchType = dialogType,
                onDismiss = { addDialogType = null },
                onSave = { text ->
                    screenModel.addTextRule(dialogType, text)
                    addDialogType = null
                },
                onInvalid = { context.toast(MR.strings.ocr_exclusion_invalid_text) },
            )
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.ocr_exclusions_screen_title),
                    navigateUp = {
                        when {
                            navigator?.canPop == true -> navigator.pop()
                            else -> backPress?.invoke()
                        }
                    },
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            if (state.isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            val words = state.zones.filter { it.matchType == OcrExclusionMatchType.WORD }
            val phrases = state.zones.filter { it.matchType == OcrExclusionMatchType.PHRASE }
            val zones = state.zones.filter {
                it.matchType == OcrExclusionMatchType.ZONE || it.matchType == OcrExclusionMatchType.COMBINED
            }

            LazyColumn(
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Text(
                        text = stringResource(MR.strings.ocr_exclusion_rule_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                item {
                    SectionHeader(stringResource(MR.strings.ocr_exclusion_section_words))
                    if (words.isEmpty()) {
                        EmptyHint()
                    }
                    words.forEach { RuleRow(it, screenModel::setEnabled, screenModel::delete) }
                    AddRow(stringResource(MR.strings.ocr_exclusion_add_word)) {
                        addDialogType = OcrExclusionMatchType.WORD
                    }
                }
                item {
                    SectionHeader(stringResource(MR.strings.ocr_exclusion_section_phrases))
                    if (phrases.isEmpty()) {
                        EmptyHint()
                    }
                    phrases.forEach { RuleRow(it, screenModel::setEnabled, screenModel::delete) }
                    AddRow(stringResource(MR.strings.ocr_exclusion_add_phrase)) {
                        addDialogType = OcrExclusionMatchType.PHRASE
                    }
                }
                item {
                    SectionHeader(stringResource(MR.strings.ocr_exclusion_section_zones))
                    if (zones.isEmpty()) {
                        EmptyHint()
                    }
                    zones.forEach { RuleRow(it, screenModel::setEnabled, screenModel::delete) }
                }
            }
        }
    }

    @Composable
    private fun SectionHeader(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    @Composable
    private fun EmptyHint() {
        Text(
            text = stringResource(MR.strings.ocr_exclusion_empty),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    @Composable
    private fun AddRow(title: String, onClick: () -> Unit) {
        TextButton(onClick = onClick, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(title)
        }
    }

    @Composable
    private fun RuleRow(
        zone: OcrExclusionZone,
        onToggleEnabled: (Long, Boolean) -> Unit,
        onDelete: (Long) -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = zone.matchText ?: zone.typeLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (zone.matchType == OcrExclusionMatchType.COMBINED) {
                        "${zone.typeLabel()} · ${zone.scopeName()}"
                    } else if (zone.matchType == OcrExclusionMatchType.ZONE && zone.scope != OcrExclusionScope.PAGE) {
                        "${zone.typeLabel()} · ${zone.scopeName()}"
                    } else {
                        zone.typeLabel()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (zone.matchType == OcrExclusionMatchType.COMBINED ||
                    (zone.matchType == OcrExclusionMatchType.ZONE && zone.pageIndex != null)
                ) {
                    Text(
                        text = "${zone.boundingBox.left}, ${zone.boundingBox.top} — " +
                            "${zone.boundingBox.right}, ${zone.boundingBox.bottom}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (zone.matchType == OcrExclusionMatchType.ZONE && zone.pageIndex == null) {
                    Text(
                        text = stringResource(MR.strings.ocr_exclusion_type_legacy),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(checked = zone.enabled, onCheckedChange = { onToggleEnabled(zone.id, it) })
            TextButton(onClick = { onDelete(zone.id) }) {
                Text(stringResource(MR.strings.action_delete))
            }
        }
        HorizontalDivider()
    }

    @Composable
    private fun OcrExclusionZone.typeLabel(): String = when (matchType) {
        OcrExclusionMatchType.ZONE -> stringResource(MR.strings.ocr_exclusion_type_zone)
        OcrExclusionMatchType.WORD -> stringResource(MR.strings.ocr_exclusion_type_word)
        OcrExclusionMatchType.PHRASE -> stringResource(MR.strings.ocr_exclusion_type_phrase)
        OcrExclusionMatchType.COMBINED -> stringResource(MR.strings.ocr_exclusion_type_combined)
    }

    @Composable
    private fun OcrExclusionZone.scopeName(): String = when (scope) {
        OcrExclusionScope.PAGE -> stringResource(MR.strings.ocr_exclusion_scope_page)
        OcrExclusionScope.CHAPTER -> stringResource(MR.strings.ocr_exclusion_scope_chapter)
        OcrExclusionScope.MANGA -> stringResource(MR.strings.ocr_exclusion_scope_manga)
        OcrExclusionScope.SOURCE -> stringResource(MR.strings.ocr_exclusion_scope_source)
    }
}

@Composable
private fun AddTextRuleDialog(
    matchType: OcrExclusionMatchType,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onInvalid: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    when (matchType) {
                        OcrExclusionMatchType.WORD -> MR.strings.ocr_exclusion_add_word
                        else -> MR.strings.ocr_exclusion_add_phrase
                    },
                ),
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onSave(text) else onInvalid() }) {
                Text(stringResource(MR.strings.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}
