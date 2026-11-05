package io.github.danilodox.warbook.routes

import com.auth0.jwt.JWT
import io.github.danilodox.warbook.api.ApiError
import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.VerifyResponse
import io.github.danilodox.warbook.auth.VerificationTokens
import io.github.danilodox.warbook.module
import io.github.danilodox.warbook.testing.CocFixtures
import io.github.danilodox.warbook.testing.FakeCoc
import io.github.danilodox.warbook.testing.clientWith
import io.github.danilodox.warbook.testing.issueToken
import io.github.danilodox.warbook.testing.postVerify
import io.github.danilodox.warbook.testing.testConfig
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VerifyRoutesTest {

    private val fake = FakeCoc()

    private fun ApplicationTestBuilder.start() = application { module(testConfig, fake.engine) }

    private suspend fun HttpResponse.errorCode(): ApiErrorCode =
        Json.decodeFromString(ApiError.serializer(), bodyAsText()).code

    @Test
    fun tokenValidoDevolveJwtDeVerificacao() = testApplication {
        start()
        fake.on(CocFixtures.VERIFY_PATH, body = CocFixtures.verifyOk)
        val installToken = issueToken()

        val response = clientWith(installToken).postVerify(tag = "2pq8lv9yc")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString(VerifyResponse.serializer(), response.bodyAsText())
        val jwt = JWT.decode(body.verificationToken)
        assertEquals(CocFixtures.PLAYER_TAG, jwt.subject, "tag normalizada, sem #")
        assertEquals(listOf(VerificationTokens.AUDIENCE), jwt.audience)
        assertEquals(JWT.decode(installToken).subject, jwt.getClaim(VerificationTokens.CLAIM_INSTALL_ID).asString())
        assertTrue(jwt.issuedAt != null)
        assertEquals(null, jwt.expiresAt, "a prova de verificação não vence")
    }

    @Test
    fun mandaOTokenSoNoCorpoDeUmPost() = testApplication {
        start()
        fake.on(CocFixtures.VERIFY_PATH, body = CocFixtures.verifyOk)

        clientWith(issueToken()).postVerify()

        val request = fake.requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals(CocFixtures.VERIFY_PATH, request.url.encodedPath)
        assertTrue(CocFixtures.gameToken !in request.url.toString())
        assertEquals("""{"token":"${CocFixtures.gameToken}"}""", fake.bodies.single())
    }

    @Test
    fun tokenInvalidoViraInvalidVerifyToken() = testApplication {
        start()
        fake.on(CocFixtures.VERIFY_PATH, body = CocFixtures.verifyInvalid)

        val response = clientWith(issueToken()).postVerify()

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(ApiErrorCode.INVALID_VERIFY_TOKEN, response.errorCode())
    }

    @Test
    fun tokenRecusadoPelaApiComBadRequestViraInvalidVerifyToken() = testApplication {
        start()
        fake.on(CocFixtures.VERIFY_PATH, HttpStatusCode.BadRequest, """{"reason":"badRequest"}""")

        val response = clientWith(issueToken()).postVerify()

        assertEquals(ApiErrorCode.INVALID_VERIFY_TOKEN, response.errorCode())
    }

    @Test
    fun tokenVazioLongoOuCorpoErradoNemChegamNaApi() = testApplication {
        start()
        val api = clientWith(issueToken())

        listOf("""{"token":"  "}""", """{"token":"${"x".repeat(65)}"}""", "nao-e-json", "{}").forEach { body ->
            val response = api.postVerify(body = body)
            assertEquals(HttpStatusCode.BadRequest, response.status, body)
            assertEquals(ApiErrorCode.INVALID_VERIFY_TOKEN, response.errorCode(), body)
        }
        assertTrue(fake.requests.isEmpty())
    }

    @Test
    fun tagInvalidaViraInvalidTag() = testApplication {
        start()

        val response = clientWith(issueToken()).postVerify(tag = "ABC!")

        assertEquals(ApiErrorCode.INVALID_TAG, response.errorCode())
        assertTrue(fake.requests.isEmpty())
    }

    @Test
    fun jogadorInexistenteViraNotFound() = testApplication {
        start()
        fake.on(CocFixtures.VERIFY_PATH, HttpStatusCode.NotFound, """{"reason":"notFound"}""")

        val response = clientWith(issueToken()).postVerify()

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(ApiErrorCode.NOT_FOUND, response.errorCode())
    }

    @Test
    fun verificacaoNaoEntraNoCache() = testApplication {
        start()
        fake.on(CocFixtures.VERIFY_PATH, body = CocFixtures.verifyOk)
        val api = clientWith(issueToken())

        api.postVerify()
        api.postVerify()

        assertEquals(2, fake.requests.size)
    }

    @Test
    fun semTokenDaInstalacaoRespondeUnauthorized() = testApplication {
        start()

        val response = client.postVerify()

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(fake.requests.isEmpty())
    }

    @Test
    fun jwtDeVerificacaoNaoServeComoTokenDaInstalacao() = testApplication {
        start()
        fake.on(CocFixtures.VERIFY_PATH, body = CocFixtures.verifyOk)
        val response = clientWith(issueToken()).postVerify()
        val proof = Json.decodeFromString(VerifyResponse.serializer(), response.bodyAsText()).verificationToken

        val asInstall = clientWith(proof).get(ApiRoutes.player(CocFixtures.PLAYER_TAG))

        assertEquals(HttpStatusCode.Unauthorized, asInstall.status)
    }
}
