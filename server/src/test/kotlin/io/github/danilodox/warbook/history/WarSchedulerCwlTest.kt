package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.coc.CocClient
import io.github.danilodox.warbook.testing.CocFixtures
import io.github.danilodox.warbook.testing.CwlFixtures
import io.github.danilodox.warbook.testing.FakeCoc
import io.github.danilodox.warbook.testing.MutableClock
import io.github.danilodox.warbook.testing.TestDatabase
import io.github.danilodox.warbook.testing.testConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class WarSchedulerCwlTest {

    private val fake = FakeCoc()
    private val client = CocClient(fake.engine, testConfig.cocApiKey, testConfig.cocBaseUrl)
    private val history = TestDatabase.history

    /** Rodada 1 já acabou (03/11 12h); a 2 acaba em 04/11 12h. */
    private val clock = MutableClock(Instant.parse("2026-11-03T13:00:00Z"))
    private val round2End = Instant.parse("2026-11-04T12:00:00Z")
    private val scope = CoroutineScope(Job())
    private val scheduler = WarScheduler(
        client,
        history,
        WarlogImporter(client, history, clock),
        scope,
        clock,
        RequestPacer(Duration.ZERO),
    )

    @BeforeTest
    fun setUp() = runBlocking {
        TestDatabase.clean()
        history.recordVerifiedVisit(CocFixtures.CLAN_TAG, "Picuí Raiz", CocFixtures.PLAYER_TAG, clock.now)
        history.markWarlogImported(CocFixtures.CLAN_TAG, clock.now)
        fake.on(CwlFixtures.GROUP_PATH, body = CwlFixtures.group)
        CwlFixtures.wars.forEach { (tag, body) -> fake.on(CwlFixtures.warPath(tag), body = body) }
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        client.close()
    }

    private fun leagueWarRequests(warTag: String) =
        fake.requests.count { it.url.encodedPath == CwlFixtures.warPath(warTag) }

    @Test
    fun primeiroCicloRecuperaAsRodadasDoCla() = runBlocking {
        scheduler.runCycle()

        val wars = history.recentWars(CocFixtures.CLAN_TAG, 10)
        assertEquals(listOf("#W3", "#W1"), wars.map { it.warTag })
        wars.forEach {
            assertEquals(WarType.CWL, it.type)
            assertEquals("2026-11", it.cwlSeason)
            assertEquals(CocFixtures.CLAN_TAG, it.clan.tag, "nosso clã sempre do lado clan")
            assertEquals(1, it.attacksPerMember)
        }
        assertEquals(listOf("BBB", "BBB"), wars.map { it.opponent.tag })
        assertEquals(WarResult.WIN, wars.last().result, "rodada 1: 20 × 15")
    }

    @Test
    fun devolveQuantasRodadasGravou() = runBlocking {
        assertEquals(2, scheduler.checkCwl(CocFixtures.CLAN_TAG))
    }

    @Test
    fun naoBuscaDeNovoRodadaEncerradaNemGuerraDosOutros() = runBlocking {
        scheduler.checkCwl(CocFixtures.CLAN_TAG)
        scheduler.checkCwl(CocFixtures.CLAN_TAG)

        assertEquals(1, leagueWarRequests("W1"), "encerrada e gravada")
        assertEquals(1, leagueWarRequests("W2"), "guerra de outros")
        assertEquals(1, leagueWarRequests("W4"), "guerra de outros")
        assertEquals(2, leagueWarRequests("W3"), "rodada aberta: olha a cada ciclo")
    }

    @Test
    fun novaTemporadaEsqueceAsGuerrasDosOutros() = runBlocking {
        scheduler.checkCwl(CocFixtures.CLAN_TAG)
        fake.on(CwlFixtures.GROUP_PATH, body = CwlFixtures.group.replace("2026-11", "2026-12"))

        scheduler.checkCwl(CocFixtures.CLAN_TAG)

        assertEquals(2, leagueWarRequests("W2"))
    }

    @Test
    fun rodadaAbertaMarcaAsOlhadasDoFim() = runBlocking {
        scheduler.checkCwl(CocFixtures.CLAN_TAG)

        assertEquals(
            listOf(round2End.minus(4.minutes.toJavaDuration()), round2End.plus(1.minutes.toJavaDuration())),
            scheduler.scheduled.pending(CocFixtures.CLAN_TAG),
        )
    }

    @Test
    fun foraDaCwlNaoBuscaRodadas() = runBlocking {
        // API sem o grupo: responde 404, como fora da CWL.
        val notInCwl = FakeCoc()
        val otherClient = CocClient(notInCwl.engine, testConfig.cocApiKey, testConfig.cocBaseUrl)
        val other = WarScheduler(
            otherClient,
            history,
            WarlogImporter(otherClient, history, clock),
            scope,
            clock,
            RequestPacer(Duration.ZERO),
        )

        assertEquals(0, other.checkCwl(CocFixtures.CLAN_TAG))

        assertTrue(notInCwl.requests.none { it.url.encodedPath.startsWith("/v1/clanwarleagues") })
        otherClient.close()
    }
}
