package com.elkrrai.techtalk.presentation.userprofile.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.elkrrai.techtalk.presentation.theme.SuccessColor
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

    DisposableEffect(Unit) {
        onDispose { viewModel.saveName() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = { IconButton(onClick = onClose) { Text("✕") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            LoadingScreen(modifier = Modifier.padding(padding).fillMaxSize())
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderCard()

            LevelCard()

            AvatarSection()

            BattleSummaryCard(
                battlesPlayed = state.battleSummary.battlesPlayed,
                winRate = state.battleSummary.winRate,
                bestStreak = state.battleSummary.bestWinStreak
            )

            Text("Recent battles", style = MaterialTheme.typography.titleSmall)

            items(state.recentBattles, key = { it.id }) { battle ->
                BattleHistoryItem(battle)
            }
        }
    }
}

@Composable
private fun HeaderCard() {
    ElevatedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = AvatarCatalog.emojiFor(state.selectedAvatarKey),
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

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
        }
    }
}

@Composable
private fun LevelCard() {
    ElevatedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text("Level ${state.level}", style = MaterialTheme.typography.titleMedium)
            AppProgressBar(
                progress = state.xpProgressFraction,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = "${state.currentXp} / ${state.xpToNextLevel} XP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AvatarSection() {
    ElevatedCard {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
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
private fun BattleSummaryCard(battlesPlayed: Int, winRate: Int, bestStreak: Int) {
    ElevatedCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatColumn(label = "Battles", value = battlesPlayed.toString())
            StatColumn(label = "Win rate", value = "$winRate%")
            StatColumn(label = "Best streak", value = bestStreak.toString())
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BattleHistoryItem(battle: BattleHistoryItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
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
                    text = battle.title(),
                    style = MaterialTheme.typography.labelSmall,
                    color = battle.color(),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "+ ${battle.xpGained} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun BattleHistoryItem.title() = when (this.status) {
    BattleStatus.WIN -> "Win"
    BattleStatus.LOSS -> "Loss"
    BattleStatus.RESIGNED -> "Resigned"
}

@Composable
private fun BattleHistoryItem.color() = when (this.status) {
    BattleStatus.WIN -> SuccessColor
    BattleStatus.LOSS -> MaterialTheme.colorScheme.error
    BattleStatus.RESIGNED -> MaterialTheme.colorScheme.onSurfaceVariant
}
