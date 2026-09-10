package com.elkrrai.techtalk.domain.model.online

import com.elkrrai.techtalk.domain.model.common.Difficulty

/** Carries both [technologyId] and [technologyName] so the opponent can display it
 * without a local lookup. */
data class OnlineMatchSettings(
    val timeControl: BattleTimeControl,
    val difficulty: Difficulty,
    val technologyId: Long,
    val technologyName: String
)

data class JoinOnlineRoomRequest(val roomCode: String)

data class SetOnlinePlayerReadyRequest(val matchId: String, val isReady: Boolean)

data class SubmitOnlineAnswerRequest(
    val matchId: String,
    val questionId: String,
    val answerId: String,
    val clientSentAtEpochMillis: Long
)

data class RequestOnlineRematchRequest(val matchId: String)

/** Null [matchId] means "leave whatever match is active". */
data class LeaveOnlineRoomRequest(val matchId: String? = null)

data class ReconnectOnlineBattleRequest(
    val matchId: String,
    val playerId: String,
    val playerToken: String
)
