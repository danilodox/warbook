package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.ApiRoutes
import io.github.danilodox.warbook.api.InstallResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException

/** `POST /v1/installs` respondeu com erro. */
class InstallTokenException(val status: Int) : Exception("POST /v1/installs respondeu $status")

/**
 * Entrega o token da instalação: o salvo ou um novo, pedido ao servidor na primeira abertura.
 * [client] não tem o plugin Auth, para não pedir token ao pedir token.
 */
class InstallTokenProvider(private val client: HttpClient, private val store: InstallTokenStore) {
    private val mutex = Mutex()

    suspend fun current(): String = mutex.withLock { store.get() ?: requestNew() }

    /** O servidor recusou [rejected]; se outra chamada já trocou o token, usa o novo em vez de pedir outro. */
    suspend fun renew(rejected: String?): String = mutex.withLock {
        store.get()?.takeIf { it != rejected } ?: requestNew()
    }

    /** Pede o token na abertura do app. Se falhar (sem rede, servidor fora), a primeira chamada tenta de novo. */
    suspend fun warmUp() {
        try {
            current()
        } catch (_: IOException) {
            // Sem rede agora; a primeira busca tenta de novo.
        } catch (_: InstallTokenException) {
            // Servidor recusou agora; a primeira busca tenta de novo.
        }
    }

    private suspend fun requestNew(): String {
        val response = client.post(ApiRoutes.INSTALLS)
        if (response.status != HttpStatusCode.OK) throw InstallTokenException(response.status.value)
        return response.body<InstallResponse>().token.also { store.save(it) }
    }
}
