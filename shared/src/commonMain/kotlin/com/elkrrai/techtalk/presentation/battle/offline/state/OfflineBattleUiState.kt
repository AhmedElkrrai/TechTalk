package com.elkrrai.techtalk.presentation.battle.offline.state

import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.presentation.battle.component.BattleAnswerOptionUi

data class OfflineBattleUiState(
    val title: String = "",
    val questionText: String = "",
    val difficulty: Difficulty = Difficulty.BEGINNER,
    val explanation: String = "",
    val currentAnswers: List<BattleAnswerOptionUi> = emptyList(),
    val selectedAnswerId: Long? = null,
    val hasAnswered: Boolean = false,
    val currentQuestionIndex: Int = 0,
    // Hardcoded to 10, matching BattleState — see its comment.
    val totalQuestions: Int = 10,
    val score: Int = 0,
    val remainingTimeSeconds: Int? = null,
    val isLastQuestion: Boolean = false
) {
    val progress: Float get() = currentQuestionIndex.toFloat() / totalQuestions.toFloat()
}
