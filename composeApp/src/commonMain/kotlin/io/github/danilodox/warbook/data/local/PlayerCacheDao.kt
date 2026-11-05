package io.github.danilodox.warbook.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerCacheDao {
    @Query("SELECT * FROM player_cache WHERE tag = :tag")
    fun observe(tag: String): Flow<PlayerCacheEntity?>

    @Upsert
    suspend fun upsert(entry: PlayerCacheEntity)

    @Query("DELETE FROM player_cache WHERE tag = :tag")
    suspend fun delete(tag: String)
}
