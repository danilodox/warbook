package io.github.danilodox.warbook.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.api.MemberWarScoreDto
import io.github.danilodox.warbook.api.ScoredAttackDto
import io.github.danilodox.warbook.api.WarDetailDto
import io.github.danilodox.warbook.api.WarKind
import io.github.danilodox.warbook.data.WarbookRepository
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.decimal_separator
import io.github.danilodox.warbook.resources.history_attack_target
import io.github.danilodox.warbook.resources.history_cwl
import io.github.danilodox.warbook.resources.history_mark_cleanup
import io.github.danilodox.warbook.resources.history_mark_easy
import io.github.danilodox.warbook.resources.history_mark_mirror
import io.github.danilodox.warbook.resources.history_missed
import io.github.danilodox.warbook.resources.history_no_attacks
import io.github.danilodox.warbook.resources.history_score_note
import io.github.danilodox.warbook.resources.history_vs
import io.github.danilodox.warbook.score.AttackMark
import io.github.danilodox.warbook.ui.components.Panel
import io.github.danilodox.warbook.ui.components.StarRow
import io.github.danilodox.warbook.ui.components.TownHallShield
import io.github.danilodox.warbook.ui.me.OfflineCard
import io.github.danilodox.warbook.ui.me.StaleBanner
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Uma guerra guardada: placar e a nota de cada membro, na ordem do mapa. [onOpenPlayer] abre o jogador. */
@Composable
fun WarRecordRoute(clanTag: String, warId: Long, onOpenPlayer: (String) -> Unit, modifier: Modifier = Modifier) {
    val api: WarbookRepository = koinInject()
    val viewModel = viewModel(key = "war-record-$clanTag-$warId") { WarRecordViewModel(clanTag, warId, api) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    WarRecordScreen(state, onRefresh = viewModel::refresh, onOpenPlayer = onOpenPlayer, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarRecordScreen(
    state: LoadState<WarDetailDto>,
    onRefresh: () -> Unit,
    onOpenPlayer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = state.value
    val error = state.error
    PullToRefreshBox(
        isRefreshing = state.loading && detail != null,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            detail != null -> Column(Modifier.fillMaxSize()) {
                if (error != null) StaleBanner(error, state.fetchedAt, onRefresh)
                WarRecordContent(detail, onOpenPlayer)
            }
            error != null -> OfflineCard(error, onRetry = onRefresh)
            else -> HistorySkeleton()
        }
    }
}

@Composable
private fun WarRecordContent(detail: WarDetailDto, onOpenPlayer: (String) -> Unit) {
    val separator = stringResource(Res.string.decimal_separator)
    LazyColumn(
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { WarHeader(detail) }
        items(detail.members, key = { it.tag }) { member ->
            MemberScoreCard(member, separator, onOpen = { onOpenPlayer(member.tag) })
        }
        item {
            Text(
                stringResource(Res.string.history_score_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun WarHeader(detail: WarDetailDto) {
    val war = detail.war
    Panel {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            ResultBadge(war)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.history_vs, war.opponentName.orEmpty()),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val kind = if (war.kind == WarKind.CWL) " · ${stringResource(Res.string.history_cwl)}" else ""
                Text(
                    warDate(war.endTime) + kind,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${war.stars} × ${war.opponentStars}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Um membro: posição, CV, nome, nota total e cada ataque com a marca. */
@Composable
private fun MemberScoreCard(member: MemberWarScoreDto, separator: String, onOpen: () -> Unit) {
    Panel {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${member.mapPosition}.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TownHallShield(member.townHall)
            Text(
                member.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).clickable(onClick = onOpen),
            )
            Text(
                formatPoints(member.total, separator),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (member.total < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
        if (member.attacks.isEmpty() && member.missed == 0) {
            Text(
                stringResource(Res.string.history_no_attacks),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        member.attacks.forEach { AttackLine(it, separator) }
        if (member.missed > 0) {
            Text(
                stringResource(Res.string.history_missed, member.missed, formatPoints(member.penalty, separator)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun AttackLine(attack: ScoredAttackDto, separator: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        StarRow(attack.stars)
        Text("${attack.destruction}%", style = MaterialTheme.typography.bodySmall)
        Text(
            stringResource(
                Res.string.history_attack_target,
                attack.defenderPosition ?: 0,
                attack.defenderName ?: attack.defenderTag,
            ) + (attack.defenderTownHall?.let { " · CV$it" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        attack.mark?.let { MarkLabel(it) }
        Text(formatPoints(attack.points, separator), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun MarkLabel(mark: AttackMark) {
    val (text, color) = when (mark) {
        AttackMark.MIRROR -> Res.string.history_mark_mirror to MaterialTheme.colorScheme.onSurfaceVariant
        AttackMark.EASY_TARGET -> Res.string.history_mark_easy to MaterialTheme.colorScheme.error
        AttackMark.CLEANUP -> Res.string.history_mark_cleanup to MaterialTheme.colorScheme.secondary
    }
    Text(stringResource(text), style = MaterialTheme.typography.labelSmall, color = color)
}
