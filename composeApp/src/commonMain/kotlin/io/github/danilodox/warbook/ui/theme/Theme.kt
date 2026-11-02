package io.github.danilodox.warbook.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors: ColorScheme = lightColorScheme(
    primary = LightTokens.primary,
    onPrimary = LightTokens.onPrimary,
    primaryContainer = LightTokens.primaryContainer,
    onPrimaryContainer = LightTokens.onPrimaryContainer,
    secondary = LightTokens.secondary,
    onSecondary = LightTokens.onSecondary,
    secondaryContainer = LightTokens.secondaryContainer,
    onSecondaryContainer = LightTokens.onSecondaryContainer,
    tertiary = LightTokens.tertiary,
    onTertiary = LightTokens.onTertiary,
    tertiaryContainer = LightTokens.tertiaryContainer,
    onTertiaryContainer = LightTokens.onTertiaryContainer,
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = DarkTokens.primary,
    onPrimary = DarkTokens.onPrimary,
    primaryContainer = DarkTokens.primaryContainer,
    onPrimaryContainer = DarkTokens.onPrimaryContainer,
    secondary = DarkTokens.secondary,
    onSecondary = DarkTokens.onSecondary,
    secondaryContainer = DarkTokens.secondaryContainer,
    onSecondaryContainer = DarkTokens.onSecondaryContainer,
    tertiary = DarkTokens.tertiary,
    onTertiary = DarkTokens.onTertiary,
    tertiaryContainer = DarkTokens.tertiaryContainer,
    onTertiaryContainer = DarkTokens.onTertiaryContainer,
)

@Composable
fun WarbookTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        shapes = Shapes(),
        content = content,
    )
}
