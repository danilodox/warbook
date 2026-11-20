package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.coc.CocClient
import io.github.danilodox.warbook.coc.toWarRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.Clock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Importa o warlog do clã (só o placar de cada guerra) uma vez, ao ativar. Roda em segundo plano: a
 * resposta da ativação não espera. Se falhar, fica sem `warlog_imported_at` e o agendador tenta de novo.
 */
class WarlogImporter(
    private val client: CocClient,
    private val history: WarHistoryRepository,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val log = LoggerFactory.getLogger(WarlogImporter::class.java)

    fun importInBackground(clanTag: String) {
        scope.launch {
            try {
                val count = import(clanTag)
                log.info("Warlog do clã {} importado: {} guerra(s)", clanTag, count)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                // ApiException (API do jogo) ou erro do banco: tenta de novo no próximo ciclo do agendador.
                log.warn("Não importei o warlog do clã {}: {}", clanTag, e.message)
            }
        }
    }

    /** Grava já e devolve quantas guerras entraram. Público para os testes e para o agendador. */
    suspend fun import(clanTag: String): Int {
        val now = clock.instant()
        val wars = client.warLog(clanTag, LIMIT).items.mapNotNull { it.toWarRecord(now) }
        history.saveWarlog(clanTag, wars)
        history.markWarlogImported(clanTag, now)
        return wars.size
    }

    companion object {
        /** Bem mais que as 10 guerras da atividade; o warlog só tem placar, é leve. */
        const val LIMIT = 100
    }
}
