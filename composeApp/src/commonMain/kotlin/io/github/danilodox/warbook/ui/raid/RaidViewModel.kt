package io.github.danilodox.warbook.ui.raid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.api.CapitalRaidsDto
import io.github.danilodox.warbook.api.RaidSeasonDto
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.WarbookRepository
import io.github.danilodox.warbook.data.currentTimeMillis
import io.github.danilodox.warbook.ui.me.activeOrFirst
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RaidUiState(
    /** Último resultado do servidor; `null` até a primeira resposta. */
    val raids: CapitalRaidsDto? = null,
    val fetchedAt: Long = 0,
    val refreshing: Boolean = false,
    /** Erro da última busca; com [raids], a tela mostra a faixa de offline. */
    val error: AppError? = null,
    /** Índice da temporada escolhida em [CapitalRaidsDto.seasons] (0 = a mais recente). */
    val selected: Int = 0,
    /** Conta ativa, para marcar "você" na lista. */
    val accountTag: String? = null,
) {
    val season: RaidSeasonDto? get() = raids?.seasons?.getOrNull(selected)
}

private data class RaidLocal(
    val raids: CapitalRaidsDto? = null,
    val fetchedAt: Long = 0,
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val selected: Int = 0,
)

/**
 * Sub-aba Capital (Clã › Capital) de [clanTag]: raids da capital, só em memória enquanto a tela existe (o
 * servidor também não salva). Busca ao abrir a sub-aba ([onOpen]) e ao puxar.
 */
class RaidViewModel(
    private val clanTag: String,
    accounts: AccountsRepository,
    private val api: WarbookRepository,
    private val clock: () -> Long = ::currentTimeMillis,
) : ViewModel() {
    private val local = MutableStateFlow(RaidLocal())

    val state: StateFlow<RaidUiState> = combine(
        local,
        accounts.accounts.map { it.activeOrFirst()?.tag }.distinctUntilChanged(),
    ) { local, accountTag ->
        RaidUiState(
            raids = local.raids,
            fetchedAt = local.fetchedAt,
            refreshing = local.refreshing,
            error = local.error,
            selected = local.selected,
            accountTag = accountTag,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RaidUiState())

    /** A sub-aba ficou visível. */
    fun onOpen() = refresh()

    fun refresh() {
        if (local.value.refreshing) return
        local.update { it.copy(refreshing = true, error = null) }
        viewModelScope.launch {
            when (val result = api.capitalRaids(clanTag)) {
                is ApiResult.Success -> local.update {
                    // A lista pode ter mudado de tamanho: volta para a mais recente se a escolhida sumiu.
                    val selected = it.selected.takeIf { index -> index in result.value.seasons.indices } ?: 0
                    it.copy(raids = result.value, fetchedAt = clock(), refreshing = false, selected = selected)
                }
                is ApiResult.Failure -> local.update { it.copy(refreshing = false, error = result.error) }
            }
        }
    }

    fun selectSeason(index: Int) = local.update {
        if (index in it.raids?.seasons.orEmpty().indices) it.copy(selected = index) else it
    }
}
