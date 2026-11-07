package io.github.danilodox.warbook.war

import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.api.CwlRoundDto
import io.github.danilodox.warbook.api.CwlState
import io.github.danilodox.warbook.api.WarAttackDto
import io.github.danilodox.warbook.api.WarState
import kotlin.test.Test
import kotlin.test.assertEquals

class SummaryOptionsTest {

    private val pt = PortugueseSummaryTexts
    private val en = EnglishSummaryTexts
    private val all = SummaryOptions(SummaryPart.entries.toSet())
    private val none = SummaryOptions(emptySet())

    /** Alvos do Os Mito: Mito01 #1 (CV 16), Brabo #2 (CV 16), Gordin #3 (CV 14). */
    private fun attack(defender: String, position: Int?, stars: Int, destruction: Int, order: Int) =
        WarAttackDto(defender.uppercase(), stars, destruction, order, defenderPosition = position)

    /** Maré Alta, Dantas e Rapadura com os ataques dados, na ordem do mapa. */
    private fun warWith(vararg attacks: List<WarAttackDto>, state: WarState = WarState.IN_WAR): CurrentWarDto {
        val war = testWar(state = state)
        val clan = war.clan!!
        return war.copy(
            clan = clan.copy(members = clan.members.mapIndexed { i, member -> member.copy(attacks = attacks[i]) }),
        )
    }

    private val mixed = warWith(
        listOf(attack("Mito01", 1, 3, 100, 1), attack("Brabo", 2, 1, 45, 4)),
        listOf(attack("Gordin", 3, 3, 100, 2)),
        listOf(attack("Mito01", 1, 0, 32, 3)),
    )

    @Test
    fun padraoEOModeloDaV040() {
        assertEquals(SummaryOptions.DEFAULT_PARTS, SummaryOptions().parts)
        assertEquals(WarSummary.war(mixed, NOW, pt), WarSummary.war(mixed, NOW, pt, SummaryOptions()))
    }

    @Test
    fun tudoLigadoEmGuerraTemMelhoresERuins() {
        assertEquals(
            """
            *Picuí Raiz × Os Mito* (3 × 3)
            ⭐ Estrelas: *31* × 28
            💥 Destruição: 78,4% × 71,0%
            ⏱ Termina em 5h 12min

            ⚔️ *Faltam 2 ataques (2 membros)*
            • Dantas (1)
            • Rapadura (1)

            🔥 *Melhores ataques*
            • Maré Alta → #1 (CV 16) ★★★ 100%
            • Dantas → #3 (CV 14) ★★★ 100%

            👎 *Ataques ruins*
            • Rapadura → #1 (CV 16) ☆☆☆ 32%
            • Maré Alta → #2 (CV 16) ★☆☆ 45%
            """.trimIndent(),
            WarSummary.war(mixed, NOW, pt, all),
        )
    }

    @Test
    fun tudoDesligadoFicaSoOCabecalho() {
        assertEquals("*Picuí Raiz × Os Mito* (3 × 3)", WarSummary.war(mixed, NOW, pt, none))
    }

    @Test
    fun encerradaSemTresEstrelasMostraOsTresMelhoresEmIngles() {
        val war = warWith(
            listOf(attack("Mito01", 1, 2, 88, 1), attack("Brabo", 2, 2, 91, 4)),
            listOf(attack("Gordin", 3, 1, 60, 2)),
            listOf(attack("Mito01", 1, 2, 70, 3)),
            state = WarState.WAR_ENDED,
        )

        assertEquals(
            """
            *Picuí Raiz × Os Mito* (3 × 3)
            🏆 *Victory!*
            ⭐ Stars: *31* × 28
            💥 Destruction: 78.4% × 71.0%

            🔥 *Best attacks*
            • Maré Alta → #2 (TH 16) ★★☆ 91%
            • Maré Alta → #1 (TH 16) ★★☆ 88%
            • Rapadura → #1 (TH 16) ★★☆ 70%
            """.trimIndent(),
            WarSummary.war(war, NOW, en, SummaryOptions(setOf(SummaryPart.SCORE, SummaryPart.BEST_ATTACKS))),
        )
    }

    @Test
    fun maisDeCincoViramMaisN() {
        val weak = (1..7).map { attack("Brabo", 2, 0, 10 + it, it) }
        val war = warWith(weak.take(2), weak.subList(2, 4), weak.drop(4))

        assertEquals(
            """
            *Picuí Raiz × Os Mito* (3 × 3)

            👎 *Ataques ruins*
            • Maré Alta → #2 (CV 16) ☆☆☆ 11%
            • Maré Alta → #2 (CV 16) ☆☆☆ 12%
            • Dantas → #2 (CV 16) ☆☆☆ 13%
            • Dantas → #2 (CV 16) ☆☆☆ 14%
            • Rapadura → #2 (CV 16) ☆☆☆ 15%
            • +2 ataques
            """.trimIndent(),
            WarSummary.war(war, NOW, pt, SummaryOptions(setOf(SummaryPart.WEAK_ATTACKS))),
        )
    }

    @Test
    fun alvoDesconhecidoFicaSemPosicao() {
        val war = warWith(listOf(attack("Sumido", null, 3, 100, 1)), emptyList(), emptyList())

        assertEquals(
            """
            *Picuí Raiz × Os Mito* (3 × 3)

            🔥 *Melhores ataques*
            • Maré Alta ★★★ 100%
            """.trimIndent(),
            WarSummary.war(war, NOW, pt, SummaryOptions(setOf(SummaryPart.BEST_ATTACKS, SummaryPart.WEAK_ATTACKS))),
        )
    }

    @Test
    fun preparacaoSemTempoNemForcaENaoMostraAtaques() {
        val war = testWar(state = WarState.PREPARATION, ourAttacks = listOf(0, 0, 0))

        assertEquals(
            """
            *Picuí Raiz × Os Mito* (3 × 3)
            🛡 Preparação
            """.trimIndent(),
            WarSummary.war(war, NOW, pt, SummaryOptions(setOf(SummaryPart.BEST_ATTACKS, SummaryPart.WEAK_ATTACKS))),
        )
    }

    @Test
    fun cwlSemNadaLigadoTemRodadaECabecalho() {
        val cwl = CwlDto(state = CwlState.IN_WAR, currentRound = 1, rounds = listOf(CwlRoundDto(1, mixed)))

        assertEquals(
            """
            *CWL · Rodada 1 de 1*
            *Picuí Raiz × Os Mito* (3 × 3)
            """.trimIndent(),
            WarSummary.cwl(cwl, round = 1, now = NOW, texts = pt, options = none),
        )
    }

    @Test
    fun fichasSoDoQueMudaOTextoEmCadaEstado() {
        assertEquals(listOf(SummaryPart.TIME, SummaryPart.STRENGTH), summaryPartsFor(WarState.PREPARATION))
        assertEquals(false, SummaryPart.TIME in summaryPartsFor(WarState.WAR_ENDED), "encerrada não tem tempo")
        assertEquals(false, SummaryPart.STRENGTH in summaryPartsFor(WarState.IN_WAR))
        assertEquals(emptyList(), summaryPartsFor(WarState.NOT_IN_WAR))
    }

    @Test
    fun toggleLigaEDesliga() {
        val on = SummaryOptions().toggle(SummaryPart.BEST_ATTACKS)
        assertEquals(true, SummaryPart.BEST_ATTACKS in on)
        assertEquals(SummaryOptions(), on.toggle(SummaryPart.BEST_ATTACKS))
    }
}
