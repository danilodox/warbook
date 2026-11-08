package io.github.danilodox.warbook.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Cores fora do M3. `success` marca quem está à frente no Comparar. */
@Immutable
data class WarbookColors(val success: Color, val successContainer: Color)

internal val DarkColors: ColorScheme = darkColorScheme(
    primary = DarkTokens.primary,
    onPrimary = DarkTokens.onPrimary,
    primaryContainer = DarkTokens.primaryContainer,
    onPrimaryContainer = DarkTokens.onPrimaryContainer,
    inversePrimary = DarkTokens.inversePrimary,
    secondary = DarkTokens.secondary,
    onSecondary = DarkTokens.onSecondary,
    secondaryContainer = DarkTokens.secondaryContainer,
    onSecondaryContainer = DarkTokens.onSecondaryContainer,
    tertiary = DarkTokens.tertiary,
    onTertiary = DarkTokens.onTertiary,
    tertiaryContainer = DarkTokens.tertiaryContainer,
    onTertiaryContainer = DarkTokens.onTertiaryContainer,
    background = DarkTokens.surface,
    onBackground = DarkTokens.onSurface,
    surface = DarkTokens.surface,
    onSurface = DarkTokens.onSurface,
    surfaceVariant = DarkTokens.surfaceContainerHighest,
    onSurfaceVariant = DarkTokens.onSurfaceVariant,
    surfaceTint = DarkTokens.primary,
    inverseSurface = DarkTokens.inverseSurface,
    inverseOnSurface = DarkTokens.inverseOnSurface,
    error = DarkTokens.error,
    onError = DarkTokens.onError,
    errorContainer = DarkTokens.errorContainer,
    onErrorContainer = DarkTokens.onErrorContainer,
    outline = DarkTokens.outline,
    outlineVariant = DarkTokens.outlineVariant,
    surfaceBright = DarkTokens.surfaceContainerHighest,
    surfaceDim = DarkTokens.surface,
    surfaceContainerLowest = DarkTokens.surfaceContainerLowest,
    surfaceContainerLow = DarkTokens.surfaceContainerLow,
    surfaceContainer = DarkTokens.surfaceContainer,
    surfaceContainerHigh = DarkTokens.surfaceContainerHigh,
    surfaceContainerHighest = DarkTokens.surfaceContainerHighest,
)

internal val LightColors: ColorScheme = lightColorScheme(
    primary = LightTokens.primary,
    onPrimary = LightTokens.onPrimary,
    primaryContainer = LightTokens.primaryContainer,
    onPrimaryContainer = LightTokens.onPrimaryContainer,
    inversePrimary = LightTokens.inversePrimary,
    secondary = LightTokens.secondary,
    onSecondary = LightTokens.onSecondary,
    secondaryContainer = LightTokens.secondaryContainer,
    onSecondaryContainer = LightTokens.onSecondaryContainer,
    tertiary = LightTokens.tertiary,
    onTertiary = LightTokens.onTertiary,
    tertiaryContainer = LightTokens.tertiaryContainer,
    onTertiaryContainer = LightTokens.onTertiaryContainer,
    background = LightTokens.surface,
    onBackground = LightTokens.onSurface,
    surface = LightTokens.surface,
    onSurface = LightTokens.onSurface,
    surfaceVariant = LightTokens.surfaceContainerHighest,
    onSurfaceVariant = LightTokens.onSurfaceVariant,
    surfaceTint = LightTokens.primary,
    inverseSurface = LightTokens.inverseSurface,
    inverseOnSurface = LightTokens.inverseOnSurface,
    error = LightTokens.error,
    onError = LightTokens.onError,
    errorContainer = LightTokens.errorContainer,
    onErrorContainer = LightTokens.onErrorContainer,
    outline = LightTokens.outline,
    outlineVariant = LightTokens.outlineVariant,
    surfaceBright = LightTokens.surfaceContainerLowest,
    surfaceDim = LightTokens.surfaceContainerHighest,
    surfaceContainerLowest = LightTokens.surfaceContainerLowest,
    surfaceContainerLow = LightTokens.surfaceContainerLow,
    surfaceContainer = LightTokens.surfaceContainer,
    surfaceContainerHigh = LightTokens.surfaceContainerHigh,
    surfaceContainerHighest = LightTokens.surfaceContainerHighest,
)

internal val DarkExtras = WarbookColors(DarkTokens.success, DarkTokens.successContainer)
internal val LightExtras = WarbookColors(LightTokens.success, LightTokens.successContainer)

internal val WarbookShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.extraSmall),
    small = RoundedCornerShape(Radius.small),
    medium = RoundedCornerShape(Radius.medium),
    large = RoundedCornerShape(Radius.large),
    extraLarge = RoundedCornerShape(Radius.extraLarge),
)

private val LocalWarbookColors = staticCompositionLocalOf { DarkExtras }
private val LocalWarbookTextStyles = staticCompositionLocalOf<WarbookTextStyles> {
    error("WarbookTextStyles fora do WarbookTheme")
}

/** Acesso ao que o Warbook acrescenta ao M3: `WarbookTheme.colors.success`, `WarbookTheme.text.tag`. */
object WarbookTheme {
    val colors: WarbookColors
        @Composable @ReadOnlyComposable
        get() = LocalWarbookColors.current

    val text: WarbookTextStyles
        @Composable @ReadOnlyComposable
        get() = LocalWarbookTextStyles.current
}

/** Tema do app. Escuro é o padrão; quem decide é a preferência das Configurações (ver `ThemeMode`). */
@Composable
fun WarbookTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val (typography, textStyles) = rememberWarbookTypography()
    CompositionLocalProvider(
        LocalWarbookColors provides if (darkTheme) DarkExtras else LightExtras,
        LocalWarbookTextStyles provides textStyles,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = typography,
            shapes = WarbookShapes,
            content = content,
        )
    }
}
