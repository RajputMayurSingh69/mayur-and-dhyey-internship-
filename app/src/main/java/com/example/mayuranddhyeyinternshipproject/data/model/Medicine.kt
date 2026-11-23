package com.example.mayuranddhyeyinternshipproject.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Medicine data class stored under "users/{userId}/medicines/{medicineId}".
 */
@Keep
@IgnoreExtraProperties
data class Medicine(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val dosage: String = "",
    val form: String = MedicineForm.TABLET.name,
    val notes: String = "",
    val reminderTimes: Map<String, String> = emptyMap(),
    val pharmacyName: String = "",
    val pharmacyContact: String = "",
    val doctorName: String = "",
    val doctorContact: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun hasSlot(slot: TimeSlot): Boolean {
        return reminderTimes.containsKey(slot.name) && reminderTimes[slot.name]?.isNotBlank() == true
    }

    fun getTimeForSlot(slot: TimeSlot): String? {
        return reminderTimes[slot.name]
    }
}
