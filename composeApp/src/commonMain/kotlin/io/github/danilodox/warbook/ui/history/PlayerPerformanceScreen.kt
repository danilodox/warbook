package io.github.danilodox.warbook.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.api.PlayerHistoryDto
import io.github.danilodox.warbook.api.PlayerWarEntryDto
import io.github.danilodox.warbook.api.WarKind
import io.github.danilodox.warbook.data.WarbookRepository
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.decimal_separator
import io.github.danilodox.warbook.resources.history_cwl
import io.github.danilodox.warbook.resources.performance_attacks
import io.github.danilodox.warbook.resources.performance_average
import io.github.danilodox.warbook.resources.performance_empty
import io.github.danilodox.warbook.resources.performance_marks
import io.github.danilodox.warbook.resources.performance_open_profile
import io.github.danilodox.warbook.resources.performance_trend
import io.github.danilodox.warbook.resources.performance_trend_none
import io.github.danilodox.warbook.resources.performance_war_attacks
import io.github.danilodox.warbook.resources.performance_wars_title
import io.github.danilodox.warbook.ui.components.Panel
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.WarbookTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Desempenho do jogador [playerTag] no clã [clanTag]: média, tendência, ataques e marcas, e a nota de cada
 * guerra. [onOpenWar] abre a guerra; [onOpenProfile], o perfil do jogador.
 */
@Composable
fun PlayerPerformanceRoute(
    clanTag: String,
    playerTag: String,
    onOpenWar: (Long) -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val api: WarbookRepository = koinInject()
    val viewModel = viewModel(key = "performance-$clanTag-$playerTag") {
        PlayerPerformanceViewModel(clanTag, playerTag, api)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoadScreen(state, onRefresh = viewModel::refresh, modifier = modifier) { history ->
        PlayerPerformanceContent(history, onOpenWar, onOpenProfile)
    }
}

@Composable
fun PlayerPerformanceContent(history: PlayerHistoryDto, onOpenWar: (Long) -> Unit, onOpenProfile: () -> Unit) {
    val separator = stringResource(Res.string.decimal_separator)
    LazyColumn(
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { SummaryPanel(history, separator, onOpenProfile) }
        if (history.wars.isEmpty()) {
            item {
                Text(
                    stringResource(Res.string.performance_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item {
                Text(
                    stringResource(Res.string.performance_wars_title, history.wars.size),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
            items(history.wars, key = { it.warId }) { entry ->
                WarEntryRow(entry, separator, onClick = { onOpenWar(entry.warId) })
            }
        }
    }
}

@Composable
private fun SummaryPanel(history: PlayerHistoryDto, separator: String, onOpenProfile: () -> Unit) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    history.name ?: history.tag,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(Res.string.performance_average),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                history.average?.let { formatPoints(it, separator) } ?: "—",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        TrendLine(history.trend, separator)
        Text(
            stringResource(Res.string.performance_attacks, history.attacksUsed, history.attacksAvailable),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            stringResource(Res.string.performance_marks, history.easyTargets, history.cleanups),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = onOpenProfile, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.performance_open_profile))
        }
    }
}

/** Tendência: verde subindo, vermelho caindo. Sem guerras para comparar, uma explicação. */
@Composable
private fun TrendLine(trend: Double?, separator: String) {
    if (trend == null) {
        Text(
            stringResource(Res.string.performance_trend_none),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val arrow = when {
        trend > 0 -> "▲"
        trend < 0 -> "▼"
        else -> "="
    }
    val color = when {
        trend > 0 -> WarbookTheme.colors.success
        trend < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        stringResource(Res.string.performance_trend, "$arrow ${formatPoints(trend, separator, signed = true)}"),
        style = MaterialTheme.typography.bodyMedium,
        color = color,
    )
}

@Composable
private fun WarEntryRow(entry: PlayerWarEntryDto, separator: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    entry.opponentName.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val cwl = if (entry.kind == WarKind.CWL) " · ${stringResource(Res.string.history_cwl)}" else ""
                Text(
                    warDate(entry.endTime) + cwl + " · " +
                        stringResource(Res.string.performance_war_attacks, entry.attacksUsed, entry.attacksAvailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                formatPoints(entry.total, separator),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (entry.total < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
