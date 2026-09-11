package com.elkrrai.techtalk.presentation.userprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.techtalk.domain.repository.TechTalkRepository
import com.elkrrai.techtalk.presentation.userprofile.state.UserProfileState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MIN_NAME_LENGTH = 2
private const val MAX_NAME_LENGTH = 24

class UserProfileViewModel(private val repository: TechTalkRepository) : ViewModel() {

    private val _state = MutableStateFlow(UserProfileState())
    val state: StateFlow<UserProfileState> = _state.asStateFlow()

    private var pendingName: String = ""

    init {
        viewModelScope.launch {
            repository.observeUserProfile().collect { profile ->
                pendingName = profile.name
                _state.update {
                    it.copy(
                        name = profile.name,
                        selectedAvatarKey = profile.avatarKey,
                        level = profile.level,
                        currentXp = profile.currentXp,
                        xpToNextLevel = profile.xpToNextLevel,
                        xpProgressFraction = profile.xpProgressFraction,
                        isLoading = false
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.observeBattleHistorySummary().collect { summary ->
                _state.update { it.copy(battleSummary = summary) }
            }
        }
        viewModelScope.launch {
            repository.observeRecentBattles().collect { battles ->
                _state.update { it.copy(recentBattles = battles) }
            }
        }
    }

    fun onNameChanged(name: String) {
        pendingName = name
        _state.update { it.copy(name = name) }
    }

    fun onAvatarSelected(avatarKey: String) {
        if (avatarKey == _state.value.selectedAvatarKey) return
        viewModelScope.launch { repository.updateUserAvatar(avatarKey) }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }
}
