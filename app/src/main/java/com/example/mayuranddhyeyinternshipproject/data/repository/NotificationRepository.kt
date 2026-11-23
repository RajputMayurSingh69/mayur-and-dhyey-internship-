package com.example.mayuranddhyeyinternshipproject.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.mayuranddhyeyinternshipproject.data.model.MedMateNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Manages in-app notifications and medicine reminder alerts with hardware-backed
 * AES-256 GCM encryption at rest using EncryptedSharedPreferences.
 *
 * Note: androidx.security:security-crypto (EncryptedSharedPreferences / MasterKey)
 * is deprecated in newer releases/Jetpack, but still works reliably on Android API 24+.
 * It should be migrated to Jetpack DataStore with cryptographic serialization in a future update.
 */
class NotificationRepository(context: Context) {

    private val applicationContext = context.applicationContext

    // MasterKey and EncryptedSharedPreferences initialization
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            applicationContext,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback for devices with broken KeyStore implementation
        Log.w(TAG, "KeyStore provider unavailable, falling back to private storage.")
        applicationContext.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val TAG = "NotificationRepository"
        private const val OLD_PLAINTEXT_PREFS_NAME = "medmate_notifications_pref"
        private const val ENCRYPTED_PREFS_NAME = "medmate_encrypted_notifications"
        private const val FALLBACK_PREFS_NAME = "medmate_notifications_fallback"
        private const val KEY_NOTIFICATIONS = "key_notifications_list"

        private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val _notificationsFlow = MutableStateFlow<List<MedMateNotification>>(emptyList())
        val notificationsFlow: StateFlow<List<MedMateNotification>> = _notificationsFlow.asStateFlow()

        @Volatile
        private var isInitialized = false
    }

    init {
        if (!isInitialized) {
            repositoryScope.launch(Dispatchers.IO) {
                // One-time migration from legacy plaintext preferences
                migrateFromPlaintextPrefs(applicationContext, prefs)
                loadFromPrefs()
                isInitialized = true
            }
        }
    }

    /**
     * One-time migration: if the old plaintext prefs file exists, copy all its entries
     * into the encrypted store, then clear and delete the old file.
     * Wrapped in try/catch so the app never crashes if it fails.
     * Logs only a generic message, never personal or medicine data.
     */
    private fun migrateFromPlaintextPrefs(context: Context, encryptedPrefs: SharedPreferences) {
        try {
            val oldPrefs = context.getSharedPreferences(OLD_PLAINTEXT_PREFS_NAME, Context.MODE_PRIVATE)
            val oldEntries = oldPrefs.all
            if (oldEntries.isNotEmpty()) {
                val editor = encryptedPrefs.edit()
                for ((key, value) in oldEntries) {
                    when (value) {
                        is String -> editor.putString(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Float -> editor.putFloat(key, value)
                        is Set<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            editor.putStringSet(key, value as? Set<String>)
                        }
                    }
                }
                editor.apply()
                oldPrefs.edit().clear().apply()

                // Delete the old plaintext XML file from disk
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.deleteSharedPreferences(OLD_PLAINTEXT_PREFS_NAME)
                } else {
                    val legacyFile = File(context.filesDir.parent, "shared_prefs/$OLD_PLAINTEXT_PREFS_NAME.xml")
                    if (legacyFile.exists()) {
                        legacyFile.delete()
                    }
                }
                Log.i(TAG, "Legacy notification preferences migration completed successfully.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Legacy notification preferences migration encountered an error or was skipped.")
        }
    }

    /**
     * Loads notifications from encrypted SharedPreferences on Dispatchers.IO.
     */
    private suspend fun loadFromPrefs() = withContext(Dispatchers.IO) {
        val jsonStr = prefs.getString(KEY_NOTIFICATIONS, null)
        if (jsonStr.isNullOrBlank()) {
            _notificationsFlow.value = emptyList()
            return@withContext
        }

        try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<MedMateNotification>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    MedMateNotification(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title"),
                        message = obj.optString("message"),
                        medicineName = obj.optString("medicineName"),
                        dosage = obj.optString("dosage"),
                        slotName = obj.optString("slotName"),
                        scheduledTime = obj.optString("scheduledTime"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", false)
                    )
                )
            }
            _notificationsFlow.value = list
        } catch (e: Exception) {
            _notificationsFlow.value = emptyList()
        }
    }

    /**
     * Serializes and writes notifications into encrypted SharedPreferences on Dispatchers.IO.
     */
    private suspend fun saveToPrefs(list: List<MedMateNotification>) = withContext(Dispatchers.IO) {
        _notificationsFlow.value = list
        try {
            val jsonArray = JSONArray()
            list.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("message", item.message)
                    put("medicineName", item.medicineName)
                    put("dosage", item.dosage)
                    put("slotName", item.slotName)
                    put("scheduledTime", item.scheduledTime)
                    put("timestamp", item.timestamp)
                    put("isRead", item.isRead)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_NOTIFICATIONS, jsonArray.toString()).apply()
        } catch (ignored: Exception) {}
    }

    /**
     * Adds a medicine reminder notification. Executes on Dispatchers.IO.
     */
    suspend fun addNotification(
        title: String,
        message: String,
        medicineName: String = "",
        dosage: String = "",
        slotName: String = "",
        scheduledTime: String = ""
    ) = withContext(Dispatchers.IO) {
        val newNotification = MedMateNotification(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            medicineName = medicineName,
            dosage = dosage,
            slotName = slotName,
            scheduledTime = scheduledTime,
            timestamp = System.currentTimeMillis()
        )
        val currentList = _notificationsFlow.value.toMutableList()
        currentList.add(0, newNotification)
        if (currentList.size > 50) {
            currentList.removeAt(currentList.lastIndex)
        }
        saveToPrefs(currentList)
    }

    /**
     * Clears all saved notifications. Executes on Dispatchers.IO.
     */
    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        saveToPrefs(emptyList())
    }

    /**
     * Deletes a single notification by id. Executes on Dispatchers.IO.
     */
    suspend fun deleteNotification(id: String) = withContext(Dispatchers.IO) {
        val currentList = _notificationsFlow.value.filter { it.id != id }
        saveToPrefs(currentList)
    }
}
