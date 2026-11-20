package io.github.danilodox.warbook.data

import io.github.danilodox.warbook.api.MonitoringDto
import io.github.danilodox.warbook.api.MonitoringState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonitoringRepositoryTest {

    private val accountDao = FakeAccountDao()
    private val api = FakeWarbookRepository()
    private var now = 1_000L
    private val profiles = RoomProfileRepository(FakePlayerCacheDao(), accountDao, api) { now }
    private val accounts = RoomAccountsRepository(accountDao, profiles, api) { now++ }
    private val repository = MonitoringRepository(api, accounts)

    private suspend fun verifiedAccount(tag: String = "2PQ8LV9YC", clan: String? = "Picuí Raiz") {
        accounts.add(testPlayer(tag = tag, clan = clan))
        api.verifyResult = ApiResult.Success("prova-$tag")
        accounts.verify(tag, "token-do-jogo")
    }

    @Test
    fun soContaVerificadaDoClaPodeAtivar() = runTest {
        accounts.add(testPlayer())
        assertFalse(repository.canActivate(CLAN))

        accounts.verify("2PQ8LV9YC", "token-do-jogo")

        assertTrue(repository.canActivate(CLAN))
        assertFalse(repository.canActivate("OUTRO"))
    }

    @Test
    fun ativarMandaAProvaDaContaDoCla() = runTest {
        verifiedAccount()

        val result = repository.activate(CLAN)

        assertEquals(MonitoringState.ACTIVE, (result as ApiResult.Success).value.state)
        assertEquals(listOf(CLAN to "prova-2PQ8LV9YC"), api.visitCalls)
    }

    @Test
    fun semContaVerificadaNemChamaOServidor() = runTest {
        accounts.add(testPlayer())

        val result = repository.activate(CLAN)

        assertEquals(ApiResult.Failure(AppError.NOT_VERIFIED), result)
        assertTrue(api.visitCalls.isEmpty())
    }

    @Test
    fun abrirOAppNaoLigaClaQueNuncaFoiAtivado() = runTest {
        verifiedAccount()

        repository.recordVisits()

        assertEquals(listOf(CLAN), api.monitoringCalls)
        assertTrue(api.visitCalls.isEmpty())
    }

    @Test
    fun abrirOAppRenovaClaAtivoETiraDaPausa() = runTest {
        verifiedAccount()
        verifiedAccount(tag = "9CJ2YQ0L", clan = null)
        api.monitorings[CLAN] = ApiResult.Success(MonitoringDto(MonitoringState.PAUSED))

        repository.recordVisits()

        assertEquals(listOf(CLAN to "prova-2PQ8LV9YC"), api.visitCalls, "conta sem clã fica de fora")
    }

    @Test
    fun soUmaVisitaPorClaACadaAberturaDoApp() = runTest {
        verifiedAccount()
        api.monitorings[CLAN] = ApiResult.Success(MonitoringDto(MonitoringState.ACTIVE))

        repository.recordVisits()
        repository.recordVisits()

        assertEquals(1, api.visitCalls.size)
    }

    @Test
    fun semRedeTentaDeNovoNaProximaVez() = runTest {
        verifiedAccount()
        api.monitorings[CLAN] = ApiResult.Failure(AppError.NETWORK)
        repository.recordVisits()

        api.monitorings[CLAN] = ApiResult.Success(MonitoringDto(MonitoringState.ACTIVE))
        repository.recordVisits()

        assertEquals(1, api.visitCalls.size)
    }

    private companion object {
        /** Clã do [testPlayer]. */
        const val CLAN = "2YQUC8J0P"
    }
}
