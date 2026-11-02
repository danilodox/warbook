package io.github.danilodox.warbook.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val TIMEOUT_MS = 15_000L

/** Servidor novo pode mandar campos que este app não conhece. */
internal val warbookJson = Json { ignoreUnknownKeys = true }

private fun HttpClientConfig<*>.warbookDefaults(baseUrl: String) {
    expectSuccess = false
    install(ContentNegotiation) { json(warbookJson) }
    install(HttpTimeout) { requestTimeoutMillis = TIMEOUT_MS }
    defaultRequest { url(baseUrl) }
}

/** Clients do servidor do Warbook. Os dois usam o mesmo [engine]; só o [api] manda o token. */
class WarbookHttpClients(engine: HttpClientEngine, baseUrl: String, store: InstallTokenStore) {
    private val installs = HttpClient(engine) { warbookDefaults(baseUrl) }

    val tokens = InstallTokenProvider(installs, store)

    val api = HttpClient(engine) {
        warbookDefaults(baseUrl)
        install(Auth) {
            bearer {
                loadTokens { BearerTokens(tokens.current(), null) }
                refreshTokens { BearerTokens(tokens.renew(oldTokens?.accessToken), null) }
                sendWithoutRequest { true }
            }
        }
    }
}
