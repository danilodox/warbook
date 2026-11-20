package io.github.danilodox.warbook.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.api.CwlPlannerDto
import io.github.danilodox.warbook.data.WarbookRepository
import io.github.danilodox.warbook.history.LineupSummary
import io.github.danilodox.warbook.history.lineupSummaryTextsFor
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.decimal_separator
import io.github.danilodox.warbook.resources.performance_war_attacks
import io.github.danilodox.warbook.resources.planner_hint
import io.github.danilodox.warbook.resources.planner_no_history
import io.github.danilodox.warbook.resources.planner_restore
import io.github.danilodox.warbook.resources.planner_selected
import io.github.danilodox.warbook.resources.planner_size
import io.github.danilodox.warbook.score.CwlPlanner
import io.github.danilodox.warbook.score.PlannerCandidate
import io.github.danilodox.warbook.ui.components.TownHallShield
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.war.SummaryDock
import io.github.danilodox.warbook.war.SummaryOptions
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Planejador da CWL do clã [clanTag]: a escalação sugerida, que o usuário ajusta marcando e desmarcando. */
@Composable
fun CwlPlannerRoute(clanTag: String, modifier: Modifier = Modifier) {
    val api: WarbookRepository = koinInject()
    val viewModel = viewModel(key = "cwl-planner-$clanTag") { CwlPlannerViewModel(clanTag, api) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize()) {
        LoadScreen(state, onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) { candidates ->
            CwlPlannerContent(
                candidates = candidates,
                selection = selection,
                onSize = viewModel::setSize,
                onToggle = viewModel::toggle,
                onRestore = viewModel::restore,
            )
        }
        // Igual às outras telas: a escalação vai para o WhatsApp pela barra presa embaixo.
        val texts = lineupSummaryTextsFor(Locale.current.language)
        val summary = state.value?.let { LineupSummary.of(it, selection.selected, selection.size, texts) }
        if (summary != null) {
            SummaryDock(summary, parts = emptyList(), options = SummaryOptions(), ended = true, onToggle = {})
        }
    }
}

@Composable
fun CwlPlannerContent(
    candidates: List<PlannerCandidate>,
    selection: PlannerSelection,
    onSize: (Int) -> Unit,
    onToggle: (String) -> Unit,
    onRestore: () -> Unit,
) {
    val separator = stringResource(Res.string.decimal_separator)
    LazyColumn(
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { PlannerHeader(selection, onSize, onRestore) }
        itemsIndexed(candidates, key = { _, candidate -> candidate.tag }) { index, candidate ->
            CandidateRow(
                position = index + 1,
                candidate = candidate,
                selected = candidate.tag in selection.selected,
                separator = separator,
                onToggle = { onToggle(candidate.tag) },
            )
        }
    }
}

@Composable
private fun PlannerHeader(selection: PlannerSelection, onSize: (Int) -> Unit, onRestore: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            CwlPlanner.SIZES.forEach { size ->
                FilterChip(
                    selected = selection.size == size,
                    onClick = { onSize(size) },
                    label = { Text(stringResource(Res.string.planner_size, size)) },
                )
            }
            Text(
                stringResource(Res.string.planner_selected, selection.selected.size, selection.size),
                style = MaterialTheme.typography.titleSmall,
                // Fora do tamanho, em vermelho: a CWL não deixa escalar mais nem menos.
                color = if (selection.selected.size == selection.size) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.weight(1f),
            )
            if (selection.edited) {
                TextButton(onClick = onRestore) { Text(stringResource(Res.string.planner_restore)) }
            }
        }
        Text(
            stringResource(Res.string.planner_hint, CwlPlannerDto.WARS),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Uma linha: marcar, posição na sugestão, CV, nome, a posição calculada e os ataques usados. */
@Composable
private fun CandidateRow(
    position: Int,
    candidate: PlannerCandidate,
    selected: Boolean,
    separator: String,
    onToggle: () -> Unit,
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().toggleable(value = selected, role = Role.Checkbox, onValueChange = {
            onToggle()
        }),
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = selected, onCheckedChange = null)
            Text(
                "$position.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TownHallShield(candidate.townHall)
            Column(Modifier.weight(1f)) {
                Text(
                    candidate.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val stats = candidate.stats
                Text(
                    if (stats == null) {
                        stringResource(Res.string.planner_no_history)
                    } else {
                        stringResource(Res.string.performance_war_attacks, stats.attacksUsed, stats.attacksAvailable)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            candidate.rating?.let {
                Text(
                    formatPoints(it, separator),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (it < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
