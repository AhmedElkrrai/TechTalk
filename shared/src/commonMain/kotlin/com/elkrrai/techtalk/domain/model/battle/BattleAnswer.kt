package com.elkrrai.techtalk.domain.model.battle

data class BattleAnswer(
    val id: Long,
    val questionId: Long,
    val answerText: String,
    val isCorrect: Boolean
)
