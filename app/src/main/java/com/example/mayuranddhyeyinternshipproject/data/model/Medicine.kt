package com.example.mayuranddhyeyinternshipproject.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Medicine data class stored under "users/{userId}/medicines/{medicineId}".
 */
@IgnoreExtraProperties
data class Medicine(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val dosage: String = "",
    val form: String = MedicineForm.TABLET.name,
    val notes: String = "",
    // Map of TimeSlot.name -> time "HH:mm", e.g. {"MORNING": "08:30", "NIGHT": "21:00"}
    val reminderTimes: Map<String, String> = emptyMap(),
    val pharmacyName: String = "",
    val pharmacyContact: String = "",
    val doctorName: String = "",
    val doctorContact: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Checks if this medicine is scheduled for a given slot.
     */
    fun hasSlot(slot: TimeSlot): Boolean {
        return reminderTimes.containsKey(slot.name) && reminderTimes[slot.name]?.isNotBlank() == true
    }

    /**
     * Returns the formatted time string for a given slot, or null if not scheduled.
     */
    fun getTimeForSlot(slot: TimeSlot): String? {
        return reminderTimes[slot.name]
    }
}
