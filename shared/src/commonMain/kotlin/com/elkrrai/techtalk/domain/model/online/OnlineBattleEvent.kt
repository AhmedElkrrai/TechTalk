package com.elkrrai.techtalk.domain.model.online

/**
 * 16-case sealed interface. [Disconnected] and [ConnectionHealthChanged] are
 * **client-originated** — the transport emits them locally; every other event maps
 * from a server frame.
 */
sealed interface OnlineBattleEvent {
    data class Connected(
        val playerId: String,
        val sessionId: String,
        val matchStartAtMillis: Long?
    ) : OnlineBattleEvent

    data object Disconnected : OnlineBattleEvent

    data class RoomCreated(
        val roomCode: String,
        val matchId: String,
        val expiresInSeconds: Int
    ) : OnlineBattleEvent

    data class RoomJoined(
        val roomCode: String,
        val matchId: String,
        val host: OnlinePlayer,
        val guest: OnlinePlayer?,
        val settings: OnlineMatchSettings
    ) : OnlineBattleEvent

    data class LobbyUpdated(
        val roomCode: String,
        val host: OnlinePlayer,
        val guest: OnlinePlayer?,
        val hostReady: Boolean,
        val guestReady: Boolean,
        val settings: OnlineMatchSettings
    ) : OnlineBattleEvent

    data class MatchStarting(
        val matchId: String,
        val startAtEpochMillis: Long,
        val countdownMillis: Long
    ) : OnlineBattleEvent

    data class MatchStarted(
        val matchId: String,
        val startedAtEpochMillis: Long,
        val totalDurationSeconds: Int?
    ) : OnlineBattleEvent

    data class QuestionPushed(
        val matchId: String,
        val questionIndex: Int,
        val payload: OnlineQuestionPayload
    ) : OnlineBattleEvent

    data class AnswerResult(
        val matchId: String,
        val questionId: String,
        val isCorrect: Boolean,
        val gainedPoints: Int,
        val scoreboard: OnlineScoreBoard
    ) : OnlineBattleEvent

    data class ScoreUpdated(
        val matchId: String,
        val scoreboard: OnlineScoreBoard
    ) : OnlineBattleEvent

    data class TimerTick(
        val matchId: String,
        val remainingSeconds: Int
    ) : OnlineBattleEvent

    data class OpponentConnectionChanged(
        val matchId: String,
        val isConnected: Boolean,
        val reconnectGraceSeconds: Int?
    ) : OnlineBattleEvent

    data class ConnectionHealthChanged(
        val isHealthy: Boolean,
        val latencyMillis: Long?
    ) : OnlineBattleEvent

    /** Null [winnerPlayerId] means a draw, not an error. */
    data class MatchEnded(
        val matchId: String,
        val winnerPlayerId: String?,
        val endReason: OnlineMatchEndReason,
        val scoreboard: OnlineScoreBoard
    ) : OnlineBattleEvent

    data class RoomExpired(
        val roomCode: String,
        val reason: String
    ) : OnlineBattleEvent

    data class Failure(
        val code: FailureCode,
        val message: String
    ) : OnlineBattleEvent
}
