package io.github.danilodox.warbook.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.WarbookRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingState(
    val lookingUp: Boolean = false,
    val error: AppError? = null,
    /** Jogador achado na O1, mostrado na O2. */
    val player: PlayerDto? = null,
    val saving: Boolean = false,
)

/** O que a O1 faz depois de uma ação. */
sealed interface TagEvent {
    data object PlayerFound : TagEvent

    data object Explore : TagEvent
}

/** O1 (tag) e O2 ("É você?"). A O3 tem o próprio ViewModel, porque também abre pelas Configurações. */
class OnboardingViewModel(
    private val api: WarbookRepository,
    private val accounts: AccountsRepository,
    private val preferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    // Um canal por tela, para um evento nunca ser consumido pela tela errada durante a animação.
    private val tagEvents = Channel<TagEvent>(Channel.BUFFERED)
    val tagScreenEvents: Flow<TagEvent> = tagEvents.receiveAsFlow()

    private val added = Channel<String>(Channel.BUFFERED)

    /** Tag da conta recém-adicionada, para abrir a O3. */
    val accountAdded: Flow<String> = added.receiveAsFlow()

    fun lookUp(rawTag: String) {
        if (_state.value.lookingUp) return
        _state.update { it.copy(lookingUp = true, error = null) }
        viewModelScope.launch {
            when (val result = api.player(rawTag)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(lookingUp = false, player = result.value) }
                    tagEvents.send(TagEvent.PlayerFound)
                }
                is ApiResult.Failure -> _state.update { it.copy(lookingUp = false, error = result.error) }
            }
        }
    }

    /** O usuário mexeu na tag: o erro da busca anterior não vale mais. */
    fun clearError() = _state.update { it.copy(error = null) }

    fun confirm() {
        val player = _state.value.player ?: return
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            accounts.add(player)
            _state.update { it.copy(saving = false) }
            added.send(player.tag)
        }
    }

    fun explore() {
        viewModelScope.launch {
            preferences.setExploreMode(true)
            tagEvents.send(TagEvent.Explore)
        }
    }
}
