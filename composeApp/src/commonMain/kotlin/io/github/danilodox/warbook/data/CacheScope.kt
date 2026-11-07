package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.data.local.AccountDao
import io.github.danilodox.warbook.data.local.FollowedDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * O que fica salvo no aparelho para abrir sem rede: contas, clãs das contas e o que é seguido. Jogadores e
 * clãs abertos de passagem ficam só na memória ([MemoryCache]), para o banco não crescer sem limite.
 */
interface CacheScope {
    suspend fun keepsPlayer(tag: String): Boolean

    suspend fun keepsClan(tag: String): Boolean
}

enum class FollowKind { PLAYER, CLAN }

class RoomCacheScope(private val accounts: AccountDao, private val followed: FollowedDao) : CacheScope {
    override suspend fun keepsPlayer(tag: String) =
        accounts.get(tag) != null || followed.count(FollowKind.PLAYER.name, tag) > 0

    override suspend fun keepsClan(tag: String) =
        accounts.countWithClan(tag) > 0 || followed.count(FollowKind.CLAN.name, tag) > 0
}

/** Só as contas (como era até a v0.4): usado nos testes antigos e onde não há "Seguindo". */
class AccountsOnlyCacheScope(private val accounts: AccountDao) : CacheScope {
    override suspend fun keepsPlayer(tag: String) = accounts.get(tag) != null

    override suspend fun keepsClan(tag: String) = accounts.countWithClan(tag) > 0
}

/** Guarda tudo (o comportamento da aba Guerra até a v0.4). */
object KeepEverything : CacheScope {
    override suspend fun keepsPlayer(tag: String) = true

    override suspend fun keepsClan(tag: String) = true
}

/**
 * Resultados que não vão para o banco, enquanto o app está aberto. Guarda os [capacity] mais recentes.
 * [observeWith] junta com o salvo e mostra o mais novo dos dois.
 */
class MemoryCache<T>(private val capacity: Int = DEFAULT_CAPACITY) {
    private val entries = MutableStateFlow<Map<String, Cached<T>>>(emptyMap())

    fun put(key: String, value: Cached<T>) = entries.update { current ->
        val updated = current + (key to value)
        if (updated.size <= capacity) {
            updated
        } else {
            updated.entries.sortedByDescending { it.value.fetchedAt }.take(capacity).associate { it.toPair() }
        }
    }

    fun remove(key: String) = entries.update { it - key }

    fun observeWith(key: String, stored: Flow<Cached<T>?>): Flow<Cached<T>?> =
        combine(stored, entries.map { it[key] }) { saved, memory ->
            when {
                saved == null -> memory
                memory == null -> saved
                memory.fetchedAt > saved.fetchedAt -> memory
                else -> saved
            }
        }

    private companion object {
        const val DEFAULT_CAPACITY = 30
    }
}
