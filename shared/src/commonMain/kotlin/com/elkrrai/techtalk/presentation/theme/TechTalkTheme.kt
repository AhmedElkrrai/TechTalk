package com.elkrrai.techtalk.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF00F0FF)
private val DarkPrimary = Color(0xFF80F8FF)
private val PrimaryContainer = Color(0xFF00A8B5)
private val Teal = Color(0xFF03DAC6)
private val TealDark = Color(0xFF018786)
private val DarkBackground = Color(0xFF121212)
private val DarkSurface = Color(0xFF1E1E1E)
private val DarkSurfaceContainer = Color(0xFF252525)
private val DarkSurfaceContainerHigh = Color(0xFF2D2D2D)

val CodeHeaderBackground = DarkSurfaceContainerHigh
val CodeBodyBackground = DarkSurface
val CodeTextColor = Color(0xFFD4D4D4)
val CodeMutedColor = Color(0xFF9E9E9E)
val SuccessColor = Color(0xFF27C93F)
val SunLight = Color(0xFFEED50B)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE0FF),
    onPrimaryContainer = PrimaryContainer,
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCEFAF5),
    onSecondaryContainer = TealDark,
    tertiary = SuccessColor,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF0ECF4),
    onSurfaceVariant = Color(0xFF5A5A5A),
    error = Color(0xFFE53935),
    surfaceContainerHighest = Color(0xFFF5F5F5)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF81C784),
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Color(0xFFEDE0FF),
    secondary = Teal,
    onSecondary = Color(0xFF003731),
    secondaryContainer = TealDark,
    onSecondaryContainer = Color(0xFFCEFAF5),
    tertiary = Color(0x3E1AF8E5),
    background = DarkBackground,
    onBackground = Color(0xFFE0E0E0),
    surface = DarkSurface,
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = Color(0xFFA0A0A0),
    error = Color(0xFFEF5350),
    surfaceContainerHighest = DarkSurfaceContainerHigh
)

@Composable
fun TechTalkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}
