package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.data.local.AccountDao
import io.github.danilodox.warbook.data.local.FollowedDao
import io.github.danilodox.warbook.data.local.PlayerCacheDao
import io.github.danilodox.warbook.data.local.PlayerCacheEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException

/** Último perfil salvo e quando ele veio do servidor (ms desde a época). */
data class CachedProfile(val player: PlayerDto, val fetchedAt: Long)

interface ProfileRepository {
    /** Último perfil de [tag] (salvo ou da memória); `null` se nunca veio ou se o formato salvo não serve mais. */
    fun observe(tag: String): Flow<CachedProfile?>

    /**
     * Busca no servidor. No sucesso, conta ou jogador seguido vai para o banco (e atualiza o cabeçalho da conta
     * e a lista "Seguindo"); os outros ficam só na memória. No erro, nada muda.
     */
    suspend fun refresh(tag: String): ApiResult<PlayerDto>

    /** Salva no banco (conta nova ou jogador que passou a ser seguido). */
    suspend fun save(player: PlayerDto)

    /** Apaga o salvo, se ele não for mais de uma conta nem de um seguido. */
    suspend fun forget(tag: String)
}

class RoomProfileRepository(
    private val cache: PlayerCacheDao,
    private val accounts: AccountDao,
    private val api: WarbookRepository,
    private val scope: CacheScope = AccountsOnlyCacheScope(accounts),
    private val followed: FollowedDao? = null,
    private val clock: () -> Long = ::currentTimeMillis,
) : ProfileRepository {
    private val memory = MemoryCache<PlayerDto>()

    override fun observe(tag: String): Flow<CachedProfile?> =
        memory.observeWith(tag, cache.observe(tag).map { it?.decode() })
            .map { it?.let { cached -> CachedProfile(cached.value, cached.fetchedAt) } }

    override suspend fun refresh(tag: String): ApiResult<PlayerDto> {
        val result = api.player(tag)
        if (result is ApiResult.Success) {
            val player = result.value
            if (scope.keepsPlayer(player.tag)) {
                save(player)
                val clan = player.clan
                accounts.updateSummary(player.tag, player.name, player.townHallLevel, clan?.name, clan?.tag)
                followed?.updateSummary(FollowKind.PLAYER.name, player.tag, player.name, player.townHallLevel, null)
            } else {
                memory.put(player.tag, Cached(player, clock()))
            }
        }
        return result
    }

    override suspend fun save(player: PlayerDto) {
        cache.upsert(PlayerCacheEntity(player.tag, warbookJson.encodeToString(PlayerDto.serializer(), player), clock()))
    }

    override suspend fun forget(tag: String) {
        if (!scope.keepsPlayer(tag)) cache.delete(tag)
    }

    private fun PlayerCacheEntity.decode(): Cached<PlayerDto>? = try {
        Cached(warbookJson.decodeFromString(PlayerDto.serializer(), json), fetchedAt)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
