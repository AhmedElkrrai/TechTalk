package com.elkrrai.techtalk.domain.model.tech

data class TechnologyInfo(
    val id: Long,
    val name: String,
    val iconResourceName: String?,
    val description: String,
    val tagColor: String
)
