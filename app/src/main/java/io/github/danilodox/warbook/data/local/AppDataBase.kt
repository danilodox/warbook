package io.github.danilodox.warbook.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.danilodox.warbook.data.local.dao.ClanDao
import io.github.danilodox.warbook.data.local.dao.PlayerDao
import io.github.danilodox.warbook.data.local.model.ClanEntity
import io.github.danilodox.warbook.data.local.model.PlayerEntity

@Database(
    entities = [PlayerEntity::class, ClanEntity::class],
    version = 2,
    exportSchema = false)

abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao

    abstract fun clanDao(): ClanDao
}