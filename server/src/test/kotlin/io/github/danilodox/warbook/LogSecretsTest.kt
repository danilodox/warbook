package io.github.danilodox.warbook

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.read.ListAppender
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.testing.CocFixtures
import io.github.danilodox.warbook.testing.FakeCoc
import io.github.danilodox.warbook.testing.clientWith
import io.github.danilodox.warbook.testing.issueToken
import io.github.danilodox.warbook.testing.postVerify
import io.github.danilodox.warbook.testing.testConfig
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.slf4j.LoggerFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Passa por sucesso, erros da API e token inválido e confere que nenhum log contém segredo. */
class LogSecretsTest {

    private val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
    private val appender = ListAppender<ILoggingEvent>()

    @BeforeTest
    fun captureLogs() {
        appender.start()
        root.addAppender(appender)
    }

    @AfterTest
    fun releaseLogs() {
        root.detachAppender(appender)
    }

    @Test
    fun logsNaoTemChaveSegredoNemToken() = testApplication {
        val fake = FakeCoc().apply {
            on(CocFixtures.PLAYER_PATH, body = CocFixtures.player)
            on(CocFixtures.WAR_PATH, HttpStatusCode.Forbidden, CocFixtures.ERROR_INVALID_IP)
            on(CocFixtures.VERIFY_PATH, body = CocFixtures.verifyOk)
        }
        application { module(testConfig, fake.engine) }
        val token = issueToken()
        val api = clientWith(token)

        api.get(ApiRoutes.player(CocFixtures.PLAYER_TAG))
        api.get(ApiRoutes.currentWar(CocFixtures.CLAN_TAG))
        clientWith("$token-alterado").get(ApiRoutes.player(CocFixtures.PLAYER_TAG))
        val proof = api.postVerify().bodyAsText()
        fake.on(CocFixtures.VERIFY_PATH, HttpStatusCode.BadGateway, CocFixtures.verifyOk)
        api.postVerify()
        fake.on(CocFixtures.VERIFY_PATH, body = "resposta quebrada ${CocFixtures.gameToken}")
        api.postVerify()

        val logs = appender.list.joinToString("\n") { event ->
            event.formattedMessage + event.throwableProxy?.let { "\n" + ThrowableProxyUtil.asString(it) }.orEmpty()
        }
        assertTrue("accessDenied.invalidIp" in logs, "a recusa da API deve aparecer no log")
        assertFalse(testConfig.cocApiKey in logs, "chave da API no log")
        assertFalse(testConfig.jwtSecret in logs, "segredo do JWT no log")
        assertFalse(token in logs, "token da instalação no log")
        assertFalse(CocFixtures.gameToken in logs, "token de verificação do jogo no log")
        assertFalse(proof.substringAfter("\":\"").substringBefore("\"") in logs, "prova de verificação no log")
    }
}
