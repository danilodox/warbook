package io.github.danilodox.warbook.ui.home


sealed class HomeUiEvent {
    data object OnViewClanClicked : HomeUiEvent()
    data object OnWarInfoClicked : HomeUiEvent()
    data object OnSearchPlayerClicked : HomeUiEvent()
    data object OnBackClicked : HomeUiEvent()
    data class OnPlayerProfileClicked(val playerTag: String) : HomeUiEvent()
}