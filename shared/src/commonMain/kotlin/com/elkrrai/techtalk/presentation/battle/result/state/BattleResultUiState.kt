package com.elkrrai.techtalk.presentation.battle.result.state

import com.elkrrai.techtalk.domain.model.user.AvatarCatalog

data class BattleResultUiState(
    val score: Int = 0,
    val totalQuestions: Int = 10,
    val technologyName: String = "",
    val xpGained: Int = 0,
    val playerName: String = "",
    val playerAvatarKey: String = AvatarCatalog.defaultAvatarKey
)
