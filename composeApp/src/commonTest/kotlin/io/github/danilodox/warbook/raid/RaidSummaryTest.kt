package io.github.danilodox.warbook.raid

import io.github.danilodox.warbook.api.RaidAbsentMemberDto
import io.github.danilodox.warbook.api.RaidMemberDto
import io.github.danilodox.warbook.api.RaidSeasonDto
import io.github.danilodox.warbook.api.RaidState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RaidSummaryTest {

    // 2026-11-13T07:00:00Z → 2026-11-16T07:00:00Z.
    private val start = 1_794_553_200_000L
    private val end = start + 3 * 86_400_000L

    private val ended = RaidSeasonDto(
        state = RaidState.ENDED,
        startTime = start,
        endTime = end,
        capitalTotalLoot = 185_110,
        raidsCompleted = 2,
        totalAttacks = 65,
        enemyDistrictsDestroyed = 23,
        offensiveReward = 148,
        defensiveReward = 0,
        members = listOf(
            RaidMemberDto("A", "Danilo", 6, 6, 12_821, 888),
            RaidMemberDto("B", "Nicolas Guedes", 5, 5, 16_050, 740),
            RaidMemberDto("C", "Rapadura", 2, 6, 3_000, 296),
        ),
        notAttacked = listOf(
            RaidAbsentMemberDto("X", "TwitchBraiiniak", 19),
            RaidAbsentMemberDto("Y", "sanji", 16),
        ),
    )

    @Test
    fun raidEncerradoEmPortugues() {
        val text = RaidSummary.season(ended, "Alliance of War", now = end + 1, texts = PortugueseRaidSummaryTexts)

        assertEquals(
            """
            🏛 *Raid da Capital · Alliance of War* (13/11)

            💰 Ouro da Capital: *185.110*
            ⚔️ Ataques: 65 · Distritos: 23 · Raids: 2
            🏅 Medalhas de raid: 148 por ataque · 888 com 6

            🎯 14 ataques sobrando no clã
            ❌ *2 membros não atacaram*
            • TwitchBraiiniak
            • sanji
            ⚠️ *Atacaram menos*
            • Rapadura (2 de 6)
            """.trimIndent(),
            text,
        )
    }

    @Test
    fun raidEmAndamentoEmInglesSemMedalhasEComTempo() {
        val ongoing = ended.copy(state = RaidState.ONGOING, offensiveReward = 0)

        val text = RaidSummary.season(ongoing, null, now = end - 26 * 3_600_000L, texts = EnglishRaidSummaryTexts)

        assertTrue(text.startsWith("🏛 *Capital Raid* (11/13)\n⏱ Ends in 1d 2h"), text)
        assertTrue("💰 Capital Gold: *185,110*" in text, text)
        assertFalse("Raid medals" in text, "medalhas só quando o raid acaba")
        assertTrue("❌ *2 members still to attack*" in text, text)
    }

    @Test
    fun ninguemFaltouViraUmaLinhaSo() {
        val full = ended.copy(members = ended.members.take(2), notAttacked = emptyList())

        val text = RaidSummary.season(full, "Alliance", now = end, texts = PortugueseRaidSummaryTexts)

        assertTrue(text.endsWith("\n\n✅ Todo mundo usou todos os ataques"), text)
    }

    @Test
    fun nomesComAsteriscoEUnderlineNaoQuebramONegrito() {
        val season = ended.copy(
            members = emptyList(),
            notAttacked = listOf(RaidAbsentMemberDto("X", "*_Rei_*", 15)),
        )

        val text = RaidSummary.season(season, "Clã_*Top*", now = end, texts = PortugueseRaidSummaryTexts)

        assertTrue("Clã＿∗Top∗" in text, text)
        assertTrue("• ∗＿Rei＿∗" in text, text)
    }

    @Test
    fun duracaoEmMinutosHorasEDias() {
        val texts = PortugueseRaidSummaryTexts
        assertEquals("45min", texts.duration(45 * 60_000L))
        assertEquals("5h 07min", texts.duration((5 * 60 + 7) * 60_000L))
        assertEquals("2d 3h", texts.duration((51 * 60) * 60_000L))
        assertEquals("0min", texts.duration(-1))
    }
}
