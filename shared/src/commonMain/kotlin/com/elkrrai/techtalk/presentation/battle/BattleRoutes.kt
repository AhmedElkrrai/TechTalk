package com.elkrrai.techtalk.presentation.battle

import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.online.BattleTimeControl
import kotlinx.serialization.Serializable

/**
 * Type-safe destinations for [BattleScreen]'s nested nav graph. Each screen's ViewModel
 * carries what it needs forward as typed route arguments (technology/mode/time
 * control/difficulty/player identity, final score/XP) instead of a shared mutable
 * session object — see the removal of `BattleSessionStore`/`BattleState`. Navigation is
 * triggered by each ViewModel emitting a one-shot nav event that [BattleScreen] collects
 * and turns into a `navController.navigate(...)` call; the battle flow has no back-stack
 * affordance of its own (resigning is explicit, via the top bar), so navigation here
 * always fully replaces the stack rather than pushing.
 */
@Serializable
data object BattleHomeRoute

@Serializable
data class OfflineBattleRoute(
    val technologyId: Long,
    val technologyName: String,
    val playerName: String,
    val playerAvatarKey: String,
    val timeControl: BattleTimeControl,
    val difficulty: Difficulty
)

@Serializable
data class BattleLobbyRoute(
    val technologyId: Long,
    val technologyName: String,
    val playerName: String,
    val playerAvatarKey: String,
    val timeControl: BattleTimeControl,
    val difficulty: Difficulty
)

@Serializable
data class OnlineBattleRoute(
    val technologyId: Long,
    val technologyName: String,
    val playerName: String,
    val playerAvatarKey: String,
    val currentPlayerId: String,
    val matchId: String,
    val startedAtEpochMillis: Long,
    val totalDurationSeconds: Int?
)

@Serializable
data class BattleResultRoute(
    val score: Int,
    val totalQuestions: Int,
    val technologyId: Long,
    val technologyName: String,
    val playerName: String,
    val playerAvatarKey: String,
    val xpGained: Int,
    val canTryAgain: Boolean,
    // Carried only so "Try again" can rebuild an OfflineBattleRoute with no repository
    // round-trip and no dependency on BattleHomeUiState still being around. Defaulted
    // because an online result (canTryAgain = false) has no meaningful values for these.
    val timeControl: BattleTimeControl = BattleTimeControl.ONE_MINUTE,
    val difficulty: Difficulty = Difficulty.RANDOM
)
