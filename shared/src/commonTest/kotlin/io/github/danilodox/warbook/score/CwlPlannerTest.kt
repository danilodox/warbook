package io.github.danilodox.warbook.score

import io.github.danilodox.warbook.api.PlannerStatsDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CwlPlannerTest {

    /** Sem [average] e sem ataques disponíveis = membro sem histórico. */
    private fun candidate(tag: String, townHall: Int, average: Double? = null, used: Int = 0, available: Int = 0) =
        PlannerCandidate(tag, tag, townHall, stats(tag, average, used, available))

    private fun stats(tag: String, average: Double?, used: Int, available: Int): PlannerStatsDto? =
        if (average == null && available == 0) null else PlannerStatsDto(tag, tag, 10, used, available, average)

    @Test
    fun notaVezesPresenca() {
        assertEquals(3.2, CwlPlanner.rating(3.2, 20, 20))
        assertEquals(2.38, CwlPlanner.rating(3.4, 14, 20))
        assertEquals(0.0, CwlPlanner.rating(3.0, 0, 20))
    }

    @Test
    fun mediaNegativaNaoMelhoraComFalta() {
        assertEquals(-2.0, CwlPlanner.rating(-2.0, 5, 20))
    }

    @Test
    fun semNotaOuSemAtaquesDisponiveisNaoTemPosicao() {
        assertNull(CwlPlanner.rating(null, 0, 0))
        assertNull(CwlPlanner.rating(3.0, 0, 0))
        assertNull(candidate("N", 16).rating)
    }

    @Test
    fun usadosAMaisNaoPassamDeCemPorCento() {
        assertEquals(3.0, CwlPlanner.rating(3.0, 25, 20))
    }

    @Test
    fun ordenaPelaPosicaoESemHistoricoNoFimPorCv() {
        val ranked = CwlPlanner.rank(
            listOf(
                candidate("NOVO15", 15),
                candidate("BIA", 15, 3.4, 14, 20),
                candidate("NOVO16", 16),
                candidate("DANILO", 16, 3.2, 20, 20),
                candidate("RUIM", 14, -1.0, 2, 20),
            ),
        )

        assertEquals(listOf("DANILO", "BIA", "RUIM", "NOVO16", "NOVO15"), ranked.map { it.tag })
    }

    @Test
    fun empateNaPosicaoVaiPeloCvMaior() {
        val ranked = CwlPlanner.rank(listOf(candidate("A", 14, 3.0, 10, 10), candidate("B", 16, 3.0, 10, 10)))

        assertEquals(listOf("B", "A"), ranked.map { it.tag })
    }

    @Test
    fun sugereOsPrimeirosDoTamanho() {
        val candidates = (1..20).map { candidate("M$it", 15, it.toDouble(), 10, 10) }

        val suggested = CwlPlanner.suggest(candidates, 15)

        assertEquals(15, suggested.size)
        assertEquals((6..20).map { "M$it" }.toSet(), suggested)
    }

    @Test
    fun claPequenoSugereTodos() {
        val candidates = listOf(candidate("A", 15, 3.0, 10, 10), candidate("B", 14))

        assertEquals(setOf("A", "B"), CwlPlanner.suggest(candidates, 15))
    }

    @Test
    fun escalacaoNaOrdemDoMapa() {
        val candidates = listOf(
            candidate("BIA", 15, 3.4, 20, 20),
            candidate("DANILO", 16, 2.0, 20, 20),
            candidate("CAIO", 15, 3.0, 20, 20),
            candidate("FORA", 17, 1.0, 20, 20),
            candidate("NOVO", 16),
        )

        val lineup = CwlPlanner.lineup(candidates, setOf("BIA", "DANILO", "CAIO", "NOVO"))

        assertEquals(listOf("DANILO", "NOVO", "BIA", "CAIO"), lineup.map { it.tag })
    }
}
