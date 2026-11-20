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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.api.CwlSeasonDto
import io.github.danilodox.warbook.api.WarKind
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.api.WarSummaryDto
import io.github.danilodox.warbook.data.WarbookRepository
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.history_cwl
import io.github.danilodox.warbook.resources.history_empty_body
import io.github.danilodox.warbook.resources.history_empty_title
import io.github.danilodox.warbook.resources.history_in_progress
import io.github.danilodox.warbook.resources.history_open_activity
import io.github.danilodox.warbook.resources.history_open_planner
import io.github.danilodox.warbook.resources.history_record
import io.github.danilodox.warbook.resources.history_score_only
import io.github.danilodox.warbook.ui.components.Panel
import io.github.danilodox.warbook.ui.components.StarIcon
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Histórico de guerras do clã [clanTag]. [onOpenWar] abre uma guerra com nota (as do warlog não abrem);
 * [onOpenActivity], a atividade dos membros; [onOpenPlanner], o planejador da CWL.
 */
@Composable
fun WarHistoryRoute(
    clanTag: String,
    onOpenWar: (Long) -> Unit,
    onOpenActivity: () -> Unit,
    onOpenPlanner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val api: WarbookRepository = koinInject()
    val viewModel = viewModel(key = "war-history-$clanTag") { WarHistoryViewModel(clanTag, api) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // A temporada da CWL é um extra: se falhar, o cartão só não aparece.
    val cwl = viewModel(key = "cwl-season-$clanTag") { CwlSeasonViewModel(clanTag, api) }
    val season by cwl.state.collectAsStateWithLifecycle()
    val refresh = {
        viewModel.refresh()
        cwl.refresh()
    }
    LoadScreen(state, onRefresh = refresh, modifier = modifier) { history ->
        if (history.wars.isEmpty()) {
            EmptyHistory()
        } else {
            WarList(history.wars, season.value, onOpenWar, onOpenActivity, onOpenPlanner)
        }
    }
}

@Composable
private fun WarList(
    wars: List<WarSummaryDto>,
    season: CwlSeasonDto?,
    onOpenWar: (Long) -> Unit,
    onOpenActivity: () -> Unit,
    onOpenPlanner: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            val count = wars.recordCount()
            Column {
                Text(
                    stringResource(Res.string.history_record, count.wins, count.losses, count.ties),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row {
                    TextButton(onClick = onOpenActivity) { Text(stringResource(Res.string.history_open_activity)) }
                    TextButton(onClick = onOpenPlanner) { Text(stringResource(Res.string.history_open_planner)) }
                }
            }
        }
        if (season?.season != null) item(key = "cwl-season") { CwlSeasonCard(season) }
        items(wars, key = { it.id }) { war ->
            WarRow(war, onClick = { onOpenWar(war.id) }.takeIf { war.detailed })
        }
    }
}

/** Uma linha do livro: resultado, adversário, data e placar. */
@Composable
private fun WarRow(war: WarSummaryDto, onClick: (() -> Unit)?) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        Row(
            Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ResultBadge(war)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    war.opponentName.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle(war),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Score(war.stars, war.opponentStars)
        }
    }
}

@Composable
private fun subtitle(war: WarSummaryDto): String {
    val parts = buildList {
        add(warDate(war.endTime))
        if (war.kind == WarKind.CWL) add(stringResource(Res.string.history_cwl))
        if (war.state != WarState.WAR_ENDED) add(stringResource(Res.string.history_in_progress))
        if (!war.detailed) add(stringResource(Res.string.history_score_only))
    }
    return parts.joinToString(" · ")
}

@Composable
private fun Score(ours: Int, theirs: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        StarIcon(filled = true)
        Text("$ours × $theirs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyHistory() {
    Panel(Modifier.padding(Spacing.lg)) {
        Text(stringResource(Res.string.history_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.history_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
