package io.github.danilodox.warbook

import com.github.benmanes.caffeine.cache.Ticker
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.HealthResponse
import io.github.danilodox.warbook.coc.CocClient
import io.github.danilodox.warbook.coc.CocService
import io.github.danilodox.warbook.config.InvalidConfigException
import io.github.danilodox.warbook.config.ServerConfig
import io.github.danilodox.warbook.errors.installErrorHandling
import io.github.danilodox.warbook.routes.cocRoutes
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlin.system.exitProcess

private const val DEFAULT_PORT = 8080

fun main() {
    val config = loadConfigOrExit()
    val port = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT
    embeddedServer(Netty, port = port, host = "0.0.0.0") { module(config) }
        .start(wait = true)
}

/** Sem a configuração completa o servidor não sobe: mostra o que falta e sai. */
private fun loadConfigOrExit(): ServerConfig = try {
    ServerConfig.fromEnv()
} catch (e: InvalidConfigException) {
    System.err.println("Configuração inválida: ${e.message}")
    exitProcess(1)
}

/** [cocEngine] e [ticker] são trocados nos testes (MockEngine e relógio falso do cache). */
fun Application.module(
    config: ServerConfig,
    cocEngine: HttpClientEngine = CIO.create(),
    ticker: Ticker = Ticker.systemTicker(),
) {
    val cocClient = CocClient(cocEngine, apiKey = config.cocApiKey, baseUrl = config.cocBaseUrl)
    monitor.subscribe(ApplicationStopped) { cocClient.close() }
    val cocService = CocService(cocClient, ticker)

    install(ContentNegotiation) {
        json()
    }
    installErrorHandling()
    routing {
        get(ApiRoutes.HEALTH) {
            call.respond(HealthResponse(status = HealthResponse.STATUS_OK, version = ServerInfo.version))
        }
        cocRoutes(cocService)
    }
}

/** Versão do servidor, gravada em recurso pelo build (`warbook-version.txt`). */
object ServerInfo {
    val version: String by lazy {
        ServerInfo::class.java.getResource("/warbook-version.txt")?.readText()?.trim() ?: "dev"
    }
}
