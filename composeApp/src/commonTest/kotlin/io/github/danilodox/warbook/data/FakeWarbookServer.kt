package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.ApiRoutes
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.io.IOException

class InMemoryTokenStore(var token: String? = null) : InstallTokenStore {
    override suspend fun get(): String? = token

    override suspend fun save(token: String) {
        this.token = token
    }
}

/** Servidor do Warbook simulado: emite `token-1`, `token-2`... e só aceita o último emitido (ou [accepted]). */
class FakeWarbookServer {
    val requests = mutableListOf<HttpRequestData>()
    var accepted: String? = null
    var installsStatus = HttpStatusCode.OK
    var offline = false
    var status = HttpStatusCode.OK
    var body = PLAYER_JSON
    private var issued = 0

    val paths: List<String> get() = requests.map { it.url.encodedPath }

    val engine = MockEngine { request ->
        if (offline) throw IOException("sem rede")
        requests += request
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
        when {
            request.url.encodedPath == ApiRoutes.INSTALLS && installsStatus != HttpStatusCode.OK ->
                respond("", installsStatus)
            request.url.encodedPath == ApiRoutes.INSTALLS -> {
                issued++
                respond("""{"token":"token-$issued"}""", HttpStatusCode.OK, jsonHeaders)
            }
            request.headers[HttpHeaders.Authorization] != "Bearer ${accepted ?: "token-$issued"}" -> respond(
                """{"code":"UNAUTHORIZED","message":"Token inválido"}""",
                HttpStatusCode.Unauthorized,
                headersOf(
                    HttpHeaders.ContentType to listOf("application/json"),
                    HttpHeaders.WWWAuthenticate to listOf("Bearer realm=\"warbook\""),
                ),
            )
            else -> respond(body, status, jsonHeaders)
        }
    }

    fun error(status: HttpStatusCode, code: String) {
        this.status = status
        body = """{"code":"$code","message":"erro de teste","extra":true}"""
    }

    companion object {
        const val PLAYER_JSON = """
            {"tag":"2PQ8LV9YC","name":"Dantas","townHallLevel":15,"expLevel":212,"trophies":4812,
             "warStars":1384,"attackWins":47,"donations":1240,"donationsReceived":860,
             "clan":{"tag":"2YQUC8J0P","name":"Picuí Raiz"},"role":"CO_LEADER","campoNovo":1}
        """
    }
}
