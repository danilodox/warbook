package io.github.danilodox.warbook.ui.war

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.WarMemberDto
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.war_map
import io.github.danilodox.warbook.resources.war_map_missed
import io.github.danilodox.warbook.resources.war_map_pending
import io.github.danilodox.warbook.ui.components.SectionHeader
import io.github.danilodox.warbook.ui.components.StarRow
import io.github.danilodox.warbook.ui.components.TownHallShield
import io.github.danilodox.warbook.ui.components.YouTag
import io.github.danilodox.warbook.ui.components.youContentColor
import io.github.danilodox.warbook.ui.components.youHighlight
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.tabular
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Mapa: cada posição do nosso lado com as estrelas de cada ataque (ou o que falta) e, na ponta,
 * o CV de quem está na mesma posição do lado deles.
 */
@Composable
internal fun WarMap(war: CurrentWarDto, accountTag: String?) {
    val ours = war.clan?.members.orEmpty()
    val theirs = war.opponent?.members.orEmpty().associateBy { it.mapPosition }
    if (ours.isEmpty()) return
    SectionHeader(stringResource(Res.string.war_map, war.teamSize))
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = Spacing.xs)) {
            ours.forEach { member ->
                MapRow(member, theirs[member.mapPosition], war, isYou = member.tag == accountTag)
            }
        }
    }
}

@Composable
private fun MapRow(ours: WarMemberDto, theirs: WarMemberDto?, war: CurrentWarDto, isYou: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs)
            .youHighlight(isYou)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs + Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            ours.mapPosition.toString(),
            style = MaterialTheme.typography.bodySmall.tabular(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(20.dp),
        )
        TownHallShield(ours.townHallLevel)
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                ours.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = youContentColor(isYou),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (isYou) YouTag()
        }
        if (war.state != WarState.PREPARATION) Attacks(ours, war)
        if (theirs != null) {
            Text("×", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TownHallShield(theirs.townHallLevel)
        }
    }
}

/** As estrelas de cada ataque feito e, se ainda falta, "2 pendentes" ou, encerrada, "não atacou". */
@Composable
private fun Attacks(member: WarMemberDto, war: CurrentWarDto) {
    val missing = (war.attacksPerMember - member.attacks.size).coerceAtLeast(0)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        member.attacks.forEach { StarRow(it.stars) }
        when {
            missing == 0 -> Unit
            war.state == WarState.WAR_ENDED -> if (member.attacks.isEmpty()) {
                Text(
                    stringResource(Res.string.war_map_missed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            else -> Text(
                pluralStringResource(Res.plurals.war_map_pending, missing, missing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
