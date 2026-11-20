package io.github.danilodox.warbook.ui.war

import io.github.danilodox.warbook.api.MonitoringDto
import io.github.danilodox.warbook.api.MonitoringState
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.FakeAccountDao
import io.github.danilodox.warbook.data.FakePlayerCacheDao
import io.github.danilodox.warbook.data.FakeWarbookRepository
import io.github.danilodox.warbook.data.MonitoringRepository
import io.github.danilodox.warbook.data.RoomAccountsRepository
import io.github.danilodox.warbook.data.RoomProfileRepository
import io.github.danilodox.warbook.data.testPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MonitoringViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeWarbookRepository()
    private val accountDao = FakeAccountDao()
    private val profiles = RoomProfileRepository(FakePlayerCacheDao(), accountDao, api) { 1L }
    private val accounts = RoomAccountsRepository(accountDao, profiles, api) { 1L }
    private val repository = MonitoringRepository(api, accounts)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.opened() = MonitoringViewModel(CLAN, repository).also { advanceUntilIdle() }

    @Test
    fun mostraOEstadoDoServidor() = runTest(dispatcher) {
        accounts.add(testPlayer())

        val state = opened().state.value

        assertEquals(MonitoringState.OFF, state.status?.state)
        assertFalse(state.canActivate)
    }

    @Test
    fun falhaAoCarregarEscondeOCartao() = runTest(dispatcher) {
        api.monitorings[CLAN] = ApiResult.Failure(AppError.NETWORK)

        val state = opened().state.value

        assertNull(state.status)
        assertNull(state.error)
    }

    @Test
    fun ativarMostraOEstadoNovo() = runTest(dispatcher) {
        accounts.add(testPlayer())
        accounts.verify("2PQ8LV9YC", "token-do-jogo")
        val viewModel = opened()
        assertTrue(viewModel.state.value.canActivate)

        viewModel.activate()
        advanceUntilIdle()

        assertEquals(MonitoringState.ACTIVE, viewModel.state.value.status?.state)
        assertFalse(viewModel.state.value.activating)
    }

    @Test
    fun erroAoAtivarApareceNoCartao() = runTest(dispatcher) {
        accounts.add(testPlayer())
        accounts.verify("2PQ8LV9YC", "token-do-jogo")
        api.visitResult = ApiResult.Failure(AppError.NOT_CLAN_MEMBER)
        val viewModel = opened()

        viewModel.activate()
        advanceUntilIdle()

        assertEquals(AppError.NOT_CLAN_MEMBER, viewModel.state.value.error)
        assertEquals(MonitoringDto(MonitoringState.OFF), viewModel.state.value.status)
    }

    private companion object {
        const val CLAN = "2YQUC8J0P"
    }
}
