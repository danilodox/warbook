package io.github.danilodox.warbook

import com.github.benmanes.caffeine.cache.Ticker
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.HealthResponse
import io.github.danilodox.warbook.api.InstallResponse
import io.github.danilodox.warbook.auth.InstallTokens
import io.github.danilodox.warbook.auth.RateLimits
import io.github.danilodox.warbook.auth.VerificationTokens
import io.github.danilodox.warbook.auth.installAuthentication
import io.github.danilodox.warbook.auth.installRateLimits
import io.github.danilodox.warbook.coc.CocClient
import io.github.danilodox.warbook.coc.CocService
import io.github.danilodox.warbook.config.InvalidConfigException
import io.github.danilodox.warbook.config.ServerConfig
import io.github.danilodox.warbook.db.DatabaseUnavailableException
import io.github.danilodox.warbook.db.WarbookDatabase
import io.github.danilodox.warbook.errors.installErrorHandling
import io.github.danilodox.warbook.history.MonitoringService
import io.github.danilodox.warbook.history.WarHistoryRepository
import io.github.danilodox.warbook.history.WarHistoryService
import io.github.danilodox.warbook.history.WarScheduler
import io.github.danilodox.warbook.history.WarlogImporter
import io.github.danilodox.warbook.index.PlayerIndex
import io.github.danilodox.warbook.index.PostgresPlayerIndex
import io.github.danilodox.warbook.routes.cocRoutes
import io.github.danilodox.warbook.routes.historyRoutes
import io.github.danilodox.warbook.routes.monitoringRoutes
import io.github.danilodox.warbook.routes.verifyRoutes
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.time.Clock
import kotlin.system.exitProcess

private const val DEFAULT_PORT = 8080

fun main() {
    val config = loadConfigOrExit()
    val database = connectDatabaseOrExit(config)
    val port = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT
    // Gravações do índice de nicks em segundo plano; uma falha não cancela as outras.
    val indexWrites = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        monitor.subscribe(ApplicationStopped) {
            indexWrites.cancel()
            database.close()
        }
        module(
            config,
            playerIndex = PostgresPlayerIndex(database.exposed, indexWrites),
            warHistory = WarHistoryRepository(database.exposed),
            startWarScheduler = true,
        )
    }.start(wait = true)
}

/** Sem a configuração completa o servidor não sobe: mostra o que falta e sai. */
private fun loadConfigOrExit(): ServerConfig = try {
    ServerConfig.fromEnv()
} catch (e: InvalidConfigException) {
    System.err.println("Configuração inválida: ${e.message}")
    exitProcess(1)
}

/** Sem o Postgres o servidor também não sobe: diz o que conferir e sai. Aplica as migrations pendentes. */
private fun connectDatabaseOrExit(config: ServerConfig): WarbookDatabase = try {
    WarbookDatabase.connect(config.database)
} catch (e: DatabaseUnavailableException) {
    System.err.println("Banco indisponível: ${e.message}")
    exitProcess(1)
}

/**
 * [cocEngine], [ticker] e [limits] são trocados nos testes (MockEngine, relógio do cache, limites baixos).
 * [playerIndex] e [warHistory] não têm padrão: o servidor passa os do Postgres; os testes, um falso ou o
 * embedded-postgres. [clock] é trocado nos testes do monitoramento. [startWarScheduler] só o servidor de
 * verdade liga: nos testes o ciclo de hora em hora chamaria a API falsa sozinho.
 */
// É onde o servidor é montado: cada parâmetro é uma peça que os testes trocam.
@Suppress("LongParameterList")
fun Application.module(
    config: ServerConfig,
    cocEngine: HttpClientEngine = CIO.create(),
    ticker: Ticker = Ticker.systemTicker(),
    limits: RateLimits = RateLimits(),
    playerIndex: PlayerIndex,
    warHistory: WarHistoryRepository,
    clock: Clock = Clock.systemUTC(),
    startWarScheduler: Boolean = false,
) {
    val cocClient = CocClient(cocEngine, apiKey = config.cocApiKey, baseUrl = config.cocBaseUrl)
    monitor.subscribe(ApplicationStopped) { cocClient.close() }
    val cocService = CocService(cocClient, playerIndex, ticker)
    val installTokens = InstallTokens(config.jwtSecret)
    val verificationTokens = VerificationTokens(config.jwtSecret)
    // O agendador roda no escopo da aplicação: para junto com o servidor.
    val scheduler = WarScheduler(cocClient, warHistory, WarlogImporter(cocClient, warHistory, clock), this, clock)
    if (startWarScheduler) scheduler.start()
    val monitoring = MonitoringService(warHistory, cocService, verificationTokens, clock) { clan ->
        scheduler.checkInBackground(clan)
    }

    install(ContentNegotiation) {
        json()
    }
    installErrorHandling()
    installAuthentication(installTokens)
    installRateLimits(limits)
    routing {
        get(ApiRoutes.HEALTH) {
            call.respond(HealthResponse(status = HealthResponse.STATUS_OK, version = ServerInfo.version))
        }
        rateLimit(RateLimits.INSTALLS) {
            post(ApiRoutes.INSTALLS) { call.respond(InstallResponse(installTokens.issue())) }
        }
        rateLimit(RateLimits.PER_IP) {
            authenticate(InstallTokens.AUTH_NAME) {
                rateLimit(RateLimits.PER_INSTALL) {
                    cocRoutes(cocService)
                    monitoringRoutes(monitoring)
                    historyRoutes(WarHistoryService(warHistory))
                    rateLimit(RateLimits.VERIFY_PER_IP) {
                        rateLimit(RateLimits.VERIFY_PER_INSTALL) {
                            verifyRoutes(cocService, verificationTokens)
                        }
                    }
                }
            }
        }
    }
}

/** Versão do servidor, gravada em recurso pelo build (`warbook-version.txt`). */
object ServerInfo {
    val version: String by lazy {
        ServerInfo::class.java.getResource("/warbook-version.txt")?.readText()?.trim() ?: "dev"
    }
}
