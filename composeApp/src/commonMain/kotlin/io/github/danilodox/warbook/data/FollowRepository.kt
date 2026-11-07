package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.api.ClanSearchResultDto
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.data.local.FollowedDao
import io.github.danilodox.warbook.data.local.FollowedEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Jogador ou clã seguido, com a cópia que a lista mostra sem rede. [level] = CV do jogador ou nível do clã. */
data class Followed(val kind: FollowKind, val tag: String, val name: String, val level: Int?, val imageUrl: String?)

enum class FollowResult { FOLLOWED, LIMIT_REACHED }

/** "Seguindo" (B1): até [LIMIT] itens no total, jogadores e clãs juntos. Seguir guarda o último dado. */
interface FollowRepository {
    /** O mais recente primeiro. */
    val followed: Flow<List<Followed>>

    fun isFollowed(kind: FollowKind, tag: String): Flow<Boolean>

    suspend fun followPlayer(player: PlayerDto): FollowResult

    suspend fun followClan(clan: ClanDto): FollowResult

    /** Seguir pela lista da busca, sem abrir o clã: o salvo dele vem na primeira vez que abrir. */
    suspend fun followClan(result: ClanSearchResultDto): FollowResult

    /** Deixa de seguir e apaga o salvo, se ele não for de uma conta. */
    suspend fun unfollow(kind: FollowKind, tag: String)

    companion object {
        const val LIMIT = 50
    }
}

class RoomFollowRepository(
    private val dao: FollowedDao,
    private val profiles: ProfileRepository,
    private val clans: ClanRepository,
    private val wars: WarRepository,
    private val clock: () -> Long = ::currentTimeMillis,
) : FollowRepository {

    override val followed: Flow<List<Followed>> = dao.observeAll().map { list -> list.mapNotNull { it.toFollowed() } }

    override fun isFollowed(kind: FollowKind, tag: String): Flow<Boolean> =
        dao.observeCount(kind.name, tag).map { it > 0 }.distinctUntilChanged()

    override suspend fun followPlayer(player: PlayerDto) =
        follow(FollowKind.PLAYER, player.tag, player.name, player.townHallLevel, null) { profiles.save(player) }

    override suspend fun followClan(clan: ClanDto) =
        follow(FollowKind.CLAN, clan.tag, clan.name, clan.clanLevel, clan.badgeUrl) { clans.save(clan) }

    override suspend fun followClan(result: ClanSearchResultDto) =
        follow(FollowKind.CLAN, result.tag, result.name, result.clanLevel, result.badgeUrl) {}

    override suspend fun unfollow(kind: FollowKind, tag: String) {
        dao.delete(kind.name, tag)
        when (kind) {
            FollowKind.PLAYER -> profiles.forget(tag)
            FollowKind.CLAN -> {
                clans.forget(tag)
                wars.forget(tag)
            }
        }
    }

    @Suppress("LongParameterList") // Os campos da cópia + o que guardar.
    private suspend fun follow(
        kind: FollowKind,
        tag: String,
        name: String,
        level: Int?,
        imageUrl: String?,
        keep: suspend () -> Unit,
    ): FollowResult {
        val already = dao.count(kind.name, tag) > 0
        if (!already && dao.countAll() >= FollowRepository.LIMIT) return FollowResult.LIMIT_REACHED
        dao.upsert(FollowedEntity(kind.name, tag, name, level, imageUrl, clock()))
        keep()
        return FollowResult.FOLLOWED
    }
}

/** Tipo que não existe mais (de outra versão) some da lista. */
private fun FollowedEntity.toFollowed(): Followed? =
    FollowKind.entries.firstOrNull { it.name == kind }?.let { Followed(it, tag, name, level, imageUrl) }
