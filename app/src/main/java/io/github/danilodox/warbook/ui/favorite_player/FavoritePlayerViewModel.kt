package io.github.danilodox.warbook.ui.favorite_player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.domain.model.Player
import io.github.danilodox.warbook.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FavoritePlayersViewModel(
    private val localRepository: PlayerRepository
) : ViewModel() {

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            localRepository.getAllFavoritePlayers()
                .collect { favoritePlayers ->
                    _players.value = favoritePlayers
                }
        }
    }
}