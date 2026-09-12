package com.elkrrai.techtalk.presentation.battle.state

/** Single source of truth for how many questions a battle runs — read by
 * [com.elkrrai.techtalk.presentation.battle.offline.OfflineBattleViewModel] (the sole
 * owner of question loading), and by the default field values in
 * [com.elkrrai.techtalk.presentation.battle.offline.state.OfflineBattleUiState],
 * [com.elkrrai.techtalk.presentation.battle.online.state.OnlineBattleUiState], and
 * [com.elkrrai.techtalk.presentation.battle.result.state.BattleResultUiState]. */
const val BATTLE_TOTAL_QUESTIONS = 10
