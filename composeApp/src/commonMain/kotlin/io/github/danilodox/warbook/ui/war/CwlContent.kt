package io.github.danilodox.warbook.ui.war

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.api.CwlClanDto
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.api.CwlState
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.cwl_group
import io.github.danilodox.warbook.resources.cwl_group_columns
import io.github.danilodox.warbook.resources.cwl_not_in_body
import io.github.danilodox.warbook.resources.cwl_not_in_title
import io.github.danilodox.warbook.resources.cwl_round_chip
import io.github.danilodox.warbook.resources.cwl_round_not_drawn
import io.github.danilodox.warbook.resources.cwl_round_of
import io.github.danilodox.warbook.resources.ic_check
import io.github.danilodox.warbook.ui.common.ClanBadge
import io.github.danilodox.warbook.ui.components.SectionHeader
import io.github.danilodox.warbook.ui.components.StarIcon
import io.github.danilodox.warbook.ui.components.youContentColor
import io.github.danilodox.warbook.ui.components.youHighlight
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.tabular
import io.github.danilodox.warbook.war.WarSummary
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Sub-aba CWL (G3): fichas das rodadas, a guerra da rodada escolhida e a classificação do grupo. */
@Composable
fun CwlContent(
    cwl: CwlDto,
    round: Int?,
    onSelectRound: (Int) -> Unit,
    context: WarContext,
    modifier: Modifier = Modifier,
) {
    if (cwl.state == CwlState.NOT_IN_CWL || cwl.rounds.isEmpty()) {
        Message(Res.string.cwl_not_in_title, Res.string.cwl_not_in_body, modifier)
        return
    }
    val selected = round ?: cwl.rounds.first().round
    val war = cwl.rounds.firstOrNull { it.round == selected }?.war
    val summary = war?.let { WarSummary.cwl(cwl, selected, context.now, context.texts, context.summaryOptions) }
    WarLayout(war, summary, context, modifier) {
        SectionHeader(stringResource(Res.string.cwl_round_of, selected, cwl.rounds.size))
        RoundChips(cwl, selected, onSelectRound)
        if (war == null) {
            Text(
                stringResource(Res.string.cwl_round_not_drawn),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            WarDetails(war, context)
        }
        GroupTable(cwl.clans, ownTag = war?.clan?.tag ?: cwl.rounds.firstNotNullOfOrNull { it.war?.clan?.tag })
    }
}

/** R1 ✓ … R7. Rodada não sorteada fica desabilitada. */
@Composable
private fun RoundChips(cwl: CwlDto, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        cwl.rounds.forEach { round ->
            val ended = round.war?.state == WarState.WAR_ENDED
            FilterChip(
                selected = round.round == selected,
                onClick = { onSelect(round.round) },
                enabled = round.war != null,
                label = { Text(stringResource(Res.string.cwl_round_chip, round.round)) },
                leadingIcon = if (ended) {
                    { Icon(painterResource(Res.drawable.ic_check), contentDescription = null, Modifier.size(16.dp)) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun GroupTable(clans: List<CwlClanDto>, ownTag: String?) {
    if (clans.isEmpty()) return
    SectionHeader(stringResource(Res.string.cwl_group)) {
        Text(
            stringResource(Res.string.cwl_group_columns),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = Spacing.xs)) {
            clans.forEachIndexed { index, clan ->
                val isOurs = clan.tag == ownTag
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.xs).youHighlight(isOurs)
                        .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(16.dp),
                    )
                    ClanBadge(clan.badgeUrl, contentDescription = null, size = 24.dp)
                    Text(
                        clan.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = youContentColor(isOurs),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    StarIcon(filled = true)
                    Text(
                        "${clan.stars} · ${clan.destructionPercentage.toInt()}%",
                        style = MaterialTheme.typography.bodyMedium.tabular(),
                        color = youContentColor(isOurs),
                    )
                }
            }
        }
    }
}
