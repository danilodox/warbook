package io.github.danilodox.warbook.ui.clan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.ClanRepository
import io.github.danilodox.warbook.data.ProfileRepository
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.clan_invite_body
import io.github.danilodox.warbook.resources.clan_invite_title
import io.github.danilodox.warbook.resources.clan_no_clan_body
import io.github.danilodox.warbook.resources.clan_no_clan_title
import io.github.danilodox.warbook.resources.clan_search_clans
import io.github.danilodox.warbook.resources.clan_tab_info
import io.github.danilodox.warbook.resources.clan_tab_members
import io.github.danilodox.warbook.resources.me_add_account
import io.github.danilodox.warbook.ui.components.SubTabs
import io.github.danilodox.warbook.ui.me.OfflineCard
import io.github.danilodox.warbook.ui.me.ProfileSkeleton
import io.github.danilodox.warbook.ui.war.Message
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Aba Clã (C1–C2). */
@Composable
fun ClanTabRoute(actions: ClanTabActions, modifier: Modifier = Modifier) {
    val accounts: AccountsRepository = koinInject()
    val profiles: ProfileRepository = koinInject()
    val clans: ClanRepository = koinInject()
    val viewModel = viewModel { ClanTabViewModel(accounts, profiles, clans) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Roda a cada vez que a aba entra na tela (voltar de outra aba também conta como abrir).
    LaunchedEffect(viewModel) { viewModel.onOpen() }

    ClanTabScreen(state, viewModel, actions, modifier)
}

@Composable
private fun ClanTabScreen(
    state: ClanTabUiState,
    viewModel: ClanTabViewModel,
    actions: ClanTabActions,
    modifier: Modifier,
) {
    val profileError = state.profileError
    when {
        !state.loaded -> Unit
        state.accountTag == null -> Message(Res.string.clan_invite_title, Res.string.clan_invite_body, modifier) {
            Button(onClick = actions.onAddAccount) { Text(stringResource(Res.string.me_add_account)) }
        }
        !state.profileKnown && profileError != null -> OfflineCard(profileError, onRetry = viewModel::refresh)
        !state.profileKnown -> ProfileSkeleton()
        state.clanTag == null -> Message(Res.string.clan_no_clan_title, Res.string.clan_no_clan_body, modifier) {
            Button(onClick = actions.onSearchClans) { Text(stringResource(Res.string.clan_search_clans)) }
        }
        else -> Column(modifier.fillMaxSize()) {
            val tabs = listOf(ClanTab.INFO to Res.string.clan_tab_info, ClanTab.MEMBERS to Res.string.clan_tab_members)
            SubTabs(tabs, state.tab, viewModel::selectTab)
            ClanSectionBody(
                state = ClanSectionState(state.clan, state.refreshing, state.error),
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
            ) { clan, contentModifier ->
                if (state.tab == ClanTab.MEMBERS) {
                    ClanMembers(
                        clan = clan,
                        sort = state.sort,
                        accountTag = state.accountTag,
                        onSort = viewModel::sortBy,
                        onOpenMember = actions.onOpenMember,
                        modifier = contentModifier,
                    )
                } else {
                    ClanInfo(clan, showHeader = true, onOpenWar = actions.onOpenWar, modifier = contentModifier)
                }
            }
        }
    }
}
