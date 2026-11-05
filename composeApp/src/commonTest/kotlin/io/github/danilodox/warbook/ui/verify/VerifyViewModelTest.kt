package io.github.danilodox.warbook.ui.verify

import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.FakeAccountDao
import io.github.danilodox.warbook.data.FakePlayerCacheDao
import io.github.danilodox.warbook.data.FakeWarbookRepository
import io.github.danilodox.warbook.data.RoomAccountsRepository
import io.github.danilodox.warbook.data.RoomProfileRepository
import io.github.danilodox.warbook.data.testPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VerifyViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeWarbookRepository()
    private val accountDao = FakeAccountDao()
    private val profiles = RoomProfileRepository(FakePlayerCacheDao(), accountDao, api) { 1L }
    private val accounts = RoomAccountsRepository(accountDao, profiles, api) { 1L }
    private val viewModel by lazy { VerifyViewModel("2PQ8LV9YC", accounts) }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun tokenCertoVerificaAConta() = runTest(dispatcher) {
        accounts.add(testPlayer())

        viewModel.verify("tk-do-jogo")
        assertEquals(VerifyState.Verifying, viewModel.state.value)
        advanceUntilIdle()

        assertEquals(VerifyState.Verified, viewModel.state.value)
        assertTrue(accounts.accounts.first().single().isVerified)
        assertEquals(listOf("2PQ8LV9YC" to "tk-do-jogo"), api.verifyCalls)
    }

    @Test
    fun tokenRecusadoMostraOErroEDeixaTentarDeNovo() = runTest(dispatcher) {
        accounts.add(testPlayer())
        api.verifyResult = ApiResult.Failure(AppError.INVALID_VERIFY_TOKEN)

        viewModel.verify("tk-velho")
        advanceUntilIdle()

        assertEquals(VerifyState.Failed(AppError.INVALID_VERIFY_TOKEN), viewModel.state.value)
        assertFalse(accounts.accounts.first().single().isVerified)

        viewModel.clearError()
        assertEquals(VerifyState.Idle, viewModel.state.value)
    }

    @Test
    fun naoMandaDuasVezesEnquantoVerifica() = runTest(dispatcher) {
        accounts.add(testPlayer())

        viewModel.verify("tk-1")
        viewModel.verify("tk-2")
        advanceUntilIdle()

        assertEquals(1, api.verifyCalls.size)
    }
}
