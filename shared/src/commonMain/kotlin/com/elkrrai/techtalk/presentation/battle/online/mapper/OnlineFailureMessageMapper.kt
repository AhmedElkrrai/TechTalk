package com.elkrrai.techtalk.presentation.battle.online.mapper

import com.elkrrai.techtalk.domain.model.online.FailureCode

fun mapOnlineFailureToUserMessage(code: FailureCode, fallbackMessage: String): String {
    val friendly = when (code) {
        FailureCode.SocketReceiveError -> "Connection error — please try again."
        FailureCode.PingSendFailed -> "Connection is unstable."
        FailureCode.ProtocolDecodeError -> "Received an unexpected message from the server."
        FailureCode.SocketNotConnected -> "Not connected to the server."
        FailureCode.ReconnectExhausted -> "Couldn't reconnect — check your connection and try again."
        FailureCode.MissingMatchId -> "This match is no longer available."
        FailureCode.InvalidPhase -> "That action isn't available right now."
    }
    return if (fallbackMessage.isNotBlank()) "$friendly ($fallbackMessage)" else friendly
}
