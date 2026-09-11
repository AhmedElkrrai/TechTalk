package com.elkrrai.techtalk.presentation.battle.online

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineMatchRole
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineMatchStage
import com.elkrrai.techtalk.presentation.component.AppButton
import com.elkrrai.techtalk.presentation.component.LoadingScreen

@Composable
fun BattleLobbyScreen(viewModel: BattleLobbyViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${state.selectedTech} · ${state.selectedTimeLabel} · ${state.selectedDifficultyLabel}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        when (state.stage) {
            OnlineMatchStage.ROLE_SELECTION -> {
                Text("Play online", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
                AppButton(
                    text = "Host a room",
                    onClick = {
                        viewModel.onRoleSelected(OnlineMatchRole.HOST)
                        viewModel.onCreateRoom()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                )
                Text("or", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp))
                OutlinedTextField(
                    value = state.roomCodeInput,
                    onValueChange = viewModel::onRoomCodeInputChanged,
                    label = { Text("Room code") },
                    modifier = Modifier.fillMaxWidth()
                )
                AppButton(
                    text = "Join room",
                    onClick = {
                        viewModel.onRoleSelected(OnlineMatchRole.JOIN)
                        viewModel.onJoinRoom()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }

            OnlineMatchStage.CREATING_ROOM, OnlineMatchStage.JOINING_ROOM -> {
                LoadingScreen(modifier = Modifier.padding(top = 24.dp))
            }

            OnlineMatchStage.WAITING_FOR_PLAYER -> {
                Text("Waiting for an opponent…", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                if (state.roomCode.isNotBlank()) {
                    Text(
                        text = state.roomCode,
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "Share this code with your opponent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AppButton(text = "Cancel", onClick = viewModel::onLeaveRoom, modifier = Modifier.padding(top = 24.dp))
            }

            OnlineMatchStage.CONNECTED -> {
                LoadingScreen(modifier = Modifier.padding(top = 24.dp))
                Text("Starting match…", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
