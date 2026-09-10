package com.elkrrai.techtalk.domain.model.battle

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * XP + level progression rules. Deterministic and platform-free — the data layer
 * relies on this exact output, so keep or change behavior deliberately.
 */
object BattleProgression {
    private const val BASE_XP = 100.0
    private const val MULTIPLIER = 1.5

    /** L1: 100, L2: 150, L3: 225, L4: 338, L5: 506, … */
    fun xpRequiredForLevel(level: Int): Int {
        val safeLevel = level.coerceAtLeast(1)
        val required = (BASE_XP * MULTIPLIER.pow(safeLevel - 1)).roundToInt()
        return required.coerceAtLeast(100)
    }

    /** Exactly 50% counts as a win (ties count as a win). */
    fun isWin(score: Int, totalQuestions: Int): Boolean {
        val total = totalQuestions.coerceAtLeast(1)
        val clampedScore = score.coerceIn(0, total)
        return clampedScore * 2 >= total
    }

    /**
     * base = 50 if perfect, else 35 if win, else 0 if resigned, else 15; plus
     * `score * 10`; plus a 25 perfect bonus.
     *
     * [isWin] is evaluated before [isResigned], so a resigning player who still
     * cleared >= 50% earns win-tier (35) base XP, not the resign-tier (0) base.
     */
    fun battleXpForScore(score: Int, totalQuestions: Int, isResigned: Boolean): Int {
        val total = totalQuestions.coerceAtLeast(1)
        val clampedScore = score.coerceIn(0, total)
        val isPerfect = clampedScore == total
        val win = isWin(clampedScore, total)
        val base = when {
            isPerfect -> 50
            win -> 35
            isResigned -> 0
            else -> 15
        }
        val perfectBonus = if (isPerfect) 25 else 0
        return base + clampedScore * 10 + perfectBonus
    }

    /** Supports multiple level-ups in a single call; never returns negative XP. */
    fun applyXp(currentLevel: Int, currentXp: Int, gainedXp: Int): Pair<Int, Int> {
        var level = currentLevel.coerceAtLeast(1)
        var xp = (currentXp + gainedXp).coerceAtLeast(0)
        while (xp >= xpRequiredForLevel(level)) {
            xp -= xpRequiredForLevel(level)
            level++
        }
        return level to xp
    }
}
