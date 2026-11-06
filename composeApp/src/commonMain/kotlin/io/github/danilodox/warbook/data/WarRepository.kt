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

/** Guerra atual e CWL de um clã, com o último resultado salvo para abrir sem rede. Tags já normalizadas. */
interface WarRepository {
    fun observeWar(clanTag: String): Flow<Cached<CurrentWarDto>?>

    fun observeCwl(clanTag: String): Flow<Cached<CwlDto>?>

    /** Busca no servidor; no sucesso salva. No erro, o salvo não muda. */
    suspend fun refreshWar(clanTag: String): ApiResult<CurrentWarDto>

    suspend fun refreshCwl(clanTag: String): ApiResult<CwlDto>
}

class RoomWarRepository(
    private val cache: WarCacheDao,
    private val api: WarbookRepository,
    private val clock: () -> Long = ::currentTimeMillis,
) : WarRepository {

    override fun observeWar(clanTag: String) = observe(warKey(clanTag), CurrentWarDto.serializer())

    override fun observeCwl(clanTag: String) = observe(cwlKey(clanTag), CwlDto.serializer())

    override suspend fun refreshWar(clanTag: String) =
        api.currentWar(clanTag).alsoSave(warKey(clanTag), CurrentWarDto.serializer())

    override suspend fun refreshCwl(clanTag: String) =
        api.currentCwl(clanTag).alsoSave(cwlKey(clanTag), CwlDto.serializer())

    private suspend fun <T> ApiResult<T>.alsoSave(key: String, serializer: KSerializer<T>): ApiResult<T> {
        if (this is ApiResult.Success) {
            cache.upsert(WarCacheEntity(key, warbookJson.encodeToString(serializer, value), clock()))
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
