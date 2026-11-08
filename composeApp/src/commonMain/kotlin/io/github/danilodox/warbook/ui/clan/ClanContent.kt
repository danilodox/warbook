package io.github.danilodox.warbook.ui.clan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.api.ClanJoinType
import io.github.danilodox.warbook.api.ClanMemberDto
import io.github.danilodox.warbook.api.WarFrequency
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.clan_capital_league
import io.github.danilodox.warbook.resources.clan_frequency_always
import io.github.danilodox.warbook.resources.clan_frequency_less_than_once
import io.github.danilodox.warbook.resources.clan_frequency_more_than_once
import io.github.danilodox.warbook.resources.clan_frequency_never
import io.github.danilodox.warbook.resources.clan_frequency_once
import io.github.danilodox.warbook.resources.clan_join
import io.github.danilodox.warbook.resources.clan_join_closed
import io.github.danilodox.warbook.resources.clan_join_invite_only
import io.github.danilodox.warbook.resources.clan_join_open
import io.github.danilodox.warbook.resources.clan_level_members
import io.github.danilodox.warbook.resources.clan_location
import io.github.danilodox.warbook.resources.clan_member_line
import io.github.danilodox.warbook.resources.clan_member_received
import io.github.danilodox.warbook.resources.clan_member_trophies
import io.github.danilodox.warbook.resources.clan_member_you
import io.github.danilodox.warbook.resources.clan_members_legend
import io.github.danilodox.warbook.resources.clan_members_season
import io.github.danilodox.warbook.resources.clan_open_war
import io.github.danilodox.warbook.resources.clan_requirements
import io.github.danilodox.warbook.resources.clan_requirements_town_hall
import io.github.danilodox.warbook.resources.clan_requirements_trophies
import io.github.danilodox.warbook.resources.clan_sort_donations
import io.github.danilodox.warbook.resources.clan_sort_position
import io.github.danilodox.warbook.resources.clan_sort_town_hall
import io.github.danilodox.warbook.resources.clan_sort_trophies
import io.github.danilodox.warbook.resources.clan_unknown_value
import io.github.danilodox.warbook.resources.clan_war_frequency
import io.github.danilodox.warbook.resources.clan_war_league
import io.github.danilodox.warbook.resources.clan_war_log
import io.github.danilodox.warbook.resources.clan_war_log_private
import io.github.danilodox.warbook.resources.clan_war_log_private_hint
import io.github.danilodox.warbook.resources.clan_war_log_public
import io.github.danilodox.warbook.resources.clan_war_wins
import io.github.danilodox.warbook.resources.clan_win_streak
import io.github.danilodox.warbook.resources.player_tag
import io.github.danilodox.warbook.ui.common.ClanBadge
import io.github.danilodox.warbook.ui.common.LeagueIcon
import io.github.danilodox.warbook.ui.search.label
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Brasão, nome, tag e "Nível · membros". [action] fica à direita (o "Seguir" da tela Clã). */
@Composable
fun ClanHeader(clan: ClanDto, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        ClanBadge(clan.badgeUrl, contentDescription = null, size = 56.dp)
        Column(Modifier.weight(1f)) {
            Text(clan.name, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(Res.string.player_tag, clan.tag), style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(Res.string.clan_level_members, clan.clanLevel, clan.memberCount),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        action()
    }
}

/**
 * C1: descrição, ligas, vitórias, requisitos. [showHeader] = falso na tela Clã, que mostra o cabeçalho
 * acima das sub-abas. [onOpenWar] some com registro privado (no lugar, o aviso).
 */
@Composable
fun ClanInfo(clan: ClanDto, showHeader: Boolean, onOpenWar: () -> Unit, modifier: Modifier = Modifier) {
    val unknown = stringResource(Res.string.clan_unknown_value)
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (showHeader) ClanHeader(clan)
        if (clan.description.isNotBlank()) Text(clan.description, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Stat(Res.string.clan_war_league, clan.warLeague ?: unknown)
            Stat(Res.string.clan_capital_league, clan.capitalLeague ?: unknown)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Stat(Res.string.clan_war_wins, clan.warWins.toString())
            Stat(Res.string.clan_win_streak, clan.warWinStreak.toString())
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            val logLabel = if (clan.isWarLogPublic) Res.string.clan_war_log_public else Res.string.clan_war_log_private
            InfoRow(Res.string.clan_war_log, stringResource(logLabel))
            HorizontalDivider()
            InfoRow(Res.string.clan_war_frequency, clan.warFrequency.label()?.let { stringResource(it) } ?: unknown)
            HorizontalDivider()
            InfoRow(Res.string.clan_requirements, requirements(clan))
            HorizontalDivider()
            InfoRow(Res.string.clan_join, clan.joinType.label()?.let { stringResource(it) } ?: unknown)
            HorizontalDivider()
            InfoRow(Res.string.clan_location, clan.location ?: unknown)
        }
        if (clan.isWarLogPublic) {
            OutlinedButton(onClick = onOpenWar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.clan_open_war))
            }
        } else {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                Text(
                    stringResource(Res.string.clan_war_log_private_hint),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
        }
    }
}

