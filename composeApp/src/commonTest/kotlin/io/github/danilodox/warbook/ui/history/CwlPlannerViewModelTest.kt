package io.github.danilodox.warbook.ui.history

import io.github.danilodox.warbook.api.CwlPlannerDto
import io.github.danilodox.warbook.api.PlannerStatsDto
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.FakeWarbookRepository
import io.github.danilodox.warbook.data.testClan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class CwlPlannerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeWarbookRepository()

    // No testClan: ABC (CV16), Dantas 2PQ8LV9YC (CV15) e Rapadura XYZ (CV13, sem histórico).
    private val stats = CwlPlannerDto(
        warsCounted = 10,
        members = listOf(
            PlannerStatsDto("2PQ8LV9YC", "Dantas", 10, attacksUsed = 20, attacksAvailable = 20, average = 3.0),
            PlannerStatsDto("ABC", "Maré Alta", 10, attacksUsed = 10, attacksAvailable = 20, average = 2.0),
            PlannerStatsDto("SAIU", "Ex-membro", 10, attacksUsed = 20, attacksAvailable = 20, average = 4.0),
        ),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        api.clans[CLAN] = ApiResult.Success(testClan())
        api.planners[CLAN] = ApiResult.Success(stats)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sugereOsMembrosAtuaisNaOrdemDaNotaVezesPresenca() = runTest(dispatcher) {
        val viewModel = CwlPlannerViewModel(CLAN, api)
        advanceUntilIdle()

        val candidates = viewModel.state.value.value.orEmpty()
        assertEquals(listOf("2PQ8LV9YC", "ABC", "XYZ"), candidates.map { it.tag }, "quem saiu do clã fica de fora")
        assertEquals(1.0, candidates[1].rating, "2,00 × 10/20")
        assertEquals(setOf("2PQ8LV9YC", "ABC", "XYZ"), viewModel.selection.value.selected, "clã menor que 15")
        assertFalse(viewModel.selection.value.edited)
    }

    @Test
    fun edicaoSobreviveAAtualizarESoSaiQuemDeixouOCla() = runTest(dispatcher) {
        val viewModel = CwlPlannerViewModel(CLAN, api)
        advanceUntilIdle()

        viewModel.toggle("XYZ")
        val clan = testClan()
        api.clans[CLAN] = ApiResult.Success(clan.copy(members = clan.members.filterNot { it.tag == "ABC" }))
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(setOf("2PQ8LV9YC"), viewModel.selection.value.selected)
        assertTrue(viewModel.selection.value.edited)
    }

    @Test
    fun trocarOTamanhoVoltaParaASugestao() = runTest(dispatcher) {
        val viewModel = CwlPlannerViewModel(CLAN, api)
        advanceUntilIdle()
        viewModel.toggle("XYZ")

        viewModel.setSize(30)

        assertEquals(30, viewModel.selection.value.size)
        assertEquals(3, viewModel.selection.value.selected.size)
        assertFalse(viewModel.selection.value.edited)
    }

    @Test
    fun falhaNasEstatisticasMostraOErro() = runTest(dispatcher) {
        api.planners[CLAN] = ApiResult.Failure(AppError.NETWORK)

        val viewModel = CwlPlannerViewModel(CLAN, api)
        advanceUntilIdle()

        assertEquals(AppError.NETWORK, viewModel.state.value.error)
    }

    private companion object {
        const val CLAN = "2YQUC8J0P"
    }
}
