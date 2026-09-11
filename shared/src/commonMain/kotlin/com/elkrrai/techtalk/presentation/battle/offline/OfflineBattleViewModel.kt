package com.elkrrai.techtalk.presentation.battle.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.techtalk.domain.model.battle.BattleProgression
import com.elkrrai.techtalk.domain.model.battle.BattleStatus
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.repository.TechTalkRepository
import com.elkrrai.techtalk.presentation.battle.component.BattleAnswerOptionUi
import com.elkrrai.techtalk.presentation.battle.home.state.BattleMode
import com.elkrrai.techtalk.presentation.battle.offline.state.OfflineBattleUiState
import com.elkrrai.techtalk.presentation.battle.state.BattlePhase
import com.elkrrai.techtalk.presentation.battle.state.BattleSessionStore
import com.elkrrai.techtalk.presentation.battle.state.BattleState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Every handler guards `mode == OFFLINE` — [com.elkrrai.techtalk.presentation.battle
 * .online.OnlineBattleViewModel] observes the same [BattleSessionStore], so an
 * unguarded handler here would corrupt online state. */
class OfflineBattleViewModel(
    private val repository: TechTalkRepository,
    private val sessionStore: BattleSessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(OfflineBattleUiState())
    val state: StateFlow<OfflineBattleUiState> = _state.asStateFlow()

    private var activeSessionId: Int = -1
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            sessionStore.state.collect { session ->
                if (session.mode != BattleMode.OFFLINE) {
                    timerJob?.cancel()
                    return@collect
                }

                _state.value = session.toOfflineBattleUiState()

                when {
                    session.phase == BattlePhase.BATTLE && session.battleSessionId != activeSessionId -> {
                        activeSessionId = session.battleSessionId
                        startTimer()
                    }
                    session.phase != BattlePhase.BATTLE -> timerJob?.cancel()
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        if (sessionStore.state.value.remainingTimeSeconds == null) return // INFINITY — no countdown
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = sessionStore.state.value
                if (current.mode != BattleMode.OFFLINE || current.phase != BattlePhase.BATTLE) return@launch
                val remaining = (current.remainingTimeSeconds ?: 0) - 1
                if (remaining <= 0) {
                    sessionStore.update { it.copy(remainingTimeSeconds = 0) }
                    finalizeBattle(isResigned = false)
                    return@launch
                }
                sessionStore.update { it.copy(remainingTimeSeconds = remaining) }
            }
        }
    }

    fun onAnswerSelected(answerId: Long) {
        val current = sessionStore.state.value
        if (current.mode != BattleMode.OFFLINE || current.hasAnswered) return
        val isCorrect = current.currentAnswers.any { it.id == answerId && it.isCorrect }
        sessionStore.update {
            it.copy(
                selectedAnswerId = answerId,
                hasAnswered = true,
                score = if (isCorrect) it.score + 1 else it.score
            )
        }
    }

    fun onNextQuestion() {
        val current = sessionStore.state.value
        if (current.mode != BattleMode.OFFLINE) return
        viewModelScope.launch {
            val nextIndex = current.currentQuestionIndex + 1
            if (nextIndex >= current.totalQuestions || nextIndex >= current.questions.size) {
                finalizeBattle(isResigned = false)
                return@launch
            }
            val nextQuestion = current.questions[nextIndex]
            val nextAnswers = repository.getAnswersByQuestionId(nextQuestion.id).shuffled()
            sessionStore.update {
                it.copy(
                    currentQuestionIndex = nextIndex,
                    currentAnswers = nextAnswers,
                    selectedAnswerId = null,
                    hasAnswered = false
                )
            }
        }
    }

    fun onResign() {
        if (sessionStore.state.value.mode != BattleMode.OFFLINE) return
        finalizeBattle(isResigned = true)
    }

    private fun finalizeBattle(isResigned: Boolean) {
        timerJob?.cancel()
        val current = sessionStore.state.value
        if (current.mode != BattleMode.OFFLINE) return
        val technology = current.selectedTechnology ?: return

        val isWin = BattleProgression.isWin(current.score, current.totalQuestions)
        val status = when {
            isResigned -> BattleStatus.RESIGNED
            isWin -> BattleStatus.WIN
            else -> BattleStatus.LOSS
        }
        val xpGained = BattleProgression.battleXpForScore(current.score, current.totalQuestions, isResigned)

        viewModelScope.launch {
            runCatching { repository.awardBattleXp(xpGained) }
            runCatching {
                repository.recordBattleResult(
                    technologyId = technology.id,
                    technologyName = technology.name,
                    score = current.score,
                    totalQuestions = current.totalQuestions,
                    status = status,
                    xpGained = xpGained
                )
            }
            sessionStore.update { it.copy(phase = BattlePhase.RESULT, xpGained = xpGained) }
        }
    }
}

private fun BattleState.toOfflineBattleUiState(): OfflineBattleUiState {
    val question = currentQuestion
    return OfflineBattleUiState(
        title = selectedTechnology?.name.orEmpty(),
        questionText = question?.questionText.orEmpty(),
        difficulty = question?.difficulty ?: Difficulty.BEGINNER,
        explanation = question?.explanation.orEmpty(),
        currentAnswers = currentAnswers.map { BattleAnswerOptionUi(it.id, it.answerText, it.isCorrect) },
        selectedAnswerId = selectedAnswerId,
        hasAnswered = hasAnswered,
        currentQuestionIndex = currentQuestionIndex,
        totalQuestions = totalQuestions,
        score = score,
        remainingTimeSeconds = remainingTimeSeconds,
        isLastQuestion = currentQuestionIndex >= totalQuestions - 1
    )
}
