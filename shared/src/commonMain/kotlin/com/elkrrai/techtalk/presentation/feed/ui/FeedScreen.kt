package com.elkrrai.techtalk.presentation.feed.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.elkrrai.techtalk.presentation.component.LoadingScreen
import com.elkrrai.techtalk.presentation.feed.FeedViewModel

/**
 * Body content only — no [androidx.compose.material3.Scaffold]/top bar of its own.
 * [com.elkrrai.techtalk.AppContent] owns the single top app bar for the Main screen
 * (hamburger + title + the Feed-specific counter/filter action when this tab is
 * selected) and the bottom nav; a second nested Scaffold here would draw its own top
 * bar UNDER the outer one, hiding the counter/filter button entirely.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onBrowseTechnologies: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingScreen()
            !state.hasSubscriptions -> SubscribeFirstPrompt(onBrowseTechnologies = onBrowseTechnologies)
            state.filteredTips.isEmpty() && state.filter != com.elkrrai.techtalk.presentation.feed.state.FeedFilter() ->
                FilterEmptyState(onClearFilters = viewModel::onClearFilters)

            state.filteredTips.isEmpty() -> AllCaughtUp(onReload = viewModel::onReloadTips)
            else -> TipFeed(
                tips = state.filteredTips,
                currentPage = state.currentPage,
                interestedTipIds = state.interestedTipIds,
                onPageChanged = viewModel::onPageChanged,
                onTipSeen = viewModel::onTipSeen,
                onToggleInterested = viewModel::onToggleInterested
            )
        }

        if (state.isFilterSheetOpen) {
            FilterBottomSheet(
                filter = state.filter,
                subscribedTechnologies = state.subscribedTechnologies,
                availableTopics = state.availableTopics,
                onSeenStatusChanged = viewModel::onSeenStatusChanged,
                onTechnologyFilterChanged = viewModel::onTechnologyFilterChanged,
                onTopicFilterToggled = viewModel::onTopicFilterToggled,
                onClearFilters = viewModel::onClearFilters,
                onDismiss = viewModel::onCloseFilterSheet
            )
        }
    }
}
