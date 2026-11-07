package io.github.danilodox.warbook.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowedDao {
    /** O mais recente primeiro. */
    @Query("SELECT * FROM followed ORDER BY followedAt DESC")
    fun observeAll(): Flow<List<FollowedEntity>>

    @Query("SELECT COUNT(*) FROM followed WHERE kind = :kind AND tag = :tag")
    fun observeCount(kind: String, tag: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM followed WHERE kind = :kind AND tag = :tag")
    suspend fun count(kind: String, tag: String): Int

    @Query("SELECT COUNT(*) FROM followed")
    suspend fun countAll(): Int

    @Upsert
    suspend fun upsert(entry: FollowedEntity)

    /** Atualiza a cópia da lista; não faz nada se não estiver seguindo. */
    @Query("UPDATE followed SET name = :name, level = :level, imageUrl = :imageUrl WHERE kind = :kind AND tag = :tag")
    suspend fun updateSummary(kind: String, tag: String, name: String, level: Int?, imageUrl: String?)

    @Query("DELETE FROM followed WHERE kind = :kind AND tag = :tag")
    suspend fun delete(kind: String, tag: String)
}
