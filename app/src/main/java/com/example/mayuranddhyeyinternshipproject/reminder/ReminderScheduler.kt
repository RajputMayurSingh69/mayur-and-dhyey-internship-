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
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

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
     * Schedules a single exact alarm for a specific medicine slot.
     */
    fun scheduleExactSlotAlarm(medicine: Medicine, slot: TimeSlot, timeStr: String) {
        val parts = timeStr.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: slot.defaultHour
        val minute = parts[1].toIntOrNull() ?: slot.defaultMinute

        val targetCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If time has already passed today, schedule for tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetCal.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetCal.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetCal.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback for security restrictions
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                targetCal.timeInMillis,
                pendingIntent
            )
        }
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
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000)
        val slot = TimeSlot.fromString(slotName)
        val requestCode = generateRequestCode(medicineId, slot) + 9999

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

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancels reminders for all slots of a medicine.
     */
    fun cancelMedicineReminders(medicineId: String) {
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
        return (medicineId.hashCode() * 31 + slot.ordinal).let { if (it < 0) -it else it }
    }

    companion object {
        const val ACTION_REMINDER = "com.example.mayuranddhyeyinternshipproject.ACTION_REMINDER"
    }
}
