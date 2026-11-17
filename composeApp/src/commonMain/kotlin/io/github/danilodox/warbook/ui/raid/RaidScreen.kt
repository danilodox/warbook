package io.github.danilodox.warbook.ui.raid

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.api.RaidSeasonDto
import io.github.danilodox.warbook.api.RaidState
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.WarbookRepository
import io.github.danilodox.warbook.data.currentTimeMillis
import io.github.danilodox.warbook.raid.RaidSummary
import io.github.danilodox.warbook.raid.attackedLess
import io.github.danilodox.warbook.raid.dayMonthUtc
import io.github.danilodox.warbook.raid.fullMedals
import io.github.danilodox.warbook.raid.raidSummaryTextsFor
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.raid_attack_log_title
import io.github.danilodox.warbook.resources.raid_attacks
import io.github.danilodox.warbook.resources.raid_capital_gold
import io.github.danilodox.warbook.resources.raid_defense_log_title
import io.github.danilodox.warbook.resources.raid_districts
import io.github.danilodox.warbook.resources.raid_empty_body
import io.github.danilodox.warbook.resources.raid_empty_title
import io.github.danilodox.warbook.resources.raid_medals_defense
import io.github.danilodox.warbook.resources.raid_medals_full
import io.github.danilodox.warbook.resources.raid_medals_pending
import io.github.danilodox.warbook.resources.raid_medals_per_attack
import io.github.danilodox.warbook.resources.raid_medals_title
import io.github.danilodox.warbook.resources.raid_ongoing
import io.github.danilodox.warbook.resources.raid_raids
import io.github.danilodox.warbook.resources.raid_season_chip
import io.github.danilodox.warbook.ui.components.Panel
import io.github.danilodox.warbook.ui.components.StatRow
import io.github.danilodox.warbook.ui.components.StatTile
import io.github.danilodox.warbook.ui.components.grouped
import io.github.danilodox.warbook.ui.me.OfflineCard
import io.github.danilodox.warbook.ui.me.ProfileSkeleton
import io.github.danilodox.warbook.ui.me.StaleBanner
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.war.Message
import io.github.danilodox.warbook.ui.war.SummaryDock
import io.github.danilodox.warbook.war.SummaryOptions
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Sub-aba Capital de qualquer clã (o seu na aba Clã, os outros na tela Clã). Busca só quando abre. */
@Composable
fun ClanCapitalTab(clanTag: String, clanName: String?, modifier: Modifier = Modifier) {
    val accounts: AccountsRepository = koinInject()
    val api: WarbookRepository = koinInject()
    val viewModel = viewModel(key = "clan-raid-$clanTag") { RaidViewModel(clanTag, accounts, api) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.onOpen() }
    Column(modifier.fillMaxSize()) {
        RaidScreen(state, viewModel::refresh, viewModel::selectSeason, Modifier.weight(1f))
        val season = state.season
        if (season != null) {
            // Igual à Guerra: barra presa embaixo com Copiar e Compartilhar (sem personalizar).
            val texts = raidSummaryTextsFor(Locale.current.language)
            val summary = RaidSummary.season(season, clanName, currentTimeMillis(), texts)
            SummaryDock(summary, parts = emptyList(), options = SummaryOptions(), ended = true, onToggle = {})
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaidScreen(
    state: RaidUiState,
    onRefresh: () -> Unit,
    onSelectSeason: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val raids = state.raids
    val error = state.error
    PullToRefreshBox(
        isRefreshing = state.refreshing && raids != null,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            raids != null && raids.seasons.isEmpty() -> Message(Res.string.raid_empty_title, Res.string.raid_empty_body)
            raids != null -> Column(Modifier.fillMaxSize()) {
                if (error != null) StaleBanner(error, state.fetchedAt, onRefresh)
                RaidContent(state, onSelectSeason, Modifier.weight(1f))
            }
            error != null -> OfflineCard(error, onRetry = onRefresh)
            else -> ProfileSkeleton()
        }
    }
}

@Composable
private fun RaidContent(state: RaidUiState, onSelectSeason: (Int) -> Unit, modifier: Modifier) {
    val seasons = state.raids?.seasons.orEmpty()
    val season = state.season ?: return
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { SeasonChips(seasons, state.selected, onSelectSeason) }
        item { MedalsPanel(season) }
        item { SeasonStats(season) }
        if (season.notAttacked.isNotEmpty() || season.attackedLess().isNotEmpty()) {
            item { MissingCard(season, state.accountTag) }
        }
        memberRows(season, state.accountTag)
        clanLog(Res.string.raid_attack_log_title, season.attackLog)
        clanLog(Res.string.raid_defense_log_title, season.defenseLog)
    }
}

/** Uma ficha por fim de semana, rotulada pela data de início. A mais recente primeiro. */
@Composable
private fun SeasonChips(seasons: List<RaidSeasonDto>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        seasons.forEachIndexed { index, season ->
            FilterChip(
                selected = index == selected,
                onClick = { onSelect(index) },
                label = { Text(seasonLabel(season)) },
            )
        }
    }
}

@Composable
private fun seasonLabel(season: RaidSeasonDto): String {
    val start = season.startTime ?: return stringResource(Res.string.raid_ongoing)
    val date = dayMonthUtc(start)
    val label = stringResource(Res.string.raid_season_chip, twoDigits(date.day), twoDigits(date.month))
    return if (season.state == RaidState.ONGOING) "$label · ${stringResource(Res.string.raid_ongoing)}" else label
}

private fun twoDigits(value: Int) = value.toString().padStart(2, '0')

/** Medalhas de raid em destaque: por ataque (e o total com 6) e a de defesa. */
@Composable
private fun MedalsPanel(season: RaidSeasonDto) {
    Panel(color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            stringResource(Res.string.raid_medals_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        if (season.state == RaidState.ONGOING || season.offensiveReward == 0) {
            Text(
                stringResource(Res.string.raid_medals_pending),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            val full = season.fullMedals()
            StatRow {
                StatTile(stringResource(Res.string.raid_medals_per_attack), season.offensiveReward.grouped())
                StatTile(stringResource(Res.string.raid_medals_defense), season.defensiveReward.grouped())
                StatTile(stringResource(Res.string.raid_medals_full), full.grouped())
            }
        }
    }
}

@Composable
private fun SeasonStats(season: RaidSeasonDto) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xxs)) {
        StatRow {
            StatTile(stringResource(Res.string.raid_attacks), season.totalAttacks.grouped())
            StatTile(stringResource(Res.string.raid_districts), season.enemyDistrictsDestroyed.grouped())
            StatTile(stringResource(Res.string.raid_raids), season.raidsCompleted.grouped())
        }
        StatRow {
            StatTile(stringResource(Res.string.raid_capital_gold), season.capitalTotalLoot.grouped())
        }
    }
}
