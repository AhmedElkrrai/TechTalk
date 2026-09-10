package com.elkrrai.techtalk.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

fun inspect(msg: Any?, tag: String = "asdf") {
    println("[$tag] $msg")
}

@Composable
fun Inspect(msg: Any?, tag: String = "asdf") {
    LaunchedEffect(msg) { inspect(msg, tag) }
}

fun <T> T.trace(tag: String = "asdf"): T {
    inspect(this, tag)
    return this
}

fun <T> T.trace(label: String, tag: String = "asdf"): T {
    inspect("$label: $this", tag)
    return this
}
