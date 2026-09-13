package com.elkrrai.techtalk.presentation.battle.online

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.presentation.battle.component.BattleAnswersList
import com.elkrrai.techtalk.presentation.battle.component.BattleProgressSection
import com.elkrrai.techtalk.presentation.battle.component.BattleQuestionHeader
import com.elkrrai.techtalk.presentation.battle.component.BattleTopBar
import com.elkrrai.techtalk.presentation.component.BattlePlayerChip

@Composable
fun OnlineBattleScreen(viewModel: OnlineBattleViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        BattleTopBar(title = "Online battle", onResign = viewModel::onResign)

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BattlePlayerChip(name = state.playerName, avatarKey = state.playerAvatarKey, score = state.playerScore, isActive = true)
            Text("vs", style = MaterialTheme.typography.labelLarge)
            BattlePlayerChip(name = state.foeName.ifBlank { "Opponent" }, avatarKey = state.foeAvatarKey, score = state.foeScore)
        }

        BattleProgressSection(
            progress = state.progress,
            questionIndex = state.currentQuestionIndex,
            totalQuestions = state.totalQuestions,
            remainingTimeSeconds = state.remainingTimeSeconds
        )

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        }

        BattleQuestionHeader(questionText = state.currentQuestionText, difficulty = state.currentQuestionDifficulty)
        // No Submit step — tapping an answer submits it immediately (see
        // OnlineBattleViewModel.onAnswerSelected) and the next question follows as soon
        // as the server round-trip completes; BattleAnswersList's own selected-border
        // and hasAnswered-disables-further-taps states are the only feedback needed.
        BattleAnswersList(
            answers = state.currentAnswers,
            selectedAnswerId = state.selectedAnswerId,
            hasAnswered = state.hasAnswered,
            onAnswerSelected = viewModel::onAnswerSelected
        )
    }
}
