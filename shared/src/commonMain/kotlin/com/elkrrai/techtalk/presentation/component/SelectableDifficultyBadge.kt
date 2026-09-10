package com.elkrrai.techtalk.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.domain.model.common.Difficulty

@Composable
fun SelectableDifficultyBadge(
    difficulty: Difficulty,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, color) = getDifficultyTitleAndColor(difficulty)
    val backgroundAlpha = if (isSelected) 0.3f else 0.1f
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .clickable(onClick = onClick)
            .background(color.copy(alpha = backgroundAlpha), RoundedCornerShape(50))
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
