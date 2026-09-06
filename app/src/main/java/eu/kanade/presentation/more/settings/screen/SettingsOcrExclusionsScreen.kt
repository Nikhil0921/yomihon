package eu.kanade.presentation.more.settings.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.widget.PreferenceGroupCard
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.tachiyomi.ui.setting.ocrexclusions.SettingsOcrExclusionsScreenModel
import eu.kanade.tachiyomi.util.system.toast
import mihon.domain.ocr.model.OcrExclusionMatchType
import mihon.domain.ocr.model.OcrExclusionScope
import mihon.domain.ocr.model.OcrExclusionZone
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
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
        var editRule by remember { mutableStateOf<OcrExclusionZone?>(null) }
        val dialogType = addDialogType
        val ruleToEdit = editRule

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

        if (ruleToEdit != null) {
            EditRuleDialog(
                rule = ruleToEdit,
                onDismiss = { editRule = null },
                onSave = { text ->
                    screenModel.updateTextRule(ruleToEdit.id, text)
                    editRule = null
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
                item(key = "section-words") {
                    PreferenceGroupCard(
                        title = stringResource(MR.strings.ocr_exclusion_section_words),
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) {
                        TypeLegend(MR.strings.ocr_exclusion_legend_word)
                        if (words.isEmpty()) {
                            EmptyHint()
                        }
                        words.forEach {
                            RuleRow(
                                zone = it,
                                identity = state.identities[it.id],
                                onToggleEnabled = screenModel::setEnabled,
                                onDelete = screenModel::delete,
                                onEdit = { editRule = it },
                            )
                        }
                        AddRow(stringResource(MR.strings.ocr_exclusion_add_word)) {
                            addDialogType = OcrExclusionMatchType.WORD
                        }
                    }
                }
                item(key = "section-phrases") {
                    PreferenceGroupCard(
                        title = stringResource(MR.strings.ocr_exclusion_section_phrases),
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) {
                        TypeLegend(MR.strings.ocr_exclusion_legend_phrase)
                        if (phrases.isEmpty()) {
                            EmptyHint()
                        }
                        phrases.forEach {
                            RuleRow(
                                zone = it,
                                identity = state.identities[it.id],
                                onToggleEnabled = screenModel::setEnabled,
                                onDelete = screenModel::delete,
                                onEdit = { editRule = it },
                            )
                        }
                        AddRow(stringResource(MR.strings.ocr_exclusion_add_phrase)) {
                            addDialogType = OcrExclusionMatchType.PHRASE
                        }
                    }
                }
                item(key = "section-zones") {
                    PreferenceGroupCard(title = stringResource(MR.strings.ocr_exclusion_section_zones)) {
                        TypeLegend(MR.strings.ocr_exclusion_legend_zone)
                        TypeLegend(MR.strings.ocr_exclusion_legend_combined)
                        if (zones.isEmpty()) {
                            EmptyHint()
                        }
                        zones.forEach {
                            RuleRow(
                                zone = it,
                                identity = state.identities[it.id],
                                onToggleEnabled = screenModel::setEnabled,
                                onDelete = screenModel::delete,
                                onEdit = null,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TypeLegend(stringRes: StringResource) {
        Text(
            text = stringResource(stringRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
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
        identity: String?,
        onToggleEnabled: (Long, Boolean) -> Unit,
        onDelete: (Long) -> Unit,
        onEdit: ((OcrExclusionZone) -> Unit)?,
    ) {
        var expanded by rememberSaveable(zone.id) { mutableStateOf(false) }
        val text = zone.matchText
        val multiLine = text != null && text.lines().size > 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .animateContentSize()
                    .then(
                        if (multiLine || onEdit != null) {
                            Modifier.clickable { expanded = !expanded }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                if (identity != null) {
                    Text(
                        text = identity,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = when {
                        text != null && !expanded && multiLine -> text.lineSequence().first()
                        text != null -> text
                        else -> zone.typeLabel()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                if (multiLine) {
                    Text(
                        text = stringResource(MR.strings.ocr_exclusion_lines, text!!.lines().size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Badge(
                        text = zone.typeLabel(),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    if (zone.matchType == OcrExclusionMatchType.COMBINED ||
                        (zone.matchType == OcrExclusionMatchType.ZONE && zone.scope != OcrExclusionScope.PAGE)
                    ) {
                        Badge(
                            text = zone.scopeName(),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
                if (zone.matchType == OcrExclusionMatchType.COMBINED ||
                    (zone.matchType == OcrExclusionMatchType.ZONE && zone.pageIndex != null)
                ) {
                    if (expanded) {
                        Text(
                            text = "${zone.boundingBox.left}, ${zone.boundingBox.top} — " +
                                "${zone.boundingBox.right}, ${zone.boundingBox.bottom}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (expanded && onEdit != null) {
                    TextButton(onClick = { onEdit(zone) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                        Text(stringResource(MR.strings.action_edit_rule))
                    }
                }
            }
            if (multiLine) {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = stringResource(MR.strings.action_expand),
                    )
                }
            }
            Switch(checked = zone.enabled, onCheckedChange = { onToggleEnabled(zone.id, it) })
            IconButton(onClick = { onDelete(zone.id) }) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(MR.strings.action_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
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
private fun EditRuleDialog(
    rule: OcrExclusionZone,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onInvalid: () -> Unit,
) {
    var text by remember(rule.id) { mutableStateOf(rule.matchText.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.ocr_exclusion_edit_rule)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = false,
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
