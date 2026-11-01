package io.github.danilodox.warbook.domain.usecase

import io.github.danilodox.warbook.domain.model.Player
import io.github.danilodox.warbook.domain.repository.PlayerRepository
import io.github.danilodox.warbook.domain.util.Result

data class PlayerProfileData(
    val player: Player,
    val isFavorite: Boolean
)

class GetPlayerProfileUseCase(private val repository: PlayerRepository) {
    suspend operator fun invoke(playerTag: String): Result<PlayerProfileData> {
        return try {
            val player = repository.getPlayerDetails(playerTag)
            if (player != null) {
                // Também verifica se o jogador já está salvo como favorito
                val isFavorite = repository.getFavoritePlayerByTag(player.tag) != null
                Result.Success(PlayerProfileData(player, isFavorite))
            } else {
                Result.Error("Player not found")
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "An unexpected error occurred")
        }
    }
}