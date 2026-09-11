package com.elkrrai.techtalk.presentation.battle.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.domain.model.user.AvatarCatalog
import com.elkrrai.techtalk.presentation.component.AppButton
import com.elkrrai.techtalk.presentation.component.AppOutlinedButton

@Composable
fun BattleResultScreen(
    viewModel: BattleResultViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    // onClose fires only from the explicit "Pick another tech" tap below — NOT from a
    // DisposableEffect(Unit) { onDispose { onClose() } } like an earlier version had.
    // That pattern fired on ANY disposal, including the one "Try again" itself causes by
    // succeeding (phase -> BATTLE routes away from this screen), which wiped the battle
    // that was just started. See BattleSessionStore's plan doc for the full story.
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            AvatarCatalog.emojiFor(state.playerAvatarKey),
            style = MaterialTheme.typography.displayLarge
        )
        Text(
            text = "${state.score} / ${state.totalQuestions}",
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = state.technologyName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        Text(
            text = "+ ${state.xpGained} XP",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // Online results never offer a rematch — see BattleResultUiState.canTryAgain.
        if (state.canTryAgain) {
            AppButton(
                text = "Try again",
                onClick = viewModel::onTryAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
                    .semantics { contentDescription = "Try again" }
            )
        }
        AppOutlinedButton(
            text = "Pick another tech",
            onClick = {
                viewModel.onPickAnother()
                onClose()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (state.canTryAgain) 8.dp else 32.dp)
                .semantics { contentDescription = "Pick another tech" }
        )
    }
}
