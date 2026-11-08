package io.github.danilodox.warbook.ui.clan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import io.github.danilodox.warbook.resources.ic_check
import io.github.danilodox.warbook.resources.player_tag
import io.github.danilodox.warbook.ui.common.ClanBadge
import io.github.danilodox.warbook.ui.components.StatRow
import io.github.danilodox.warbook.ui.components.StatTile
import io.github.danilodox.warbook.ui.components.TownHallShield
import io.github.danilodox.warbook.ui.components.YouTag
import io.github.danilodox.warbook.ui.components.grouped
import io.github.danilodox.warbook.ui.components.youContentColor
import io.github.danilodox.warbook.ui.components.youHighlight
import io.github.danilodox.warbook.ui.search.label
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.WarbookTheme
import io.github.danilodox.warbook.ui.theme.tabular
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Brasão, nome, tag e "Nível · membros". [action] fica à direita (o "Seguir" da tela Clã). */
@Composable
fun ClanHeader(clan: ClanDto, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ClanBadge(clan.badgeUrl, contentDescription = null, size = 48.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(
                clan.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(Res.string.player_tag, clan.tag),
                style = WarbookTheme.text.tag,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(Res.string.clan_level_members, clan.clanLevel, clan.memberCount),
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (showHeader) ClanHeader(clan)
        if (clan.description.isNotBlank()) Text(clan.description, style = MaterialTheme.typography.bodyMedium)
        StatRow {
            StatTile(stringResource(Res.string.clan_war_wins), clan.warWins.grouped())
            StatTile(stringResource(Res.string.clan_win_streak), clan.warWinStreak.toString())
        }
        StatRow {
            StatTile(stringResource(Res.string.clan_war_league), clan.warLeague ?: unknown)
            StatTile(stringResource(Res.string.clan_capital_league), clan.capitalLeague ?: unknown)
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                val log = if (clan.isWarLogPublic) Res.string.clan_war_log_public else Res.string.clan_war_log_private
                InfoRow(Res.string.clan_war_log, stringResource(log))
                InfoDivider()
                InfoRow(Res.string.clan_war_frequency, clan.warFrequency.label()?.let { stringResource(it) } ?: unknown)
                InfoDivider()
                InfoRow(Res.string.clan_requirements, requirements(clan))
                InfoDivider()
                InfoRow(Res.string.clan_join, clan.joinType.label()?.let { stringResource(it) } ?: unknown)
                InfoDivider()
                InfoRow(Res.string.clan_location, clan.location ?: unknown)
            }
        }
        if (clan.isWarLogPublic) {
            OutlinedButton(onClick = onOpenWar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.clan_open_war))
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    stringResource(Res.string.clan_war_log_private_hint),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(Spacing.md),
                )
            }
        }
    }
}

/** C2: fichas de ordem, a legenda e a lista. Tocar abre a tela Jogador; [accountTag] marca "você". */
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
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        item { SortChips(sort, onSort) }
        item {
            Text(
                stringResource(Res.string.clan_members_season) + " · " + stringResource(Res.string.clan_members_legend),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            )
        }
        itemsIndexed(members, key = { _, item -> item.tag }) { index, member ->
            MemberRow(
                index = index + 1,
                member = member,
                isYou = member.tag == accountTag,
                onClick = { onOpenMember(member.tag) },
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
            val isSelected = sort == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSort(sort) },
                label = { Text(stringResource(label)) },
                leadingIcon = if (isSelected) {
                    { Icon(painterResource(Res.drawable.ic_check), contentDescription = null, Modifier.size(16.dp)) }
                } else {
                    null
                },
            )
        }
    }
}

/** Posição, escudo do CV, nome (com "você"), cargo · troféus, e à direita doou e ↓ recebeu. */
@Composable
private fun MemberRow(index: Int, member: ClanMemberDto, isYou: Boolean, onClick: () -> Unit) {
    val content = youContentColor(isYou)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm)
            .youHighlight(isYou)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            index.toString(),
            style = MaterialTheme.typography.bodySmall.tabular(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(20.dp),
        )
        TownHallShield(member.townHallLevel)
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
            Text(
                stringResource(
                    Res.string.clan_member_line,
                    stringResource(member.role.label),
                    member.trophies.grouped(),
                ),
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(member.donations.grouped(), style = MaterialTheme.typography.titleSmall.tabular(), color = content)
            Text(
                stringResource(Res.string.clan_member_received, member.donationsReceived.grouped()),
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoRow(label: StringResource, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium.tabular(), textAlign = TextAlign.End)
    }
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(Modifier.padding(horizontal = Spacing.lg), color = MaterialTheme.colorScheme.outlineVariant)
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
