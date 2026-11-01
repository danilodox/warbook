package io.github.danilodox.warbook.di

import androidx.room.Room
import io.github.danilodox.warbook.data.datastore.SecureTokenProvider
import io.github.danilodox.warbook.data.local.AppDatabase
import io.github.danilodox.warbook.data.local.DatabaseKeyProvider
import io.github.danilodox.warbook.domain.repository.PlayerRepository
import io.github.danilodox.warbook.data.repository.PlayerRepositoryImpl
import io.github.danilodox.warbook.data.remote.RetrofitService
import io.github.danilodox.warbook.data.remote.interceptor.AuthInterceptor
import net.sqlcipher.database.SupportFactory
import org.koin.dsl.module

val dataModule = module {

    single { SecureTokenProvider(get()) }

    single { DatabaseKeyProvider(get()) }

    single {
        val passphrase = get<DatabaseKeyProvider>().getKey()
        val factory = SupportFactory(passphrase)

        Room.databaseBuilder(
            get(),
            AppDatabase::class.java,
            "warbook-db")
            .openHelperFactory(factory)
            .build()
    }

    single { get<AppDatabase>().playerDao() }
    single { get<AppDatabase>().clanDao() }

    single { AuthInterceptor(get()) }

    single { RetrofitService(get()) }

    single { get<RetrofitService>().service }

    single <PlayerRepository> {
        PlayerRepositoryImpl (
            apiService = get(),
            playerDao = get(),
            clanDao = get()
        )
    }



}