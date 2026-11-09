package io.github.danilodox.warbook.testing

import io.github.danilodox.warbook.api.PlayerSearchResultDto
import io.github.danilodox.warbook.index.IndexedPlayer
import io.github.danilodox.warbook.index.PlayerIndex
import java.util.Collections

/** Índice de nicks em memória para os testes de rota: guarda o que foi gravado e devolve [results]. */
class FakePlayerIndex(var results: List<PlayerSearchResultDto> = emptyList()) : PlayerIndex {
    /** Cada chamada de [record], na ordem. */
    val recorded: MutableList<List<IndexedPlayer>> = Collections.synchronizedList(mutableListOf())

    /** Nomes recebidos em [search], na ordem. */
    val searches: MutableList<String> = Collections.synchronizedList(mutableListOf())

    override fun record(players: List<IndexedPlayer>) {
        recorded += players
    }

    override suspend fun search(name: String): List<PlayerSearchResultDto> {
        searches += name
        return results
    }
}
