package io.github.danilodox.warbook.coc

import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.history.WarRecordAttack
import io.github.danilodox.warbook.history.WarRecordMember
import io.github.danilodox.warbook.history.WarResult
import io.github.danilodox.warbook.history.WarSource
import io.github.danilodox.warbook.history.WarType
import io.github.danilodox.warbook.testing.CocFixtures
import kotlinx.serialization.json.Json
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CocSnapshotMapperTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val now = Instant.parse("2026-11-03T10:00:00Z")
    private val origin = SnapshotOrigin(CocFixtures.CLAN_TAG, capturedAt = now)

    private fun war(body: String = CocFixtures.warInWar) = json.decodeFromString<CocWar>(body)

    @Test
    fun retratoDaGuerraEmAndamento() {
        val record = war().toSnapshot(origin)!!

        assertEquals(WarSource.SNAPSHOT, record.source)
        assertEquals(WarType.WAR, record.type)
        assertEquals(WarState.IN_WAR, record.state)
        assertEquals(15, record.teamSize)
        assertEquals(2, record.attacksPerMember)
        assertEquals(Instant.parse("2026-11-03T12:00:00Z"), record.endTime)
        assertEquals(Instant.parse("2026-11-02T12:00:00Z"), record.startTime)
        assertEquals(now, record.capturedAt)
        assertEquals(CocFixtures.CLAN_TAG, record.clan.tag)
        assertEquals(31, record.clan.stars)
        assertEquals("OPP", record.opponent.tag)
        assertNull(record.result, "sem resultado antes do fim")
    }

    @Test
    fun posicoesSeguemAOrdemDoMapaSemBuracos() {
        val members = war().toSnapshot(origin)!!.members

        assertEquals(
            listOf(
                WarRecordMember("ABC", isOpponent = false, "Maré Alta", 16, mapPosition = 1),
                WarRecordMember(CocFixtures.PLAYER_TAG, isOpponent = false, "Dantas", 15, mapPosition = 2),
                WarRecordMember("OPP1", isOpponent = true, "Mito01", 16, mapPosition = 1),
                WarRecordMember("DEF", isOpponent = true, "Brabo", 16, mapPosition = 2),
            ),
            members,
        )
    }

    @Test
    fun ataquesDosDoisLadosEmOrdem() {
        val attacks = war().toSnapshot(origin)!!.attacks

        assertEquals(
            listOf(
                WarRecordAttack(5, CocFixtures.PLAYER_TAG, "OPP1", stars = 2, destruction = 88, duration = 150),
                WarRecordAttack(9, CocFixtures.PLAYER_TAG, "DEF", stars = 3, destruction = 100, duration = 120),
            ),
            attacks,
        )
    }

    @Test
    fun guerraEncerradaTemResultado() {
        val record = war(CocFixtures.warInWar.replace("\"inWar\"", "\"warEnded\"")).toSnapshot(origin)!!

        assertEquals(WarState.WAR_ENDED, record.state)
        assertEquals(WarResult.WIN, record.result, "31 a 28 estrelas")
    }

    @Test
    fun naCwlOClaDoLadoAdversarioTrocaOsLados() {
        val original = war()
        val swapped = original.copy(clan = original.opponent, opponent = original.clan)
        val cwl = origin.copy(type = WarType.CWL, warTag = "#8QJ2", cwlSeason = "2026-11")

        val record = swapped.toSnapshot(cwl)!!

        assertEquals(CocFixtures.CLAN_TAG, record.clan.tag)
        assertEquals("OPP", record.opponent.tag)
        assertEquals(1, record.attacksPerMember, "na CWL é sempre 1")
        assertEquals("#8QJ2", record.warTag)
        assertEquals("2026-11", record.cwlSeason)
        val ours = record.members.filterNot { it.isOpponent }.map { it.tag }
        assertEquals(setOf("ABC", CocFixtures.PLAYER_TAG), ours.toSet())
    }

    @Test
    fun guerraDeOutroClaNaoViraRetrato() {
        assertNull(war().toSnapshot(origin.copy(clanTag = "OUTRO")))
    }

    @Test
    fun foraDeGuerraNaoViraRetrato() {
        assertNull(war(CocFixtures.WAR_NOT_IN_WAR).toSnapshot(origin))
    }
}
