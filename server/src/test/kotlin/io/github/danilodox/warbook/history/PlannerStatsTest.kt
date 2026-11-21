package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.WarState
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class PlannerStatsTest {

    private val now = Instant.parse("2026-11-20T12:00:00Z")

    private fun war(daysAgo: Long, type: WarType, attacks: List<WarRecordAttack>, anaName: String = "Ana") = WarRecord(
        type = type,
        source = WarSource.SNAPSHOT,
        state = WarState.WAR_ENDED,
        teamSize = 2,
        attacksPerMember = if (type == WarType.CWL) 1 else 2,
        endTime = now.minus(daysAgo, ChronoUnit.DAYS),
        clan = WarSide("CLA", "Nós", stars = 0, destruction = 0.0),
        opponent = WarSide("OPP", "Eles", stars = 0, destruction = 0.0),
        capturedAt = now,
        members = listOf(
            WarRecordMember("A", false, anaName, 15, 1),
            WarRecordMember("B", false, "Bia", 15, 2),
            WarRecordMember("E1", true, "E1", 15, 1),
            WarRecordMember("E2", true, "E2", 15, 2),
        ),
        attacks = attacks,
    )

    private fun triple(order: Int, from: String, to: String) = WarRecordAttack(order, from, to, 3, 100)

    @Test
    fun somaGuerrasComunsECwl() {
        val stats = PlannerStats.of(
            listOf(
                war(1, WarType.CWL, listOf(triple(1, "A", "E1"))),
                war(
                    3,
                    WarType.WAR,
                    listOf(triple(1, "A", "E1"), triple(2, "A", "E2"), triple(3, "B", "E2")),
                    "Ana Antiga",
                ),
            ),
        )

        assertEquals(2, stats.warsCounted)
        val ana = stats.members.single { it.tag == "A" }
        assertEquals("Ana", ana.name, "nome da guerra mais recente")
        assertEquals(2, ana.wars)
        assertEquals(3, ana.attacksUsed)
        assertEquals(3, ana.attacksAvailable)
        assertEquals(6.0, ana.average, "média de 4,00 e 8,00")
        val bia = stats.members.single { it.tag == "B" }
        assertEquals(1, bia.attacksUsed)
        assertEquals(3, bia.attacksAvailable)
        assertEquals(-0.5, bia.average, "−3 na CWL e 4,00 − 2 na guerra: média de −3 e 2")
    }

    @Test
    fun semGuerrasVemVazio() {
        val stats = PlannerStats.of(emptyList())

        assertEquals(0, stats.warsCounted)
        assertEquals(emptyList(), stats.members)
    }
}
