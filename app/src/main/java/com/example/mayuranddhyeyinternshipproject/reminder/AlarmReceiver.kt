package com.example.mayuranddhyeyinternshipproject.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Triggered by AlarmManager when a medicine reminder alarm fires.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val userId = intent.getStringExtra(NotificationHelper.EXTRA_USER_ID) ?: ""
        val medicineId = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_ID) ?: return
        val medicineName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_NAME) ?: "Medicine"
        val dosage = intent.getStringExtra(NotificationHelper.EXTRA_DOSAGE) ?: "1 dose"
        val slotName = intent.getStringExtra(NotificationHelper.EXTRA_SLOT_NAME) ?: "Daily"
        val scheduledTime = intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULED_TIME) ?: ""

        val notificationId = (medicineId.hashCode() * 31 + slotName.hashCode()).let {
            if (it < 0) -it else it
        }

        val notificationHelper = NotificationHelper(context)
        notificationHelper.showReminderNotification(
            notificationId = notificationId,
            userId = userId,
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            slotName = slotName,
            scheduledTime = scheduledTime
        )
    }
}
