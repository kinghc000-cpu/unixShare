package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class InstalledAppsRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getInstalledApps(): List<InstalledAppItem> = withContext(ioDispatcher) {
        val pm = context.packageManager
        val result = mutableListOf<InstalledAppItem>()

        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val launchableActivities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            val seenPackages = mutableSetOf<String>()

            for (resolveInfo in launchableActivities) {
                try {
                    val pkgName = resolveInfo.activityInfo?.packageName ?: continue
                    if (seenPackages.contains(pkgName)) continue
                    seenPackages.add(pkgName)

                    val label = resolveInfo.loadLabel(pm)?.toString()?.ifBlank { pkgName } ?: pkgName
                    val icon = try {
                        resolveInfo.loadIcon(pm)
                    } catch (e: Exception) {
                        null
                    }

                    var versionName = ""
                    var versionCode = 0L
                    var apkSize = 0L
                    var isSystem = false
                    var installTime = 0L

                    try {
                        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(0L))
                        } else {
                            @Suppress("DEPRECATION")
                            pm.getPackageInfo(pkgName, 0)
                        }

                        versionName = pkgInfo.versionName ?: ""
                        versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            pkgInfo.longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            pkgInfo.versionCode.toLong()
                        }
                        installTime = pkgInfo.firstInstallTime

                        val appInfo = pkgInfo.applicationInfo
                        if (appInfo != null) {
                            isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                            val sourceDir = appInfo.sourceDir
                            if (sourceDir != null) {
                                val file = File(sourceDir)
                                if (file.exists()) {
                                    apkSize = file.length().coerceAtLeast(0L)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not query package info for $pkgName", e)
                    }

                    result.add(
                        InstalledAppItem(
                            packageName = pkgName,
                            label = label,
                            versionName = versionName,
                            versionCode = versionCode,
                            apkSizeBytes = apkSize,
                            icon = icon,
                            isSystemApp = isSystem,
                            installTimeMillis = installTime
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error resolving app entry", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query launchable activities", e)
        }

        result.sortedBy { it.label.lowercase() }
    }

    companion object {
        private const val TAG = "InstalledAppsRepo"
    }
}
