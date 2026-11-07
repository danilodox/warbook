package io.github.danilodox.warbook.ui.war

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.war_all_attacked
import io.github.danilodox.warbook.resources.war_attacks
import io.github.danilodox.warbook.resources.war_destruction
import io.github.danilodox.warbook.resources.war_ends_in
import io.github.danilodox.warbook.resources.war_matchmaking_body
import io.github.danilodox.warbook.resources.war_matchmaking_title
import io.github.danilodox.warbook.resources.war_missed_title
import io.github.danilodox.warbook.resources.war_not_in_war_body
import io.github.danilodox.warbook.resources.war_not_in_war_title
import io.github.danilodox.warbook.resources.war_pending_note
import io.github.danilodox.warbook.resources.war_pending_title
import io.github.danilodox.warbook.resources.war_stars
import io.github.danilodox.warbook.resources.war_starts_in
import io.github.danilodox.warbook.resources.war_state_ended
import io.github.danilodox.warbook.resources.war_state_in_war
import io.github.danilodox.warbook.resources.war_state_preparation
import io.github.danilodox.warbook.resources.war_strength_average
import io.github.danilodox.warbook.resources.war_strength_max
import io.github.danilodox.warbook.resources.war_strength_title
import io.github.danilodox.warbook.resources.war_town_hall
import io.github.danilodox.warbook.resources.war_you
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.war.PendingAttacker
import io.github.danilodox.warbook.war.WarSummary
import io.github.danilodox.warbook.war.pendingAttackers
import io.github.danilodox.warbook.war.result
import io.github.danilodox.warbook.war.strength
import io.github.danilodox.warbook.war.summaryPartsFor
import org.jetbrains.compose.resources.stringResource

/** Sub-aba Guerra atual: G1 (em guerra), G2 (preparação), encerrada e G4 (sem guerra). */
@Composable
fun CurrentWarContent(war: CurrentWarDto, context: WarContext, modifier: Modifier = Modifier) {
    when (war.state) {
        WarState.NOT_IN_WAR -> Message(Res.string.war_not_in_war_title, Res.string.war_not_in_war_body, modifier)
        WarState.MATCHMAKING -> Message(Res.string.war_matchmaking_title, Res.string.war_matchmaking_body, modifier)
        else -> Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) { WarDetails(war, WarSummary.war(war, context.now, context.texts, context.summaryOptions), context) }
    }
}

/** Placar, o bloco do momento (força, pendentes ou perdidos), o resumo e o mapa. Usado também na CWL. */
@Composable
internal fun WarDetails(war: CurrentWarDto, summary: String?, context: WarContext) {
    ScoreCard(war, context)
    when (war.state) {
        WarState.PREPARATION -> StrengthCard(war, context)
        WarState.IN_WAR -> AttackersCard(war.pendingAttackers(), war, context, ended = false)
        WarState.WAR_ENDED -> AttackersCard(war.pendingAttackers(), war, context, ended = true)
        WarState.NOT_IN_WAR, WarState.MATCHMAKING -> Unit
    }
    if (summary != null) {
        SummaryActions(
            summary = summary,
            parts = summaryPartsFor(war.state),
            options = context.summaryOptions,
            ended = war.state == WarState.WAR_ENDED,
            onToggle = context.onToggleSummaryPart,
        )
    }
    WarMap(war, context.accountTag)
}

@Composable
private fun ScoreCard(war: CurrentWarDto, context: WarContext) {
    val ours = war.clan ?: return
    val theirs = war.opponent ?: return
    val texts = context.texts
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(war.state.label()), style = MaterialTheme.typography.labelLarge)
                Text(timeLine(war, context), style = MaterialTheme.typography.bodySmall)
            }
            war.result()?.takeIf { war.state == WarState.WAR_ENDED }?.let {
                Text(texts.result(it), style = MaterialTheme.typography.titleMedium)
            }
            ScoreRow(ours.name, "${war.teamSize} × ${war.teamSize}", theirs.name, MaterialTheme.typography.titleSmall)
            if (war.state != WarState.PREPARATION) {
                ScoreRow(
                    ours.stars.toString(),
                    stringResource(Res.string.war_stars),
                    theirs.stars.toString(),
                    MaterialTheme.typography.headlineMedium,
                )
                ScoreRow(
                    "${texts.decimal(ours.destructionPercentage)}%",
                    stringResource(Res.string.war_destruction),
                    "${texts.decimal(theirs.destructionPercentage)}%",
                )
                val total = war.teamSize * war.attacksPerMember
                ScoreRow("${ours.attacks}/$total", stringResource(Res.string.war_attacks), "${theirs.attacks}/$total")
            }
        }
    }
}

@Composable
private fun timeLine(war: CurrentWarDto, context: WarContext): String {
    val texts = context.texts
    return when (war.state) {
        WarState.PREPARATION ->
            stringResource(Res.string.war_starts_in, texts.duration((war.startTime ?: context.now) - context.now))
        WarState.IN_WAR -> stringResource(
            Res.string.war_ends_in,
            texts.duration(
                (war.endTime ?: context.now) - context.now,
            ),
        )
        else -> ""
    }
}

private fun WarState.label() = when (this) {
    WarState.PREPARATION -> Res.string.war_state_preparation
    WarState.WAR_ENDED -> Res.string.war_state_ended
    else -> Res.string.war_state_in_war
}

/** Nosso lado · rótulo · lado deles. */
@Composable
private fun ScoreRow(
    ours: String,
    label: String,
    theirs: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(ours, style = style, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Text(
            theirs,
            style = style,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Quem falta atacar (em destaque) ou, na guerra encerrada, quem não atacou. */
@Composable
private fun AttackersCard(attackers: List<PendingAttacker>, war: CurrentWarDto, context: WarContext, ended: Boolean) {
    if (attackers.isEmpty()) {
        Text("✅ ${stringResource(Res.string.war_all_attacked)}", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val total = attackers.sumOf { it.remaining }
    val colors = if (ended) {
        CardDefaults.cardColors()
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    }
    Card(colors = colors, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            val title = if (ended) Res.string.war_missed_title else Res.string.war_pending_title
            Text("⚠ ${stringResource(title, total)}", style = MaterialTheme.typography.titleSmall)
            val you = stringResource(Res.string.war_you)
            attackers.forEach {
                val count = if (war.attacksPerMember > 1) " (${it.remaining})" else ""
                val isYou = it.tag == context.accountTag
                Text(
                    "${it.mapPosition}. ${it.name}$count" + if (isYou) " · $you" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isYou) FontWeight.Bold else null,
                )
            }
            if (!ended) Text(stringResource(Res.string.war_pending_note), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** G2: comparação de força só por CV. */
@Composable
private fun StrengthCard(war: CurrentWarDto, context: WarContext) {
    val strength = war.strength() ?: return
    val texts = context.texts
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Text(stringResource(Res.string.war_strength_title), style = MaterialTheme.typography.titleSmall)
            ScoreRow(
                texts.decimal(strength.clan.averageTownHall),
                stringResource(Res.string.war_strength_average),
                texts.decimal(strength.opponent.averageTownHall),
            )
            ScoreRow(
                strength.clan.maxTownHall.toString(),
                stringResource(Res.string.war_strength_max),
                strength.opponent.maxTownHall.toString(),
            )
            strength.townHallLevels.forEach { level ->
                ScoreRow(
                    (strength.clan.townHallCounts[level] ?: 0).toString(),
                    stringResource(Res.string.war_town_hall, level),
                    (strength.opponent.townHallCounts[level] ?: 0).toString(),
                )
            }
        }
    }
}
