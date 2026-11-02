package io.github.danilodox.warbook.di

import android.content.Context
import io.github.danilodox.warbook.data.DATA_STORE_FILE
import io.github.danilodox.warbook.data.createDataStore
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<HttpClientEngine> { OkHttp.create() }
    single { createDataStore(get<Context>().filesDir.resolve(DATA_STORE_FILE).absolutePath) }
}
