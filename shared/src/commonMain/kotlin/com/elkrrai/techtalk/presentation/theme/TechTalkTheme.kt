package com.elkrrai.techtalk.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrandPrimary = Color(0xFF00F0FF)
private val BrandPrimaryDark = Color(0xFF80F8FF)
private val BrandPrimaryContainer = Color(0xFF00A8B5)
private val BrandSecondary = Color(0xFF03DAC6)
private val BrandTertiary = Color(0xFFD500F9)
private val BrandTertiaryDark = Color(0xFFEA80FC)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color(0xFF00363A),
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = Color(0xFFF0FFFF),
    secondary = BrandSecondary,
    onSecondary = Color(0xFF00201C),
    secondaryContainer = Color(0xFFB3F5EC),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = BrandTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF8D9FF),
    onTertiaryContainer = Color(0xFF4A0060),
    background = Color(0xFFFAFDFD),
    onBackground = Color(0xFF191C1C),
    surface = Color(0xFFFAFDFD),
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDAE5E5),
    onSurfaceVariant = Color(0xFF5A5A5A),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF00363A),
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = Color(0xFFCFFAFF),
    secondary = BrandSecondary,
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFF7FF5E4),
    tertiary = BrandTertiaryDark,
    onTertiary = Color(0xFF4A0060),
    tertiaryContainer = Color(0xFF7A00A3),
    onTertiaryContainer = Color(0xFFF8D9FF),
    background = Color(0xFF0E1414),
    onBackground = Color(0xFFDDE4E3),
    surface = Color(0xFF0E1414),
    onSurface = Color(0xFFDDE4E3),
    surfaceVariant = Color(0xFF3F4949),
    onSurfaceVariant = Color(0xFFA0A0A0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

// Always-dark code block palette (used regardless of app theme).
val CodeHeaderBackground = Color(0xFF1E1E1E)
val CodeBodyBackground = Color(0xFF252526)
val CodeTextColor = Color(0xFFD4D4D4)
val CodeMutedColor = Color(0xFF6A6A6A)
val SuccessColor = Color(0xFF27C93F)

/** The tertiary accent — used wherever a badge/icon needs to stand out from the
 * cyan/teal pair (e.g. the "Random" difficulty badge, the lit "interested" icon). */
val NeonAccent = BrandTertiary

@Composable
fun TechTalkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}
