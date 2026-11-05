package io.github.danilodox.warbook.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [AccountEntity::class, PlayerCacheEntity::class], version = 1)
@ConstructedBy(WarbookDatabaseConstructor::class)
abstract class WarbookDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao

    abstract fun playerCache(): PlayerCacheDao
}

/** Implementado pelo Room (KSP) em cada plataforma. */
@Suppress("KotlinNoActualForExpect")
expect object WarbookDatabaseConstructor : RoomDatabaseConstructor<WarbookDatabase> {
    override fun initialize(): WarbookDatabase
}

const val DATABASE_FILE = "warbook.db"

/** [builder] vem de cada plataforma (caminho do arquivo); o resto da configuração é igual em todas. */
fun buildWarbookDatabase(builder: RoomDatabase.Builder<WarbookDatabase>): WarbookDatabase = builder
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
