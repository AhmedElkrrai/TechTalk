package com.elkrrai.techtalk.presentation.battle.offline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.elkrrai.techtalk.presentation.battle.component.BattleAnswersList
import com.elkrrai.techtalk.presentation.battle.component.BattleExplanationSection
import com.elkrrai.techtalk.presentation.battle.component.BattleProgressSection
import com.elkrrai.techtalk.presentation.battle.component.BattleQuestionHeader
import com.elkrrai.techtalk.presentation.battle.component.BattleTopBar
import kotlinx.coroutines.delay

/** Delay between an answer being locked in and auto-advancing to the next question —
 * long enough to register the correct/wrong feedback, short enough to not need a
 * "Next question" button or any further input from the user. */
private const val AUTO_ADVANCE_DELAY_MS = 500L

@Composable
fun OfflineBattleScreen(viewModel: OfflineBattleViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.hasAnswered) {
        if (state.hasAnswered) {
            delay(AUTO_ADVANCE_DELAY_MS)
            viewModel.onNextQuestion()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        BattleTopBar(title = state.title, onResign = viewModel::onResign)
        BattleProgressSection(
            progress = state.progress,
            questionIndex = state.currentQuestionIndex,
            totalQuestions = state.totalQuestions,
            remainingTimeSeconds = state.remainingTimeSeconds
        )
        BattleQuestionHeader(questionText = state.questionText, difficulty = state.difficulty)
        BattleAnswersList(
            answers = state.currentAnswers,
            selectedAnswerId = state.selectedAnswerId,
            hasAnswered = state.hasAnswered,
            onAnswerSelected = viewModel::onAnswerSelected
        )
        if (state.hasAnswered) {
            BattleExplanationSection(explanation = state.explanation)
        }
    }
}
