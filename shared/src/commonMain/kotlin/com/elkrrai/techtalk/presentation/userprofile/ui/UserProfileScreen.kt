package com.elkrrai.techtalk.presentation.userprofile.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.domain.model.battle.BattleHistoryItem
import com.elkrrai.techtalk.domain.model.battle.BattleStatus
import com.elkrrai.techtalk.domain.model.user.AvatarCatalog
import com.elkrrai.techtalk.domain.model.user.AvatarOption
import com.elkrrai.techtalk.presentation.component.AppProgressBar
import com.elkrrai.techtalk.presentation.component.LoadingScreen
import com.elkrrai.techtalk.presentation.theme.SuccessColor
import com.elkrrai.techtalk.presentation.userprofile.UserProfileViewModel
import com.elkrrai.techtalk.presentation.userprofile.state.UserProfileState
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
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.semantics { contentDescription = "Close" }
                    ) { Text("✕") }
                },
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
            HeaderCard(
                selectedAvatarKey = state.selectedAvatarKey,
                name = state.name,
                onNameChanged = viewModel::onNameChanged,
                message = state.message
            )

            LevelCard(
                level = state.level,
                xpProgressFraction = state.xpProgressFraction,
                currentXp = state.currentXp,
                xpToNextLevel = state.xpToNextLevel
            )

            AvatarSection(
                avatars = state.avatars,
                selectedAvatarKey = state.selectedAvatarKey,
                onAvatarSelected = viewModel::onAvatarSelected
            )

            BattleSummaryCard(
                battlesPlayed = state.battleSummary.battlesPlayed,
                winRate = state.battleSummary.winRate,
                bestStreak = state.battleSummary.bestWinStreak
            )

            RecentBattles(state)
        }
    }
}

@Composable
private fun HeaderCard(
    selectedAvatarKey: String,
    name: String,
    onNameChanged: (String) -> Unit,
    message: String?
) {
    ContentSection {
        Text(
            text = AvatarCatalog.emojiFor(selectedAvatarKey),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (message != null) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun LevelCard(
    level: Int,
    xpProgressFraction: Float,
    currentXp: Int,
    xpToNextLevel: Int
) {
    ContentSection {
        Title("Level $level")

        Text(
            text = "$currentXp / $xpToNextLevel XP",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AppProgressBar(progress = xpProgressFraction)
    }
}

@Composable
private fun AvatarSection(
    avatars: List<AvatarOption>,
    selectedAvatarKey: String,
    onAvatarSelected: (String) -> Unit
) {
    Title("Avatar")

    ContentSection {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            avatars.forEach { avatar ->
                AvatarOptionChip(
                    avatar = avatar,
                    isSelected = avatar.key == selectedAvatarKey,
                    onClick = { onAvatarSelected(avatar.key) }
                )
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
    Title("Battle summary")

    ContentSection {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecentBattles(state: UserProfileState) {
    Title("Recent battles")

    ContentSection {
        state.recentBattles.forEachIndexed { index, battle ->
            BattleHistoryItem(battle)
            if (index != state.recentBattles.size - 1) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun BattleHistoryItem(battle: BattleHistoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center
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

@Composable
private fun ContentSection(
    content: @Composable () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
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
