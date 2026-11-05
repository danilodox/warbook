package io.github.danilodox.warbook.di

import android.content.Context
import androidx.room3.Room
import io.github.danilodox.warbook.data.DATA_STORE_FILE
import io.github.danilodox.warbook.data.createDataStore
import io.github.danilodox.warbook.data.local.DATABASE_FILE
import io.github.danilodox.warbook.data.local.WarbookDatabase
import io.github.danilodox.warbook.data.local.buildWarbookDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<HttpClientEngine> { OkHttp.create() }
    single { createDataStore(get<Context>().filesDir.resolve(DATA_STORE_FILE).absolutePath) }
    single {
        val context = get<Context>()
        buildWarbookDatabase(
            Room.databaseBuilder<WarbookDatabase>(context, context.getDatabasePath(DATABASE_FILE).absolutePath),
        )
    }
}
