package com.elkrrai.techtalk.presentation.userprofile.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.domain.model.battle.BattleHistoryItem
import com.elkrrai.techtalk.domain.model.battle.BattleStatus
import com.elkrrai.techtalk.domain.model.user.AvatarCatalog
import com.elkrrai.techtalk.domain.model.user.AvatarOption
import com.elkrrai.techtalk.presentation.component.AppProgressBar
import com.elkrrai.techtalk.presentation.component.LoadingScreen
import com.elkrrai.techtalk.presentation.userprofile.UserProfileViewModel
import com.elkrrai.techtalk.utils.formatRelativeTime
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(3000)
            viewModel.onMessageShown()
        }
    }

    DisposableEffect(Unit){
        onDispose { viewModel.saveName() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = { IconButton(onClick = onClose) { Text("✕") } }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            LoadingScreen(modifier = Modifier.padding(padding).fillMaxSize())
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = AvatarCatalog.emojiFor(state.selectedAvatarKey),
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChanged,
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (state.message != null) {
                        Text(
                            text = state.message.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("Level ${state.level}", style = MaterialTheme.typography.titleMedium)
                    AppProgressBar(progress = state.xpProgressFraction, modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "${state.currentXp} / ${state.xpToNextLevel} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    Text("Avatar", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.avatars.forEach { avatar ->
                            AvatarOptionChip(
                                avatar = avatar,
                                isSelected = avatar.key == state.selectedAvatarKey,
                                onClick = { viewModel.onAvatarSelected(avatar.key) }
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    BattleSummaryRow(
                        battlesPlayed = state.battleSummary.battlesPlayed,
                        winRate = state.battleSummary.winRate,
                        bestStreak = state.battleSummary.bestWinStreak
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Recent battles", style = MaterialTheme.typography.titleSmall)
                }
            }

            items(state.recentBattles, key = { it.id }) { battle ->
                BattleHistoryRow(battle)
            }
        }
    }
}

@Composable
private fun AvatarOptionChip(avatar: AvatarOption, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = avatar.emoji,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            )
            .padding(8.dp)
    )
}

@Composable
private fun BattleSummaryRow(battlesPlayed: Int, winRate: Int, bestStreak: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        StatColumn(label = "Battles", value = battlesPlayed.toString())
        StatColumn(label = "Win rate", value = "$winRate%")
        StatColumn(label = "Best streak", value = bestStreak.toString())
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BattleHistoryRow(battle: BattleHistoryItem) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(battle.technologyName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = formatRelativeTime(battle.playedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${battle.score}/${battle.totalQuestions}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = when (battle.status) {
                        BattleStatus.WIN -> "Win"
                        BattleStatus.LOSS -> "Loss"
                        BattleStatus.RESIGNED -> "Resigned"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (battle.status == BattleStatus.WIN) {
                        com.elkrrai.techtalk.presentation.theme.SuccessColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
