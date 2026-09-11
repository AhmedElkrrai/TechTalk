package com.elkrrai.techtalk.presentation.battle.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.online.BattleTimeControl
import com.elkrrai.techtalk.domain.repository.TechTalkRepository
import com.elkrrai.techtalk.presentation.battle.home.state.BattleHomeUiState
import com.elkrrai.techtalk.presentation.battle.home.state.BattleMode
import com.elkrrai.techtalk.presentation.battle.state.BattlePhase
import com.elkrrai.techtalk.presentation.battle.state.BattleSessionStore
import com.elkrrai.techtalk.presentation.battle.state.BattleState
import com.elkrrai.techtalk.presentation.battle.state.OnlineBattlePhase
import com.elkrrai.techtalk.presentation.technologylist.mapper.toTechnologyUiItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BattleHomeViewModel(
    private val repository: TechTalkRepository,
    private val sessionStore: BattleSessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(BattleHomeUiState())
    val state: StateFlow<BattleHomeUiState> = _state.asStateFlow()

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
        observeSessionResets()
    }

    /** Restores defaults whenever the session returns to HOME with no technology. */
    private fun observeSessionResets() {
        viewModelScope.launch {
            sessionStore.state.collect { session ->
                if (session.phase == BattlePhase.HOME && session.selectedTechnology == null) {
                    _state.update { it.copy(isStartingBattle = false) }
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
            BattleMode.ONLINE -> sessionStore.startBattle(
                BattleState(
                    phase = BattlePhase.BATTLE,
                    subscribedTechnologies = current.technologies,
                    playerName = current.playerName,
                    playerAvatarKey = current.playerAvatarKey,
                    selectedTechnology = technology,
                    mode = BattleMode.ONLINE,
                    onlinePhase = OnlineBattlePhase.LOBBY,
                    selectedTimeControl = current.selectedTimeControl,
                    selectedDifficulty = current.selectedDifficulty,
                    remainingTimeSeconds = remainingSecondsFor(current.selectedTimeControl),
                    questions = emptyList()
                )
            )

            BattleMode.OFFLINE -> startOfflineBattle(current, technology.id)
        }
    }

    private fun startOfflineBattle(current: BattleHomeUiState, technologyId: Long) {
        _state.update { it.copy(isStartingBattle = true, errorMessage = null) }
        viewModelScope.launch {
            val questionIds = (
                if (current.selectedDifficulty == Difficulty.RANDOM) {
                    repository.getQuestionIdsByTechnology(technologyId)
                } else {
                    repository.getQuestionIdsByTechnologyAndDifficulty(technologyId, current.selectedDifficulty)
                }
                ).shuffled().take(10)

            if (questionIds.isEmpty()) {
                _state.update {
                    it.copy(isStartingBattle = false, errorMessage = "No questions available for this technology yet")
                }
                return@launch
            }

            val questions = questionIds.mapNotNull { repository.getQuestionById(it) }
            val firstAnswers = questions.firstOrNull()
                ?.let { repository.getAnswersByQuestionId(it.id) }
                ?.shuffled()
                .orEmpty()

            sessionStore.startBattle(
                BattleState(
                    phase = BattlePhase.BATTLE,
                    subscribedTechnologies = current.technologies,
                    playerName = current.playerName,
                    playerAvatarKey = current.playerAvatarKey,
                    selectedTechnology = current.selectedTechnology,
                    mode = BattleMode.OFFLINE,
                    selectedTimeControl = current.selectedTimeControl,
                    selectedDifficulty = current.selectedDifficulty,
                    remainingTimeSeconds = remainingSecondsFor(current.selectedTimeControl),
                    questions = questions,
                    currentAnswers = firstAnswers
                )
            )
            _state.update { it.copy(isStartingBattle = false) }
        }
    }

    private fun remainingSecondsFor(timeControl: BattleTimeControl): Int? =
        if (timeControl == BattleTimeControl.INFINITY) null else timeControl.totalSeconds
}
