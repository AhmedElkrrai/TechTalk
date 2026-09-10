package com.elkrrai.techtalk.presentation.feed.state

/** Session-only — never persisted. */
data class FeedFilter(
    val seenStatus: SeenStatus = SeenStatus.ALL,
    val technologyId: Long? = null,
    val topicIds: Set<Long> = emptySet()
)

enum class SeenStatus { ALL, SEEN, UNSEEN, INTERESTING }
