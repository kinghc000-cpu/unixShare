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
