package io.github.danilodox.warbook.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ClanCacheDao {
    @Query("SELECT * FROM clan_cache WHERE tag = :tag")
    fun observe(tag: String): Flow<ClanCacheEntity?>

    @Upsert
    suspend fun upsert(entry: ClanCacheEntity)

    @Query("DELETE FROM clan_cache WHERE tag = :tag")
    suspend fun delete(tag: String)
}
