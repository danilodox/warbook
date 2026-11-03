package io.github.danilodox.warbook.ui.me

import io.github.danilodox.warbook.api.UnitLevelDto
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
class MeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeWarbookRepository()
    private val accountDao = FakeAccountDao()
    private var now = 1_000L
    private val profiles = RoomProfileRepository(FakePlayerCacheDao(), accountDao, api) { now }
    private val accounts = RoomAccountsRepository(accountDao, profiles, api) { now++ }

    private val main = testPlayer()
    private val mini = testPlayer(tag = "9CJ2YQ0L", name = "Dantas Mini", townHall = 11, clan = "Picuí Base")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): MeViewModel = MeViewModel(accounts, profiles).also { advanceUntilIdle() }

    @Test
    fun semContaFicaVazio() = runTest(dispatcher) {
        val state = viewModel().state.value

        assertTrue(state.loaded)
        assertNull(state.active)
        assertTrue(api.playerCalls.isEmpty())
    }

    @Test
    fun abrirAtualizaOPerfilDaContaAtiva() = runTest(dispatcher) {
        accounts.add(main)
        api.players[main.tag] = ApiResult.Success(main.copy(trophies = 5000))

        val state = viewModel().state.value

        assertEquals(listOf(main.tag), api.playerCalls)
        assertEquals(5000, state.profile?.player?.trophies)
        assertFalse(state.refreshing)
        assertNull(state.refreshError)
    }

    @Test
    fun semRedeMostraOPerfilSalvoComOErro() = runTest(dispatcher) {
        accounts.add(main)
        api.players[main.tag] = ApiResult.Failure(AppError.NETWORK)

        val state = viewModel().state.value

        assertEquals("Dantas", state.profile?.player?.name)
        assertEquals(AppError.NETWORK, state.refreshError)
    }

    @Test
    fun puxarParaAtualizarLimpaOErroQuandoVolta() = runTest(dispatcher) {
        accounts.add(main)
        api.players[main.tag] = ApiResult.Failure(AppError.NETWORK)
        val viewModel = viewModel()

        api.players[main.tag] = ApiResult.Success(main)
        viewModel.refresh()
        advanceUntilIdle()

        assertNull(viewModel.state.value.refreshError)
        assertEquals(2, api.playerCalls.size)
    }

    @Test
    fun trocarDeContaMostraAOutraEAtualizaEla() = runTest(dispatcher) {
        accounts.add(main)
        accounts.add(mini)
        accounts.activate(main.tag)
        api.players[main.tag] = ApiResult.Failure(AppError.NETWORK)
        api.players[mini.tag] = ApiResult.Success(mini)
        val viewModel = viewModel()

        viewModel.switchTo(mini.tag)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(mini.tag, state.active?.tag)
        assertEquals("Dantas Mini", state.profile?.player?.name)
        // O erro era da outra conta.
        assertNull(state.refreshError)
        assertEquals(listOf(main.tag, mini.tag), api.playerCalls)
    }

    @Test
    fun removerAAtivaPassaParaAOutra() = runTest(dispatcher) {
        accounts.add(main)
        accounts.add(mini)
        val viewModel = viewModel()
        assertEquals(mini.tag, viewModel.state.value.active?.tag)

        viewModel.remove(mini.tag)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(listOf(main.tag), state.accounts.map { it.tag })
        assertEquals(main.tag, state.active?.tag)
        assertEquals("Dantas", state.profile?.player?.name)
    }

    @Test
    fun idadeDoPerfilSalvo() {
        assertEquals(Age.JustNow, ageOf(fetchedAt = 0, now = 59_000))
        assertEquals(Age.Minutes(5), ageOf(fetchedAt = 0, now = 5 * 60_000L + 1))
        assertEquals(Age.Hours(2), ageOf(fetchedAt = 0, now = 2 * 3_600_000L))
        assertEquals(Age.Days(3), ageOf(fetchedAt = 0, now = 3 * 86_400_000L))
        // Relógio do aparelho voltou: não mostra tempo negativo.
        assertEquals(Age.JustNow, ageOf(fetchedAt = 10_000, now = 0))
    }

    @Test
    fun progressoDaUnidade() {
        assertEquals(0.5f, UnitLevelDto("Barbarian", 5, 10).progress)
        assertEquals(1f, UnitLevelDto("Barbarian", 12, 10).progress)
        assertEquals(0f, UnitLevelDto("Barbarian", 3, 0).progress)
    }
}
