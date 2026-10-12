package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class UserPreferencesRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _themeMode = MutableStateFlow(readThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _appLanguage = MutableStateFlow(readAppLanguage())
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _userProfile = MutableStateFlow(readUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun readThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(name)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    private fun readAppLanguage(): AppLanguage {
        val name = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name
        return try {
            AppLanguage.valueOf(name)
        } catch (_: IllegalArgumentException) {
            AppLanguage.SYSTEM
        }
    }

    private fun readUserProfile(): UserProfile {
        val name = prefs.getString(KEY_USER_NAME, UserProfile.DEFAULT_NAME) ?: UserProfile.DEFAULT_NAME
        val bio = prefs.getString(KEY_USER_BIO, UserProfile.DEFAULT_BIO) ?: UserProfile.DEFAULT_BIO
        val avatar = prefs.getInt(KEY_USER_AVATAR, 0)
        return UserProfile(
            displayName = name,
            bio = bio,
            avatarColorIndex = avatar.coerceIn(0, 4)
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = withContext(ioDispatcher) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    suspend fun setAppLanguage(language: AppLanguage) = withContext(ioDispatcher) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language.name).apply()
        _appLanguage.value = language
    }

    suspend fun setUserProfile(profile: UserProfile) = withContext(ioDispatcher) {
        prefs.edit()
            .putString(KEY_USER_NAME, profile.displayName)
            .putString(KEY_USER_BIO, profile.bio)
            .putInt(KEY_USER_AVATAR, profile.avatarColorIndex)
            .apply()
        _userProfile.value = profile
    }

    suspend fun resetDefaults() = withContext(ioDispatcher) {
        prefs.edit().clear().apply()
        _themeMode.value = ThemeMode.SYSTEM
        _appLanguage.value = AppLanguage.SYSTEM
        _userProfile.value = UserProfile()
    }

    companion object {
        const val PREFS_NAME = "unex_share_preferences"
        const val KEY_THEME_MODE = "pref_theme_mode"
        const val KEY_APP_LANGUAGE = "pref_app_language"
        const val KEY_USER_NAME = "pref_user_name"
        const val KEY_USER_BIO = "pref_user_bio"
        const val KEY_USER_AVATAR = "pref_user_avatar"
    }
}
