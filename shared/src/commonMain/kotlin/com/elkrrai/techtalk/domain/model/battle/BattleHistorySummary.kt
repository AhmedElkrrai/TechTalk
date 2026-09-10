package com.elkrrai.techtalk.domain.model.battle

/** Computed by the repository from the DESC-ordered history list. [losses] counts
 * everything that is not [BattleStatus.WIN] (so [BattleStatus.RESIGNED] is a loss). */
data class BattleHistorySummary(
    val battlesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val winRate: Int = 0,
    val highestScore: Int = 0,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0
)
