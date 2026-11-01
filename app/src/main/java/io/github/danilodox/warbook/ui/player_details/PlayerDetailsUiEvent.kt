package io.github.danilodox.warbook.ui.player_details

sealed class PlayerDetailsUiEvent {
    data object OnBackClicked : PlayerDetailsUiEvent()
}