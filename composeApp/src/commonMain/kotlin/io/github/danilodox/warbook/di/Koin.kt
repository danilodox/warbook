package io.github.danilodox.warbook.di

import io.github.danilodox.warbook.data.AccountsRepository
import io.github.danilodox.warbook.data.AppPreferences
import io.github.danilodox.warbook.data.CacheScope
import io.github.danilodox.warbook.data.ClanRepository
import io.github.danilodox.warbook.data.DataStoreAppPreferences
import io.github.danilodox.warbook.data.DataStoreInstallTokenStore
import io.github.danilodox.warbook.data.FollowRepository
import io.github.danilodox.warbook.data.InstallTokenStore
import io.github.danilodox.warbook.data.KtorWarbookRepository
import io.github.danilodox.warbook.data.ProfileRepository
import io.github.danilodox.warbook.data.RoomAccountsRepository
import io.github.danilodox.warbook.data.RoomCacheScope
import io.github.danilodox.warbook.data.RoomClanRepository
import io.github.danilodox.warbook.data.RoomFollowRepository
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
    single { get<WarbookDatabase>().warCache() }
    single { get<WarbookDatabase>().clanCache() }
    single { get<WarbookDatabase>().followed() }
    single<CacheScope> { RoomCacheScope(accounts = get(), followed = get()) }
    single<ProfileRepository> {
        RoomProfileRepository(cache = get(), accounts = get(), api = get(), scope = get(), followed = get())
    }
    single<AccountsRepository> { RoomAccountsRepository(dao = get(), profiles = get(), api = get()) }
    single<WarRepository> { RoomWarRepository(cache = get(), api = get(), scope = get()) }
    single<ClanRepository> { RoomClanRepository(cache = get(), api = get(), scope = get(), followed = get()) }
    single<FollowRepository> { RoomFollowRepository(dao = get(), profiles = get(), clans = get(), wars = get()) }
}

/** Inicia o Koin. Chamado uma vez por plataforma (Application no Android, MainViewController no iOS). */
fun initKoin(appInfo: AppInfo, config: KoinAppDeclaration = {}): KoinApplication = startKoin {
    config()
    modules(appModule(appInfo), platformModule())
}
