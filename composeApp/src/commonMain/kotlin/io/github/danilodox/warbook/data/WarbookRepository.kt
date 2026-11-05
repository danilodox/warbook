package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.ApiError
import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.api.TagNormalizer
import io.github.danilodox.warbook.api.VerifyRequest
import io.github.danilodox.warbook.api.VerifyResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException

/** Resultado de uma chamada ao servidor: sempre um valor ou um erro tipado, nunca `null`. */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>

    data class Failure(val error: AppError) : ApiResult<Nothing>
}

/** Erros que as telas sabem mostrar. */
enum class AppError {
    INVALID_TAG,
    NOT_FOUND,
    WAR_LOG_PRIVATE,
    API_MAINTENANCE,
    RATE_LIMITED,

    /** Token de verificação do jogo inválido ou vencido. */
    INVALID_VERIFY_TOKEN,

    /** Sem internet ou servidor fora do ar. */
    NETWORK,

    /** Qualquer outra falha do servidor ou da API do jogo. */
    SERVER,
}

interface WarbookRepository {
    /** [rawTag] como o usuário digitou; é normalizada aqui. */
    suspend fun player(rawTag: String): ApiResult<PlayerDto>

    suspend fun clan(rawTag: String): ApiResult<ClanDto>

    suspend fun currentWar(rawClanTag: String): ApiResult<CurrentWarDto>

    /** Troca o token do jogo pela prova de verificação assinada pelo servidor. O token não é guardado. */
    suspend fun verify(rawTag: String, gameToken: String): ApiResult<String>
}

class KtorWarbookRepository(private val client: HttpClient) : WarbookRepository {

    override suspend fun player(rawTag: String) = get(rawTag, ApiRoutes::player) { it.body<PlayerDto>() }

    override suspend fun clan(rawTag: String) = get(rawTag, ApiRoutes::clan) { it.body<ClanDto>() }

    override suspend fun currentWar(rawClanTag: String) =
        get(rawClanTag, ApiRoutes::currentWar) { it.body<CurrentWarDto>() }

    override suspend fun verify(rawTag: String, gameToken: String): ApiResult<String> {
        val token = gameToken.trim()
        if (token.isEmpty()) return ApiResult.Failure(AppError.INVALID_VERIFY_TOKEN)
        return call(rawTag, { it.body<VerifyResponse>().verificationToken }) { tag ->
            client.post(ApiRoutes.verify(tag)) {
                contentType(ContentType.Application.Json)
                setBody(VerifyRequest(token))
            }
        }
    }

    private suspend fun <T> get(rawTag: String, path: (String) -> String, decode: suspend (HttpResponse) -> T) =
        call(rawTag, decode) { tag -> client.get(path(tag)) }

    private suspend fun <T> call(
        rawTag: String,
        decode: suspend (HttpResponse) -> T,
        request: suspend (tag: String) -> HttpResponse,
    ): ApiResult<T> {
        val tag = TagNormalizer.normalize(rawTag) ?: return ApiResult.Failure(AppError.INVALID_TAG)
        return try {
            val response = request(tag)
            if (response.status == HttpStatusCode.OK) {
                ApiResult.Success(decode(response))
            } else {
                ApiResult.Failure(response.toAppError())
            }
        } catch (_: IOException) {
            ApiResult.Failure(AppError.NETWORK)
        } catch (_: ContentConvertException) {
            // Resposta 200 que não bate com o DTO.
            ApiResult.Failure(AppError.SERVER)
        } catch (e: InstallTokenException) {
            val error = if (e.status == HttpStatusCode.TooManyRequests.value) AppError.RATE_LIMITED else AppError.SERVER
            ApiResult.Failure(error)
        }
    }

    private suspend fun HttpResponse.toAppError(): AppError {
        val code = runCatching { warbookJson.decodeFromString(ApiError.serializer(), bodyAsText()).code }.getOrNull()
        return when (code) {
            ApiErrorCode.INVALID_TAG -> AppError.INVALID_TAG
            ApiErrorCode.NOT_FOUND -> AppError.NOT_FOUND
            ApiErrorCode.WAR_LOG_PRIVATE -> AppError.WAR_LOG_PRIVATE
            ApiErrorCode.API_MAINTENANCE -> AppError.API_MAINTENANCE
            ApiErrorCode.RATE_LIMITED -> AppError.RATE_LIMITED
            ApiErrorCode.INVALID_VERIFY_TOKEN -> AppError.INVALID_VERIFY_TOKEN
            ApiErrorCode.UNAUTHORIZED, ApiErrorCode.UPSTREAM_ERROR -> AppError.SERVER
            // Corpo fora do formato (proxy, servidor antigo): decide pelo status.
            null -> if (status == HttpStatusCode.TooManyRequests) AppError.RATE_LIMITED else AppError.SERVER
        }
    }
}
