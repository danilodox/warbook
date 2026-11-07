package io.github.danilodox.warbook.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WarCacheDao {
    @Query("SELECT * FROM war_cache WHERE cacheKey = :cacheKey")
    fun observe(cacheKey: String): Flow<WarCacheEntity?>

    @Upsert
    suspend fun upsert(entry: WarCacheEntity)
}
