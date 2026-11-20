package io.github.danilodox.warbook.history

import io.github.danilodox.warbook.coc.CocClient
import io.github.danilodox.warbook.coc.toWarRecord
import java.time.Clock

/**
 * Importa o warlog do clã (só o placar de cada guerra). O [WarScheduler] chama ao ativar, em segundo plano,
 * e de novo a cada ciclo enquanto o clã estiver sem `warlog_imported_at` (falhou da outra vez).
 */
class WarlogImporter(
    private val client: CocClient,
    private val history: WarHistoryRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    /** Grava já e devolve quantas guerras entraram. */
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
