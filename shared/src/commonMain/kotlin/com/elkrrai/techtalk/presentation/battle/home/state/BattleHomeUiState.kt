package com.elkrrai.techtalk.presentation.battle.home.state

import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.online.BattleTimeControl
import com.elkrrai.techtalk.domain.model.user.AvatarCatalog
import com.elkrrai.techtalk.presentation.technologylist.state.TechnologyUiItem

data class BattleHomeUiState(
    val technologies: List<TechnologyUiItem> = emptyList(),
    val selectedTechnology: TechnologyUiItem? = null,
    val mode: BattleMode = BattleMode.OFFLINE,
    val selectedTimeControl: BattleTimeControl = BattleTimeControl.ONE_MINUTE,
    val selectedDifficulty: Difficulty = Difficulty.RANDOM,
    val playerName: String = "",
    val playerAvatarKey: String = AvatarCatalog.defaultAvatarKey,
    // True until the real profile + subscribed-technologies data has loaded at least
    // once — lets the screen show a spinner instead of a flash of default avatar/name
    // and an empty technology list before the first real emission arrives.
    val isLoading: Boolean = true,
    val isStartingBattle: Boolean = false,
    val errorMessage: String? = null
)
