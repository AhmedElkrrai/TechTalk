package com.elkrrai.techtalk.presentation.battle.state

import com.elkrrai.techtalk.domain.model.battle.BattleAnswer
import com.elkrrai.techtalk.domain.model.battle.BattleQuestion
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.online.BattleTimeControl
import com.elkrrai.techtalk.domain.model.user.AvatarCatalog
import com.elkrrai.techtalk.presentation.battle.home.state.BattleMode
import com.elkrrai.techtalk.presentation.technologylist.state.TechnologyUiItem

/** Shared session model — the only channel between home -> battle -> result, held by
 * the [BattleSessionStore] singleton. */
data class BattleState(
    val phase: BattlePhase = BattlePhase.HOME,
    val battleSessionId: Int = 1,
    val subscribedTechnologies: List<TechnologyUiItem> = emptyList(),
    val isLoading: Boolean = false,
    val playerName: String = "",
    val playerAvatarKey: String = AvatarCatalog.defaultAvatarKey,
    val selectedTechnology: TechnologyUiItem? = null,
    val mode: BattleMode = BattleMode.OFFLINE,
    val onlinePhase: OnlineBattlePhase = OnlineBattlePhase.LOBBY,
    val selectedTimeControl: BattleTimeControl = BattleTimeControl.ONE_MINUTE,
    val selectedDifficulty: Difficulty = Difficulty.RANDOM,
    val remainingTimeSeconds: Int? = null,
    val questions: List<BattleQuestion> = emptyList(),
    val currentAnswers: List<BattleAnswer> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswerId: Long? = null,
    val hasAnswered: Boolean = false,
    val score: Int = 0,
    val xpGained: Int = 0,
    // Not in the original doc's field list, but necessary here in practice:
    // BattleLobbyViewModel and OnlineBattleViewModel are separate ViewModel instances
    // (a new screen -> a new instance), so the `Connected` event's playerId — needed
    // for score-orientation and persistence in the *online match* screen — would
    // otherwise be lost the moment the lobby hands off to the match screen, since the
    // events SharedFlow doesn't replay past values to new subscribers. Threading it
    // through the shared session store is the natural fix given this architecture.
    val currentPlayerId: String? = null
) {
    // Hardcoded in three places across the battle feature — keep consistent if ever
    // changed: here, OfflineBattleUiState, and OnlineBattleUiState.
    val totalQuestions: Int get() = 10

    val currentQuestion: BattleQuestion? get() = questions.getOrNull(currentQuestionIndex)

    val isSelectedCorrect: Boolean
        get() = selectedAnswerId != null && currentAnswers.any { it.id == selectedAnswerId && it.isCorrect }

    val progress: Float
        get() = currentQuestionIndex.toFloat() / totalQuestions.toFloat()
}

enum class BattlePhase { HOME, BATTLE, RESULT }
enum class OnlineBattlePhase { LOBBY, MATCH }
