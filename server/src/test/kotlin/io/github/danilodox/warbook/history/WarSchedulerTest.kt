package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.coc.CocClient
import io.github.danilodox.warbook.testing.CocFixtures
import io.github.danilodox.warbook.testing.FakeCoc
import io.github.danilodox.warbook.testing.MutableClock
import io.github.danilodox.warbook.testing.TestDatabase
import io.github.danilodox.warbook.testing.WarlogFixtures
import io.github.danilodox.warbook.testing.testConfig
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.toJavaDuration

class WarSchedulerTest {

    private val fake = FakeCoc()
    private val client = CocClient(fake.engine, testConfig.cocApiKey, testConfig.cocBaseUrl)
    private val history = TestDatabase.history
    private val end = Instant.parse("2026-11-03T12:00:00Z")
    private val clock = MutableClock(end - 2.hours)
    private val scope = CoroutineScope(Job())
    private val scheduler = WarScheduler(
        client,
        history,
        WarlogImporter(client, history, clock),
        scope,
        clock,
        RequestPacer(Duration.ZERO),
    )

    private operator fun Instant.minus(d: Duration): Instant = minus(d.toJavaDuration())
    private operator fun Instant.plus(d: Duration): Instant = plus(d.toJavaDuration())

    @BeforeTest
    fun setUp() = runBlocking {
        TestDatabase.clean()
        history.recordVerifiedVisit(CocFixtures.CLAN_TAG, "Picuí Raiz", CocFixtures.PLAYER_TAG, clock.now)
        history.markWarlogImported(CocFixtures.CLAN_TAG, clock.now)
        Unit
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        client.close()
    }

    private fun warRequests(clanTag: String = CocFixtures.CLAN_TAG) =
        fake.requests.count { it.url.encodedPath == "/v1/clans/%23$clanTag/currentwar" }

    @Test
    fun olhadasAntesEDepoisDoFim() {
        assertEquals(
            listOf(end - 4.minutes, end + 1.minutes),
            WarScheduler.checkTimes(end, end - 1.days),
        )
        assertEquals(listOf(end + 1.minutes), WarScheduler.checkTimes(end, end - 2.minutes), "−4 já passou")
        assertEquals(emptyList(), WarScheduler.checkTimes(end, end + 2.minutes), "tudo já passou")
    }

    @Test
    fun guerraAbertaGravaORetratoEMarcaAsDuasOlhadas() = runBlocking {
        fake.on(CocFixtures.WAR_PATH, body = CocFixtures.warInWar)

        assertEquals(WarState.IN_WAR, scheduler.checkWar(CocFixtures.CLAN_TAG))

        val saved = history.recentWars(CocFixtures.CLAN_TAG, 1).single()
        assertEquals(WarSource.SNAPSHOT, saved.source)
        assertEquals(end, saved.endTime)
        assertEquals(listOf(end - 4.minutes, end + 1.minutes), scheduler.scheduled.pending(CocFixtures.CLAN_TAG))
    }

    @Test
    fun olharDeNovoNaoMarcaEmDobro() = runBlocking {
        fake.on(CocFixtures.WAR_PATH, body = CocFixtures.warInWar)

        scheduler.checkWar(CocFixtures.CLAN_TAG)
        scheduler.checkWar(CocFixtures.CLAN_TAG)

        assertEquals(2, scheduler.scheduled.pending(CocFixtures.CLAN_TAG).size)
        assertEquals(1, history.recentWars(CocFixtures.CLAN_TAG, 10).size)
    }

    @Test
    fun guerraEncerradaNaoMarcaOlhada() = runBlocking {
        fake.on(CocFixtures.WAR_PATH, body = warEnded)

        assertEquals(WarState.WAR_ENDED, scheduler.checkWar(CocFixtures.CLAN_TAG))

        assertTrue(scheduler.scheduled.pending(CocFixtures.CLAN_TAG).isEmpty())
        assertEquals(WarResult.WIN, history.recentWars(CocFixtures.CLAN_TAG, 1).single().result)
    }

