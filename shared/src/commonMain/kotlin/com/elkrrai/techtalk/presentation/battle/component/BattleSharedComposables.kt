package com.elkrrai.techtalk.presentation.battle.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.presentation.component.DifficultyBadge
import com.elkrrai.techtalk.presentation.component.AppProgressBar
import com.elkrrai.techtalk.presentation.theme.SuccessColor

/** Answer option shown in-battle. [isCorrect] is only ever true for offline battles —
 * online answers never carry a correctness flag (only the server knows). */
data class BattleAnswerOptionUi(
    val id: Long,
    val text: String,
    val isCorrect: Boolean
)

@Composable
fun BattleTopBar(title: String, onResign: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Resign",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.clickable(onClick = onResign)
        )
    }
}

@Composable
fun BattleProgressSection(
    progress: Float,
    questionIndex: Int,
    totalQuestions: Int,
    remainingTimeSeconds: Int?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Question ${(questionIndex + 1).coerceAtMost(totalQuestions)}/$totalQuestions",
                style = MaterialTheme.typography.labelLarge
            )
            if (remainingTimeSeconds != null) {
                val minutes = remainingTimeSeconds / 60
                val seconds = remainingTimeSeconds % 60
                Text(
                    text = "$minutes:${seconds.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        AppProgressBar(progress = progress, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun BattleQuestionHeader(questionText: String, difficulty: Difficulty, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        DifficultyBadge(difficulty)
        Spacer(Modifier.height(8.dp))
        Text(text = questionText, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun BattleAnswersList(
    answers: List<BattleAnswerOptionUi>,
    selectedAnswerId: Long?,
    hasAnswered: Boolean,
    onAnswerSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Online battles never carry per-answer correctness client-side — every
    // BattleAnswerOptionUi.isCorrect is hardcoded false there (only the server knows).
    // Only treat hasAnswered as "reveal right/wrong" when correctness is actually known
    // (offline always has exactly one correct answer); otherwise just highlight what was
    // submitted, instead of every online pick painting itself red as if it were wrong.
    val correctnessKnown = answers.any { it.isCorrect }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        answers.forEach { answer ->
            val isSelected = answer.id == selectedAnswerId
            val isWrongSelected = hasAnswered && correctnessKnown && isSelected && !answer.isCorrect
            val isRevealedCorrect = hasAnswered && correctnessKnown && answer.isCorrect
            val isSubmittedUnknown = hasAnswered && !correctnessKnown && isSelected
            val backgroundColor = when {
                isRevealedCorrect -> SuccessColor.copy(alpha = 0.25f)
                isWrongSelected -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                isSubmittedUnknown -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val borderColor = when {
                isWrongSelected -> MaterialTheme.colorScheme.error
                isRevealedCorrect -> SuccessColor
                isSelected -> MaterialTheme.colorScheme.primary
                else -> backgroundColor
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable(enabled = !hasAnswered) { onAnswerSelected(answer.id) }
                    .border(
                        width = if (isSelected || isRevealedCorrect) 1.5.dp else 0.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(backgroundColor).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(answer.text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun BattleExplanationSection(explanation: String?, modifier: Modifier = Modifier) {
    if (explanation.isNullOrBlank()) return
    Card(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Explanation", style = MaterialTheme.typography.labelLarge)
            Text(explanation, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
