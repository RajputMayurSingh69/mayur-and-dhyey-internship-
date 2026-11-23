package com.example.mayuranddhyeyinternshipproject.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.mayuranddhyeyinternshipproject.data.model.MedMateNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Manages in-app notifications and reminders with persistence in SharedPreferences.
 */
class NotificationRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("medmate_notifications_pref", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOTIFICATIONS = "key_notifications_list"
        private val _notificationsFlow = MutableStateFlow<List<MedMateNotification>>(emptyList())
        val notificationsFlow: StateFlow<List<MedMateNotification>> = _notificationsFlow.asStateFlow()
        private var isInitialized = false
    }

    init {
        if (!isInitialized) {
            loadFromPrefs()
            isInitialized = true
        }
    }

    private fun loadFromPrefs() {
        val jsonStr = prefs.getString(KEY_NOTIFICATIONS, null)
        if (jsonStr.isNullOrBlank()) {
            _notificationsFlow.value = emptyList()
            return
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

    private fun saveToPrefs(list: List<MedMateNotification>) {
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

    fun addNotification(
        title: String,
        message: String,
        medicineName: String = "",
        dosage: String = "",
        slotName: String = "",
        scheduledTime: String = ""
    ) {
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
        // Keep max 50 recent notifications
        if (currentList.size > 50) {
            currentList.removeAt(currentList.lastIndex)
        }
        saveToPrefs(currentList)
    }

    fun clearAllNotifications() {
        saveToPrefs(emptyList())
    }

    fun deleteNotification(id: String) {
        val currentList = _notificationsFlow.value.filter { it.id != id }
        saveToPrefs(currentList)
    }
}
