@file:Suppress("MatchingDeclarationName") // o arquivo leva o nome do componente principal

package io.github.danilodox.warbook.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.danilodox.warbook.ui.theme.tabular

/** Tamanhos do escudo de CV: em listas, no cabeçalho do perfil e no cartão. */
enum class ShieldSize(val width: Dp, val height: Dp, val text: TextUnit, val display: Boolean) {
    Small(26.dp, 28.dp, 11.5.sp, display = false),
    Large(48.dp, 52.dp, 19.sp, display = true),
    ExtraLarge(68.dp, 74.dp, 28.sp, display = true),
}

// Escudo em 24 × 26 (o mesmo dos mockups).
private val ShieldPath = PathParser()
    .parsePathString("M12 1l10 3.5v8c0 6.3-4.5 10.7-10 12.5C6.5 23.2 2 18.8 2 12.5v-8z")
    .toPath()

private const val VIEW_W = 24f
private const val STROKE = 1.2f

/** O nível do CV dentro de um escudo de couro com borda dourada. */
@Composable
fun TownHallShield(level: Int, modifier: Modifier = Modifier, size: ShieldSize = ShieldSize.Small) {
    val fill = MaterialTheme.colorScheme.secondaryContainer
    val border = MaterialTheme.colorScheme.primary
    Box(modifier.size(size.width, size.height), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val factor = this.size.width / VIEW_W
            scale(factor, factor, pivot = Offset.Zero) {
                drawPath(ShieldPath, fill)
                drawPath(ShieldPath, border, style = Stroke(width = STROKE))
            }
        }
        val base = if (size.display) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelMedium
        Text(
            level.toString(),
            style = base.tabular().copy(fontSize = size.text, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(bottom = size.height * 0.08f),
        )
    }
}
