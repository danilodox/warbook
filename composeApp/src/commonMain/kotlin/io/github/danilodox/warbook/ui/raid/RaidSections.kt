package io.github.danilodox.warbook.ui.raid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import io.github.danilodox.warbook.api.RaidAbsentMemberDto
import io.github.danilodox.warbook.api.RaidClanDto
import io.github.danilodox.warbook.api.RaidMemberDto
import io.github.danilodox.warbook.api.RaidSeasonDto
import io.github.danilodox.warbook.api.RaidState
import io.github.danilodox.warbook.raid.unusedAttacks
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.raid_capital_gold
import io.github.danilodox.warbook.resources.raid_clan_line
import io.github.danilodox.warbook.resources.raid_member_line
import io.github.danilodox.warbook.resources.raid_member_medals
import io.github.danilodox.warbook.resources.raid_members_title
import io.github.danilodox.warbook.resources.raid_not_attacked_hint
import io.github.danilodox.warbook.resources.raid_not_attacked_title
import io.github.danilodox.warbook.resources.raid_unused_attacks
import io.github.danilodox.warbook.ui.common.ClanBadge
import io.github.danilodox.warbook.ui.components.Panel
import io.github.danilodox.warbook.ui.components.SectionHeader
import io.github.danilodox.warbook.ui.components.TownHallShield
import io.github.danilodox.warbook.ui.components.YouTag
import io.github.danilodox.warbook.ui.components.grouped
import io.github.danilodox.warbook.ui.components.youContentColor
import io.github.danilodox.warbook.ui.components.youHighlight
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.tabular
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Blocos da sub-aba Capital: quem faltou, quem atacou e os clãs do raid.

/**
 * Quem não atacou (só na temporada atual) e quantos ataques sobraram. Fundo neutro do painel: com muitos nomes,
 * o vermelho de "pendente" tomava a tela; o destaque fica só no título.
 */
@Composable
internal fun MissingCard(season: RaidSeasonDto, accountTag: String?) {
    Panel {
        Text(
            stringResource(Res.string.raid_unused_attacks, season.unusedAttacks()),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.tertiary,
        )
        if (season.notAttacked.isNotEmpty()) {
            Text(
                stringResource(Res.string.raid_not_attacked_title, season.notAttacked.size),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            season.notAttacked.forEach { AbsentRow(it, isYou = it.tag == accountTag) }
            Text(
                stringResource(Res.string.raid_not_attacked_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AbsentRow(member: RaidAbsentMemberDto, isYou: Boolean) {
    Row(
        Modifier.fillMaxWidth().youHighlight(isYou).padding(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        TownHallShield(member.townHallLevel)
        Text(
            member.name,
            style = MaterialTheme.typography.bodyMedium,
            color = youContentColor(isYou),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (isYou) YouTag()
    }
}

internal fun LazyListScope.memberRows(season: RaidSeasonDto, accountTag: String?) {
    if (season.members.isEmpty()) return
    item {
        SectionHeader(stringResource(Res.string.raid_members_title, season.members.size))
    }
    items(season.members, key = { "member-${it.tag}" }) { member ->
        MemberRow(member, isYou = member.tag == accountTag, medalsPaid = season.state != RaidState.ONGOING)
    }
}

/** Nome, "x de 5/6 ataques" (em destaque se faltou) e medalhas; à direita o Ouro da Capital saqueado. */
@Composable
private fun MemberRow(member: RaidMemberDto, isYou: Boolean, medalsPaid: Boolean) {
    val content = youContentColor(isYou)
    val missed = member.attacks < member.attackLimit
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .youHighlight(isYou)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    member.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isYou) YouTag()
            }
            val attacks = stringResource(Res.string.raid_member_line, member.attacks, member.attackLimit)
            val line = if (medalsPaid && member.medals > 0) {
                attacks + " · " + stringResource(Res.string.raid_member_medals, member.medals.grouped())
            } else {
                attacks
            }
            Text(
                line,
                style = MaterialTheme.typography.bodySmall.tabular(),
                fontWeight = if (missed) FontWeight.Bold else null,
                color = if (missed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                member.capitalResourcesLooted.grouped(),
                style = MaterialTheme.typography.titleSmall.tabular(),
                color = content,
            )
            Text(
                stringResource(Res.string.raid_capital_gold),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun LazyListScope.clanLog(title: StringResource, clans: List<RaidClanDto>) {
    if (clans.isEmpty()) return
    item { SectionHeader(stringResource(title)) }
    // Chave pela posição: o mesmo clã pode aparecer no ataque e na defesa (e a API não garante tag única).
    itemsIndexed(clans, key = { index, _ -> "${title.key}-$index" }) { _, clan -> ClanLogRow(clan) }
}

/** Brasão, nome e "destruídos/total distritos · n ataques". */
@Composable
private fun ClanLogRow(clan: RaidClanDto) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ClanBadge(clan.badgeUrl, contentDescription = null, size = BADGE_SIZE)
        Column(Modifier.weight(1f)) {
            Text(
                clan.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(
                    Res.string.raid_clan_line,
                    clan.districtsDestroyed,
                    clan.districtCount,
                    clan.attackCount,
                ),
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val BADGE_SIZE = Spacing.xl + Spacing.sm
