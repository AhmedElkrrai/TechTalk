package com.elkrrai.techtalk.domain.repository

import com.elkrrai.techtalk.domain.model.online.JoinOnlineRoomRequest
import com.elkrrai.techtalk.domain.model.online.LeaveOnlineRoomRequest
import com.elkrrai.techtalk.domain.model.online.OnlineBattleEvent
import com.elkrrai.techtalk.domain.model.online.OnlineMatchSettings
import com.elkrrai.techtalk.domain.model.online.ReconnectOnlineBattleRequest
import com.elkrrai.techtalk.domain.model.online.RequestOnlineRematchRequest
import com.elkrrai.techtalk.domain.model.online.SetOnlinePlayerReadyRequest
import com.elkrrai.techtalk.domain.model.online.SubmitOnlineAnswerRequest
import kotlinx.coroutines.flow.Flow

/**
 * The transport contract. Fire-and-forget by design: every command returns [Unit]; all
 * results — including errors — arrive asynchronously on [events]. Callers must never
 * try/catch for protocol outcomes; they observe [OnlineBattleEvent.Failure] instead.
 */
interface OnlineBattleRepository {
    val events: Flow<OnlineBattleEvent>

    suspend fun connect(playerName: String)
    suspend fun disconnect()
    suspend fun createRoom(settings: OnlineMatchSettings)
    suspend fun joinRoom(request: JoinOnlineRoomRequest)
    suspend fun setPlayerReady(request: SetOnlinePlayerReadyRequest)
    suspend fun submitAnswer(request: SubmitOnlineAnswerRequest)
    suspend fun requestRematch(request: RequestOnlineRematchRequest)
    suspend fun leaveRoom(request: LeaveOnlineRoomRequest)
    suspend fun reconnect(request: ReconnectOnlineBattleRequest)
}
