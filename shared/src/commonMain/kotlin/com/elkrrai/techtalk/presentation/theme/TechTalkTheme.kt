package com.elkrrai.techtalk.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand colors
private val BrandPrimary = Color(0xFF00F0FF)
private val BrandPrimaryDark = Color(0xFF80F8FF)
private val BrandPrimaryContainer = Color(0xFF00A8B5)
private val BrandSecondary = Color(0xFF03DAC6)
private val PrimaryContainer = Color(0xFF00A8B5)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimaryContainer,
    onPrimary = Color.White,
    primaryContainer = BrandPrimary,
    onPrimaryContainer = PrimaryContainer,
    secondary = BrandSecondary,
    onSecondary = Color(0xFF00201C),
    background = Color(0xFFFAFDFD),
    onBackground = Color(0xFF191C1C),
    surface = Color(0xFFFAFDFD),
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDAE5E5),
    onSurfaceVariant = Color(0xFF3F4949),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF00363A),
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = Color(0xFFEDE0FF),
    secondary = BrandSecondary,
    onSecondary = Color(0xFF00382F),
    background = Color(0xFF0E1414),
    onBackground = Color(0xFFDDE4E3),
    surface = Color(0xFF0E1414),
    onSurface = Color(0xFFDDE4E3),
    surfaceVariant = Color(0xFF3F4949),
    onSurfaceVariant = Color(0xFFBEC9C8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

// Always-dark code block palette (used regardless of app theme).
val CodeHeaderBackground = Color(0xFF1E1E1E)
val CodeBodyBackground = Color(0xFF252526)
val CodeTextColor = Color(0xFFD4D4D4)
val CodeMutedColor = Color(0xFF6A6A6A)
val SuccessColor = Color(0xFF27C93F)
val SunLight = Color(0xFFEED50B)

@Composable
fun TechTalkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}
