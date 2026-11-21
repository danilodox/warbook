package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.testing.TestDatabase
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WarHistoryRepositoryTest {

    private val repo = WarHistoryRepository(TestDatabase.database)
    private val now = Instant.parse("2026-11-20T12:00:00Z")
    private val end = Instant.parse("2026-11-21T18:00:00Z")

    @BeforeTest
    fun clean() = TestDatabase.clean()

    private fun activate(clanTag: String = CLAN, at: Instant = now) =
        runBlocking { repo.recordVerifiedVisit(clanTag, "Alliance", "P1", at) }

    private fun snapshot(
        state: WarState = WarState.IN_WAR,
        endTime: Instant = end,
        attacks: List<WarRecordAttack> = listOf(attack(1)),
        clanStars: Int = attacks.sumOf { it.stars },
        warTag: String? = null,
    ) = WarRecord(
        type = if (warTag == null) WarType.WAR else WarType.CWL,
        source = WarSource.SNAPSHOT,
        state = state,
        teamSize = 2,
        attacksPerMember = 2,
        endTime = endTime,
        startTime = endTime.minus(1, ChronoUnit.DAYS),
        clan = WarSide(CLAN, "Alliance", stars = clanStars, destruction = 50.0, attacks = attacks.size),
        opponent = WarSide("OPP", "Inimigo", "https://badge", stars = 1, destruction = 20.0, attacks = 1),
        capturedAt = now,
        warTag = warTag,
        result = if (state == WarState.WAR_ENDED) WarResult.WIN else null,
        members = listOf(
            WarRecordMember("P1", isOpponent = false, name = "Danilo", townHall = 16, mapPosition = 1),
            WarRecordMember("P2", isOpponent = false, name = "Ana", townHall = 15, mapPosition = 2),
            WarRecordMember("E1", isOpponent = true, name = "Eles 1", townHall = 16, mapPosition = 1),
            WarRecordMember("E2", isOpponent = true, name = "Eles 2", townHall = 14, mapPosition = 2),
        ),
        attacks = attacks,
    )

    private fun attack(order: Int, attacker: String = "P1", defender: String = "E1", stars: Int = 2) =
        WarRecordAttack(order, attacker, defender, stars, destruction = 80, duration = 170)

    private fun warlog(endTime: Instant = end, stars: Int = 5, result: WarResult = WarResult.WIN) = WarRecord(
        type = WarType.WAR,
        source = WarSource.WARLOG,
        state = WarState.WAR_ENDED,
        teamSize = 2,
        attacksPerMember = 2,
        endTime = endTime,
        clan = WarSide(CLAN, "Alliance", stars = stars, destruction = 90.0, attacks = 4),
        opponent = WarSide("OPP", "Inimigo", stars = 2, destruction = 40.0),
        capturedAt = now,
        result = result,
    )

    @Test
    fun primeiraVisitaAtivaERegistraQuemLigou() {
        val visit = activate()

        assertTrue(visit.newlyActivated)
        assertEquals("P1", visit.clan.activatedBy)
        assertEquals(now, visit.clan.activatedAt)
        assertEquals(now, visit.clan.lastSeenAt)
        assertNull(visit.clan.warlogImportedAt)
    }

    @Test
    fun visitaSeguinteSoRenovaOVistoEm() = runBlocking {
        activate()
        val later = now.plus(3, ChronoUnit.DAYS)

        val visit = repo.recordVerifiedVisit(CLAN, "Alliance BR", "P2", later)

        assertFalse(visit.newlyActivated)
        assertEquals("P1", visit.clan.activatedBy)
        assertEquals(now, visit.clan.activatedAt)
        assertEquals(later, visit.clan.lastSeenAt)
        assertEquals("Alliance BR", visit.clan.clanName)
    }

    @Test
    fun pausaDepoisDeTrintaDiasSemMembroVerificado() = runBlocking {
        val clan = activate().clan

        assertTrue(clan.isActive(now.plus(29, ChronoUnit.DAYS)))
        assertFalse(clan.isActive(now.plus(30, ChronoUnit.DAYS)))
    }

    @Test
    fun agendadorSoVeClasNaoPausados() = runBlocking {
        activate("A", at = now)
        activate("B", at = now.minus(31, ChronoUnit.DAYS))

        assertEquals(listOf("A"), repo.activeClans(now).map { it.clanTag })
    }

    @Test
    fun visitaTiraDaPausa() = runBlocking {
        activate(at = now.minus(40, ChronoUnit.DAYS))
        assertTrue(repo.activeClans(now).isEmpty())

        repo.recordVerifiedVisit(CLAN, "Alliance", "P9", now)

        assertEquals(listOf(CLAN), repo.activeClans(now).map { it.clanTag })
    }

    @Test
    fun marcaOWarlogImportado() = runBlocking {
        activate()

        repo.markWarlogImported(CLAN, now)

        assertEquals(now, repo.monitoredClan(CLAN)?.warlogImportedAt)
    }

    @Test
    fun clanQueNuncaAtivouNaoExiste() = runBlocking {
        assertNull(repo.monitoredClan("NADA"))
    }

    @Test
    fun mesmoRetratoDuasVezesNaoDuplica() = runBlocking {
        activate()

        val first = repo.saveSnapshot(CLAN, snapshot())
        val second = repo.saveSnapshot(CLAN, snapshot())

        assertEquals(first, second)
        assertEquals(1, repo.recentWars(CLAN, 10).size)
        val war = assertNotNull(repo.war(CLAN, first))
        assertEquals(4, war.members.size)
        assertEquals(1, war.attacks.size)
    }

    @Test
    fun retratoSeguinteSomaOsAtaquesNovos() = runBlocking {
        activate()
        repo.saveSnapshot(CLAN, snapshot(attacks = listOf(attack(1))))

        val id = repo.saveSnapshot(CLAN, snapshot(attacks = listOf(attack(1), attack(2, "P2", "E2", 3))))

        val war = assertNotNull(repo.war(CLAN, id))
        assertEquals(listOf(1, 2), war.attacks.map { it.order })
        assertEquals(5, war.clan.stars)
    }

    @Test
    fun guardaTudoDoRetrato() = runBlocking {
        activate()
        val id = repo.saveSnapshot(CLAN, snapshot(state = WarState.WAR_ENDED))

        val war = assertNotNull(repo.war(CLAN, id))

        assertEquals(WarType.WAR, war.type)
        assertEquals(WarSource.SNAPSHOT, war.source)
        assertEquals(WarState.WAR_ENDED, war.state)
        assertEquals(WarResult.WIN, war.result)
        assertEquals(end, war.endTime)
        assertEquals(end.minus(1, ChronoUnit.DAYS), war.startTime)
        assertEquals("OPP", war.opponent.tag)
        assertEquals("https://badge", war.opponent.badgeUrl)
        // Nossos primeiro, cada lado pela posição no mapa.
        assertEquals(listOf("P1", "P2", "E1", "E2"), war.members.map { it.tag })
        assertEquals(attack(1), war.attacks.single())
    }

    @Test
    fun retratoEncerradoNaoETrocadoPorUmMaisAntigo() = runBlocking {
        activate()
        val ended = snapshot(state = WarState.WAR_ENDED, attacks = listOf(attack(1), attack(2, "P2", "E2", 3)))
        val id = repo.saveSnapshot(CLAN, ended)

        repo.saveSnapshot(CLAN, snapshot(state = WarState.IN_WAR, attacks = listOf(attack(1))))

        val war = assertNotNull(repo.war(CLAN, id))
        assertEquals(WarState.WAR_ENDED, war.state)
        assertEquals(5, war.clan.stars)
        assertEquals(2, war.attacks.size)
    }

    @Test
    fun warlogEntraSoComOResultado() = runBlocking {
        activate()

        repo.saveWarlog(CLAN, listOf(warlog()))

        val war = repo.recentWars(CLAN, 10).single()
        assertEquals(WarSource.WARLOG, war.source)
        assertEquals(WarResult.WIN, war.result)
        assertNull(war.opponent.attacks)
        val detail = assertNotNull(repo.war(CLAN, assertNotNull(war.id)))
        assertTrue(detail.members.isEmpty())
        assertTrue(detail.attacks.isEmpty())
    }

    @Test
    fun warlogImportadoDuasVezesNaoDuplica() = runBlocking {
        activate()

        repo.saveWarlog(CLAN, listOf(warlog(), warlog(end.minus(2, ChronoUnit.DAYS))))
        repo.saveWarlog(CLAN, listOf(warlog(), warlog(end.minus(2, ChronoUnit.DAYS))))

        assertEquals(2, repo.recentWars(CLAN, 10).size)
    }

    @Test
    fun warlogNaoMexeNoRetratoEncerrado() = runBlocking {
        activate()
        val id = repo.saveSnapshot(CLAN, snapshot(state = WarState.WAR_ENDED))

        repo.saveWarlog(CLAN, listOf(warlog(stars = 6, result = WarResult.LOSE)))

        val war = assertNotNull(repo.war(CLAN, id))
        assertEquals(WarSource.SNAPSHOT, war.source)
        assertEquals(2, war.clan.stars)
        assertEquals(WarResult.WIN, war.result)
    }

    @Test
    fun warlogCompletaOPlacarDoRetratoDeAntesDoFim() = runBlocking {
        activate()
        val id = repo.saveSnapshot(CLAN, snapshot(state = WarState.IN_WAR))

        repo.saveWarlog(CLAN, listOf(warlog(stars = 6, result = WarResult.LOSE)))

        val war = assertNotNull(repo.war(CLAN, id))
        // Continua sendo retrato (com escalação e ataques), mas com o placar final do warlog.
        assertEquals(WarSource.SNAPSHOT, war.source)
        assertEquals(6, war.clan.stars)
        assertEquals(WarResult.LOSE, war.result)
        assertEquals(1, war.attacks.size)
    }

    @Test
    fun retratoSobrescreveOWarlogDaMesmaGuerra() = runBlocking {
        activate()
        repo.saveWarlog(CLAN, listOf(warlog()))

        val id = repo.saveSnapshot(CLAN, snapshot(state = WarState.WAR_ENDED))

        val war = assertNotNull(repo.war(CLAN, id))
        assertEquals(WarSource.SNAPSHOT, war.source)
        assertEquals(4, war.members.size)
        assertEquals(1, repo.recentWars(CLAN, 10).size)
    }

    @Test
    fun warlogComOFimUmSegundoDiferenteEAMesmaGuerra() = runBlocking {
        // Caso real: o warlog deu 15:14:50 e a currentwar 15:14:49 para a mesma guerra.
        activate()
        repo.saveWarlog(CLAN, listOf(warlog(end.plusSeconds(1))))

        val id = repo.saveSnapshot(CLAN, snapshot(state = WarState.WAR_ENDED))

        val wars = repo.recentWars(CLAN, 10)
        assertEquals(1, wars.size)
        assertEquals(WarSource.SNAPSHOT, wars.single().source)
        assertEquals(end, wars.single().endTime, "fica o fim do retrato")
        assertEquals(id, wars.single().id)
    }

    @Test
    fun warlogDepoisDoRetratoComOFimDiferenteNaoDuplica() = runBlocking {
        activate()
        repo.saveSnapshot(CLAN, snapshot(state = WarState.WAR_ENDED))

        repo.saveWarlog(CLAN, listOf(warlog(end.minusSeconds(1)), warlog(end.minus(2, ChronoUnit.DAYS))))

        val wars = repo.recentWars(CLAN, 10)
        assertEquals(2, wars.size, "a guerra de 2 dias antes é outra")
        assertEquals(WarSource.SNAPSHOT, wars.first().source)
    }

    @Test
    fun guerrasDaMaisRecenteParaAMaisAntigaComLimite() = runBlocking {
        activate()
        val ends = (1..5).map { end.minus(it.toLong(), ChronoUnit.DAYS) }
        repo.saveWarlog(CLAN, ends.shuffled().map { warlog(it) })

        assertEquals(ends.take(3), repo.recentWars(CLAN, 3).map { it.endTime })
    }

    @Test
    fun guerraDeOutroClaNaoAparece() = runBlocking {
        activate("A")
        activate("B")
        val id = repo.saveSnapshot("A", snapshot())

        assertNull(repo.war("B", id))
        assertTrue(repo.recentWars("B", 10).isEmpty())
    }

    @Test
    fun warTagsDaCwlJaEncerradas() = runBlocking {
        activate()
        repo.saveSnapshot(CLAN, snapshot(state = WarState.WAR_ENDED, warTag = "#W1", endTime = end))
        repo.saveSnapshot(CLAN, snapshot(state = WarState.IN_WAR, warTag = "#W2", endTime = end.plusSeconds(DAY)))

        assertEquals(setOf("#W1"), repo.endedWarTags(CLAN))
    }

    private companion object {
        const val CLAN = "2Y0"
        const val DAY = 86_400L
    }
}
