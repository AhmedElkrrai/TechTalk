package com.elkrrai.techtalk.presentation.getmorecontent.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.presentation.component.AppButton
import com.elkrrai.techtalk.presentation.feed.ui.JsonFilePicker
import com.elkrrai.techtalk.presentation.getmorecontent.GetMoreContentViewModel
import com.elkrrai.techtalk.utils.buildBattlePrompt
import com.elkrrai.techtalk.utils.buildTipsPrompt
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GetMoreContentScreen(
    viewModel: GetMoreContentViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var tipPackJson by remember { mutableStateOf("") }
    var battlePackJson by remember { mutableStateOf("") }

    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(4000)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Get more content") },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.semantics { contentDescription = "Close" }
                    ) { Text("✕") }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            item {
                if (state.message != null) {
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Text(state.message.orEmpty(), modifier = Modifier.padding(12.dp))
                    }
                }
                if (state.isBusy) {
                    CircularProgressIndicator(modifier = Modifier.padding(bottom = 12.dp))
                }

                Text("Tip pack", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Ask an LLM to write a pack using the prompt below, then paste the JSON here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PromptBlock(buildTipsPrompt())
                OutlinedTextField(
                    value = tipPackJson,
                    onValueChange = { tipPackJson = it },
                    label = { Text("Tip pack JSON") },
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
                AppButton(
                    text = "Import tip pack",
                    enabled = tipPackJson.isNotBlank() && !state.isBusy,
                    onClick = { viewModel.onImportTipPackJson(tipPackJson) }
                )
                JsonFilePicker { content -> if (content != null) viewModel.onImportTipPackJson(content) }
                AppButton(text = "Export all tips by topic", enabled = !state.isBusy, onClick = viewModel::onExportAllTips)

                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

                Text("Battle pack", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Same idea, for quiz questions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PromptBlock(buildBattlePrompt())
                OutlinedTextField(
                    value = battlePackJson,
                    onValueChange = { battlePackJson = it },
                    label = { Text("Battle pack JSON") },
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
                AppButton(
                    text = "Import battle pack",
                    enabled = battlePackJson.isNotBlank() && !state.isBusy,
                    onClick = { viewModel.onImportBattlePackJson(battlePackJson) }
                )
                JsonFilePicker { content -> if (content != null) viewModel.onImportBattlePackJson(content) }
                AppButton(text = "Export all battles by tech", enabled = !state.isBusy, onClick = viewModel::onExportAllBattles)
            }
        }
    }
}

@Composable
private fun PromptBlock(prompt: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = prompt,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp)
        )
    }
}
