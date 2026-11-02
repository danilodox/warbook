package io.github.danilodox.warbook.ui.onboarding

import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.FakeAccountDao
import io.github.danilodox.warbook.data.FakeAppPreferences
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeWarbookRepository()
    private val accountDao = FakeAccountDao()
    private val cacheDao = FakePlayerCacheDao()
    private val profiles = RoomProfileRepository(cacheDao, accountDao, api) { 1L }
    private val accounts = RoomAccountsRepository(accountDao, profiles, api) { 1L }
    private val preferences = FakeAppPreferences()
    private val viewModel by lazy { OnboardingViewModel(api, accounts, preferences) }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun tagAchadaGuardaOJogadorEAbreAO2() = runTest(dispatcher) {
        api.players["#2pq8lv9yc"] = ApiResult.Success(testPlayer())

        viewModel.lookUp("#2pq8lv9yc")
        assertTrue(viewModel.state.value.lookingUp)
        advanceUntilIdle()

        assertEquals(TagEvent.PlayerFound, viewModel.tagScreenEvents.first())
        assertEquals("Dantas", viewModel.state.value.player?.name)
        assertFalse(viewModel.state.value.lookingUp)
        // Só achar não adiciona a conta: isso é na O2.
        assertTrue(accounts.accounts.first().isEmpty())
    }

    @Test
    fun tagComErroFicaNaO1ComOErro() = runTest(dispatcher) {
        viewModel.lookUp("2PQ8LV9YC")
        advanceUntilIdle()

        assertEquals(AppError.NOT_FOUND, viewModel.state.value.error)
        assertNull(viewModel.state.value.player)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun confirmarAdicionaAContaAtivaEAbreAVerificacao() = runTest(dispatcher) {
        api.players["2PQ8LV9YC"] = ApiResult.Success(testPlayer())
        viewModel.lookUp("2PQ8LV9YC")
        advanceUntilIdle()

        viewModel.confirm()
        advanceUntilIdle()

        assertEquals("2PQ8LV9YC", viewModel.accountAdded.first())
        val account = accounts.accounts.first().single()
        assertTrue(account.isActive)
        assertFalse(account.isVerified)
        // O perfil já fica salvo para a aba Eu abrir sem rede.
        assertEquals("Dantas", profiles.observe("2PQ8LV9YC").first()?.player?.name)
    }

    @Test
    fun confirmarSemJogadorNaoFazNada() = runTest(dispatcher) {
        viewModel.confirm()
        advanceUntilIdle()

        assertTrue(accounts.accounts.first().isEmpty())
    }

    @Test
    fun soExplorarLigaAFlagEAbreABusca() = runTest(dispatcher) {
        viewModel.explore()
        advanceUntilIdle()

        assertEquals(TagEvent.Explore, viewModel.tagScreenEvents.first())
        assertTrue(preferences.exploreMode.value)
    }
}
