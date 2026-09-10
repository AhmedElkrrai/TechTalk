package com.elkrrai.techtalk.utils

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun formatRelativeTime(epochMillis: Long): String {
    val nowMillis = Clock.System.now().toEpochMilliseconds()
    val deltaMillis = (nowMillis - epochMillis).coerceAtLeast(0)

    val minutes = deltaMillis / 60_000
    val hours = deltaMillis / 3_600_000
    val days = deltaMillis / 86_400_000

    return when {
        minutes < 1 -> "Just now"
        hours < 1 -> "${minutes}m ago"
        days < 1 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}
