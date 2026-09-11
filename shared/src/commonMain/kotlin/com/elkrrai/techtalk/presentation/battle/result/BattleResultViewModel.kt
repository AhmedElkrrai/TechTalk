package com.elkrrai.techtalk.presentation.battle.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.repository.TechTalkRepository
import com.elkrrai.techtalk.presentation.battle.home.state.BattleMode
import com.elkrrai.techtalk.presentation.battle.result.state.BattleResultUiState
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

    /** Always reloads *local* questions and preserves the session [BattleMode] — after
     * an online match this restarts into the online flow with unusable local
     * questions. Matches the original app's behavior; verify before relying on it. */
    fun onTryAgain() {
        val session = sessionStore.state.value
        val technology = session.selectedTechnology ?: return
        viewModelScope.launch {
            val questionIds = (
                if (session.selectedDifficulty == Difficulty.RANDOM) {
                    repository.getQuestionIdsByTechnology(technology.id)
                } else {
                    repository.getQuestionIdsByTechnologyAndDifficulty(technology.id, session.selectedDifficulty)
                }
                ).shuffled().take(10)
            if (questionIds.isEmpty()) return@launch

            val questions = questionIds.mapNotNull { repository.getQuestionById(it) }
            val firstAnswers = questions.firstOrNull()
                ?.let { repository.getAnswersByQuestionId(it.id) }
                ?.shuffled()
                .orEmpty()

            sessionStore.startBattle(
                session.copy(
                    phase = BattlePhase.BATTLE,
                    mode = session.mode,
                    questions = questions,
                    currentAnswers = firstAnswers,
                    currentQuestionIndex = 0,
                    selectedAnswerId = null,
                    hasAnswered = false,
                    score = 0,
                    xpGained = 0,
                    remainingTimeSeconds = if (session.selectedTimeControl.totalSeconds < 0) null else session.selectedTimeControl.totalSeconds
                )
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
    playerAvatarKey = playerAvatarKey
)
