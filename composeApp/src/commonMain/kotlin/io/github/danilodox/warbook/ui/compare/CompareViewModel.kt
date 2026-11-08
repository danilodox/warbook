package io.github.danilodox.warbook.ui.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.CachedProfile
import io.github.danilodox.warbook.data.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompareUiState(
    val loaded: Boolean = false,
    /** Sem conta ativa não há "comigo". */
    val hasAccount: Boolean = true,
    /** A conta ativa. */
    val mine: PlayerDto? = null,
    /** O jogador aberto. */
    val theirs: PlayerDto? = null,
    /** Quando o perfil do outro foi salvo (para a faixa de offline). */
    val theirsFetchedAt: Long = 0L,
    /** Só com os dois perfis (salvos ou da memória). */
    val comparison: Comparison? = null,
    val refreshing: Boolean = false,
    /** Erro da última atualização; com [comparison], a tela mostra a faixa de offline. */
    val error: AppError? = null,
)

private data class Status(val refreshing: Boolean = false, val error: AppError? = null)

/** Comparar (T3): a conta ativa contra o jogador [tag]. Atualiza os dois perfis ao abrir e ao puxar. */
@OptIn(ExperimentalCoroutinesApi::class)
class CompareViewModel(
    private val tag: String,
    accounts: AccountsRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val activeTag: Flow<String?> = accounts.accounts.map { list -> list.firstOrNull { it.isActive }?.tag }
    private val status = MutableStateFlow(Status())

    private val mine: Flow<Pair<Boolean, CachedProfile?>> = activeTag.flatMapLatest { active ->
        if (active == null) flowOf(false to null) else profiles.observe(active).map { true to it }
    }

    val state: StateFlow<CompareUiState> = combine(mine, profiles.observe(tag), status) { me, other, status ->
        val (hasAccount, myProfile) = me
        val myPlayer = myProfile?.player
        val theirPlayer = other?.player
        CompareUiState(
            loaded = true,
            hasAccount = hasAccount,
            mine = myPlayer,
            theirs = theirPlayer,
            theirsFetchedAt = other?.fetchedAt ?: 0L,
            comparison = if (myPlayer != null && theirPlayer != null) compare(myPlayer, theirPlayer) else null,
            refreshing = status.refreshing,
            error = status.error,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CompareUiState())

    init {
        refresh()
    }

    fun refresh() {
        if (status.value.refreshing) return
        status.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            val tags = listOfNotNull(activeTag.first(), tag).distinct()
            val results = tags.map { async { profiles.refresh(it) } }.awaitAll()
            val error = results.filterIsInstance<ApiResult.Failure>().firstOrNull()?.error
            status.update { Status(refreshing = false, error = error) }
        }
    }
}
