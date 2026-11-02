package io.github.danilodox.warbook.testing

import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.InstallResponse
import io.github.danilodox.warbook.api.VerifyRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.json.Json

/** Pede um token em `POST /v1/installs`, como o app faz na primeira abertura. */
suspend fun ApplicationTestBuilder.issueToken(): String =
    Json.decodeFromString(InstallResponse.serializer(), client.post(ApiRoutes.INSTALLS).bodyAsText()).token

/** Client que manda o token em todas as requisições. */
fun ApplicationTestBuilder.clientWith(token: String): HttpClient = createClient {
    defaultRequest { bearerAuth(token) }
}

suspend fun ApplicationTestBuilder.authorizedClient(): HttpClient = clientWith(issueToken())

/** `POST /v1/players/{tag}/verify` com [body] já em JSON (ou fora do formato, nos testes de erro). */
suspend fun HttpClient.postVerify(
    tag: String = CocFixtures.PLAYER_TAG,
    body: String = Json.encodeToString(VerifyRequest.serializer(), VerifyRequest(CocFixtures.gameToken)),
): HttpResponse = post(ApiRoutes.verify(tag)) {
    contentType(ContentType.Application.Json)
    setBody(body)
}
