package com.example

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.FileCategory
import com.example.data.FileManagerRepository
import com.example.data.MediaFileItem
import com.example.data.SortOption
import com.example.data.UserPreferencesRepository
import com.example.data.ViewMode
import com.example.ui.FileManagerUiState
import com.example.ui.MainViewModel
import com.example.ui.navigation.Screen
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FileManagerTest {

    private lateinit var app: Application
    private lateinit var sampleFiles: List<MediaFileItem>

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext<Application>()
        sampleFiles = listOf(
            MediaFileItem(
                id = 1L,
                uri = Uri.parse("content://media/external/images/media/1"),
                name = "sunset_photo.jpg",
                size = 2_500_000L, // ~2.4 MB
                mimeType = "image/jpeg",
                dateModified = 1700000000L,
                category = FileCategory.IMAGES
            ),
            MediaFileItem(
                id = 2L,
                uri = Uri.parse("content://media/external/video/media/2"),
                name = "vacation_clip.mp4",
                size = 50_000_000L, // ~47.7 MB
                mimeType = "video/mp4",
                dateModified = 1710000000L,
                category = FileCategory.VIDEOS
            ),
            MediaFileItem(
                id = 3L,
                uri = Uri.parse("content://media/external/file/media/3"),
                name = "project_brief.pdf",
                size = 450_000L, // ~439.5 KB
                mimeType = "application/pdf",
                dateModified = 1690000000L,
                category = FileCategory.DOCUMENTS
            ),
            MediaFileItem(
                id = 4L,
                uri = Uri.parse("content://media/external/audio/media/4"),
                name = "podcast_ep1.mp3",
                size = 12_000_000L, // ~11.4 MB
                mimeType = "audio/mpeg",
                dateModified = 1705000000L,
                category = FileCategory.AUDIO
            )
        )
    }

    @Test
    fun formatByteSize_formatsProperly() {
        assertEquals("0 B", MediaFileItem.formatByteSize(0L))
        assertEquals("512 B", MediaFileItem.formatByteSize(512L))
        assertEquals("1.0 KB", MediaFileItem.formatByteSize(1024L))
        assertEquals("1.0 MB", MediaFileItem.formatByteSize(1024L * 1024L))
        assertEquals("1.0 GB", MediaFileItem.formatByteSize(1024L * 1024L * 1024L))
    }

    @Test
    fun resolveCategoryFromMime_identifiesCorrectly() {
        assertEquals(FileCategory.IMAGES, FileManagerRepository.resolveCategoryFromMime("image/png", "test.png"))
        assertEquals(FileCategory.VIDEOS, FileManagerRepository.resolveCategoryFromMime("video/mp4", "clip.mp4"))
        assertEquals(FileCategory.AUDIO, FileManagerRepository.resolveCategoryFromMime("audio/mp3", "song.mp3"))
        assertEquals(FileCategory.DOCUMENTS, FileManagerRepository.resolveCategoryFromMime("application/pdf", "document.pdf"))
        assertEquals(FileCategory.DOWNLOADS, FileManagerRepository.resolveCategoryFromMime("application/zip", "archive.zip"))
        assertEquals(FileCategory.OTHER, FileManagerRepository.resolveCategoryFromMime("application/unknown", "binary.bin"))
    }

    @Test
    fun fileManagerUiState_categoryFiltering() {
        val state = FileManagerUiState(
            allFiles = sampleFiles,
            selectedCategory = FileCategory.IMAGES
        )
        val filtered = state.filteredFiles
        assertEquals(1, filtered.size)
        assertEquals("sunset_photo.jpg", filtered[0].name)
    }

    @Test
    fun fileManagerUiState_searchQueryFiltering() {
        val state = FileManagerUiState(
            allFiles = sampleFiles,
            selectedCategory = FileCategory.ALL,
            searchQuery = "brief"
        )
        val filtered = state.filteredFiles
        assertEquals(1, filtered.size)
        assertEquals("project_brief.pdf", filtered[0].name)
    }

    @Test
    fun fileManagerUiState_sortingByName() {
        val state = FileManagerUiState(
            allFiles = sampleFiles,
            selectedCategory = FileCategory.ALL,
            sortOption = SortOption.NAME_ASC
        )
        val filtered = state.filteredFiles
        assertEquals("podcast_ep1.mp3", filtered[0].name)
        assertEquals("vacation_clip.mp4", filtered.last().name)
    }

    @Test
    fun fileManagerUiState_sortingBySize() {
        val state = FileManagerUiState(
            allFiles = sampleFiles,
            selectedCategory = FileCategory.ALL,
            sortOption = SortOption.SIZE_DESC
        )
        val filtered = state.filteredFiles
        assertEquals("vacation_clip.mp4", filtered[0].name)
        assertEquals("project_brief.pdf", filtered.last().name)
    }

    @Test
    fun fileManagerUiState_sortingByDate() {
        val state = FileManagerUiState(
            allFiles = sampleFiles,
            selectedCategory = FileCategory.ALL,
            sortOption = SortOption.DATE_DESC
        )
        val filtered = state.filteredFiles
        assertEquals("vacation_clip.mp4", filtered[0].name) // 1710000000L
        assertEquals("project_brief.pdf", filtered.last().name) // 1690000000L
    }

    @Test
    fun fileManagerUiState_multiSelectAndTotalSize() {
        val state = FileManagerUiState(
            allFiles = sampleFiles,
            selectedFileIds = setOf(1L, 3L) // 2_500_000 + 450_000 = 2_950_000
        )
        assertEquals(2, state.selectedCount)
        assertEquals(2_950_000L, state.selectedTotalSize)
        assertTrue(state.formattedSelectedTotalSize.contains("MB"))
    }

    @Test
    fun mainViewModel_fileManagerInteractions() {
        val prefsRepo = UserPreferencesRepository(app)
        val viewModel = MainViewModel(app, prefsRepo)

        // Navigation
        viewModel.navigateTo(Screen.FILE_MANAGER)
        assertEquals(Screen.FILE_MANAGER, viewModel.currentScreen.value)

        // View Mode toggle
        assertEquals(ViewMode.LIST, viewModel.fileManagerUiState.value.viewMode)
        viewModel.toggleViewMode()
        assertEquals(ViewMode.GRID, viewModel.fileManagerUiState.value.viewMode)

        // Category selection
        viewModel.selectFileCategory(FileCategory.DOCUMENTS)
        assertEquals(FileCategory.DOCUMENTS, viewModel.fileManagerUiState.value.selectedCategory)

        // Selection operations
        viewModel.toggleFileSelection(100L)
        assertTrue(viewModel.fileManagerUiState.value.selectedFileIds.contains(100L))

        viewModel.toggleFileSelection(100L)
        assertFalse(viewModel.fileManagerUiState.value.selectedFileIds.contains(100L))

        viewModel.toggleFileSelection(200L)
        viewModel.clearFileSelection()
        assertTrue(viewModel.fileManagerUiState.value.selectedFileIds.isEmpty())

        // File Details
        val item = sampleFiles[0]
        viewModel.setActiveFileDetails(item)
        assertEquals(item, viewModel.fileManagerUiState.value.activeFileDetails)

        viewModel.setActiveFileDetails(null)
        assertNull(viewModel.fileManagerUiState.value.activeFileDetails)

        // Navigate back
        val popped = viewModel.navigateBack()
        assertTrue(popped)
        assertEquals(Screen.HOME, viewModel.currentScreen.value)
    }

    @Test
    fun mainActivity_launchesAndNavigatesToFileManager() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        assertFalse(activity.isFinishing)

        val viewModel = androidx.lifecycle.ViewModelProvider(activity)[MainViewModel::class.java]
        viewModel.navigateTo(Screen.FILE_MANAGER)
        assertEquals(Screen.FILE_MANAGER, viewModel.currentScreen.value)

        controller.destroy()
    }

    @Test
    fun storagePermissionState_evaluatesCategoryAccessCorrectly() {
        // Full permissions
        val fullState = com.example.data.StoragePermissionState(
            imagesGranted = true,
            videosGranted = true,
            audioGranted = true,
            storageGranted = true
        )
        assertTrue(fullState.isCategoryPermitted(FileCategory.IMAGES))
        assertTrue(fullState.isCategoryPermitted(FileCategory.VIDEOS))
        assertTrue(fullState.isCategoryPermitted(FileCategory.AUDIO))
        assertTrue(fullState.isFullyGranted)
        assertFalse(fullState.isPartiallyGranted)

        // Partial permissions: images only
        val partialState = com.example.data.StoragePermissionState(
            imagesGranted = true,
            videosGranted = false,
            audioGranted = false,
            storageGranted = false
        )
        assertTrue(partialState.isCategoryPermitted(FileCategory.IMAGES))
        assertFalse(partialState.isCategoryPermitted(FileCategory.VIDEOS))
        assertFalse(partialState.isCategoryPermitted(FileCategory.AUDIO))
        assertTrue(partialState.hasAnyPermission)
        assertTrue(partialState.isPartiallyGranted)
        assertFalse(partialState.isFullyGranted)

        // Denied state
        val deniedState = com.example.data.StoragePermissionState(
            imagesGranted = false,
            videosGranted = false,
            audioGranted = false,
            storageGranted = false,
            isPermanentlyDenied = true
        )
        assertFalse(deniedState.isCategoryPermitted(FileCategory.IMAGES))
        assertFalse(deniedState.hasAnyPermission)
        assertTrue(deniedState.isPermanentlyDenied)
    }

    @Test
    fun storagePermissionState_requiredPermissionsPerCategory() {
        val imagePerms = com.example.data.StoragePermissionState.getRequiredPermissionsForCategory(FileCategory.IMAGES)
        assertTrue(imagePerms.contains(android.Manifest.permission.READ_MEDIA_IMAGES))

        val audioPerms = com.example.data.StoragePermissionState.getRequiredPermissionsForCategory(FileCategory.AUDIO)
        assertEquals(listOf(android.Manifest.permission.READ_MEDIA_AUDIO), audioPerms)

        val videoPerms = com.example.data.StoragePermissionState.getRequiredPermissionsForCategory(FileCategory.VIDEOS)
        assertTrue(videoPerms.contains(android.Manifest.permission.READ_MEDIA_VIDEO))
    }

    @Test
    fun mainViewModel_updatePermissionsRefreshesState() {
        val prefsRepo = UserPreferencesRepository(app)
        val viewModel = MainViewModel(app, prefsRepo)

        val permissionState = com.example.data.StoragePermissionState(
            imagesGranted = true,
            videosGranted = true,
            audioGranted = true,
            hasRequested = true
        )

        viewModel.updatePermissions(permissionState)
        assertEquals(permissionState, viewModel.fileManagerUiState.value.permissionState)
        assertTrue(viewModel.fileManagerUiState.value.permissionGranted)
    }

    @Test
    fun mainViewModel_onPermissionResultHandlesGrantsAndDenials() {
        val prefsRepo = UserPreferencesRepository(app)
        val viewModel = MainViewModel(app, prefsRepo)

        // Simulate granting image permission only (partial grant)
        val imageResult = mapOf(
            android.Manifest.permission.READ_MEDIA_IMAGES to true,
            android.Manifest.permission.READ_MEDIA_VIDEO to false
        )
        viewModel.onPermissionResult(imageResult)
        val uiState = viewModel.fileManagerUiState.value
        assertTrue(uiState.permissionState.imagesGranted)
        assertFalse(uiState.permissionState.videosGranted)
        assertTrue(uiState.permissionState.isCategoryPermitted(FileCategory.IMAGES))
        assertFalse(uiState.permissionState.isCategoryPermitted(FileCategory.VIDEOS))
        assertTrue(uiState.permissionState.isPartiallyGranted)
        assertFalse(uiState.permissionState.isFullyGranted)
        assertTrue(uiState.permissionGranted) // has any permission

        // Simulate complete denial
        val deniedResult = mapOf(
            android.Manifest.permission.READ_MEDIA_IMAGES to false,
            android.Manifest.permission.READ_MEDIA_VIDEO to false,
            android.Manifest.permission.READ_MEDIA_AUDIO to false
        )
        viewModel.onPermissionResult(deniedResult)
        val deniedUiState = viewModel.fileManagerUiState.value
        assertFalse(deniedUiState.permissionState.hasAnyPermission)
        assertFalse(deniedUiState.permissionGranted)
    }

    @Test
    fun storagePermissionState_visualUserSelectedPartialAccess() {
        val visualOnly = com.example.data.StoragePermissionState(
            imagesGranted = false,
            videosGranted = false,
            visualUserSelected = true
        )
        assertTrue(visualOnly.isCategoryPermitted(FileCategory.IMAGES))
        assertTrue(visualOnly.isCategoryPermitted(FileCategory.VIDEOS))
        assertFalse(visualOnly.isCategoryPermitted(FileCategory.AUDIO))
        assertTrue(visualOnly.hasAnyPermission)
        assertTrue(visualOnly.isPartiallyGranted)
    }

    @Test
    fun fileManagerUiState_emptyStateDistinctions() {
        // 1. Permission not granted state
        val unpermittedState = FileManagerUiState(
            permissionState = com.example.data.StoragePermissionState(),
            selectedCategory = FileCategory.IMAGES,
            allFiles = emptyList()
        )
        assertFalse(unpermittedState.permissionGranted)
        assertFalse(unpermittedState.permissionState.isCategoryPermitted(FileCategory.IMAGES))

        // 2. Permission granted, but no matching files found
        val emptyState = FileManagerUiState(
            permissionState = com.example.data.StoragePermissionState(imagesGranted = true),
            selectedCategory = FileCategory.IMAGES,
            allFiles = emptyList(),
            isLoading = false,
            errorMessage = null
        )
        assertTrue(emptyState.permissionGranted)
        assertTrue(emptyState.permissionState.isCategoryPermitted(FileCategory.IMAGES))
        assertTrue(emptyState.filteredFiles.isEmpty())
        assertFalse(emptyState.isLoading)
        assertNull(emptyState.errorMessage)

        // 3. Loading state
        val loadingState = FileManagerUiState(
            permissionState = com.example.data.StoragePermissionState(imagesGranted = true),
            isLoading = true
        )
        assertTrue(loadingState.isLoading)

        // 4. Query failure state
        val errorState = FileManagerUiState(
            permissionState = com.example.data.StoragePermissionState(imagesGranted = true),
            errorMessage = "Failed to query storage"
        )
        assertNotNull(errorState.errorMessage)
    }
}
