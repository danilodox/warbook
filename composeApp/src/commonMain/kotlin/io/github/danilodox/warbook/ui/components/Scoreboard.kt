@file:Suppress("MatchingDeclarationName") // o arquivo leva o nome do componente principal

package io.github.danilodox.warbook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.ic_star_filled
import io.github.danilodox.warbook.ui.common.ClanBadge
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.tabular
import org.jetbrains.compose.resources.painterResource

/** Um lado do placar: nome e brasão do clã, estrelas e a linha de baixo (destruição e ataques). */
data class ScoreSide(val name: String, val badgeUrl: String?, val stars: Int, val detail: String)

/**
 * Placar da guerra: chip do estado em vermelho-sangue, estrelas em Cinzel e a barra dividida
 * (nós em dourado, eles em vermelho). Sem [showStars] (preparação) mostra só os clãs.
 */
@Composable
fun WarScoreboard(
    state: String,
    time: String,
    ours: ScoreSide,
    theirs: ScoreSide,
    showStars: Boolean,
    modifier: Modifier = Modifier,
    center: String? = null,
    result: String? = null,
    stale: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(colors.surfaceContainerLow)
            .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.large)
            .padding(horizontal = Spacing.md, vertical = Spacing.md)
            .then(if (stale) Modifier.dimmed() else Modifier),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StateChip(state)
            Text(
                time,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        if (result != null) Text(result, style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Side(ours, alignEnd = false, modifier = Modifier.weight(1f))
            if (showStars) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xxs),
                    modifier = Modifier.clearAndSetSemantics { contentDescription = "${ours.stars} × ${theirs.stars}" },
                ) {
                    Text(ours.stars.toString(), style = MaterialTheme.typography.headlineMedium.tabular())
                    StarIcon(filled = true, size = 20.dp)
                    Text(theirs.stars.toString(), style = MaterialTheme.typography.headlineMedium.tabular())
                }
            } else if (center != null) {
                Text(center, style = MaterialTheme.typography.titleLarge.tabular())
            }
            Side(theirs, alignEnd = true, modifier = Modifier.weight(1f))
        }
        if (showStars) {
            SplitBar(ours.stars, theirs.stars)
            DetailRow(ours.detail, theirs.detail)
        }
    }
}

@Composable
private fun DetailRow(ours: String, theirs: String) {
    val style = MaterialTheme.typography.bodySmall.tabular()
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth()) {
        Text(ours, style = style, color = color, modifier = Modifier.weight(1f))
        Text(theirs, style = style, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

/** Chip do estado da guerra ("Dia de batalha", "Preparação"…). */
@Composable
fun StateChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier,
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
        )
    }
}

@Composable
private fun Side(side: ScoreSide, alignEnd: Boolean, modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        ClanBadge(side.badgeUrl, contentDescription = null, size = 28.dp)
        Text(
            side.name,
            style = MaterialTheme.typography.labelMedium,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Barra das estrelas: a parte dourada é a nossa fração do total. */
@Composable
private fun SplitBar(ours: Int, theirs: Int) {
    val total = ours + theirs
    val fraction = if (total == 0) HALF else ours.toFloat() / total
    Box(
        Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.tertiary),
    ) {
        Box(Modifier.fillMaxWidth(fraction).height(6.dp).background(MaterialTheme.colorScheme.primary))
    }
}

/** Estrela de guerra: dourada se conquistada, apagada se não. */
@Composable
fun StarIcon(filled: Boolean, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    Icon(
        painterResource(Res.drawable.ic_star_filled),
        contentDescription = null,
        tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.size(size),
    )
}

/** Estrelas de um ataque (0 a [total]). */
@Composable
fun StarRow(stars: Int, modifier: Modifier = Modifier, total: Int = 3) {
    Row(modifier.clearAndSetSemantics { contentDescription = "$stars/$total" }) {
        repeat(total) { StarIcon(filled = it < stars) }
    }
}

private const val STALE_ALPHA = 0.62f
private const val HALF = 0.5f

/** Conteúdo salvo enquanto offline: um pouco apagado. */
fun Modifier.dimmed(): Modifier = alpha(STALE_ALPHA)
