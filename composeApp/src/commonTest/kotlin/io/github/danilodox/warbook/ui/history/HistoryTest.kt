package io.github.danilodox.warbook.ui.history

import io.github.danilodox.warbook.api.WarDetailDto
import io.github.danilodox.warbook.api.WarHistoryDto
import io.github.danilodox.warbook.api.WarKind
import io.github.danilodox.warbook.api.WarOutcome
import io.github.danilodox.warbook.api.WarState
import io.github.danilodox.warbook.api.WarSummaryDto
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.FakeWarbookRepository
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryTest {

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeWarbookRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun war(id: Long, result: WarOutcome?) = WarSummaryDto(
        id = id,
        kind = WarKind.WAR,
        state = if (result == null) WarState.IN_WAR else WarState.WAR_ENDED,
        endTime = 1_000L * id,
        teamSize = 15,
        attacksPerMember = 2,
        stars = 30,
        destruction = 80.0,
        opponentStars = 28,
        opponentDestruction = 70.0,
        detailed = true,
        result = result,
    )

    @Test
    fun formataANotaComOSeparadorDoIdioma() {
        assertEquals("2,80", formatPoints(2.8, ","))
        assertEquals("2.80", formatPoints(2.8, "."))
        assertEquals("-2,00", formatPoints(-2.0, ","))
        assertEquals("0,99", formatPoints(0.99, ","))
        assertEquals("+3,22", formatPoints(3.22, ",", signed = true))
        assertEquals("0,00", formatPoints(0.0, ",", signed = true))
        assertEquals("-0,50", formatPoints(-0.5, ","))
        assertEquals("12,35", formatPoints(12.345_1, ","))
    }

    @Test
    fun contaVitoriasDerrotasEEmpates() {
        val wars = listOf(
            war(1, WarOutcome.WIN),
            war(2, WarOutcome.WIN),
            war(3, WarOutcome.LOSE),
            war(4, WarOutcome.TIE),
            war(5, null),
        )

        assertEquals(WarRecordCount(wins = 2, losses = 1, ties = 1), wars.recordCount())
    }

    @Test
    fun listaCarregaAoAbrir() = runTest(dispatcher) {
        api.histories[CLAN] = ApiResult.Success(WarHistoryDto(listOf(war(1, WarOutcome.WIN))))

        val viewModel = WarHistoryViewModel(CLAN, api)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.loading)
        assertEquals(listOf(1L), state.value?.wars?.map { it.id })
        assertNull(state.error)
    }

    @Test
    fun falhaNaAtualizacaoMantemOQueJaVeio() = runTest(dispatcher) {
        api.histories[CLAN] = ApiResult.Success(WarHistoryDto(listOf(war(1, WarOutcome.WIN))))
        val viewModel = WarHistoryViewModel(CLAN, api)
        advanceUntilIdle()

        api.histories[CLAN] = ApiResult.Failure(AppError.NETWORK)
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AppError.NETWORK, state.error)
        assertEquals(1, state.value?.wars?.size, "a lista continua na tela")
    }

    @Test
    fun guerraQueNaoExisteMostraOErro() = runTest(dispatcher) {
        val viewModel = WarRecordViewModel(CLAN, warId = 9, api)
        advanceUntilIdle()

        assertEquals(AppError.NOT_FOUND, viewModel.state.value.error)
        assertNull(viewModel.state.value.value)
    }

    @Test
    fun guerraCarregaODetalhe() = runTest(dispatcher) {
        val detail = WarDetailDto(war(7, WarOutcome.LOSE), members = emptyList())
        api.warDetails[CLAN to 7L] = ApiResult.Success(detail)

        val viewModel = WarRecordViewModel(CLAN, warId = 7, api)
        advanceUntilIdle()

        assertEquals(detail, viewModel.state.value.value)
    }

    private companion object {
        const val CLAN = "2YQUC8J0P"
    }
}
