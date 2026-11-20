package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.PlannerStatsDto
import io.github.danilodox.warbook.score.CwlPlanner
import io.github.danilodox.warbook.score.PlannerCandidate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LineupSummaryTest {

    private fun candidate(tag: String, name: String, townHall: Int, average: Double?) = PlannerCandidate(
        tag = tag,
        name = name,
        townHall = townHall,
        stats = average?.let { PlannerStatsDto(tag, name, 10, 20, 20, it) },
    )

    private val candidates = CwlPlanner.rank(
        listOf(
            candidate("D", "Danilo", 16, 3.0),
            candidate("B", "Bia", 15, 3.4),
            candidate("C", "Caio", 15, 2.0),
            candidate("A", "*Ana*", 14, 1.0),
            candidate("N", "Novato", 16, null),
        ) + (1..6).map { candidate("R$it", "Reserva $it", 12, 1.0 - it / 10.0) },
    )

    @Test
    fun escalacaoNaOrdemDoMapaComReservas() {
        val text = LineupSummary.of(candidates, setOf("D", "B", "C", "N"), 4, PortugueseLineupSummaryTexts)

        val expected = """
            📋 *Escalação da CWL* (4 × 4)
            1. Danilo · CV 16
            2. Novato · CV 16
            3. Bia · CV 15
            4. Caio · CV 15

            🪑 *Reservas*
            • ∗Ana∗ · CV 14
            • Reserva 1 · CV 12
            • Reserva 2 · CV 12
            • Reserva 3 · CV 12
            • Reserva 4 · CV 12
        """.trimIndent()
        assertEquals(expected, text)
    }

    @Test
    fun foraDoTamanhoMostraQuantosFaltamEmIngles() {
        val all = candidates.map { it.tag }.toSet()

        val text = LineupSummary.of(candidates, all - "R6", 15, EnglishLineupSummaryTexts).orEmpty()

        assertEquals("📋 *CWL lineup* (10 of 15)", text.lines().first())
        assertEquals("1. Danilo · TH 16", text.lines()[1])
        assertEquals(listOf("🪑 *Reserves*", "• Reserva 6 · TH 12"), text.lines().takeLast(2))
    }

    @Test
    fun ninguemEscolhidoNaoTemResumo() {
        assertNull(LineupSummary.of(candidates, emptySet(), 15, PortugueseLineupSummaryTexts))
    }
}
