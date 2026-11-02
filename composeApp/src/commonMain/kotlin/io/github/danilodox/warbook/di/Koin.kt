package io.github.danilodox.warbook.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Dados do app que cada plataforma informa ao iniciar. */
data class AppInfo(
    val versionName: String,
)

fun appModule(appInfo: AppInfo): Module = module {
    single { appInfo }
}

/** Inicia o Koin. Chamado uma vez por plataforma (Application no Android, MainViewController no iOS). */
fun initKoin(appInfo: AppInfo, config: KoinAppDeclaration = {}): KoinApplication = startKoin {
    config()
    modules(appModule(appInfo))
}
