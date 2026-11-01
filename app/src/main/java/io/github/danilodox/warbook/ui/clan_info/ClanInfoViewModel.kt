package io.github.danilodox.warbook.ui.clan_info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.domain.usecase.GetClanDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.github.danilodox.warbook.domain.util.Result
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow


class ClanInfoViewModel(
     private val getClanDetailsUseCase: GetClanDetailsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClanInfoUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    fun loadClanDetails(clanTag: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = getClanDetailsUseCase(clanTag)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, clanDetails = result.data)
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

    fun onEvent(event: ClanInfoUiEvent) {
        when (event) {
            is ClanInfoUiEvent.OnMemberClicked -> {
                viewModelScope.launch {
                    _navigationEvent.send(NavigationEvent.ToMemberDetails(event.playerTag))
                }
            }
            // O OnBackClicked será tratado na UI com o NavController
            else -> {}
        }
    }
}

sealed class NavigationEvent {
    data class ToMemberDetails(val playerTag: String) : NavigationEvent()
}