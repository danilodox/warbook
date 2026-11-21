package io.github.danilodox.warbook.history

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.toKotlinDuration

/**
 * Olhadas marcadas para um horário. Marcar de novo a mesma (clã + guerra + horário) não duplica. Ficam só na
 * memória: se o servidor reiniciar, o próximo ciclo do [WarScheduler] marca de novo as que ainda cabem.
 */
class ScheduledChecks(private val scope: CoroutineScope, private val clock: Clock) {
    /** [warTag] nulo = guerra atual; preenchido = rodada da CWL. */
    data class Key(val clanTag: String, val warTag: String?, val at: Instant)

    private val jobs = ConcurrentHashMap<Key, Job>()

    fun schedule(key: Key, action: suspend () -> Unit) {
        // LAZY: só começa depois de entrar no mapa, para o `remove` do fim nunca rodar antes do `put`.
        jobs.computeIfAbsent(key) {
            scope.launch(start = CoroutineStart.LAZY) {
                try {
                    delay(java.time.Duration.between(clock.instant(), key.at).toKotlinDuration())
                    action()
                } finally {
                    jobs.remove(key)
                }
            }
        }.start()
    }

    /** Horários marcados para [clanTag], em ordem. */
    fun pending(clanTag: String): List<Instant> = jobs.keys.filter { it.clanTag == clanTag }.map { it.at }.sorted()
}

/**
 * Por clã: temporada + warTags do grupo da CWL que não são dele (3 de 4 por rodada). Não são buscadas de novo
 * na mesma temporada; a troca de temporada esvazia, então a memória não cresce. Depois de reiniciar, busca
 * uma vez de novo.
 */
class ForeignWarTags {
    private val byClan = ConcurrentHashMap<String, Pair<String?, Set<String>>>()

    fun get(clanTag: String, season: String?): Set<String> =
        byClan[clanTag]?.takeIf { it.first == season }?.second.orEmpty()

    fun add(clanTag: String, season: String?, warTag: String) {
        byClan.compute(clanTag) { _, old -> season to old?.takeIf { it.first == season }?.second.orEmpty() + warTag }
    }
}
