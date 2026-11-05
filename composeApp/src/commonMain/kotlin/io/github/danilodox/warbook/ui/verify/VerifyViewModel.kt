package io.github.danilodox.warbook.ui.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface VerifyState {
    data object Idle : VerifyState

    data object Verifying : VerifyState

    data object Verified : VerifyState

    data class Failed(val error: AppError) : VerifyState
}

/** O3: verifica a conta [tag] com o token do jogo. O token só passa por aqui; não fica no estado. */
class VerifyViewModel(private val tag: String, private val accounts: AccountsRepository) : ViewModel() {
    private val _state = MutableStateFlow<VerifyState>(VerifyState.Idle)
    val state: StateFlow<VerifyState> = _state.asStateFlow()

    fun verify(gameToken: String) {
        if (_state.value == VerifyState.Verifying || _state.value == VerifyState.Verified) return
        _state.value = VerifyState.Verifying
        viewModelScope.launch {
            _state.value = when (val result = accounts.verify(tag, gameToken)) {
                is ApiResult.Success -> VerifyState.Verified
                is ApiResult.Failure -> VerifyState.Failed(result.error)
            }
        }
    }

    /** O usuário mexeu no token: o erro anterior não vale mais. */
    fun clearError() {
        if (_state.value is VerifyState.Failed) _state.value = VerifyState.Idle
    }
}
