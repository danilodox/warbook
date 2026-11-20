package io.github.danilodox.warbook.routes

import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.MonitoringRequest
import io.github.danilodox.warbook.errors.ApiException
import io.github.danilodox.warbook.history.MonitoringService
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

/** Estado do monitoramento (qualquer um vê) e ativação/renovação (só membro verificado). */
fun Route.monitoringRoutes(service: MonitoringService) {
    get(ApiRoutes.MONITORING) { call.respond(service.status(call.tag())) }
    post(ApiRoutes.MONITORING) {
        val tag = call.tag()
        val proof = runCatching { call.receive<MonitoringRequest>() }.getOrNull()?.verificationToken
            ?.takeIf { it.isNotBlank() }
            ?: throw ApiException(ApiErrorCode.NOT_VERIFIED, "Prova de verificação ausente")
        call.respond(service.recordVisit(tag, proof))
    }
}
