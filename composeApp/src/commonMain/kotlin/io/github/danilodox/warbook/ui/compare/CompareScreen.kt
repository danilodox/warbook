package io.github.danilodox.warbook.ui.compare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.ProfileRepository
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.compare_donations
import io.github.danilodox.warbook.resources.compare_general
import io.github.danilodox.warbook.resources.compare_me_ahead
import io.github.danilodox.warbook.resources.compare_them_ahead
import io.github.danilodox.warbook.resources.compare_ties
import io.github.danilodox.warbook.resources.compare_war_stars
import io.github.danilodox.warbook.resources.me_attack_wins
import io.github.danilodox.warbook.resources.me_heroes
import io.github.danilodox.warbook.resources.me_season
import io.github.danilodox.warbook.resources.player_level
import io.github.danilodox.warbook.resources.player_town_hall
import io.github.danilodox.warbook.resources.player_trophies
import io.github.danilodox.warbook.ui.components.ShieldSize
import io.github.danilodox.warbook.ui.components.TownHallShield
import io.github.danilodox.warbook.ui.components.YouTag
import io.github.danilodox.warbook.ui.components.grouped
import io.github.danilodox.warbook.ui.me.NoAccountScreen
import io.github.danilodox.warbook.ui.me.OfflineCard
import io.github.danilodox.warbook.ui.me.ProfileSkeleton
import io.github.danilodox.warbook.ui.me.StaleBanner
import io.github.danilodox.warbook.ui.search.label
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.WarbookTheme
import io.github.danilodox.warbook.ui.theme.tabular
import io.github.danilodox.warbook.ui.units.UnitNames
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** A coluna do nome da linha é um pouco mais larga que as dos valores. */
private const val LABEL_WEIGHT = 1.2f

/** Comparar (T3): a conta ativa contra o jogador [tag]. */
@Composable
fun CompareRoute(tag: String, onAddAccount: () -> Unit, modifier: Modifier = Modifier) {
    val accounts: AccountsRepository = koinInject()
    val profiles: ProfileRepository = koinInject()
    val viewModel = viewModel(key = "compare-$tag") { CompareViewModel(tag, accounts, profiles) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    CompareScreen(state, viewModel::refresh, onAddAccount, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    state: CompareUiState,
    onRefresh: () -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        !state.loaded -> Unit
        !state.hasAccount -> NoAccountScreen(onAddAccount, modifier)
        else -> PullToRefreshBox(
            isRefreshing = state.refreshing && state.comparison != null,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize(),
        ) {
            val comparison = state.comparison
            val error = state.error
            when {
                comparison != null && state.mine != null && state.theirs != null -> Column(Modifier.fillMaxSize()) {
                    if (error != null) StaleBanner(error, state.theirsFetchedAt, onRefresh)
                    CompareContent(state.mine, state.theirs, comparison, Modifier.weight(1f))
                }
                error != null -> OfflineCard(error, onRetry = onRefresh)
                else -> ProfileSkeleton()
            }
        }
    }
}

@Composable
private fun CompareContent(mine: PlayerDto, theirs: PlayerDto, comparison: Comparison, modifier: Modifier) {
    val language = Locale.current.language
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        item { Versus(mine, theirs) }
        item { Verdict(theirs.name, comparison, Modifier.padding(vertical = Spacing.sm)) }
        comparison.groups.forEach { group ->
            item(key = group.section.name) { SectionTitle(group.section) }
            items(group.rows, key = { "${group.section}-${it.label}" }) { CompareRowView(it, language) }
        }
    }
}

@Composable
private fun Versus(mine: PlayerDto, theirs: PlayerDto) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Side(mine, Modifier.weight(1f)) { YouTag() }
        Text(
            "×",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = WarbookTheme.text.wordmark.fontFamily),
            color = MaterialTheme.colorScheme.tertiary,
        )
        Side(theirs, Modifier.weight(1f)) {
            theirs.role?.let {
                Text(
                    stringResource(it.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Side(player: PlayerDto, modifier: Modifier, extra: @Composable () -> Unit) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        TownHallShield(player.townHallLevel, size = ShieldSize.Large)
        Text(
            player.name,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = WarbookTheme.text.wordmark.fontFamily),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        extra()
    }
}

/** "Você está à frente em 5 de 10 · Rafa em 4 · 1 empate". */
@Composable
private fun Verdict(theirName: String, comparison: Comparison, modifier: Modifier) {
    val parts = buildList {
        add(stringResource(Res.string.compare_me_ahead, comparison.mineAhead, comparison.total))
        add(stringResource(Res.string.compare_them_ahead, theirName, comparison.theirsAhead))
        if (comparison.ties > 0) add(pluralStringResource(Res.plurals.compare_ties, comparison.ties, comparison.ties))
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(
            parts.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}

@Composable
private fun SectionTitle(section: CompareSection) {
    val title = when (section) {
        CompareSection.GENERAL -> Res.string.compare_general
        CompareSection.HEROES -> Res.string.me_heroes
        CompareSection.SEASON -> Res.string.me_season
    }
    Text(
        stringResource(title).uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
    )
}

@Composable
private fun CompareRowView(row: CompareRow, language: String) {
    val label = when (val l = row.label) {
        is CompareLabel.Hero -> UnitNames.display(l.apiName, language)
        is CompareLabel.Stat -> stringResource(
            when (l.stat) {
                CompareStat.TOWN_HALL -> Res.string.player_town_hall
                CompareStat.LEVEL -> Res.string.player_level
                CompareStat.TROPHIES -> Res.string.player_trophies
                CompareStat.WAR_STARS -> Res.string.compare_war_stars
                CompareStat.DONATIONS -> Res.string.compare_donations
                CompareStat.ATTACK_WINS -> Res.string.me_attack_wins
            },
        )
    }
    val mine = row.mine?.grouped() ?: "—"
    val theirs = row.theirs?.grouped() ?: "—"
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "$label: $mine × $theirs" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            Value(if (row.result > 0) "▲ $mine" else mine, ahead = row.result > 0)
        }
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(LABEL_WEIGHT),
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Value(if (row.result < 0) "$theirs ▲" else theirs, ahead = row.result < 0)
        }
    }
}

/** Quem está à frente fica em verde com ▲ sobre fundo suave; empate e quem está atrás ficam neutros. */
@Composable
private fun Value(text: String, ahead: Boolean) {
    val colors = WarbookTheme.colors
    Surface(
        color = if (ahead) colors.successContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (ahead) colors.success else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall.tabular(),
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
        )
    }
}
