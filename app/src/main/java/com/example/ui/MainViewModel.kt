package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppLanguage
import com.example.data.FileCategory
import com.example.data.FileManagerRepository
import com.example.data.MediaFileItem
import com.example.data.PreparedShareBundle
import com.example.data.SortOption
import com.example.data.StorageDiagnostics
import com.example.data.StorageInfo
import com.example.data.StoragePermissionState
import com.example.data.ThemeMode
import com.example.data.UserProfile
import com.example.data.UserPreferencesRepository
import com.example.data.ViewMode
import com.example.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileEditState(
    val displayName: String = "",
    val bio: String = "",
    val avatarColorIndex: Int = 0,
    val nameError: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

data class FileManagerUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val permissionState: StoragePermissionState = StoragePermissionState(),
    val storageInfo: StorageInfo? = null,
    val allFiles: List<MediaFileItem> = emptyList(),
    val safFiles: List<MediaFileItem> = emptyList(),
    val selectedCategory: FileCategory = FileCategory.ALL,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.DATE_DESC,
    val viewMode: ViewMode = ViewMode.LIST,
    val selectedFileIds: Set<Long> = emptySet(),
    val preparedShareBundle: PreparedShareBundle? = null,
    val activeFileDetails: MediaFileItem? = null,
    val errorMessage: String? = null,
    val storageDiagnostics: StorageDiagnostics? = null,
    val showDiagnosticsDialog: Boolean = false
) {
    val permissionGranted: Boolean
        get() = permissionState.hasAnyPermission

    val isCategoryPermitted: Boolean
        get() = permissionState.isCategoryPermitted(selectedCategory)

    val isCategoryAccessPartial: Boolean
        get() = permissionState.isCategoryAccessPartial(selectedCategory)

    val filteredFiles: List<MediaFileItem>
        get() {
            var result = if (selectedCategory == FileCategory.ALL) {
                allFiles
            } else {
                allFiles.filter { it.category == selectedCategory }
            }

            if (searchQuery.isNotBlank()) {
                val query = searchQuery.trim().lowercase()
                result = result.filter { it.name.lowercase().contains(query) }
            }

            return when (sortOption) {
                SortOption.DATE_DESC -> result.sortedByDescending { it.dateModified }
                SortOption.DATE_ASC -> result.sortedBy { it.dateModified }
                SortOption.NAME_ASC -> result.sortedBy { it.name.lowercase() }
                SortOption.NAME_DESC -> result.sortedByDescending { it.name.lowercase() }
                SortOption.SIZE_DESC -> result.sortedByDescending { it.size }
                SortOption.SIZE_ASC -> result.sortedBy { it.size }
                SortOption.TYPE -> result.sortedWith(compareBy({ it.category.name }, { it.name.lowercase() }))
            }
        }

    val selectedCount: Int
        get() = selectedFileIds.size

    val selectedTotalSize: Long
        get() = allFiles.filter { it.id in selectedFileIds }.sumOf { it.size }

    val formattedSelectedTotalSize: String
        get() = MediaFileItem.formatByteSize(selectedTotalSize)
}

