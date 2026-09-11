package eu.kanade.tachiyomi.ui.reader.setting

/**
 * Reader bottom toolbar action ordering model.
 *
 * Serialization: comma-joined enum names in [ReaderPreferences.bottomBarActionOrder].
 * Empty string = default order (existing users see no change after upgrade).
 * Hidden actions stay in the stored order; visibility is a separate concern
 * (ocrTextSelectionEnabled / readAloudButtonEnabled).
 */
enum class ReaderBottomBarAction {
    READING_MODE,
    ORIENTATION,
    CROP_BORDERS,
    OCR,
    READ_ALOUD,
    SETTINGS,
    ;

    companion object {
        val DEFAULT_ORDER: List<ReaderBottomBarAction> = entries.toList()

        fun fromStoredIds(stored: List<String>): List<ReaderBottomBarAction> {
            val seen = HashSet<ReaderBottomBarAction>()
            val ordered = stored.mapNotNull { name ->
                entries.firstOrNull { it.name == name }?.takeIf(seen::add)
            }
            // Settings is pinned last: stable entry point into reader settings.
            val withoutSettings = ordered.filterNot { it == SETTINGS }
            val missing = DEFAULT_ORDER.filterNot { it in seen }
            return withoutSettings + missing.filterNot { it == SETTINGS } + listOf(SETTINGS)
        }

        fun serialize(actions: List<ReaderBottomBarAction>): String =
            actions.filterNot { it == SETTINGS }.joinToString(",") { it.name }
    }
}
