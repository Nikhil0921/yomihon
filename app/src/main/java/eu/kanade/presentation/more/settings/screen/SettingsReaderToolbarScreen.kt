package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.tachiyomi.ui.reader.setting.ReaderBottomBarAction
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.plus
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Reader toolbar customization: drag-reorder the reader bottom bar actions
 * and toggle the optional ones (OCR / Read aloud). Ordering is stored
 * separately from visibility; Settings is fixed in the last position.
 */
class SettingsReaderToolbarScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val readerPreferences = remember { Injekt.get<ReaderPreferences>() }
        val storedOrder by readerPreferences.bottomBarActionOrder.collectAsState()
        val showOcr by readerPreferences.ocrTextSelectionEnabled.collectAsState()
        val showReadAloud by readerPreferences.readAloudButtonEnabled.collectAsState()
        var showResetDialog by remember { mutableStateOf(false) }

        val actions = remember(storedOrder) {
            ReaderBottomBarAction.fromStoredIds(
                storedOrder.takeIf { it.isNotBlank() }?.split(',') ?: emptyList(),
            )
        }.filterNot { it == ReaderBottomBarAction.SETTINGS }
            .toMutableStateList()

        val lazyListState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            val item = actions.removeAt(from.index)
            actions.add(to.index.coerceIn(0, actions.size), item)
            readerPreferences.bottomBarActionOrder.set(ReaderBottomBarAction.serialize(actions))
        }

        LaunchedEffect(storedOrder) {
            if (!reorderableState.isAnyItemDragging) {
                val fresh = ReaderBottomBarAction.fromStoredIds(
                    storedOrder.takeIf { it.isNotBlank() }?.split(',') ?: emptyList(),
                ).filterNot { it == ReaderBottomBarAction.SETTINGS }
                if (fresh != actions.toList()) {
                    actions.clear()
                    actions.addAll(fresh)
                }
            }
        }

        val moveAction: (Int, Int) -> Unit = { from, to ->
            val validTo = to.coerceIn(0, actions.lastIndex)
            if (from != validTo && from in actions.indices) {
                val item = actions.removeAt(from)
                actions.add(validTo, item)
                readerPreferences.bottomBarActionOrder.set(ReaderBottomBarAction.serialize(actions))
            }
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(stringResource(MR.strings.pref_reader_toolbar_reset)) },
                text = { Text(stringResource(MR.strings.pref_reader_toolbar_reset_confirm)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            readerPreferences.bottomBarActionOrder.delete()
                            readerPreferences.ocrTextSelectionEnabled.set(true)
                            readerPreferences.readAloudButtonEnabled.set(true)
                            showResetDialog = false
                        },
                    ) {
                        Text(stringResource(MR.strings.action_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(MR.strings.action_cancel))
                    }
                },
            )
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.pref_reader_customize_toolbar),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                    actions = {
                        TextButton(onClick = { showResetDialog = true }) {
                            Text(stringResource(MR.strings.action_reset))
                        }
                    },
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                Text(
                    text = stringResource(MR.strings.pref_reader_toolbar_order_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(vertical = 8.dp),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    contentPadding = topSmallPaddingValues +
                        PaddingValues(horizontal = MaterialTheme.padding.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    items(
                        items = actions,
                        key = { it.name },
                    ) { action ->
                        val index = actions.indexOf(action)
                        ReorderableItem(reorderableState, key = action.name) {
                            ToolbarActionRow(
                                action = action,
                                visible = when (action) {
                                    ReaderBottomBarAction.OCR -> showOcr
                                    ReaderBottomBarAction.READ_ALOUD -> showReadAloud
                                    else -> null
                                },
                                onToggleVisible = when (action) {
                                    ReaderBottomBarAction.OCR -> {
                                        { readerPreferences.ocrTextSelectionEnabled.set(it) }
                                    }
                                    ReaderBottomBarAction.READ_ALOUD -> {
                                        { readerPreferences.readAloudButtonEnabled.set(it) }
                                    }
                                    else -> null
                                },
                                onMove = { offset -> moveAction(index, index + offset) },
                            )
                        }
                    }
                    item(key = "fixed_settings") {
                        FixedActionRow(action = ReaderBottomBarAction.SETTINGS)
                    }
                }
            }
        }
    }

    @Composable
    private fun ReorderableCollectionItemScope.ToolbarActionRow(
        action: ReaderBottomBarAction,
        visible: Boolean?,
        onToggleVisible: ((Boolean) -> Unit)?,
        onMove: (Int) -> Unit,
    ) {
        val label = stringResource(action.labelRes)
        val upLabel = stringResource(MR.strings.action_move_up)
        val downLabel = stringResource(MR.strings.action_move_down)
        ElevatedCard(
            modifier = Modifier.semantics {
                customActions = listOf(
                    CustomAccessibilityAction(upLabel) {
                        onMove(-1)
                        true
                    },
                    CustomAccessibilityAction(downLabel) {
                        onMove(1)
                        true
                    },
                )
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.padding.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(MaterialTheme.padding.medium)
                        .draggableHandle(),
                )
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = MaterialTheme.padding.small),
                )
                if (onToggleVisible != null) {
                    Switch(
                        checked = visible ?: true,
                        onCheckedChange = onToggleVisible,
                        modifier = Modifier
                            .padding(end = MaterialTheme.padding.medium)
                            .semantics { contentDescription = label },
                    )
                }
            }
        }
    }

    @Composable
    private fun FixedActionRow(action: ReaderBottomBarAction) {
        val label = stringResource(action.labelRes)
        val fixedLabel = stringResource(MR.strings.pref_reader_toolbar_action_fixed)
        ElevatedCard(modifier = Modifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.padding.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = fixedLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(MaterialTheme.padding.medium),
                )
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = MaterialTheme.padding.small),
                )
            }
        }
    }
}

private val ReaderBottomBarAction.icon: ImageVector
    get() = when (this) {
        ReaderBottomBarAction.READING_MODE -> Icons.AutoMirrored.Outlined.MenuBook
        ReaderBottomBarAction.ORIENTATION -> Icons.Outlined.ScreenRotation
        ReaderBottomBarAction.CROP_BORDERS -> Icons.Outlined.Crop
        ReaderBottomBarAction.OCR -> Icons.Outlined.DocumentScanner
        ReaderBottomBarAction.READ_ALOUD -> Icons.Outlined.RecordVoiceOver
        ReaderBottomBarAction.SETTINGS -> Icons.Outlined.Settings
    }

private val ReaderBottomBarAction.labelRes
    get() = when (this) {
        ReaderBottomBarAction.READING_MODE -> MR.strings.pref_reader_toolbar_action_reading_mode
        ReaderBottomBarAction.ORIENTATION -> MR.strings.pref_reader_toolbar_action_orientation
        ReaderBottomBarAction.CROP_BORDERS -> MR.strings.pref_reader_toolbar_action_crop_borders
        ReaderBottomBarAction.OCR -> MR.strings.pref_reader_toolbar_action_ocr
        ReaderBottomBarAction.READ_ALOUD -> MR.strings.pref_reader_toolbar_action_read_aloud
        ReaderBottomBarAction.SETTINGS -> MR.strings.pref_reader_toolbar_action_settings
    }
