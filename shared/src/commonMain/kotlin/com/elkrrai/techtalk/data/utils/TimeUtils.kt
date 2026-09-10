package com.elkrrai.techtalk.data.utils

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Single clock source for the data layer — every persisted timestamp goes through this. */
@OptIn(ExperimentalTime::class)
fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
