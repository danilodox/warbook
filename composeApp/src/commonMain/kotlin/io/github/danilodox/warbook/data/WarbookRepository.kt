package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.ApiError
import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.api.TagNormalizer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
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
}

class KtorWarbookRepository(private val client: HttpClient) : WarbookRepository {

    override suspend fun player(rawTag: String) = fetch(rawTag, ApiRoutes::player) { it.body<PlayerDto>() }

    override suspend fun clan(rawTag: String) = fetch(rawTag, ApiRoutes::clan) { it.body<ClanDto>() }

    override suspend fun currentWar(rawClanTag: String) =
        fetch(rawClanTag, ApiRoutes::currentWar) { it.body<CurrentWarDto>() }

    private suspend fun <T> fetch(
        rawTag: String,
        path: (String) -> String,
        decode: suspend (HttpResponse) -> T,
    ): ApiResult<T> {
        val tag = TagNormalizer.normalize(rawTag) ?: return ApiResult.Failure(AppError.INVALID_TAG)
        return try {
            val response = client.get(path(tag))
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
            ApiErrorCode.UNAUTHORIZED, ApiErrorCode.UPSTREAM_ERROR -> AppError.SERVER
            // Corpo fora do formato (proxy, servidor antigo): decide pelo status.
            null -> if (status == HttpStatusCode.TooManyRequests) AppError.RATE_LIMITED else AppError.SERVER
        }
    }
}
