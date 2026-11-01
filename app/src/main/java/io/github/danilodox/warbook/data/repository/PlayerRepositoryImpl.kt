package io.github.danilodox.warbook.data.repository

import io.github.danilodox.warbook.data.local.dao.ClanDao
import io.github.danilodox.warbook.data.local.dao.PlayerDao
import io.github.danilodox.warbook.data.local.model.toDomain
import io.github.danilodox.warbook.data.local.model.toEntity
import io.github.danilodox.warbook.data.remote.model.toDomain
import io.github.danilodox.warbook.domain.model.ClanDetails
import io.github.danilodox.warbook.domain.model.Player
import io.github.danilodox.warbook.domain.repository.PlayerRepository
import io.github.danilodox.warbook.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

class PlayerRepositoryImpl(
    private val apiService: ApiService,
    private val playerDao: PlayerDao,
    private val clanDao: ClanDao
) : PlayerRepository {

    override suspend fun getPlayerDetails(playerTag: String): Player? {
        return try {
            val playerDto = apiService.getPlayerDetails(playerTag)
            playerDto.toDomain()
        } catch (e: IOException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getClanDetails(clanTag: String): ClanDetails? {
        return try {
            val clanDto = apiService.getClanDetails(clanTag)
            clanDto.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveFavoritePlayer(player: Player) {
        player.clan?.let {
            clanDao.insertClan(it.toEntity())
        }
        playerDao.insertPlayer(player.toEntity())
    }

    override suspend fun removeFavoritePlayer(player: Player) {
        playerDao.deletePlayer(player.toEntity())
    }

    override suspend fun searchFavoritePlayerByName(name: String): List<Player> {
        return playerDao.searchPlayersWithClansByName(name).map { it.toDomain() }
    }

    override suspend fun getFavoritePlayerByTag(tag: String): Player? {
        val playerWithClan = playerDao.getPlayerWithClanByTag(tag)
        return playerWithClan?.toDomain()
    }

    override fun getAllFavoritePlayers(): Flow<List<Player>> {
        return playerDao.getAllPlayersWithClans().map { list ->
            list.map { it.toDomain() }
        }
    }
}