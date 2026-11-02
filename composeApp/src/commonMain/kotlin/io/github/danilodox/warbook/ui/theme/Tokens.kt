package io.github.danilodox.warbook.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Tokens base do Material 3 (paleta padrão). O visual final do Warbook entra na v0.6.0.

internal object LightTokens {
    val primary = Color(0xFF6750A4)
    val onPrimary = Color(0xFFFFFFFF)
    val primaryContainer = Color(0xFFEADDFF)
    val onPrimaryContainer = Color(0xFF21005D)
    val secondary = Color(0xFF625B71)
    val onSecondary = Color(0xFFFFFFFF)
    val secondaryContainer = Color(0xFFE8DEF8)
    val onSecondaryContainer = Color(0xFF1D192B)
    val tertiary = Color(0xFF7D5260)
    val onTertiary = Color(0xFFFFFFFF)
    val tertiaryContainer = Color(0xFFFFD8E4)
    val onTertiaryContainer = Color(0xFF31111D)
}

internal object DarkTokens {
    val primary = Color(0xFFD0BCFF)
    val onPrimary = Color(0xFF381E72)
    val primaryContainer = Color(0xFF4F378B)
    val onPrimaryContainer = Color(0xFFEADDFF)
    val secondary = Color(0xFFCCC2DC)
    val onSecondary = Color(0xFF332D41)
    val secondaryContainer = Color(0xFF4A4458)
    val onSecondaryContainer = Color(0xFFE8DEF8)
    val tertiary = Color(0xFFEFB8C8)
    val onTertiary = Color(0xFF492532)
    val tertiaryContainer = Color(0xFF633B48)
    val onTertiaryContainer = Color(0xFFFFD8E4)
}

/** Espaçamentos padrão das telas. */
object Spacing {
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
}
