package io.github.danilodox.warbook.coc

import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.errors.ApiException
import io.github.danilodox.warbook.testing.testConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** A API do jogo às vezes recusa a conexão num dos IPs; o cliente tenta de novo só nesse caso. */
class CocClientRetryTest {

    private val calls = AtomicInteger()

    private fun client(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): CocClient {
        val engine = MockEngine { request ->
            calls.incrementAndGet()
            handler(request)
        }
        return CocClient(engine, testConfig.cocApiKey, testConfig.cocBaseUrl)
    }

    private fun MockRequestHandleScope.ok() =
        respond("""{"items":[]}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun conexaoRecusadaUmaVezTentaDeNovoEPassa() = runBlocking {
        val client = client { if (calls.get() == 1) throw ConnectException("Connection refused") else ok() }

        assertEquals(emptyList(), client.searchClans("Alliance", limit = 5).items)
        assertEquals(2, calls.get())
    }

    @Test
    fun conexaoSempreRecusadaDesisteDepoisDasNovasTentativas() = runBlocking {
        val client = client { throw ConnectException("Connection refused") }

        val error = assertFailsWith<ApiException> { client.searchClans("Alliance", limit = 5) }

        assertEquals(ApiErrorCode.UPSTREAM_ERROR, error.code)
        assertEquals(1 + CONNECT_RETRIES, calls.get())
    }

    @Test
    fun timeoutNaoTentaDeNovo() = runBlocking {
        val client = client { throw SocketTimeoutException("Read timed out") }

        val error = assertFailsWith<ApiException> { client.searchClans("Alliance", limit = 5) }

        assertEquals(ApiErrorCode.UPSTREAM_ERROR, error.code)
        assertEquals(1, calls.get())
    }

    @Test
    fun erroDoServidorNaoTentaDeNovo() = runBlocking {
        val client = client { respond("""{"reason":"inMaintenance"}""", HttpStatusCode.ServiceUnavailable) }

        val error = assertFailsWith<ApiException> { client.searchClans("Alliance", limit = 5) }

        assertEquals(ApiErrorCode.API_MAINTENANCE, error.code)
        assertEquals(1, calls.get())
    }
}
