package io.github.danilodox.warbook.index

import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.api.PlayerSearchResultDto

/**
 * Índice de nicks: cresce só com o que passa pelo Warbook (jogadores consultados e membros de clãs abertos),
 * sem varredura. Só dados públicos.
 */
interface PlayerIndex {
    /** Grava (ou atualiza) em segundo plano: não atrasa a resposta e um erro do banco só vira log. */
    fun record(players: List<IndexedPlayer>)

    /** [name] já validado por `PlayerSearch.normalize`. Até `PlayerSearch.MAX_RESULTS`, os mais parecidos antes. */
    suspend fun search(name: String): List<PlayerSearchResultDto>
}

/** O que o índice guarda de cada jogador. Tags sem `#`. */
data class IndexedPlayer(
    val tag: String,
    val name: String,
    val townHallLevel: Int,
    val clanTag: String?,
    val clanName: String?,
)

fun PlayerDto.toIndexed() = IndexedPlayer(tag, name, townHallLevel, clan?.tag, clan?.name)

fun ClanDto.membersToIndexed(): List<IndexedPlayer> =
    members.map { IndexedPlayer(it.tag, it.name, it.townHallLevel, clanTag = tag, clanName = name) }
