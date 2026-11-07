package io.github.danilodox.warbook.ui.search

import io.github.danilodox.warbook.api.ClanSearchResultDto
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.FollowKind
import io.github.danilodox.warbook.data.FollowRepository
import io.github.danilodox.warbook.data.RecentSearch
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val rig = DetailTestRig()
    private val mito = ClanSearchResultDto("8QU0VC", "Os Mito", "https://img/m.png", 20, 48)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(mode: SearchMode = SearchMode.PLAYER) =
        SearchViewModel(rig.clans, rig.follows, rig.preferences).also {
            it.selectMode(mode)
            advanceUntilIdle()
        }

    private fun SearchViewModel.typed(query: String) = apply {
        submit(query)
        dispatcher.scheduler.advanceUntilIdle()
    }

    private val SearchViewModel.results get() = assertIs<SearchOutcome.Results>(state.value.outcome).clans

    @Test
    fun jogadorPorTagComHashAbreATelaJogador() = runTest(dispatcher) {
        val viewModel = viewModel().typed(" #2pq8lv9yc ")

        assertEquals(SearchTarget(FollowKind.PLAYER, "2PQ8LV9YC"), viewModel.state.value.open)
        viewModel.onOpened()
        advanceUntilIdle()
        assertNull(viewModel.state.value.open)
    }

    @Test
    fun jogadorSoComCaracteresDeTagViraTag() = runTest(dispatcher) {
        val viewModel = viewModel().typed("2PQ8LV9YC")

        assertEquals(SearchTarget(FollowKind.PLAYER, "2PQ8LV9YC"), viewModel.state.value.open)
    }

    @Test
    fun nickAvisaQueChegaNaV07() = runTest(dispatcher) {
        val viewModel = viewModel().typed("Dantas")

        assertEquals(SearchOutcome.NickNotice, viewModel.state.value.outcome)
        assertNull(viewModel.state.value.open)
        assertTrue(rig.api.playerCalls.isEmpty())
    }

    @Test
    fun tagComHashInvalidaMostraErro() = runTest(dispatcher) {
        val viewModel = viewModel().typed("#ABC")

        assertEquals(SearchOutcome.Failed(AppError.INVALID_TAG), viewModel.state.value.outcome)
    }

    @Test
    fun claPorTagComOuSemHashAbreATelaCla() = runTest(dispatcher) {
        val viewModel = viewModel(SearchMode.CLAN).typed("#2yquc8j0p")
        assertEquals(SearchTarget(FollowKind.CLAN, "2YQUC8J0P"), viewModel.state.value.open)
        viewModel.onOpened()

        viewModel.typed("2YQUC8J0P")

        assertEquals(SearchTarget(FollowKind.CLAN, "2YQUC8J0P"), viewModel.state.value.open)
        assertTrue(rig.api.searchCalls.isEmpty())
    }

    @Test
    fun nomeSoComLetrasDeTagBuscaPorNome() = runTest(dispatcher) {
        val viewModel = viewModel(SearchMode.CLAN).typed("PURY")

        assertEquals(listOf("PURY"), rig.api.searchCalls)
        assertNull(viewModel.state.value.open)
    }

    @Test
    fun nomeCurtoNaoVaiAoServidor() = runTest(dispatcher) {
        val viewModel = viewModel(SearchMode.CLAN).typed("ab")

        assertEquals(SearchOutcome.TooShort, viewModel.state.value.outcome)
        assertTrue(rig.api.searchCalls.isEmpty())
    }

    @Test
    fun nomeMostraOsResultadosESegueComAEstrela() = runTest(dispatcher) {
        rig.api.searches["Os Mito"] = ApiResult.Success(listOf(mito))
        val viewModel = viewModel(SearchMode.CLAN).typed("Os Mito")
        assertEquals(listOf(ClanResult(mito, following = false)), viewModel.results)

        viewModel.toggleFollow(viewModel.results.single())
        advanceUntilIdle()
        assertTrue(viewModel.results.single().following)
        assertEquals(listOf("8QU0VC"), viewModel.state.value.followed.map { it.tag })

        viewModel.toggleFollow(viewModel.results.single())
        advanceUntilIdle()
        assertFalse(viewModel.results.single().following)
        assertTrue(viewModel.state.value.followed.isEmpty())
    }

    @Test
    fun erroDaBuscaPorNome() = runTest(dispatcher) {
        rig.api.searches["Os Mito"] = ApiResult.Failure(AppError.NETWORK)

        val viewModel = viewModel(SearchMode.CLAN).typed("Os Mito")

        assertEquals(SearchOutcome.Failed(AppError.NETWORK), viewModel.state.value.outcome)
    }

    @Test
    fun limiteCheioAvisaENaoSegue() = runTest(dispatcher) {
        repeat(FollowRepository.LIMIT) { rig.follows.followClan(ClanSearchResultDto("C$it", "Clã", null, 1, 1)) }
        rig.api.searches["Os Mito"] = ApiResult.Success(listOf(mito))
        val viewModel = viewModel(SearchMode.CLAN).typed("Os Mito")

        viewModel.toggleFollow(viewModel.results.single())
        advanceUntilIdle()

        assertTrue(viewModel.state.value.followLimitReached)
        assertFalse(viewModel.results.single().following)
    }

    @Test
    fun trocarDeModoOuLimparVoltaParaSeguindoERecentes() = runTest(dispatcher) {
        val viewModel = viewModel().typed("Dantas")

        viewModel.selectMode(SearchMode.CLAN)
        advanceUntilIdle()
        assertEquals(SearchOutcome.None, viewModel.state.value.outcome)

        viewModel.typed("ab").clear()
        advanceUntilIdle()
        assertEquals(SearchOutcome.None, viewModel.state.value.outcome)
    }

    @Test
    fun editarERemoverDoSeguindo() = runTest(dispatcher) {
        rig.follows.followClan(mito)
        val viewModel = viewModel()

        viewModel.toggleEditing()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.editing)

        viewModel.unfollow(viewModel.state.value.followed.single())
        advanceUntilIdle()
        assertTrue(viewModel.state.value.followed.isEmpty())
        assertFalse(viewModel.state.value.editing, "lista vazia sai do modo de edição")
    }

    @Test
    fun recentesAparecemELimpam() = runTest(dispatcher) {
        rig.preferences.addRecentSearch(RecentSearch(FollowKind.PLAYER, "2PQ8LV9YC", "Dantas", 15))
        val viewModel = viewModel()
        assertEquals("Dantas", viewModel.state.value.recent.single().name)

        viewModel.clearRecent()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.recent.isEmpty())
    }
}
