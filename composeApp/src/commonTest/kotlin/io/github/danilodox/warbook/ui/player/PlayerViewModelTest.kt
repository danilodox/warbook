package io.github.danilodox.warbook.ui.player

import io.github.danilodox.warbook.api.ClanSearchResultDto
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.FollowKind
import io.github.danilodox.warbook.data.FollowRepository
import io.github.danilodox.warbook.data.RecentSearch
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
class PlayerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val rig = DetailTestRig()
    private val brabo = testPlayer(tag = "BRABO", name = "Brabo", townHall = 16)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        rig.api.players["BRABO"] = ApiResult.Success(brabo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.opened(fromSearch: Boolean = false) =
        PlayerViewModel("BRABO", rig.profiles, rig.follows, rig.preferences, fromSearch).also { advanceUntilIdle() }

    @Test
    fun abrirBuscaEMostraOPerfil() = runTest(dispatcher) {
        val state = opened().state.value

        assertEquals("Brabo", state.profile?.player?.name)
        assertFalse(state.refreshing)
        assertFalse(state.following)
        assertTrue(rig.preferences.recentSearches.value.isEmpty(), "fora da busca não entra nas recentes")
    }

    @Test
    fun abertoPelaBuscaEntraNasRecentesComCv() = runTest(dispatcher) {
        opened(fromSearch = true)

        assertEquals(
            listOf(RecentSearch(FollowKind.PLAYER, "BRABO", "Brabo", 16)),
            rig.preferences.recentSearches.value,
        )
    }

    @Test
    fun seguirGuardaParaOfflineEDeixarDeSeguirApaga() = runTest(dispatcher) {
        val viewModel = opened()

        viewModel.toggleFollow()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.following)
        assertNotNull(rig.playerCache.rows.value["BRABO"])

        viewModel.toggleFollow()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.following)
        assertNull(rig.playerCache.rows.value["BRABO"])
    }

    @Test
    fun seguidoAbreOfflineComOUltimoDado() = runTest(dispatcher) {
        opened().apply { toggleFollow() }
        advanceUntilIdle()
        rig.api.players["BRABO"] = ApiResult.Failure(AppError.NETWORK)

        val state = opened().state.value

        assertEquals("Brabo", state.profile?.player?.name)
        assertEquals(AppError.NETWORK, state.error)
    }

    @Test
    fun limiteCheioAvisaENaoSegue() = runTest(dispatcher) {
        repeat(FollowRepository.LIMIT) { rig.follows.followClan(ClanSearchResultDto("C$it", "Clã", null, 1, 1)) }
        val viewModel = opened()

        viewModel.toggleFollow()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.followLimitReached)
        assertFalse(viewModel.state.value.following)
    }

    @Test
    fun erroSemNadaSalvo() = runTest(dispatcher) {
        rig.api.players["BRABO"] = ApiResult.Failure(AppError.NOT_FOUND)

        val state = opened().state.value

        assertNull(state.profile)
        assertEquals(AppError.NOT_FOUND, state.error)
    }
}
