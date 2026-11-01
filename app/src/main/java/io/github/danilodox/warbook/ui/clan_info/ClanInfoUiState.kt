package io.github.danilodox.warbook.ui.clan_info

import io.github.danilodox.warbook.domain.model.ClanDetails

data class ClanInfoUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val clanDetails: ClanDetails? = null
)