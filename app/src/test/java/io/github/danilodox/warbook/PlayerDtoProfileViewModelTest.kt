package io.github.danilodox.warbook

import io.github.danilodox.warbook.data.datastore.SecureTokenProvider
import io.github.danilodox.warbook.domain.util.Result
import io.github.danilodox.warbook.domain.model.Clan
import io.github.danilodox.warbook.domain.model.Player
import io.github.danilodox.warbook.domain.usecase.GetPlayerProfileUseCase
import io.github.danilodox.warbook.domain.usecase.PlayerProfileData
import io.github.danilodox.warbook.ui.home.HomeViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getPlayerProfileUseCase: GetPlayerProfileUseCase
    private lateinit var secureTokenProvider: SecureTokenProvider

    private lateinit var viewModel: HomeViewModel

    private val testPlayerTag = "#123ABC"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)


        getPlayerProfileUseCase = mockk()
        secureTokenProvider = mockk()


        // Simulate SavedStateHandle providing the playerTag
        every { secureTokenProvider.getSavedTag() } returns testPlayerTag


        // Instantiate the ViewModel with correct dependencies
        viewModel = HomeViewModel(
            getPlayerProfileUseCase = getPlayerProfileUseCase,
            secureTokenProvider = secureTokenProvider,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should update uiState with Player on successful data load`() = runTest {
        // Arrange
        // Create test data using domain models
        val fakeClan = Clan(tag = "#CLAN123", name = "Clashers", clanLevel = 10)
        val fakePlayer = Player(
            tag = testPlayerTag,
            name = "Danilo",
            expLevel = 100,
            trophies = 5000,
            warStars = 100,
            donations = 50,
            townHallLevel = 15,
            clan = fakeClan,
            troops = emptyList(),
            heroes = emptyList(),
            spells = emptyList()
        )

        val profileData = PlayerProfileData(player = fakePlayer, isFavorite = false) // isFavorite doesn't matter here

        coEvery { getPlayerProfileUseCase(testPlayerTag) } returns Result.Success(profileData)

        // Act
        // Data loading happens in ViewModel's init block during setup(),
        // so we just need to wait for coroutines to finish.
        advanceUntilIdle()

        // Assert
        // Verify the final UI state
        val finalState = viewModel.uiState.value

        assertEquals(false, finalState.isLoading)
        assertEquals(fakePlayer, finalState.player)
        assertEquals(null, finalState.error)
    }

    @Test
    fun `init should update uiState with error on data load failure`() = runTest {
        // Arrange
        val errorMessage = "Player not found"

        // Configure UseCase mock to return an error result
        coEvery { getPlayerProfileUseCase(testPlayerTag) } returns Result.Error(errorMessage)

        // Act
        advanceUntilIdle() // Wait for init block coroutine

        // Assert
        val finalState = viewModel.uiState.value

        assertEquals(false, finalState.isLoading)
        assertEquals(errorMessage, finalState.error)
        assertEquals(null, finalState.player)
    }

    @Test
    fun `init should update uiState with error if playerTag is missing`() = runTest {

        // Override the setup mock for SavedStateHandle to return null
        every { secureTokenProvider.getSavedTag() } returns null

        // Re-initialize ViewModel with the failing SavedStateHandle
        viewModel = HomeViewModel(getPlayerProfileUseCase, secureTokenProvider)

        // Act
        advanceUntilIdle()

        // Assert
        val finalState = viewModel.uiState.value

        assertEquals(false, finalState.isLoading)
        assertEquals("No player tag found.", finalState.error)
        assertEquals(null, finalState.player)
    }
}