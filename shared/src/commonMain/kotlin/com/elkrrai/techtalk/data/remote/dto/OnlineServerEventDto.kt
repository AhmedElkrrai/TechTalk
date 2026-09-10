package com.elkrrai.techtalk.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface OnlineServerEventDto {

    @Serializable
    @SerialName("connected")
    data class Connected(
        val playerId: String,
        val sessionId: String,
        val matchStartAtMillis: Long? = null
    ) : OnlineServerEventDto

    @Serializable
    @SerialName("room_created")
    data class RoomCreated(
        val roomCode: String,
        val matchId: String,
        val expiresInSeconds: Int
    ) : OnlineServerEventDto

    @Serializable
    @SerialName("room_joined")
    data class RoomJoined(
        val roomCode: String,
        val matchId: String,
        val host: OnlinePlayerDto,
        val guest: OnlinePlayerDto? = null,
        val settings: OnlineMatchSettingsDto
    ) : OnlineServerEventDto

    @Serializable
    @SerialName("lobby_updated")
    data class LobbyUpdated(
        val roomCode: String,
        val host: OnlinePlayerDto,
        val guest: OnlinePlayerDto? = null,
        val hostReady: Boolean,
        val guestReady: Boolean,
        val settings: OnlineMatchSettingsDto
    ) : OnlineServerEventDto

    @Serializable
    @SerialName("match_starting")
    data class MatchStarting(
        val matchId: String,
        val startAtEpochMillis: Long,
        val countdownMillis: Long
    ) : OnlineServerEventDto

    @Serializable
    @SerialName("match_started")
    data class MatchStarted(
        val matchId: String,
        val startedAtEpochMillis: Long,
        val totalDurationSeconds: Int? = null
    ) : OnlineServerEventDto

    @Serializable
    @SerialName("question_push")
    data class QuestionPush(
        val matchId: String,
        val questionIndex: Int,
        val payload: OnlineQuestionPayloadDto
    ) : OnlineServerEventDto

    @Serializable
    @SerialName("answer_result")
    data class AnswerResult(
        val matchId: String,
        val questionId: String,
        val isCorrect: Boolean,
        val gainedPoints: Int,
        val scoreboard: OnlineScoreBoardDto
    ) : OnlineServerEventDto

    @Serializable
    @SerialName("score_updated")
    data class ScoreUpdated(
        val matchId: String,
        val scoreboard: OnlineScoreBoardDto
    ) : OnlineServerEventDto

    @Serializable
    @SerialName("timer_tick")
    data class TimerTick(
        val matchId: String,
        val remainingSeconds: Int
    ) : OnlineServerEventDto

    @Serializable
    @SerialName("opponent_connection_changed")
    data class OpponentConnectionChanged(
        val matchId: String,
        val isConnected: Boolean,
        val reconnectGraceSeconds: Int? = null
    ) : OnlineServerEventDto

    @Serializable
    @SerialName("match_ended")
    data class MatchEnded(
        val matchId: String,
        val winnerPlayerId: String? = null,
        val endReason: String,
        val scoreboard: OnlineScoreBoardDto
    ) : OnlineServerEventDto

    @Serializable
    @SerialName("room_expired")
    data class RoomExpired(
        val roomCode: String,
        val reason: String
    ) : OnlineServerEventDto

    @Serializable
    @SerialName("pong")
    data class Pong(val serverTimeEpochMillis: Long = 0) : OnlineServerEventDto

    @Serializable
    @SerialName("failure")
    data class Failure(
        val code: String,
        val message: String
    ) : OnlineServerEventDto

    @Serializable
    @SerialName("ack")
    data class Ack(val eventId: String) : OnlineServerEventDto
}
