package com.elkrrai.techtalk.data.remote.dto

import kotlinx.serialization.Serializable

const val ONLINE_PROTOCOL_VERSION = 1

@Serializable
data class OutgoingOnlineMessageDto(
    val protocolVersion: Int = ONLINE_PROTOCOL_VERSION,
    val requestId: String,
    val matchId: String? = null,
    val body: OnlineClientCommandDto
)

@Serializable
data class IncomingOnlineMessageDto(
    val protocolVersion: Int,
    val eventId: String,
    val matchId: String? = null,
    val sentAtEpochMillis: Long,
    val body: OnlineServerEventDto
)
