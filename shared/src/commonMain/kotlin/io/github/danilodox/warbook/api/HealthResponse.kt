package io.github.danilodox.warbook.api

import kotlinx.serialization.Serializable

/** Resposta de `GET /health`. */
@Serializable
data class HealthResponse(val status: String, val version: String) {
    companion object {
        const val STATUS_OK = "ok"
    }
}
