package io.github.danilodox.warbook.api

import kotlinx.serialization.Serializable

/** [OFF]: nunca foi ligado. [PAUSED]: 30 dias sem um membro verificado abrir o app; o histórico continua. */
@Serializable
enum class MonitoringState { OFF, ACTIVE, PAUSED }

/**
 * Estado do monitoramento do clã (`GET` e resposta do `POST /v1/clans/{tag}/monitoring`). Horários em
 * epoch ms. [pausesAt]: quando pausa se nenhum membro verificado abrir o app até lá.
 */
@Serializable
data class MonitoringDto(
    val state: MonitoringState,
    val activatedAt: Long? = null,
    val lastSeenAt: Long? = null,
    val pausesAt: Long? = null,
    val warlogImported: Boolean = false,
)

/**
 * Corpo do `POST /v1/clans/{tag}/monitoring`: a prova de verificação de um membro do clã ([VerifyResponse]).
 * O mesmo pedido ativa, reativa e conta como "membro verificado abriu o app".
 */
@Serializable
data class MonitoringRequest(val verificationToken: String) {
    override fun toString(): String = "MonitoringRequest(verificationToken=***)"
}
