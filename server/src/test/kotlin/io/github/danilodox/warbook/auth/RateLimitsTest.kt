package io.github.danilodox.warbook.auth

import io.github.danilodox.warbook.api.ApiError
import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.module
import io.github.danilodox.warbook.testing.CocFixtures
import io.github.danilodox.warbook.testing.FakeCoc
import io.github.danilodox.warbook.testing.clientWith
import io.github.danilodox.warbook.testing.issueToken
import io.github.danilodox.warbook.testing.postVerify
import io.github.danilodox.warbook.testing.testConfig
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class RateLimitsTest {

    private val fake = FakeCoc().apply { on(CocFixtures.PLAYER_PATH, body = CocFixtures.player) }
    private val path = ApiRoutes.player(CocFixtures.PLAYER_TAG)

    private fun ApplicationTestBuilder.start(limits: RateLimits) =
        application { module(testConfig, fake.engine, limits = limits) }

    @Test
    fun limitePorInstalacaoNaoAtrapalhaOutraInstalacao() = testApplication {
        start(RateLimits(perMinutePerInstall = 2, perMinutePerIp = 100))
        val first = clientWith(issueToken())
        val second = clientWith(issueToken())

        repeat(2) { assertEquals(HttpStatusCode.OK, first.get(path).status) }
        val blocked = first.get(path)

        assertEquals(HttpStatusCode.TooManyRequests, blocked.status)
        assertEquals(ApiErrorCode.RATE_LIMITED, Json.decodeFromString(ApiError.serializer(), blocked.bodyAsText()).code)
        assertEquals(HttpStatusCode.OK, second.get(path).status)
    }

    @Test
    fun limitePorIpSomaTodasAsInstalacoes() = testApplication {
        start(RateLimits(perMinutePerInstall = 100, perMinutePerIp = 3))
        val first = clientWith(issueToken())
        val second = clientWith(issueToken())

        repeat(2) { assertEquals(HttpStatusCode.OK, first.get(path).status) }
        assertEquals(HttpStatusCode.OK, second.get(path).status)

        assertEquals(HttpStatusCode.TooManyRequests, second.get(path).status)
    }

    @Test
    fun verifyTemLimiteProprioMaisApertado() = testApplication {
        fake.on(CocFixtures.VERIFY_PATH, body = CocFixtures.verifyInvalid)
        start(RateLimits(verifyPerHourPerInstall = 2, verifyPerHourPerIp = 100))
        val api = clientWith(issueToken())

        repeat(2) { assertEquals(HttpStatusCode.BadRequest, api.postVerify().status) }

        assertEquals(HttpStatusCode.TooManyRequests, api.postVerify().status)
        assertEquals(HttpStatusCode.OK, api.get(path).status, "o resto da API continua liberado")
        assertEquals(HttpStatusCode.BadRequest, clientWith(issueToken()).postVerify().status, "outra instalação")
    }

    @Test
    fun verifyPorIpSomaTodasAsInstalacoes() = testApplication {
        fake.on(CocFixtures.VERIFY_PATH, body = CocFixtures.verifyInvalid)
        start(RateLimits(verifyPerHourPerInstall = 100, verifyPerHourPerIp = 2))

        repeat(2) { clientWith(issueToken()).postVerify() }

        assertEquals(HttpStatusCode.TooManyRequests, clientWith(issueToken()).postVerify().status)
    }

    @Test
    fun limiteDeNovasInstalacoesPorIp() = testApplication {
        start(RateLimits(installsPerHourPerIp = 2))

        repeat(2) { assertEquals(HttpStatusCode.OK, client.post(ApiRoutes.INSTALLS).status) }

        assertEquals(HttpStatusCode.TooManyRequests, client.post(ApiRoutes.INSTALLS).status)
    }
}
