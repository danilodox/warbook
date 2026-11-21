package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.MonitoringDto
import io.github.danilodox.warbook.api.MonitoringState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Monitoramento de guerras do clã (G5). O estado vem sempre do servidor, sem guardar no aparelho.
 * Ativar é sempre um toque do usuário; abrir o app só renova (e tira da pausa) um clã que já foi ativado.
 */
class MonitoringRepository(private val api: WarbookRepository, private val accounts: AccountsRepository) {
    private val visitedThisRun = mutableSetOf<String>()
    private val mutex = Mutex()

    suspend fun status(clanTag: String): ApiResult<MonitoringDto> = api.monitoring(clanTag)

    /** `true` se alguma conta verificada do aparelho é desse clã (só ela pode ativar). */
    suspend fun canActivate(clanTag: String): Boolean = accounts.verifiedMembers().any { it.clanTag == clanTag }

    /** Ativa ou reativa com a prova de uma conta verificada do clã (a ativa, se for dele). */
    suspend fun activate(clanTag: String): ApiResult<MonitoringDto> {
        val member = accounts.verifiedMembers().firstOrNull { it.clanTag == clanTag }
            ?: return ApiResult.Failure(AppError.NOT_VERIFIED)
        return api.recordMonitoringVisit(clanTag, member.proof).also {
            if (it is ApiResult.Success) mutex.withLock { visitedThisRun += clanTag }
        }
    }

    /**
     * Ao abrir o app: para cada clã com conta verificada, conta que um membro verificado abriu o app. Um clã que
     * nunca foi ativado fica como está; um pausado volta a ser acompanhado. Uma vez por clã a cada abertura do
     * app. Falhas são ignoradas: na próxima abertura tenta de novo.
     */
    suspend fun recordVisits() {
        accounts.verifiedMembers().groupBy { it.clanTag }.forEach { (clanTag, members) ->
            val firstTime = mutex.withLock { visitedThisRun.add(clanTag) }
            if (firstTime) visit(clanTag, members.first().proof)
        }
    }

    private suspend fun visit(clanTag: String, proof: String) {
        when ((api.monitoring(clanTag) as? ApiResult.Success)?.value?.state) {
            // Sem resposta: tenta de novo na próxima abertura.
            null -> mutex.withLock { visitedThisRun -= clanTag }
            // Desligado não liga sozinho.
            MonitoringState.OFF -> Unit
            MonitoringState.ACTIVE, MonitoringState.PAUSED -> api.recordMonitoringVisit(clanTag, proof)
        }
    }
}
