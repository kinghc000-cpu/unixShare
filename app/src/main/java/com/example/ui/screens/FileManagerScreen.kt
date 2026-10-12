package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.FileCategory
import com.example.data.MediaFileItem
import com.example.data.SortOption
import com.example.data.StorageInfo
import com.example.data.ViewMode
import com.example.ui.FileManagerUiState
import com.example.ui.components.StatusPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    state: FileManagerUiState,
    onRequestPermissions: () -> Unit,
    onBrowseSafFiles: () -> Unit,
    onCategorySelect: (FileCategory) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortSelect: (SortOption) -> Unit,
    onToggleViewMode: () -> Unit,
    onToggleFileSelect: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelect: () -> Unit,
    onPrepareShare: () -> Unit,
    onClearPreparedBundle: () -> Unit,
    onShowFileDetails: (MediaFileItem?) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRequestCategoryPermission: (FileCategory) -> Unit = { onRequestPermissions() },
    onOpenAppSettings: () -> Unit = {}
) {
    BackHandler(onBack = onNavigateBack)

    var isSearchVisible by remember { mutableStateOf(false) }
    var isSortMenuVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("file_manager_screen"),
        topBar = {
            TopAppBar(
                title = {
                    if (state.selectedCount > 0) {
                        Text(
                            text = stringResource(
                                R.string.selected_count_format,
                                state.selectedCount,
                                state.formattedSelectedTotalSize
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.file_manager_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // Search toggle
                    IconButton(
                        onClick = {
                            isSearchVisible = !isSearchVisible
                            if (!isSearchVisible) onSearchQueryChange("")
                        },
                        modifier = Modifier.testTag("btn_search_toggle")
                    ) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_files_hint)
                        )
                    }

                    // View Mode toggle
                    IconButton(
                        onClick = onToggleViewMode,
                        modifier = Modifier.testTag("btn_toggle_view")
                    ) {
                        Icon(
                            imageVector = if (state.viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = stringResource(
                                if (state.viewMode == ViewMode.LIST) R.string.view_mode_grid else R.string.view_mode_list
                            )
                        )
                    }

                    // Sort menu
                    Box {
                        IconButton(
                            onClick = { isSortMenuVisible = true },
                            modifier = Modifier.testTag("btn_sort_menu")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = stringResource(R.string.sort_by)
                            )
                        }

                        DropdownMenu(
                            expanded = isSortMenuVisible,
                            onDismissRequest = { isSortMenuVisible = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_date_desc)) },
                                onClick = {
                                    onSortSelect(SortOption.DATE_DESC)
                                    isSortMenuVisible = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_date_asc)) },
                                onClick = {
                                    onSortSelect(SortOption.DATE_ASC)
                                    isSortMenuVisible = false
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_name_asc)) },
                                onClick = {
                                    onSortSelect(SortOption.NAME_ASC)
                                    isSortMenuVisible = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_name_desc)) },
                                onClick = {
                                    onSortSelect(SortOption.NAME_DESC)
                                    isSortMenuVisible = false
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_size_desc)) },
                                onClick = {
                                    onSortSelect(SortOption.SIZE_DESC)
                                    isSortMenuVisible = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_size_asc)) },
                                onClick = {
                                    onSortSelect(SortOption.SIZE_ASC)
                                    isSortMenuVisible = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_type)) },
                                onClick = {
                                    onSortSelect(SortOption.TYPE)
                                    isSortMenuVisible = false
                                }
                            )
                        }
                    }

                    // Select All
                    IconButton(
                        onClick = onSelectAll,
                        modifier = Modifier.testTag("btn_select_all")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = stringResource(R.string.select_all)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (state.selectedCount > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = onClearSelect,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_clear_selection")
                        ) {
                            Text(stringResource(R.string.clear_selection))
                        }

                        Button(
                            onClick = onPrepareShare,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_prepare_share")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${stringResource(R.string.prepare_share_button)} (${state.selectedCount})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Field
            AnimatedVisibility(visible = isSearchVisible) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.search_files_hint)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_input")
                )
            }

            // Storage Overview Card
            state.storageInfo?.let { storage ->
                StorageOverviewCard(storageInfo = storage)
            }

            // Category Shortcut Chips Row
            CategoryShortcutsRow(
                selectedCategory = state.selectedCategory,
                onCategorySelect = onCategorySelect
            )

            // Permission warning banners
            if (!state.permissionGranted) {
                PermissionRequestBanner(
                    onGrantClick = { onRequestCategoryPermission(state.selectedCategory) },
                    onBrowseSafClick = onBrowseSafFiles,
                    isPermanentlyDenied = state.permissionState.isPermanentlyDenied,
                    onOpenSettings = onOpenAppSettings
                )
            } else if (state.permissionState.isPartiallyGranted && !state.isCategoryPermitted) {
                PartialPermissionBanner(
                    category = state.selectedCategory,
                    onGrantClick = { onRequestCategoryPermission(state.selectedCategory) }
                )
            }

            // Secondary SAF action strip
            SafBrowseBar(
                onBrowseSafClick = onBrowseSafFiles
            )

            // Content Area (Loading, Empty, List or Grid)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    state.isLoading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    state.filteredFiles.isEmpty() -> {
                        val isCategoryPermitted = state.isCategoryPermitted
                        val isPermanentlyDenied = state.permissionState.isPermanentlyDenied

                        when {
                            !isCategoryPermitted && !isPermanentlyDenied -> {
                                EmptyStatePermissionNotGranted(
                                    category = state.selectedCategory,
                                    onRequestPermission = { onRequestCategoryPermission(state.selectedCategory) },
                                    onBrowseSafClick = onBrowseSafFiles
                                )
                            }
                            !isCategoryPermitted && isPermanentlyDenied -> {
                                EmptyStatePermissionDenied(
                                    category = state.selectedCategory,
                                    onOpenSettings = onOpenAppSettings,
                                    onRetry = { onRequestCategoryPermission(state.selectedCategory) },
                                    onBrowseSafClick = onBrowseSafFiles
                                )
                            }
                            else -> {
                                EmptyStateNoFiles(
                                    category = state.selectedCategory,
                                    searchQuery = state.searchQuery,
                                    onBrowseSafClick = onBrowseSafFiles
                                )
                            }
                        }
                    }

                    state.viewMode == ViewMode.LIST -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("file_list_view"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = state.filteredFiles,
                                key = { it.id }
                            ) { file ->
                                val isSelected = state.selectedFileIds.contains(file.id)
                                FileListItemRow(
                                    file = file,
                                    isSelected = isSelected,
                                    onToggleSelect = { onToggleFileSelect(file.id) },
                                    onInfoClick = { onShowFileDetails(file) }
                                )
                            }
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 130.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("file_grid_view"),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = state.filteredFiles,
                                key = { it.id }
                            ) { file ->
                                val isSelected = state.selectedFileIds.contains(file.id)
                                FileGridItemCard(
                                    file = file,
                                    isSelected = isSelected,
                                    onToggleSelect = { onToggleFileSelect(file.id) },
                                    onInfoClick = { onShowFileDetails(file) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // File Details Bottom Sheet
    state.activeFileDetails?.let { file ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { onShowFileDetails(null) },
            sheetState = sheetState
        ) {
            FileDetailsSheetContent(
                file = file,
                onDismiss = { onShowFileDetails(null) }
            )
        }
    }

    // Prepared Share Bundle Dialog
    state.preparedShareBundle?.let { bundle ->
        AlertDialog(
            onDismissRequest = onClearPreparedBundle,
            title = {
                Text(
                    text = stringResource(R.string.prepared_bundle_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(
                            R.string.prepared_bundle_desc,
                            bundle.files.size,
                            bundle.formattedTotalSize
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Prepared Files:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    bundle.files.take(4).forEach { f ->
                        Text(
                            text = "• ${f.name} (${f.formattedSize})",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (bundle.files.size > 4) {
                        Text(
                            text = "... and ${bundle.files.size - 4} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onClearPreparedBundle,
                    modifier = Modifier.testTag("btn_bundle_done")
                ) {
                    Text(stringResource(R.string.prepared_bundle_done))
                }
            }
        )
    }
}

@Composable
private fun StorageOverviewCard(storageInfo: StorageInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("storage_overview_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.storage_overview),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = stringResource(
                        R.string.storage_used_of,
                        storageInfo.formattedUsed,
                        storageInfo.formattedTotal
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { storageInfo.usagePercentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
private fun CategoryShortcutsRow(
    selectedCategory: FileCategory,
    onCategorySelect: (FileCategory) -> Unit
) {
    val categories = listOf(
        FileCategory.ALL to (stringResource(R.string.category_all) to Icons.Default.AccessTime),
        FileCategory.IMAGES to (stringResource(R.string.category_images) to Icons.Default.Image),
        FileCategory.VIDEOS to (stringResource(R.string.category_videos) to Icons.Default.Videocam),
        FileCategory.AUDIO to (stringResource(R.string.category_audio) to Icons.Default.MusicNote),
        FileCategory.DOCUMENTS to (stringResource(R.string.category_documents) to Icons.Default.Description),
        FileCategory.DOWNLOADS to (stringResource(R.string.category_downloads) to Icons.Default.Download),
        FileCategory.OTHER to (stringResource(R.string.category_other) to Icons.Default.Folder)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("category_shortcuts_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { (cat, info) ->
            val (label, icon) = info
            val isSelected = selectedCategory == cat

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelect(cat) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun PermissionRequestBanner(
    onGrantClick: () -> Unit,
    onBrowseSafClick: () -> Unit,
    isPermanentlyDenied: Boolean = false,
    onOpenSettings: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("permission_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isPermanentlyDenied) Icons.Default.Settings else Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(
                        if (isPermanentlyDenied) R.string.permission_denied_title else R.string.permission_storage_title
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(
                    if (isPermanentlyDenied) R.string.permission_denied_settings_desc else R.string.permission_storage_desc
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isPermanentlyDenied) {
                    Button(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("btn_banner_open_settings")
                    ) {
                        Text(stringResource(R.string.open_settings_button))
                    }
                } else {
                    Button(
                        onClick = onGrantClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("btn_grant_permission")
                    ) {
                        Text(stringResource(R.string.permission_grant_button))
                    }
                }

                OutlinedButton(
                    onClick = onBrowseSafClick,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.browse_other_locations))
                }
            }
        }
    }
}

@Composable
private fun PartialPermissionBanner(
    category: FileCategory,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("partial_permission_banner"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.permission_partial_banner),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onGrantClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("btn_partial_grant")
            ) {
                Text(
                    text = stringResource(R.string.permission_grant_button),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun SafBrowseBar(
    onBrowseSafClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBrowseSafClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("btn_saf_picker"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.browse_other_locations),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ) {
            Text(
                text = "SAF",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun FileListItemRow(
    file: MediaFileItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onInfoClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggleSelect)
            .then(
                if (isSelected) {
                    Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(14.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            FileCategoryIconBox(category = file.category, extension = file.extension)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = file.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    Text(
                        text = file.formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }

            IconButton(
                onClick = onInfoClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_file_info_${file.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.file_details_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun FileGridItemCard(
    file: MediaFileItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onInfoClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggleSelect)
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                FileCategoryIconBox(category = file.category, extension = file.extension)

                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = file.formattedSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FileCategoryIconBox(
    category: FileCategory,
    extension: String
) {
    val (icon, color) = when (category) {
        FileCategory.IMAGES -> Icons.Default.Image to Color(0xFF0284C7)
        FileCategory.VIDEOS -> Icons.Default.Videocam to Color(0xFF7C3AED)
        FileCategory.AUDIO -> Icons.Default.MusicNote to Color(0xFFE11D48)
        FileCategory.DOCUMENTS -> Icons.Default.Description to Color(0xFFD97706)
        FileCategory.DOWNLOADS -> Icons.Default.Download to Color(0xFF059669)
        else -> Icons.Default.Folder to Color(0xFF475569)
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            if (extension.isNotEmpty()) {
                Text(
                    text = extension,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStatePermissionNotGranted(
    category: FileCategory,
    onRequestPermission: () -> Unit,
    onBrowseSafClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("empty_state_permission_not_granted"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.permission_storage_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        val catName = getCategoryDisplayName(category)
        Text(
            text = stringResource(R.string.permission_category_desc, catName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("btn_grant_permission")
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.permission_grant_button))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBrowseSafClick,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("btn_empty_saf")
        ) {
            Text(stringResource(R.string.browse_other_locations))
        }
    }
}

@Composable
private fun EmptyStatePermissionDenied(
    category: FileCategory,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onBrowseSafClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("empty_state_permission_denied"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.permission_denied_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.permission_denied_settings_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.testTag("btn_open_settings")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.open_settings_button))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_retry_permission")
            ) {
                Text(stringResource(R.string.retry))
            }

            OutlinedButton(
                onClick = onBrowseSafClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_empty_saf_denied")
            ) {
                Text(stringResource(R.string.browse_other_locations))
            }
        }
    }
}

@Composable
private fun EmptyStateNoFiles(
    category: FileCategory,
    searchQuery: String,
    onBrowseSafClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("empty_state_no_files"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.empty_files_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        val desc = if (searchQuery.isNotBlank()) {
            stringResource(R.string.empty_search_desc, searchQuery)
        } else {
            val catName = getCategoryDisplayName(category)
            stringResource(R.string.empty_category_files_desc, catName)
        }

        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedButton(
            onClick = onBrowseSafClick,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("btn_empty_saf_nofiles")
        ) {
            Text(stringResource(R.string.browse_other_locations))
        }
    }
}

@Composable
private fun getCategoryDisplayName(category: FileCategory): String {
    return when (category) {
        FileCategory.ALL -> stringResource(R.string.category_all)
        FileCategory.IMAGES -> stringResource(R.string.category_images)
        FileCategory.VIDEOS -> stringResource(R.string.category_videos)
        FileCategory.AUDIO -> stringResource(R.string.category_audio)
        FileCategory.DOCUMENTS -> stringResource(R.string.category_documents)
        FileCategory.DOWNLOADS -> stringResource(R.string.category_downloads)
        FileCategory.OTHER -> stringResource(R.string.category_other)
    }
}

@Composable
private fun EmptyFilesView() {
    EmptyStateNoFiles(
        category = FileCategory.ALL,
        searchQuery = "",
        onBrowseSafClick = {}
    )
}

@Composable
private fun FileDetailsSheetContent(
    file: MediaFileItem,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("file_details_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.file_details_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            StatusPill(text = file.category.name)
        }

        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(label = stringResource(R.string.file_detail_name), value = file.name)
        DetailRow(label = stringResource(R.string.file_detail_size), value = "${file.formattedSize} (${file.size} bytes)")
        DetailRow(label = stringResource(R.string.file_detail_type), value = file.mimeType)
        DetailRow(label = stringResource(R.string.file_detail_modified), value = file.formattedDate)
        DetailRow(label = stringResource(R.string.file_detail_uri), value = file.uri.toString())

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.dismiss))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
