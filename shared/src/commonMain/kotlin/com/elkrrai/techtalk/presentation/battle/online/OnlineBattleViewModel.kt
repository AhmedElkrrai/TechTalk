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
import com.elkrrai.techtalk.presentation.battle.online.mapper.mapOnlineFailureToUserMessage
import com.elkrrai.techtalk.presentation.battle.online.mapper.toBattleAnswerOptionUi
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineBattleUiState
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineConnectionStatus
import com.elkrrai.techtalk.presentation.battle.state.BattlePhase
import com.elkrrai.techtalk.presentation.battle.state.BattleSessionStore
import com.elkrrai.techtalk.presentation.battle.state.BattleState
import com.elkrrai.techtalk.presentation.battle.state.OnlineBattlePhase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Four constructor args: [sessionStore], [repository], [observeOnlineBattleEvents],
 * [submitOnlineAnswer]. Ignores everything once [OnlineBattleUiState.isMatchEnded];
 * drops events whose matchId conflicts with the active one. */
class OnlineBattleViewModel(
    private val sessionStore: BattleSessionStore,
    private val repository: TechTalkRepository,
    observeOnlineBattleEvents: ObserveOnlineBattleEventsUseCase,
    private val submitOnlineAnswer: SubmitOnlineAnswerUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnlineBattleUiState())
    val state: StateFlow<OnlineBattleUiState> = _state.asStateFlow()

    // Client-side dedupe on top of the transport's own eventId dedupe.
    private var lastQuestionEventKey: String? = null
    private var lastScoreBoardKey: String? = null

    // Maps the shared UI component's Long id (derived from a hash) back to the
    // server's original String answerId, so the right one gets submitted.
    private var answerIdByHash: Map<Long, String> = emptyMap()

    private var hasPersistedResult = false
    private var countdownJob: Job? = null

    init {
        val session = sessionStore.state.value
        _state.update {
            it.copy(
                playerName = session.playerName,
                playerAvatarKey = session.playerAvatarKey,
                connectionStatus = OnlineConnectionStatus.CONNECTED
            )
        }
        viewModelScope.launch {
            observeOnlineBattleEvents().collect { event -> handleEvent(event) }
        }
    }

    private fun handleEvent(event: OnlineBattleEvent) {
        if (_state.value.isMatchEnded) return
        if (hasMatchIdConflict(event)) return

        when (event) {
            is OnlineBattleEvent.MatchStarted -> {
                hasPersistedResult = false
                _state.update { it.copy(matchId = event.matchId, isMatchEnded = false) }
                startLocalCountdown(event.totalDurationSeconds, event.startedAtEpochMillis)
            }

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

            // Lobby-only events, irrelevant once on the match screen.
            is OnlineBattleEvent.Connected,
            is OnlineBattleEvent.RoomCreated,
            is OnlineBattleEvent.RoomJoined,
            is OnlineBattleEvent.LobbyUpdated,
            is OnlineBattleEvent.MatchStarting -> Unit
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

    /** Compares `currentPlayerId` to [OnlineScoreBoard.hostPlayerId] to decide which
     * side is "player" vs "foe". If `currentPlayerId` was never set, this treats the
     * user as the guest, silently swapping scores — matches the original app's known
     * behavior; [com.elkrrai.techtalk.presentation.battle.online.BattleLobbyViewModel]
     * is responsible for populating it on `Connected`. */
    private fun withScoreBoard(scoreboard: OnlineScoreBoard) {
        val key = "${scoreboard.hostScore}:${scoreboard.guestScore}"
        if (key == lastScoreBoardKey) return
        lastScoreBoardKey = key

        val currentPlayerId = sessionStore.state.value.currentPlayerId
        val isHost = currentPlayerId != null && currentPlayerId == scoreboard.hostPlayerId
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

    /** Computes XP with [BattleProgression.battleXpForScore] and pushes the session to
     * RESULT with `onlinePhase = LOBBY`. Also persists via [persistResult] — unlike
     * offline, [BattleStatus] is derived from the head-to-head result, not the 50%
     * rule. [hasPersistedResult] guards against double-counting when a resign is
     * followed by a server `MatchEnded`; it's reset on `MatchStarted`. */
    private fun finalizeToResult(playerScore: Int, isResigned: Boolean) {
        if (_state.value.isMatchEnded) return
        countdownJob?.cancel()
        _state.update { it.copy(isMatchEnded = true) }

        val session = sessionStore.state.value
        val totalQuestions = session.totalQuestions
        val foeScore = _state.value.foeScore
        val status = when {
            isResigned -> BattleStatus.RESIGNED
            playerScore > foeScore -> BattleStatus.WIN
            else -> BattleStatus.LOSS
        }
        val xpGained = BattleProgression.battleXpForScore(playerScore, totalQuestions, isResigned)

        persistResult(session, playerScore, totalQuestions, status, xpGained)

        sessionStore.update {
            it.copy(
                phase = BattlePhase.RESULT,
                onlinePhase = OnlineBattlePhase.LOBBY,
                score = playerScore,
                xpGained = xpGained
            )
        }
    }

    /** Persistence is skipped when no technology is selected, and both repository
     * calls are wrapped in runCatching so a DB failure never blocks the result screen. */
    private fun persistResult(
        session: BattleState,
        score: Int,
        totalQuestions: Int,
        status: BattleStatus,
        xpGained: Int
    ) {
        if (hasPersistedResult) return
        val technology = session.selectedTechnology ?: return
        hasPersistedResult = true
        viewModelScope.launch {
            runCatching { repository.awardBattleXp(xpGained) }
            runCatching {
                repository.recordBattleResult(
                    technologyId = technology.id,
                    technologyName = technology.name,
                    score = score,
                    totalQuestions = totalQuestions,
                    status = status,
                    xpGained = xpGained
                )
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
