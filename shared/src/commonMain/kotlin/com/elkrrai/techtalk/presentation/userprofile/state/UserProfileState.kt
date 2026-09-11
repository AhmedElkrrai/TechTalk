package com.elkrrai.techtalk.presentation.userprofile.state

import com.elkrrai.techtalk.domain.model.battle.BattleHistoryItem
import com.elkrrai.techtalk.domain.model.battle.BattleHistorySummary
import com.elkrrai.techtalk.domain.model.user.AvatarCatalog
import com.elkrrai.techtalk.domain.model.user.AvatarOption

data class UserProfileState(
    val name: String = "",
    val selectedAvatarKey: String = AvatarCatalog.defaultAvatarKey,
    val avatars: List<AvatarOption> = AvatarCatalog.options,
    val level: Int = 1,
    val currentXp: Int = 0,
    val xpToNextLevel: Int = 100,
    val xpProgressFraction: Float = 0f,
    val battleSummary: BattleHistorySummary = BattleHistorySummary(),
    val recentBattles: List<BattleHistoryItem> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null
)
