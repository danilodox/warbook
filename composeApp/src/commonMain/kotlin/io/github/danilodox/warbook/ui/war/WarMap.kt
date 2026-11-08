package io.github.danilodox.warbook.ui.war

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.WarMemberDto
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.war_map
import io.github.danilodox.warbook.resources.war_town_hall
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

private const val MAX_STARS = 3

/** Mapa espelho: cada posição do nosso lado contra a mesma posição do lado deles. */
@Composable
internal fun WarMap(war: CurrentWarDto, accountTag: String?) {
    val ours = war.clan?.members.orEmpty()
    val theirs = war.opponent?.members.orEmpty().associateBy { it.mapPosition }
    if (ours.isEmpty()) return
    Text(stringResource(Res.string.war_map), style = MaterialTheme.typography.titleSmall)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ours.forEach { member ->
                MapRow(member, theirs[member.mapPosition], showStars = war.state != WarState.PREPARATION, accountTag)
            }
        }
    }
}

@Composable
private fun MapRow(ours: WarMemberDto, theirs: WarMemberDto?, showStars: Boolean, accountTag: String?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("${ours.mapPosition}", style = MaterialTheme.typography.labelSmall)
        MemberCell(ours, showStars, isYou = ours.tag == accountTag, Modifier.weight(1f))
        Text("×", style = MaterialTheme.typography.labelSmall)
        if (theirs != null) MemberCell(theirs, showStars, isYou = false, Modifier.weight(1f), end = true)
    }
}

@Composable
private fun MemberCell(
    member: WarMemberDto,
    showStars: Boolean,
    isYou: Boolean,
    modifier: Modifier,
    end: Boolean = false,
) {
    val align = if (end) TextAlign.End else TextAlign.Start
    Column(modifier) {
        Text(
            "${member.name} · ${stringResource(Res.string.war_town_hall, member.townHallLevel)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isYou) FontWeight.Bold else null,
            textAlign = align,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showStars) {
            Text(
                member.attacks.joinToString(" · ") { stars(it.stars) }.ifEmpty { "—" },
                style = MaterialTheme.typography.bodySmall,
                textAlign = align,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun stars(count: Int) = "★".repeat(count) + "☆".repeat((MAX_STARS - count).coerceAtLeast(0))
