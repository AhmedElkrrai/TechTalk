package com.elkrrai.techtalk.presentation.feed.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.presentation.component.LoadingScreen
import com.elkrrai.techtalk.presentation.feed.FeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onBrowseTechnologies: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Feed") },
                actions = {
                    Text(
                        text = "${state.currentPage + 1}/${state.filteredTips.size}",
                        modifier = Modifier.padding(end = 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                    IconButton(onClick = viewModel::onOpenFilterSheet) {
                        Text("⚙️")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
}
