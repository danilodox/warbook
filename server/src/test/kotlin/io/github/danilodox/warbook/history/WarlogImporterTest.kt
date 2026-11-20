package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.coc.CocClient
import io.github.danilodox.warbook.errors.ApiException
import io.github.danilodox.warbook.testing.CocFixtures
import io.github.danilodox.warbook.testing.FakeCoc
import io.github.danilodox.warbook.testing.MutableClock
import io.github.danilodox.warbook.testing.TestDatabase
import io.github.danilodox.warbook.testing.WarlogFixtures
import io.github.danilodox.warbook.testing.testConfig
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class WarlogImporterTest {

    private val fake = FakeCoc()
    private val client = CocClient(fake.engine, testConfig.cocApiKey, testConfig.cocBaseUrl)
    private val now = Instant.parse("2026-11-20T12:00:00Z")
    private val history = TestDatabase.history
    private val importer = WarlogImporter(client, history, MutableClock(now))

    @BeforeTest
    fun setUp() = runBlocking {
        TestDatabase.clean()
        history.recordVerifiedVisit(CocFixtures.CLAN_TAG, "Picuí Raiz", CocFixtures.PLAYER_TAG, now)
        Unit
    }

    @AfterTest
    fun tearDown() = client.close()

    @Test
    fun importaAsGuerrasComunsEPulaACwl() = runBlocking {
        fake.on(WarlogFixtures.PATH, body = WarlogFixtures.warLog)

        assertEquals(3, importer.import(CocFixtures.CLAN_TAG))

        val wars = history.recentWars(CocFixtures.CLAN_TAG, 10)
        assertEquals(listOf("OPP1", "OPP2", "OPP3"), wars.map { it.opponent.tag })
        assertEquals(listOf(WarResult.WIN, WarResult.LOSE, WarResult.TIE), wars.map { it.result })
        wars.forEach {
            assertEquals(WarSource.WARLOG, it.source)
            assertEquals(WarType.WAR, it.type)
        }
    }

    @Test
    fun guardaOPlacarDeCadaGuerra() = runBlocking {
        fake.on(WarlogFixtures.PATH, body = WarlogFixtures.warLog)

        importer.import(CocFixtures.CLAN_TAG)

        val win = history.recentWars(CocFixtures.CLAN_TAG, 1).single()
        assertEquals(Instant.parse("2026-11-18T20:00:00Z"), win.endTime)
        assertEquals(15, win.teamSize)
        assertEquals(2, win.attacksPerMember)
        assertEquals(
            WarSide(CocFixtures.CLAN_TAG, "Picuí Raiz", stars = 40, destruction = 91.5, attacks = 28),
            win.clan,
        )
        assertEquals(
            WarSide("OPP1", "Os Mito", "https://img/o1.png", stars = 33, destruction = 80.2, attacks = null),
            win.opponent,
        )
    }

    @Test
    fun pedeOWarlogComLimite() = runBlocking {
        fake.on(WarlogFixtures.PATH, body = WarlogFixtures.warLog)

        importer.import(CocFixtures.CLAN_TAG)

        val url = fake.requests.single().url
        assertEquals(WarlogFixtures.PATH, url.encodedPath)
        assertEquals(WarlogImporter.LIMIT.toString(), url.parameters["limit"])
    }

    @Test
    fun marcaOClaComoImportado() = runBlocking {
        fake.on(WarlogFixtures.PATH, body = WarlogFixtures.warLog)

        importer.import(CocFixtures.CLAN_TAG)

        assertEquals(now, history.monitoredClan(CocFixtures.CLAN_TAG)?.warlogImportedAt)
    }

    @Test
    fun registroPrivadoNaoMarcaComoImportado() = runBlocking {
        fake.on(WarlogFixtures.PATH, HttpStatusCode.Forbidden, """{"reason":"accessDenied"}""")

        val error = assertFailsWith<ApiException> { importer.import(CocFixtures.CLAN_TAG) }

        assertEquals(ApiErrorCode.WAR_LOG_PRIVATE, error.code)
        assertNull(history.monitoredClan(CocFixtures.CLAN_TAG)?.warlogImportedAt)
    }

    @Test
    fun importarDeNovoNaoDuplica() = runBlocking {
        fake.on(WarlogFixtures.PATH, body = WarlogFixtures.warLog)

        importer.import(CocFixtures.CLAN_TAG)
        importer.import(CocFixtures.CLAN_TAG)

        assertEquals(3, history.recentWars(CocFixtures.CLAN_TAG, 10).size)
    }
}
