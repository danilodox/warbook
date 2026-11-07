package io.github.danilodox.warbook.ui.clan

import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClanTabViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val rig = DetailTestRig()
    private val player = testPlayer()
    private val clanTag = "2YQUC8J0P"

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        rig.api.clans[clanTag] = ApiResult.Success(testClan())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.opened() = ClanTabViewModel(rig.accounts, rig.profiles, rig.clans).also {
        it.onOpen()
        advanceUntilIdle()
    }

    @Test
    fun semContaConvidaENaoChamaNada() = runTest(dispatcher) {
        val state = opened().state.value

        assertTrue(state.loaded)
        assertNull(state.accountTag)
        assertTrue(rig.api.clanCalls.isEmpty())
    }

    @Test
    fun abrirBuscaOClaDaContaESalvaNoBanco() = runTest(dispatcher) {
        rig.accounts.add(player)

        val state = opened().state.value

        assertEquals(listOf(clanTag), rig.api.clanCalls)
        assertEquals("Picuí Raiz", state.clan?.value?.name)
        assertFalse(state.refreshing)
        assertNotNull(rig.clanCache.rows.value[clanTag], "o clã da conta abre offline depois")
    }

    @Test
    fun semPerfilSalvoBuscaOPerfilAntes() = runTest(dispatcher) {
        rig.accounts.add(player)
        rig.playerCache.delete(player.tag)
        rig.api.players[player.tag] = ApiResult.Success(player)

        val state = opened().state.value

        assertEquals(listOf(player.tag), rig.api.playerCalls)
        assertTrue(state.profileKnown)
        assertEquals(listOf(clanTag), rig.api.clanCalls)
    }

    @Test
    fun perfilQueFalhaMostraOErro() = runTest(dispatcher) {
        rig.accounts.add(player)
        rig.playerCache.delete(player.tag)
        rig.api.players[player.tag] = ApiResult.Failure(AppError.NETWORK)

        val state = opened().state.value

        assertFalse(state.profileKnown)
        assertEquals(AppError.NETWORK, state.profileError)
        assertTrue(rig.api.clanCalls.isEmpty())
    }

    @Test
    fun semClaNaoBuscaClan() = runTest(dispatcher) {
        rig.accounts.add(testPlayer(clan = null))

        val state = opened().state.value

        assertTrue(state.profileKnown)
        assertNull(state.clanTag)
        assertTrue(rig.api.clanCalls.isEmpty())
    }

    @Test
    fun offlineMantemOSalvoEMostraOErro() = runTest(dispatcher) {
        rig.accounts.add(player)
        val viewModel = opened()
        rig.api.clans[clanTag] = ApiResult.Failure(AppError.NETWORK)

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AppError.NETWORK, state.error)
        assertEquals("Picuí Raiz", state.clan?.value?.name)
    }

    @Test
    fun trocaDeSubAbaEOrdem() = runTest(dispatcher) {
        rig.accounts.add(player)
        val viewModel = opened()

        viewModel.selectTab(ClanTab.MEMBERS)
        viewModel.sortBy(MemberSort.TROPHIES)
        advanceUntilIdle()

        assertEquals(ClanTab.MEMBERS, viewModel.state.value.tab)
        assertEquals(MemberSort.TROPHIES, viewModel.state.value.sort)
    }
}
