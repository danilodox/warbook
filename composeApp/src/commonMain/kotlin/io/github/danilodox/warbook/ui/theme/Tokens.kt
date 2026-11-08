package io.github.danilodox.warbook.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Paleta "livro de guerra" (design.md, v0.6.0): dourado = ação e destaque, couro = apoio,
// vermelho-sangue = guerra (pendentes, inimigo). Erro em laranja queimado para não se confundir com a guerra.
// Cores fixas da marca, sem Material You.

internal object DarkTokens {
    val primary = Color(0xFFE8BC5E)
    val onPrimary = Color(0xFF3F2B00)
    val primaryContainer = Color(0xFF5C4300)
    val onPrimaryContainer = Color(0xFFFFDFA6)
    val secondary = Color(0xFFDDB893)
    val onSecondary = Color(0xFF3F2815)
    val secondaryContainer = Color(0xFF5A402A)
    val onSecondaryContainer = Color(0xFFFBDCC0)
    val tertiary = Color(0xFFFFB3AA)
    val onTertiary = Color(0xFF5F1210)
    val tertiaryContainer = Color(0xFF8A1F1C)
    val onTertiaryContainer = Color(0xFFFFDAD5)
    val error = Color(0xFFFFB68F)
    val onError = Color(0xFF552000)
    val errorContainer = Color(0xFF793000)
    val onErrorContainer = Color(0xFFFFDBCA)
    val surface = Color(0xFF17120E)
    val onSurface = Color(0xFFEEE3D4)
    val onSurfaceVariant = Color(0xFFD0C1AC)
    val surfaceContainerLowest = Color(0xFF110D0A)
    val surfaceContainerLow = Color(0xFF1F1813)
    val surfaceContainer = Color(0xFF251D17)
    val surfaceContainerHigh = Color(0xFF30261E)
    val surfaceContainerHighest = Color(0xFF3B3027)
    val outline = Color(0xFF9A8973)
    val outlineVariant = Color(0xFF4E4236)
    val inverseSurface = Color(0xFFEEE3D4)
    val inverseOnSurface = Color(0xFF362F27)
    val inversePrimary = Color(0xFF7A5900)
    val success = Color(0xFF9CD49B)
    val successContainer = Color(0xFF1F4D22)
}

internal object LightTokens {
    val primary = Color(0xFF7A5900)
    val onPrimary = Color(0xFFFFFFFF)
    val primaryContainer = Color(0xFFFFDFA6)
    val onPrimaryContainer = Color(0xFF261A00)
    val secondary = Color(0xFF73563B)
    val onSecondary = Color(0xFFFFFFFF)
    val secondaryContainer = Color(0xFFFCDCC0)
    val onSecondaryContainer = Color(0xFF2B1705)
    val tertiary = Color(0xFF9B2423)
    val onTertiary = Color(0xFFFFFFFF)
    val tertiaryContainer = Color(0xFFFFDAD5)
    val onTertiaryContainer = Color(0xFF410004)
    val error = Color(0xFFA33F00)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFFFDBCA)
    val onErrorContainer = Color(0xFF351000)
    val surface = Color(0xFFFBF6EE)
    val onSurface = Color(0xFF1F1B16)
    val onSurfaceVariant = Color(0xFF4F4539)
    val surfaceContainerLowest = Color(0xFFFFFFFF)
    val surfaceContainerLow = Color(0xFFF6EFE4)
    val surfaceContainer = Color(0xFFF0E8DC)
    val surfaceContainerHigh = Color(0xFFEAE2D5)
    val surfaceContainerHighest = Color(0xFFE4DBCE)
    val outline = Color(0xFF7F7462)
    val outlineVariant = Color(0xFFD3C5B2)
    val inverseSurface = Color(0xFF352F28)
    val inverseOnSurface = Color(0xFFF8EFE4)
    val inversePrimary = Color(0xFFE8BC5E)
    val success = Color(0xFF2E6B32)
    val successContainer = Color(0xFFC8EEC4)
}

/** Escala de espaçamento (base 4 dp). `lg` é a borda da tela; `xl` separa seções. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/** Cantos um pouco mais retos que o M3 padrão: lembram capa de livro. */
internal object Radius {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 28.dp
}
