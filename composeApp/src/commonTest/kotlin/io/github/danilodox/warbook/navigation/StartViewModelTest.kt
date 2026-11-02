package io.github.danilodox.warbook.navigation

import io.github.danilodox.warbook.data.FakeAccountDao
import io.github.danilodox.warbook.data.FakeAppPreferences
import io.github.danilodox.warbook.data.FakePlayerCacheDao
import io.github.danilodox.warbook.data.FakeWarbookRepository
import io.github.danilodox.warbook.data.RoomAccountsRepository
import io.github.danilodox.warbook.data.RoomProfileRepository
import io.github.danilodox.warbook.data.testPlayer
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class StartViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeWarbookRepository()
    private val accountDao = FakeAccountDao()
    private val profiles = RoomProfileRepository(FakePlayerCacheDao(), accountDao, api) { 1L }
    private val accounts = RoomAccountsRepository(accountDao, profiles, api) { 1L }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun regraDaPrimeiraTela() {
        assertEquals(Route.Onboarding, startRoute(hasAccounts = false, exploreMode = false))
        assertEquals(Route.Main, startRoute(hasAccounts = false, exploreMode = true))
        assertEquals(Route.Main, startRoute(hasAccounts = true, exploreMode = false))
        assertEquals(Route.Main, startRoute(hasAccounts = true, exploreMode = true))
    }

    @Test
    fun semContaESemExplorarAbreOOnboarding() = runTest(dispatcher) {
        val viewModel = StartViewModel(accounts, FakeAppPreferences())
        assertNull(viewModel.start.value)

        advanceUntilIdle()

        assertEquals(Route.Onboarding, viewModel.start.value)
    }

    @Test
    fun comContaAbreAsAbas() = runTest(dispatcher) {
        accounts.add(testPlayer())

        val viewModel = StartViewModel(accounts, FakeAppPreferences())
        advanceUntilIdle()

        assertEquals(Route.Main, viewModel.start.value)
    }

    @Test
    fun soExplorarAbreAsAbas() = runTest(dispatcher) {
        val viewModel = StartViewModel(accounts, FakeAppPreferences(explore = true))
        advanceUntilIdle()

        assertEquals(Route.Main, viewModel.start.value)
    }
}
