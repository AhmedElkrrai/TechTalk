package com.elkrrai.techtalk.presentation.technologylist.state

import org.jetbrains.compose.resources.DrawableResource

data class TechnologyUiItem(
    val id: Long,
    val name: String,
    val iconResourceName: String?,
    val iconResource: DrawableResource?,
    val description: String,
    val tagColor: String,
    val isSubscribed: Boolean
)
