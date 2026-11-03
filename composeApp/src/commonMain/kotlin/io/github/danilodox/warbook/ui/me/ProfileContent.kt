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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.api.UnitLevelDto
import io.github.danilodox.warbook.data.Account
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.me_attack_wins
import io.github.danilodox.warbook.resources.me_donations
import io.github.danilodox.warbook.resources.me_equipment
import io.github.danilodox.warbook.resources.me_heroes
import io.github.danilodox.warbook.resources.me_pets
import io.github.danilodox.warbook.resources.me_received
import io.github.danilodox.warbook.resources.me_season
import io.github.danilodox.warbook.resources.me_sieges
import io.github.danilodox.warbook.resources.me_spells
import io.github.danilodox.warbook.resources.me_switch_account
import io.github.danilodox.warbook.resources.me_troops
import io.github.danilodox.warbook.resources.me_verified
import io.github.danilodox.warbook.resources.me_verify_account
import io.github.danilodox.warbook.resources.player_clan_role
import io.github.danilodox.warbook.resources.player_level
import io.github.danilodox.warbook.resources.player_no_clan
import io.github.danilodox.warbook.resources.player_tag
import io.github.danilodox.warbook.resources.player_town_hall
import io.github.danilodox.warbook.resources.player_trophies
import io.github.danilodox.warbook.resources.player_war_stars
import io.github.danilodox.warbook.resources.unit_level
import io.github.danilodox.warbook.ui.search.label
import io.github.danilodox.warbook.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** E1: cabeçalho, números principais, unidades em texto com barra (sem ícones até a v0.6.0) e temporada. */
@Composable
fun ProfileContent(
    player: PlayerDto,
    account: Account,
    onOpenAccounts: () -> Unit,
    onVerify: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = listOf(
        Res.string.me_heroes to player.heroes,
        Res.string.me_equipment to player.heroEquipment,
        Res.string.me_troops to player.troops,
        Res.string.me_spells to player.spells,
        Res.string.me_sieges to player.siegeMachines,
        Res.string.me_pets to player.pets,
    ).filter { (_, units) -> units.isNotEmpty() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        item { ProfileHeader(player, account, onOpenAccounts, onVerify) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Stat(Res.string.player_town_hall, player.townHallLevel)
                Stat(Res.string.player_level, player.expLevel)
                Stat(Res.string.player_trophies, player.trophies)
                Stat(Res.string.player_war_stars, player.warStars)
            }
        }
        groups.forEach { (title, units) ->
            item(key = title.key) { SectionTitle(title) }
            items(units, key = { "${title.key}-${it.name}" }) { UnitRow(it) }
        }
        item { SectionTitle(Res.string.me_season) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Stat(Res.string.me_donations, player.donations)
                Stat(Res.string.me_received, player.donationsReceived)
                Stat(Res.string.me_attack_wins, player.attackWins)
            }
        }
    }
}

@Composable
private fun ProfileHeader(player: PlayerDto, account: Account, onOpenAccounts: () -> Unit, onVerify: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small / 2)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            // O nome troca de conta (E2).
            Text(
                "${player.name} ▾",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.clickable(
                    onClickLabel = stringResource(Res.string.me_switch_account),
                    role = Role.Button,
                    onClick = onOpenAccounts,
                ),
            )
            if (account.isVerified) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                    Text(
                        "✓ ${stringResource(Res.string.me_verified)}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = Spacing.small / 2),
                    )
                }
            }
        }
        Text(stringResource(Res.string.player_tag, player.tag), style = MaterialTheme.typography.bodySmall)
        val clan = player.clan
        val role = player.role
        Text(
            when {
                clan == null -> stringResource(Res.string.player_no_clan)
                role == null -> clan.name
                else -> stringResource(Res.string.player_clan_role, clan.name, stringResource(role.label))
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        player.league?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (!account.isVerified) {
            TextButton(onClick = onVerify) { Text(stringResource(Res.string.me_verify_account)) }
        }
    }
}

@Composable
private fun SectionTitle(title: StringResource) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = Spacing.small),
    )
}

@Composable
private fun UnitRow(unit: UnitLevelDto) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.small / 2)) {
        Row(Modifier.fillMaxWidth()) {
            Text(unit.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                stringResource(Res.string.unit_level, unit.level, unit.maxLevel),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        LinearProgressIndicator(progress = { unit.progress }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RowScope.Stat(label: StringResource, value: Int) {
    OutlinedCard(Modifier.weight(1f)) {
        Column(Modifier.padding(Spacing.small)) {
            Text(stringResource(label), style = MaterialTheme.typography.labelSmall)
            Text(value.toString(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Fração do nível máximo, de 0 a 1. */
internal val UnitLevelDto.progress: Float
    get() = if (maxLevel <= 0) 0f else (level.toFloat() / maxLevel).coerceIn(0f, 1f)
