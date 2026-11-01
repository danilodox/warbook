package io.github.danilodox.warbook.domain.usecase

import io.github.danilodox.warbook.domain.model.ClanDetails
import io.github.danilodox.warbook.domain.repository.PlayerRepository // Supondo que o método do clã esteja aqui
import io.github.danilodox.warbook.domain.util.Result

class GetClanDetailsUseCase(
    private val repository: PlayerRepository
) {
    suspend operator fun invoke(clanTag: String): Result<ClanDetails> {
        return try {
            val clanDetails = repository.getClanDetails(clanTag)
            if (clanDetails != null) {
                Result.Success(clanDetails)
            } else {
                Result.Error("Clan not found")
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "An unexpected error occurred")
        }
    }
}