package com.elkrrai.techtalk.presentation.battle.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.common.getTitle
import com.elkrrai.techtalk.domain.model.online.BattleTimeControl
import com.elkrrai.techtalk.domain.model.user.AvatarCatalog
import com.elkrrai.techtalk.presentation.battle.home.state.BattleMode
import com.elkrrai.techtalk.presentation.battle.state.getLabel
import com.elkrrai.techtalk.presentation.component.AppButton
import com.elkrrai.techtalk.presentation.component.LoadingScreen

@Composable
fun BattleHomeScreen(viewModel: BattleHomeViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "${AvatarCatalog.emojiFor(state.playerAvatarKey)}  ${state.playerName.ifBlank { "Player" }}",
            style = MaterialTheme.typography.titleLarge
        )

        Text("Mode", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 20.dp))
        SectionRow {
            BattleMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.mode == mode,
                    onClick = { viewModel.onModeSelected(mode) },
                    label = { Text(if (mode == BattleMode.OFFLINE) "Offline" else "Online") }
                )
            }
        }

        Text("Technology", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 20.dp))
        if (state.technologies.isEmpty()) {
            Text(
                text = "Subscribe to a technology first to start a battle.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            SectionRow {
                state.technologies.forEach { tech ->
                    FilterChip(
                        selected = state.selectedTechnology?.id == tech.id,
                        onClick = { viewModel.onTechSelected(tech.id) },
                        label = { Text(tech.name) }
                    )
                }
            }
        }

        Text("Time control", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 20.dp))
        SectionRow {
            BattleTimeControl.entries.forEach { timeControl ->
                FilterChip(
                    selected = state.selectedTimeControl == timeControl,
                    onClick = { viewModel.onTimeSelected(timeControl) },
                    label = { Text(timeControl.getLabel()) }
                )
            }
        }

        Text("Difficulty", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 20.dp))
        SectionRow {
            Difficulty.entries.forEach { difficulty ->
                FilterChip(
                    selected = state.selectedDifficulty == difficulty,
                    onClick = { viewModel.onDifficultySelected(difficulty) },
                    label = { Text(difficulty.getTitle()) }
                )
            }
        }

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        if (state.isStartingBattle) {
            LoadingScreen(modifier = Modifier.padding(top = 24.dp))
        } else {
            AppButton(
                text = "Start battle",
                enabled = state.selectedTechnology != null,
                onClick = viewModel::onStartBattle,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
            )
        }
    }
}

@Composable
private fun SectionRow(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) { content() }
}
