package io.github.danilodox.warbook.api

import kotlinx.serialization.Serializable

/** Corpo de `POST /v1/players/{tag}/verify`: o token da API que o jogo mostra nas configurações. Nunca é guardado. */
@Serializable
data class VerifyRequest(val token: String) {
    override fun toString(): String = "VerifyRequest(token=***)"

    companion object {
        /** Os tokens do jogo são curtos; acima disso nem chega na API. */
        const val MAX_TOKEN_LENGTH = 64
    }
}

/**
 * Prova de que a conta é do usuário: JWT assinado pelo servidor (tag, data, instalação), sem validade.
 * O app guarda com a conta; na v0.8 é o que autoriza ativar o monitoramento do clã.
 */
@Serializable
data class VerifyResponse(val verificationToken: String)
