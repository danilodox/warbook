package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.MemberActivityDto
import io.github.danilodox.warbook.api.WarState
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClanActivityTest {

    private val now = Instant.parse("2026-11-20T12:00:00Z")

    private fun member(tag: String, name: String = tag, th: Int = 15, pos: Int = 1, opponent: Boolean = false) =
        WarRecordMember(tag, opponent, name, th, pos)

    private fun war(
        daysAgo: Long,
        ours: List<WarRecordMember>,
        attacks: List<Pair<String, String>>,
        type: WarType = WarType.WAR,
        season: String? = null,
    ): WarRecord {
        val opponents = listOf(member("E1", opponent = true), member("E2", pos = 2, opponent = true))
        return WarRecord(
            type = type,
            source = WarSource.SNAPSHOT,
            state = WarState.WAR_ENDED,
            teamSize = ours.size,
            attacksPerMember = if (type == WarType.CWL) 1 else 2,
            endTime = now.minus(daysAgo, ChronoUnit.DAYS),
            clan = WarSide("CLA", "Nós", stars = 0, destruction = 0.0),
            opponent = WarSide("OPP", "Eles", stars = 0, destruction = 0.0),
            capturedAt = now,
            cwlSeason = season,
            members = ours + opponents,
            attacks = attacks.mapIndexed { i, (from, to) ->
                WarRecordAttack(i + 1, from, to, stars = 2, destruction = 50)
            },
        )
    }

    private fun List<MemberActivityDto>.of(tag: String) = single { it.tag == tag }

    @Test
    fun contaAtaquesUsadosEDisponiveisNasGuerras() {
        val activity = ClanActivity.of(
            wars = listOf(
                war(1, listOf(member("A"), member("B")), listOf("A" to "E1", "A" to "E2", "B" to "E1")),
                war(3, listOf(member("A"), member("B")), listOf("A" to "E1")),
            ),
            cwl = emptyList(),
        )

        assertEquals(2, activity.warsCounted)
        val a = activity.members.of("A")
        assertEquals(2, a.wars)
        assertEquals(3, a.attacksUsed)
        assertEquals(4, a.attacksAvailable)
        val b = activity.members.of("B")
        assertEquals(1, b.attacksUsed)
        assertEquals(4, b.attacksAvailable)
    }

    @Test
    fun quemDeixouDeAtacarMaisVemPrimeiro() {
        val activity = ClanActivity.of(
            wars = listOf(
                war(
                    1,
                    listOf(member("A", "Ana"), member("B", "Bia"), member("C", "Caio")),
                    listOf(
                        "A" to "E1",
                        "A" to "E2",
                        "C" to "E1",
                    ),
                ),
            ),
            cwl = emptyList(),
        )

        assertEquals(listOf("B", "C", "A"), activity.members.map { it.tag })
    }

    @Test
    fun ataqueContraOProprioClaOuAMaisNaoContam() {
        val activity = ClanActivity.of(
            wars = listOf(
                war(
                    1,
                    listOf(member("A"), member("B")),
                    listOf(
                        "A" to "B",
                        "E1" to "A",
                        "A" to "E1",
                        "A" to "E2",
                        "A" to "E1",
                    ),
                ),
            ),
            cwl = emptyList(),
        )

        assertEquals(2, activity.members.of("A").attacksUsed, "no máximo os disponíveis")
    }

    @Test
    fun cwlContaSoATemporadaMaisRecente() {
        val activity = ClanActivity.of(
            wars = emptyList(),
            cwl = listOf(
                war(1, listOf(member("A"), member("B")), listOf("A" to "E1"), WarType.CWL, "2026-11"),
                war(2, listOf(member("A")), listOf("A" to "E2"), WarType.CWL, "2026-11"),
                war(32, listOf(member("A"), member("B")), listOf("B" to "E1"), WarType.CWL, "2026-10"),
            ),
        )

        assertEquals("2026-11", activity.cwlSeason)
        assertEquals(2, activity.cwlRounds)
        val a = activity.members.of("A")
        assertEquals(2, a.cwlRounds)
        assertEquals(2, a.cwlAttacksUsed)
        val b = activity.members.of("B")
        assertEquals(1, b.cwlRounds)
        assertEquals(0, b.cwlAttacksUsed)
        assertEquals(0, b.wars, "sem guerras comuns guardadas")
    }

    @Test
    fun nomeECvSaoOsDaGuerraMaisRecente() {
        val activity = ClanActivity.of(
            wars = listOf(
                war(1, listOf(member("A", "Nome Novo", th = 16)), emptyList()),
                war(5, listOf(member("A", "Nome Velho", th = 15)), emptyList()),
            ),
            cwl = emptyList(),
        )

        val a = activity.members.single()
        assertEquals("Nome Novo", a.name)
        assertEquals(16, a.townHall)
    }

    @Test
    fun semGuerrasGuardadas() {
        val activity = ClanActivity.of(emptyList(), emptyList())

        assertEquals(0, activity.warsCounted)
        assertEquals(emptyList(), activity.members)
        assertNull(activity.cwlSeason)
    }
}
