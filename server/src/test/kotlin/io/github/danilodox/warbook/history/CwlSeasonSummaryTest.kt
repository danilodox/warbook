package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.CwlSeasonDto
import io.github.danilodox.warbook.api.WarState
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class CwlSeasonSummaryTest {

    private val now = Instant.parse("2026-11-20T12:00:00Z")

    private fun member(tag: String, pos: Int, opponent: Boolean = false, name: String = tag) =
        WarRecordMember(tag, opponent, name, 15, pos)

    private fun round(
        daysAgo: Long,
        season: String,
        result: WarResult,
        attacks: List<WarRecordAttack>,
        names: Pair<String, String> = "Ana" to "Bia",
    ) = WarRecord(
        type = WarType.CWL,
        source = WarSource.SNAPSHOT,
        state = WarState.WAR_ENDED,
        teamSize = 2,
        attacksPerMember = 1,
        endTime = now.minus(daysAgo, ChronoUnit.DAYS),
        clan = WarSide("CLA", "Nós", stars = 4, destruction = 75.0),
        opponent = WarSide("OPP", "Eles", stars = 3, destruction = 60.0),
        capturedAt = now,
        cwlSeason = season,
        result = result,
        members = listOf(
            member("A", 1, name = names.first),
            member("B", 2, name = names.second),
            member("E1", 1, opponent = true),
            member("E2", 2, opponent = true),
        ),
        attacks = attacks,
    )

    private fun attack(order: Int, from: String, to: String, stars: Int, destruction: Int) =
        WarRecordAttack(order, from, to, stars, destruction)

    @Test
    fun somaAsRodadasDaTemporadaMaisRecente() {
        val season = CwlSeasonSummary.of(
            listOf(
                round(1, "2026-11", WarResult.LOSE, listOf(attack(1, "A", "E2", 2, 50), attack(2, "B", "E1", 3, 100))),
                round(2, "2026-11", WarResult.WIN, listOf(attack(1, "A", "E1", 3, 100)), names = "Ana Antiga" to "Bia"),
                round(32, "2026-10", WarResult.WIN, listOf(attack(1, "B", "E2", 3, 100))),
            ),
        )

        assertEquals("2026-11", season.season)
        assertEquals(2, season.rounds)
        assertEquals(1, season.wins)
        assertEquals(1, season.losses)
        assertEquals(0, season.ties)
        assertEquals(8, season.stars)
        assertEquals(6, season.opponentStars)
        assertEquals(listOf("A", "B"), season.members.map { it.tag }, "da maior nota para a menor")
        val ana = season.members[0]
        assertEquals("Ana", ana.name, "nome da rodada mais recente")
        assertEquals(2, ana.rounds)
        assertEquals(2, ana.attacksUsed)
        assertEquals(5, ana.stars)
        assertEquals(6.5, ana.total, "4,00 + 2,50")
        val bia = season.members[1]
        assertEquals(1, bia.attacksUsed)
        assertEquals(1.0, bia.total, "4,00 menos 3 do ataque não usado na CWL")
    }

    @Test
    fun semCwlGuardadaVemVazio() {
        assertEquals(CwlSeasonDto(), CwlSeasonSummary.of(emptyList()))
    }
}
