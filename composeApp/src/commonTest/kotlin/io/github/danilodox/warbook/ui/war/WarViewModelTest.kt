package io.github.danilodox.warbook.ui.war

import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.api.CwlState
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.FakeAccountDao
import io.github.danilodox.warbook.data.FakeAppPreferences
import io.github.danilodox.warbook.data.FakePlayerCacheDao
import io.github.danilodox.warbook.data.FakeWarCacheDao
import io.github.danilodox.warbook.data.FakeWarbookRepository
import io.github.danilodox.warbook.data.RoomAccountsRepository
import io.github.danilodox.warbook.data.RoomProfileRepository
import io.github.danilodox.warbook.data.RoomWarRepository
import io.github.danilodox.warbook.data.testPlayer
import io.github.danilodox.warbook.war.SummaryOptions
import io.github.danilodox.warbook.war.SummaryPart
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
class WarViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeWarbookRepository()
    private val accountDao = FakeAccountDao()
    private var now = 1_000L
    private val profiles = RoomProfileRepository(FakePlayerCacheDao(), accountDao, api) { now }
    private val accounts = RoomAccountsRepository(accountDao, profiles, api) { now++ }
    private val wars = RoomWarRepository(FakeWarCacheDao(), api) { now }
    private val preferences = FakeAppPreferences()

    private val player = testPlayer()
    private val clanTag = "2YQUC8J0P"

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        api.wars[clanTag] = ApiResult.Success(CurrentWarDto(WarState.IN_WAR, teamSize = 15))
        api.cwls[clanTag] = ApiResult.Success(CwlDto(CwlState.NOT_IN_CWL))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.opened(): WarViewModel = WarViewModel(accounts, profiles, wars, preferences).also {
        it.onOpen()
        advanceUntilIdle()
    }

    @Test
    fun semContaConvidaENaoChamaNada() = runTest(dispatcher) {
        val state = opened().state.value

        assertTrue(state.loaded)
        assertNull(state.accountTag)
        assertTrue(api.warCalls.isEmpty())
    }

    @Test
    fun abrirBuscaAGuerraDoClaDaContaAtiva() = runTest(dispatcher) {
        accounts.add(player)

        val state = opened().state.value

        assertEquals(listOf(clanTag), api.warCalls)
        assertEquals(WarState.IN_WAR, state.war.cached?.value?.state)
        assertFalse(state.war.refreshing)
        assertTrue(api.cwlCalls.isEmpty(), "a CWL só busca quando a sub-aba abre")
    }

    @Test
    fun semPerfilSalvoBuscaOPerfilAntes() = runTest(dispatcher) {
        accounts.add(player)
        profiles.forget(player.tag)
        api.players[player.tag] = ApiResult.Success(player)

        val state = opened().state.value

        assertEquals(listOf(player.tag), api.playerCalls)
        assertTrue(state.profileKnown)
        assertEquals(listOf(clanTag), api.warCalls)
    }

    @Test
    fun semClaNaoBuscaGuerra() = runTest(dispatcher) {
        accounts.add(testPlayer(clan = null))

        val state = opened().state.value

        assertTrue(state.profileKnown)
        assertNull(state.clan)
        assertTrue(api.warCalls.isEmpty())
    }

    @Test
    fun cwlAtualizaNaPrimeiraVezQueASubAbaAbre() = runTest(dispatcher) {
        accounts.add(player)
        val viewModel = opened()

        viewModel.selectTab(WarTab.CWL)
        advanceUntilIdle()
        viewModel.selectTab(WarTab.CURRENT)
        viewModel.selectTab(WarTab.CWL)
        advanceUntilIdle()

        assertEquals(1, api.cwlCalls.size, "trocar de novo não busca outra vez")
        assertEquals(1, api.warCalls.size)
        assertEquals(CwlState.NOT_IN_CWL, viewModel.state.value.cwl.cached?.value?.state)

        viewModel.onOpen()
        advanceUntilIdle()
        assertEquals(2, api.cwlCalls.size, "reabrir a aba atualiza a sub-aba aberta")
    }

    @Test
    fun erroMantemOSalvoEMostraOErro() = runTest(dispatcher) {
        accounts.add(player)
        val viewModel = opened()
        api.wars[clanTag] = ApiResult.Failure(AppError.NETWORK)

        viewModel.refresh()
        advanceUntilIdle()

        val war = viewModel.state.value.war
        assertEquals(AppError.NETWORK, war.error)
        assertEquals(WarState.IN_WAR, war.cached?.value?.state)
    }

    @Test
    fun registroPrivadoChegaComoErro() = runTest(dispatcher) {
        accounts.add(player)
        api.wars[clanTag] = ApiResult.Failure(AppError.WAR_LOG_PRIVATE)

        val state = opened().state.value

        assertEquals(AppError.WAR_LOG_PRIVATE, state.war.error)
        assertNull(state.war.cached)
    }

    @Test
    fun rodadaEscolhidaVoltaParaAAtualAoReabrir() = runTest(dispatcher) {
        accounts.add(player)
        val viewModel = opened()

        viewModel.selectRound(3)
        advanceUntilIdle()
        assertEquals(3, viewModel.state.value.selectedRound)
        viewModel.onOpen()
        advanceUntilIdle()

        assertNull(viewModel.state.value.selectedRound)
    }

    @Test
    fun ligarUmaParteDoResumoFicaSalva() = runTest(dispatcher) {
        val viewModel = opened()
        assertEquals(SummaryOptions(), viewModel.state.value.summaryOptions)

        viewModel.toggleSummaryPart(SummaryPart.BEST_ATTACKS)
        viewModel.toggleSummaryPart(SummaryPart.SCORE)
        advanceUntilIdle()

        val expected = SummaryOptions(SummaryOptions.DEFAULT_PARTS - SummaryPart.SCORE + SummaryPart.BEST_ATTACKS)
        assertEquals(expected, preferences.summaryOptions.value, "dois toques seguidos não se perdem")
        assertEquals(expected, viewModel.state.value.summaryOptions)
    }
}
