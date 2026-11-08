package io.github.danilodox.warbook.ui.war

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.ic_check
import io.github.danilodox.warbook.resources.war_all_attacked
import io.github.danilodox.warbook.resources.war_ends_in
import io.github.danilodox.warbook.resources.war_matchmaking_body
import io.github.danilodox.warbook.resources.war_matchmaking_title
import io.github.danilodox.warbook.resources.war_missed_title
import io.github.danilodox.warbook.resources.war_not_in_war_body
import io.github.danilodox.warbook.resources.war_not_in_war_title
import io.github.danilodox.warbook.resources.war_pending_attacks
import io.github.danilodox.warbook.resources.war_pending_note
import io.github.danilodox.warbook.resources.war_pending_title
import io.github.danilodox.warbook.resources.war_starts_in
import io.github.danilodox.warbook.resources.war_state_ended
import io.github.danilodox.warbook.resources.war_state_in_war
import io.github.danilodox.warbook.resources.war_state_preparation
import io.github.danilodox.warbook.resources.war_strength_average
import io.github.danilodox.warbook.resources.war_strength_max
import io.github.danilodox.warbook.resources.war_strength_title
import io.github.danilodox.warbook.resources.war_town_hall
import io.github.danilodox.warbook.ui.components.Panel
import io.github.danilodox.warbook.ui.components.ScoreSide
import io.github.danilodox.warbook.ui.components.SectionHeader
import io.github.danilodox.warbook.ui.components.WarScoreboard
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.WarbookTheme
import io.github.danilodox.warbook.ui.theme.tabular
import io.github.danilodox.warbook.war.PendingAttacker
import io.github.danilodox.warbook.war.WarSummary
import io.github.danilodox.warbook.war.pendingAttackers
import io.github.danilodox.warbook.war.result
import io.github.danilodox.warbook.war.strength
import io.github.danilodox.warbook.war.summaryPartsFor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Sub-aba Guerra atual: G1 (em guerra), G2 (preparação), encerrada e G4 (sem guerra). */
@Composable
fun CurrentWarContent(war: CurrentWarDto, context: WarContext, modifier: Modifier = Modifier) {
    when (war.state) {
        WarState.NOT_IN_WAR -> Message(Res.string.war_not_in_war_title, Res.string.war_not_in_war_body, modifier)
        WarState.MATCHMAKING -> Message(Res.string.war_matchmaking_title, Res.string.war_matchmaking_body, modifier)
        else -> WarLayout(
            war,
            WarSummary.war(war, context.now, context.texts, context.summaryOptions),
            context,
            modifier,
        ) {
            WarDetails(war, context)
        }
    }
}

/**
 * Conteúdo rolável em cima e, quando há resumo, a barra do WhatsApp presa embaixo.
 * [war] diz quais partes do resumo dá para escolher.
 */
@Composable
internal fun WarLayout(
    war: CurrentWarDto?,
    summary: String?,
    context: WarContext,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            content = content,
        )
        if (summary != null && war != null) {
            SummaryDock(
                summary = summary,
                parts = summaryPartsFor(war.state),
                options = context.summaryOptions,
                ended = war.state == WarState.WAR_ENDED,
                onToggle = context.onToggleSummaryPart,
            )
        }
    }
}

/** Placar, o bloco do momento (força, pendentes ou perdidos) e o mapa. Usado também na CWL. */
@Composable
internal fun WarDetails(war: CurrentWarDto, context: WarContext) {
    ScoreCard(war, context)
    when (war.state) {
        WarState.PREPARATION -> StrengthCard(war, context)
        WarState.IN_WAR -> AttackersCard(war.pendingAttackers(), context, ended = false)
        WarState.WAR_ENDED -> AttackersCard(war.pendingAttackers(), context, ended = true)
        WarState.NOT_IN_WAR, WarState.MATCHMAKING -> Unit
    }
    WarMap(war, context.accountTag)
}

