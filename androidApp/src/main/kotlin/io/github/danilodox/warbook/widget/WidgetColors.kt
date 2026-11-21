package io.github.danilodox.warbook.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import io.github.danilodox.warbook.data.ThemeMode
import io.github.danilodox.warbook.ui.theme.DarkTokens
import io.github.danilodox.warbook.ui.theme.LightTokens

/**
 * Cores do widget, tiradas dos mesmos tokens do tema do app ([LightTokens] e [DarkTokens]): mudou o tema,
 * o widget muda junto. Segue o tema escolhido nas Configurações; em [ThemeMode.SYSTEM], o do aparelho.
 */
internal class WidgetColors(mode: ThemeMode) {
    private val pick: (Color, Color) -> ColorProvider = when (mode) {
        ThemeMode.DARK -> { _, dark -> ColorProvider(dark) }
        ThemeMode.LIGHT -> { light, _ -> ColorProvider(light) }
        ThemeMode.SYSTEM -> { light, dark -> ColorProvider(day = light, night = dark) }
    }

    val background = pick(LightTokens.surfaceContainer, DarkTokens.surfaceContainer)
    val ink = pick(LightTokens.onSurface, DarkTokens.onSurface)
    val muted = pick(LightTokens.onSurfaceVariant, DarkTokens.onSurfaceVariant)
    val gold = pick(LightTokens.primary, DarkTokens.primary)
    val war = pick(LightTokens.tertiary, DarkTokens.tertiary)
}
