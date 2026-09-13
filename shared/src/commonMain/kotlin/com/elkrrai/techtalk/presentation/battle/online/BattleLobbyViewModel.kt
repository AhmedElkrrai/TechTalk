package com.elkrrai.techtalk.presentation.battle.online

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.techtalk.domain.model.common.getTitle
import com.elkrrai.techtalk.domain.model.online.JoinOnlineRoomRequest
import com.elkrrai.techtalk.domain.model.online.OnlineBattleEvent
import com.elkrrai.techtalk.domain.model.online.OnlineMatchSettings
import com.elkrrai.techtalk.domain.usecase.online.ConnectOnlineBattleUseCase
import com.elkrrai.techtalk.domain.usecase.online.CreateOnlineRoomUseCase
import com.elkrrai.techtalk.domain.usecase.online.DisconnectOnlineBattleUseCase
import com.elkrrai.techtalk.domain.usecase.online.JoinOnlineRoomUseCase
import com.elkrrai.techtalk.domain.usecase.online.ObserveOnlineBattleEventsUseCase
import com.elkrrai.techtalk.presentation.battle.BattleLobbyRoute
import com.elkrrai.techtalk.presentation.battle.OnlineBattleRoute
import com.elkrrai.techtalk.presentation.battle.online.mapper.applyOnlineEvent
import com.elkrrai.techtalk.presentation.battle.online.state.BattleLobbyUiState
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineConnectionStatus
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineMatchRole
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineMatchStage
import com.elkrrai.techtalk.presentation.battle.state.getLabel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BattleLobbyViewModel(
    private val route: BattleLobbyRoute,
    private val connect: ConnectOnlineBattleUseCase,
    private val disconnect: DisconnectOnlineBattleUseCase,
    private val createRoom: CreateOnlineRoomUseCase,
    private val joinRoom: JoinOnlineRoomUseCase,
    private val observeEvents: ObserveOnlineBattleEventsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(
        BattleLobbyUiState(
            selectedTimeLabel = route.timeControl.getLabel(),
            selectedDifficultyLabel = route.difficulty.getTitle(),
            selectedTech = route.technologyName
        )
    )
    val state: StateFlow<BattleLobbyUiState> = _state.asStateFlow()

    private val _navigateToMatch = Channel<OnlineBattleRoute>(Channel.BUFFERED)
    val navigateToMatch: Flow<OnlineBattleRoute> = _navigateToMatch.receiveAsFlow()

    private val _navigateToHome = Channel<Unit>(Channel.BUFFERED)
    val navigateToHome: Flow<Unit> = _navigateToHome.receiveAsFlow()

    private var currentPlayerId: String? = null

    init {
        viewModelScope.launch {
            observeEvents().collect { event -> handleEvent(event) }
        }
    }

    private fun handleEvent(event: OnlineBattleEvent) {
        if (event is OnlineBattleEvent.Connected) {
            currentPlayerId = event.playerId
        }

        if (event is OnlineBattleEvent.MatchStarted) {
            val playerId = currentPlayerId ?: return
            if (event.matchId.isBlank()) return
            viewModelScope.launch {
                _navigateToMatch.send(
                    OnlineBattleRoute(
                        technologyId = route.technologyId,
                        technologyName = route.technologyName,
                        playerName = route.playerName,
                        playerAvatarKey = route.playerAvatarKey,
                        currentPlayerId = playerId,
                        matchId = event.matchId,
                        startedAtEpochMillis = event.startedAtEpochMillis,
                        totalDurationSeconds = event.totalDurationSeconds
                    )
                )
            }
            _state.update { it.copy(stage = OnlineMatchStage.CONNECTED, isBusy = false) }
            return
        }

        _state.update { it.applyOnlineEvent(event, currentPlayerId) }
    }

    fun onRoleSelected(role: OnlineMatchRole) {
        _state.update { it.copy(selectedRole = role) }
        viewModelScope.launch { ensureConnected() }
    }

    /**
     * Connects only when currently disconnected, player name falling back to
     * `"Player"` when blank — and **awaits** the connection rather than firing it as a
     * detached coroutine. This must run to completion before any command that needs a
     * `playerId` (createRoom/joinRoom) — calling those from a separate `launch{}` racing
     * against this one is exactly what used to produce "Not connected to the server" on
     * a fresh app start, since `createRoom`/`joinRoom` could reach the repository before
     * `connect()`'s anonymous sign-in had actually finished.
     *
     * When another caller is already connecting (e.g. [onRoleSelected] fired first on
     * the same tap that also calls [onCreateRoom]/[onJoinRoom]), this waits for that
     * attempt to resolve instead of starting a second concurrent sign-in.
     */
    private suspend fun ensureConnected() {
        when (_state.value.connectionStatus) {
            OnlineConnectionStatus.CONNECTED -> Unit
            OnlineConnectionStatus.CONNECTING -> {
                state.first { it.connectionStatus != OnlineConnectionStatus.CONNECTING || it.errorMessage != null }
            }
            else -> {
                val playerName = route.playerName.ifBlank { "Player" }
                _state.update { it.copy(connectionStatus = OnlineConnectionStatus.CONNECTING) }
                connect(playerName)
            }
        }
    }

    fun onCreateRoom() {
        _state.update { it.copy(stage = OnlineMatchStage.CREATING_ROOM, isBusy = true) }
        viewModelScope.launch {
            ensureConnected()
            createRoom(
                OnlineMatchSettings(
                    timeControl = route.timeControl,
                    difficulty = route.difficulty,
                    technologyId = route.technologyId,
                    technologyName = route.technologyName
                )
            )
        }
    }

    /** Normalizes to `uppercase().filter { isLetterOrDigit() }.take(6)`. */
    fun onRoomCodeInputChanged(input: String) {
        _state.update {
            it.copy(roomCodeInput = input.uppercase().filter { c -> c.isLetterOrDigit() }.take(6))
        }
    }

    fun onJoinRoom() {
        val input = _state.value.roomCodeInput
        if (input.length < 6) {
            _state.update { it.copy(errorMessage = "Enter the 6-character room code") }
            return
        }
        _state.update { it.copy(stage = OnlineMatchStage.JOINING_ROOM, isBusy = true) }
        viewModelScope.launch {
            ensureConnected()
            joinRoom(JoinOnlineRoomRequest(input))
        }
    }

    /** Calls [disconnect] directly, not a `LeaveOnlineRoom` use case — matches the
     * original app's behavior (that use case has no UI caller). Cancelling out of the
     * lobby (waiting-for-opponent or role-selection) leaves the online flow entirely
     * and returns to Battle Home, rather than resetting back to this same screen. */
    fun onLeaveRoom() {
        viewModelScope.launch {
            disconnect()
            _navigateToHome.send(Unit)
        }
    }
}
