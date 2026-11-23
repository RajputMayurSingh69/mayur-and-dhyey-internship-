package com.example.mayuranddhyeyinternshipproject.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mayuranddhyeyinternshipproject.data.repository.AuthRepository
import com.example.mayuranddhyeyinternshipproject.data.repository.MedicineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Reschedules all active medicine reminders upon device reboot (BOOT_COMPLETED).
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            scope.launch {
                try {
                    val authRepo = AuthRepository()
                    val userId = authRepo.getCurrentUserId()
                    if (!userId.isNullOrBlank()) {
                        val medicineRepo = MedicineRepository()
                        val scheduler = ReminderScheduler(context)
                        val medicines = medicineRepo.getMedicinesStream(userId).firstOrNull() ?: emptyList()
                        medicines.forEach { medicine ->
                            scheduler.scheduleMedicineReminders(medicine)
                        }
                    }
                } catch (e: Exception) {
                    // Suppress error
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
