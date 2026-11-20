package io.github.danilodox.warbook.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.api.ClanActivityDto
import io.github.danilodox.warbook.api.CwlSeasonDto
import io.github.danilodox.warbook.api.PlayerHistoryDto
import io.github.danilodox.warbook.api.WarDetailDto
import io.github.danilodox.warbook.api.WarHistoryDto
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.WarbookRepository
import io.github.danilodox.warbook.data.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Algo carregado do servidor: [value] fica com o último que chegou, mesmo se a atualização falhar.
 * [fetchedAt]: quando [value] chegou (epoch ms), para a faixa de "atualizado há".
 */
data class LoadState<T>(
    val loading: Boolean = true,
    val value: T? = null,
    val error: AppError? = null,
    val fetchedAt: Long = 0,
)

/** Carrega ao abrir e ao puxar para atualizar. O histórico não fica salvo no aparelho: é sempre do servidor. */
abstract class LoadViewModel<T>(
    private val load: suspend () -> ApiResult<T>,
    private val now: () -> Long = ::currentTimeMillis,
) : ViewModel() {
    private val mutable = MutableStateFlow(LoadState<T>())
    val state: StateFlow<LoadState<T>> = mutable.asStateFlow()

    fun refresh() {
        mutable.update { it.copy(loading = true) }
        viewModelScope.launch {
            val result = load()
            mutable.update {
                when (result) {
                    is ApiResult.Success -> LoadState(loading = false, value = result.value, fetchedAt = now())
                    is ApiResult.Failure -> it.copy(loading = false, error = result.error)
                }
            }
        }
    }
}

/** Lista de guerras guardadas do clã. */
class WarHistoryViewModel(clanTag: String, api: WarbookRepository) :
    LoadViewModel<WarHistoryDto>({ api.warHistory(clanTag) }) {
    init {
        refresh()
    }
}

/** Desempenho do jogador [playerTag] nas guerras guardadas do clã. */
class PlayerPerformanceViewModel(clanTag: String, playerTag: String, api: WarbookRepository) :
    LoadViewModel<PlayerHistoryDto>({ api.playerHistory(clanTag, playerTag) }) {
    init {
        refresh()
    }
}

/** Atividade dos membros do clã. */
class ClanActivityViewModel(clanTag: String, api: WarbookRepository) :
    LoadViewModel<ClanActivityDto>({ api.clanActivity(clanTag) }) {
    init {
        refresh()
    }
}

/** Uma guerra guardada com a nota de cada membro. */
class WarRecordViewModel(clanTag: String, warId: Long, api: WarbookRepository) :
    LoadViewModel<WarDetailDto>({ api.warDetail(clanTag, warId) }) {
    init {
        refresh()
    }
}

/** Temporada mais recente da CWL guardada, para o cartão do histórico. */
class CwlSeasonViewModel(clanTag: String, api: WarbookRepository) :
    LoadViewModel<CwlSeasonDto>({ api.cwlSeason(clanTag) }) {
    init {
        refresh()
    }
}
