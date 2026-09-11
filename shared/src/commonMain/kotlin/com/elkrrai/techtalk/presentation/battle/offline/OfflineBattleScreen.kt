package com.elkrrai.techtalk.presentation.battle.offline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.presentation.battle.component.BattleAnswersList
import com.elkrrai.techtalk.presentation.battle.component.BattleExplanationSection
import com.elkrrai.techtalk.presentation.battle.component.BattleProgressSection
import com.elkrrai.techtalk.presentation.battle.component.BattleQuestionHeader
import com.elkrrai.techtalk.presentation.battle.component.BattleTopBar
import com.elkrrai.techtalk.presentation.component.AppButton
import kotlinx.coroutines.delay

@Composable
fun OfflineBattleScreen(viewModel: OfflineBattleViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(state.hasAnswered) {
        if (state.hasAnswered) {
            delay(500L)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
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
            AppButton(
                text = if (state.isLastQuestion) "Finish" else "Next question",
                onClick = viewModel::onNextQuestion,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .semantics { contentDescription = "Next question" }
            )
        }
    }
}
