package io.github.danilodox.warbook.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Decide a primeira tela uma vez, ao abrir o app. `null` enquanto lê o banco e as preferências. */
class StartViewModel(accounts: AccountsRepository, preferences: AppPreferences) : ViewModel() {
    private val _start = MutableStateFlow<Route?>(null)
    val start: StateFlow<Route?> = _start.asStateFlow()

    init {
        viewModelScope.launch {
            val hasAccounts = accounts.accounts.first().isNotEmpty()
            _start.value = startRoute(hasAccounts, preferences.exploreMode.first())
        }
    }
}
