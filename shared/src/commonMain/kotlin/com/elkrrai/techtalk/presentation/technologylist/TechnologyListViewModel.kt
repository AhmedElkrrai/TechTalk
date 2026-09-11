package com.elkrrai.techtalk.presentation.technologylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.techtalk.domain.repository.TechTalkRepository
import com.elkrrai.techtalk.presentation.technologylist.mapper.toTechnologyUiItem
import com.elkrrai.techtalk.presentation.technologylist.state.TechnologyListState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TechnologyListViewModel(private val repository: TechTalkRepository) : ViewModel() {

    private val _state = MutableStateFlow(TechnologyListState())
    val state: StateFlow<TechnologyListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeAllTechnologies(),
                repository.observeSubscribedTechnologies()
            ) { all, subscribed ->
                val subscribedIds = subscribed.map { it.id }.toSet()
                all.map { it.toTechnologyUiItem(isSubscribed = it.id in subscribedIds) }
            }.collect { items ->
                _state.value = TechnologyListState(items = items, isLoading = false)
            }
        }
    }

    fun onToggleSubscription(technologyId: Long) {
        viewModelScope.launch {
            if (repository.isSubscribed(technologyId)) {
                repository.unsubscribe(technologyId)
            } else {
                repository.subscribe(technologyId)
            }
        }
    }
}
