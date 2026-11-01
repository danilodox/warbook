package io.github.danilodox.warbook.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.data.datastore.SecureTokenProvider
import io.github.danilodox.warbook.domain.util.Result
import io.github.danilodox.warbook.domain.usecase.GetPlayerProfileUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getPlayerProfileUseCase: GetPlayerProfileUseCase,
    private val secureTokenProvider: SecureTokenProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        loadInitialPlayerData()
    }

    private fun loadInitialPlayerData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val playerTag = secureTokenProvider.getSavedTag()

            if (playerTag.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, error = "No player tag found.") }
                return@launch
            }

            when (val result = getPlayerProfileUseCase(playerTag)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            player = result.data?.player,
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }

                else -> {}
            }
        }
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.OnPlayerProfileClicked -> {
                viewModelScope.launch {
                    _navigationEvent.send(NavigationEvent.ToPlayerDetails(event.playerTag))
                }
            }

            is HomeUiEvent.OnViewClanClicked -> {
                viewModelScope.launch {
                    //_navigationEvent.send(NavigationEvent.ToClanDetails())
                }
            }
            HomeUiEvent.OnViewClanClicked -> { /* TODO: Lógica de navegação para a tela do clã */ }
            HomeUiEvent.OnWarInfoClicked -> { /* TODO: Lógica de navegação para a tela de war info */ }
            HomeUiEvent.OnSearchPlayerClicked -> { /* TODO: Lógica de navegação para a tela de busca */ }
            else -> {}
        }
    }
}

sealed class NavigationEvent {
    data class ToPlayerDetails(val playerTag: String) : NavigationEvent()
    data class ToClanDetails(val playerTag: String) : NavigationEvent()
}