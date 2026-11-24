package com.example.mayuranddhyeyinternshipproject.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Handles encrypted local preferences storage according to Android security guidelines.
 * Sensitive data such as tokens and user preferences are encrypted at rest using AES-256 GCM.
 */
class SecurityPreferences(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "medmate_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to private mode if keystore or security provider fails on specific OEM
        context.getSharedPreferences("medmate_fallback_prefs", Context.MODE_PRIVATE)
    }

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var isScreenPrivacyEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_PRIVACY, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_PRIVACY, value).apply()

    var cachedUserId: String?
        get() = prefs.getString(KEY_CACHED_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_CACHED_USER_ID, value).apply()

    var cachedUserName: String?
        get() = prefs.getString(KEY_CACHED_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_CACHED_USER_NAME, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_DARK_MODE = "pref_dark_mode"
        private const val KEY_SCREEN_PRIVACY = "pref_screen_privacy"
        private const val KEY_CACHED_USER_ID = "pref_cached_user_id"
        private const val KEY_CACHED_USER_NAME = "pref_cached_user_name"
    }
}
