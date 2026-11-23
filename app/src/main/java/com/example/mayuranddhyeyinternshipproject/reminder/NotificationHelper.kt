package com.example.mayuranddhyeyinternshipproject.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mayuranddhyeyinternshipproject.MainActivity
import com.example.mayuranddhyeyinternshipproject.R

/**
 * Handles creation of Notification Channels and building reminder notifications
 * with audible ringtone, vibration, Heads-Up pop-up alert, and interactive action buttons.
 */
class NotificationHelper(private val context: Context) {

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                enableLights(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            val notificationManager: NotificationManager? =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(
        notificationId: Int,
        userId: String,
        medicineId: String,
        medicineName: String,
        dosage: String,
        slotName: String,
        scheduledTime: String
    ) {
        val safeNotificationId = notificationId and 0x7FFFFFFF

        // Main intent to open app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MEDICINE_ID, medicineId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            safeNotificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Request codes guaranteed never to overflow or collide
        val reqTaken = ((safeNotificationId.toLong() * 31 + 1) and 0x7FFFFFFF).toInt()
        val reqSnooze = ((safeNotificationId.toLong() * 31 + 2) and 0x7FFFFFFF).toInt()

        // Action: Taken
        val takenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_TAKEN
            putExtra(EXTRA_NOTIFICATION_ID, safeNotificationId)
            putExtra(EXTRA_USER_ID, userId)
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_SLOT_NAME, slotName)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            reqTaken,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze (15 minutes)
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_NOTIFICATION_ID, safeNotificationId)
            putExtra(EXTRA_USER_ID, userId)
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_SLOT_NAME, slotName)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reqSnooze,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💊 MedMate: Time for $medicineName")
            .setContentText("Dose: $dosage ($slotName)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Please take your $medicineName ($dosage) scheduled for $slotName at $scheduledTime.\nTaking medicines on time keeps you healthy!")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setAutoCancel(true)
            .setFullScreenIntent(contentPendingIntent, true) // Heads-Up pop-up banner on screen!
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_agenda, context.getString(R.string.action_taken), takenPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, context.getString(R.string.action_snooze), snoozePendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(safeNotificationId, builder.build())
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    companion object {
        const val CHANNEL_ID = "medmate_alarms_v3"
        const val ACTION_MARK_TAKEN = "com.example.mayuranddhyeyinternshipproject.ACTION_MARK_TAKEN"
        const val ACTION_SNOOZE = "com.example.mayuranddhyeyinternshipproject.ACTION_SNOOZE"

        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_MEDICINE_ID = "extra_medicine_id"
        const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_SLOT_NAME = "extra_slot_name"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    }
}
