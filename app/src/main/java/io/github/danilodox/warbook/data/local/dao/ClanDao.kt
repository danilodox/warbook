package io.github.danilodox.warbook.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import io.github.danilodox.warbook.data.local.model.ClanEntity

@Dao
interface ClanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClan(clan: ClanEntity)
}