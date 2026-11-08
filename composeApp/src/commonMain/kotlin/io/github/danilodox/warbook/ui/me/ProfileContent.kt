package io.github.danilodox.warbook.ui.me

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.api.UnitLevelDto
import io.github.danilodox.warbook.data.Account
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.ic_check
import io.github.danilodox.warbook.resources.ic_chevron_right
import io.github.danilodox.warbook.resources.ic_expand_more
import io.github.danilodox.warbook.resources.me_attack_wins
import io.github.danilodox.warbook.resources.me_donations
import io.github.danilodox.warbook.resources.me_equipment
import io.github.danilodox.warbook.resources.me_heroes
import io.github.danilodox.warbook.resources.me_maxed_count
import io.github.danilodox.warbook.resources.me_pets
import io.github.danilodox.warbook.resources.me_received
import io.github.danilodox.warbook.resources.me_season
import io.github.danilodox.warbook.resources.me_spells_sieges
import io.github.danilodox.warbook.resources.me_switch_account
import io.github.danilodox.warbook.resources.me_troops
import io.github.danilodox.warbook.resources.me_verified
import io.github.danilodox.warbook.resources.me_verify_account
import io.github.danilodox.warbook.resources.player_clan_role
import io.github.danilodox.warbook.resources.player_level
import io.github.danilodox.warbook.resources.player_no_clan
import io.github.danilodox.warbook.resources.player_tag
import io.github.danilodox.warbook.resources.player_trophies
import io.github.danilodox.warbook.resources.player_war_stars
import io.github.danilodox.warbook.resources.screen_clan
import io.github.danilodox.warbook.ui.common.ClanBadge
import io.github.danilodox.warbook.ui.common.LeagueIcon
import io.github.danilodox.warbook.ui.components.HeroRow
import io.github.danilodox.warbook.ui.components.SectionHeader
import io.github.danilodox.warbook.ui.components.ShieldSize
import io.github.danilodox.warbook.ui.components.StatRow
import io.github.danilodox.warbook.ui.components.StatTile
import io.github.danilodox.warbook.ui.components.TownHallShield
import io.github.danilodox.warbook.ui.components.UnitGrid
import io.github.danilodox.warbook.ui.components.UnitItem
import io.github.danilodox.warbook.ui.components.grouped
import io.github.danilodox.warbook.ui.search.label
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.WarbookTheme
import io.github.danilodox.warbook.ui.units.UnitNames
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * E1 e T1: [header] (o da conta na aba Eu, o com "Seguir" na tela Jogador), números principais,
 * heróis em lista com barra, o resto em grade com o nível no canto, e a temporada.
 */
@Composable
fun ProfileContent(player: PlayerDto, header: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val language = Locale.current.language
    val heroes = player.heroes.map { it.toItem(language) }
    val grids = listOf(
        Res.string.me_equipment to player.heroEquipment,
        Res.string.me_pets to player.pets,
        Res.string.me_troops to player.troops,
        Res.string.me_spells_sieges to player.spells + player.siegeMachines,
    ).filter { (_, units) -> units.isNotEmpty() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { header() }
        item {
            StatRow {
                StatTile(stringResource(Res.string.player_level), player.expLevel.toString())
                StatTile(stringResource(Res.string.player_trophies), player.trophies.grouped())
                StatTile(stringResource(Res.string.player_war_stars), player.warStars.grouped())
            }
        }
        if (heroes.isNotEmpty()) {
            item(key = "heroes") { UnitSection(Res.string.me_heroes, heroes) }
            items(heroes, key = { "hero-${it.key}" }) { HeroRow(it) }
        }
        grids.forEach { (title, units) ->
            val list = units.map { it.toItem(language) }
            item(key = title.key) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    UnitSection(title, list)
                    UnitGrid(list)
                }
            }
        }
        item { SectionHeader(stringResource(Res.string.me_season)) }
        item {
            StatRow {
                StatTile(stringResource(Res.string.me_donations), player.donations.grouped())
                StatTile(stringResource(Res.string.me_received), player.donationsReceived.grouped())
                StatTile(stringResource(Res.string.me_attack_wins), player.attackWins.grouped())
            }
        }
    }
}

