package io.github.danilodox.warbook.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.CachedProfile
import io.github.danilodox.warbook.data.FollowKind
import io.github.danilodox.warbook.data.FollowRepository
import io.github.danilodox.warbook.data.FollowResult
import io.github.danilodox.warbook.data.ProfileRepository
import io.github.danilodox.warbook.data.RecentSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val loaded: Boolean = false,
    /** Último perfil (salvo ou da memória). */
    val profile: CachedProfile? = null,
    val refreshing: Boolean = false,
    /** Erro da última atualização; com [profile], a tela mostra a faixa de offline. */
    val error: AppError? = null,
    val following: Boolean = false,
    /** Tentou seguir com o limite cheio; some no próximo toque. */
    val followLimitReached: Boolean = false,
)

private data class Status(
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val limitReached: Boolean = false,
)

/**
 * Tela Jogador (T1), aberta de qualquer aba. Busca ao abrir e ao puxar. [fromSearch] = aberta pela busca:
 * quando o perfil chega, ele entra nas "Recentes" com nome e CV.
 */
class PlayerViewModel(
    private val tag: String,
    private val profiles: ProfileRepository,
    private val follows: FollowRepository,
    private val preferences: AppPreferences,
    private val fromSearch: Boolean = false,
) : ViewModel() {
    private val status = MutableStateFlow(Status())

    val state: StateFlow<PlayerUiState> = combine(
        profiles.observe(tag),
        follows.isFollowed(FollowKind.PLAYER, tag),
        status,
    ) { profile, following, status ->
        PlayerUiState(
            loaded = true,
            profile = profile,
            refreshing = status.refreshing,
            error = status.error,
            following = following,
            followLimitReached = status.limitReached,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerUiState())

    init {
        refresh()
    }

    fun refresh() {
        if (status.value.refreshing) return
        status.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            val result = profiles.refresh(tag)
            if (result is ApiResult.Success && fromSearch) {
                val player = result.value
                preferences.addRecentSearch(
                    RecentSearch(FollowKind.PLAYER, player.tag, player.name, player.townHallLevel),
                )
            }
            status.update { it.copy(refreshing = false, error = (result as? ApiResult.Failure)?.error) }
        }
    }

    /** Seguir ou deixar de seguir. Sem perfil carregado não há o que guardar. */
    fun toggleFollow() {
        val current = state.value
        val player = current.profile?.player ?: return
        viewModelScope.launch {
            if (current.following) {
                follows.unfollow(FollowKind.PLAYER, tag)
                status.update { it.copy(limitReached = false) }
            } else {
                val limit = follows.followPlayer(player) == FollowResult.LIMIT_REACHED
                status.update { it.copy(limitReached = limit) }
            }
        }
    }
}
