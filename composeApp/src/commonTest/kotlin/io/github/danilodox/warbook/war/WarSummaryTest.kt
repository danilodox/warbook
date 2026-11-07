package io.github.danilodox.warbook.war

import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.api.CwlRoundDto
import io.github.danilodox.warbook.api.CwlState
import io.github.danilodox.warbook.api.WarState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WarSummaryTest {

    private val pt = PortugueseSummaryTexts
    private val en = EnglishSummaryTexts

    @Test
    fun emGuerraTemPlacarTempoEQuemFalta() {
        val text = WarSummary.war(testWar(), NOW, pt)

        assertEquals(
            """
            *Picuí Raiz × Os Mito* (3 × 3)
            ⭐ Estrelas: *31* × 28
            💥 Destruição: 78,4% × 71,0%
            ⏱ Termina em 5h 12min

            ⚔️ *Faltam 3 ataques (2 membros)*
            • Dantas (1)
            • Rapadura (2)
            """.trimIndent(),
            text,
        )
    }

    @Test
    fun emGuerraSemPendentesEmIngles() {
        val text = WarSummary.war(testWar(ourAttacks = listOf(2, 2, 2), endsIn = 45 * MINUTE), NOW, en)

        assertEquals(
            """
            *Picuí Raiz × Os Mito* (3 × 3)
            ⭐ Stars: *31* × 28
            💥 Destruction: 78.4% × 71.0%
            ⏱ Ends in 45m

            ✅ Everyone has attacked
            """.trimIndent(),
            text,
        )
    }

    @Test
    fun preparacaoTemContagemEForcaPorCv() {
        val text = WarSummary.war(testWar(state = WarState.PREPARATION, ourAttacks = listOf(0, 0, 0)), NOW, en)

        assertEquals(
            """
            *Picuí Raiz × Os Mito* (3 × 3)
            🛡 Preparation: battle starts in 14h 03m

            *Strength by TH*
            Average TH: 14.7 × 15.3
            Max TH: 16 × 16
            TH 16: 1 × 2
            TH 15: 1 × 0
            TH 14: 0 × 1
            TH 13: 1 × 0
            """.trimIndent(),
            text,
        )
    }

    @Test
    fun encerradaTemResultadoEAtaquesPerdidos() {
        val text = WarSummary.war(testWar(state = WarState.WAR_ENDED), NOW, pt)

        assertEquals(
            """
            *Picuí Raiz × Os Mito* (3 × 3)
            🏆 *Vitória!*
            ⭐ Estrelas: *31* × 28
            💥 Destruição: 78,4% × 71,0%

            ❌ *3 ataques não foram feitos*
            • Dantas (1)
            • Rapadura (2)
            """.trimIndent(),
            text,
        )
    }

    @Test
    fun cwlTemARodadaENomesSemContagem() {
        val war = testWar(attacksPerMember = 1, ourAttacks = listOf(1, 1, 0), endsIn = 45 * MINUTE, isCwl = true)
        val cwl = CwlDto(
            state = CwlState.IN_WAR,
            currentRound = 4,
            rounds = (1..7).map { CwlRoundDto(it, war.takeIf { _ -> it == 4 }) },
        )

        assertEquals(
            """
            *CWL · Rodada 4 de 7*
            *Picuí Raiz × Os Mito* (3 × 3)
            ⭐ Estrelas: *31* × 28
            💥 Destruição: 78,4% × 71,0%
            ⏱ Termina em 45min

            ⚔️ *Falta 1 ataque (1 membro)*
            • Rapadura
            """.trimIndent(),
            WarSummary.cwl(cwl, round = 4, now = NOW, texts = pt),
        )
        assertNull(WarSummary.cwl(cwl, round = 5, now = NOW, texts = pt), "rodada não sorteada")
    }

    @Test
    fun semGuerraNaoTemResumo() {
        assertNull(WarSummary.war(CurrentWarDto(WarState.NOT_IN_WAR), NOW, pt))
        assertNull(WarSummary.war(CurrentWarDto(WarState.MATCHMAKING), NOW, en))
    }

    @Test
    fun idiomaPeloCodigo() {
        assertEquals(PortugueseSummaryTexts, summaryTextsFor("pt"))
        assertEquals(PortugueseSummaryTexts, summaryTextsFor("pt-BR"))
        assertEquals(EnglishSummaryTexts, summaryTextsFor("en"))
        assertEquals(EnglishSummaryTexts, summaryTextsFor("es"))
    }

    @Test
    fun duracaoEDecimal() {
        assertEquals("0min", pt.duration(-5 * MINUTE), "tempo passado vira zero")
        assertEquals("59min", pt.duration(59 * MINUTE + 59_000))
        assertEquals("1h 05min", pt.duration(HOUR + 5 * MINUTE))
        assertEquals("23h 00m", en.duration(23 * HOUR))
        assertEquals("100,0", pt.decimal(99.96))
        assertEquals("14.0", en.decimal(14.0))
    }
}
