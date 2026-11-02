package io.github.danilodox.warbook.api

import kotlinx.serialization.Serializable

/** Rotas do servidor, compartilhadas entre o app e o servidor. */
object ApiRoutes {
    const val HEALTH = "/health"
}

/** Resposta de `GET /health`. */
@Serializable
data class HealthResponse(
    val status: String,
    val version: String,
) {
    companion object {
        const val STATUS_OK = "ok"
    }
}
