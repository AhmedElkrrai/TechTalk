package com.elkrrai.techtalk.domain.model.battle

data class BattleHistoryItem(
    val id: Long,
    val technologyName: String,
    val score: Int,
    val totalQuestions: Int,
    val status: BattleStatus,
    val xpGained: Int,
    val playedAt: Long
)
