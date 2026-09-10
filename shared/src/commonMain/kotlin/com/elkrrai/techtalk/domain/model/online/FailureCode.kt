package com.elkrrai.techtalk.domain.model.online

/**
 * [value] is the wire string used by the server. [MissingMatchId] and [InvalidPhase]
 * are declared for server-side/validation use and are not emitted by the current
 * client transport.
 */
enum class FailureCode(val value: String) {
    SocketReceiveError("SOCKET_RECEIVE_ERROR"),
    PingSendFailed("PING_SEND_FAILED"),
    ProtocolDecodeError("PROTOCOL_DECODE_ERROR"),
    SocketNotConnected("SOCKET_NOT_CONNECTED"),
    ReconnectExhausted("RECONNECT_EXHAUSTED"),
    MissingMatchId("MISSING_MATCH_ID"),
    InvalidPhase("INVALID_PHASE")
}
