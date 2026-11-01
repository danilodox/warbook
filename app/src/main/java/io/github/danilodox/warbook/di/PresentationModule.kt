package io.github.danilodox.warbook.di

import io.github.danilodox.warbook.ui.clan_info.ClanInfoViewModel
import io.github.danilodox.warbook.ui.favorite_player.FavoritePlayersViewModel
import io.github.danilodox.warbook.ui.player_login.PlayerLoginViewModel
import io.github.danilodox.warbook.ui.home.HomeViewModel
import io.github.danilodox.warbook.ui.player_details.PlayerDetailsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel { HomeViewModel(get(), get()) }
    viewModel { PlayerLoginViewModel(get()) }
    viewModel { FavoritePlayersViewModel(get()) }
    viewModel { ClanInfoViewModel(get()) }
    viewModel { PlayerDetailsViewModel(get(), get()) }
}