package io.github.danilodox.warbook.ui.player_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.domain.usecase.GetPlayerProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.github.danilodox.warbook.domain.util.Result

class PlayerDetailsViewModel(
    private val getPlayerProfileUseCase: GetPlayerProfileUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerDetailsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val playerTag = savedStateHandle.get<String>("playerTag")
        if (playerTag != null) {
            loadPlayerData(playerTag)
        } else {
            _uiState.update { it.copy(isLoading = false, error = "Player tag not found.") }
        }
    }

    private fun loadPlayerData(playerTag: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getPlayerProfileUseCase(playerTag)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, player = result.data?.player) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }

                else -> {}
            }
        }
    }
}