package com.example.mayuranddhyeyinternshipproject

import android.app.Application
import android.content.pm.ApplicationInfo
import com.example.mayuranddhyeyinternshipproject.reminder.NotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Application class for MedMate.
 * Configures Firebase App Check, offline Firestore caching, notification channels, and global safety defaults.
 */
class MedMateApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Firebase & App Check (Phase 3)
        try {
            FirebaseApp.initializeApp(this)
            initAppCheck()
        } catch (ignored: Exception) {}

        // 2. Initialize Notification Channels
        try {
            NotificationHelper(this)
        } catch (ignored: Exception) {}

        // 3. Enable persistent offline Firestore caching for instant offline app experience
        try {
            val firestore = FirebaseFirestore.getInstance()
            val cacheSettings = com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build()
            firestore.firestoreSettings = settings
        } catch (ignored: Exception) {}
    }

    /**
     * Initializes Firebase App Check:
     * - Debug build: DebugAppCheckProviderFactory (generates debug token for testing in Logcat)
     * - Release build: PlayIntegrityAppCheckProviderFactory (attestation via Google Play Integrity)
     */
    private fun initAppCheck() {
        try {
            val appCheck = FirebaseAppCheck.getInstance()
            val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

            if (isDebug) {
                // Load DebugAppCheckProviderFactory dynamically to allow debugImplementation in Gradle
                try {
                    val debugFactoryClass = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                    val getInstanceMethod = debugFactoryClass.getMethod("getInstance")
                    val factory = getInstanceMethod.invoke(null) as AppCheckProviderFactory
                    appCheck.installAppCheckProviderFactory(factory)
                } catch (e: Exception) {
                    appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
                }
            } else {
                appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
            }
        } catch (ignored: Exception) {}
    }
}
