package com.elkrrai.techtalk.presentation.getmorecontent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.presentation.component.AppButton
import com.elkrrai.techtalk.presentation.component.AppOutlinedButton
import com.elkrrai.techtalk.presentation.component.Close
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
    val uriHandler = LocalUriHandler.current

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
                title = { Text("Get More Content") },
                navigationIcon = {
                    Close(onClose)
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (state.message != null) {
                    Text(
                        text = state.message.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (state.isBusy) {
                    CircularProgressIndicator()
                }
            }

            item {
                ContentSection(
                    title = "Tips",
                    subtitle = "Import-export TipPack JSON files"
                ) {
                    AppButton(
                        text = "Export all tips",
                        enabled = !state.isBusy,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = viewModel::onExportAllTips,
                    )
                    JsonFilePicker(label = "Import tips from .json") { content ->
                        if (content != null) viewModel.onImportTipPackJson(content)
                    }
                }
            }

            item {
                ContentSection(title = "Battles", subtitle = "Question/answer import-export") {
                    AppButton(
                        text = "Export all Battles",
                        enabled = !state.isBusy,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = viewModel::onExportAllBattles
                    )
                    JsonFilePicker(label = "Import Battles") { content ->
                        if (content != null) viewModel.onImportBattlePackJson(content)
                    }
                }
            }

            item {
                ContentSection(
                    title = "Generate with AI",
                    subtitle = "Opens Claude or ChatGPT with a prepared prompt"
                ) {
                    AppOutlinedButton(
                        text = "Generate tips prompt (Claude)",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { uriHandler.openUri(claudeUrl(buildTipsPrompt())) }
                    )
                    AppOutlinedButton(
                        text = "Generate tips prompt (ChatGPT)",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { uriHandler.openUri(chatGptUrl(buildTipsPrompt())) }
                    )
                    AppOutlinedButton(
                        text = "Generate Battles prompt (Claude)",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { uriHandler.openUri(claudeUrl(buildBattlePrompt())) }
                    )
                    AppOutlinedButton(
                        text = "Generate Battles prompt (ChatGPT)",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { uriHandler.openUri(chatGptUrl(buildBattlePrompt())) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentSection(title: String, subtitle: String, content: @Composable () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

/** claude.ai's `/new` route accepts `q` to pre-fill the composer of a fresh chat. */
private fun claudeUrl(prompt: String): String = "https://claude.ai/new?q=${prompt.urlEncode()}"

/** chatgpt.com accepts `q` on its root route to pre-fill the composer. */
private fun chatGptUrl(prompt: String): String = "https://chatgpt.com/?q=${prompt.urlEncode()}"

/** Percent-encodes over raw UTF-8 bytes — no java.net.URLEncoder equivalent in common Kotlin. */
private fun String.urlEncode(): String = buildString {
    for (byte in encodeToByteArray()) {
        val b = byte.toInt() and 0xFF
        val isUnreserved = b in 'A'.code..'Z'.code ||
                b in 'a'.code..'z'.code ||
                b in '0'.code..'9'.code ||
                b == '-'.code || b == '_'.code || b == '.'.code || b == '~'.code
        if (isUnreserved) {
            append(b.toChar())
        } else {
            append('%')
            append(b.toString(16).padStart(2, '0').uppercase())
        }
    }
}
