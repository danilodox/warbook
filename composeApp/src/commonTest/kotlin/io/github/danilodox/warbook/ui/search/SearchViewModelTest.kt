package io.github.danilodox.warbook.ui.search

import io.github.danilodox.warbook.api.ClanDto
import io.github.danilodox.warbook.api.CurrentWarDto
import io.github.danilodox.warbook.api.CwlDto
import io.github.danilodox.warbook.api.PlayerDto
import io.github.danilodox.warbook.data.ApiResult
import io.github.danilodox.warbook.data.AppError
import io.github.danilodox.warbook.data.WarbookRepository
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
class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val player = PlayerDto(
        tag = "2PQ8LV9YC",
        name = "Dantas",
        townHallLevel = 15,
        expLevel = 212,
        trophies = 4812,
        warStars = 1384,
        attackWins = 47,
        donations = 1240,
        donationsReceived = 860,
    )

    private class FakeRepository(var result: ApiResult<PlayerDto>) : WarbookRepository {
        val searched = mutableListOf<String>()

        override suspend fun player(rawTag: String): ApiResult<PlayerDto> = result.also { searched += rawTag }

        override suspend fun clan(rawTag: String): ApiResult<ClanDto> = error("não usado")

        override suspend fun searchClans(rawName: String) = error("não usado")

        override suspend fun currentWar(rawClanTag: String): ApiResult<CurrentWarDto> = error("não usado")

        override suspend fun currentCwl(rawClanTag: String): ApiResult<CwlDto> = error("não usado")

        override suspend fun verify(rawTag: String, gameToken: String): ApiResult<String> = error("não usado")
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun comecaParado() {
        val viewModel = SearchViewModel(FakeRepository(ApiResult.Success(player)))

        assertEquals(SearchState.Idle, viewModel.state.value)
    }

    @Test
    fun mostraCarregandoEDepoisOJogador() = runTest(dispatcher) {
        val repository = FakeRepository(ApiResult.Success(player))
        val viewModel = SearchViewModel(repository)

        viewModel.search("#2pq8lv9yc")
        assertEquals(SearchState.Loading, viewModel.state.value)

        advanceUntilIdle()
        assertEquals(SearchState.Found(player), viewModel.state.value)
        assertEquals(listOf("#2pq8lv9yc"), repository.searched)
    }

    @Test
    fun mostraOErroTipado() = runTest(dispatcher) {
        val viewModel = SearchViewModel(FakeRepository(ApiResult.Failure(AppError.NOT_FOUND)))

        viewModel.search("2PQ8LV9YC")
        advanceUntilIdle()

        assertEquals(SearchState.Failed(AppError.NOT_FOUND), viewModel.state.value)
    }

    @Test
    fun soOfereceTentarDeNovoQuandoPodeMudar() {
        assertTrue(AppError.NETWORK.canRetry)
        assertTrue(AppError.SERVER.canRetry)
        assertTrue(AppError.RATE_LIMITED.canRetry)
        assertFalse(AppError.INVALID_TAG.canRetry)
        assertFalse(AppError.NOT_FOUND.canRetry)
    }
}
