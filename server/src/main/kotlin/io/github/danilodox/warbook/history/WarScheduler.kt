package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.api.ApiErrorCode
import io.github.danilodox.warbook.api.CwlState
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.coc.CocClient
import io.github.danilodox.warbook.coc.CocLeagueGroup
import io.github.danilodox.warbook.coc.SnapshotOrigin
import io.github.danilodox.warbook.coc.cocCwlState
import io.github.danilodox.warbook.coc.drawnWarTags
import io.github.danilodox.warbook.coc.involves
import io.github.danilodox.warbook.coc.toSnapshot
import io.github.danilodox.warbook.errors.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Guarda as guerras dos clãs monitorados. A cada [CYCLE] olha, em cada clã ativo, a guerra atual e as
 * rodadas da CWL, e grava o retrato. Com guerra aberta, marca mais duas olhadas: [BEFORE_END] antes do fim
 * (último retrato com a guerra rolando) e [AFTER_END] depois (o placar final).
 *
 * Recuperação: o primeiro ciclo roda ao subir o servidor. Ele pega a guerra que estiver aberta e todas as
 * rodadas da CWL da temporada que ainda não têm retrato final (enquanto a API mostrar o grupo).
 * Todas as chamadas à API do jogo passam pelo [pacer].
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

    /** Olhadas marcadas perto do fim de cada guerra. Público para os testes. */
    val scheduled = ScheduledChecks(scope, clock)
    private val foreignWarTags = ForeignWarTags()

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

    /** Importa o warlog se ainda falta, olha a guerra atual e a CWL. Nunca lança: registra a falha e segue. */
    suspend fun check(clan: MonitoredClan) {
        if (clan.warlogImportedAt == null) {
            safely(clan.clanTag, "o warlog") {
                val count = pacer.paced { warlogImporter.import(clan.clanTag) }
                log.info("Warlog do clã {} importado: {} guerra(s)", clan.clanTag, count)
            }
        }
        safely(clan.clanTag, "a guerra atual") { checkWar(clan.clanTag) }
        safely(clan.clanTag, "a CWL") { checkCwl(clan.clanTag) }
    }

    /** Grava o retrato da guerra atual e marca as próximas olhadas. Devolve o estado visto. */
    suspend fun checkWar(clanTag: String): WarState? {
        val now = clock.instant()
        val war = pacer.paced { client.currentWar(clanTag) }
        val record = war.toSnapshot(SnapshotOrigin(clanTag, capturedAt = now)) ?: return null
        history.saveSnapshot(clanTag, record)
        if (record.state != WarState.WAR_ENDED) plan(clanTag, null, record.endTime, now)
        return record.state
    }

    /**
     * Grava as rodadas da CWL do clã que ainda não têm retrato final. Rodada encerrada já gravada não é
     * buscada de novo. Devolve quantas rodadas do clã foram gravadas; 0 fora da CWL.
     */
    suspend fun checkCwl(clanTag: String): Int {
        val group = leagueGroup(clanTag)?.takeIf { cocCwlState(it.state) != CwlState.NOT_IN_CWL } ?: return 0
        val ended = history.endedWarTags(clanTag)
        val foreign = foreignWarTags.get(clanTag, group.season)
        val pending = group.drawnWarTags().filter { it !in ended && it !in foreign }
        return pending.count { checkLeagueWar(clanTag, it, group.season) != null }
    }

    /** Fora da CWL a API responde 404: aqui vira `null`. */
    private suspend fun leagueGroup(clanTag: String): CocLeagueGroup? = try {
        pacer.paced { client.leagueGroup(clanTag) }
    } catch (e: ApiException) {
        if (e.code != ApiErrorCode.NOT_FOUND) throw e
        null
    }

    /** Grava a rodada [warTag] se for do clã. Devolve o estado visto, ou `null` se a guerra é de outros. */
    private suspend fun checkLeagueWar(clanTag: String, warTag: String, season: String?): WarState? {
        val now = clock.instant()
        val war = pacer.paced { client.leagueWar(warTag) }
        val record = if (war.involves(clanTag)) {
            war.toSnapshot(SnapshotOrigin(clanTag, now, WarType.CWL, warTag, season))
        } else {
            foreignWarTags.add(clanTag, season, warTag)
            null
        }
        if (record == null) return null
        history.saveSnapshot(clanTag, record)
        if (record.state != WarState.WAR_ENDED) plan(clanTag, warTag, record.endTime, now, season)
        return record.state
    }

    /** Marca as olhadas perto de [endTime] que ainda não passaram. [warTag] nulo = guerra atual. */
    private fun plan(clanTag: String, warTag: String?, endTime: Instant, now: Instant, season: String? = null) {
        checkTimes(endTime, now).forEach { at ->
            scheduled.schedule(ScheduledChecks.Key(clanTag, warTag, at)) {
                safely(clanTag, "a guerra no horário marcado") {
                    if (warTag == null) checkWar(clanTag) else checkLeagueWar(clanTag, warTag, season)
                }
            }
        }
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
