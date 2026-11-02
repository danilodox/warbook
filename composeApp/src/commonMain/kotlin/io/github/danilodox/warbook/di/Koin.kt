package io.github.danilodox.warbook.di

import io.github.danilodox.warbook.data.DataStoreInstallTokenStore
import io.github.danilodox.warbook.data.InstallTokenStore
import io.github.danilodox.warbook.data.KtorWarbookRepository
import io.github.danilodox.warbook.data.WarbookHttpClients
import io.github.danilodox.warbook.data.WarbookRepository
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun appModule(appInfo: AppInfo): Module = module {
    single { appInfo }
    single<InstallTokenStore> { DataStoreInstallTokenStore(get()) }
    single { WarbookHttpClients(engine = get(), baseUrl = appInfo.serverUrl, store = get()) }
    single<WarbookRepository> { KtorWarbookRepository(get<WarbookHttpClients>().api) }
}

/** Inicia o Koin. Chamado uma vez por plataforma (Application no Android, MainViewController no iOS). */
fun initKoin(appInfo: AppInfo, config: KoinAppDeclaration = {}): KoinApplication = startKoin {
    config()
    modules(appModule(appInfo), platformModule())
}
