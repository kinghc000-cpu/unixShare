package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppLanguage
import com.example.data.ThemeMode
import com.example.data.UserProfile
import com.example.data.UserPreferencesRepository
import com.example.ui.MainViewModel
import com.example.ui.navigation.Screen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UnexSharePersistenceTest {

    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: UserPreferencesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        // Clear preferences before each test
        context.getSharedPreferences(UserPreferencesRepository.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        repository = UserPreferencesRepository(context, testDispatcher)
    }

    @Test
    fun verifyAppNameResource() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Unex SHARE", appName)
    }

    @Test
    fun verifyArabicStringsResource() {
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(java.util.Locale.forLanguageTag("ar"))
        val arContext = context.createConfigurationContext(config)
        val sendString = arContext.getString(R.string.action_send)
        assertEquals("إرسال", sendString)
        val receiveString = arContext.getString(R.string.action_receive)
        assertEquals("استقبال", receiveString)
    }

    @Test
    fun themeModePersistence_defaultsToSystem_andPersistsChanges() = runTest(testDispatcher) {
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.value)

        repository.setThemeMode(ThemeMode.DARK)
        testScheduler.advanceUntilIdle()
        assertEquals(ThemeMode.DARK, repository.themeMode.value)

        // Create new repository instance to simulate app restart
        val reloadedRepository = UserPreferencesRepository(context, testDispatcher)
        assertEquals(ThemeMode.DARK, reloadedRepository.themeMode.value)

        repository.setThemeMode(ThemeMode.LIGHT)
        testScheduler.advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, repository.themeMode.value)
    }

    @Test
    fun appLanguagePersistence_defaultsToSystem_andPersistsChanges() = runTest(testDispatcher) {
        assertEquals(AppLanguage.SYSTEM, repository.appLanguage.value)

        repository.setAppLanguage(AppLanguage.AR)
        testScheduler.advanceUntilIdle()
        assertEquals(AppLanguage.AR, repository.appLanguage.value)

        // Simulate app restart
        val reloadedRepository = UserPreferencesRepository(context, testDispatcher)
        assertEquals(AppLanguage.AR, reloadedRepository.appLanguage.value)

        repository.setAppLanguage(AppLanguage.EN)
        testScheduler.advanceUntilIdle()
        assertEquals(AppLanguage.EN, repository.appLanguage.value)
    }

    @Test
    fun userProfilePersistence_defaultsAndUpdates() = runTest(testDispatcher) {
        val defaultProfile = repository.userProfile.value
        assertEquals(UserProfile.DEFAULT_NAME, defaultProfile.displayName)
        assertEquals(UserProfile.DEFAULT_BIO, defaultProfile.bio)
        assertEquals(0, defaultProfile.avatarColorIndex)

        val updatedProfile = UserProfile(
            displayName = "Pixel 8 Pro",
            bio = "Active transfer device in office",
            avatarColorIndex = 2
        )
        repository.setUserProfile(updatedProfile)
        testScheduler.advanceUntilIdle()
        assertEquals("Pixel 8 Pro", repository.userProfile.value.displayName)
        assertEquals("Active transfer device in office", repository.userProfile.value.bio)
        assertEquals(2, repository.userProfile.value.avatarColorIndex)

        // Simulate restart
        val reloadedRepository = UserPreferencesRepository(context, testDispatcher)
        assertEquals("Pixel 8 Pro", reloadedRepository.userProfile.value.displayName)
        assertEquals("Active transfer device in office", reloadedRepository.userProfile.value.bio)
        assertEquals(2, reloadedRepository.userProfile.value.avatarColorIndex)
    }

    @Test
    fun resetDefaults_clearsAllSettingsAndProfile() = runTest(testDispatcher) {
        repository.setThemeMode(ThemeMode.DARK)
        repository.setAppLanguage(AppLanguage.AR)
        repository.setUserProfile(UserProfile("Custom Device", "Custom Bio", 3))
        testScheduler.advanceUntilIdle()

        repository.resetDefaults()
        testScheduler.advanceUntilIdle()

        assertEquals(ThemeMode.SYSTEM, repository.themeMode.value)
        assertEquals(AppLanguage.SYSTEM, repository.appLanguage.value)
        assertEquals(UserProfile.DEFAULT_NAME, repository.userProfile.value.displayName)
    }

    @Test
    fun mainViewModel_navigationStackAndBackNavigation() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app, repository)

        assertEquals(Screen.HOME, viewModel.currentScreen.value)

        // Navigate to PROFILE
        viewModel.navigateTo(Screen.PROFILE)
        assertEquals(Screen.PROFILE, viewModel.currentScreen.value)

        // Navigate to SETTINGS
        viewModel.navigateTo(Screen.SETTINGS)
        assertEquals(Screen.SETTINGS, viewModel.currentScreen.value)

        // Navigate back -> PROFILE
        val poppedSettings = viewModel.navigateBack()
        assertTrue(poppedSettings)
        assertEquals(Screen.PROFILE, viewModel.currentScreen.value)

        // Navigate back -> HOME
        val poppedProfile = viewModel.navigateBack()
        assertTrue(poppedProfile)
        assertEquals(Screen.HOME, viewModel.currentScreen.value)

        // Back from HOME should not pop further
        val poppedHome = viewModel.navigateBack()
        assertFalse(poppedHome)
        assertEquals(Screen.HOME, viewModel.currentScreen.value)
    }

    @Test
    fun mainViewModel_profileEditingValidation() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app, repository)

        viewModel.navigateTo(Screen.PROFILE)

        // Empty name validation
        viewModel.onDisplayNameChanged("")
        assertEquals("EMPTY", viewModel.profileEditState.value.nameError)

        viewModel.onDisplayNameChanged("   ")
        assertEquals("EMPTY", viewModel.profileEditState.value.nameError)

        // Too long name validation (> 30 characters)
        viewModel.onDisplayNameChanged("This device name is definitely way too long to be valid")
        assertEquals("TOO_LONG", viewModel.profileEditState.value.nameError)

        // Valid name
        viewModel.onDisplayNameChanged("Galaxy S24 Ultra")
        assertNull(viewModel.profileEditState.value.nameError)
    }

    @Test
    fun mainActivity_launchesSuccessfully_andInitializesViewModel() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        assertFalse(activity.isFinishing)
        controller.destroy()
    }
}
