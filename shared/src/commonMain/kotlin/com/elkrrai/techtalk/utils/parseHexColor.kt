package com.elkrrai.techtalk.utils

import androidx.compose.ui.graphics.Color
import com.elkrrai.techtalk.presentation.theme.CodeMutedColor

/** Parses a `"#RRGGBB"` / `"#AARRGGBB"` hex string, falling back to [CodeMutedColor] on
 * any parse failure. Used to render per-technology accent colours stored as strings. */
fun parseHexColor(hex: String): Color = runCatching {
    val cleaned = hex.removePrefix("#")
    val colorLong = cleaned.toLong(16)
    when (cleaned.length) {
        6 -> Color(colorLong or 0xFF000000L)
        8 -> Color(colorLong)
        else -> throw IllegalArgumentException("Unexpected hex color length: $hex")
    }
}.getOrDefault(CodeMutedColor)
