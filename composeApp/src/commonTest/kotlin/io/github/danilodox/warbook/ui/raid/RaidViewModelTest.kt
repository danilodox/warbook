package io.github.danilodox.warbook.ui.raid

import io.github.danilodox.warbook.api.CapitalRaidsDto
import io.github.danilodox.warbook.api.RaidMemberDto
import io.github.danilodox.warbook.api.RaidSeasonDto
import io.github.danilodox.warbook.api.RaidState
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.testPlayer
import io.github.danilodox.warbook.ui.DetailTestRig
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RaidViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val rig = DetailTestRig()
    private val raids = CapitalRaidsDto(
        listOf(
            RaidSeasonDto(
                RaidState.ONGOING,
                members = listOf(RaidMemberDto("2PQ8LV9YC", "Dantas", 3, 5, 9000)),
            ),
            RaidSeasonDto(RaidState.ENDED, offensiveReward = 148),
        ),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.opened(): RaidViewModel = RaidViewModel("OPP", rig.accounts, rig.api) { 5_000L }.also {
        it.onOpen()
        advanceUntilIdle()
    }

    @Test
    fun abrirBuscaOsRaidsEMostraOMaisRecente() = runTest(dispatcher) {
        rig.api.raids["OPP"] = ApiResult.Success(raids)

        val state = opened().state.value

        assertEquals(listOf("OPP"), rig.api.raidCalls)
        assertEquals(RaidState.ONGOING, state.season?.state)
        assertEquals(5_000L, state.fetchedAt)
        assertNull(state.error)
    }

    @Test
    fun escolherOutraTemporada() = runTest(dispatcher) {
        rig.api.raids["OPP"] = ApiResult.Success(raids)
        val viewModel = opened()

        viewModel.selectSeason(1)
        advanceUntilIdle()
        assertEquals(148, viewModel.state.value.season?.offensiveReward)

        viewModel.selectSeason(7)
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.selected, "índice que não existe é ignorado")
    }

    @Test
    fun erroSemNadaCarregadoDeixaSoOErro() = runTest(dispatcher) {
        rig.api.raids["OPP"] = ApiResult.Failure(AppError.NETWORK)

        val state = opened().state.value

        assertNull(state.raids)
        assertEquals(AppError.NETWORK, state.error)
    }

    @Test
    fun erroAoAtualizarMantemOQueJaTinha() = runTest(dispatcher) {
        rig.api.raids["OPP"] = ApiResult.Success(raids)
        val viewModel = opened()

        rig.api.raids["OPP"] = ApiResult.Failure(AppError.API_MAINTENANCE)
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(raids, state.raids)
        assertEquals(AppError.API_MAINTENANCE, state.error)
    }

    @Test
    fun temporadaEscolhidaQueSumiuVoltaParaAMaisRecente() = runTest(dispatcher) {
        rig.api.raids["OPP"] = ApiResult.Success(raids)
        val viewModel = opened()
        viewModel.selectSeason(1)

        rig.api.raids["OPP"] = ApiResult.Success(CapitalRaidsDto(raids.seasons.take(1)))
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(0, viewModel.state.value.selected)
    }

    @Test
    fun marcaAContaAtivaParaOVoce() = runTest(dispatcher) {
        rig.accounts.add(testPlayer())
        rig.api.raids["OPP"] = ApiResult.Success(raids)

        val state = opened().state.value

        assertEquals("2PQ8LV9YC", state.accountTag)
        assertTrue(state.season?.members.orEmpty().any { it.tag == state.accountTag })
    }
}
