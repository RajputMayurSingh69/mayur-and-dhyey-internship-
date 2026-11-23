package com.example.mayuranddhyeyinternshipproject.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mayuranddhyeyinternshipproject.data.repository.AuthRepository
import com.example.mayuranddhyeyinternshipproject.data.repository.MedicineRepository
import com.example.mayuranddhyeyinternshipproject.data.repository.SecurityPreferences
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Reschedules all active medicine reminders upon device reboot (BOOT_COMPLETED).
 * Safe, idempotent, and non-blocking using goAsync() and Dispatchers.IO.
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        try {
            // Check that intent.action is one of the expected actions. If not, return immediately.
            val action = intent.action ?: return
            if (action != Intent.ACTION_BOOT_COMPLETED &&
                action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
                action != "android.intent.action.QUICKBOOT_POWERON"
            ) {
                return
            }

            // Do the rescheduling work on a background coroutine using goAsync()
            val pendingResult = goAsync()
            scope.launch(Dispatchers.IO) {
                try {
                    if (FirebaseApp.getApps(context).isEmpty()) {
                        FirebaseApp.initializeApp(context)
                    }

                    // Read user session data through the encrypted repository (never plaintext prefs)
                    val securityPreferences = SecurityPreferences(context)
                    val userId = securityPreferences.cachedUserId
                        ?: AuthRepository().getCurrentUserId()

                    if (!userId.isNullOrBlank()) {
                        val medicineRepo = MedicineRepository()
                        val scheduler = ReminderScheduler(context)
                        val medicines = medicineRepo.getMedicinesStream(userId).firstOrNull() ?: emptyList()

                        // Rescheduling is idempotent: creates/updates alarms with same request code and FLAG_UPDATE_CURRENT
                        medicines.forEach { medicine ->
                            scheduler.scheduleMedicineReminders(medicine)
                        }
                    }
                } catch (e: Exception) {
                    // Suppress and protect from crash
                } finally {
                    // Finish the broadcast transaction
                    pendingResult.finish()
                }
            }
        } catch (e: Exception) {
            // Failure must never crash the app
        }
    }
}
