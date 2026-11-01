package io.github.danilodox.warbook.domain.usecase

import io.github.danilodox.warbook.domain.model.Player
import io.github.danilodox.warbook.domain.repository.PlayerRepository

class ToggleFavoriteStatusUseCase(private val repository: PlayerRepository) {
    suspend operator fun invoke(player: Player): Boolean {
        val isCurrentlyFavorite = repository.getFavoritePlayerByTag(player.tag) != null
        return if (isCurrentlyFavorite) {
            repository.removeFavoritePlayer(player)
            false
        } else {
            repository.saveFavoritePlayer(player)
            true
        }
    }
}