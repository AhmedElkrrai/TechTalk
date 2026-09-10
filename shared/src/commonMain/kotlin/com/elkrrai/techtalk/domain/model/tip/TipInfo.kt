package com.elkrrai.techtalk.domain.model.tip

import com.elkrrai.techtalk.domain.model.common.Difficulty

data class TipInfo(
    val id: Long,
    val topicId: Long,
    val title: String,
    val content: String,
    val codeSnippet: String?,
    val codeLang: String?,
    val difficulty: Difficulty,
    val interestCount: Int,
    val createdAt: Long
)
