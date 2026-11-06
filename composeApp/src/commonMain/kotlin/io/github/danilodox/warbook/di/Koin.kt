package io.github.danilodox.warbook.di

import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.DataStoreAppPreferences
import io.github.danilodox.warbook.data.DataStoreInstallTokenStore
import io.github.danilodox.warbook.data.InstallTokenStore
import io.github.danilodox.warbook.data.KtorWarbookRepository
import io.github.danilodox.warbook.data.ProfileRepository
import io.github.danilodox.warbook.data.RoomAccountsRepository
import io.github.danilodox.warbook.data.RoomProfileRepository
import io.github.danilodox.warbook.data.RoomWarRepository
import io.github.danilodox.warbook.data.WarRepository
import io.github.danilodox.warbook.data.WarbookHttpClients
import io.github.danilodox.warbook.data.WarbookRepository
import io.github.danilodox.warbook.data.local.WarbookDatabase
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun appModule(appInfo: AppInfo): Module = module {
    single { appInfo }
    single<InstallTokenStore> { DataStoreInstallTokenStore(get()) }
    single<AppPreferences> { DataStoreAppPreferences(get()) }
    single { WarbookHttpClients(engine = get(), baseUrl = appInfo.serverUrl, store = get()) }
    single<WarbookRepository> { KtorWarbookRepository(get<WarbookHttpClients>().api) }
    single { get<WarbookDatabase>().accounts() }
    single { get<WarbookDatabase>().playerCache() }
    single<ProfileRepository> { RoomProfileRepository(cache = get(), accounts = get(), api = get()) }
    single<AccountsRepository> { RoomAccountsRepository(dao = get(), profiles = get(), api = get()) }
    single { get<WarbookDatabase>().warCache() }
    single<WarRepository> { RoomWarRepository(cache = get(), api = get()) }
}

/** Inicia o Koin. Chamado uma vez por plataforma (Application no Android, MainViewController no iOS). */
fun initKoin(appInfo: AppInfo, config: KoinAppDeclaration = {}): KoinApplication = startKoin {
    config()
    modules(appModule(appInfo), platformModule())
}
