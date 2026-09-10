package com.elkrrai.techtalk.presentation.feed.state

import com.elkrrai.techtalk.domain.model.tech.TechnologyInfo
import com.elkrrai.techtalk.domain.model.tip.FeedTip

data class FeedState(
    val tips: List<FeedTip> = emptyList(),
    val filteredTips: List<FeedTip> = emptyList(),
    val isLoading: Boolean = true,
    val hasSubscriptions: Boolean = false,
    val interestedTipIds: Set<Long> = emptySet(),
    val seenTipIds: Set<Long> = emptySet(),
    val currentPage: Int = 0,
    val filter: FeedFilter = FeedFilter(),
    val subscribedTechnologies: List<TechnologyInfo> = emptyList(),
    val availableTopics: List<TopicFilterOption> = emptyList(),
    val isFilterSheetOpen: Boolean = false,
    // Declared but not currently set by FeedViewModel — import feedback lives in
    // GetMoreContentState instead. Kept for source-compat with the original screen.
    val importMessage: String? = null,
    val isReloading: Boolean = false
) {
    val showTipsCounter: Boolean
        get() = hasSubscriptions && !isLoading && filteredTips.isNotEmpty()
}
