package io.github.danilodox.warbook.ui.clan

import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.FollowKind
import io.github.danilodox.warbook.data.RecentSearch
import io.github.danilodox.warbook.data.testClan
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClanDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val rig = DetailTestRig()
    private val mito = testClan(tag = "OPP", name = "Os Mito", level = 20)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        rig.api.clans["OPP"] = ApiResult.Success(mito)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.opened(fromSearch: Boolean = false) = ClanDetailViewModel(
        "OPP",
        rig.clans,
        rig.follows,
        rig.accounts,
        rig.preferences,
        fromSearch,
    ).also { advanceUntilIdle() }

    @Test
    fun abrirBuscaOClaSemSalvarNoBanco() = runTest(dispatcher) {
        val state = opened().state.value

        assertEquals("Os Mito", state.clan?.value?.name)
        assertEquals(ClanTab.INFO, state.tab)
        assertTrue(rig.clanCache.rows.value.isEmpty(), "clã de passagem fica só na memória")
    }

    @Test
    fun abertoPelaBuscaEntraNasRecentesComBrasao() = runTest(dispatcher) {
        opened(fromSearch = true)

        assertEquals(
            listOf(RecentSearch(FollowKind.CLAN, "OPP", "Os Mito", 20, "https://img/m.png")),
            rig.preferences.recentSearches.value,
        )
    }

    @Test
    fun trocaDeSubAbaEOrdem() = runTest(dispatcher) {
        val viewModel = opened()

        viewModel.selectTab(ClanTab.MEMBERS)
        viewModel.sortBy(MemberSort.DONATIONS)
        advanceUntilIdle()

        assertEquals(ClanTab.MEMBERS, viewModel.state.value.tab)
        assertEquals(MemberSort.DONATIONS, viewModel.state.value.sort)
    }

    @Test
    fun marcaVoceComAContaAtiva() = runTest(dispatcher) {
        rig.accounts.add(testPlayer())

        assertEquals("2PQ8LV9YC", opened().state.value.accountTag)
    }

    @Test
    fun seguirGuardaEAbreOffline() = runTest(dispatcher) {
        opened().apply { toggleFollow() }
        advanceUntilIdle()
        assertNotNull(rig.clanCache.rows.value["OPP"])
        rig.api.clans["OPP"] = ApiResult.Failure(AppError.NETWORK)

        val state = opened().state.value

        assertTrue(state.following)
        assertEquals("Os Mito", state.clan?.value?.name)
        assertEquals(AppError.NETWORK, state.error)
        assertFalse(state.refreshing)
    }
}
