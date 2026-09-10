package com.elkrrai.techtalk.data.remote.mapper

import com.elkrrai.techtalk.data.remote.dto.IncomingOnlineMessageDto
import com.elkrrai.techtalk.data.remote.dto.OnlineAnswerOptionDto
import com.elkrrai.techtalk.data.remote.dto.OnlineClientCommandDto
import com.elkrrai.techtalk.data.remote.dto.OnlineMatchSettingsDto
import com.elkrrai.techtalk.data.remote.dto.OnlinePlayerDto
import com.elkrrai.techtalk.data.remote.dto.OnlineQuestionPayloadDto
import com.elkrrai.techtalk.data.remote.dto.OnlineScoreBoardDto
import com.elkrrai.techtalk.data.remote.dto.OnlineServerEventDto
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.online.BattleTimeControl
import com.elkrrai.techtalk.domain.model.online.FailureCode
import com.elkrrai.techtalk.domain.model.online.JoinOnlineRoomRequest
import com.elkrrai.techtalk.domain.model.online.LeaveOnlineRoomRequest
import com.elkrrai.techtalk.domain.model.online.OnlineAnswerOption
import com.elkrrai.techtalk.domain.model.online.OnlineBattleEvent
import com.elkrrai.techtalk.domain.model.online.OnlineMatchEndReason
import com.elkrrai.techtalk.domain.model.online.OnlineMatchSettings
import com.elkrrai.techtalk.domain.model.online.OnlinePlayer
import com.elkrrai.techtalk.domain.model.online.OnlineQuestionPayload
import com.elkrrai.techtalk.domain.model.online.OnlineScoreBoard
import com.elkrrai.techtalk.domain.model.online.ReconnectOnlineBattleRequest
import com.elkrrai.techtalk.domain.model.online.RequestOnlineRematchRequest
import com.elkrrai.techtalk.domain.model.online.SetOnlinePlayerReadyRequest
import com.elkrrai.techtalk.domain.model.online.SubmitOnlineAnswerRequest

// domain request -> command DTO

fun buildConnectCommand(playerName: String): OnlineClientCommandDto =
    OnlineClientCommandDto.Connect(playerName)

fun buildHeartbeatPing(): OnlineClientCommandDto = OnlineClientCommandDto.Ping

fun OnlineMatchSettings.toCreateRoomCommand(): OnlineClientCommandDto =
    OnlineClientCommandDto.CreateRoom(toDto())

fun JoinOnlineRoomRequest.toJoinRoomCommand(): OnlineClientCommandDto =
    OnlineClientCommandDto.JoinRoom(roomCode)

fun SetOnlinePlayerReadyRequest.toSetReadyCommand(): OnlineClientCommandDto =
    OnlineClientCommandDto.SetReady(matchId, isReady)

fun SubmitOnlineAnswerRequest.toSubmitAnswerCommand(): OnlineClientCommandDto =
    OnlineClientCommandDto.SubmitAnswer(matchId, questionId, answerId, clientSentAtEpochMillis)

fun RequestOnlineRematchRequest.toRematchCommand(): OnlineClientCommandDto =
    OnlineClientCommandDto.RequestRematch(matchId)

fun LeaveOnlineRoomRequest.toLeaveRoomCommand(): OnlineClientCommandDto =
    OnlineClientCommandDto.LeaveRoom(matchId)

fun ReconnectOnlineBattleRequest.toReconnectCommand(): OnlineClientCommandDto =
    OnlineClientCommandDto.Reconnect(matchId, playerId, playerToken)

private fun OnlineMatchSettings.toDto(): OnlineMatchSettingsDto = OnlineMatchSettingsDto(
    timeControl = timeControl.name,
    difficulty = difficulty.name,
    technologyId = technologyId,
    technologyName = technologyName
)

// wire payload DTOs -> domain

private fun OnlineMatchSettingsDto.toDomain(): OnlineMatchSettings = OnlineMatchSettings(
    timeControl = runCatching { BattleTimeControl.valueOf(timeControl) }.getOrDefault(BattleTimeControl.ONE_MINUTE),
    difficulty = runCatching { Difficulty.valueOf(difficulty) }.getOrDefault(Difficulty.RANDOM),
    technologyId = technologyId,
    technologyName = technologyName
)

