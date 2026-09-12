package com.elkrrai.techtalk.presentation.battle.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.online.BattleTimeControl
import com.elkrrai.techtalk.domain.repository.TechTalkRepository
import com.elkrrai.techtalk.presentation.battle.BattleLobbyRoute
import com.elkrrai.techtalk.presentation.battle.OfflineBattleRoute
import com.elkrrai.techtalk.presentation.battle.home.state.BattleHomeUiState
import com.elkrrai.techtalk.presentation.battle.home.state.BattleMode
import com.elkrrai.techtalk.presentation.technologylist.mapper.toTechnologyUiItem
import com.elkrrai.techtalk.presentation.technologylist.state.TechnologyUiItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BattleHomeViewModel(
    private val repository: TechTalkRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BattleHomeUiState())
    val state: StateFlow<BattleHomeUiState> = _state.asStateFlow()

    private val _navigateToOfflineBattle = Channel<OfflineBattleRoute>(Channel.BUFFERED)
    val navigateToOfflineBattle: Flow<OfflineBattleRoute> = _navigateToOfflineBattle.receiveAsFlow()

    private val _navigateToLobby = Channel<BattleLobbyRoute>(Channel.BUFFERED)
    val navigateToLobby: Flow<BattleLobbyRoute> = _navigateToLobby.receiveAsFlow()

    // isLoading only ever needs to flip false once both flows below have emitted at
    // least once — tracked separately since either can arrive first (or re-emit later).
    private var hasLoadedTechnologies = false
    private var hasLoadedProfile = false

    init {
        viewModelScope.launch {
            repository.observeSubscribedTechnologies().collect { technologies ->
                val items = technologies.map { it.toTechnologyUiItem(isSubscribed = true) }
                hasLoadedTechnologies = true
                _state.update { current ->
                    val stillValid = current.selectedTechnology?.let { sel -> items.firstOrNull { it.id == sel.id } }
                    current.copy(
                        technologies = items,
                        selectedTechnology = stillValid ?: items.firstOrNull(),
                        isLoading = !(hasLoadedTechnologies && hasLoadedProfile)
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.observeUserProfile().collect { profile ->
                hasLoadedProfile = true
                _state.update {
                    it.copy(
                        playerName = profile.name,
                        playerAvatarKey = profile.avatarKey,
                        isLoading = !(hasLoadedTechnologies && hasLoadedProfile)
                    )
                }
            }
        }
    }

    fun onTechSelected(technologyId: Long) {
        _state.update { current -> current.copy(selectedTechnology = current.technologies.firstOrNull { it.id == technologyId }) }
    }

    fun onTimeSelected(timeControl: BattleTimeControl) {
        _state.update { it.copy(selectedTimeControl = timeControl) }
    }

    fun onDifficultySelected(difficulty: Difficulty) {
        _state.update { it.copy(selectedDifficulty = difficulty) }
    }

    fun onModeSelected(mode: BattleMode) {
        _state.update { it.copy(mode = mode) }
    }

    fun onStartBattle() {
        val current = _state.value
        val technology = current.selectedTechnology ?: return
        if (current.isStartingBattle) return

        when (current.mode) {
            BattleMode.ONLINE -> viewModelScope.launch {
                _navigateToLobby.send(
                    BattleLobbyRoute(
                        technologyId = technology.id,
                        technologyName = technology.name,
                        playerName = current.playerName,
                        playerAvatarKey = current.playerAvatarKey,
                        timeControl = current.selectedTimeControl,
                        difficulty = current.selectedDifficulty
                    )
                )
            }

            BattleMode.OFFLINE -> startOfflineBattle(current, technology)
        }
    }

    /** Only checks that at least one question exists — the real fetch/shuffle/load
     * happens in [com.elkrrai.techtalk.presentation.battle.offline.OfflineBattleViewModel]
     * itself once we navigate there, so this screen doesn't duplicate that work. Doing
     * this cheap existence check here (rather than letting the destination discover
     * "no questions" on its own) is what preserves today's UX: the error shows on Home,
     * with a way back, instead of stranding the user on a battle screen with nothing to
     * answer and no way out except Resign. */
    private fun startOfflineBattle(current: BattleHomeUiState, technology: TechnologyUiItem) {
        _state.update { it.copy(isStartingBattle = true, errorMessage = null) }
        viewModelScope.launch {
            val hasQuestions = if (current.selectedDifficulty == Difficulty.RANDOM) {
                repository.getQuestionIdsByTechnology(technology.id).isNotEmpty()
            } else {
                repository.getQuestionIdsByTechnologyAndDifficulty(technology.id, current.selectedDifficulty).isNotEmpty()
            }

            if (!hasQuestions) {
                _state.update {
                    it.copy(isStartingBattle = false, errorMessage = "No questions available for this technology yet")
                }
                return@launch
            }

            _state.update { it.copy(isStartingBattle = false) }
            _navigateToOfflineBattle.send(
                OfflineBattleRoute(
                    technologyId = technology.id,
                    technologyName = technology.name,
                    playerName = current.playerName,
                    playerAvatarKey = current.playerAvatarKey,
                    timeControl = current.selectedTimeControl,
                    difficulty = current.selectedDifficulty
                )
            )
        }
    }
}
