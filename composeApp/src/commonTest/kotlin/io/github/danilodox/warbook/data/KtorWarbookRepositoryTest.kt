package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.ClanRole
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.api.CwlState
import io.github.danilodox.warbook.api.PlayerDto
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KtorWarbookRepositoryTest {

    private val server = FakeWarbookServer()
    private val store = InMemoryTokenStore()
    private val clients = WarbookHttpClients(server.engine, baseUrl = "http://10.0.2.2:8080", store = store)
    private val repository = KtorWarbookRepository(clients.api)

    private val playerPath = ApiRoutes.player("2PQ8LV9YC")

    @Test
    fun primeiraChamadaPedeTokenESalva() = runTest {
        val result = repository.player("#2pq8lv9yc")

        val player = assertIs<ApiResult.Success<PlayerDto>>(result).value
        assertEquals("Dantas", player.name)
        assertEquals(ClanRole.CO_LEADER, player.role)
        assertEquals(listOf(ApiRoutes.INSTALLS, playerPath), server.paths)
        assertEquals("Bearer token-1", server.requests.last().headers[HttpHeaders.Authorization])
        assertEquals("token-1", store.token)
    }

    @Test
    fun reusaOTokenSalvo() = runTest {
        store.token = "token-salvo"
        server.accepted = "token-salvo"

        assertIs<ApiResult.Success<*>>(repository.player("2PQ8LV9YC"))

        assertEquals(listOf(playerPath), server.paths)
    }

    @Test
    fun tokenRecusadoERenovadoEAChamadaRepetida() = runTest {
        store.token = "token-velho"

        assertIs<ApiResult.Success<*>>(repository.player("2PQ8LV9YC"))

        assertEquals(listOf(playerPath, ApiRoutes.INSTALLS, playerPath), server.paths)
        assertEquals("token-1", store.token)
    }

    @Test
    fun tagInvalidaNemChamaOServidor() = runTest {
        assertEquals(ApiResult.Failure(AppError.INVALID_TAG), repository.player("#ABC!"))
        assertTrue(server.requests.isEmpty())
    }

    @Test
    fun cadaCodigoDoServidorViraUmErroDoApp() = runTest {
        val cases = listOf(
            Triple(HttpStatusCode.NotFound, "NOT_FOUND", AppError.NOT_FOUND),
            Triple(HttpStatusCode.Forbidden, "WAR_LOG_PRIVATE", AppError.WAR_LOG_PRIVATE),
            Triple(HttpStatusCode.ServiceUnavailable, "API_MAINTENANCE", AppError.API_MAINTENANCE),
            Triple(HttpStatusCode.TooManyRequests, "RATE_LIMITED", AppError.RATE_LIMITED),
            Triple(HttpStatusCode.BadRequest, "INVALID_TAG", AppError.INVALID_TAG),
            Triple(HttpStatusCode.BadGateway, "UPSTREAM_ERROR", AppError.SERVER),
            Triple(HttpStatusCode.BadGateway, "CODIGO_NOVO", AppError.SERVER),
        )
        for ((status, code, expected) in cases) {
            server.error(status, code)

            assertEquals(ApiResult.Failure(expected), repository.currentWar("2YQUC8J0P"), code)
        }
    }

    @Test
    fun corpoForaDoFormatoNaoQuebra() = runTest {
        server.status = HttpStatusCode.InternalServerError
        server.body = "<html>erro</html>"

        assertEquals(ApiResult.Failure(AppError.SERVER), repository.clan("2YQUC8J0P"))
    }

    @Test
    fun respostaOkComCorpoInvalidoViraErroDoServidor() = runTest {
        server.body = """{"inesperado":true}"""

        assertEquals(ApiResult.Failure(AppError.SERVER), repository.player("2PQ8LV9YC"))
    }

    @Test
    fun cwlUsaARotaPropriaEForaDaLigaESucesso() = runTest {
        server.body = """{"state":"NOT_IN_CWL"}"""

        val result = repository.currentCwl("#2yquc8j0p")

        assertEquals(ApiResult.Success(CwlDto(CwlState.NOT_IN_CWL)), result)
        assertEquals(ApiRoutes.currentCwl("2YQUC8J0P"), server.paths.last())
    }

    @Test
    fun semRedeViraNetwork() = runTest {
        server.offline = true

        assertEquals(ApiResult.Failure(AppError.NETWORK), repository.player("2PQ8LV9YC"))
    }

    @Test
    fun verificacaoMandaOTokenNoCorpoEDevolveAProva() = runTest {
        server.body = """{"verificationToken":"prova-assinada"}"""

        val result = repository.verify("#2pq8lv9yc", "  tk-do-jogo ")

        assertEquals(ApiResult.Success("prova-assinada"), result)
        val request = server.requests.last()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals(ApiRoutes.verify("2PQ8LV9YC"), request.url.encodedPath)
        assertEquals("""{"token":"tk-do-jogo"}""", request.body.toByteArray().decodeToString())
    }

    @Test
    fun verificacaoComTokenVazioNemChamaOServidor() = runTest {
        assertEquals(ApiResult.Failure(AppError.INVALID_VERIFY_TOKEN), repository.verify("2PQ8LV9YC", "   "))
        assertTrue(server.requests.isEmpty())
    }

    @Test
    fun tokenRecusadoViraInvalidVerifyToken() = runTest {
        server.error(HttpStatusCode.BadRequest, "INVALID_VERIFY_TOKEN")

        assertEquals(ApiResult.Failure(AppError.INVALID_VERIFY_TOKEN), repository.verify("2PQ8LV9YC", "tk"))
    }

    @Test
    fun limiteAoPedirTokenViraRateLimited() = runTest {
        server.installsStatus = HttpStatusCode.TooManyRequests

        assertEquals(ApiResult.Failure(AppError.RATE_LIMITED), repository.player("2PQ8LV9YC"))
    }
}
