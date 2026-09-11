package com.elkrrai.techtalk.presentation.battle.online.mapper

import com.elkrrai.techtalk.domain.model.online.OnlineBattleEvent
import com.elkrrai.techtalk.domain.model.online.OnlinePlayer
import com.elkrrai.techtalk.presentation.battle.online.state.BattleLobbyUiState
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineConnectionStatus
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineMatchStage

/** Handles Connected, Disconnected, RoomCreated, RoomJoined, LobbyUpdated,
 * ConnectionHealthChanged, OpponentConnectionChanged, RoomExpired, Failure and ignores
 * the rest (`MatchStarted` promotes the session and is handled directly by
 * `BattleLobbyViewModel`; match-play events belong to the online battle screen). */
fun BattleLobbyUiState.applyOnlineEvent(event: OnlineBattleEvent, currentPlayerId: String?): BattleLobbyUiState =
    when (event) {
        is OnlineBattleEvent.Connected -> copy(connectionStatus = OnlineConnectionStatus.CONNECTED)

        is OnlineBattleEvent.Disconnected -> copy(connectionStatus = OnlineConnectionStatus.DISCONNECTED)

        is OnlineBattleEvent.RoomCreated -> copy(
            stage = OnlineMatchStage.WAITING_FOR_PLAYER,
            roomCode = event.roomCode,
            activeMatchId = event.matchId,
            roomExpiresInSeconds = event.expiresInSeconds,
            isBusy = false
        )

        is OnlineBattleEvent.RoomJoined -> copy(
            stage = OnlineMatchStage.WAITING_FOR_PLAYER,
            roomCode = event.roomCode,
            activeMatchId = event.matchId,
            opponentName = resolveOpponentName(event.host, event.guest, currentPlayerId),
            isBusy = false
        )

        is OnlineBattleEvent.LobbyUpdated -> copy(
            opponentName = resolveOpponentName(event.host, event.guest, currentPlayerId)
        )

        is OnlineBattleEvent.ConnectionHealthChanged -> copy(
            connectionStatus = if (event.isHealthy) connectionStatus else OnlineConnectionStatus.RECONNECTING
        )

        is OnlineBattleEvent.OpponentConnectionChanged -> this

        is OnlineBattleEvent.RoomExpired -> copy(
            stage = OnlineMatchStage.ROLE_SELECTION,
            roomCode = "",
            activeMatchId = null,
            errorMessage = "Room expired: ${event.reason}",
            isBusy = false
        )

        is OnlineBattleEvent.Failure -> copy(
            errorMessage = mapOnlineFailureToUserMessage(event.code, event.message),
            isBusy = false
        )

        else -> this
    }

/** [currentPlayerId == host.playerId] means the local player is the host, so the
 * opponent is the guest — and vice versa. */
private fun resolveOpponentName(host: OnlinePlayer, guest: OnlinePlayer?, currentPlayerId: String?): String? =
    if (currentPlayerId == host.playerId) guest?.displayName else host.displayName
