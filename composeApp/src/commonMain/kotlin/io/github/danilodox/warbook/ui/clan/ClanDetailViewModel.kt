package io.github.danilodox.warbook.ui.clan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.Cached
import io.github.danilodox.warbook.data.ClanRepository
import io.github.danilodox.warbook.data.FollowKind
import io.github.danilodox.warbook.data.FollowRepository
import io.github.danilodox.warbook.data.FollowResult
import io.github.danilodox.warbook.data.RecentSearch
import io.github.danilodox.warbook.ui.me.activeOrFirst
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ClanTab { INFO, MEMBERS, WAR }

data class ClanDetailUiState(
    val loaded: Boolean = false,
    val clan: Cached<ClanDto>? = null,
    val refreshing: Boolean = false,
    /** Erro da última atualização; com [clan], a tela mostra a faixa de offline. */
    val error: AppError? = null,
    val following: Boolean = false,
    val followLimitReached: Boolean = false,
    val tab: ClanTab = ClanTab.INFO,
    val sort: MemberSort = MemberSort.POSITION,
    /** Conta ativa, para marcar "você" na lista. */
    val accountTag: String? = null,
)

private data class Local(
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val limitReached: Boolean = false,
    val tab: ClanTab = ClanTab.INFO,
    val sort: MemberSort = MemberSort.POSITION,
)

/**
 * Tela Clã (T2), de qualquer clã. A sub-aba Guerra tem o próprio [io.github.danilodox.warbook.ui.war.WarViewModel].
 * [fromSearch] = aberta pela busca: quando o clã chega, ele entra nas "Recentes".
 */
class ClanDetailViewModel(
    private val tag: String,
    private val clans: ClanRepository,
    private val follows: FollowRepository,
    accounts: AccountsRepository,
    private val preferences: AppPreferences,
    private val fromSearch: Boolean = false,
) : ViewModel() {
    private val local = MutableStateFlow(Local())

    val state: StateFlow<ClanDetailUiState> = combine(
        clans.observe(tag),
        follows.isFollowed(FollowKind.CLAN, tag),
        accounts.accounts.map { it.activeOrFirst()?.tag },
        local,
    ) { clan, following, accountTag, local ->
        ClanDetailUiState(
            loaded = true,
            clan = clan,
            refreshing = local.refreshing,
            error = local.error,
            following = following,
            followLimitReached = local.limitReached,
            tab = local.tab,
            sort = local.sort,
            accountTag = accountTag,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ClanDetailUiState())

    init {
        refresh()
    }

    fun refresh() {
        if (local.value.refreshing) return
        local.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            val result = clans.refresh(tag)
            if (result is ApiResult.Success && fromSearch) {
                val clan = result.value
                preferences.addRecentSearch(
                    RecentSearch(FollowKind.CLAN, clan.tag, clan.name, clan.clanLevel, clan.badgeUrl),
                )
            }
            local.update { it.copy(refreshing = false, error = (result as? ApiResult.Failure)?.error) }
        }
    }

    fun selectTab(tab: ClanTab) = local.update { it.copy(tab = tab) }

    fun sortBy(sort: MemberSort) = local.update { it.copy(sort = sort) }

    fun toggleFollow() {
        val current = state.value
        val clan = current.clan?.value ?: return
        viewModelScope.launch {
            if (current.following) {
                follows.unfollow(FollowKind.CLAN, tag)
                local.update { it.copy(limitReached = false) }
            } else {
                val limit = follows.followClan(clan) == FollowResult.LIMIT_REACHED
                local.update { it.copy(limitReached = limit) }
            }
        }
    }
}
