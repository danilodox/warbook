package io.github.danilodox.warbook.di

import io.github.danilodox.warbook.domain.usecase.LoginPlayerUseCase
import io.github.danilodox.warbook.domain.usecase.CalculateTownHallProgressUseCase
import io.github.danilodox.warbook.domain.usecase.GetClanDetailsUseCase
import io.github.danilodox.warbook.domain.usecase.GetPlayerProfileUseCase
import io.github.danilodox.warbook.domain.usecase.ToggleFavoriteStatusUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { LoginPlayerUseCase(get(), get()) }
    factory { GetPlayerProfileUseCase(get()) }
    factory { ToggleFavoriteStatusUseCase(get()) }
    factory { CalculateTownHallProgressUseCase() }
    factory { GetClanDetailsUseCase(get()) }
}