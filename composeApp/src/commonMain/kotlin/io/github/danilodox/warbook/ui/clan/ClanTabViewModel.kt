package io.github.danilodox.warbook.ui.clan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.Cached
import io.github.danilodox.warbook.data.CachedProfile
import io.github.danilodox.warbook.data.ClanRepository
import io.github.danilodox.warbook.data.ProfileRepository
import io.github.danilodox.warbook.ui.me.activeOrFirst
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClanTabUiState(
    /** `false` só até a primeira leitura do banco. */
    val loaded: Boolean = false,
    /** Conta ativa; `null` = sem conta ou "só explorar" (a tela convida a adicionar). */
    val accountTag: String? = null,
    /** `false` enquanto o perfil da conta nunca foi salvo: ainda não se sabe o clã. */
    val profileKnown: Boolean = false,
    /** Erro ao buscar o perfil que ainda não existia. */
    val profileError: AppError? = null,
    /** Clã da conta; `null` com [profileKnown] = a conta não está em clã (S2). */
    val clanTag: String? = null,
    val clan: Cached<ClanDto>? = null,
    val refreshing: Boolean = false,
    /** Erro da última atualização do clã; com [clan], a tela mostra a faixa de offline. */
    val error: AppError? = null,
    val tab: ClanTab = ClanTab.INFO,
    val sort: MemberSort = MemberSort.POSITION,
)

/** O que a aba Clã abre: onboarding, a busca de clãs, a aba Guerra e a tela Jogador de um membro. */
data class ClanTabActions(
    val onAddAccount: () -> Unit,
    val onSearchClans: () -> Unit,
    val onOpenWar: () -> Unit,
    val onOpenMember: (String) -> Unit,
)

/** Perfil salvo de [tag] (que pode nem existir ainda). */
private data class TabLookup(val tag: String?, val cached: CachedProfile?)

private data class TabLocal(
    /** Conta (perfil) ou clã cuja atualização roda ou falhou: trocar de conta não mostra o erro da outra. */
    val key: String? = null,
    val refreshing: Boolean = false,
    val profileError: AppError? = null,
    val error: AppError? = null,
    val tab: ClanTab = ClanTab.INFO,
    val sort: MemberSort = MemberSort.POSITION,
)

/**
 * Aba Clã (C1–C2): Info e Membros do clã da conta ativa, sempre com o último resultado salvo (o clã de
 * uma conta vai para o banco). Só busca no servidor quando a aba abre ([onOpen]) e ao puxar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClanTabViewModel(
    accounts: AccountsRepository,
    private val profiles: ProfileRepository,
    private val clans: ClanRepository,
) : ViewModel() {
    private val local = MutableStateFlow(TabLocal())

    private val activeTag: Flow<String?> = accounts.accounts.map { it.activeOrFirst()?.tag }.distinctUntilChanged()

    private val profile: Flow<TabLookup> = activeTag.flatMapLatest { tag ->
        if (tag == null) flowOf(TabLookup(null, null)) else profiles.observe(tag).map { TabLookup(tag, it) }
    }

    private val clanTag: Flow<String?> = profile
        .map { lookup -> lookup.cached?.takeIf { it.player.tag == lookup.tag }?.player?.clan?.tag }
        .distinctUntilChanged()

    private val clan = clanTag.flatMapLatest { tag -> if (tag == null) flowOf(null) else clans.observe(tag) }

    val state: StateFlow<ClanTabUiState> = combine(profile, clanTag, clan, local) { lookup, clanTag, clan, local ->
        // Na troca de conta, o perfil antigo ainda pode chegar um instante; não mostra.
        val known = lookup.cached?.player?.tag.let { it != null && it == lookup.tag }
        val profileLoad = local.takeIf { it.key == lookup.tag && lookup.tag != null }
        val clanLoad = local.takeIf { it.key == clanTag && clanTag != null }
        ClanTabUiState(
            loaded = true,
            accountTag = lookup.tag,
            profileKnown = known,
            profileError = profileLoad?.profileError,
            clanTag = clanTag,
            clan = clan?.takeIf { it.value.tag == clanTag },
            refreshing = (profileLoad ?: clanLoad)?.refreshing == true,
            error = clanLoad?.error,
            tab = local.tab,
            sort = local.sort,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ClanTabUiState())

    /** A aba ficou visível. */
    fun onOpen() = refresh()

    /** Puxar para atualizar. Sem perfil salvo, busca o perfil antes para saber o clã. */
    fun refresh() {
        if (local.value.refreshing) return
        viewModelScope.launch {
            val accountTag = activeTag.first() ?: return@launch
            val clanTag = profiles.observe(accountTag).first()?.player?.let { it.clan?.tag ?: return@launch }
                ?: refreshProfile(accountTag)
                ?: return@launch
            local.update { it.copy(key = clanTag, refreshing = true, error = null) }
            // Clã da conta ativa: sempre salvo, mesmo antes de a conta saber a tag do clã.
            val error = (clans.refresh(clanTag, keep = true) as? ApiResult.Failure)?.error
            local.update { if (it.key == clanTag) it.copy(refreshing = false, error = error) else it }
        }
    }

    /** Busca o perfil que ainda não existe; devolve a tag do clã (`null` = sem clã ou falhou). */
    private suspend fun refreshProfile(accountTag: String): String? {
        local.update { it.copy(key = accountTag, refreshing = true, profileError = null) }
        val result = profiles.refresh(accountTag)
        local.update {
            if (it.key == accountTag) {
                it.copy(refreshing = false, profileError = (result as? ApiResult.Failure)?.error)
            } else {
                it
            }
        }
        return (result as? ApiResult.Success)?.value?.clan?.tag
    }

    fun selectTab(tab: ClanTab) = local.update { it.copy(tab = tab) }

    fun sortBy(sort: MemberSort) = local.update { it.copy(sort = sort) }
}
