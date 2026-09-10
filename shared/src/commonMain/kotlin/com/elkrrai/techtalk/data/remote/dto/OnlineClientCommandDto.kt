package com.elkrrai.techtalk.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface OnlineClientCommandDto {

    @Serializable
    @SerialName("connect")
    data class Connect(val playerName: String) : OnlineClientCommandDto

    @Serializable
    @SerialName("create_room")
    data class CreateRoom(val settings: OnlineMatchSettingsDto) : OnlineClientCommandDto

    @Serializable
    @SerialName("join_room")
    data class JoinRoom(val roomCode: String) : OnlineClientCommandDto

    @Serializable
    @SerialName("set_ready")
    data class SetReady(val matchId: String, val isReady: Boolean) : OnlineClientCommandDto

    @Serializable
    @SerialName("submit_answer")
    data class SubmitAnswer(
        val matchId: String,
        val questionId: String,
        val answerId: String,
        val clientSentAtEpochMillis: Long
    ) : OnlineClientCommandDto

    @Serializable
    @SerialName("request_rematch")
    data class RequestRematch(val matchId: String) : OnlineClientCommandDto

    @Serializable
    @SerialName("leave_room")
    data class LeaveRoom(val matchId: String? = null) : OnlineClientCommandDto

    @Serializable
    @SerialName("reconnect")
    data class Reconnect(
        val matchId: String,
        val playerId: String,
        val playerToken: String
    ) : OnlineClientCommandDto

    @Serializable
    @SerialName("ping")
    data object Ping : OnlineClientCommandDto

    @Serializable
    @SerialName("ack")
    data class Ack(val eventId: String) : OnlineClientCommandDto
}
