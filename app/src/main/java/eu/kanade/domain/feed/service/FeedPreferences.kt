package eu.kanade.domain.feed.service

import eu.kanade.domain.feed.model.FeedItem
import eu.kanade.domain.feed.model.FeedListing
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class FeedPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun feeds(): Preference<List<FeedItem>> = preferenceStore.getObjectFromString(
        "pref_feed_items",
        emptyList(),
        { feeds -> serializeFeeds(feeds) },
        { raw -> deserializeFeeds(raw) },
    )

    fun showSourceSelector(): Preference<Boolean> = preferenceStore.getBoolean("pref_feed_show_source_selector", true)

    fun showListingSelector(): Preference<Boolean> = preferenceStore.getBoolean("pref_feed_show_listing_selector", true)

    fun defaultListing(): Preference<FeedListing?> = preferenceStore.getObjectFromString(
        "pref_feed_default_listing",
        defaultValue = null,
        serializer = { it?.name ?: "" },
        deserializer = { raw ->
            FeedListing.entries.firstOrNull { it.name == raw }
        },
    )

    fun gridColumns(): Preference<Int> = preferenceStore.getInt("pref_feed_grid_columns", 0)

    fun compactGrid(): Preference<Boolean> = preferenceStore.getBoolean("pref_feed_compact_grid", false)

    private fun serializeFeeds(feeds: List<FeedItem>): String =
        feedJson.encodeToString(kotlinx.serialization.builtins.ListSerializer(FeedItem.serializer()), feeds)

    private fun deserializeFeeds(raw: String): List<FeedItem> =
        runCatching {
            feedJson.decodeFromString(kotlinx.serialization.builtins.ListSerializer(FeedItem.serializer()), raw)
        }.getOrDefault(emptyList())

    private companion object {
        val feedJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    }
}
