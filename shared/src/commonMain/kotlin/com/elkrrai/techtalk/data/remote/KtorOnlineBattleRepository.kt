package com.elkrrai.techtalk.data.remote

import com.elkrrai.techtalk.data.remote.dto.IncomingOnlineMessageDto
import com.elkrrai.techtalk.data.remote.dto.ONLINE_PROTOCOL_VERSION
import com.elkrrai.techtalk.data.remote.dto.OnlineClientCommandDto
import com.elkrrai.techtalk.data.remote.dto.OnlineServerEventDto
import com.elkrrai.techtalk.data.remote.dto.OnlineTransportConfig
import com.elkrrai.techtalk.data.remote.dto.OutgoingOnlineMessageDto
import com.elkrrai.techtalk.data.remote.mapper.buildConnectCommand
import com.elkrrai.techtalk.data.remote.mapper.buildHeartbeatPing
import com.elkrrai.techtalk.data.remote.mapper.toCreateRoomCommand
import com.elkrrai.techtalk.data.remote.mapper.toDomainEvent
import com.elkrrai.techtalk.data.remote.mapper.toJoinRoomCommand
import com.elkrrai.techtalk.data.remote.mapper.toLeaveRoomCommand
import com.elkrrai.techtalk.data.remote.mapper.toRematchCommand
import com.elkrrai.techtalk.data.remote.mapper.toReconnectCommand
import com.elkrrai.techtalk.data.remote.mapper.toSetReadyCommand
import com.elkrrai.techtalk.data.remote.mapper.toSubmitAnswerCommand
import com.elkrrai.techtalk.data.remote.serialization.OnlineSocketJson
import com.elkrrai.techtalk.data.utils.currentEpochMillis
import com.elkrrai.techtalk.domain.model.online.BattleTimeControl
import com.elkrrai.techtalk.domain.model.online.FailureCode
import com.elkrrai.techtalk.domain.model.online.JoinOnlineRoomRequest
import com.elkrrai.techtalk.domain.model.online.LeaveOnlineRoomRequest
import com.elkrrai.techtalk.domain.model.online.OnlineBattleEvent
import com.elkrrai.techtalk.domain.model.online.OnlineMatchSettings
import com.elkrrai.techtalk.domain.model.online.ReconnectOnlineBattleRequest
import com.elkrrai.techtalk.domain.model.online.RequestOnlineRematchRequest
import com.elkrrai.techtalk.domain.model.online.SetOnlinePlayerReadyRequest
import com.elkrrai.techtalk.domain.model.online.SubmitOnlineAnswerRequest
import com.elkrrai.techtalk.domain.repository.OnlineBattleRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val DEFAULT_HEARTBEAT_INTERVAL_MS = 5_000L
private const val MAX_SEEN_EVENT_IDS = 256
private const val MAX_RECONNECT_ATTEMPTS = 3

/**
 * WebSocket implementation of [OnlineBattleRepository]. Fire-and-forget: every command
 * suspend function just sends; all results — including errors — arrive later on
 * [events]. Call [clear] when the app/session tears down to cancel the internal scope.
 */
