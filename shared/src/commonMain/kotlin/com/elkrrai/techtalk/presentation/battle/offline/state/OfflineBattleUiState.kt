package com.elkrrai.techtalk.presentation.battle.offline.state

import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.presentation.battle.component.BattleAnswerOptionUi
import com.elkrrai.techtalk.presentation.battle.state.BATTLE_TOTAL_QUESTIONS

data class OfflineBattleUiState(
    val title: String = "",
    val questionText: String = "",
    val difficulty: Difficulty = Difficulty.BEGINNER,
    val explanation: String = "",
    val currentAnswers: List<BattleAnswerOptionUi> = emptyList(),
    val selectedAnswerId: Long? = null,
    val hasAnswered: Boolean = false,
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = BATTLE_TOTAL_QUESTIONS,
    val score: Int = 0,
    val remainingTimeSeconds: Int? = null,
    val isLastQuestion: Boolean = false
) {
    /** Counts the current question as "done" the moment it's answered, rather than only
     * after advancing to the next one — so the bar visibly moves on every answer tap. */
    val progress: Float get() {
        val completed = if (hasAnswered) currentQuestionIndex + 1 else currentQuestionIndex
        return completed.toFloat() / totalQuestions.toFloat()
    }
}
