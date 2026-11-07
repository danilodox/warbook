package io.github.danilodox.warbook.ui.clan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.Cached
import io.github.danilodox.warbook.data.ClanRepository
import io.github.danilodox.warbook.data.FollowRepository
import io.github.danilodox.warbook.data.ProfileRepository
import io.github.danilodox.warbook.data.WarRepository
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.clan_tab_info
import io.github.danilodox.warbook.resources.clan_tab_members
import io.github.danilodox.warbook.resources.clan_tab_war
import io.github.danilodox.warbook.ui.common.FollowButton
import io.github.danilodox.warbook.ui.me.OfflineCard
import io.github.danilodox.warbook.ui.me.ProfileSkeleton
import io.github.danilodox.warbook.ui.me.StaleBanner
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.war.WarActions
import io.github.danilodox.warbook.ui.war.WarScreen
import io.github.danilodox.warbook.ui.war.WarViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Tela Clã (T2). [onOpenMember] abre a tela Jogador. */
@Composable
fun ClanDetailRoute(tag: String, fromSearch: Boolean, onOpenMember: (String) -> Unit, modifier: Modifier = Modifier) {
    val clans: ClanRepository = koinInject()
    val follows: FollowRepository = koinInject()
    val accounts: AccountsRepository = koinInject()
    val preferences: AppPreferences = koinInject()
    val viewModel = viewModel(key = "clan-$tag") {
        ClanDetailViewModel(tag, clans, follows, accounts, preferences, fromSearch)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        val clan = state.clan?.value
        if (clan != null) {
            ClanHeader(clan, Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small)) {
                FollowButton(
                    state.following,
                    state.followLimitReached,
                    enabled = true,
                    onToggle = viewModel::toggleFollow,
                )
            }
        }
        ClanTabs(state.tab, viewModel::selectTab)
        if (state.tab == ClanTab.WAR) {
            ClanWarTab(tag, Modifier.weight(1f))
        } else {
            ClanSectionBody(
                state = ClanSectionState(state.clan, state.refreshing, state.error),
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
            ) { cached, contentModifier ->
                when (state.tab) {
                    ClanTab.MEMBERS -> ClanMembers(
                        clan = cached,
                        sort = state.sort,
                        accountTag = state.accountTag,
                        onSort = viewModel::sortBy,
                        onOpenMember = onOpenMember,
                        modifier = contentModifier,
                    )
                    else -> ClanInfo(
                        clan = cached,
                        showHeader = false,
                        onOpenWar = { viewModel.selectTab(ClanTab.WAR) },
                        modifier = contentModifier,
                    )
                }
            }
        }
    }
}

/** A Guerra de qualquer clã, com os mesmos componentes da aba Guerra. */
@Composable
private fun ClanWarTab(tag: String, modifier: Modifier) {
    val accounts: AccountsRepository = koinInject()
    val profiles: ProfileRepository = koinInject()
    val wars: WarRepository = koinInject()
    val preferences: AppPreferences = koinInject()
    val warViewModel = viewModel(key = "clan-war-$tag") {
        WarViewModel(accounts, profiles, wars, preferences, fixedClanTag = tag)
    }
    val warState by warViewModel.state.collectAsStateWithLifecycle()
    // Busca a guerra só quando a sub-aba abre, não junto com o clã.
    LaunchedEffect(warViewModel) { warViewModel.onOpen() }
    WarScreen(
        state = warState,
        actions = WarActions(
            onSelectTab = warViewModel::selectTab,
            onSelectRound = warViewModel::selectRound,
            onRefresh = warViewModel::refresh,
            onAddAccount = {},
            onToggleSummaryPart = warViewModel::toggleSummaryPart,
        ),
        modifier = modifier,
    )
}

@Composable
private fun ClanTabs(selected: ClanTab, onSelect: (ClanTab) -> Unit) {
    val tabs = listOf(
        ClanTab.INFO to Res.string.clan_tab_info,
        ClanTab.MEMBERS to Res.string.clan_tab_members,
        ClanTab.WAR to Res.string.clan_tab_war,
    )
    SegmentedTabs(tabs, selected, onSelect)
}

/** Sub-abas em botões segmentados (Info | Membros, Info | Membros | Guerra). */
@Composable
internal fun <T> SegmentedTabs(tabs: List<Pair<T, StringResource>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.medium, vertical = Spacing.small),
    ) {
        tabs.forEachIndexed { index, (tab, label) ->
            SegmentedButton(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
            ) { Text(stringResource(label)) }
        }
    }
}

/** O salvo do clã e a atualização dele, para o corpo da aba Clã e da tela Clã. */
data class ClanSectionState(val cached: Cached<ClanDto>?, val refreshing: Boolean, val error: AppError?)

/** O salvo (com a faixa se a atualização falhou), o cartão de erro sem nada salvo ou o skeleton. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClanSectionBody(
    state: ClanSectionState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (ClanDto, Modifier) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.refreshing && state.cached != null,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        val cached = state.cached
        val error = state.error
        when {
            cached != null -> Column(Modifier.fillMaxSize()) {
                if (error != null) StaleBanner(error, cached.fetchedAt)
                content(cached.value, Modifier.weight(1f))
            }
            error != null -> OfflineCard(error, onRetry = onRefresh)
            else -> ProfileSkeleton()
        }
    }
}
