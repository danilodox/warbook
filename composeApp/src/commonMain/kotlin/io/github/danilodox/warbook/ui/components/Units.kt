@file:Suppress("MatchingDeclarationName") // o arquivo leva o nome do componente principal

package io.github.danilodox.warbook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.ic_check
import io.github.danilodox.warbook.resources.unit_full_th
import io.github.danilodox.warbook.resources.unit_level
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.tabular
import io.github.danilodox.warbook.ui.units.UnitIcon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Uma unidade do perfil já com o nome que aparece na tela.
 * [maxLevel] é o máximo do jogo (da API); [townHallMax] o que o CV do jogador permite (tabela própria,
 * igual a [maxLevel] quando a unidade não está na tabela). "No máximo" é sempre em relação ao CV.
 */
data class UnitItem(
    val key: String,
    val name: String,
    val level: Int,
    val maxLevel: Int,
    val townHallMax: Int = maxLevel,
) {
    val isMax: Boolean get() = townHallMax in 1..level
    val isGameMax: Boolean get() = maxLevel in 1..level
    val progress: Float get() = if (townHallMax <= 0) 0f else (level.toFloat() / townHallMax).coerceIn(0f, 1f)
}

private const val GRID_COLUMNS = 5
private val HERO_PORTRAIT = 44.dp

/** Barra de nível: dourada em progresso, couro no máximo. */
@Composable
fun LevelBar(progress: Float, isMax: Boolean, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val fill = if (isMax) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Box(modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.extraSmall).background(track)) {
        Box(Modifier.fillMaxWidth(progress).height(6.dp).clip(MaterialTheme.shapes.extraSmall).background(fill))
    }
}

/** Herói em lista: ícone, nome, nível/máximo e barra. */
@Composable
fun HeroRow(item: UnitItem, modifier: Modifier = Modifier, icon: @Composable (UnitItem) -> Unit = { UnitIcon(it) }) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Retrato num quadro fixo: todos os heróis com o mesmo tamanho, com ou sem ícone.
        Box(
            Modifier
                .size(HERO_PORTRAIT)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) { icon(item) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                LevelText(item)
            }
            LevelBar(item.progress, item.isMax)
        }
    }
}

/** "nível/máximo do CV"; no máximo do CV, dourado e com ✓. */
@Composable
fun LevelText(item: UnitItem, modifier: Modifier = Modifier) {
    val color = if (item.isMax) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val full = stringResource(Res.string.unit_full_th)
    Row(
        modifier.semantics(mergeDescendants = true) { if (item.isMax) stateDescription = full },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        if (item.isMax) {
            Icon(painterResource(Res.drawable.ic_check), contentDescription = null, Modifier.size(14.dp), tint = color)
        }
        Text(
            stringResource(Res.string.unit_level, item.level, item.townHallMax),
            style = MaterialTheme.typography.bodySmall.tabular(),
            fontWeight = if (item.isMax) FontWeight.Bold else null,
            color = color,
        )
    }
}

/**
 * Tropas, pets, feitiços e máquinas em grade de 5, com o nível no canto.
 * Borda e etiqueta douradas com ✓ = no máximo do CV.
 * É uma grade simples (linhas de [Row]) para caber dentro de uma `LazyColumn`.
 */
@Composable
fun UnitGrid(
    items: List<UnitItem>,
    modifier: Modifier = Modifier,
    icon: @Composable (UnitItem) -> Unit = { UnitIcon(it) },
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xxs)) {
        items.chunked(GRID_COLUMNS).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xxs)) {
                row.forEach { item -> UnitCell(item, icon, Modifier.weight(1f)) }
                repeat(GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun UnitCell(item: UnitItem, icon: @Composable (UnitItem) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    val level = stringResource(Res.string.unit_level, item.level, item.townHallMax)
    val full = stringResource(Res.string.unit_full_th)
    val description = if (item.isMax) "${item.name} $level, $full" else "${item.name} $level"
    Box(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .border(1.5.dp, if (item.isMax) colors.primary else colors.surfaceContainerHigh, shape)
            .padding(3.dp)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        icon(item)
        Surface(
            color = if (item.isMax) colors.primary else colors.surface,
            contentColor = if (item.isMax) colors.onPrimary else colors.onSurface,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            Row(
                Modifier.padding(horizontal = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                if (item.isMax) {
                    Icon(painterResource(Res.drawable.ic_check), contentDescription = null, Modifier.size(9.dp))
                }
                Text(
                    item.level.toString(),
                    style = MaterialTheme.typography.labelSmall.tabular().copy(fontSize = 10.sp, letterSpacing = 0.sp),
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

/** Sem ícone do Fan Kit: as primeiras letras do nome. */
@Composable
fun UnitInitials(item: UnitItem, modifier: Modifier = Modifier) {
    val initials = item.name.split(' ', '.').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }
    Text(
        initials.uppercase(),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
