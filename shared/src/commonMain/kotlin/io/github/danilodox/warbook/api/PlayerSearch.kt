package io.github.danilodox.warbook.api

import kotlinx.serialization.Serializable

/**
 * Resposta de `GET /v1/players/search?name=`. Só acha quem já apareceu no Warbook (jogadores consultados e
 * membros de clãs abertos por alguém); não há varredura.
 */
@Serializable
data class PlayerSearchResponse(val items: List<PlayerSearchResultDto>)

/** Jogador achado pelo nick, com o nick original e os dados de quando foi visto pela última vez. */
@Serializable
data class PlayerSearchResultDto(
    val tag: String,
    val name: String,
    val townHallLevel: Int,
    val clanTag: String? = null,
    val clanName: String? = null,
    /** Quando o Warbook viu esse jogador pela última vez (epoch ms). */
    val seenAt: Long,
)

/** Regras da busca de jogador por nick, iguais no app e no servidor. */
object PlayerSearch {
    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 50
    const val MAX_RESULTS = 20

    /** Nick aparado, ou `null` se for curto ou longo demais. */
    fun normalize(raw: String): String? = raw.trim().takeIf { it.length in MIN_LENGTH..MAX_LENGTH }
}
