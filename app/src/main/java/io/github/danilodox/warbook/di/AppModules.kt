package io.github.danilodox.warbook.di

import org.koin.core.module.Module

val appModules: List<Module> = listOf( dataModule, uiModule, domainModule )