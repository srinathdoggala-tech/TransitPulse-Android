package com.mishr.habittracker.feature.settings

import com.mishr.habittracker.core.datastore.UserPreferencesDataStore
import com.mishr.habittracker.domain.model.AuthState
import com.mishr.habittracker.domain.model.Category
import com.mishr.habittracker.domain.model.ThemeMode
import com.mishr.habittracker.domain.repository.CategoryRepository
import com.mishr.habittracker.domain.repository.SyncRepository
import com.mishr.habittracker.domain.result.AppResult
import com.mishr.habittracker.domain.usecase.ObserveAuthStateUseCase
import com.mishr.habittracker.domain.usecase.SignOutUseCase
import com.mishr.habittracker.domain.usecase.DeleteAccountUseCase
import com.mishr.habittracker.domain.usecase.SignInUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val mockDataStore: UserPreferencesDataStore = mockk()
    private val mockCategoryRepo: CategoryRepository = mockk(relaxed = true)
    private val mockSyncRepo: SyncRepository = mockk(relaxed = true)
    private val mockObserveAuth: ObserveAuthStateUseCase = mockk(relaxed = true)
    private val mockSignOut: SignOutUseCase = mockk(relaxed = true)
    private val mockDeleteAccount: DeleteAccountUseCase = mockk(relaxed = true)
    private val mockSignIn: SignInUseCase = mockk(relaxed = true)

    private val themeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    private val langFlow = MutableStateFlow("en")
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val authFlow = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    private val syncFlow = MutableStateFlow<Int>(0)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { mockDataStore.themeMode } returns themeFlow
        every { mockDataStore.languageCode } returns langFlow
        every { mockCategoryRepo.getAllCategories() } returns categoriesFlow
        every { mockObserveAuth() } returns authFlow
        every { mockSyncRepo.getPendingSyncCount() } returns syncFlow

        viewModel = SettingsViewModel(
            mockDataStore,
            mockCategoryRepo,
            mockSyncRepo,
            mockObserveAuth,
            mockSignOut,
            mockDeleteAccount,
            mockSignIn
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `State reflects datastore flows`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }

        themeFlow.value = ThemeMode.DARK
        langFlow.value = "es"

        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ThemeMode.DARK, state.themeMode)
        assertEquals("es", state.languageCode)
    }

    @Test
    fun `Sign Out Guard state verification - pendingSyncCount updates`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }

        syncFlow.value = 5
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(5, state.pendingSyncCount)
    }

    @Test
    fun `SetThemeMode calls datastore`() = runTest {
        coEvery { mockDataStore.setThemeMode(any()) } returns AppResult.Success(Unit)

        viewModel.processIntent(SettingsIntent.SetThemeMode(ThemeMode.LIGHT))
        testScheduler.advanceUntilIdle()

        coVerify { mockDataStore.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun `SetLanguage calls datastore`() = runTest {
        coEvery { mockDataStore.setLanguageCode(any()) } returns AppResult.Success(Unit)

        viewModel.processIntent(SettingsIntent.SetLanguage("fr"))
        testScheduler.advanceUntilIdle()

        coVerify { mockDataStore.setLanguageCode("fr") }
    }
}
