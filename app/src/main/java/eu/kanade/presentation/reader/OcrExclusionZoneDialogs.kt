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
 * Wider-than-page scopes create a combined rule: the rectangle only suppresses
 * regions that also contain the entered text. Text detected inside the
 * selection is pre-filled; the user can edit or clear it before saving.
 * Cancel + re-select runs detection again.
 */
@Composable
fun ExclusionZoneScopeDialog(
    detectedText: String? = null,
    onDismissRequest: () -> Unit,
    onScopeSelected: (OcrExclusionScope, String?) -> Unit,
) {
    var selectedScope by remember { mutableStateOf<OcrExclusionScope?>(null) }
    var matchText by remember(detectedText) { mutableStateOf(detectedText.orEmpty()) }
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
                if (chosen != null && chosen != OcrExclusionScope.PAGE) {
                    OutlinedTextField(
                        value = matchText,
                        onValueChange = { matchText = it },
                        label = { Text(stringResource(MR.strings.ocr_exclusion_match_text_label)) },
                        singleLine = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (chosen != null && chosen != OcrExclusionScope.PAGE) {
                TextButton(
                    onClick = { onScopeSelected(chosen, matchText) },
                    enabled = matchText.isNotBlank(),
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

/** Legacy pre-redesign rows: rectangle rules wider than a single page are dormant. */
private fun isLegacyZone(zone: OcrExclusionZone): Boolean =
    zone.matchType == OcrExclusionMatchType.ZONE && zone.scope != OcrExclusionScope.PAGE

@Composable
private fun ruleTypeLabel(zone: OcrExclusionZone): String = when (zone.matchType) {
    OcrExclusionMatchType.ZONE -> stringResource(MR.strings.ocr_exclusion_type_zone)
    OcrExclusionMatchType.WORD -> stringResource(MR.strings.ocr_exclusion_type_word)
    OcrExclusionMatchType.PHRASE -> stringResource(MR.strings.ocr_exclusion_type_phrase)
    OcrExclusionMatchType.COMBINED -> stringResource(MR.strings.ocr_exclusion_type_combined)
}
