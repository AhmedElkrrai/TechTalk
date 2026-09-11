package com.elkrrai.techtalk.presentation.technologylist.state

data class TechnologyListState(
    val items: List<TechnologyUiItem> = emptyList(),
    val isLoading: Boolean = true
)
