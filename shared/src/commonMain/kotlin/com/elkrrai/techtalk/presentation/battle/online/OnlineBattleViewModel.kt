package com.elkrrai.techtalk.presentation.battle.online

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.techtalk.domain.model.battle.BattleProgression
import com.elkrrai.techtalk.domain.model.battle.BattleStatus
import com.elkrrai.techtalk.domain.model.online.OnlineBattleEvent
import com.elkrrai.techtalk.domain.model.online.OnlineScoreBoard
import com.elkrrai.techtalk.domain.model.online.SubmitOnlineAnswerRequest
import com.elkrrai.techtalk.domain.repository.TechTalkRepository
import com.elkrrai.techtalk.domain.usecase.online.ObserveOnlineBattleEventsUseCase
import com.elkrrai.techtalk.domain.usecase.online.SubmitOnlineAnswerUseCase
import com.elkrrai.techtalk.presentation.battle.BattleResultRoute
import com.elkrrai.techtalk.presentation.battle.OnlineBattleRoute
import com.elkrrai.techtalk.presentation.battle.online.mapper.mapOnlineFailureToUserMessage
import com.elkrrai.techtalk.presentation.battle.online.mapper.toBattleAnswerOptionUi
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineBattleUiState
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineConnectionStatus
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Created only once [OnlineBattleEvent.MatchStarted] has already fired — everything it
 * needs from that event ([route]'s `matchId`/`startedAtEpochMillis`/`totalDurationSeconds`)
 * arrives via nav args instead of being re-observed from the (non-replaying) events
 * SharedFlow, which used to be a real, if minor, race. Ignores everything once
 * [OnlineBattleUiState.isMatchEnded]; drops events whose matchId conflicts with the
 * active one. */
class OnlineBattleViewModel(
    private val route: OnlineBattleRoute,
    private val repository: TechTalkRepository,
    observeOnlineBattleEvents: ObserveOnlineBattleEventsUseCase,
    private val submitOnlineAnswer: SubmitOnlineAnswerUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnlineBattleUiState())
    val state: StateFlow<OnlineBattleUiState> = _state.asStateFlow()

    private val _navigateToResult = Channel<BattleResultRoute>(Channel.BUFFERED)
    val navigateToResult: Flow<BattleResultRoute> = _navigateToResult.receiveAsFlow()

    // Client-side dedupe on top of the transport's own eventId dedupe.
    private var lastQuestionEventKey: String? = null
    private var lastScoreBoardKey: String? = null

    // Maps the shared UI component's Long id (derived from a hash) back to the
    // server's original String answerId, so the right one gets submitted.
    private var answerIdByHash: Map<Long, String> = emptyMap()

    private var hasPersistedResult = false
    private var countdownJob: Job? = null

    init {
        _state.update {
            it.copy(
                playerName = route.playerName,
                playerAvatarKey = route.playerAvatarKey,
                connectionStatus = OnlineConnectionStatus.CONNECTED,
                matchId = route.matchId
            )
        }
        startLocalCountdown(route.totalDurationSeconds, route.startedAtEpochMillis)
        viewModelScope.launch {
            observeOnlineBattleEvents().collect { event -> handleEvent(event) }
        }
    }

    private fun handleEvent(event: OnlineBattleEvent) {
        if (_state.value.isMatchEnded) return
        if (hasMatchIdConflict(event)) return

        when (event) {
            is OnlineBattleEvent.QuestionPushed -> handleQuestionPushed(event)

            is OnlineBattleEvent.AnswerResult -> {
                _state.update { it.copy(hasAnswered = true, isSubmittingAnswer = false) }
                withScoreBoard(event.scoreboard)
            }

            is OnlineBattleEvent.ScoreUpdated -> withScoreBoard(event.scoreboard)

            // Explicitly ignored — the countdown is local, re-derived from
            // startedAtEpochMillis rather than trusting server ticks.
            is OnlineBattleEvent.TimerTick -> Unit

            is OnlineBattleEvent.OpponentConnectionChanged -> _state.update {
                it.copy(
                    connectionStatus = if (event.isConnected) OnlineConnectionStatus.CONNECTED else OnlineConnectionStatus.RECONNECTING
                )
            }

            is OnlineBattleEvent.ConnectionHealthChanged -> _state.update {
                it.copy(
                    connectionStatus = if (event.isHealthy) OnlineConnectionStatus.CONNECTED else OnlineConnectionStatus.RECONNECTING
                )
            }

            is OnlineBattleEvent.Disconnected -> _state.update {
                it.copy(connectionStatus = OnlineConnectionStatus.DISCONNECTED)
            }

            is OnlineBattleEvent.MatchEnded -> {
                withScoreBoard(event.scoreboard)
                finalizeToResult(playerScore = _state.value.playerScore, isResigned = false)
            }

            is OnlineBattleEvent.Failure -> _state.update {
                it.copy(errorMessage = mapOnlineFailureToUserMessage(event.code, event.message))
            }

            is OnlineBattleEvent.RoomExpired -> _state.update {
                it.copy(errorMessage = "Room expired: ${event.reason}")
            }

            // Lobby-only events (plus MatchStarted, which is what caused this ViewModel
            // to be created in the first place — everything it carries already arrived
            // via `route` — so re-observing it here would be redundant, not a fix).
            is OnlineBattleEvent.Connected,
            is OnlineBattleEvent.RoomCreated,
            is OnlineBattleEvent.RoomJoined,
            is OnlineBattleEvent.LobbyUpdated,
            is OnlineBattleEvent.MatchStarting,
            is OnlineBattleEvent.MatchStarted -> Unit
        }
    }

    private fun handleQuestionPushed(event: OnlineBattleEvent.QuestionPushed) {
        val key = "${event.matchId}#${event.questionIndex}#${event.payload.questionId}"
        if (key == lastQuestionEventKey) return
        lastQuestionEventKey = key

        answerIdByHash = event.payload.options.associate { it.answerId.hashCode().toLong() to it.answerId }

        _state.update {
            it.copy(
                matchId = event.matchId,
                currentQuestionIndex = event.questionIndex,
                currentQuestionId = event.payload.questionId,
                currentQuestionText = event.payload.prompt,
                currentQuestionDifficulty = event.payload.difficulty,
                currentAnswers = event.payload.options.toBattleAnswerOptionUi(),
                selectedAnswerId = null,
                hasAnswered = false,
                isSubmittingAnswer = false,
                explanation = null
            )
        }
    }

    private fun hasMatchIdConflict(event: OnlineBattleEvent): Boolean {
        val activeMatchId = _state.value.matchId ?: return false
        val eventMatchId = eventMatchId(event) ?: return false
        return eventMatchId != activeMatchId
    }

    private fun eventMatchId(event: OnlineBattleEvent): String? = when (event) {
        is OnlineBattleEvent.RoomCreated -> event.matchId
        is OnlineBattleEvent.RoomJoined -> event.matchId
        is OnlineBattleEvent.MatchStarting -> event.matchId
        is OnlineBattleEvent.MatchStarted -> event.matchId
        is OnlineBattleEvent.QuestionPushed -> event.matchId
        is OnlineBattleEvent.AnswerResult -> event.matchId
        is OnlineBattleEvent.ScoreUpdated -> event.matchId
        is OnlineBattleEvent.TimerTick -> event.matchId
        is OnlineBattleEvent.OpponentConnectionChanged -> event.matchId
        is OnlineBattleEvent.MatchEnded -> event.matchId
        else -> null
    }

    /** Compares [route]'s `currentPlayerId` to [OnlineScoreBoard.hostPlayerId] to decide
     * which side is "player" vs "foe" — matches the original app's behavior;
     * [com.elkrrai.techtalk.presentation.battle.online.BattleLobbyViewModel] is
     * responsible for supplying it in [route]. */
    private fun withScoreBoard(scoreboard: OnlineScoreBoard) {
        val key = "${scoreboard.hostScore}:${scoreboard.guestScore}"
        if (key == lastScoreBoardKey) return
        lastScoreBoardKey = key

        val isHost = route.currentPlayerId == scoreboard.hostPlayerId
        val playerScore = if (isHost) scoreboard.hostScore else scoreboard.guestScore
        val foeScore = if (isHost) scoreboard.guestScore else scoreboard.hostScore

        _state.update { it.copy(playerScore = playerScore, foeScore = foeScore) }
    }

    @OptIn(ExperimentalTime::class)
    private fun startLocalCountdown(totalDurationSeconds: Int?, startedAtEpochMillis: Long) {
        countdownJob?.cancel()
        if (totalDurationSeconds == null) {
            _state.update { it.copy(remainingTimeSeconds = null) }
            return
        }
        countdownJob = viewModelScope.launch {
            while (true) {
                val elapsedSeconds = ((nowEpochMillis() - startedAtEpochMillis) / 1000).toInt()
                val remaining = (totalDurationSeconds - elapsedSeconds).coerceAtLeast(0)
                _state.update { it.copy(remainingTimeSeconds = remaining) }
                if (remaining <= 0) return@launch
                delay(1000)
            }
        }
    }

    fun onAnswerSelected(answerId: Long) {
        if (_state.value.hasAnswered) return
        _state.update { it.copy(selectedAnswerId = answerId) }
    }

    fun onSubmitAnswer() {
        val current = _state.value
        if (!current.canSubmitAnswer) return
        val matchId = current.matchId ?: return
        val questionId = current.currentQuestionId ?: return
        val selectedId = current.selectedAnswerId ?: return
        val originalAnswerId = answerIdByHash[selectedId] ?: return

        _state.update { it.copy(isSubmittingAnswer = true) }
        viewModelScope.launch {
            submitOnlineAnswer(
                SubmitOnlineAnswerRequest(
                    matchId = matchId,
                    questionId = questionId,
                    answerId = originalAnswerId,
                    clientSentAtEpochMillis = nowEpochMillis()
                )
            )
        }
    }

    fun onResign() {
        finalizeToResult(playerScore = _state.value.playerScore, isResigned = true)
    }

    /** Computes XP with [BattleProgression.battleXpForScore] and navigates to
     * [BattleResultRoute] (`canTryAgain = false` — online never offers a rematch). Also
     * persists via [persistResult] — unlike offline, [BattleStatus] is derived from the
     * head-to-head result, not the 50% rule. [hasPersistedResult] guards against
     * double-counting when a resign is followed by a server `MatchEnded`. */
    private fun finalizeToResult(playerScore: Int, isResigned: Boolean) {
        if (_state.value.isMatchEnded) return
        countdownJob?.cancel()
        _state.update { it.copy(isMatchEnded = true) }

        val foeScore = _state.value.foeScore
        val status = when {
            isResigned -> BattleStatus.RESIGNED
            playerScore > foeScore -> BattleStatus.WIN
            else -> BattleStatus.LOSS
        }
        val xpGained = BattleProgression.battleXpForScore(playerScore, BATTLE_TOTAL_QUESTIONS, isResigned)

        persistResult(playerScore, status, xpGained)

        viewModelScope.launch {
            _navigateToResult.send(
                BattleResultRoute(
                    score = playerScore,
                    totalQuestions = BATTLE_TOTAL_QUESTIONS,
                    technologyId = route.technologyId,
                    technologyName = route.technologyName,
                    playerName = route.playerName,
                    playerAvatarKey = route.playerAvatarKey,
                    xpGained = xpGained,
                    canTryAgain = false
                )
            )
        }
    }

    /** Both repository calls are wrapped in runCatching so a DB failure never blocks the
     * result screen. */
    private fun persistResult(score: Int, status: BattleStatus, xpGained: Int) {
        if (hasPersistedResult) return
        hasPersistedResult = true
        viewModelScope.launch {
            runCatching { repository.awardBattleXp(xpGained) }
            runCatching {
                repository.recordBattleResult(
                    technologyId = route.technologyId,
                    technologyName = route.technologyName,
                    score = score,
                    totalQuestions = BATTLE_TOTAL_QUESTIONS,
                    status = status,
                    xpGained = xpGained
                )
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
