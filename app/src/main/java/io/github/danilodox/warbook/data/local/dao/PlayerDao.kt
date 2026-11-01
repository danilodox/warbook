package io.github.danilodox.warbook.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.danilodox.warbook.data.local.model.PlayerEntity
import io.github.danilodox.warbook.data.local.model.PlayerWithClan
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity)

    @Delete
    suspend fun deletePlayer(player: PlayerEntity)

    /**
     * Busca um jogador e seu clã associado usando a relação.
     */
    @Transaction
    @Query("SELECT * FROM players WHERE tag = :tag")
    suspend fun getPlayerWithClanByTag(tag: String): PlayerWithClan?


    @Transaction
    @Query("SELECT * FROM players")
    fun getAllPlayersWithClans(): Flow<List<PlayerWithClan>>

    @Transaction
    @Query("SELECT * FROM players WHERE name LIKE '%' || :query || '%'")
    suspend fun searchPlayersWithClansByName(query: String): List<PlayerWithClan>
}