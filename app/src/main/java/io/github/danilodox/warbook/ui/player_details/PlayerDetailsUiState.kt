package io.github.danilodox.warbook.ui.player_details

import io.github.danilodox.warbook.domain.model.Player

data class PlayerDetailsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val player: Player? = null
)