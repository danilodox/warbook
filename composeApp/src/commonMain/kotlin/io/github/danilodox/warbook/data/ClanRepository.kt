package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.api.ClanSearchResultDto
import io.github.danilodox.warbook.data.local.ClanCacheDao
import io.github.danilodox.warbook.data.local.ClanCacheEntity
import io.github.danilodox.warbook.data.local.FollowedDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException

/**
 * Clã (Info e Membros) com o último resultado para abrir sem rede. Tags já normalizadas.
 * Vai para o banco o clã de uma conta ou seguido ([CacheScope]), ou quando `keep` força; o resto fica na memória.
 */
interface ClanRepository {
    fun observe(tag: String): Flow<Cached<ClanDto>?>

    /** Busca no servidor; no sucesso guarda (e atualiza a lista "Seguindo"). No erro, nada muda. */
    suspend fun refresh(tag: String, keep: Boolean = false): ApiResult<ClanDto>

    /** Busca por nome (B2); não guarda nada. */
    suspend fun search(rawName: String): ApiResult<List<ClanSearchResultDto>>

    /** Salva no banco (clã que passou a ser seguido). */
    suspend fun save(clan: ClanDto)

    /** Apaga o salvo, se o clã não for mais de uma conta nem seguido. */
    suspend fun forget(tag: String)
}

class RoomClanRepository(
    private val cache: ClanCacheDao,
    private val api: WarbookRepository,
    private val scope: CacheScope,
    private val followed: FollowedDao? = null,
    private val clock: () -> Long = ::currentTimeMillis,
) : ClanRepository {
    private val memory = MemoryCache<ClanDto>()

    override fun observe(tag: String): Flow<Cached<ClanDto>?> =
        memory.observeWith(tag, cache.observe(tag).map { it?.decode() })

    override suspend fun refresh(tag: String, keep: Boolean): ApiResult<ClanDto> {
        val result = api.clan(tag)
        if (result is ApiResult.Success) {
            val clan = result.value
            if (keep || scope.keepsClan(clan.tag)) {
                save(clan)
                followed?.updateSummary(FollowKind.CLAN.name, clan.tag, clan.name, clan.clanLevel, clan.badgeUrl)
            } else {
                memory.put(clan.tag, Cached(clan, clock()))
            }
        }
        return result
    }

    override suspend fun search(rawName: String) = api.searchClans(rawName)

    override suspend fun save(clan: ClanDto) {
        cache.upsert(ClanCacheEntity(clan.tag, warbookJson.encodeToString(ClanDto.serializer(), clan), clock()))
    }

    override suspend fun forget(tag: String) {
        if (!scope.keepsClan(tag)) cache.delete(tag)
    }

    private fun ClanCacheEntity.decode(): Cached<ClanDto>? = try {
        Cached(warbookJson.decodeFromString(ClanDto.serializer(), json), fetchedAt)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
