package com.elkrrai.techtalk.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Cycles "." -> ".." -> "..." -> "." while [active], for suffixing onto a loading
 * message (e.g. "Loading specialties" + rememberAnimatedDots(...)). Resets to a
 * single dot as soon as [active] goes false, so a fast success/error doesn't leave a
 * message frozen mid-cycle.
 */
@Composable
fun rememberAnimatedDots(
    active: Boolean,
    maxDotCount: Int = 3,
    stepMillis: Long = 1000
): String {
    var dotCount by remember { mutableIntStateOf(1) }

    LaunchedEffect(active) {
        if (!active) {
            dotCount = 1
            return@LaunchedEffect
        }
        while (true) {
            delay(stepMillis)
            dotCount = if (dotCount >= maxDotCount) 1 else dotCount + 1
        }
    }

    return ".".repeat(dotCount)
}
