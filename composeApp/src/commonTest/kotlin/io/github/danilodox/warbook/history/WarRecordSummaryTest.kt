package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.MemberWarScoreDto
import io.github.danilodox.warbook.api.ScoredAttackDto
import io.github.danilodox.warbook.api.WarDetailDto
import io.github.danilodox.warbook.api.WarKind
import io.github.danilodox.warbook.api.WarOutcome
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.api.WarSummaryDto
import io.github.danilodox.warbook.score.AttackMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WarRecordSummaryTest {

    // 2026-11-19T18:00:00Z.
    private val end = 1_795_111_200_000L

    private fun war(result: WarOutcome?, kind: WarKind = WarKind.WAR) = WarSummaryDto(
        id = 1,
        kind = kind,
        state = if (result == null) WarState.IN_WAR else WarState.WAR_ENDED,
        endTime = end,
        teamSize = 5,
        attacksPerMember = 2,
        stars = 12,
        destruction = 85.34,
        opponentStars = 10,
        opponentDestruction = 70.06,
        detailed = true,
        opponentName = "Rivais",
        result = result,
    )

    private fun attack(points: Double, mark: AttackMark? = null) =
        ScoredAttackDto(order = 1, defenderTag = "Q", stars = 3, destruction = 100, factor = 1.0, points, mark)

    private fun member(name: String, position: Int, total: Double, attacks: List<ScoredAttackDto>, missed: Int = 0) =
        MemberWarScoreDto("T$position", name, 15, position, attacks, missed, penalty = -2.0 * missed, total)

    private val members = listOf(
        member("Danilo", 1, 8.0, listOf(attack(4.0), attack(4.0))),
        member("Rapadura", 2, 6.4, listOf(attack(4.0), attack(2.4, AttackMark.EASY_TARGET))),
        member("Nicolas", 3, 1.0, listOf(attack(3.0)), missed = 1),
        member("sanji", 4, -4.0, emptyList(), missed = 2),
        member("Bia", 5, 7.0, listOf(attack(3.5), attack(3.5))),
    )

    @Test
    fun guerraEncerradaEmPortugues() {
        val text = WarRecordSummary.of(WarDetailDto(war(WarOutcome.WIN), members), PortugueseWarRecordSummaryTexts)

        val expected = """
            🏁 *Fim da guerra contra Rivais* (19/11)
            🏆 *Vitória!*
            ⭐ Estrelas: *12* × 10
            💥 Destruição: 85,3% × 70,1%

            📊 *Melhores notas*
            • Danilo 8,00
            • Bia 7,00
            • Rapadura 6,40

            ❌ *3 ataques não foram feitos*
            • Nicolas (1 de 2)
            • sanji (0 de 2)

            🎯 *Pegaram alvo fácil*
            • Rapadura (1 ataque)
        """.trimIndent()
        assertEquals(expected, text)
    }

    @Test
    fun rodadaDaCwlEmInglesSemFaltas() {
        val all = members.filter { it.missed == 0 }
            .map { member -> member.copy(attacks = member.attacks.map { it.copy(mark = null) }) }
        val text = WarRecordSummary.of(
            WarDetailDto(war(WarOutcome.LOSE, WarKind.CWL), all),
            EnglishWarRecordSummaryTexts,
        )

        val expected = """
            🏁 *CWL round against Rivais is over* (11/19)
            😞 *Defeat*
            ⭐ Stars: *12* × 10
            💥 Destruction: 85.3% × 70.1%

            📊 *Top scores*
            • Danilo 8.00
            • Bia 7.00
            • Rapadura 6.40

            ✅ Everyone used all attacks
        """.trimIndent()
        assertEquals(expected, text)
    }

    @Test
    fun guerraEmAndamentoNaoTemResumo() {
        assertNull(WarRecordSummary.of(WarDetailDto(war(null), members), PortugueseWarRecordSummaryTexts))
    }

    @Test
    fun guerraImportadaSemAtaquesSoTemOPlacar() {
        val text = WarRecordSummary.of(WarDetailDto(war(WarOutcome.TIE), emptyList()), PortugueseWarRecordSummaryTexts)

        val expected = """
            🏁 *Fim da guerra contra Rivais* (19/11)
            🤝 *Empate*
            ⭐ Estrelas: *12* × 10
            💥 Destruição: 85,3% × 70,1%
        """.trimIndent()
        assertEquals(expected, text)
    }

    @Test
    fun nomesComAsteriscoNaoViramNegrito() {
        val detail = WarDetailDto(
            war(WarOutcome.WIN).copy(opponentName = "*Rei_do_Norte*"),
            listOf(member("*Ana*", 1, 4.0, listOf(attack(4.0)))),
        )

        val text = WarRecordSummary.of(detail, PortugueseWarRecordSummaryTexts).orEmpty()

        assertTrue("contra ∗Rei＿do＿Norte∗*" in text)
        assertTrue("• ∗Ana∗ 4,00" in text)
    }
}
