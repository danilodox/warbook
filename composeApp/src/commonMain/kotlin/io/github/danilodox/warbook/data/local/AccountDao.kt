package io.github.danilodox.warbook.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

// Uma função por consulta do Room; agrupar esconderia o SQL de cada uma.
@Suppress("TooManyFunctions")
@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY addedAt")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE tag = :tag")
    suspend fun get(tag: String): AccountEntity?

    @Query("SELECT tag FROM accounts ORDER BY addedAt LIMIT 1")
    suspend fun firstTag(): String?

    @Upsert
    suspend fun upsert(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE tag = :tag")
    suspend fun delete(tag: String)

    /** Só uma conta fica ativa. */
    @Query("UPDATE accounts SET isActive = (tag = :tag)")
    suspend fun setActive(tag: String)

    @Query("UPDATE accounts SET verificationToken = :token WHERE tag = :tag")
    suspend fun setVerificationToken(tag: String, token: String)

    @Query(
        "UPDATE accounts SET name = :name, townHallLevel = :townHallLevel, clanName = :clanName, " +
            "clanTag = :clanTag WHERE tag = :tag",
    )
    suspend fun updateSummary(tag: String, name: String, townHallLevel: Int, clanName: String?, clanTag: String?)

    @Query("SELECT COUNT(*) FROM accounts WHERE clanTag = :clanTag")
    suspend fun countWithClan(clanTag: String): Int

    /** Adiciona (ou atualiza) e deixa ativa. Conta que já existia mantém a verificação e a data. */
    @Transaction
    suspend fun addAndActivate(account: AccountEntity) {
        val existing = get(account.tag)
        upsert(
            account.copy(
                verificationToken = account.verificationToken ?: existing?.verificationToken,
                addedAt = existing?.addedAt ?: account.addedAt,
            ),
        )
        setActive(account.tag)
    }

    /** Remove; se era a ativa, a mais antiga que sobrou passa a ser a ativa. */
    @Transaction
    suspend fun removeAndPickNext(tag: String) {
        val wasActive = get(tag)?.isActive == true
        delete(tag)
        if (wasActive) firstTag()?.let { setActive(it) }
    }
}
