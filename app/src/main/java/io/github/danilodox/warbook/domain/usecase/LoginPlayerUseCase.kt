package io.github.danilodox.warbook.domain.usecase

import io.github.danilodox.warbook.data.datastore.SecureTokenProvider
import io.github.danilodox.warbook.domain.model.Player
import io.github.danilodox.warbook.domain.repository.PlayerRepository
import io.github.danilodox.warbook.domain.util.Result // (Veja a classe Result no final)

/**
 * UseCase responsável pela lógica de negócio do login.
 */
class LoginPlayerUseCase(
    private val repository: PlayerRepository, // 1. Depende da interface do repositório
    private val secureTokenProvider: SecureTokenProvider // 2. E do provider para salvar a tag
) {
    /**
     * Usar 'operator fun invoke' permite chamar a classe como se fosse uma função.
     * Ex: loginPlayerUseCase("#PLAYER_TAG")
     */
    suspend operator fun invoke(playerTag: String): Result<Player> {
        return try {
            val player = repository.getPlayerDetails(playerTag)

            if (player != null) {
                secureTokenProvider.saveTag(playerTag)
                Result.Success(player)
            } else {
                Result.Error("Player not found")
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "An unexpected error occurred")
        }
    }
}