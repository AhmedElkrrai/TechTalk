package com.elkrrai.techtalk.presentation.battle.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.repository.TechTalkRepository
import com.elkrrai.techtalk.presentation.battle.home.state.BattleMode
import com.elkrrai.techtalk.presentation.battle.result.state.BattleResultUiState
import com.elkrrai.techtalk.presentation.battle.state.BATTLE_TOTAL_QUESTIONS
import com.elkrrai.techtalk.presentation.battle.state.BattlePhase
import com.elkrrai.techtalk.presentation.battle.state.BattleSessionStore
import com.elkrrai.techtalk.presentation.battle.state.BattleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BattleResultViewModel(
    private val repository: TechTalkRepository,
    private val sessionStore: BattleSessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(BattleResultUiState())
    val state: StateFlow<BattleResultUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.state.collect { session ->
                if (session.phase != BattlePhase.RESULT)
                    return@collect
                _state.value = session.toResultUiState()
            }
        }
    }

    /** Offline only — an online result never shows this button (see
     * [BattleResultUiState.canTryAgain]), and this guard covers the one-frame race where
     * a tap lands right as the button is disappearing. [BattleSessionStore.restartOfflineBattle]
     * itself also hardcodes `mode = OFFLINE`, so even a direct call here can't produce a
     * battle whose mode doesn't match the local questions it's about to load. */
    fun onTryAgain() {
        val session = sessionStore.state.value
        if (session.mode != BattleMode.OFFLINE) return
        val technology = session.selectedTechnology ?: return
        viewModelScope.launch {
            val questionIds = (
                if (session.selectedDifficulty == Difficulty.RANDOM) {
                    repository.getQuestionIdsByTechnology(technology.id)
                } else {
                    repository.getQuestionIdsByTechnologyAndDifficulty(technology.id, session.selectedDifficulty)
                }
                ).shuffled().take(BATTLE_TOTAL_QUESTIONS)
            if (questionIds.isEmpty()) return@launch

            val questions = questionIds.mapNotNull { repository.getQuestionById(it) }
            val firstAnswers = questions.firstOrNull()
                ?.let { repository.getAnswersByQuestionId(it.id) }
                ?.shuffled()
                .orEmpty()

            sessionStore.restartOfflineBattle(
                technology = technology,
                subscribedTechnologies = session.subscribedTechnologies,
                playerName = session.playerName,
                playerAvatarKey = session.playerAvatarKey,
                timeControl = session.selectedTimeControl,
                difficulty = session.selectedDifficulty,
                questions = questions,
                currentAnswers = firstAnswers
            )
        }
    }

    fun onPickAnother() {
        sessionStore.reset()
    }
}

private fun BattleState.toResultUiState(): BattleResultUiState = BattleResultUiState(
    score = score,
    totalQuestions = totalQuestions,
    technologyName = selectedTechnology?.name.orEmpty(),
    xpGained = xpGained,
    playerName = playerName,
    playerAvatarKey = playerAvatarKey,
    canTryAgain = mode == BattleMode.OFFLINE
)
