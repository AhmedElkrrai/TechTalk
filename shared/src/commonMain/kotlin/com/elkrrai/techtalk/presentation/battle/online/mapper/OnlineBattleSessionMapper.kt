package com.elkrrai.techtalk.presentation.battle.online.mapper

import com.elkrrai.techtalk.presentation.battle.state.BattleState
import com.elkrrai.techtalk.presentation.battle.state.OnlineBattlePhase

/** Sets MATCH phase, clears questions/score, null time for INFINITY (when
 * [totalDurationSeconds] is already null). */
fun BattleState.toOnlineBattleState(totalDurationSeconds: Int?): BattleState = copy(
    onlinePhase = OnlineBattlePhase.MATCH,
    questions = emptyList(),
    score = 0,
    remainingTimeSeconds = totalDurationSeconds
)
