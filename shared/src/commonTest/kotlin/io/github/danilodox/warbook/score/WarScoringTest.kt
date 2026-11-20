package io.github.danilodox.warbook.score

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WarScoringTest {

    private fun assertNear(expected: Double, actual: Double, message: String? = null) =
        assertEquals(expected, actual, 0.001, message)

    private fun member(tag: String, th: Int, pos: Int) = ScoreMember(tag, th, pos)

    private fun attack(order: Int, from: String, to: String, stars: Int, destruction: Int) =
        ScoreAttack(order, from, to, stars, destruction)

    private fun war(
        members: List<ScoreMember>,
        opponents: List<ScoreMember>,
        attacks: List<ScoreAttack>,
        ended: Boolean = true,
        isCwl: Boolean = false,
    ) = ScoreWar(isCwl, attacksPerMember = if (isCwl) 1 else 2, members, opponents, attacks, ended)

    private fun List<MemberScore>.of(tag: String) = single { it.tag == tag }

    // --- Base e fator de CV ---

    @Test
    fun baseEEstrelasMaisDestruicao() {
        assertNear(0.0, WarScoring.base(0, 0))
        assertNear(2.88, WarScoring.base(2, 88))
        assertNear(4.0, WarScoring.base(3, 100))
    }

    @Test
    fun fatorSobeOuDesce15PorCentoPorCv() {
        assertNear(1.0, WarScoring.townHallFactor(15, 15))
        assertNear(1.15, WarScoring.townHallFactor(15, 16))
        assertNear(0.85, WarScoring.townHallFactor(15, 14))
        assertNear(0.7, WarScoring.townHallFactor(15, 13))
    }

    @Test
    fun fatorFicaEntre055E130() {
        assertNear(1.30, WarScoring.townHallFactor(13, 15), "+2 CV bate no teto")
        assertNear(1.30, WarScoring.townHallFactor(12, 17))
        assertNear(0.55, WarScoring.townHallFactor(15, 12), "−3 CV bate no piso")
        assertNear(0.55, WarScoring.townHallFactor(16, 10))
    }

    // --- Ataques ---

    @Test
    fun ataqueParaCimaGanhaBonus() {
        val scores = WarScoring.score(
            war(
                members = listOf(member("A", 14, 2)),
                opponents = listOf(member("X", 15, 1), member("Y", 14, 2)),
                attacks = listOf(attack(1, "A", "X", 2, 80)),
                ended = false,
            ),
        )

        val hit = scores.of("A").attacks.single()
        assertNear(2.8, hit.base)
        assertNear(1.15, hit.factor)
        assertNear(3.22, hit.points)
        assertNull(hit.mark)
    }

    @Test
    fun espelhoENeutroMesmoComCvDiferente() {
        val scores = WarScoring.score(
            war(
                members = listOf(member("A", 14, 1)),
                opponents = listOf(member("X", 16, 1)),
                attacks = listOf(attack(1, "A", "X", 1, 50)),
                ended = false,
            ),
        )

        val hit = scores.of("A").attacks.single()
        assertEquals(AttackMark.MIRROR, hit.mark)
        assertNear(1.0, hit.factor)
        assertNear(1.5, hit.points)
    }

    @Test
    fun mesmoCvForaDoEspelhoTemFator1SemMarca() {
        val hit = WarScoring.score(
            war(
                members = listOf(member("A", 15, 1)),
                opponents = listOf(member("X", 15, 2), member("Y", 15, 1)),
                attacks = listOf(attack(1, "A", "X", 3, 100)),
                ended = false,
            ),
        ).of("A").attacks.single()

        assertNull(hit.mark)
        assertNear(4.0, hit.points)
    }

    @Test
    fun alvoFacilQuandoAindaHaviaBaseIgualOuMaiorSem3Estrelas() {
        // Y (CV 15) segue aberto quando A (CV 15) bate no Z (CV 13).
        val hit = WarScoring.score(
            war(
                members = listOf(member("A", 15, 1)),
                opponents = listOf(member("Y", 15, 2), member("Z", 13, 3)),
                attacks = listOf(attack(1, "A", "Z", 3, 100)),
                ended = false,
            ),
        ).of("A").attacks.single()

        assertEquals(AttackMark.EASY_TARGET, hit.mark)
        assertNear(0.7, hit.factor)
        assertNear(2.8, hit.points)
    }

    @Test
    fun limpezaQuandoTodasAsBasesIguaisOuMaioresJaTinham3Estrelas() {
        val scores = WarScoring.score(
            war(
                members = listOf(member("A", 15, 1), member("B", 15, 2)),
                opponents = listOf(member("Y", 15, 1), member("Z", 13, 2)),
                attacks = listOf(
                    attack(1, "B", "Y", 3, 100),
                    attack(2, "A", "Z", 3, 100),
                ),
                ended = false,
            ),
        )

        val cleanup = scores.of("A").attacks.single()
        assertEquals(AttackMark.CLEANUP, cleanup.mark)
        assertNear(1.0, cleanup.factor, "sem penalidade")
        assertNear(4.0, cleanup.points)
    }

    @Test
    fun aOrdemDosAtaquesDecideAMarca() {
        // Mesmos ataques do teste anterior, mas o A bate no Z antes do B derrubar o Y.
        val scores = WarScoring.score(
            war(
                members = listOf(member("A", 15, 1), member("B", 15, 2)),
                opponents = listOf(member("Y", 15, 1), member("Z", 13, 2)),
                attacks = listOf(
                    attack(2, "B", "Y", 3, 100),
                    attack(1, "A", "Z", 3, 100),
                ),
                ended = false,
            ),
        )

        assertEquals(AttackMark.EASY_TARGET, scores.of("A").attacks.single().mark)
    }

    @Test
    fun duasEstrelasNaoFechamABase() {
        // Y levou só 2★: segue aberto, então bater no Z ainda é alvo fácil.
        val scores = WarScoring.score(
            war(
                members = listOf(member("A", 15, 1), member("B", 15, 2)),
                opponents = listOf(member("Y", 15, 1), member("Z", 13, 2)),
                attacks = listOf(
                    attack(1, "B", "Y", 2, 99),
                    attack(2, "A", "Z", 3, 100),
                ),
                ended = false,
            ),
        )

        assertEquals(AttackMark.EASY_TARGET, scores.of("A").attacks.single().mark)
    }

    @Test
    fun baseMaiorQueOAtacanteTambemContaComoAberta() {
        // Só sobra o X (CV 16) aberto: CV maior que o do atacante também segura a marca de alvo fácil.
        val scores = WarScoring.score(
            war(
                members = listOf(member("A", 15, 2)),
                opponents = listOf(member("X", 16, 1), member("Z", 14, 3)),
                attacks = listOf(attack(1, "A", "Z", 3, 100)),
                ended = false,
            ),
        )

        assertEquals(AttackMark.EASY_TARGET, scores.of("A").attacks.single().mark)
    }

    @Test
    fun espelhoParaBaixoContinuaNeutro() {
        val hit = WarScoring.score(
            war(
                members = listOf(member("A", 16, 1)),
                opponents = listOf(member("X", 14, 1), member("Y", 16, 2)),
                attacks = listOf(attack(1, "A", "X", 3, 100)),
                ended = false,
            ),
        ).of("A").attacks.single()

        assertEquals(AttackMark.MIRROR, hit.mark)
        assertNear(4.0, hit.points)
    }

    @Test
    fun aRegraDeAlvoFacilValeNaCwl() {
        val hit = WarScoring.score(
            war(
                members = listOf(member("A", 15, 1)),
                opponents = listOf(member("Y", 15, 2), member("Z", 12, 3)),
                attacks = listOf(attack(1, "A", "Z", 3, 100)),
                isCwl = true,
            ),
        ).of("A").attacks.single()

        assertEquals(AttackMark.EASY_TARGET, hit.mark)
        assertNear(0.55, hit.factor)
        assertNear(2.2, hit.points)
    }

    // --- Ataques não usados ---

    @Test
    fun ataqueNaoUsadoNaGuerraVale2Negativo() {
        val score = WarScoring.score(
            war(
                members = listOf(member("A", 15, 1)),
                opponents = listOf(member("X", 15, 1)),
                attacks = listOf(attack(1, "A", "X", 3, 100)),
            ),
        ).of("A")

        assertEquals(1, score.missed)
        assertNear(-2.0, score.penalty)
        assertNear(2.0, score.total)
    }

    @Test
    fun nenhumAtaqueNaGuerraVale4Negativo() {
        val score = WarScoring.score(
            war(members = listOf(member("A", 15, 1)), opponents = listOf(member("X", 15, 1)), attacks = emptyList()),
        ).of("A")

        assertEquals(2, score.missed)
        assertNear(-4.0, score.total)
    }

    @Test
    fun ataqueNaoUsadoNaCwlVale3Negativo() {
        val score = WarScoring.score(
            war(
                members = listOf(member("A", 15, 1)),
                opponents = listOf(member("X", 15, 1)),
                attacks = emptyList(),
                isCwl = true,
            ),
        ).of("A")

        assertEquals(1, score.missed)
        assertNear(-3.0, score.total)
    }

    @Test
    fun guerraEmAndamentoNaoPenaliza() {
        val score = WarScoring.score(
            war(
                members = listOf(member("A", 15, 1)),
                opponents = listOf(member("X", 15, 1)),
                attacks = emptyList(),
                ended = false,
            ),
        ).of("A")

        assertEquals(0, score.missed)
        assertNear(0.0, score.total)
    }

    // --- Conjunto ---

    @Test
    fun somaOsDoisAtaquesDoMembro() {
        val score = WarScoring.score(
            war(
                members = listOf(member("A", 15, 1)),
                opponents = listOf(member("X", 15, 1), member("Y", 16, 2)),
                attacks = listOf(
                    attack(1, "A", "X", 2, 90),
                    attack(5, "A", "Y", 1, 60),
                ),
            ),
        ).of("A")

        assertEquals(listOf(1, 5), score.attacks.map { it.order })
        assertNear(2.9 + 1.6 * 1.15, score.total)
        assertEquals(0, score.missed)
    }

    @Test
    fun defesaEAtaquesDoAdversarioNaoContam() {
        val scores = WarScoring.score(
            war(
                members = listOf(member("A", 15, 1)),
                opponents = listOf(member("X", 15, 1)),
                attacks = listOf(
                    attack(1, "X", "A", 3, 100),
                    attack(2, "A", "X", 1, 40),
                ),
                ended = false,
            ),
        )

        assertEquals(listOf(2), scores.of("A").attacks.map { it.order })
    }

    @Test
    fun ataqueContraBaseQueNaoEstaNaEscalacaoFicaDeFora() {
        val score = WarScoring.score(
            war(
                members = listOf(member("A", 15, 1)),
                opponents = listOf(member("X", 15, 1)),
                attacks = listOf(attack(1, "A", "SUMIU", 3, 100)),
                ended = false,
            ),
        ).of("A")

        assertEquals(emptyList(), score.attacks)
    }

    @Test
    fun membrosSaemNaOrdemDoMapa() {
        val scores = WarScoring.score(
            war(
                members = listOf(member("C", 13, 3), member("A", 15, 1), member("B", 14, 2)),
                opponents = listOf(member("X", 15, 1)),
                attacks = emptyList(),
            ),
        )

        assertEquals(listOf("A", "B", "C"), scores.map { it.tag })
    }
}
