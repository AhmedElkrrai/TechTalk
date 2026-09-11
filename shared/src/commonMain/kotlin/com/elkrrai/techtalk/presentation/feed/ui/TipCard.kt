package com.elkrrai.techtalk.presentation.feed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.domain.model.tip.FeedTip
import com.elkrrai.techtalk.presentation.component.Badge
import com.elkrrai.techtalk.presentation.component.DifficultyBadge
import com.elkrrai.techtalk.presentation.theme.CodeBodyBackground
import com.elkrrai.techtalk.presentation.theme.CodeHeaderBackground
import com.elkrrai.techtalk.presentation.theme.CodeTextColor
import com.elkrrai.techtalk.presentation.theme.SunLight
import com.elkrrai.techtalk.utils.parseHexColor
import kotlinx.coroutines.delay

@Composable
fun TipCard(
    tip: FeedTip,
    isInterested: Boolean,
    onToggleInterested: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge(
                    title = tip.technologyName,
                    color = parseHexColor(tip.technologyTagColor)
                )
                Badge(
                    title = tip.topicName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DifficultyBadge(tip.difficulty)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = tip.title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = tip.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
            )

            val snippet = tip.codeSnippet
            if (!snippet.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                CodeSnippet(tip, snippet)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Icon(
                imageVector = if (isInterested) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                modifier = Modifier.clickable(onClick = onToggleInterested),
                tint = if (isInterested) SunLight
                else MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = "Interesting",
            )
        }
    }
}

@Composable
private fun CodeSnippet(tip: FeedTip, snippet: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CodeHeaderBackground)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tip.codeLang ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = CodeTextColor
            )

            val clipboardManager = LocalClipboardManager.current
            var isCopied by remember { mutableStateOf(false) }
            LaunchedEffect(isCopied) {
                if (isCopied) {
                    delay(1500)
                    isCopied = false
                }
            }
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(snippet))
                    isCopied = true
                }
            ) {
                Icon(
                    imageVector = if (isCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = "Copy code",
                    tint = CodeTextColor
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().background(CodeBodyBackground)
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = snippet,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = CodeTextColor,
                softWrap = false
            )
        }
    }
}
