package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.data.local.WarCacheDao
import io.github.danilodox.warbook.data.local.WarCacheEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException

/** Último resultado salvo e quando ele veio do servidor (ms desde a época). */
data class Cached<T>(val value: T, val fetchedAt: Long)

/**
 * Guerra atual e CWL de um clã, com o último resultado para abrir sem rede. Tags já normalizadas.
 * Vai para o banco o clã de uma conta ou seguido ([CacheScope]), ou quando [keep] força; o resto fica na memória.
 */
interface WarRepository {
    fun observeWar(clanTag: String): Flow<Cached<CurrentWarDto>?>

    fun observeCwl(clanTag: String): Flow<Cached<CwlDto>?>

    /** Busca no servidor; no sucesso guarda. No erro, o guardado não muda. */
    suspend fun refreshWar(clanTag: String, keep: Boolean = false): ApiResult<CurrentWarDto>

    suspend fun refreshCwl(clanTag: String, keep: Boolean = false): ApiResult<CwlDto>

    /** Apaga a guerra e a CWL salvas, se o clã não for mais de uma conta nem seguido. */
    suspend fun forget(clanTag: String)
}

class RoomWarRepository(
    private val cache: WarCacheDao,
    private val api: WarbookRepository,
    private val scope: CacheScope = KeepEverything,
    private val clock: () -> Long = ::currentTimeMillis,
) : WarRepository {
    private val warMemory = MemoryCache<CurrentWarDto>()
    private val cwlMemory = MemoryCache<CwlDto>()

    override fun observeWar(clanTag: String) =
        warMemory.observeWith(clanTag, observe(warKey(clanTag), CurrentWarDto.serializer()))

    override fun observeCwl(clanTag: String) =
        cwlMemory.observeWith(clanTag, observe(cwlKey(clanTag), CwlDto.serializer()))

    override suspend fun refreshWar(clanTag: String, keep: Boolean) = api.currentWar(clanTag)
        .alsoKeep(clanTag, keep, warKey(clanTag), CurrentWarDto.serializer(), warMemory)

    override suspend fun refreshCwl(clanTag: String, keep: Boolean) = api.currentCwl(clanTag)
        .alsoKeep(clanTag, keep, cwlKey(clanTag), CwlDto.serializer(), cwlMemory)

    override suspend fun forget(clanTag: String) {
        if (scope.keepsClan(clanTag)) return
        cache.delete(warKey(clanTag))
        cache.delete(cwlKey(clanTag))
    }

    @Suppress("LongParameterList") // Guerra e CWL só mudam a chave, o formato e a memória.
    private suspend fun <T> ApiResult<T>.alsoKeep(
        clanTag: String,
        keep: Boolean,
        key: String,
        serializer: KSerializer<T>,
        memory: MemoryCache<T>,
    ): ApiResult<T> {
        if (this !is ApiResult.Success) return this
        val now = clock()
        if (keep || scope.keepsClan(clanTag)) {
            cache.upsert(WarCacheEntity(key, warbookJson.encodeToString(serializer, value), now))
        } else {
            memory.put(clanTag, Cached(value, now))
        }
        return this
    }

    /** Formato salvo que não serve mais (versão antiga do app) vira `null`. */
    private fun <T> observe(key: String, serializer: KSerializer<T>): Flow<Cached<T>?> =
        cache.observe(key).map { entry -> entry?.decode(serializer) }

    private fun <T> WarCacheEntity.decode(serializer: KSerializer<T>): Cached<T>? = try {
        Cached(warbookJson.decodeFromString(serializer, json), fetchedAt)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private companion object {
        fun warKey(clanTag: String) = "war:$clanTag"

        fun cwlKey(clanTag: String) = "cwl:$clanTag"
    }
}
