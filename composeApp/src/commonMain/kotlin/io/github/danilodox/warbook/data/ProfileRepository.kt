package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.data.local.AccountDao
import io.github.danilodox.warbook.data.local.PlayerCacheDao
import io.github.danilodox.warbook.data.local.PlayerCacheEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException

/** Último perfil salvo e quando ele veio do servidor (ms desde a época). */
data class CachedProfile(val player: PlayerDto, val fetchedAt: Long)

interface ProfileRepository {
    /** Perfil salvo de [tag]; `null` se nunca foi salvo ou se o formato salvo não serve mais. */
    fun observe(tag: String): Flow<CachedProfile?>

    /** Busca no servidor; no sucesso salva o perfil e atualiza o cabeçalho da conta. No erro, nada muda. */
    suspend fun refresh(tag: String): ApiResult<PlayerDto>

    suspend fun save(player: PlayerDto)

    suspend fun forget(tag: String)
}

class RoomProfileRepository(
    private val cache: PlayerCacheDao,
    private val accounts: AccountDao,
    private val api: WarbookRepository,
    private val clock: () -> Long = ::currentTimeMillis,
) : ProfileRepository {

    override fun observe(tag: String): Flow<CachedProfile?> = cache.observe(tag).map { it?.decode() }

    override suspend fun refresh(tag: String): ApiResult<PlayerDto> {
        val result = api.player(tag)
        if (result is ApiResult.Success) {
            val player = result.value
            save(player)
            accounts.updateSummary(player.tag, player.name, player.townHallLevel, player.clan?.name)
        }
        return result
    }

    override suspend fun save(player: PlayerDto) {
        cache.upsert(PlayerCacheEntity(player.tag, warbookJson.encodeToString(PlayerDto.serializer(), player), clock()))
    }

    override suspend fun forget(tag: String) = cache.delete(tag)

    private fun PlayerCacheEntity.decode(): CachedProfile? = try {
        CachedProfile(warbookJson.decodeFromString(PlayerDto.serializer(), json), fetchedAt)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
