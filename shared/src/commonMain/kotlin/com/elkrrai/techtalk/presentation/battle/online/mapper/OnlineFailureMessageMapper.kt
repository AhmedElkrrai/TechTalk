package com.elkrrai.techtalk.presentation.battle.online.mapper

import com.elkrrai.techtalk.domain.model.online.FailureCode

/** [code] alone always produces a complete, user-facing sentence — never append the
 * raw underlying exception message (coroutine/SDK internals like "StandaloneCoroutine
 * was cancelled" are meaningless, and occasionally alarming, to someone playing a
 * battle) — see the [com.elkrrai.techtalk.domain.model.online.OnlineBattleEvent.Failure]
 * event this maps, whose own `message` field stays for debugging/logging only. */
fun mapOnlineFailureToUserMessage(code: FailureCode): String = when (code) {
    FailureCode.SocketReceiveError -> "Connection error — please try again."
    FailureCode.PingSendFailed -> "Connection is unstable."
    FailureCode.ProtocolDecodeError -> "Received an unexpected message from the server."
    FailureCode.SocketNotConnected -> "Not connected to the server."
    FailureCode.ReconnectExhausted -> "Couldn't reconnect — check your connection and try again."
    FailureCode.MissingMatchId -> "This match is no longer available."
    FailureCode.InvalidPhase -> "That action isn't available right now."
}
