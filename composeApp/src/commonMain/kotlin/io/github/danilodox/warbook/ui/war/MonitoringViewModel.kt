package io.github.danilodox.warbook.ui.war

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.api.MonitoringDto
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.MonitoringRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [error] só aparece se veio de um toque em Ativar/Reativar; falha ao carregar o estado esconde o cartão. */
data class MonitoringUiState(
    val status: MonitoringDto? = null,
    val canActivate: Boolean = false,
    val activating: Boolean = false,
    val error: AppError? = null,
)

/** Cartão do monitoramento (G5) na aba Guerra, para o clã da conta ativa. */
class MonitoringViewModel(private val clanTag: String, private val repository: MonitoringRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(MonitoringUiState())
    val state: StateFlow<MonitoringUiState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val canActivate = repository.canActivate(clanTag)
            val status = (repository.status(clanTag) as? ApiResult.Success)?.value
            mutableState.update {
                it.copy(status = status ?: it.status, canActivate = canActivate)
            }
        }
    }

    fun activate() {
        if (mutableState.value.activating) return
        mutableState.update { it.copy(activating = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.activate(clanTag)) {
                is ApiResult.Success -> mutableState.update { it.copy(status = result.value, activating = false) }
                is ApiResult.Failure -> mutableState.update { it.copy(activating = false, error = result.error) }
            }
        }
    }
}