@Composable
private fun ScoreCard(war: CurrentWarDto, context: WarContext) {
    val ours = war.clan ?: return
    val theirs = war.opponent ?: return
    val texts = context.texts
    val total = war.teamSize * war.attacksPerMember
    WarScoreboard(
        state = stringResource(war.state.label()),
        time = timeLine(war, context),
        ours = ScoreSide(
            ours.name,
            ours.badgeUrl,
            ours.stars,
            "${texts.decimal(ours.destructionPercentage)}% · ${ours.attacks}/$total",
        ),
        theirs = ScoreSide(
            theirs.name,
            theirs.badgeUrl,
            theirs.stars,
            "${texts.decimal(theirs.destructionPercentage)}% · ${theirs.attacks}/$total",
        ),
        showStars = war.state != WarState.PREPARATION,
        center = "${war.teamSize} × ${war.teamSize}",
        result = war.result()?.takeIf { war.state == WarState.WAR_ENDED }?.let { texts.result(it) },
    )
}

@Composable
private fun timeLine(war: CurrentWarDto, context: WarContext): String {
    val texts = context.texts
    return when (war.state) {
        WarState.PREPARATION ->
            stringResource(Res.string.war_starts_in, texts.duration((war.startTime ?: context.now) - context.now))
        WarState.IN_WAR ->
            stringResource(Res.string.war_ends_in, texts.duration((war.endTime ?: context.now) - context.now))
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
    style: TextStyle = MaterialTheme.typography.bodyMedium.tabular(),
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(ours, style = style, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

/**
 * Quem falta atacar, em vermelho-sangue, com "você" em destaque. Na guerra encerrada, quem não atacou,
 * num bloco neutro.
 */
@Composable
private fun AttackersCard(attackers: List<PendingAttacker>, context: WarContext, ended: Boolean) {
    if (attackers.isEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Icon(
                painterResource(Res.drawable.ic_check),
                contentDescription = null,
                tint = WarbookTheme.colors.success,
                modifier = Modifier.size(20.dp),
            )
            Text(stringResource(Res.string.war_all_attacked), style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    val colors = MaterialTheme.colorScheme
    val container = if (ended) colors.surfaceContainerHigh else colors.tertiaryContainer
    val content = if (ended) colors.onSurface else colors.onTertiaryContainer
    val remaining = attackers.sumOf { it.remaining }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            val title = if (ended) Res.string.war_missed_title else Res.string.war_pending_title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(title, attackers.size),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (remaining != attackers.size) {
                    Text(
                        stringResource(Res.string.war_pending_attacks, remaining),
                        style = MaterialTheme.typography.bodySmall.tabular(),
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xxs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xxs),
            ) {
                attackers.forEach { AttackerChip(it, isYou = it.tag == context.accountTag, ended) }
            }
            if (!ended) Text(stringResource(Res.string.war_pending_note), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** "3. Bia (2)": posição, nome e, com dois ataques, quantos faltam. A sua ficha vem cheia. */
@Composable
private fun AttackerChip(attacker: PendingAttacker, isYou: Boolean, ended: Boolean) {
    val colors = MaterialTheme.colorScheme
    val (background, foreground) = when {
        isYou && ended -> colors.primary to colors.onPrimary
        isYou -> colors.tertiary to colors.onTertiary
        ended -> Color.Transparent to colors.onSurface
        else -> Color.Transparent to colors.onTertiaryContainer
    }
    val count = if (attacker.remaining > 1) " (${attacker.remaining})" else ""
    Surface(
        color = background,
        contentColor = foreground,
        shape = MaterialTheme.shapes.small,
        border = if (isYou) null else BorderStroke(1.dp, colors.outline),
    ) {
        Text(
            "${attacker.mapPosition}. ${attacker.name}$count",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}

/** G2: comparação de força só por CV. */
@Composable
private fun StrengthCard(war: CurrentWarDto, context: WarContext) {
    val strength = war.strength() ?: return
    val texts = context.texts
    SectionHeader(stringResource(Res.string.war_strength_title))
    Panel {
        ScoreRow(
            texts.decimal(strength.clan.averageTownHall),
            stringResource(Res.string.war_strength_average),
            texts.decimal(strength.opponent.averageTownHall),
            MaterialTheme.typography.titleMedium.tabular(),
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
