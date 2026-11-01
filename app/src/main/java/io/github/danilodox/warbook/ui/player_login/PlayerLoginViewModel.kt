package io.github.danilodox.warbook.ui.player_login

import io.github.danilodox.warbook.domain.usecase.LoginPlayerUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.github.danilodox.warbook.domain.util.Result

class PlayerLoginViewModel(
    private val loginPlayerUseCase: LoginPlayerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerLoginUiState())
    val uiState: StateFlow<PlayerLoginUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<PlayerLoginUiEvent>()
    val uiEvent: SharedFlow<PlayerLoginUiEvent> = _uiEvent.asSharedFlow()

    fun loginWithTag(playerTag: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }

            when (val result = loginPlayerUseCase(playerTag)){
                is Result.Success -> {
                    _uiEvent.emit(PlayerLoginUiEvent.NavigateToHome(playerTag))
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isError = true) }
                    _uiEvent.emit(PlayerLoginUiEvent.ShowToast(result.message ?: "An error occurred"))
                }

                else -> {}
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

}