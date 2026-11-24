package com.example.mayuranddhyeyinternshipproject.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * User Profile data class stored in Cloud Firestore under "users/{userId}".
 * Health data notice: Contains user personal data; protected under Firestore security rules.
 */
@Keep
@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val fullPhone: String = "",
    val dob: String = "", // Format: YYYY-MM-DD
    val defaultTimes: Map<String, String> = mapOf(
        TimeSlot.MORNING.name to "08:00",
        TimeSlot.AFTERNOON.name to "13:00",
        TimeSlot.EVENING.name to "18:00",
        TimeSlot.NIGHT.name to "21:00"
    ),
    val pharmacyName: String = "",
    val pharmacyContact: String = "",
    val pharmacyAddress: String = "",
    val doctorName: String = "",
    val doctorContact: String = "",
    val rateLimitedUntil: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
