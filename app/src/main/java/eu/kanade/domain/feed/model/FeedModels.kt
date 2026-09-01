package eu.kanade.domain.feed.model

import kotlinx.serialization.Serializable

/** Listing type of a feed section; availability follows Source.supportsLatest. */
enum class FeedListing { POPULAR, LATEST }

/** A configured feed section. Order in the persisted list = display order. */
@Serializable
data class FeedItem(
    val sourceId: Long,
    val listing: FeedListing,
    val enabled: Boolean = true,
)
