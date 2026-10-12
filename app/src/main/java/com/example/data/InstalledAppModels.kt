package com.example.data

import android.graphics.drawable.Drawable

data class InstalledAppItem(
    val packageName: String,
    val label: String,
    val versionName: String = "",
    val versionCode: Long = 0L,
    val apkSizeBytes: Long = 0L,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false,
    val installTimeMillis: Long = 0L
) {
    val formattedSize: String
        get() = MediaFileItem.formatByteSize(apkSizeBytes)
}

enum class AppManagerTab {
    ALL_APPS,
    SELECTED_APPS
}

data class AppManagerUiState(
    val isLoading: Boolean = false,
    val installedApps: List<InstalledAppItem> = emptyList(),
    val selectedPackageNames: Set<String> = emptySet(),
    val searchQuery: String = "",
    val activeTab: AppManagerTab = AppManagerTab.ALL_APPS,
    val errorMessage: String? = null,
    val preparedAppShareNotice: String? = null
) {
    val selectedApps: List<InstalledAppItem>
        get() = installedApps.filter { it.packageName in selectedPackageNames }

    val filteredApps: List<InstalledAppItem>
        get() {
            val list = if (activeTab == AppManagerTab.SELECTED_APPS) selectedApps else installedApps
            return if (searchQuery.isBlank()) {
                list
            } else {
                val q = searchQuery.trim().lowercase()
                list.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
            }
        }

    val selectedCount: Int
        get() = selectedPackageNames.size

    val selectedTotalSizeBytes: Long
        get() = selectedApps.sumOf { it.apkSizeBytes }

    val formattedSelectedTotalSize: String
        get() = MediaFileItem.formatByteSize(selectedTotalSizeBytes)
}
