package io.github.danilodox.warbook.routes

import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.TagNormalizer
import io.github.danilodox.warbook.api.VerifyRequest
import io.github.danilodox.warbook.api.VerifyResponse
import io.github.danilodox.warbook.auth.InstallPrincipal
import io.github.danilodox.warbook.auth.VerificationTokens
import io.github.danilodox.warbook.coc.CocService
import io.github.danilodox.warbook.errors.ApiException
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post

/** Jogador, clã e guerra atual. A tag é validada antes de chamar a API do jogo. */
fun Route.cocRoutes(service: CocService) {
    get(ApiRoutes.PLAYER) { call.respond(service.player(call.tag())) }
    get(ApiRoutes.CLAN) { call.respond(service.clan(call.tag())) }
    get(ApiRoutes.CURRENT_WAR) { call.respond(service.currentWar(call.tag())) }
}

/** Verificação de conta. O token do jogo só passa adiante para a API; nunca é guardado nem logado. */
fun Route.verifyRoutes(service: CocService, tokens: VerificationTokens) {
    post(ApiRoutes.VERIFY) {
        val tag = call.tag()
        val token = call.verifyToken()
        if (!service.verifyToken(tag, token)) throw invalidVerifyToken()
        val installId = requireNotNull(call.principal<InstallPrincipal>()) { "rota fora do authenticate" }.installId
        call.respond(VerifyResponse(tokens.issue(tag, installId)))
    }
}

private fun RoutingCall.tag(): String = TagNormalizer.normalize(parameters[ApiRoutes.PARAM_TAG].orEmpty())
    ?: throw ApiException(ApiErrorCode.INVALID_TAG, "Tag inválida")

/** Corpo fora do formato, token vazio ou longo demais nem chegam na API do jogo. */
private suspend fun RoutingCall.verifyToken(): String {
    val token = runCatching { receive<VerifyRequest>() }.getOrNull()?.token?.trim()
    if (token.isNullOrEmpty() || token.length > VerifyRequest.MAX_TOKEN_LENGTH) throw invalidVerifyToken()
    return token
}

private fun invalidVerifyToken() = ApiException(ApiErrorCode.INVALID_VERIFY_TOKEN, "Token inválido ou vencido")
