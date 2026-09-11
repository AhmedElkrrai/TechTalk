package com.elkrrai.techtalk.presentation.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.elkrrai.techtalk.domain.model.common.Difficulty
import com.elkrrai.techtalk.domain.model.common.getTitle
import com.elkrrai.techtalk.presentation.theme.SunLight

@Composable
fun getDifficultyTitleAndColor(difficulty: Difficulty): Pair<String, Color> {
    val color = when (difficulty) {
        Difficulty.BEGINNER -> MaterialTheme.colorScheme.onPrimary
        Difficulty.INTERMEDIATE -> MaterialTheme.colorScheme.secondary
        Difficulty.ADVANCED -> MaterialTheme.colorScheme.error
        Difficulty.RANDOM -> SunLight
    }
    return difficulty.getTitle() to color
}
