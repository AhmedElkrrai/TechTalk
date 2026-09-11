package com.elkrrai.techtalk.presentation.component

import androidx.compose.runtime.Composable
import com.elkrrai.techtalk.domain.model.common.Difficulty

@Composable
fun DifficultyBadge(difficulty: Difficulty) {
    val (title, color) = getDifficultyTitleAndColor(difficulty)
    Badge(title, color)
}
