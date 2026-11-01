package io.github.danilodox.warbook.domain.repository

import io.github.danilodox.warbook.domain.model.ClanDetails
import io.github.danilodox.warbook.domain.model.Player
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {

    suspend fun getPlayerDetails(playerTag: String): Player?

    suspend fun getClanDetails(clanTag: String): ClanDetails?


    suspend fun saveFavoritePlayer(player: Player)


    suspend fun removeFavoritePlayer(player: Player)

    /**
     * Busca jogadores favoritos no banco de dados local pelo nome.
     * Retorna uma lista pois pode conter nomes iguais para diferentes jogadores
     * uma vez que a chave primaria é a tag.
     */
    suspend fun searchFavoritePlayerByName(name: String): List<Player>

    suspend fun getFavoritePlayerByTag(tag: String): Player?

    fun getAllFavoritePlayers(): Flow<List<Player>>
}