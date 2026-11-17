package io.github.danilodox.warbook.raid

import io.github.danilodox.warbook.api.RaidAbsentMemberDto
import io.github.danilodox.warbook.api.RaidMemberDto
import io.github.danilodox.warbook.api.RaidSeasonDto
import io.github.danilodox.warbook.api.RaidState
import kotlin.test.Test
import kotlin.test.assertEquals

class RaidInsightsTest {

    @Test
    fun dataEmUtcSemBibliotecaDeDatas() {
        assertEquals(DayMonth(1, 1), dayMonthUtc(0))
        // 2026-11-13T07:00:00Z, sexta de raid.
        assertEquals(DayMonth(13, 11), dayMonthUtc(1_794_553_200_000))
        // 2024-02-29T23:59:59Z, ano bissexto.
        assertEquals(DayMonth(29, 2), dayMonthUtc(1_709_251_199_000))
        // 2024-03-01T00:00:00Z.
        assertEquals(DayMonth(1, 3), dayMonthUtc(1_709_251_200_000))
        // 1969-12-31T23:00:00Z: antes de 1970 também.
        assertEquals(DayMonth(31, 12), dayMonthUtc(-3_600_000))
    }

    @Test
    fun quemAtacouMenosVemPrimeiroEOsCompletosFicamFora() {
        val season = season(
            member("A", attacks = 6, limit = 6),
            member("B", attacks = 3, limit = 5),
            member("C", attacks = 1, limit = 6),
            member("D", attacks = 5, limit = 5),
        )

        assertEquals(listOf("C", "B"), season.attackedLess().map { it.tag })
    }

    @Test
    fun ataquesSobrandoSomamFaltasEOsCincoDeQuemNaoAtacou() {
        val season = season(
            member("B", attacks = 3, limit = 5),
            member("C", attacks = 1, limit = 6),
            absent = listOf(RaidAbsentMemberDto("X", "Xis", 14), RaidAbsentMemberDto("Y", "Ípsilon", 12)),
        )

        assertEquals(2 + 5 + 2 * BASE_ATTACKS, season.unusedAttacks())
    }

    private fun member(tag: String, attacks: Int, limit: Int) = RaidMemberDto(tag, tag, attacks, limit, 1000)

    private fun season(vararg members: RaidMemberDto, absent: List<RaidAbsentMemberDto> = emptyList()) =
        RaidSeasonDto(RaidState.ONGOING, members = members.toList(), notAttacked = absent)
}
