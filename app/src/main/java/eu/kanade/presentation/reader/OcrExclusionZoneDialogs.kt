package eu.kanade.presentation.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AdaptiveSheet
import mihon.domain.ocr.model.OcrExclusionMatchType
import mihon.domain.ocr.model.OcrExclusionScope
import mihon.domain.ocr.model.OcrExclusionZone
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Scope chooser shown after the user drag-selects an OCR exclusion region.
 * Every scope saves the page-anchored rectangle; wider scopes (CHAPTER/MANGA/
 * SOURCE) reuse the same rect on the same page index in that scope. Entering
 * text is OPTIONAL (starts empty) and turns the rule into a combined rect+text
 * rule. Cancel + re-select runs detection again.
 */
@Composable
fun ExclusionZoneScopeDialog(
    onDismissRequest: () -> Unit,
    onScopeSelected: (OcrExclusionScope, String?) -> Unit,
) {
    var selectedScope by remember { mutableStateOf<OcrExclusionScope?>(null) }
    // Never pre-fill: pre-filled OCR text turns every zone save into a COMBINED
    // rect+text rule (RC1 regression). Pure zones need an explicitly empty field.
    var matchText by remember { mutableStateOf("") }
    val chosen = selectedScope

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.ocr_exclusion_scope_title)) },
        text = {
            Column {
                scopeOption(MR.strings.ocr_exclusion_scope_page, OcrExclusionScope.PAGE) {
                    onScopeSelected(OcrExclusionScope.PAGE, null)
                }
                scopeOption(MR.strings.ocr_exclusion_scope_chapter, OcrExclusionScope.CHAPTER) {
                    selectedScope = OcrExclusionScope.CHAPTER
                }
                scopeOption(MR.strings.ocr_exclusion_scope_manga, OcrExclusionScope.MANGA) {
                    selectedScope = OcrExclusionScope.MANGA
                }
                scopeOption(MR.strings.ocr_exclusion_scope_source, OcrExclusionScope.SOURCE) {
                    selectedScope = OcrExclusionScope.SOURCE
                }
                if (chosen != null) {
                    OutlinedTextField(
                        value = matchText,
                        onValueChange = { matchText = it },
                        label = { Text(stringResource(MR.strings.ocr_exclusion_match_text_label)) },
                        supportingText = {
                            Text(stringResource(MR.strings.ocr_exclusion_match_text_optional))
                        },
                        singleLine = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (chosen != null) {
                TextButton(
                    onClick = { onScopeSelected(chosen, matchText) },
                ) {
                    Text(stringResource(MR.strings.action_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
private fun scopeOption(
    labelRes: StringResource,
    scope: OcrExclusionScope,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(labelRes)) },
        modifier = Modifier.clickable { onClick() },
    )
}

/**
 * Management sheet for existing exclusion zones of the current manga/source.
 */
@Composable
fun OcrExclusionZonesSheet(
    zones: List<OcrExclusionZone>,
    onDismissRequest: () -> Unit,
    onToggleEnabled: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
) {
    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                text = stringResource(MR.strings.ocr_exclusion_manage_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (zones.isEmpty()) {
                Text(
                    text = stringResource(MR.strings.ocr_exclusion_manage_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                )
            }
            zones.forEach { zone ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(ruleTypeLabel(zone), style = MaterialTheme.typography.bodyMedium)
                        zone.matchText?.let { text ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (zone.matchType != OcrExclusionMatchType.WORD &&
                            zone.matchType != OcrExclusionMatchType.PHRASE
                        ) {
                            Text(
                                text = "${zone.boundingBox.left}, ${zone.boundingBox.top} — " +
                                    "${zone.boundingBox.right}, ${zone.boundingBox.bottom}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (isLegacyZone(zone)) {
                            Text(
                                text = stringResource(MR.strings.ocr_exclusion_type_legacy),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Switch(
                        checked = zone.enabled,
                        onCheckedChange = { onToggleEnabled(zone.id, it) },
                    )
                    TextButton(onClick = { onDelete(zone.id) }) {
                        Text(stringResource(MR.strings.action_delete))
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

/** Legacy pre-redesign rows: rect rules without a drawing page are dormant. */
private fun isLegacyZone(zone: OcrExclusionZone): Boolean =
    zone.matchType == OcrExclusionMatchType.ZONE && zone.pageIndex == null

@Composable
private fun ruleTypeLabel(zone: OcrExclusionZone): String = when (zone.matchType) {
    OcrExclusionMatchType.ZONE -> stringResource(MR.strings.ocr_exclusion_type_zone)
    OcrExclusionMatchType.WORD -> stringResource(MR.strings.ocr_exclusion_type_word)
    OcrExclusionMatchType.PHRASE -> stringResource(MR.strings.ocr_exclusion_type_phrase)
    OcrExclusionMatchType.COMBINED -> stringResource(MR.strings.ocr_exclusion_type_combined)
}
