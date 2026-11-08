package io.github.danilodox.warbook.ui.compare

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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CompareViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val rig = DetailTestRig()
    private val eu = testPlayer(tag = "EU", name = "Dantas", townHall = 16)
    private val brabo = testPlayer(tag = "BRABO", name = "Brabo", townHall = 15)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        rig.api.players["EU"] = ApiResult.Success(eu)
        rig.api.players["BRABO"] = ApiResult.Success(brabo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.opened() = CompareViewModel("BRABO", rig.accounts, rig.profiles).also { advanceUntilIdle() }

    @Test
    fun comContaComparaAContaAtivaComOJogador() = runTest(dispatcher) {
        rig.accounts.add(eu)

        val state = opened().state.value

        assertEquals("Dantas", state.mine?.name)
        assertEquals("Brabo", state.theirs?.name)
        val comparison = assertNotNull(state.comparison)
        val townHall = comparison.groups.first().rows.first()
        assertEquals(CompareLabel.Stat(CompareStat.TOWN_HALL), townHall.label)
        assertEquals(1, townHall.result)
        assertFalse(state.refreshing)
        assertNull(state.error)
    }

    @Test
    fun semContaNaoCompara() = runTest(dispatcher) {
        val state = opened().state.value

        assertFalse(state.hasAccount)
        assertNull(state.comparison)
        assertEquals("Brabo", state.theirs?.name)
    }

    @Test
    fun semRedeComparaComOSalvoEMostraOErro() = runTest(dispatcher) {
        rig.accounts.add(eu)
        rig.api.players["BRABO"] = ApiResult.Failure(AppError.NETWORK)

        val state = opened().state.value

        assertEquals(AppError.NETWORK, state.error)
        assertNull(state.comparison, "o outro nunca carregou: não há o que comparar")
        assertEquals("Dantas", state.mine?.name)
    }
}
