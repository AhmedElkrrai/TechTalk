package com.elkrrai.techtalk.presentation.battle.result.state

import com.elkrrai.techtalk.domain.model.user.AvatarCatalog
import com.elkrrai.techtalk.presentation.battle.state.BATTLE_TOTAL_QUESTIONS

data class BattleResultUiState(
    val score: Int = 0,
    val totalQuestions: Int = BATTLE_TOTAL_QUESTIONS,
    val technologyName: String = "",
    val xpGained: Int = 0,
    val playerName: String = "",
    val playerAvatarKey: String = AvatarCatalog.defaultAvatarKey,
    // Online results never offer "Try again" — see BattleResultViewModel.onTryAgain.
    val canTryAgain: Boolean = false
)
