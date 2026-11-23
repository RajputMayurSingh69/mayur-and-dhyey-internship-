package com.example.mayuranddhyeyinternshipproject.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.mayuranddhyeyinternshipproject.data.model.Medicine
import com.example.mayuranddhyeyinternshipproject.data.model.TimeSlot
import java.util.Calendar

/**
 * Manages scheduling and cancelling exact alarms using Android AlarmManager.
 * Uses AlarmClockInfo for 100% exact delivery, Doze mode bypass, and status bar alarm indication.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    /**
     * Schedules daily reminders for all active slots of a medicine.
     */
    fun scheduleMedicineReminders(medicine: Medicine) {
        medicine.reminderTimes.forEach { (slotName, timeStr) ->
            if (timeStr.isNotBlank()) {
                val slot = TimeSlot.fromString(slotName)
                scheduleExactSlotAlarm(medicine, slot, timeStr)
            }
        }
    }

    /**
     * Schedules an exact alarm for a specific medicine slot using setAlarmClock for maximum precision.
     */
    fun scheduleExactSlotAlarm(medicine: Medicine, slot: TimeSlot, timeStr: String) {
        if (alarmManager == null) return
        val parts = timeStr.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: slot.defaultHour
        val minute = parts[1].toIntOrNull() ?: slot.defaultMinute

        val now = Calendar.getInstance()
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If time has already passed today by more than 1 minute, schedule for tomorrow
            if (timeInMillis < (now.timeInMillis - 60_000)) {
                add(Calendar.DAY_OF_YEAR, 1)
            } else if (timeInMillis <= now.timeInMillis) {
                // If it's the current minute or just seconds ago, trigger 5 seconds from now
                timeInMillis = now.timeInMillis + 5_000
            }
        }

        val requestCode = generateRequestCode(medicine.id, slot)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(NotificationHelper.EXTRA_USER_ID, medicine.userId)
            putExtra(NotificationHelper.EXTRA_MEDICINE_ID, medicine.id)
            putExtra(NotificationHelper.EXTRA_MEDICINE_NAME, medicine.name)
            putExtra(NotificationHelper.EXTRA_DOSAGE, medicine.dosage)
            putExtra(NotificationHelper.EXTRA_SLOT_NAME, slot.name)
            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, timeStr)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactWithFallback(targetCal.timeInMillis, pendingIntent)
    }

    /**
     * Schedules an immediate test alarm to ring in `seconds` seconds with sound and notification.
     */
    fun scheduleTestAlarm(medicineName: String = "Paracetamol", dosage: String = "500 mg", seconds: Int = 5) {
        if (alarmManager == null) return
        val triggerAtMillis = System.currentTimeMillis() + (seconds * 1000)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(NotificationHelper.EXTRA_USER_ID, "test_user")
            putExtra(NotificationHelper.EXTRA_MEDICINE_ID, "test_${System.currentTimeMillis()}")
            putExtra(NotificationHelper.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(NotificationHelper.EXTRA_DOSAGE, dosage)
            putExtra(NotificationHelper.EXTRA_SLOT_NAME, "Test Alarm")
            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, "Just Now")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactWithFallback(triggerAtMillis, pendingIntent)
    }

    /**
     * Schedules a one-shot snooze alarm (15 minutes from now).
     */
    fun scheduleSnoozeAlarm(
        userId: String,
        medicineId: String,
        medicineName: String,
        dosage: String,
        slotName: String,
        scheduledTime: String,
        snoozeMinutes: Int = 15
    ) {
        if (alarmManager == null) return
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000)
        val slot = TimeSlot.fromString(slotName)
        val requestCode = generateSnoozeRequestCode(medicineId, slot)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(NotificationHelper.EXTRA_USER_ID, userId)
            putExtra(NotificationHelper.EXTRA_MEDICINE_ID, medicineId)
            putExtra(NotificationHelper.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(NotificationHelper.EXTRA_DOSAGE, dosage)
            putExtra(NotificationHelper.EXTRA_SLOT_NAME, slotName)
            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, scheduledTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactWithFallback(triggerAtMillis, pendingIntent)
    }

    private fun scheduleExactWithFallback(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val am = alarmManager ?: return
        try {
            // Primary: setAlarmClock guarantees exact wakeup, sound, and status bar alarm display
            val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
            am.setAlarmClock(clockInfo, pendingIntent)
        } catch (e: Exception) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (am.canScheduleExactAlarms()) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (fallbackEx: Exception) {
                try {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } catch (ignored: Exception) {}
            }
        }
    }

    /**
     * Cancels reminders for all slots of a medicine.
     */
    fun cancelMedicineReminders(medicineId: String) {
        if (alarmManager == null) return
        TimeSlot.entries.forEach { slot ->
            val requestCode = generateRequestCode(medicineId, slot)
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    private fun generateRequestCode(medicineId: String, slot: TimeSlot): Int {
        return (((medicineId.hashCode().toLong() * 31 + slot.ordinal) and 0x7FFFFFFF) % 1000000).toInt()
    }

    private fun generateSnoozeRequestCode(medicineId: String, slot: TimeSlot): Int {
        return generateRequestCode(medicineId, slot) + 1000000
    }

    companion object {
        const val ACTION_REMINDER = "com.example.mayuranddhyeyinternshipproject.ACTION_REMINDER"
    }
}
