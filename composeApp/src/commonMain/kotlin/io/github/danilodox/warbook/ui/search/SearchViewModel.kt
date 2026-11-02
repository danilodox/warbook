package io.github.danilodox.warbook.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.WarbookRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchState {
    data object Idle : SearchState

    data object Loading : SearchState

    data class Found(val player: PlayerDto) : SearchState

    data class Failed(val error: AppError) : SearchState
}

/** Busca de jogador por tag (tela mínima da v0.2.0; a busca completa é a B1/B2 da v0.4.0). */
class SearchViewModel(private val repository: WarbookRepository) : ViewModel() {
    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun search(rawTag: String) {
        searchJob?.cancel()
        _state.value = SearchState.Loading
        searchJob = viewModelScope.launch {
            _state.value = when (val result = repository.player(rawTag)) {
                is ApiResult.Success -> SearchState.Found(result.value)
                is ApiResult.Failure -> SearchState.Failed(result.error)
            }
        }
    }
}
