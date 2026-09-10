package com.elkrrai.techtalk.domain.model.user

import com.elkrrai.techtalk.domain.model.battle.BattleProgression

/** [currentXp] is XP **within the current level**, not lifetime XP. */
data class UserProfile(
    val name: String,
    val avatarKey: String,
    val level: Int = 1,
    val currentXp: Int = 0
) {
    val xpToNextLevel: Int
        get() = BattleProgression.xpRequiredForLevel(level).coerceAtLeast(1)

    val xpProgressFraction: Float
        get() = (currentXp.toFloat() / xpToNextLevel.toFloat()).coerceIn(0f, 1f)
}
