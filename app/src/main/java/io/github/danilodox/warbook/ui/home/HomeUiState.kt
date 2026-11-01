package io.github.danilodox.warbook.ui.home

import io.github.danilodox.warbook.domain.model.Player


data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val player: Player? = null,
)
