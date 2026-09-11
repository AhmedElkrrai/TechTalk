package com.elkrrai.techtalk.presentation.feed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.domain.model.tip.FeedTip
import com.elkrrai.techtalk.presentation.component.DifficultyBadge
import com.elkrrai.techtalk.presentation.component.TechnologyBadge
import com.elkrrai.techtalk.presentation.component.getDifficultyTitleAndColor
import com.elkrrai.techtalk.presentation.theme.CodeBodyBackground
import com.elkrrai.techtalk.presentation.theme.CodeHeaderBackground
import com.elkrrai.techtalk.presentation.theme.CodeTextColor
import com.elkrrai.techtalk.utils.parseHexColor

@Composable
fun TipCard(
    tip: FeedTip,
    isInterested: Boolean,
    onToggleInterested: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TechnologyBadge(
                    title =tip.technologyName,
                    color = parseHexColor(tip.technologyTagColor)
                )
                FilledPill(text = tip.topicName)
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
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(CodeHeaderBackground, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tip.codeLang ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = CodeTextColor
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().background(CodeBodyBackground)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = snippet,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = CodeTextColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isInterested) "❤️" else "🤍",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.clickable(onClick = onToggleInterested).padding(8.dp)
                )
                Text(
                    text = "${tip.interestCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Colored-outline pill — used for technology and difficulty badges. */
@Composable
private fun OutlinedPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = modifier
            .wrapContentWidth()
            .border(width = 1.dp, color = color, shape = RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** Neutral filled pill — used for the topic badge. */
@Composable
private fun FilledPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .wrapContentWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
