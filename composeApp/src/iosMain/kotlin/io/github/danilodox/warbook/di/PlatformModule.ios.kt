package io.github.danilodox.warbook.di

import io.github.danilodox.warbook.data.DATA_STORE_FILE
import io.github.danilodox.warbook.data.createDataStore
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun platformModule(): Module = module {
    single<HttpClientEngine> { Darwin.create() }
    single {
        val documents = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        createDataStore(requireNotNull(documents?.path) + "/" + DATA_STORE_FILE)
    }
}
