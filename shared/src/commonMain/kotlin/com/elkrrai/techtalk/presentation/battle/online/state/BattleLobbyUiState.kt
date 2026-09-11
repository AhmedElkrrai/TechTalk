package com.elkrrai.techtalk.presentation.battle.online.state

data class BattleLobbyUiState(
    val selectedRole: OnlineMatchRole? = null,
    val stage: OnlineMatchStage = OnlineMatchStage.ROLE_SELECTION,
    val connectionStatus: OnlineConnectionStatus = OnlineConnectionStatus.DISCONNECTED,
    val selectedTimeLabel: String = "",
    val selectedDifficultyLabel: String = "",
    val selectedTech: String = "",
    val roomCodeInput: String = "",
    val roomCode: String = "",
    val activeMatchId: String? = null,
    val roomExpiresInSeconds: Int? = null,
    // Leftover from a direct peer-to-peer design — never populated.
    val hostAddress: String? = null,
    val opponentName: String? = null,
    val isBusy: Boolean = false,
    val errorMessage: String? = null
)

enum class OnlineMatchRole { HOST, JOIN }

enum class OnlineMatchStage { ROLE_SELECTION, CREATING_ROOM, WAITING_FOR_PLAYER, JOINING_ROOM, CONNECTED }

enum class OnlineConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING }
