package com.example.mayuranddhyeyinternshipproject.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.mayuranddhyeyinternshipproject.data.model.Medicine
import com.example.mayuranddhyeyinternshipproject.data.model.TimeSlot
import com.example.mayuranddhyeyinternshipproject.data.repository.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles actions from the notification:
 * - "Taken": records the dose intake directly into Firestore
 * - "Snooze": reschedules the alarm for 15 minutes later
 */
class NotificationActionReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 0)
        val rawUserId = intent.getStringExtra(NotificationHelper.EXTRA_USER_ID) ?: ""
        val userId = if (rawUserId.isNotBlank()) rawUserId else (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "")
        val medicineId = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_ID) ?: ""
        val medicineName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_NAME) ?: ""
        val dosage = intent.getStringExtra(NotificationHelper.EXTRA_DOSAGE) ?: ""
        val slotName = intent.getStringExtra(NotificationHelper.EXTRA_SLOT_NAME) ?: ""
        val scheduledTime = intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULED_TIME) ?: ""

        // Dismiss the notification safely
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (ignored: Exception) {}

        when (intent.action) {
            NotificationHelper.ACTION_MARK_TAKEN -> {
                if (userId.isNotBlank() && medicineId.isNotBlank()) {
                    val pendingResult = goAsync()
                    receiverScope.launch {
                        try {
                            val historyRepo = HistoryRepository()
                            val slot = TimeSlot.fromString(slotName)
                            val medicine = Medicine(
                                id = medicineId,
                                userId = userId,
                                name = medicineName,
                                dosage = dosage
                            )
                            historyRepo.recordDoseTaken(
                                userId = userId,
                                medicine = medicine,
                                slot = slot,
                                scheduledTime = scheduledTime
                            )
                        } catch (e: Exception) {
                            // Suppress in background
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            NotificationHelper.ACTION_SNOOZE -> {
                val scheduler = ReminderScheduler(context)
                scheduler.scheduleSnoozeAlarm(
                    userId = userId,
                    medicineId = medicineId,
                    medicineName = medicineName,
                    dosage = dosage,
                    slotName = slotName,
                    scheduledTime = scheduledTime,
                    snoozeMinutes = 15
                )
                Toast.makeText(context, "Reminder snoozed for 15 minutes", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
