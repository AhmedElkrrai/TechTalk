package com.elkrrai.techtalk.presentation.battle.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Registered as a Koin singleton, so it outlives every battle ViewModel — the only
 * channel between home -> battle -> result. [BattleState.battleSessionId] is what lets
 * [com.elkrrai.techtalk.presentation.battle.offline.OfflineBattleViewModel] detect "a
 * genuinely new battle started" and restart its timer. */
class BattleSessionStore {

    private val _state = MutableStateFlow(BattleState())
    val state: StateFlow<BattleState> = _state.asStateFlow()

    private var nextSessionId = 1

    fun startBattle(state: BattleState) {
        _state.value = state.copy(battleSessionId = nextSessionId)
        nextSessionId++
    }

    fun update(transform: (BattleState) -> BattleState) {
        _state.value = transform(_state.value)
    }

    /** Back to defaults; the session id counter also resets to 1. */
    fun reset() {
        nextSessionId = 1
        _state.value = BattleState()
    }
}
