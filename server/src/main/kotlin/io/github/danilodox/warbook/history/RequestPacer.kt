package io.github.danilodox.warbook.history

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Limite próprio do agendador: uma chamada à API do jogo por vez, com [interval] entre o início de uma e da
 * próxima. Os pedidos do app não passam por aqui, então o agendador nunca disputa fila com eles; só gasta
 * no máximo 1/[interval] da cota da chave.
 */
class RequestPacer(private val interval: Duration = DEFAULT_INTERVAL) {
    private val mutex = Mutex()
    private var last: TimeSource.Monotonic.ValueTimeMark? = null

    suspend fun <T> paced(block: suspend () -> T): T = mutex.withLock {
        last?.let { delay(interval - it.elapsedNow()) }
        last = TimeSource.Monotonic.markNow()
        block()
    }

    companion object {
        /** 4 chamadas por segundo: pouco perto do limite da chave, que o app divide com o agendador. */
        val DEFAULT_INTERVAL: Duration = 250.milliseconds
    }
}
