package io.github.danilodox.warbook.di

import org.koin.core.module.Module

/** Engine HTTP e DataStore de cada plataforma. */
expect fun platformModule(): Module
