package com.elkrrai.techtalk.presentation.feed.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.presentation.component.AppButton

/** Shown when a filter combination matches zero tips. */
@Composable
fun FilterEmptyState(onClearFilters: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🔍", style = MaterialTheme.typography.displayMedium)
        Text(
            text = "No tips match your filters",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        AppButton(text = "Clear filters", onClick = onClearFilters)
    }
}