private fun OnlinePlayerDto.toDomain(): OnlinePlayer = OnlinePlayer(playerId, displayName)

private fun OnlineScoreBoardDto.toDomain(): OnlineScoreBoard =
    OnlineScoreBoard(hostScore, guestScore, hostPlayerId, guestPlayerId)

private fun OnlineAnswerOptionDto.toDomain(): OnlineAnswerOption = OnlineAnswerOption(answerId, text)

private fun OnlineQuestionPayloadDto.toDomain(): OnlineQuestionPayload = OnlineQuestionPayload(
    questionId = questionId,
    prompt = prompt,
    difficulty = runCatching { Difficulty.valueOf(difficulty) }.getOrDefault(Difficulty.BEGINNER),
    options = options.map { it.toDomain() }
)

/**
 * Maps a decoded server frame to a domain event. Returns null for `pong` and `ack`
 * frames — those are transport-internal plumbing (heartbeat tracking, at-least-once
 * delivery bookkeeping) that [com.elkrrai.techtalk.data.remote.KtorOnlineBattleRepository]
 * handles directly rather than surfacing to the UI.
 */
fun IncomingOnlineMessageDto.toDomainEvent(nowEpochMillis: Long): OnlineBattleEvent? =
    when (val body = body) {
        is OnlineServerEventDto.Connected ->
            OnlineBattleEvent.Connected(body.playerId, body.sessionId, body.matchStartAtMillis)

        is OnlineServerEventDto.RoomCreated ->
            OnlineBattleEvent.RoomCreated(body.roomCode, body.matchId, body.expiresInSeconds)

        is OnlineServerEventDto.RoomJoined ->
            OnlineBattleEvent.RoomJoined(
                body.roomCode,
                body.matchId,
                body.host.toDomain(),
                body.guest?.toDomain(),
                body.settings.toDomain()
            )

        is OnlineServerEventDto.LobbyUpdated ->
            OnlineBattleEvent.LobbyUpdated(
                body.roomCode,
                body.host.toDomain(),
                body.guest?.toDomain(),
                body.hostReady,
                body.guestReady,
                body.settings.toDomain()
            )

        is OnlineServerEventDto.MatchStarting ->
            OnlineBattleEvent.MatchStarting(body.matchId, body.startAtEpochMillis, body.countdownMillis)

        is OnlineServerEventDto.MatchStarted ->
            OnlineBattleEvent.MatchStarted(body.matchId, body.startedAtEpochMillis, body.totalDurationSeconds)

        is OnlineServerEventDto.QuestionPush ->
            OnlineBattleEvent.QuestionPushed(body.matchId, body.questionIndex, body.payload.toDomain())

        is OnlineServerEventDto.AnswerResult ->
            OnlineBattleEvent.AnswerResult(
                body.matchId,
                body.questionId,
                body.isCorrect,
                body.gainedPoints,
                body.scoreboard.toDomain()
            )

        is OnlineServerEventDto.ScoreUpdated ->
            OnlineBattleEvent.ScoreUpdated(body.matchId, body.scoreboard.toDomain())

        is OnlineServerEventDto.TimerTick ->
            OnlineBattleEvent.TimerTick(body.matchId, body.remainingSeconds)

        is OnlineServerEventDto.OpponentConnectionChanged ->
            OnlineBattleEvent.OpponentConnectionChanged(body.matchId, body.isConnected, body.reconnectGraceSeconds)

        is OnlineServerEventDto.MatchEnded ->
            OnlineBattleEvent.MatchEnded(
                body.matchId,
                body.winnerPlayerId,
                runCatching { OnlineMatchEndReason.valueOf(body.endReason) }.getOrDefault(OnlineMatchEndReason.UNKNOWN),
                body.scoreboard.toDomain()
            )

        is OnlineServerEventDto.RoomExpired ->
            OnlineBattleEvent.RoomExpired(body.roomCode, body.reason)

        is OnlineServerEventDto.Failure ->
            OnlineBattleEvent.Failure(
                code = FailureCode.entries.firstOrNull { it.value == body.code } ?: FailureCode.ProtocolDecodeError,
                message = body.message
            )

        is OnlineServerEventDto.Pong -> null
        is OnlineServerEventDto.Ack -> null
    }
