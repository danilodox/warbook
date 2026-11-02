package io.github.danilodox.warbook.routes

import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.TagNormalizer
import io.github.danilodox.warbook.coc.CocService
import io.github.danilodox.warbook.errors.ApiException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get

/** Jogador, clã e guerra atual. A tag é validada antes de chamar a API do jogo. */
fun Route.cocRoutes(service: CocService) {
    get(ApiRoutes.PLAYER) { call.respond(service.player(call.tag())) }
    get(ApiRoutes.CLAN) { call.respond(service.clan(call.tag())) }
    get(ApiRoutes.CURRENT_WAR) { call.respond(service.currentWar(call.tag())) }
}

private fun RoutingCall.tag(): String = TagNormalizer.normalize(parameters[ApiRoutes.PARAM_TAG].orEmpty())
    ?: throw ApiException(ApiErrorCode.INVALID_TAG, "Tag inválida")
