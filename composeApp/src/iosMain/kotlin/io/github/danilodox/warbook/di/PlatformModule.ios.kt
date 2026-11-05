package io.github.danilodox.warbook.di

import androidx.room3.Room
import io.github.danilodox.warbook.data.DATA_STORE_FILE
import io.github.danilodox.warbook.data.createDataStore
import io.github.danilodox.warbook.data.local.DATABASE_FILE
import io.github.danilodox.warbook.data.local.WarbookDatabase
import io.github.danilodox.warbook.data.local.buildWarbookDatabase
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
private fun documentsPath(): String {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(documents?.path)
}

actual fun platformModule(): Module = module {
    single<HttpClientEngine> { Darwin.create() }
    single { createDataStore(documentsPath() + "/" + DATA_STORE_FILE) }
    single { buildWarbookDatabase(Room.databaseBuilder<WarbookDatabase>(name = documentsPath() + "/" + DATABASE_FILE)) }
}
