package io.github.danilodox.warbook.testing

import com.github.benmanes.caffeine.cache.Ticker
import io.github.danilodox.warbook.config.ServerConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.time.Duration

val testConfig = ServerConfig(
    cocApiKey = "chave-de-teste-coc",
    // Montado com repeat para não parecer chave real ao Gitleaks.
    jwtSecret = "teste".repeat(8),
    cocBaseUrl = "https://api.clashofclans.com/v1",
)

/** API do jogo simulada: responde pelo caminho codificado (ex.: `/v1/players/%232PQ8LV9YC`). */
class FakeCoc {
    val requests = mutableListOf<HttpRequestData>()

    /** Corpo de cada requisição, na mesma ordem de [requests]. */
    val bodies = mutableListOf<String>()
    private val routes = mutableMapOf<String, Pair<HttpStatusCode, String>>()

    fun on(encodedPath: String, status: HttpStatusCode = HttpStatusCode.OK, body: String) {
        routes[encodedPath] = status to body
    }

    val engine = MockEngine { request ->
        requests += request
        bodies += request.body.toByteArray().decodeToString()
        val (status, body) = routes[request.url.encodedPath] ?: (HttpStatusCode.NotFound to """{"reason":"notFound"}""")
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }
}

/** Relógio do cache controlado pelo teste. */
class FakeTicker : Ticker {
    private var nanos = 0L

    fun advance(duration: Duration) {
        nanos += duration.inWholeNanoseconds
    }

    override fun read(): Long = nanos
}
