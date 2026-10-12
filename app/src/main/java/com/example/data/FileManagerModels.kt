package com.example.data

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileCategory {
    ALL,
    IMAGES,
    VIDEOS,
    AUDIO,
    DOCUMENTS,
    DOWNLOADS,
    OTHER
}

enum class SortOption {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC,
    SIZE_ASC,
    TYPE
}

enum class ViewMode {
    LIST,
    GRID
}

data class MediaFileItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val dateModified: Long, // timestamp in seconds
    val category: FileCategory,
    val relativePath: String? = null
) {
    val formattedSize: String
        get() = formatByteSize(size)

    val formattedDate: String
        get() {
            if (dateModified <= 0) return "—"
            val date = Date(dateModified * 1000L)
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return formatter.format(date)
        }

    val extension: String
        get() = name.substringAfterLast('.', "").uppercase().take(5)

    companion object {
        fun formatByteSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}

data class StorageInfo(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val formattedUsed: String,
    val formattedTotal: String,
    val usagePercentage: Float
)

data class PreparedShareBundle(
    val files: List<MediaFileItem>,
    val totalSize: Long,
    val formattedTotalSize: String,
    val preparedTimestamp: Long = System.currentTimeMillis()
)

data class StoragePermissionState(
    val imagesGranted: Boolean = false,
    val videosGranted: Boolean = false,
    val audioGranted: Boolean = false,
    val visualUserSelected: Boolean = false,
    val storageGranted: Boolean = false,
    val isPermanentlyDenied: Boolean = false,
    val hasRequested: Boolean = false
) {
    fun isCategoryPermitted(category: FileCategory): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            when (category) {
                FileCategory.ALL -> imagesGranted || videosGranted || audioGranted || visualUserSelected
                FileCategory.IMAGES -> imagesGranted || visualUserSelected
                FileCategory.VIDEOS -> videosGranted || visualUserSelected
                FileCategory.AUDIO -> audioGranted
                FileCategory.DOCUMENTS -> true
                FileCategory.DOWNLOADS -> true
                FileCategory.OTHER -> true
            }
        } else {
            storageGranted
        }
    }

    val hasAnyPermission: Boolean
        get() = imagesGranted || videosGranted || audioGranted || visualUserSelected || storageGranted

    val isFullyGranted: Boolean
        get() = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            imagesGranted && videosGranted && audioGranted
        } else {
            storageGranted
        }

    val isPartiallyGranted: Boolean
        get() = hasAnyPermission && !isFullyGranted

    companion object {
        fun getRequiredPermissionsForCategory(category: FileCategory): List<String> {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                when (category) {
                    FileCategory.ALL -> {
                        val list = mutableListOf(
                            android.Manifest.permission.READ_MEDIA_IMAGES,
                            android.Manifest.permission.READ_MEDIA_VIDEO,
                            android.Manifest.permission.READ_MEDIA_AUDIO
                        )
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            list.add(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                        }
                        list
                    }
                    FileCategory.IMAGES -> {
                        val list = mutableListOf(android.Manifest.permission.READ_MEDIA_IMAGES)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            list.add(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                        }
                        list
                    }
                    FileCategory.VIDEOS -> {
                        val list = mutableListOf(android.Manifest.permission.READ_MEDIA_VIDEO)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            list.add(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                        }
                        list
                    }
                    FileCategory.AUDIO -> listOf(android.Manifest.permission.READ_MEDIA_AUDIO)
                    FileCategory.DOCUMENTS,
                    FileCategory.DOWNLOADS,
                    FileCategory.OTHER -> {
                        val list = mutableListOf(
                            android.Manifest.permission.READ_MEDIA_IMAGES,
                            android.Manifest.permission.READ_MEDIA_VIDEO,
                            android.Manifest.permission.READ_MEDIA_AUDIO
                        )
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            list.add(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                        }
                        list
                    }
                }
            } else {
                listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        fun checkDevicePermissions(context: android.content.Context): StoragePermissionState {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val images = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_MEDIA_IMAGES
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                val videos = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_MEDIA_VIDEO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                val audio = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_MEDIA_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                val visualSelected = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else false

                StoragePermissionState(
                    imagesGranted = images,
                    videosGranted = videos,
                    audioGranted = audio,
                    visualUserSelected = visualSelected,
                    storageGranted = false
                )
            } else {
                val storage = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                StoragePermissionState(
                    imagesGranted = storage,
                    videosGranted = storage,
                    audioGranted = storage,
                    visualUserSelected = false,
                    storageGranted = storage
                )
            }
        }
    }
}

