package com.elkrrai.techtalk.domain.model.battle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleProgressionTest {

    @Test
    fun levelCurve_matchesSpecTable() {
        assertEquals(100, BattleProgression.xpRequiredForLevel(1))
        assertEquals(150, BattleProgression.xpRequiredForLevel(2))
        assertEquals(225, BattleProgression.xpRequiredForLevel(3))
        assertEquals(338, BattleProgression.xpRequiredForLevel(4))
        assertEquals(506, BattleProgression.xpRequiredForLevel(5))
    }

    @Test
    fun xpRequiredForLevel_coercesBelowLevelOneUpToOne() {
        assertEquals(BattleProgression.xpRequiredForLevel(1), BattleProgression.xpRequiredForLevel(0))
        assertEquals(BattleProgression.xpRequiredForLevel(1), BattleProgression.xpRequiredForLevel(-5))
    }

    @Test
    fun isWin_exactlyFiftyPercentCountsAsWin() {
        assertTrue(BattleProgression.isWin(score = 5, totalQuestions = 10))
    }

    @Test
    fun isWin_belowFiftyPercentIsNotAWin() {
        assertFalse(BattleProgression.isWin(score = 4, totalQuestions = 10))
    }

    @Test
    fun battleXpForScore_perfectScoreGetsFullBonus() {
        // base 50 (perfect) + score*10 (10*10=100) + 25 perfect bonus = 175
        assertEquals(175, BattleProgression.battleXpForScore(score = 10, totalQuestions = 10, isResigned = false))
    }

    @Test
    fun battleXpForScore_winTierBelowPerfect() {
        // base 35 (win, not perfect) + score*10 (6*10=60) = 95
        assertEquals(95, BattleProgression.battleXpForScore(score = 6, totalQuestions = 10, isResigned = false))
    }

    @Test
    fun battleXpForScore_lossTierWhenNotResigned() {
        // base 15 (loss) + score*10 (3*10=30) = 45
        assertEquals(45, BattleProgression.battleXpForScore(score = 3, totalQuestions = 10, isResigned = false))
    }

    @Test
    fun battleXpForScore_resignedBelowWinThresholdGetsZeroBase() {
        // base 0 (resigned, below 50%) + score*10 (2*10=20) = 20
        assertEquals(20, BattleProgression.battleXpForScore(score = 2, totalQuestions = 10, isResigned = true))
    }

    @Test
    fun battleXpForScore_resignedAboveWinThresholdStillGetsWinBase() {
        // isWin is checked before isResigned: base 35 (win) + score*10 (7*10=70) = 105
        assertEquals(105, BattleProgression.battleXpForScore(score = 7, totalQuestions = 10, isResigned = true))
    }

    @Test
    fun applyXp_supportsMultipleLevelUpsInOneCall() {
        // L1 needs 100, L2 needs 150 -> from level 1 / 0xp, gaining 400 crosses two levels
        // 400 - 100(L1) = 300, -150(L2) = 150, -225(L3) = -75 -> stop at L4 with 150 xp left over? recompute:
        // level 1, xp 0, gain 400 -> xp=400
        // xp(400) >= req(L1=100): xp=300, level=2
        // xp(300) >= req(L2=150): xp=150, level=3
        // xp(150) < req(L3=225): stop
        val (level, xp) = BattleProgression.applyXp(currentLevel = 1, currentXp = 0, gainedXp = 400)
        assertEquals(3, level)
        assertEquals(150, xp)
    }

    @Test
    fun applyXp_neverReturnsNegativeXp() {
        val (level, xp) = BattleProgression.applyXp(currentLevel = 2, currentXp = 10, gainedXp = -1000)
        assertEquals(2, level)
        assertEquals(0, xp)
    }
}