/** C2: fichas de ordem e a lista. Tocar abre a tela Jogador; [accountTag] marca "você". */
@Composable
fun ClanMembers(
    clan: ClanDto,
    sort: MemberSort,
    accountTag: String?,
    onSort: (MemberSort) -> Unit,
    onOpenMember: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val members = clan.members.sortedBy(sort)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Spacing.sm),
    ) {
        item {
            Text(
                stringResource(Res.string.clan_members_season),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
        }
        item { SortChips(sort, onSort) }
        itemsIndexed(members, key = { _, item -> item.tag }) { index, member ->
            MemberRow(
                index = index + 1,
                member = member,
                isYou = member.tag == accountTag,
                onClick = { onOpenMember(member.tag) },
            )
            HorizontalDivider()
        }
        item {
            Text(
                stringResource(Res.string.clan_members_legend),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(Spacing.lg),
            )
        }
    }
}

@Composable
private fun SortChips(selected: MemberSort, onSort: (MemberSort) -> Unit) {
    val sorts = listOf(
        MemberSort.POSITION to Res.string.clan_sort_position,
        MemberSort.TOWN_HALL to Res.string.clan_sort_town_hall,
        MemberSort.TROPHIES to Res.string.clan_sort_trophies,
        MemberSort.DONATIONS to Res.string.clan_sort_donations,
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(sorts, key = { it.first }) { (sort, label) ->
            FilterChip(selected = sort == selected, onClick = { onSort(sort) }, label = { Text(stringResource(label)) })
        }
    }
}

/** Posição na lista, nome, cargo · CV · nível, troféus com o ícone da liga, doadas e ↓ recebidas. */
@Composable
private fun MemberRow(index: Int, member: ClanMemberDto, isYou: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(index.toString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(24.dp))
        LeagueIcon(member.league?.iconUrl, contentDescription = member.league?.name, size = 28.dp)
        Column(Modifier.weight(1f)) {
            Text(
                member.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isYou) FontWeight.Bold else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val line = stringResource(
                Res.string.clan_member_line,
                stringResource(member.role.label),
                member.townHallLevel,
                member.expLevel,
            )
            val you = if (isYou) " · ${stringResource(Res.string.clan_member_you)}" else ""
            Text(line + you, style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(Res.string.clan_member_trophies, member.trophies),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(member.donations.toString(), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(Res.string.clan_member_received, member.donationsReceived),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RowScope.Stat(label: StringResource, value: String) {
    OutlinedCard(Modifier.weight(1f)) {
        Column(Modifier.padding(Spacing.sm)) {
            Text(stringResource(label), style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun InfoRow(label: StringResource, value: String) {
    Row(Modifier.fillMaxWidth().padding(Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun requirements(clan: ClanDto): String {
    val townHall = clan.requiredTownHallLevel
    return if (townHall == null) {
        stringResource(Res.string.clan_requirements_trophies, clan.requiredTrophies)
    } else {
        stringResource(Res.string.clan_requirements_town_hall, clan.requiredTrophies, townHall)
    }
}

private fun WarFrequency.label(): StringResource? = when (this) {
    WarFrequency.ALWAYS -> Res.string.clan_frequency_always
    WarFrequency.MORE_THAN_ONCE_PER_WEEK -> Res.string.clan_frequency_more_than_once
    WarFrequency.ONCE_PER_WEEK -> Res.string.clan_frequency_once
    WarFrequency.LESS_THAN_ONCE_PER_WEEK -> Res.string.clan_frequency_less_than_once
    WarFrequency.NEVER -> Res.string.clan_frequency_never
    WarFrequency.UNKNOWN -> null
}

private fun ClanJoinType.label(): StringResource? = when (this) {
    ClanJoinType.OPEN -> Res.string.clan_join_open
    ClanJoinType.INVITE_ONLY -> Res.string.clan_join_invite_only
    ClanJoinType.CLOSED -> Res.string.clan_join_closed
    ClanJoinType.UNKNOWN -> null
}