/** Título da seção com quantos já estão no nível máximo. */
@Composable
private fun UnitSection(title: StringResource, items: List<UnitItem>) {
    SectionHeader(stringResource(title)) {
        Text(
            stringResource(Res.string.me_maxed_count, items.count { it.isMax }, items.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Cabeçalho do perfil: escudo do CV, nome (com [nameExtras] ao lado), tag, clã e liga.
 * Com [onOpenClan] a linha do clã abre a tela Clã.
 */
@Composable
fun ProfileHeader(
    player: PlayerDto,
    modifier: Modifier = Modifier,
    onNameClick: (() -> Unit)? = null,
    onOpenClan: ((String) -> Unit)? = null,
    nameExtras: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        TownHallShield(player.townHallLevel, size = ShieldSize.Large)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                NameText(player.name, onNameClick, Modifier.weight(1f, fill = false))
                nameExtras()
            }
            Text(
                stringResource(Res.string.player_tag, player.tag),
                style = WarbookTheme.text.tag,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ClanLine(player, onOpenClan)
            LeagueLine(player)
        }
    }
}

@Composable
private fun NameText(name: String, onClick: (() -> Unit)?, modifier: Modifier) {
    val clickable = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(
            onClickLabel = stringResource(Res.string.me_switch_account),
            role = Role.Button,
            onClick = onClick,
        )
    }
    Row(modifier.then(clickable), verticalAlignment = Alignment.CenterVertically) {
        Text(
            name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        // O nome troca de conta (E2).
        if (onClick != null) {
            Icon(painterResource(Res.drawable.ic_expand_more), contentDescription = null, Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ClanLine(player: PlayerDto, onOpenClan: ((String) -> Unit)?) {
    val clan = player.clan
    if (clan == null) {
        Text(
            stringResource(Res.string.player_no_clan),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val text = player.role
        ?.let { stringResource(Res.string.player_clan_role, clan.name, stringResource(it.label)) }
        ?: clan.name
    val clickable = if (onOpenClan == null) {
        Modifier
    } else {
        Modifier.clickable(onClickLabel = stringResource(Res.string.screen_clan), role = Role.Button) {
            onOpenClan(clan.tag)
        }
    }
    Row(
        clickable.padding(vertical = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        ClanBadge(clan.badgeUrl, contentDescription = null, size = 20.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (onOpenClan != null) {
            Icon(painterResource(Res.drawable.ic_chevron_right), contentDescription = null, Modifier.size(16.dp))
        }
    }
}

/** Cabeçalho da aba Eu: o nome troca de conta, selo de verificado e o convite para verificar. */
@Composable
fun AccountHeader(player: PlayerDto, account: Account, onOpenAccounts: () -> Unit, onVerify: () -> Unit) {
    Column {
        ProfileHeader(player, onNameClick = onOpenAccounts) {
            if (account.isVerified) VerifiedChip()
        }
        if (!account.isVerified) {
            TextButton(onClick = onVerify, modifier = Modifier.padding(start = Spacing.xxxl + Spacing.md)) {
                Text(stringResource(Res.string.me_verify_account))
            }
        }
    }
}

@Composable
private fun VerifiedChip() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.xs + Spacing.xxs, vertical = Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            Icon(painterResource(Res.drawable.ic_check), contentDescription = null, Modifier.size(12.dp))
            Text(stringResource(Res.string.me_verified).lowercase(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Liga de troféus com o ícone que a API entrega; sem liga, nada. */
@Composable
internal fun LeagueLine(player: PlayerDto) {
    val league = player.league ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        LeagueIcon(player.leagueIconUrl, contentDescription = null, size = 20.dp)
        Text(league, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A unidade da API como aparece na tela: [UnitItem.key] é o nome da API, [UnitItem.name] o do idioma. */
internal fun UnitLevelDto.toItem(language: String = "en"): UnitItem =
    UnitItem(key = name, name = UnitNames.display(name, language), level = level, maxLevel = maxLevel)
