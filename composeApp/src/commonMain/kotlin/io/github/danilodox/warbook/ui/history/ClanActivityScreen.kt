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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.api.ClanActivityDto
import io.github.danilodox.warbook.api.MemberActivityDto
import io.github.danilodox.warbook.data.WarbookRepository
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.activity_cwl_header
import io.github.danilodox.warbook.resources.activity_member_cwl
import io.github.danilodox.warbook.resources.activity_member_wars
import io.github.danilodox.warbook.resources.activity_wars_header
import io.github.danilodox.warbook.resources.history_empty_body
import io.github.danilodox.warbook.resources.history_empty_title
import io.github.danilodox.warbook.ui.components.Panel
import io.github.danilodox.warbook.ui.components.TownHallShield
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Atividade dos membros do clã [clanTag]. [onOpenMember] abre o desempenho do membro. */
@Composable
fun ClanActivityRoute(clanTag: String, onOpenMember: (String) -> Unit, modifier: Modifier = Modifier) {
    val api: WarbookRepository = koinInject()
    val viewModel = viewModel(key = "activity-$clanTag") { ClanActivityViewModel(clanTag, api) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoadScreen(state, onRefresh = viewModel::refresh, modifier = modifier) { ClanActivityContent(it, onOpenMember) }
}

@Composable
fun ClanActivityContent(activity: ClanActivityDto, onOpenMember: (String) -> Unit) {
    if (activity.members.isEmpty()) {
        Panel(Modifier.padding(Spacing.lg)) {
            Text(stringResource(Res.string.history_empty_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(Res.string.history_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { ActivityHeader(activity) }
        items(activity.members, key = { it.tag }) { member ->
            MemberRow(member, showCwl = activity.cwlRounds > 0, onClick = { onOpenMember(member.tag) })
        }
    }
}

@Composable
private fun ActivityHeader(activity: ClanActivityDto) {
    Column(Modifier.padding(bottom = Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Text(
            stringResource(Res.string.activity_wars_header, activity.warsCounted),
            style = MaterialTheme.typography.titleMedium,
        )
        val season = activity.cwlSeason
        if (season != null) {
            Text(
                stringResource(Res.string.activity_cwl_header, season, activity.cwlRounds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Um membro: CV, nome e os ataques usados; o que faltou fica no tom de alerta. */
@Composable
private fun MemberRow(member: MemberActivityDto, showCwl: Boolean, onClick: () -> Unit) {
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
            TownHallShield(member.townHall)
            Text(
                member.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    stringResource(Res.string.activity_member_wars, member.attacksUsed, member.attacksAvailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = missedColor(member.attacksUsed < member.attacksAvailable),
                )
                if (showCwl) {
                    Text(
                        stringResource(Res.string.activity_member_cwl, member.cwlAttacksUsed, member.cwlRounds),
                        style = MaterialTheme.typography.bodySmall,
                        color = missedColor(member.cwlAttacksUsed < member.cwlRounds),
                    )
                }
            }
        }
    }
}

@Composable
private fun missedColor(missed: Boolean): Color =
    if (missed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
