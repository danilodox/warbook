package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.coc.CocClient
import io.github.danilodox.warbook.coc.SnapshotOrigin
import io.github.danilodox.warbook.coc.toSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

/**
 * Guarda as guerras dos clãs monitorados. A cada [CYCLE] olha a guerra atual de cada clã ativo e grava o
 * retrato. Com guerra aberta, marca mais duas olhadas: [BEFORE_END] antes do fim (último retrato com a
 * guerra rolando) e [AFTER_END] depois (o placar final). O primeiro ciclo roda ao subir o servidor, o que
 * recupera a guerra que estiver aberta. Todas as chamadas à API do jogo passam pelo [pacer].
 */
class WarScheduler(
    private val client: CocClient,
    private val history: WarHistoryRepository,
    private val warlogImporter: WarlogImporter,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.systemUTC(),
    private val pacer: RequestPacer = RequestPacer(),
) {
    private val log = LoggerFactory.getLogger(WarScheduler::class.java)

    /** Olhadas marcadas, por clã + horário; evita marcar a mesma duas vezes a cada ciclo. */
    private val timers = ConcurrentHashMap<Pair<String, Instant>, Job>()

    /** Liga o ciclo de hora em hora. Para junto com o [scope]. */
    fun start(): Job = scope.launch {
        while (isActive) {
            runCycle()
            delay(CYCLE)
        }
    }

    /** Uma olhada em cada clã ativo, um depois do outro. Falha num clã não para os outros. */
    suspend fun runCycle() {
        val clans = history.activeClans(clock.instant())
        clans.forEach { check(it) }
        log.info("Ciclo do monitoramento: {} clã(s)", clans.size)
    }

    /** Ao ativar: olha o clã já, sem esperar o próximo ciclo. */
    fun checkInBackground(clan: MonitoredClan) {
        scope.launch { check(clan) }
    }

    /** Importa o warlog se ainda falta e olha a guerra atual. Nunca lança: registra a falha e segue. */
    suspend fun check(clan: MonitoredClan) {
        if (clan.warlogImportedAt == null) {
            safely(clan.clanTag, "o warlog") {
                val count = pacer.paced { warlogImporter.import(clan.clanTag) }
                log.info("Warlog do clã {} importado: {} guerra(s)", clan.clanTag, count)
            }
        }
        safely(clan.clanTag, "a guerra atual") { checkWar(clan.clanTag) }
    }

    /** Grava o retrato da guerra atual e marca as próximas olhadas. Devolve o estado visto. */
    suspend fun checkWar(clanTag: String): WarState? {
        val now = clock.instant()
        val war = pacer.paced { client.currentWar(clanTag) }
        val record = war.toSnapshot(SnapshotOrigin(clanTag, capturedAt = now)) ?: return null
        history.saveSnapshot(clanTag, record)
        if (record.state != WarState.WAR_ENDED) checkTimes(record.endTime, now).forEach { plan(clanTag, it) }
        return record.state
    }

    /** Horários das olhadas marcadas para [clanTag], em ordem. Para os testes e para o log. */
    fun pendingChecks(clanTag: String): List<Instant> =
        timers.keys.filter { it.first == clanTag }.map { it.second }.sorted()

    private fun plan(clanTag: String, at: Instant) {
        val key = clanTag to at
        // LAZY: só começa depois de entrar no mapa, para o `remove` do fim nunca rodar antes do `put`.
        val job = timers.computeIfAbsent(key) {
            scope.launch(start = CoroutineStart.LAZY) {
                try {
                    delay(java.time.Duration.between(clock.instant(), at).toKotlinDuration())
                    safely(clanTag, "a guerra no horário marcado") { checkWar(clanTag) }
                } finally {
                    timers.remove(key)
                }
            }
        }
        job.start()
    }

    private suspend fun safely(clanTag: String, what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // ApiException (API do jogo: registro privado, manutenção, limite) ou erro do banco.
            log.warn("Não consegui olhar {} do clã {}: {}", what, clanTag, e.message)
        }
    }

    companion object {
        val CYCLE: Duration = 1.hours
        val BEFORE_END: Duration = 4.minutes
        val AFTER_END: Duration = 1.minutes

        /** Olhadas que ainda cabem antes e depois de [endTime]; as que já passaram ficam de fora. */
        fun checkTimes(endTime: Instant, now: Instant): List<Instant> =
            listOf(endTime - BEFORE_END.toJavaDuration(), endTime + AFTER_END.toJavaDuration()).filter { it > now }
    }
}
