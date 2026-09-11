package com.elkrrai.techtalk.presentation.battle.online.state

import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.user.AvatarCatalog
import com.elkrrai.techtalk.presentation.battle.component.BattleAnswerOptionUi

data class OnlineBattleUiState(
    val matchId: String? = null,
    val roomCode: String = "",
    val currentQuestionId: String? = null,
    val currentQuestionText: String = "",
    val currentQuestionDifficulty: Difficulty = Difficulty.BEGINNER,
    val currentAnswers: List<BattleAnswerOptionUi> = emptyList(),
    val selectedAnswerId: Long? = null,
    val hasAnswered: Boolean = false,
    val currentQuestionIndex: Int = 0,
    // Hardcoded to 10, matching BattleState — see its comment.
    val totalQuestions: Int = 10,
    val remainingTimeSeconds: Int? = null,
    val playerName: String = "",
    val playerAvatarKey: String = AvatarCatalog.defaultAvatarKey,
    val foeName: String = "",
    val playerScore: Int = 0,
    val foeScore: Int = 0,
    val connectionStatus: OnlineConnectionStatus = OnlineConnectionStatus.DISCONNECTED,
    val isSubmittingAnswer: Boolean = false,
    val isMatchEnded: Boolean = false,
    val errorMessage: String? = null,
    val explanation: String? = null
) {
    val progress: Float get() = currentQuestionIndex.toFloat() / totalQuestions.toFloat()
    val isLastQuestion: Boolean get() = currentQuestionIndex >= totalQuestions - 1
    val canSubmitAnswer: Boolean
        get() = selectedAnswerId != null && !hasAnswered && !isSubmittingAnswer &&
            connectionStatus == OnlineConnectionStatus.CONNECTED
}