class KtorOnlineBattleRepository(
    private val httpClient: HttpClient,
    private val transportConfig: OnlineTransportConfig = OnlineTransportConfig()
) : OnlineBattleRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _events = MutableSharedFlow<OnlineBattleEvent>(extraBufferCapacity = 64)
    override val events: Flow<OnlineBattleEvent> = _events

    private var session: DefaultClientWebSocketSession? = null
    private var receiverJob: Job? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    private var playerName: String = "Player"
    private var currentMatchId: String? = null
    private var currentTimeControl: BattleTimeControl? = null
    private var lastPongAtEpochMillis: Long = currentEpochMillis()
    private var manuallyDisconnected = false
    private var pendingReconnectCommand: OnlineClientCommandDto? = null

    // LRU-ish dedupe of inbound eventIds.
    private val seenEventIds = ArrayDeque<String>()
    private val seenEventIdSet = mutableSetOf<String>()

    override suspend fun connect(playerName: String) {
        this.playerName = playerName
        manuallyDisconnected = false
        reconnectJob?.cancel()
        reconnectJob = null
        closeSocketQuietly()
        establishSession()
    }

    override suspend fun disconnect() {
        manuallyDisconnected = true
        reconnectJob?.cancel()
        reconnectJob = null
        closeSocketQuietly()
        _events.emit(OnlineBattleEvent.Disconnected)
    }

    override suspend fun createRoom(settings: OnlineMatchSettings) {
        currentTimeControl = settings.timeControl
        sendCommand(settings.toCreateRoomCommand())
    }

    override suspend fun joinRoom(request: JoinOnlineRoomRequest) {
        sendCommand(request.toJoinRoomCommand())
    }

    override suspend fun setPlayerReady(request: SetOnlinePlayerReadyRequest) {
        sendCommand(request.toSetReadyCommand())
    }

    override suspend fun submitAnswer(request: SubmitOnlineAnswerRequest) {
        sendCommand(request.toSubmitAnswerCommand())
    }

    override suspend fun requestRematch(request: RequestOnlineRematchRequest) {
        sendCommand(request.toRematchCommand())
    }

    override suspend fun leaveRoom(request: LeaveOnlineRoomRequest) {
        sendCommand(request.toLeaveRoomCommand())
    }

    override suspend fun reconnect(request: ReconnectOnlineBattleRequest) {
        val command = request.toReconnectCommand()
        pendingReconnectCommand = command
        sendCommand(command)
    }

    /** Cancels the internal scope — call when the app/session tears down. */
    fun clear() {
        scope.cancel()
    }

    private suspend fun establishSession(): Boolean {
        val newSession = runCatching {
            httpClient.webSocketSession { url(transportConfig.serverUrl) }
        }.getOrElse {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketNotConnected, it.message ?: "Failed to connect"))
            return false
        }
        session = newSession
        lastPongAtEpochMillis = currentEpochMillis()
        startReceiverLoop(newSession)
        startHeartbeatLoop()
        sendCommand(buildConnectCommand(playerName))
        pendingReconnectCommand?.let { sendCommand(it) }
        return true
    }

    private suspend fun closeSocketQuietly() {
        receiverJob?.cancel()
        receiverJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        runCatching { session?.close() }
        session = null
    }

    private suspend fun sendCommand(command: OnlineClientCommandDto, matchId: String? = currentMatchId) {
        val activeSession = session
        if (activeSession == null) {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketNotConnected, "Not connected"))
            return
        }
        val message = OutgoingOnlineMessageDto(requestId = newRequestId(), matchId = matchId, body = command)
        runCatching {
            activeSession.send(OnlineSocketJson.instance.encodeToString(OutgoingOnlineMessageDto.serializer(), message))
        }.onFailure {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, it.message ?: "Send failed"))
        }
    }

    private fun newRequestId(): String =
        "rq_" + currentEpochMillis().toString(36) + Random.nextLong().toString(36)

    private fun startReceiverLoop(activeSession: DefaultClientWebSocketSession) {
        receiverJob = scope.launch {
            try {
                for (frame in activeSession.incoming) {
                    if (frame !is Frame.Text) continue
                    handleIncomingText(frame.readText())
                }
            } catch (e: Exception) {
                _events.emit(OnlineBattleEvent.Failure(FailureCode.SocketReceiveError, e.message ?: "Receive failed"))
                scheduleReconnect()
            }
        }
    }

    /**
     * decode -> reject mismatched protocolVersion -> drop duplicate eventId -> drop
     * events from a foreign matchId -> update currentMatchId -> refresh
     * lastPongAtEpochMillis on pong -> map to domain -> emit -> always ack (except for
     * incoming ack).
     */
    private suspend fun handleIncomingText(text: String) {
        val message = runCatching {
            OnlineSocketJson.instance.decodeFromString(IncomingOnlineMessageDto.serializer(), text)
        }.getOrElse {
            _events.emit(OnlineBattleEvent.Failure(FailureCode.ProtocolDecodeError, it.message ?: "Decode failed"))
            return
        }

        if (message.protocolVersion != ONLINE_PROTOCOL_VERSION) return

        if (!seenEventIdSet.add(message.eventId)) return
        seenEventIds.addLast(message.eventId)
        if (seenEventIds.size > MAX_SEEN_EVENT_IDS) {
            seenEventIdSet.remove(seenEventIds.removeFirst())
        }

        val messageMatchId = message.matchId
        if (messageMatchId != null && currentMatchId != null && messageMatchId != currentMatchId) return
        if (messageMatchId != null) currentMatchId = messageMatchId

        if (message.body is OnlineServerEventDto.Pong) {
            lastPongAtEpochMillis = currentEpochMillis()
        }

        message.toDomainEvent(currentEpochMillis())?.let { _events.emit(it) }

        if (message.body !is OnlineServerEventDto.Ack) {
            sendCommand(OnlineClientCommandDto.Ack(message.eventId), matchId = messageMatchId ?: currentMatchId)
        }
    }

    private fun startHeartbeatLoop() {
        heartbeatJob = scope.launch {
            while (true) {
                val intervalMs = heartbeatIntervalMillis(currentTimeControl)
                delay(intervalMs)
                runCatching { sendCommand(buildHeartbeatPing()) }
                    .onFailure { _events.emit(OnlineBattleEvent.Failure(FailureCode.PingSendFailed, it.message ?: "Ping failed")) }

                val elapsedSincePong = currentEpochMillis() - lastPongAtEpochMillis
                if (elapsedSincePong > intervalMs * 2) {
                    _events.emit(OnlineBattleEvent.ConnectionHealthChanged(isHealthy = false, latencyMillis = elapsedSincePong))
                }
            }
        }
    }

    private fun heartbeatIntervalMillis(timeControl: BattleTimeControl?): Long = when (timeControl) {
        BattleTimeControl.ONE_MINUTE -> 2_000L
        BattleTimeControl.TWO_MINUTES -> 4_000L
        BattleTimeControl.THREE_MINUTES -> 5_000L
        BattleTimeControl.INFINITY -> 5_000L
        null -> DEFAULT_HEARTBEAT_INTERVAL_MS
    }

    private fun scheduleReconnect() {
        if (manuallyDisconnected) return
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            var attempt = 0
            var succeeded = false
            while (attempt < MAX_RECONNECT_ATTEMPTS) {
                delay((attempt + 1) * 1500L)
                closeSocketQuietly()
                if (establishSession()) {
                    succeeded = true
                    break
                }
                attempt++
            }
            if (!succeeded) {
                _events.emit(OnlineBattleEvent.Failure(FailureCode.ReconnectExhausted, "Reconnect attempts exhausted"))
            }
        }
    }
}
