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
import com.elkrrai.techtalk.presentation.battle.online.mapper.applyOnlineEvent
import com.elkrrai.techtalk.presentation.battle.online.mapper.toOnlineBattleState
import com.elkrrai.techtalk.presentation.battle.online.state.BattleLobbyUiState
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineConnectionStatus
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineMatchRole
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineMatchStage
import com.elkrrai.techtalk.presentation.battle.state.BattleSessionStore
import com.elkrrai.techtalk.presentation.battle.state.getLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BattleLobbyViewModel(
    private val sessionStore: BattleSessionStore,
    private val connect: ConnectOnlineBattleUseCase,
    private val disconnect: DisconnectOnlineBattleUseCase,
    private val createRoom: CreateOnlineRoomUseCase,
    private val joinRoom: JoinOnlineRoomUseCase,
    private val observeEvents: ObserveOnlineBattleEventsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BattleLobbyUiState())
    val state: StateFlow<BattleLobbyUiState> = _state.asStateFlow()

    private var currentPlayerId: String? = null

    init {
        val session = sessionStore.state.value
        _state.update {
            it.copy(
                selectedTimeLabel = session.selectedTimeControl.getLabel(),
                selectedDifficultyLabel = session.selectedDifficulty.getTitle(),
                selectedTech = session.selectedTechnology?.name.orEmpty()
            )
        }
        viewModelScope.launch {
            observeEvents().collect { event -> handleEvent(event) }
        }
    }

    private fun handleEvent(event: OnlineBattleEvent) {
        if (event is OnlineBattleEvent.Connected) {
            currentPlayerId = event.playerId
            sessionStore.update { it.copy(currentPlayerId = event.playerId) }
        }

        if (event is OnlineBattleEvent.MatchStarted) {
            val session = sessionStore.state.value
            if (event.matchId.isBlank() || session.selectedTechnology == null) return
            sessionStore.update { it.toOnlineBattleState(event.totalDurationSeconds) }
            _state.update { it.copy(stage = OnlineMatchStage.CONNECTED, isBusy = false) }
            return
        }

        _state.update { it.applyOnlineEvent(event, currentPlayerId) }
    }

    fun onRoleSelected(role: OnlineMatchRole) {
        _state.update { it.copy(selectedRole = role) }
        ensureConnected()
    }

    /** Connects only when currently disconnected — player name falls back to
     * `"Player"` when blank. */
    private fun ensureConnected() {
        if (_state.value.connectionStatus != OnlineConnectionStatus.DISCONNECTED) return
        val playerName = sessionStore.state.value.playerName.ifBlank { "Player" }
        _state.update { it.copy(connectionStatus = OnlineConnectionStatus.CONNECTING) }
        viewModelScope.launch { connect(playerName) }
    }

    fun onCreateRoom() {
        val session = sessionStore.state.value
        val technology = session.selectedTechnology ?: return
        ensureConnected()
        _state.update { it.copy(stage = OnlineMatchStage.CREATING_ROOM, isBusy = true) }
        viewModelScope.launch {
            createRoom(
                OnlineMatchSettings(
                    timeControl = session.selectedTimeControl,
                    difficulty = session.selectedDifficulty,
                    technologyId = technology.id,
                    technologyName = technology.name
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
        ensureConnected()
        _state.update { it.copy(stage = OnlineMatchStage.JOINING_ROOM, isBusy = true) }
        viewModelScope.launch { joinRoom(JoinOnlineRoomRequest(input)) }
    }

    /** Calls [disconnect] directly, not a `LeaveOnlineRoom` use case — matches the
     * original app's behavior (that use case has no UI caller). */
    fun onLeaveRoom() {
        viewModelScope.launch { disconnect() }
        _state.value = BattleLobbyUiState()
    }
}
