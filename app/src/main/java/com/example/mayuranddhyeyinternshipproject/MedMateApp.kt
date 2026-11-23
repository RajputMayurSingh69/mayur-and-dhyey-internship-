package com.example.mayuranddhyeyinternshipproject

import android.app.Application
import com.example.mayuranddhyeyinternshipproject.reminder.NotificationHelper

/**
 * Application class for MedMate.
 * Initializes notification channels and global preferences safely.
 */
class MedMateApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Notification Channels
        NotificationHelper(this)
    }
}
