package com.elkrrai.techtalk.presentation.feed.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.elkrrai.techtalk.domain.model.tip.FeedTip

@Composable
fun TipFeed(
    tips: List<FeedTip>,
    currentPage: Int,
    interestedTipIds: Set<Long>,
    onPageChanged: (Int) -> Unit,
    onTipSeen: (Long) -> Unit,
    onToggleInterested: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = currentPage) { tips.size }

    LaunchedEffect(pagerState.settledPage, tips) {
        val tip = tips.getOrNull(pagerState.settledPage) ?: return@LaunchedEffect
        onPageChanged(pagerState.settledPage)
        onTipSeen(tip.id)
    }

    HorizontalPager(state = pagerState, modifier = modifier.fillMaxSize()) { page ->
        val tip = tips[page]
        TipCard(
            tip = tip,
            isInterested = tip.id in interestedTipIds,
            onToggleInterested = { onToggleInterested(tip.id) }
        )
    }
}
