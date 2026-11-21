package io.github.danilodox.warbook.routes

import io.github.danilodox.warbook.api.ApiError
import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.MonitoringDto
import io.github.danilodox.warbook.api.MonitoringRequest
import io.github.danilodox.warbook.api.MonitoringState
import io.github.danilodox.warbook.auth.VerificationTokens
import io.github.danilodox.warbook.module
import io.github.danilodox.warbook.testing.CocFixtures
import io.github.danilodox.warbook.testing.FakeCoc
import io.github.danilodox.warbook.testing.FakePlayerIndex
import io.github.danilodox.warbook.testing.MutableClock
import io.github.danilodox.warbook.testing.TestDatabase
import io.github.danilodox.warbook.testing.WarlogFixtures
import io.github.danilodox.warbook.testing.authorizedClient
import io.github.danilodox.warbook.testing.issueToken
import io.github.danilodox.warbook.testing.testConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MonitoringRoutesTest {

    private val fake = FakeCoc()
    private val start = Instant.parse("2026-11-20T12:00:00Z")
    private val clock = MutableClock(start)
    private val proofs = VerificationTokens(testConfig.jwtSecret)

    @BeforeTest
    fun clean() = TestDatabase.clean()

    private fun ApplicationTestBuilder.start() = application {
        module(
            testConfig,
            fake.engine,
            playerIndex = FakePlayerIndex(),
            warHistory = TestDatabase.history,
            clock = clock,
        )
    }

    private fun proofFor(tag: String = CocFixtures.PLAYER_TAG) = proofs.issue(tag, installId = "instalacao-1")

    private suspend fun HttpClient.activate(proof: String?): HttpResponse =
        post(ApiRoutes.monitoring(CocFixtures.CLAN_TAG)) {
            contentType(ContentType.Application.Json)
            setBody(proof?.let { Json.encodeToString(MonitoringRequest.serializer(), MonitoringRequest(it)) } ?: "{}")
        }

    private suspend fun HttpClient.status(): MonitoringDto = get(ApiRoutes.monitoring(CocFixtures.CLAN_TAG)).dto()

    private suspend fun HttpResponse.dto(): MonitoringDto =
        Json.decodeFromString(MonitoringDto.serializer(), bodyAsText())

    private suspend fun HttpResponse.errorCode(): ApiErrorCode =
        Json.decodeFromString(ApiError.serializer(), bodyAsText()).code

    @Test
    fun claQueNuncaAtivouEstaDesligado() = testApplication {
        start()

        assertEquals(MonitoringDto(MonitoringState.OFF), authorizedClient().status())
    }

    @Test
    fun membroVerificadoAtiva() = testApplication {
        start()
        fake.on(CocFixtures.CLAN_PATH, body = CocFixtures.clan)
        val client = authorizedClient()

        val response = client.activate(proofFor())

        assertEquals(HttpStatusCode.OK, response.status)
        val expected = MonitoringDto(
            state = MonitoringState.ACTIVE,
            activatedAt = start.toEpochMilli(),
            lastSeenAt = start.toEpochMilli(),
            pausesAt = start.plus(30, ChronoUnit.DAYS).toEpochMilli(),
            warlogImported = false,
        )
        assertEquals(expected, response.dto())
        assertEquals(expected, client.status())
    }

    @Test
    fun primeiraAtivacaoImportaOWarlogEmSegundoPlano() = testApplication {
        start()
        fake.on(CocFixtures.CLAN_PATH, body = CocFixtures.clan)
        fake.on(WarlogFixtures.PATH, body = WarlogFixtures.warLog)
        val client = authorizedClient()

        client.activate(proofFor())
        // A importação não segura a resposta; espera ela terminar (até 5 s).
        withTimeout(5.seconds) {
            while (!client.status().warlogImported) delay(20.milliseconds)
        }
        client.activate(proofFor())

        assertEquals(3, TestDatabase.history.recentWars(CocFixtures.CLAN_TAG, 10).size)
        assertEquals(1, fake.requests.count { it.url.encodedPath == WarlogFixtures.PATH }, "só na primeira vez")
    }

    @Test
    fun semProvaNaoAtiva() = testApplication {
        start()
        fake.on(CocFixtures.CLAN_PATH, body = CocFixtures.clan)

        val response = authorizedClient().activate(proof = null)

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(ApiErrorCode.NOT_VERIFIED, response.errorCode())
    }

    @Test
    fun provaAssinadaComOutroSegredoNaoServe() = testApplication {
        start()
        fake.on(CocFixtures.CLAN_PATH, body = CocFixtures.clan)
        val forged = VerificationTokens("outro".repeat(8)).issue(CocFixtures.PLAYER_TAG, "x")

        val response = authorizedClient().activate(forged)

        assertEquals(ApiErrorCode.NOT_VERIFIED, response.errorCode())
    }

    @Test
    fun tokenDaInstalacaoNaoServeComoProva() = testApplication {
        start()
        fake.on(CocFixtures.CLAN_PATH, body = CocFixtures.clan)
        val installToken = issueToken()

        val response = authorizedClient().activate(installToken)

        assertEquals(ApiErrorCode.NOT_VERIFIED, response.errorCode())
    }

    @Test
    fun contaDeForaDoClaNaoAtiva() = testApplication {
        start()
        fake.on(CocFixtures.CLAN_PATH, body = CocFixtures.clan)
        val client = authorizedClient()

        val response = client.activate(proofFor("Q0GLJRPC9"))

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(ApiErrorCode.NOT_CLAN_MEMBER, response.errorCode())
        assertEquals(MonitoringState.OFF, client.status().state)
    }

    @Test
    fun registroPrivadoNaoAtiva() = testApplication {
        start()
        val private = CocFixtures.clan.replace("\"isWarLogPublic\": true", "\"isWarLogPublic\": false")
        fake.on(CocFixtures.CLAN_PATH, body = private)
        val client = authorizedClient()

        val response = client.activate(proofFor())

        assertEquals(ApiErrorCode.WAR_LOG_PRIVATE, response.errorCode())
        assertEquals(MonitoringState.OFF, client.status().state)
    }

    @Test
    fun pausaDepoisDeTrintaDiasEMembroReativa() = testApplication {
        start()
        fake.on(CocFixtures.CLAN_PATH, body = CocFixtures.clan)
        val client = authorizedClient()
        client.activate(proofFor())

        clock.now = start.plus(30, ChronoUnit.DAYS)
        assertEquals(MonitoringState.PAUSED, client.status().state)

        val reactivated = client.activate(proofFor()).dto()

        assertEquals(MonitoringState.ACTIVE, reactivated.state)
        assertEquals(start.toEpochMilli(), reactivated.activatedAt, "a data da primeira ativação fica")
        assertEquals(clock.now.toEpochMilli(), reactivated.lastSeenAt)
    }

    @Test
    fun membroAbrindoOAppAdiaAPausa() = testApplication {
        start()
        fake.on(CocFixtures.CLAN_PATH, body = CocFixtures.clan)
        val client = authorizedClient()
        client.activate(proofFor())

        clock.now = start.plus(20, ChronoUnit.DAYS)
        client.activate(proofFor())
        clock.now = start.plus(40, ChronoUnit.DAYS)

        assertEquals(MonitoringState.ACTIVE, client.status().state)
    }

    @Test
    fun semTokenDaInstalacaoRecusa() = testApplication {
        start()

        val response = client.get(ApiRoutes.monitoring(CocFixtures.CLAN_TAG))

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertFalse(fake.requests.any())
    }
}
