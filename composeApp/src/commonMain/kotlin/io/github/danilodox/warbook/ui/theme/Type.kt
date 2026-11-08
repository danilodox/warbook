package io.github.danilodox.warbook.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.alegreya_sans_bold
import io.github.danilodox.warbook.resources.alegreya_sans_extrabold
import io.github.danilodox.warbook.resources.alegreya_sans_medium
import io.github.danilodox.warbook.resources.alegreya_sans_regular
import io.github.danilodox.warbook.resources.cinzel_bold
import io.github.danilodox.warbook.resources.cinzel_extrabold
import io.github.danilodox.warbook.resources.cinzel_semibold
import io.github.danilodox.warbook.resources.jetbrains_mono_medium
import org.jetbrains.compose.resources.Font

/** A Alegreya Sans usa números estilo antigo por padrão; `lnum` liga os números alinhados. */
internal const val LINING = "lnum"

/** Números alinhados e de largura igual: placares, listas e colunas de valores. */
internal const val LINING_TABULAR = "lnum, tnum"

/** Estilos fora do M3: tag em monoespaçada e o nome "WARBOOK" ao lado do ícone. */
@Immutable
data class WarbookTextStyles(val tag: TextStyle, val wordmark: TextStyle)

@Composable
private fun displayFamily() = FontFamily(
    Font(Res.font.cinzel_semibold, FontWeight.SemiBold),
    Font(Res.font.cinzel_bold, FontWeight.Bold),
    Font(Res.font.cinzel_extrabold, FontWeight.ExtraBold),
)

@Composable
private fun bodyFamily() = FontFamily(
    Font(Res.font.alegreya_sans_regular, FontWeight.Normal),
    Font(Res.font.alegreya_sans_medium, FontWeight.Medium),
    Font(Res.font.alegreya_sans_bold, FontWeight.Bold),
    Font(Res.font.alegreya_sans_extrabold, FontWeight.ExtraBold),
)

@Composable
internal fun rememberWarbookTypography(): Pair<Typography, WarbookTextStyles> {
    val display = displayFamily()
    val body = bodyFamily()
    val mono = FontFamily(Font(Res.font.jetbrains_mono_medium, FontWeight.Medium))
    return remember(display, body, mono) { warbookTypography(display, body) to warbookTextStyles(display, mono) }
}

@Suppress("MagicNumber") // a escala tipográfica do design.md
private fun warbookTypography(display: FontFamily, body: FontFamily): Typography {
    fun d(size: Int, line: Int, weight: FontWeight) = TextStyle(
        fontFamily = display,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = 0.02.em,
        fontFeatureSettings = LINING,
    )
    fun b(size: Int, line: Int, weight: FontWeight, spacing: Double = 0.0) = TextStyle(
        fontFamily = body,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = spacing.sp,
        fontFeatureSettings = LINING,
    )
    return Typography(
        displayLarge = d(57, 64, FontWeight.Bold),
        displayMedium = d(45, 52, FontWeight.Bold),
        displaySmall = d(36, 44, FontWeight.Bold),
        headlineLarge = d(32, 40, FontWeight.Bold),
        headlineMedium = d(28, 36, FontWeight.Bold),
        headlineSmall = d(24, 32, FontWeight.SemiBold),
        titleLarge = d(22, 28, FontWeight.Bold),
        titleMedium = b(16, 24, FontWeight.Bold),
        titleSmall = b(14, 20, FontWeight.Bold),
        bodyLarge = b(16, 24, FontWeight.Normal),
        bodyMedium = b(14, 20, FontWeight.Normal),
        bodySmall = b(12, 16, FontWeight.Normal),
        labelLarge = b(14, 20, FontWeight.Bold),
        labelMedium = b(12, 16, FontWeight.Bold, 0.5),
        labelSmall = b(11, 16, FontWeight.Bold, 0.6),
    )
}

@Suppress("MagicNumber")
private fun warbookTextStyles(display: FontFamily, mono: FontFamily) = WarbookTextStyles(
    tag = TextStyle(fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    wordmark = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        letterSpacing = 0.16.em,
    ),
)

/** O mesmo estilo com números alinhados e de largura igual (placares e listas). */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = LINING_TABULAR)
