package io.github.danilodox.warbook.auth

import com.auth0.jwt.JWT
import io.github.danilodox.warbook.api.ApiError
import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.module
import io.github.danilodox.warbook.testing.CocFixtures
import io.github.danilodox.warbook.testing.FakeCoc
import io.github.danilodox.warbook.testing.clientWith
import io.github.danilodox.warbook.testing.issueToken
import io.github.danilodox.warbook.testing.testConfig
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

class InstallAuthTest {

    private val fake = FakeCoc().apply { on(CocFixtures.PLAYER_PATH, body = CocFixtures.player) }

    private fun ApplicationTestBuilder.start() = application { module(testConfig, fake.engine) }

    private suspend fun HttpResponse.errorCode(): ApiErrorCode =
        Json.decodeFromString(ApiError.serializer(), bodyAsText()).code

    @Test
    fun installsDevolveTokenQueAbreOsEndpoints() = testApplication {
        start()
        val token = issueToken()

        val response = clientWith(token).get(ApiRoutes.player(CocFixtures.PLAYER_TAG))

        assertEquals(HttpStatusCode.OK, response.status)
        val jwt = JWT.decode(token)
        assertEquals(InstallTokens.ISSUER, jwt.issuer)
        assertTrue(jwt.subject.isNotBlank())
    }

    @Test
    fun cadaInstalacaoGanhaUmIdDiferente() = testApplication {
        start()

        assertNotEquals(JWT.decode(issueToken()).subject, JWT.decode(issueToken()).subject)
    }

    @Test
    fun semTokenRespondeUnauthorizedSemChamarAApi() = testApplication {
        start()

        val response = client.get(ApiRoutes.player(CocFixtures.PLAYER_TAG))

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(ApiErrorCode.UNAUTHORIZED, response.errorCode())
        assertTrue(response.headers[HttpHeaders.WWWAuthenticate]!!.startsWith("Bearer"))
        assertTrue(fake.requests.isEmpty())
    }

    @Test
    fun tokenAssinadoComOutroSegredoERecusado() = testApplication {
        start()
        val forged = InstallTokens("outro".repeat(8)).issue()

        val response = clientWith(forged).get(ApiRoutes.player(CocFixtures.PLAYER_TAG))

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(ApiErrorCode.UNAUTHORIZED, response.errorCode())
    }

    @Test
    fun tokenQualquerERecusado() = testApplication {
        start()

        val response = clientWith("nao-e-um-jwt").get(ApiRoutes.player(CocFixtures.PLAYER_TAG))

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun tokenVencidoERecusado() = testApplication {
        start()
        val issuedLongAgo = Instant.now() - (InstallTokens.VALIDITY + 1.days).toJavaDuration()
        val expired = InstallTokens(testConfig.jwtSecret) { issuedLongAgo }.issue()

        val response = clientWith(expired).get(ApiRoutes.player(CocFixtures.PLAYER_TAG))

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun healthContinuaPublico() = testApplication {
        start()

        assertEquals(HttpStatusCode.OK, client.get(ApiRoutes.HEALTH).status)
    }
}
