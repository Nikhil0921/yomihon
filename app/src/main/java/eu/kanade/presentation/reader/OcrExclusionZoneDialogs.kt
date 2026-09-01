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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AdaptiveSheet
import mihon.domain.ocr.model.OcrExclusionScope
import mihon.domain.ocr.model.OcrExclusionZone
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Scope chooser shown after the user drag-selects an OCR exclusion region.
 */
@Composable
fun ExclusionZoneScopeDialog(
    onDismissRequest: () -> Unit,
    onScopeSelected: (OcrExclusionScope) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.ocr_exclusion_scope_title)) },
        text = {
            Column {
                scopeOption(MR.strings.ocr_exclusion_scope_page, OcrExclusionScope.PAGE, onScopeSelected)
                scopeOption(MR.strings.ocr_exclusion_scope_chapter, OcrExclusionScope.CHAPTER, onScopeSelected)
                scopeOption(MR.strings.ocr_exclusion_scope_manga, OcrExclusionScope.MANGA, onScopeSelected)
                scopeOption(MR.strings.ocr_exclusion_scope_source, OcrExclusionScope.SOURCE, onScopeSelected)
            }
        },
        confirmButton = {},
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
    onScopeSelected: (OcrExclusionScope) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(labelRes)) },
        modifier = Modifier.clickable { onScopeSelected(scope) },
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
                        Text(scopeLabel(zone.scope), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${zone.boundingBox.left}, ${zone.boundingBox.top} — " +
                                "${zone.boundingBox.right}, ${zone.boundingBox.bottom}",
                            style = MaterialTheme.typography.bodySmall,
                        )
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

@Composable
private fun scopeLabel(scope: OcrExclusionScope): String = when (scope) {
    OcrExclusionScope.PAGE -> stringResource(MR.strings.ocr_exclusion_scope_page)
    OcrExclusionScope.CHAPTER -> stringResource(MR.strings.ocr_exclusion_scope_chapter)
    OcrExclusionScope.MANGA -> stringResource(MR.strings.ocr_exclusion_scope_manga)
    OcrExclusionScope.SOURCE -> stringResource(MR.strings.ocr_exclusion_scope_source)
}
