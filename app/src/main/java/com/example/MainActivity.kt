package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.navigation.Screen
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.UnexShareTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val profileEditState by viewModel.profileEditState.collectAsStateWithLifecycle()
            val showResetDialog by viewModel.showResetDialog.collectAsStateWithLifecycle()
            val fileManagerUiState by viewModel.fileManagerUiState.collectAsStateWithLifecycle()

            val baseContext = LocalContext.current

            val requiredPermissions = remember {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO
                    )
                } else {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                val granted = results.values.any { it }
                viewModel.onPermissionResult(granted)
            }

            val safLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenMultipleDocuments()
            ) { uris ->
                if (uris.isNotEmpty()) {
                    viewModel.addExternalFilesFromUris(uris)
                }
            }

            LaunchedEffect(Unit) {
                val hasPermission = requiredPermissions.any {
                    ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
                }
                viewModel.onPermissionResult(hasPermission)
            }

            val localizedContext = remember(appLanguage, baseContext) {
                createLocalizedContext(baseContext, appLanguage)
            }

            val layoutDirection = when (appLanguage) {
                AppLanguage.AR -> LayoutDirection.Rtl
                AppLanguage.EN -> LayoutDirection.Ltr
                AppLanguage.SYSTEM -> {
                    val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        baseContext.resources.configuration.locales[0]
                    } else {
                        @Suppress("DEPRECATION")
                        baseContext.resources.configuration.locale
                    }
                    if (systemLocale.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalLayoutDirection provides layoutDirection,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                UnexShareTheme(themeMode = themeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "screen_transition"
                        ) { screen ->
                            when (screen) {
                                Screen.HOME -> {
                                    HomeScreen(
                                        userProfile = userProfile,
                                        onNavigateToProfile = { viewModel.navigateTo(Screen.PROFILE) },
                                        onNavigateToSettings = { viewModel.navigateTo(Screen.SETTINGS) },
                                        onNavigateToFileManager = { viewModel.navigateTo(Screen.FILE_MANAGER) }
                                    )
                                }
                                Screen.FILE_MANAGER -> {
                                    FileManagerScreen(
                                        state = fileManagerUiState,
                                        onRequestPermissions = { permissionLauncher.launch(requiredPermissions) },
                                        onBrowseSafFiles = { safLauncher.launch(arrayOf("*/*")) },
                                        onCategorySelect = viewModel::selectFileCategory,
                                        onSearchQueryChange = viewModel::setSearchQuery,
                                        onSortSelect = viewModel::setSortOption,
                                        onToggleViewMode = viewModel::toggleViewMode,
                                        onToggleFileSelect = viewModel::toggleFileSelection,
                                        onSelectAll = viewModel::selectAllFiltered,
                                        onClearSelect = viewModel::clearFileSelection,
                                        onPrepareShare = viewModel::prepareFilesForSharing,
                                        onClearPreparedBundle = viewModel::clearPreparedBundle,
                                        onShowFileDetails = viewModel::setActiveFileDetails,
                                        onNavigateBack = { viewModel.navigateBack() }
                                    )
                                }
                                Screen.PROFILE -> {
                                    ProfileScreen(
                                        state = profileEditState,
                                        onNameChange = viewModel::onDisplayNameChanged,
                                        onBioChange = viewModel::onBioChanged,
                                        onAvatarColorChange = viewModel::onAvatarColorChanged,
                                        onSaveProfile = viewModel::saveProfile,
                                        onDismissSaveSuccess = viewModel::dismissSaveSuccess,
                                        onNavigateBack = { viewModel.navigateBack() }
                                    )
                                }
                                Screen.SETTINGS -> {
                                    SettingsScreen(
                                        currentTheme = themeMode,
                                        currentLanguage = appLanguage,
                                        showResetDialog = showResetDialog,
                                        onThemeSelect = viewModel::setThemeMode,
                                        onLanguageSelect = viewModel::setAppLanguage,
                                        onShowResetDialog = viewModel::setShowResetDialog,
                                        onConfirmReset = viewModel::resetDefaults,
                                        onNavigateBack = { viewModel.navigateBack() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createLocalizedContext(context: Context, language: AppLanguage): Context {
        val targetLocale = when (language) {
            AppLanguage.AR -> Locale.forLanguageTag("ar")
            AppLanguage.EN -> Locale.forLanguageTag("en")
            AppLanguage.SYSTEM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ResourcesSystemCompat.getSystemLocale(context)
                } else {
                    @Suppress("DEPRECATION")
                    context.resources.configuration.locale
                }
            }
        }
        val config = Configuration(context.resources.configuration)
        config.setLocale(targetLocale)
        config.setLayoutDirection(targetLocale)
        return context.createConfigurationContext(config)
    }

    private object ResourcesSystemCompat {
        fun getSystemLocale(context: Context): Locale {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
        }
    }
}