    @Test
    fun foraDeGuerraNaoGravaNada() = runBlocking {
        fake.on(CocFixtures.WAR_PATH, body = CocFixtures.WAR_NOT_IN_WAR)

        scheduler.checkWar(CocFixtures.CLAN_TAG)

        assertTrue(history.recentWars(CocFixtures.CLAN_TAG, 10).isEmpty())
    }

    @Test
    fun olhadaMarcadaGravaOPlacarFinal() = runBlocking {
        // Faltam 4 min + 200 ms para o fim: a olhada de antes do fim dispara em 200 ms.
        clock.now = end - 4.minutes - 200.milliseconds
        fake.on(CocFixtures.WAR_PATH, body = CocFixtures.warInWar)
        scheduler.checkWar(CocFixtures.CLAN_TAG)
        fake.on(CocFixtures.WAR_PATH, body = warEnded)

        withTimeout(5.seconds) {
            while (history.recentWars(CocFixtures.CLAN_TAG, 1).single().state != WarState.WAR_ENDED) {
                delay(20.milliseconds)
            }
        }
        assertEquals(2, warRequests())
    }

    @Test
    fun cicloPulaClaPausado() = runBlocking {
        fake.on(CocFixtures.WAR_PATH, body = CocFixtures.warInWar)
        clock.now += MonitoredClan.PAUSE_AFTER + 1.minutes

        scheduler.runCycle()

        assertEquals(0, warRequests())
    }

    @Test
    fun cicloImportaOWarlogQueFaltou() = runBlocking {
        history.recordVerifiedVisit("OUTRO", "Outro Clã", "P2", clock.now)
        fake.on("/v1/clans/%23OUTRO/warlog", body = WarlogFixtures.warLog)

        scheduler.runCycle()

        assertNotNull(history.monitoredClan("OUTRO")?.warlogImportedAt)
        val warlogCalls = fake.requests.map { it.url.encodedPath }.filter { it.endsWith("/warlog") }
        assertEquals(listOf("/v1/clans/%23OUTRO/warlog"), warlogCalls, "o já importado não pede de novo")
    }

    @Test
    fun falhaNumClaNaoParaOsOutros() = runBlocking {
        history.recordVerifiedVisit("OUTRO", "Outro Clã", "P2", clock.now)
        history.markWarlogImported("OUTRO", clock.now)
        fake.on(CocFixtures.WAR_PATH, HttpStatusCode.Forbidden, CocFixtures.ERROR_ACCESS_DENIED)
        fake.on("/v1/clans/%23OUTRO/currentwar", body = CocFixtures.warInWar.replace(CocFixtures.CLAN_TAG, "OUTRO"))

        scheduler.runCycle()

        assertEquals(1, warRequests())
        assertEquals(1, history.recentWars("OUTRO", 10).size)
    }

    @Test
    fun ativacaoOlhaOClaNaHora() = runBlocking {
        history.recordVerifiedVisit("OUTRO", "Outro Clã", "P2", clock.now)
        fake.on("/v1/clans/%23OUTRO/warlog", body = WarlogFixtures.warLog)
        fake.on("/v1/clans/%23OUTRO/currentwar", body = CocFixtures.warInWar.replace(CocFixtures.CLAN_TAG, "OUTRO"))

        scheduler.checkInBackground(history.monitoredClan("OUTRO")!!)

        withTimeout(5.seconds) {
            while (history.recentWars("OUTRO", 10).none { it.source == WarSource.SNAPSHOT }) delay(20.milliseconds)
        }
        assertNotNull(history.monitoredClan("OUTRO")?.warlogImportedAt, "o warlog vem antes da guerra")
        Unit
    }

    @Test
    fun limitadorEspacaAsChamadas() = runBlocking {
        val pacer = RequestPacer(100.milliseconds)
        val start = TimeSource.Monotonic.markNow()

        repeat(3) { pacer.paced { } }

        assertTrue(start.elapsedNow() >= 200.milliseconds, "3 chamadas = 2 intervalos")
    }

    private companion object {
        val warEnded = CocFixtures.warInWar.replace("\"inWar\"", "\"warEnded\"")
    }
}
