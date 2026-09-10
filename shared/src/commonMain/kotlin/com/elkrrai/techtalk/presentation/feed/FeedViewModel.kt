package com.elkrrai.techtalk.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.techtalk.domain.repository.TechTalkRepository
import com.elkrrai.techtalk.presentation.feed.state.FeedFilter
import com.elkrrai.techtalk.presentation.feed.state.FeedState
import com.elkrrai.techtalk.presentation.feed.state.SeenStatus
import com.elkrrai.techtalk.presentation.feed.state.TopicFilterOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedViewModel(private val repository: TechTalkRepository) : ViewModel() {

    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()

    init {
        // This is the app's de-facto bootstrap — DB seeding only happens because the
        // Feed tab's ViewModel is constructed at root composition.
        viewModelScope.launch { repository.initialize() }
        observeSubscribedTechnologies()
    }

    private fun observeSubscribedTechnologies() {
        viewModelScope.launch {
            repository.observeSubscribedTechnologies().collect { technologies ->
                val hasSubs = technologies.isNotEmpty()
                _state.update { it.copy(subscribedTechnologies = technologies, hasSubscriptions = hasSubs, isLoading = hasSubs) }
                if (hasSubs) {
                    loadFeed()
                } else {
                    _state.update { it.copy(isLoading = false, tips = emptyList(), filteredTips = emptyList()) }
                }
            }
        }
    }

    private suspend fun loadFeed() {
        val tips = repository.getFeedTipsWithDetails().shuffled()
        val interested = repository.getInterestedTipIds()
        val seen = repository.getSeenTipIds()
        // Copies from _state.value (not a captured snapshot) so filters chosen during
        // this suspend call are not clobbered.
        _state.value = applyFilter(
            _state.value.copy(
                tips = tips,
                interestedTipIds = interested,
                seenTipIds = seen,
                isLoading = false
            )
        )
    }

    /** Always resets [FeedState.currentPage] to 0 and recomputes [FeedState
     * .availableTopics] (derived from the loaded tips, empty unless a technology
     * filter is selected). */
    private fun applyFilter(state: FeedState): FeedState {
        val selectedTechnologyName = state.filter.technologyId
            ?.let { id -> state.subscribedTechnologies.firstOrNull { it.id == id }?.name }

        val filtered = state.tips.filter { tip ->
            val matchesSeen = when (state.filter.seenStatus) {
                SeenStatus.ALL -> true
                SeenStatus.SEEN -> tip.id in state.seenTipIds
                SeenStatus.UNSEEN -> tip.id !in state.seenTipIds
                SeenStatus.INTERESTING -> tip.id in state.interestedTipIds
            }
            val matchesTechnology = selectedTechnologyName == null || tip.technologyName == selectedTechnologyName
            val matchesTopic = state.filter.topicIds.isEmpty() || tip.topicId in state.filter.topicIds
            matchesSeen && matchesTechnology && matchesTopic
        }

        val availableTopics = if (selectedTechnologyName == null) {
            emptyList()
        } else {
            state.tips
                .filter { it.technologyName == selectedTechnologyName }
                .map { TopicFilterOption(it.topicId, it.topicName) }
                .distinctBy { it.id }
                .sortedBy { it.name }
        }

        return state.copy(filteredTips = filtered, currentPage = 0, availableTopics = availableTopics)
    }

    fun onTipSeen(tipId: Long) {
        viewModelScope.launch { repository.markTipSeen(tipId) }
        _state.update { it.copy(seenTipIds = it.seenTipIds + tipId) }
    }

    fun onToggleInterested(tipId: Long) {
        viewModelScope.launch { repository.toggleInterested(tipId) }
        _state.update {
            val newSet = if (tipId in it.interestedTipIds) it.interestedTipIds - tipId else it.interestedTipIds + tipId
            it.copy(interestedTipIds = newSet)
        }
    }

    fun onPageChanged(page: Int) {
        _state.update { it.copy(currentPage = page) }
    }

    fun onReloadTips() {
        viewModelScope.launch {
            _state.update { it.copy(isReloading = true) }
            loadFeed()
            _state.update { it.copy(isReloading = false) }
        }
    }

    fun onImportMessageShown() {
        _state.update { it.copy(importMessage = null) }
    }

    fun onOpenFilterSheet() {
        _state.update { it.copy(isFilterSheetOpen = true) }
    }

    fun onCloseFilterSheet() {
        _state.update { it.copy(isFilterSheetOpen = false) }
    }

    fun onSeenStatusChanged(seenStatus: SeenStatus) {
        _state.update { applyFilter(it.copy(filter = it.filter.copy(seenStatus = seenStatus))) }
    }

    /** Resets [FeedFilter.topicIds] — topic chips are only meaningful within one
     * technology. */
    fun onTechnologyFilterChanged(technologyId: Long?) {
        _state.update { applyFilter(it.copy(filter = it.filter.copy(technologyId = technologyId, topicIds = emptySet()))) }
    }

    fun onTopicFilterToggled(topicId: Long) {
        _state.update {
            val current = it.filter.topicIds
            val newTopics = if (topicId in current) current - topicId else current + topicId
            applyFilter(it.copy(filter = it.filter.copy(topicIds = newTopics)))
        }
    }

    fun onClearFilters() {
        _state.update { applyFilter(it.copy(filter = FeedFilter())) }
    }
}
