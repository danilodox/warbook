package io.github.danilodox.warbook.war

import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.WarState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WarInsightsTest {

    @Test
    fun pendentesPelaPosicaoComQuantosFaltam() {
        val pending = testWar(ourAttacks = listOf(0, 2, 1)).pendingAttackers()

        assertEquals(listOf("Maré Alta" to 2, "Rapadura" to 1), pending.map { it.name to it.remaining })
    }

    @Test
    fun semGuerraNinguemPendente() {
        assertEquals(emptyList(), CurrentWarDto(WarState.NOT_IN_WAR).pendingAttackers())
    }

    @Test
    fun resultadoPorEstrelasEDepoisDestruicao() {
        assertEquals(WarResult.WIN, testWar(ourStars = 30, theirStars = 29).result())
        assertEquals(WarResult.LOSS, testWar(ourStars = 30, theirStars = 30, ourDestruction = 70.0).result())
        assertEquals(
            WarResult.DRAW,
            testWar(ourStars = 30, theirStars = 30, ourDestruction = 71.0, theirDestruction = 71.0).result(),
        )
        assertNull(CurrentWarDto(WarState.NOT_IN_WAR).result())
    }

    @Test
    fun forcaPorCvMediaMaximoEDistribuicao() {
        val strength = testWar().strength()!!

        assertEquals(44.0 / 3, strength.clan.averageTownHall)
        assertEquals(16, strength.opponent.maxTownHall)
        assertEquals(mapOf(16 to 2, 14 to 1), strength.opponent.townHallCounts)
        assertEquals(listOf(16, 15, 14, 13), strength.townHallLevels)
    }

    @Test
    fun forcaSemMembrosNaoSeCalcula() {
        assertNull(CurrentWarDto(WarState.PREPARATION).strength())
    }
}
