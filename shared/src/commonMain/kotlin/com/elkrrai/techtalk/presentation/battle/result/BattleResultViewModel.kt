package com.elkrrai.techtalk.presentation.battle.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.techtalk.presentation.battle.BattleResultRoute
import com.elkrrai.techtalk.presentation.battle.OfflineBattleRoute
import com.elkrrai.techtalk.presentation.battle.result.state.BattleResultUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** No repository needed — the offline battle it just left already persisted the result
 * and did all the question-loading, and "try again" now just means "navigate to a fresh
 * [OfflineBattleRoute]"; [OfflineBattleViewModel][com.elkrrai.techtalk.presentation.battle
 * .offline.OfflineBattleViewModel] does the actual (re-)loading once we get there. */
class BattleResultViewModel(private val route: BattleResultRoute) : ViewModel() {

    val state: StateFlow<BattleResultUiState> = MutableStateFlow(route.toResultUiState()).asStateFlow()

    private val _navigateToOfflineBattle = Channel<OfflineBattleRoute>(Channel.BUFFERED)
    val navigateToOfflineBattle: Flow<OfflineBattleRoute> = _navigateToOfflineBattle.receiveAsFlow()

    private val _navigateToHome = Channel<Unit>(Channel.BUFFERED)
    val navigateToHome: Flow<Unit> = _navigateToHome.receiveAsFlow()

    /** Offline only — an online result never shows this button (see
     * [BattleResultUiState.canTryAgain]); this guard covers the one-frame race where a
     * tap lands right as the button is disappearing. */
    fun onTryAgain() {
        if (!route.canTryAgain) return
        viewModelScope.launch {
            _navigateToOfflineBattle.send(
                OfflineBattleRoute(
                    technologyId = route.technologyId,
                    technologyName = route.technologyName,
                    playerName = route.playerName,
                    playerAvatarKey = route.playerAvatarKey,
                    timeControl = route.timeControl,
                    difficulty = route.difficulty
                )
            )
        }
    }

    fun onPickAnother() {
        viewModelScope.launch { _navigateToHome.send(Unit) }
    }
}

private fun BattleResultRoute.toResultUiState(): BattleResultUiState = BattleResultUiState(
    score = score,
    totalQuestions = totalQuestions,
    technologyName = technologyName,
    xpGained = xpGained,
    playerName = playerName,
    playerAvatarKey = playerAvatarKey,
    canTryAgain = canTryAgain
)
