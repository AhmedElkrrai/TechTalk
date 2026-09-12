package com.elkrrai.techtalk.presentation.battle.online

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineMatchRole
import com.elkrrai.techtalk.presentation.battle.online.state.OnlineMatchStage
import com.elkrrai.techtalk.presentation.component.AppButton
import com.elkrrai.techtalk.presentation.component.LoadingScreen
import com.elkrrai.techtalk.utils.rememberAnimatedDots
import kotlinx.coroutines.delay

@Composable
fun BattleLobbyScreen(viewModel: BattleLobbyViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
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
                Text(
                    "Play online",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                AppButton(
                    text = "Host a room",
                    onClick = {
                        viewModel.onRoleSelected(OnlineMatchRole.HOST)
                        viewModel.onCreateRoom()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                )
                Text(
                    "or",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
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
                Text(
                    text = "Waiting for an opponent" + rememberAnimatedDots(active = true),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                if (state.roomCode.isNotBlank()) {
                    val clipboardManager = LocalClipboardManager.current
                    var isCopied by remember { mutableStateOf(false) }
                    LaunchedEffect(isCopied) {
                        if (isCopied) {
                            delay(1500)
                            isCopied = false
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(
                            text = state.roomCode,
                            style = MaterialTheme.typography.displayMedium
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(state.roomCode))
                                isCopied = true
                            }
                        ) {
                            Icon(
                                imageVector = if (isCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                contentDescription = "Copy room code"
                            )
                        }
                    }
                    Text(
                        text = if (isCopied) "Copied!" else "Share this code with your opponent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AppButton(
                    text = "Cancel",
                    onClick = viewModel::onLeaveRoom,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            OnlineMatchStage.CONNECTED -> {
                LoadingScreen(modifier = Modifier.padding(top = 24.dp))
                Text("Starting match…", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
