package com.example.mayuranddhyeyinternshipproject.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Dose intake record stored under "users/{userId}/history/{historyId}".
 */
@IgnoreExtraProperties
data class DoseRecord(
    val id: String = "",
    val userId: String = "",
    val medicineId: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val slot: String = TimeSlot.MORNING.name,
    val scheduledTime: String = "", // e.g. "08:00"
    val takenTime: String = "", // e.g. "08:12"
    val date: String = "", // Format: "YYYY-MM-DD"
    val status: String = DoseStatus.ON_TIME.name,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
