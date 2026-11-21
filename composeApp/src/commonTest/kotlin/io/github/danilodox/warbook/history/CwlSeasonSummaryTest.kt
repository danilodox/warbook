package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.CwlMemberSeasonDto
import io.github.danilodox.warbook.api.CwlSeasonDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CwlSeasonSummaryTest {

    private fun member(name: String, rounds: Int, attacks: Int, stars: Int, total: Double) =
        CwlMemberSeasonDto(name, name, 15, rounds, attacks, stars, total)

    private val season = CwlSeasonDto(
        season = "2026-11",
        rounds = 7,
        wins = 5,
        losses = 1,
        ties = 1,
        stars = 180,
        opponentStars = 150,
        members = listOf(
            member("Danilo", 7, 7, 20, 25.4),
            member("Bia", 7, 7, 19, 24.0),
            member("Rapadura", 6, 5, 13, 12.25),
            member("sanji", 3, 0, 0, -9.0),
        ),
    )

    @Test
    fun temporadaEmPortugues() {
        val expected = """
            🏅 *CWL de 11/2026* · 7 de 7 rodadas
            📈 5 vitórias · 1 derrota · 1 empate
            ⭐ Estrelas: *180* × 150

            📊 *Melhores notas da temporada*
            • Danilo 25,40 (20⭐)
            • Bia 24,00 (19⭐)
            • Rapadura 12,25 (13⭐)

            ❌ *4 ataques não foram feitos*
            • Rapadura (5 de 6)
            • sanji (0 de 3)
        """.trimIndent()
        assertEquals(expected, CwlSeasonSummary.of(season, PortugueseCwlSeasonSummaryTexts))
    }

    @Test
    fun temporadaEmInglesSemFaltas() {
        val full = season.copy(rounds = 2, members = listOf(member("Danilo", 2, 2, 6, 8.0)))

        val expected = """
            🏅 *CWL 11/2026* · 2 of 7 rounds
            📈 5 wins · 1 loss · 1 draw
            ⭐ Stars: *180* × 150

            📊 *Season top scores*
            • Danilo 8.00 (6⭐)

            ✅ Everyone attacked in every round
        """.trimIndent()
        assertEquals(expected, CwlSeasonSummary.of(full, EnglishCwlSeasonSummaryTexts))
    }

    @Test
    fun semTemporadaNaoTemResumo() {
        assertNull(CwlSeasonSummary.of(CwlSeasonDto(), PortugueseCwlSeasonSummaryTexts))
    }

    @Test
    fun rotuloDaTemporada() {
        assertEquals("11/2026", CwlSeasonSummary.seasonLabel("2026-11"))
        assertEquals("estranho", CwlSeasonSummary.seasonLabel("estranho"))
    }
}
