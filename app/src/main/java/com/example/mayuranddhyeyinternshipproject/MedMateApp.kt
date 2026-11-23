package com.example.mayuranddhyeyinternshipproject

import android.app.Application
import com.example.mayuranddhyeyinternshipproject.reminder.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Application class for MedMate.
 * Configures offline Firestore caching, notification channels, and global safety defaults.
 */
class MedMateApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Notification Channels
        try {
            NotificationHelper(this)
        } catch (ignored: Exception) {}

        // 2. Enable persistent offline Firestore caching for instant offline app experience
        try {
            val firestore = FirebaseFirestore.getInstance()
            val cacheSettings = com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build()
            firestore.firestoreSettings = settings
        } catch (ignored: Exception) {}
    }
}
