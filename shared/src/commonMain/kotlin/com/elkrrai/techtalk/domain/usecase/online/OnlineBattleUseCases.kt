package com.elkrrai.techtalk.domain.usecase.online

import com.elkrrai.techtalk.domain.model.online.JoinOnlineRoomRequest
import com.elkrrai.techtalk.domain.model.online.LeaveOnlineRoomRequest
import com.elkrrai.techtalk.domain.model.online.OnlineBattleEvent
import com.elkrrai.techtalk.domain.model.online.OnlineMatchSettings
import com.elkrrai.techtalk.domain.model.online.ReconnectOnlineBattleRequest
import com.elkrrai.techtalk.domain.model.online.RequestOnlineRematchRequest
import com.elkrrai.techtalk.domain.model.online.SetOnlinePlayerReadyRequest
import com.elkrrai.techtalk.domain.model.online.SubmitOnlineAnswerRequest
import com.elkrrai.techtalk.domain.repository.OnlineBattleRepository
import kotlinx.coroutines.flow.Flow

/**
 * Ten thin pass-through wrappers around [OnlineBattleRepository]. The seam exists so
 * orchestration (validation, retry, analytics) can be added later without touching
 * ViewModels. There are no use cases for [com.elkrrai.techtalk.domain.repository
 * .TechTalkRepository] — the presentation layer injects it directly; keep that
 * asymmetry deliberate.
 */
class ObserveOnlineBattleEventsUseCase(private val repository: OnlineBattleRepository) {
    operator fun invoke(): Flow<OnlineBattleEvent> = repository.events
}

class ConnectOnlineBattleUseCase(private val repository: OnlineBattleRepository) {
    suspend operator fun invoke(playerName: String) = repository.connect(playerName)
}

class DisconnectOnlineBattleUseCase(private val repository: OnlineBattleRepository) {
    suspend operator fun invoke() = repository.disconnect()
}

class CreateOnlineRoomUseCase(private val repository: OnlineBattleRepository) {
    suspend operator fun invoke(settings: OnlineMatchSettings) = repository.createRoom(settings)
}

class JoinOnlineRoomUseCase(private val repository: OnlineBattleRepository) {
    suspend operator fun invoke(request: JoinOnlineRoomRequest) = repository.joinRoom(request)
}

class SetOnlinePlayerReadyUseCase(private val repository: OnlineBattleRepository) {
    suspend operator fun invoke(request: SetOnlinePlayerReadyRequest) = repository.setPlayerReady(request)
}

class SubmitOnlineAnswerUseCase(private val repository: OnlineBattleRepository) {
    suspend operator fun invoke(request: SubmitOnlineAnswerRequest) = repository.submitAnswer(request)
}

class RequestOnlineRematchUseCase(private val repository: OnlineBattleRepository) {
    suspend operator fun invoke(request: RequestOnlineRematchRequest) = repository.requestRematch(request)
}

class LeaveOnlineRoomUseCase(private val repository: OnlineBattleRepository) {
    suspend operator fun invoke(request: LeaveOnlineRoomRequest) = repository.leaveRoom(request)
}

class ReconnectOnlineBattleUseCase(private val repository: OnlineBattleRepository) {
    suspend operator fun invoke(request: ReconnectOnlineBattleRequest) = repository.reconnect(request)
}
