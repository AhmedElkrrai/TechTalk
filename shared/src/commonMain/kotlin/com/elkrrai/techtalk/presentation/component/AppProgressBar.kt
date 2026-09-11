package com.elkrrai.techtalk.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun AppProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    animateOnEachChange: Boolean = false
) {
    val target = progress.coerceIn(0f, 1f)
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(target) {
        if (animateOnEachChange) {
            animatedProgress.snapTo(0f)
        }

        animatedProgress.animateTo(
            targetValue = target,
            animationSpec = tween(900)
        )
    }
    LinearProgressIndicator(
        progress = { animatedProgress.value },
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
        drawStopIndicator = {}
    )
}
