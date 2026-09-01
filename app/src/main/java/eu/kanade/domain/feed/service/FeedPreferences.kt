package eu.kanade.domain.feed.service

import eu.kanade.domain.feed.model.FeedItem
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
