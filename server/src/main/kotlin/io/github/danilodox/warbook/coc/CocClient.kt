package io.github.danilodox.warbook.coc

import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.errors.ApiException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.IOException

private const val TIMEOUT_MS = 10_000L

/**
 * Cliente da API oficial do Clash of Clans. Recebe tags já normalizadas (sem `#`).
 * Falhas viram [ApiException]. A chave só vai no cabeçalho e nunca é registrada em log.
 */
class CocClient(engine: HttpClientEngine, private val apiKey: String, baseUrl: String) : AutoCloseable {
    private val log = LoggerFactory.getLogger(CocClient::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    private val http = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) { requestTimeoutMillis = TIMEOUT_MS }
        defaultRequest { url(baseUrl.trimEnd('/') + "/") }
    }

    internal suspend fun player(tag: String): CocPlayer = fetch("players", "#$tag").body()

    internal suspend fun clan(tag: String): CocClan = fetch("clans", "#$tag").body()

    internal suspend fun currentWar(clanTag: String): CocWar =
        fetch("clans", "#$clanTag", "currentwar", isWar = true).body()

    private suspend fun fetch(vararg segments: String, isWar: Boolean = false): HttpResponse {
        val response = try {
            // appendPathSegments codifica o `#` como %23.
            http.get {
                url { appendPathSegments(*segments) }
                bearerAuth(apiKey)
            }
        } catch (e: IOException) {
            log.warn("API do jogo não respondeu: {}", e::class.simpleName)
            throw ApiException(ApiErrorCode.UPSTREAM_ERROR, "A API do jogo não respondeu")
        }
        if (response.status == HttpStatusCode.OK) return response
        throw toApiException(response, isWar)
    }

    private suspend fun toApiException(response: HttpResponse, isWar: Boolean): ApiException {
        val reason = runCatching { response.body<CocClientError>().reason }.getOrNull()
        return when (response.status) {
            HttpStatusCode.NotFound -> ApiException(ApiErrorCode.NOT_FOUND, "Não encontrado")
            HttpStatusCode.ServiceUnavailable -> ApiException(ApiErrorCode.API_MAINTENANCE, "API do jogo em manutenção")
            HttpStatusCode.TooManyRequests -> ApiException(ApiErrorCode.RATE_LIMITED, "Limite da API do jogo atingido")
            HttpStatusCode.Forbidden if isWar && reason == REASON_ACCESS_DENIED ->
                ApiException(ApiErrorCode.WAR_LOG_PRIVATE, "Registro de guerra privado")
            else -> {
                // `reason` é um código fixo da API (ex.: accessDenied.invalidIp), sem dados sensíveis.
                log.warn("API do jogo recusou: status={} reason={}", response.status.value, reason)
                ApiException(ApiErrorCode.UPSTREAM_ERROR, "Falha na API do jogo")
            }
        }
    }

    override fun close() = http.close()

    private companion object {
        const val REASON_ACCESS_DENIED = "accessDenied"
    }
}
