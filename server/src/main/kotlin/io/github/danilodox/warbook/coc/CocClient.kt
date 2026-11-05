package io.github.danilodox.warbook.coc

import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.errors.ApiException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.IOException

private const val TIMEOUT_MS = 10_000L

/** Erro específico de uma chamada; `null` cai no tratamento comum. Recebe o status e o `reason` da API. */
private typealias SpecificError = (HttpStatusCode, String?) -> ApiException?

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

    internal suspend fun player(tag: String): CocPlayer = send("players", "#$tag").body()

    internal suspend fun clan(tag: String): CocClan = send("clans", "#$tag").body()

    internal suspend fun currentWar(clanTag: String): CocWar =
        send("clans", "#$clanTag", "currentwar", specificError = ::warError).body()

    /**
     * `true` se [token] (gerado no jogo) prova que [tag] é do usuário. O token só vai no corpo da requisição;
     * a resposta, que o repete, é descartada sem passar por log nem por mensagem de exceção.
     */
    internal suspend fun verifyToken(tag: String, token: String): Boolean {
        val response = send("players", "#$tag", "verifytoken", specificError = verifyError) {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(CocVerifyTokenRequest(token))
        }
        // Sem a causa: a mensagem de erro de parse pode conter o corpo, que tem o token.
        val status = runCatching { response.body<CocVerifyTokenResponse>().status }.getOrNull()
            ?: throw ApiException(ApiErrorCode.UPSTREAM_ERROR, "Resposta inesperada da API do jogo")
        return status == CocVerifyTokenResponse.STATUS_OK
    }

    private suspend fun send(
        vararg segments: String,
        specificError: SpecificError = { _, _ -> null },
        configure: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        val response = try {
            // appendPathSegments codifica o `#` como %23.
            http.request {
                url { appendPathSegments(*segments) }
                bearerAuth(apiKey)
                configure()
            }
        } catch (e: IOException) {
            log.warn("API do jogo não respondeu: {}", e::class.simpleName)
            throw ApiException(ApiErrorCode.UPSTREAM_ERROR, "A API do jogo não respondeu")
        }
        if (response.status == HttpStatusCode.OK) return response
        throw toApiException(response, specificError)
    }

    private suspend fun toApiException(response: HttpResponse, specificError: SpecificError): ApiException {
        val reason = runCatching { response.body<CocClientError>().reason }.getOrNull()
        specificError(response.status, reason)?.let { return it }
        return when (response.status) {
            HttpStatusCode.NotFound -> ApiException(ApiErrorCode.NOT_FOUND, "Não encontrado")
            HttpStatusCode.ServiceUnavailable -> ApiException(ApiErrorCode.API_MAINTENANCE, "API do jogo em manutenção")
            HttpStatusCode.TooManyRequests -> ApiException(ApiErrorCode.RATE_LIMITED, "Limite da API do jogo atingido")
            else -> {
                // `reason` é um código fixo da API (ex.: accessDenied.invalidIp), sem dados sensíveis.
                log.warn("API do jogo recusou: status={} reason={}", response.status.value, reason)
                ApiException(ApiErrorCode.UPSTREAM_ERROR, "Falha na API do jogo")
            }
        }
    }

    private fun warError(status: HttpStatusCode, reason: String?): ApiException? =
        if (status == HttpStatusCode.Forbidden && reason == REASON_ACCESS_DENIED) {
            ApiException(ApiErrorCode.WAR_LOG_PRIVATE, "Registro de guerra privado")
        } else {
            null
        }

    /** Token fora do formato a API recusa com 400; para o usuário é o mesmo que token inválido. */
    private val verifyError: SpecificError = { status, _ ->
        if (status == HttpStatusCode.BadRequest) {
            ApiException(ApiErrorCode.INVALID_VERIFY_TOKEN, "Token inválido ou vencido")
        } else {
            null
        }
    }

    override fun close() = http.close()

    private companion object {
        const val REASON_ACCESS_DENIED = "accessDenied"
    }
}
