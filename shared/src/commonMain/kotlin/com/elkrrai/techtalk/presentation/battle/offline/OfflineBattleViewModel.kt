package com.elkrrai.techtalk.presentation.battle.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.techtalk.domain.model.battle.BattleAnswer
import com.elkrrai.techtalk.domain.model.battle.BattleProgression
import com.elkrrai.techtalk.domain.model.battle.BattleQuestion
import com.elkrrai.techtalk.domain.model.battle.BattleStatus
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.online.BattleTimeControl
import com.elkrrai.techtalk.domain.repository.TechTalkRepository
import com.elkrrai.techtalk.presentation.battle.BattleResultRoute
import com.elkrrai.techtalk.presentation.battle.OfflineBattleRoute
import com.elkrrai.techtalk.presentation.battle.component.BattleAnswerOptionUi
import com.elkrrai.techtalk.presentation.battle.offline.state.OfflineBattleUiState
import com.elkrrai.techtalk.presentation.battle.state.BATTLE_TOTAL_QUESTIONS
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Owns question loading for BOTH a fresh start and a "try again" — both just mean
 * "navigate here with this config," so [route] carries everything needed and this
 * ViewModel fetches its own data, rather than depending on whichever screen navigated
 * here having already fetched it. Also owns the whole battle's live progress (current
 * question, answers, score, timer) as purely local state — nothing outside this
 * ViewModel's own lifetime ever needs to see it mid-battle. */
class OfflineBattleViewModel(
    private val route: OfflineBattleRoute,
    private val repository: TechTalkRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OfflineBattleUiState(title = route.technologyName))
    val state: StateFlow<OfflineBattleUiState> = _state.asStateFlow()

    private val _navigateToResult = Channel<BattleResultRoute>(Channel.BUFFERED)
    val navigateToResult: Flow<BattleResultRoute> = _navigateToResult.receiveAsFlow()

    private var questions: List<BattleQuestion> = emptyList()
    private var timerJob: Job? = null

    // Guards against the timer and a resign/next-question tap racing each other into a
    // double finalize — purely local now; no other screen's ViewModel can ever touch
    // this instance's state, so this is the only race left to guard against.
    private var hasFinalized = false

    init {
        viewModelScope.launch {
            val ids = (
                if (route.difficulty == Difficulty.RANDOM) {
                    repository.getQuestionIdsByTechnology(route.technologyId)
                } else {
                    repository.getQuestionIdsByTechnologyAndDifficulty(route.technologyId, route.difficulty)
                }
                ).shuffled().take(BATTLE_TOTAL_QUESTIONS)

            // BattleHomeViewModel already did a cheap existence check before navigating
            // here, so this should be empty only in a rare race (data changed between
            // that check and this load). Nothing to answer in that case — surface it
            // plainly rather than crashing on an out-of-bounds question lookup later.
            questions = ids.mapNotNull { repository.getQuestionById(it) }
            if (questions.isEmpty()) {
                _state.update { it.copy(questionText = "No questions available for this technology yet") }
                return@launch
            }

            val firstAnswers = repository.getAnswersByQuestionId(questions.first().id).shuffled()
            _state.update { applyQuestion(it, index = 0, answers = firstAnswers) }

            _state.update {
                it.copy(remainingTimeSeconds = if (route.timeControl == BattleTimeControl.INFINITY) null else route.timeControl.totalSeconds)
            }
            startTimer()
        }
    }

    private fun applyQuestion(state: OfflineBattleUiState, index: Int, answers: List<BattleAnswer>): OfflineBattleUiState {
        val question = questions[index]
        return state.copy(
            questionText = question.questionText,
            difficulty = question.difficulty,
            explanation = question.explanation,
            currentAnswers = answers.map { BattleAnswerOptionUi(it.id, it.answerText, it.isCorrect) },
            selectedAnswerId = null,
            hasAnswered = false,
            currentQuestionIndex = index,
            isLastQuestion = index >= BATTLE_TOTAL_QUESTIONS - 1 || index >= questions.size - 1
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        if (_state.value.remainingTimeSeconds == null) return // INFINITY — no countdown
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (hasFinalized) return@launch
                val remaining = (_state.value.remainingTimeSeconds ?: 0) - 1
                if (remaining <= 0) {
                    _state.update { it.copy(remainingTimeSeconds = 0) }
                    finalizeBattle(isResigned = false)
                    return@launch
                }
                _state.update { it.copy(remainingTimeSeconds = remaining) }
            }
        }
    }

    fun onAnswerSelected(answerId: Long) {
        val current = _state.value
        if (current.hasAnswered) return
        val isCorrect = current.currentAnswers.any { it.id == answerId && it.isCorrect }
        _state.update {
            it.copy(
                selectedAnswerId = answerId,
                hasAnswered = true,
                score = if (isCorrect) it.score + 1 else it.score
            )
        }
    }

    fun onNextQuestion() {
        if (hasFinalized) return
        val nextIndex = _state.value.currentQuestionIndex + 1
        if (nextIndex >= BATTLE_TOTAL_QUESTIONS || nextIndex >= questions.size) {
            finalizeBattle(isResigned = false)
            return
        }
        viewModelScope.launch {
            val nextAnswers = repository.getAnswersByQuestionId(questions[nextIndex].id).shuffled()
            _state.update { applyQuestion(it, index = nextIndex, answers = nextAnswers) }
        }
    }

    fun onResign() {
        if (hasFinalized) return
        finalizeBattle(isResigned = true)
    }

    private fun finalizeBattle(isResigned: Boolean) {
        if (hasFinalized) return
        hasFinalized = true
        timerJob?.cancel()
        val current = _state.value

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
                    technologyId = route.technologyId,
                    technologyName = route.technologyName,
                    score = current.score,
                    totalQuestions = current.totalQuestions,
                    status = status,
                    xpGained = xpGained
                )
            }
            _navigateToResult.send(
                BattleResultRoute(
                    score = current.score,
                    totalQuestions = current.totalQuestions,
                    technologyId = route.technologyId,
                    technologyName = route.technologyName,
                    playerName = route.playerName,
                    playerAvatarKey = route.playerAvatarKey,
                    xpGained = xpGained,
                    canTryAgain = true,
                    timeControl = route.timeControl,
                    difficulty = route.difficulty
                )
            )
        }
    }
}