class MainViewModel(
    application: Application,
    private val repository: UserPreferencesRepository,
    private val fileManagerRepository: FileManagerRepository = FileManagerRepository(application),
    private val appsRepository: com.example.data.InstalledAppsRepository = com.example.data.InstalledAppsRepository(application)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        UserPreferencesRepository(application),
        FileManagerRepository(application),
        com.example.data.InstalledAppsRepository(application)
    )

    constructor(
        application: Application,
        repository: UserPreferencesRepository
    ) : this(
        application,
        repository,
        FileManagerRepository(application),
        com.example.data.InstalledAppsRepository(application)
    )

    val themeMode: StateFlow<ThemeMode> = repository.themeMode
    val appLanguage: StateFlow<AppLanguage> = repository.appLanguage
    val userProfile: StateFlow<UserProfile> = repository.userProfile

    private val navStack = mutableListOf(Screen.HOME)

    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _profileEditState = MutableStateFlow(ProfileEditState())
    val profileEditState: StateFlow<ProfileEditState> = _profileEditState.asStateFlow()

    private val _showResetDialog = MutableStateFlow(false)
    val showResetDialog: StateFlow<Boolean> = _showResetDialog.asStateFlow()

    // File Manager State
    private val _fileManagerUiState = MutableStateFlow(FileManagerUiState())
    val fileManagerUiState: StateFlow<FileManagerUiState> = _fileManagerUiState.asStateFlow()

    // App Manager State
    private val _appManagerUiState = MutableStateFlow(com.example.data.AppManagerUiState())
    val appManagerUiState: StateFlow<com.example.data.AppManagerUiState> = _appManagerUiState.asStateFlow()

    init {
        initProfileEditState(repository.userProfile.value)
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            navStack.add(screen)
            _currentScreen.value = screen
            if (screen == Screen.PROFILE) {
                initProfileEditState(userProfile.value)
            } else if (screen == Screen.APP_MANAGER && _appManagerUiState.value.installedApps.isEmpty()) {
                loadInstalledApps()
            }
        }
    }

    fun navigateBack(): Boolean {
        if (navStack.size > 1) {
            navStack.removeAt(navStack.size - 1)
            _currentScreen.value = navStack.last()
            return true
        }
        return false
    }

    private fun initProfileEditState(profile: UserProfile) {
        _profileEditState.value = ProfileEditState(
            displayName = profile.displayName,
            bio = profile.bio,
            avatarColorIndex = profile.avatarColorIndex,
            nameError = null,
            isSaving = false,
            saveSuccess = false
        )
    }

    fun onDisplayNameChanged(name: String) {
        val error = when {
            name.isBlank() -> "EMPTY"
            name.length > 30 -> "TOO_LONG"
            else -> null
        }
        _profileEditState.value = _profileEditState.value.copy(
            displayName = name,
            nameError = error,
            saveSuccess = false
        )
    }

    fun onBioChanged(bio: String) {
        _profileEditState.value = _profileEditState.value.copy(
            bio = bio.take(120),
            saveSuccess = false
        )
    }

    fun onAvatarColorChanged(index: Int) {
        _profileEditState.value = _profileEditState.value.copy(
            avatarColorIndex = index.coerceIn(0, 4),
            saveSuccess = false
        )
    }

    fun saveProfile() {
        val current = _profileEditState.value
        val trimmedName = current.displayName.trim()
        if (trimmedName.isEmpty()) {
            _profileEditState.value = current.copy(nameError = "EMPTY")
            return
        }
        if (trimmedName.length > 30) {
            _profileEditState.value = current.copy(nameError = "TOO_LONG")
            return
        }

        viewModelScope.launch {
            _profileEditState.value = _profileEditState.value.copy(isSaving = true)
            val updatedProfile = UserProfile(
                displayName = trimmedName,
                bio = current.bio.trim(),
                avatarColorIndex = current.avatarColorIndex
            )
            repository.setUserProfile(updatedProfile)
            _profileEditState.value = _profileEditState.value.copy(
                isSaving = false,
                saveSuccess = true
            )
        }
    }

    fun dismissSaveSuccess() {
        _profileEditState.value = _profileEditState.value.copy(saveSuccess = false)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setAppLanguage(language)
        }
    }

    fun setShowResetDialog(show: Boolean) {
        _showResetDialog.value = show
    }

    fun resetDefaults() {
        viewModelScope.launch {
            repository.resetDefaults()
            _showResetDialog.value = false
            initProfileEditState(repository.userProfile.value)
        }
    }

    // ==========================================
    // File Manager Logic
    // ==========================================

    fun updatePermissions(permissions: StoragePermissionState) {
        _fileManagerUiState.value = _fileManagerUiState.value.copy(permissionState = permissions)
        loadStorageAndFiles(permissions)
    }

    fun loadStorageAndFiles(permissions: StoragePermissionState = _fileManagerUiState.value.permissionState) {
        viewModelScope.launch {
            _fileManagerUiState.value = _fileManagerUiState.value.copy(
                isLoading = true,
                permissionState = permissions,
                errorMessage = null
            )

            val storage = fileManagerRepository.getStorageInfo()

            try {
                val mediaFiles = fileManagerRepository.queryAccessibleFiles(permissions)
                // Combine with user-selected SAF files ensuring no duplicates
                val combinedFiles = mediaFiles.toMutableList()
                for (safFile in _fileManagerUiState.value.safFiles) {
                    if (combinedFiles.none { it.uri == safFile.uri }) {
                        combinedFiles.add(0, safFile)
                    }
                }

                val counts = FileCategory.entries.associateWith { cat ->
                    if (cat == FileCategory.ALL) combinedFiles.size else combinedFiles.count { it.category == cat }
                }

                val diagnostics = StorageDiagnostics(
                    androidVersion = "${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})",
                    permissionStatus = mapOf(
                        "Images" to permissions.imagesGranted,
                        "Videos" to permissions.videosGranted,
                        "Audio" to permissions.audioGranted,
                        "VisualUserSelected" to permissions.visualUserSelected,
                        "StorageLegacy" to permissions.storageGranted
                    ),
                    totalFilesQueried = combinedFiles.size,
                    countPerCategory = counts,
                    lastQueryTimestampMillis = System.currentTimeMillis(),
                    lastError = null,
                    isPartialAccess = permissions.isPartiallyGranted,
                    safSelectedFilesCount = _fileManagerUiState.value.safFiles.size
                )

                _fileManagerUiState.value = _fileManagerUiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    storageInfo = storage,
                    allFiles = combinedFiles,
                    storageDiagnostics = diagnostics,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _fileManagerUiState.value = _fileManagerUiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    storageInfo = storage,
                    errorMessage = e.localizedMessage ?: "Failed to query accessible files"
                )
            }
        }
    }

    fun refreshFiles() {
        _fileManagerUiState.value = _fileManagerUiState.value.copy(isRefreshing = true)
        loadStorageAndFiles(_fileManagerUiState.value.permissionState)
    }

    fun setDiagnosticsDialogVisible(visible: Boolean) {
        _fileManagerUiState.value = _fileManagerUiState.value.copy(showDiagnosticsDialog = visible)
    }

    fun loadStorageAndFiles(hasPermission: Boolean) {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            StoragePermissionState(
                imagesGranted = hasPermission,
                videosGranted = hasPermission,
                audioGranted = hasPermission,
                hasRequested = true
            )
        } else {
            StoragePermissionState(
                storageGranted = hasPermission,
                imagesGranted = hasPermission,
                videosGranted = hasPermission,
                audioGranted = hasPermission,
                hasRequested = true
            )
        }
        loadStorageAndFiles(permissions)
    }

    fun onPermissionResult(granted: Boolean) {
        loadStorageAndFiles(hasPermission = granted)
    }

    fun selectFileCategory(category: FileCategory) {
        _fileManagerUiState.value = _fileManagerUiState.value.copy(selectedCategory = category)
    }

    fun setSearchQuery(query: String) {
        _fileManagerUiState.value = _fileManagerUiState.value.copy(searchQuery = query)
    }

    fun setSortOption(sortOption: SortOption) {
        _fileManagerUiState.value = _fileManagerUiState.value.copy(sortOption = sortOption)
    }

    fun toggleViewMode() {
        val nextMode = if (_fileManagerUiState.value.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        _fileManagerUiState.value = _fileManagerUiState.value.copy(viewMode = nextMode)
    }

    fun toggleFileSelection(fileId: Long) {
        val currentSelected = _fileManagerUiState.value.selectedFileIds.toMutableSet()
        if (currentSelected.contains(fileId)) {
            currentSelected.remove(fileId)
        } else {
            currentSelected.add(fileId)
        }
        _fileManagerUiState.value = _fileManagerUiState.value.copy(selectedFileIds = currentSelected)
    }

    fun selectAllFiltered() {
        val filtered = _fileManagerUiState.value.filteredFiles
        val allIds = filtered.map { it.id }.toSet()
        _fileManagerUiState.value = _fileManagerUiState.value.copy(selectedFileIds = allIds)
    }

    fun clearFileSelection() {
        _fileManagerUiState.value = _fileManagerUiState.value.copy(selectedFileIds = emptySet())
    }

    fun prepareFilesForSharing() {
        val state = _fileManagerUiState.value
        val selectedItems = state.allFiles.filter { it.id in state.selectedFileIds }
        if (selectedItems.isNotEmpty()) {
            val totalBytes = selectedItems.sumOf { it.size }
            val bundle = PreparedShareBundle(
                files = selectedItems,
                totalSize = totalBytes,
                formattedTotalSize = MediaFileItem.formatByteSize(totalBytes)
            )
            _fileManagerUiState.value = _fileManagerUiState.value.copy(preparedShareBundle = bundle)
        }
    }

    fun clearPreparedBundle() {
        _fileManagerUiState.value = _fileManagerUiState.value.copy(preparedShareBundle = null)
    }

    fun setActiveFileDetails(file: MediaFileItem?) {
        _fileManagerUiState.value = _fileManagerUiState.value.copy(activeFileDetails = file)
    }

    fun addExternalFilesFromUris(uris: List<Uri>) {
        viewModelScope.launch {
            val resolvedFiles = mutableListOf<MediaFileItem>()
            for (uri in uris) {
                val item = fileManagerRepository.resolveFileFromUri(uri)
                if (item != null) {
                    resolvedFiles.add(item)
                }
            }

            if (resolvedFiles.isNotEmpty()) {
                val currentFiles = _fileManagerUiState.value.allFiles.toMutableList()
                val currentSaf = _fileManagerUiState.value.safFiles.toMutableList()
                val currentSelected = _fileManagerUiState.value.selectedFileIds.toMutableSet()
                for (file in resolvedFiles) {
                    if (currentFiles.none { it.uri == file.uri }) {
                        currentFiles.add(0, file)
                        currentSaf.add(file)
                    }
                    currentSelected.add(file.id)
                }
                _fileManagerUiState.value = _fileManagerUiState.value.copy(
                    allFiles = currentFiles,
                    safFiles = currentSaf,
                    selectedFileIds = currentSelected
                )
            }
        }
    }

    fun addExternalFilesFromTreeUri(treeUri: Uri) {
        viewModelScope.launch {
            _fileManagerUiState.value = _fileManagerUiState.value.copy(isLoading = true)
            val files = fileManagerRepository.resolveFilesFromTreeUri(treeUri)
            if (files.isNotEmpty()) {
                val currentFiles = _fileManagerUiState.value.allFiles.toMutableList()
                val currentSaf = _fileManagerUiState.value.safFiles.toMutableList()
                for (file in files) {
                    if (currentFiles.none { it.uri == file.uri }) {
                        currentFiles.add(0, file)
                        currentSaf.add(file)
                    }
                }
                _fileManagerUiState.value = _fileManagerUiState.value.copy(
                    isLoading = false,
                    allFiles = currentFiles,
                    safFiles = currentSaf
                )
            } else {
                _fileManagerUiState.value = _fileManagerUiState.value.copy(isLoading = false)
            }
        }
    }

    // ==========================================
    // App Manager Logic
    // ==========================================

    fun loadInstalledApps() {
        viewModelScope.launch {
            _appManagerUiState.value = _appManagerUiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val apps = appsRepository.getInstalledApps()
                _appManagerUiState.value = _appManagerUiState.value.copy(
                    isLoading = false,
                    installedApps = apps,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _appManagerUiState.value = _appManagerUiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to query installed apps"
                )
            }
        }
    }

    fun toggleAppSelection(packageName: String) {
        val current = _appManagerUiState.value.selectedPackageNames.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _appManagerUiState.value = _appManagerUiState.value.copy(selectedPackageNames = current)
    }

    fun selectAllApps() {
        val all = _appManagerUiState.value.filteredApps.map { it.packageName }.toSet()
        _appManagerUiState.value = _appManagerUiState.value.copy(selectedPackageNames = all)
    }

    fun clearAppSelection() {
        _appManagerUiState.value = _appManagerUiState.value.copy(
            selectedPackageNames = emptySet(),
            preparedAppShareNotice = null
        )
    }

    fun removeSelectedApp(packageName: String) {
        val current = _appManagerUiState.value.selectedPackageNames.toMutableSet()
        current.remove(packageName)
        _appManagerUiState.value = _appManagerUiState.value.copy(selectedPackageNames = current)
    }

    fun setAppSearchQuery(query: String) {
        _appManagerUiState.value = _appManagerUiState.value.copy(searchQuery = query)
    }

    fun setAppManagerTab(tab: com.example.data.AppManagerTab) {
        _appManagerUiState.value = _appManagerUiState.value.copy(activeTab = tab)
    }

    fun prepareSelectedAppsForSharing() {
        val state = _appManagerUiState.value
        val count = state.selectedCount
        if (count > 0) {
            val notice = "$count app(s) staged (${state.formattedSelectedTotalSize}). Peer APK transfer pipeline is pending Phase 3 networking."
            _appManagerUiState.value = state.copy(preparedAppShareNotice = notice)
        }
    }

    fun clearPreparedAppShareNotice() {
        _appManagerUiState.value = _appManagerUiState.value.copy(preparedAppShareNotice = null)
    }
}
