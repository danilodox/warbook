package io.github.danilodox.warbook.routes

import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.TagNormalizer
import io.github.danilodox.warbook.api.WarHistoryDto
import io.github.danilodox.warbook.errors.ApiException
import io.github.danilodox.warbook.history.WarHistoryService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get

/** Histórico guardado do clã: lista de guerras, uma guerra com a nota de cada membro e o histórico do jogador. */
fun Route.historyRoutes(service: WarHistoryService) {
    get(ApiRoutes.WARS) { call.respond(service.wars(call.tag(), call.limit())) }
    get(ApiRoutes.WAR) { call.respond(service.war(call.tag(), call.warId())) }
    get(ApiRoutes.PLAYER_HISTORY) { call.respond(service.player(call.tag(), call.playerTag())) }
    get(ApiRoutes.ACTIVITY) { call.respond(service.activity(call.tag())) }
    get(ApiRoutes.CWL_SEASON) { call.respond(service.cwlSeason(call.tag())) }
    get(ApiRoutes.CWL_PLANNER) { call.respond(service.planner(call.tag())) }
}

/** Sem `?limit=`, o padrão; fora de 1 a [WarHistoryDto.MAX_LIMIT], erro. */
private fun RoutingCall.limit(): Int {
    val raw = request.queryParameters[ApiRoutes.PARAM_LIMIT] ?: return WarHistoryDto.DEFAULT_LIMIT
    return raw.toIntOrNull()?.takeIf { it in 1..WarHistoryDto.MAX_LIMIT }
        ?: throw ApiException(ApiErrorCode.INVALID_QUERY, "limit de 1 a ${WarHistoryDto.MAX_LIMIT}")
}

/** Id que não é número é guerra que não existe. */
private fun RoutingCall.warId(): Long = parameters[ApiRoutes.PARAM_WAR_ID]?.toLongOrNull()
    ?: throw ApiException(ApiErrorCode.NOT_FOUND, "Guerra não encontrada")

private fun RoutingCall.playerTag(): String = TagNormalizer.normalize(parameters[ApiRoutes.PARAM_PLAYER_TAG].orEmpty())
    ?: throw ApiException(ApiErrorCode.INVALID_TAG, "Tag inválida")
