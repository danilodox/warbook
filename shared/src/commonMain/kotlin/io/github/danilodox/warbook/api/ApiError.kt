package io.github.danilodox.warbook.api

import kotlinx.serialization.Serializable

/** Corpo de toda resposta de erro do servidor. */
@Serializable
data class ApiError(val code: ApiErrorCode, val message: String)

@Serializable
enum class ApiErrorCode {
    /** Tag com caracteres que não existem no jogo. HTTP 400. */
    INVALID_TAG,

    /** Jogador ou clã não existe. HTTP 404. */
    NOT_FOUND,

    /** O clã deixou o registro de guerra privado. HTTP 403. */
    WAR_LOG_PRIVATE,

    /** A API do jogo está em manutenção. HTTP 503. */
    API_MAINTENANCE,

    /** Muitas requisições (no Warbook ou na API do jogo). HTTP 429. */
    RATE_LIMITED,

    /** Token da instalação ausente, inválido ou vencido. HTTP 401. */
    UNAUTHORIZED,

    /** Outra falha da API do jogo (chave, IP, erro interno). HTTP 502. */
    UPSTREAM_ERROR,

    /** Token de verificação do jogo inválido, vencido ou de outra conta. HTTP 400. */
    INVALID_VERIFY_TOKEN,

    /** Nome da busca curto ou longo demais. HTTP 400. */
    INVALID_QUERY,
}

/** Resposta de `POST /v1/installs`: token que o app manda em `Authorization: Bearer`. */
@Serializable
data class InstallResponse(val token: String)
