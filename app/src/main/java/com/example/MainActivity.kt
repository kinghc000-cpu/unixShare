package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppLanguage
import com.example.data.FileCategory
import com.example.data.StoragePermissionState
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
            var pendingRationaleCategory by remember { mutableStateOf<FileCategory?>(null) }
            var hasRequestedPermissionsOnOpen by remember { mutableStateOf(false) }

            val openAppSettings = remember {
                {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                val current = StoragePermissionState.checkDevicePermissions(this@MainActivity)
                val anyPermanentlyDenied = results.any { (perm, granted) ->
                    !granted && !ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, perm)
                }
                val updated = current.copy(
                    isPermanentlyDenied = anyPermanentlyDenied && !current.hasAnyPermission,
                    hasRequested = true
                )
                viewModel.updatePermissions(updated)
            }

            val requestCategoryPermissions = remember {
                { category: FileCategory ->
                    val required = StoragePermissionState.getRequiredPermissionsForCategory(category)
                    val missing = required.filter {
                        ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (missing.isEmpty()) {
                        val current = StoragePermissionState.checkDevicePermissions(this@MainActivity)
                        viewModel.updatePermissions(current)
                    } else {
                        val shouldShowRationale = missing.any {
                            ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, it)
                        }
                        if (shouldShowRationale) {
                            pendingRationaleCategory = category
                        } else {
                            permissionLauncher.launch(missing.toTypedArray())
                        }
                    }
                }
            }

            val safLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenMultipleDocuments()
            ) { uris ->
                if (uris.isNotEmpty()) {
                    viewModel.addExternalFilesFromUris(uris)
                }
            }

            // Sync permissions on resume (e.g. returning from system Settings)
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        val current = StoragePermissionState.checkDevicePermissions(this@MainActivity)
                        viewModel.updatePermissions(current)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            // Auto-request or check on opening File Manager
            LaunchedEffect(currentScreen) {
                if (currentScreen == Screen.FILE_MANAGER) {
                    val current = StoragePermissionState.checkDevicePermissions(this@MainActivity)
                    viewModel.updatePermissions(current)

                    if (!current.hasAnyPermission && !hasRequestedPermissionsOnOpen) {
                        hasRequestedPermissionsOnOpen = true
                        requestCategoryPermissions(viewModel.fileManagerUiState.value.selectedCategory)
                    }
                }
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
                                        onRequestPermissions = { requestCategoryPermissions(fileManagerUiState.selectedCategory) },
                                        onRequestCategoryPermission = requestCategoryPermissions,
                                        onOpenAppSettings = openAppSettings,
                                        onBrowseSafFiles = { safLauncher.launch(arrayOf("*/*")) },
                                        onCategorySelect = { cat ->
                                            viewModel.selectFileCategory(cat)
                                            if (!viewModel.fileManagerUiState.value.permissionState.isCategoryPermitted(cat)) {
                                                requestCategoryPermissions(cat)
                                            }
                                        },
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

                        // Permission Rationale Explanation Dialog
                        pendingRationaleCategory?.let { category ->
                            val catName = when (category) {
                                FileCategory.ALL -> stringResource(R.string.category_all)
                                FileCategory.IMAGES -> stringResource(R.string.category_images)
                                FileCategory.VIDEOS -> stringResource(R.string.category_videos)
                                FileCategory.AUDIO -> stringResource(R.string.category_audio)
                                FileCategory.DOCUMENTS -> stringResource(R.string.category_documents)
                                FileCategory.DOWNLOADS -> stringResource(R.string.category_downloads)
                                FileCategory.OTHER -> stringResource(R.string.category_other)
                            }

                            AlertDialog(
                                onDismissRequest = { pendingRationaleCategory = null },
                                title = {
                                    Text(
                                        text = stringResource(R.string.permission_rationale_title),
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Text(
                                        text = stringResource(R.string.permission_rationale_desc, catName),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val missing = StoragePermissionState.getRequiredPermissionsForCategory(category).filter {
                                                ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                                            }
                                            pendingRationaleCategory = null
                                            if (missing.isNotEmpty()) {
                                                permissionLauncher.launch(missing.toTypedArray())
                                            }
                                        },
                                        modifier = Modifier.testTag("btn_rationale_confirm")
                                    ) {
                                        Text(stringResource(R.string.permission_grant_button))
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(
                                        onClick = { pendingRationaleCategory = null },
                                        modifier = Modifier.testTag("btn_rationale_dismiss")
                                    ) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }
                            )
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
