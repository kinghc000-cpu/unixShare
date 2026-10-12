package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileManagerRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getStorageInfo(): StorageInfo = withContext(ioDispatcher) {
        try {
            val path = Environment.getDataDirectory().path
            val stat = StatFs(path)
            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
            val usagePct = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) else 0f

            StorageInfo(
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                usedBytes = usedBytes,
                formattedUsed = MediaFileItem.formatByteSize(usedBytes),
                formattedTotal = MediaFileItem.formatByteSize(totalBytes),
                usagePercentage = usagePct
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query storage stats", e)
            StorageInfo(
                totalBytes = 0L,
                freeBytes = 0L,
                usedBytes = 0L,
                formattedUsed = "0 B",
                formattedTotal = "0 B",
                usagePercentage = 0f
            )
        }
    }

    suspend fun queryAccessibleFiles(
        permissions: StoragePermissionState = StoragePermissionState(
            imagesGranted = true,
            videosGranted = true,
            audioGranted = true,
            storageGranted = true
        )
    ): List<MediaFileItem> = withContext(ioDispatcher) {
        val fileList = mutableListOf<MediaFileItem>()

        val canQueryImages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.imagesGranted || permissions.visualUserSelected
        } else {
            permissions.storageGranted
        }

        val canQueryVideos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.videosGranted || permissions.visualUserSelected
        } else {
            permissions.storageGranted
        }

        val canQueryAudio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.audioGranted
        } else {
            permissions.storageGranted
        }

        // 1. Query Images if permitted
        if (canQueryImages) {
            queryMediaStoreCategory(
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                defaultCategory = FileCategory.IMAGES,
                targetList = fileList
            )
        }

        // 2. Query Videos if permitted
        if (canQueryVideos) {
            queryMediaStoreCategory(
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                defaultCategory = FileCategory.VIDEOS,
                targetList = fileList
            )
        }

        // 3. Query Audio if permitted
        if (canQueryAudio) {
            queryMediaStoreCategory(
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                defaultCategory = FileCategory.AUDIO,
                targetList = fileList
            )
        }

        val canQueryDocuments = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            permissions.storageGranted
        }

        // 4. Query Documents and Files via MediaStore.Files
        if (canQueryDocuments) {
            queryMediaStoreDocuments(fileList)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                queryMediaStoreDownloads(fileList)
            }
        }

        // Return sorted by date modified descending by default
        fileList.sortedByDescending { it.dateModified }
    }

    private fun queryMediaStoreDownloads(targetList: MutableList<MediaFileItem>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )

            cursor?.use {
                val idCol = it.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameCol = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val mimeCol = it.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val dateCol = it.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

                while (it.moveToNext()) {
                    if (idCol != -1) {
                        val id = it.getLong(idCol)
                        val name = if (nameCol != -1) it.getString(nameCol) ?: "Download" else "Download"
                        val size = if (sizeCol != -1) it.getLong(sizeCol).coerceAtLeast(0L) else 0L
                        val mime = if (mimeCol != -1) it.getString(mimeCol) ?: "application/octet-stream" else "application/octet-stream"
                        val dateModified = if (dateCol != -1) it.getLong(dateCol) else 0L
                        val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                        val category = resolveCategoryFromMime(mime, name)

                        if (targetList.none { existing -> existing.uri == uri }) {
                            targetList.add(
                                MediaFileItem(
                                    id = id,
                                    uri = uri,
                                    name = name,
                                    size = size,
                                    mimeType = mime,
                                    dateModified = dateModified,
                                    category = if (category == FileCategory.OTHER) FileCategory.DOWNLOADS else category
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore for Downloads", e)
        }
    }

    private fun queryMediaStoreCategory(
        contentUri: Uri,
        defaultCategory: FileCategory,
        targetList: MutableList<MediaFileItem>
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        try {
            val cursor = context.contentResolver.query(
                contentUri,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )

            cursor?.use {
                val idCol = it.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameCol = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val mimeCol = it.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val dateCol = it.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

                while (it.moveToNext()) {
                    if (idCol != -1) {
                        val id = it.getLong(idCol)
                        val name = if (nameCol != -1) it.getString(nameCol) ?: "Unknown" else "Unknown"
                        val size = if (sizeCol != -1) it.getLong(sizeCol).coerceAtLeast(0L) else 0L
                        val mime = if (mimeCol != -1) it.getString(mimeCol) ?: "application/octet-stream" else "application/octet-stream"
                        val dateModified = if (dateCol != -1) it.getLong(dateCol) else 0L
                        val uri = ContentUris.withAppendedId(contentUri, id)

                        targetList.add(
                            MediaFileItem(
                                id = id,
                                uri = uri,
                                name = name,
                                size = size,
                                mimeType = mime,
                                dateModified = dateModified,
                                category = defaultCategory
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore for $defaultCategory", e)
        }
    }

    private fun queryMediaStoreDocuments(targetList: MutableList<MediaFileItem>) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_NONE}"

        val selectionArgs = arrayOf(
            "application/pdf",
            "text/%",
            "application/msword%",
            "application/vnd.openxmlformats-officedocument%",
            "application/vnd.ms-%",
            "application/zip",
            "application/vnd.android.package-archive"
        )

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )

            cursor?.use {
                val idCol = it.getColumnIndex(MediaStore.Files.FileColumns._ID)
                val nameCol = it.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = it.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val mimeCol = it.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                val dateCol = it.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (it.moveToNext()) {
                    if (idCol != -1) {
                        val id = it.getLong(idCol)
                        val name = if (nameCol != -1) it.getString(nameCol) ?: "Document" else "Document"
                        val size = if (sizeCol != -1) it.getLong(sizeCol).coerceAtLeast(0L) else 0L
                        val mime = if (mimeCol != -1) it.getString(mimeCol) ?: "application/octet-stream" else "application/octet-stream"
                        val dateModified = if (dateCol != -1) it.getLong(dateCol) else 0L
                        val uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                        val category = resolveCategoryFromMime(mime, name)

                        if (targetList.none { existing -> existing.uri == uri }) {
                            targetList.add(
                                MediaFileItem(
                                    id = id,
                                    uri = uri,
                                    name = name,
                                    size = size,
                                    mimeType = mime,
                                    dateModified = dateModified,
                                    category = if (category == FileCategory.ALL) FileCategory.DOCUMENTS else category
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore for Documents", e)
        }
    }

    suspend fun resolveFilesFromTreeUri(treeUri: Uri): List<MediaFileItem> = withContext(ioDispatcher) {
        val result = mutableListOf<MediaFileItem>()
        try {
            val docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            val projection = arrayOf(
                android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                android.provider.DocumentsContract.Document.COLUMN_SIZE,
                android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
                android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            val cursor = context.contentResolver.query(childrenUri, projection, null, null, null)
            cursor?.use {
                val idCol = it.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = it.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val sizeCol = it.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_SIZE)
                val mimeCol = it.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)
                val dateCol = it.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (it.moveToNext()) {
                    val childDocId = if (idCol != -1) it.getString(idCol) ?: continue else continue
                    val name = if (nameCol != -1) it.getString(nameCol) ?: "File" else "File"
                    val size = if (sizeCol != -1) it.getLong(sizeCol).coerceAtLeast(0L) else 0L
                    val mime = if (mimeCol != -1) it.getString(mimeCol) ?: "application/octet-stream" else "application/octet-stream"
                    val dateModified = if (dateCol != -1) (it.getLong(dateCol) / 1000L) else (System.currentTimeMillis() / 1000L)

                    // Skip directories
                    if (mime == android.provider.DocumentsContract.Document.MIME_TYPE_DIR) continue

                    val itemUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                    val category = resolveCategoryFromMime(mime, name)

                    result.add(
                        MediaFileItem(
                            id = itemUri.hashCode().toLong(),
                            uri = itemUri,
                            name = name,
                            size = size,
                            mimeType = mime,
                            dateModified = dateModified,
                            category = category
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve folder documents from treeUri: $treeUri", e)
        }
        result
    }

    suspend fun resolveFileFromUri(uri: Uri): MediaFileItem? = withContext(ioDispatcher) {
        try {
            var name = "External_File"
            var size = 0L

            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1 && !it.isNull(nameIndex)) {
                        name = it.getString(nameIndex)
                    }
                    if (sizeIndex != -1 && !it.isNull(sizeIndex)) {
                        size = it.getLong(sizeIndex).coerceAtLeast(0L)
                    }
                }
            }

            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val category = resolveCategoryFromMime(mime, name)
            val generatedId = uri.hashCode().toLong()

            MediaFileItem(
                id = generatedId,
                uri = uri,
                name = name,
                size = size,
                mimeType = mime,
                dateModified = System.currentTimeMillis() / 1000L,
                category = category
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve file metadata from Uri: $uri", e)
            null
        }
    }

    companion object {
        private const val TAG = "FileManagerRepo"

        fun resolveCategoryFromMime(mimeType: String, fileName: String): FileCategory {
            val lowerMime = mimeType.lowercase()
            val lowerName = fileName.lowercase()

            return when {
                lowerMime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") || lowerName.endsWith(".gif") -> FileCategory.IMAGES
                lowerMime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".mov") || lowerName.endsWith(".avi") -> FileCategory.VIDEOS
                lowerMime.startsWith("audio/") || lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") || lowerName.endsWith(".flac") || lowerName.endsWith(".aac") -> FileCategory.AUDIO
                lowerMime.contains("pdf") || lowerMime.contains("word") || lowerMime.contains("document") || lowerMime.contains("sheet") || lowerMime.contains("presentation") || lowerName.endsWith(".pdf") || lowerName.endsWith(".docx") || lowerName.endsWith(".txt") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".pptx") -> FileCategory.DOCUMENTS
                lowerName.contains("download") || lowerName.endsWith(".zip") || lowerName.endsWith(".rar") || lowerName.endsWith(".tar") || lowerName.endsWith(".apk") -> FileCategory.DOWNLOADS
                else -> FileCategory.OTHER
            }
        }
    }
}
