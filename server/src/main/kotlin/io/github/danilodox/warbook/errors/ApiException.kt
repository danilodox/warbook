package io.github.danilodox.warbook.errors

import io.github.danilodox.warbook.api.ApiError
import io.github.danilodox.warbook.api.ApiErrorCode
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

/** Erro que vira resposta `{ code, message }` com o status HTTP correspondente. */
class ApiException(val code: ApiErrorCode, override val message: String) : RuntimeException(message) {
    val status: HttpStatusCode
        get() = when (code) {
            ApiErrorCode.INVALID_TAG -> HttpStatusCode.BadRequest
            ApiErrorCode.NOT_FOUND -> HttpStatusCode.NotFound
            ApiErrorCode.WAR_LOG_PRIVATE -> HttpStatusCode.Forbidden
            ApiErrorCode.API_MAINTENANCE -> HttpStatusCode.ServiceUnavailable
            ApiErrorCode.RATE_LIMITED -> HttpStatusCode.TooManyRequests
            ApiErrorCode.UNAUTHORIZED -> HttpStatusCode.Unauthorized
            ApiErrorCode.UPSTREAM_ERROR -> HttpStatusCode.BadGateway
        }
}

fun Application.installErrorHandling() {
    install(StatusPages) {
        exception<ApiException> { call, error ->
            call.respond(error.status, ApiError(error.code, error.message))
        }
    }
}
