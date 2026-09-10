package com.elkrrai.techtalk.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elkrrai.techtalk.domain.model.user.AvatarCatalog

@Composable
fun BattlePlayerChip(
    name: String,
    avatarKey: String,
    score: Int,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    Row(
        modifier = modifier
            .background(
                if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(AvatarCatalog.emojiFor(avatarKey))
        Text(name, style = MaterialTheme.typography.labelLarge)
        Text("•", style = MaterialTheme.typography.labelLarge)
        Text(score.toString(), style = MaterialTheme.typography.labelLarge)
    }
}
