package com.elkrrai.techtalk.domain.model.battle

import com.elkrrai.techtalk.domain.model.common.Difficulty

/** Linked to a **topic**, not a technology directly — technology is reached via the topic. */
data class BattleQuestion(
    val id: Long,
    val topicId: Long,
    val questionText: String,
    val difficulty: Difficulty,
    val explanation: String
)
