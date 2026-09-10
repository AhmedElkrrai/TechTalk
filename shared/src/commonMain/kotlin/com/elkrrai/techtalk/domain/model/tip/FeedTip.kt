package com.elkrrai.techtalk.domain.model.tip

import com.elkrrai.techtalk.domain.model.common.Difficulty

/**
 * A tip plus its topic/technology context — the feed-card model. Carries everything a
 * card renders so the UI needs no extra lookups.
 */
data class FeedTip(
    val id: Long,
    val topicId: Long,
    val title: String,
    val content: String,
    val codeSnippet: String?,
    val codeLang: String?,
    val difficulty: Difficulty,
    val interestCount: Int,
    val createdAt: Long,
    val topicName: String,
    val technologyName: String,
    val technologyTagColor: String
)
