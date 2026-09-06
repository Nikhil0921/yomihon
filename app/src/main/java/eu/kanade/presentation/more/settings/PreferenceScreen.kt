package eu.kanade.presentation.more.settings

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.widget.PreferenceGroupCard
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import kotlin.time.Duration.Companion.seconds

/**
 * Preference Screen composable which contains a list of [Preference] items
 * @param items [Preference] items which should be displayed on the preference screen. An item can be a single [PreferenceItem] or a group ([Preference.PreferenceGroup])
 * @param modifier [Modifier] to be applied to the preferenceScreen layout
 */
@Composable
fun PreferenceScreen(
    items: List<Preference>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val state = rememberLazyListState()
    val highlightKey = SearchableSettings.highlightKey
    val rowPx = with(LocalDensity.current) { LocalPreferenceMinHeight.current.toPx() }
    if (highlightKey != null) {
        LaunchedEffect(Unit) {
            val target = items.findHighlightedItem(highlightKey)
            if (target != null) {
                delay(0.5.seconds)
                state.animateScrollToItem(target.first)
                if (target.second > 0) state.animateScrollBy(target.second * rowPx)
            }
            SearchableSettings.highlightKey = null
        }
    }

    ScrollbarLazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
    ) {
        items.fastForEachIndexed { i, preference ->
            when (preference) {
                // One group = one lazily-composed grouped surface; blank title = unheaded card
                is Preference.PreferenceGroup -> {
                    if (!preference.enabled) return@fastForEachIndexed

                    item(key = "group-$i-${preference.title}") {
                        PreferenceGroupCard(title = preference.title.takeIf { it.isNotBlank() }) {
                            preference.preferenceItems.forEach { groupItem ->
                                PreferenceItem(
                                    item = groupItem,
                                    highlightKey = highlightKey,
                                )
                            }
                        }
                    }
                    item(key = "spacer-$i") {
                        if (i < items.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // Create Preference Item
                is Preference.PreferenceItem<*, *> -> item {
                    PreferenceItem(
                        item = preference,
                        highlightKey = highlightKey,
                    )
                }
            }
        }
    }
}

/**
 * Locates the highlighted preference by lazy-column coordinates: the index of the
 * group's card item (or of the bare item) plus the row index within the group.
 * Must mirror the item emission above: enabled groups emit card + spacer (2 lazy
 * items), disabled groups emit none, bare items emit 1.
 */
private fun List<Preference>.findHighlightedItem(highlightKey: String): Pair<Int, Int>? {
    var lazyIndex = 0
    forEach { preference ->
        when (preference) {
            is Preference.PreferenceGroup -> {
                if (preference.enabled) {
                    preference.preferenceItems.forEachIndexed { rowIndex, groupItem ->
                        if (groupItem.title == highlightKey) return lazyIndex to rowIndex
                    }
                    lazyIndex += 2
                }
            }
            is Preference.PreferenceItem<*, *> -> {
                if (preference.title == highlightKey) return lazyIndex to 0
                lazyIndex++
            }
        }
    }
    return null
}
