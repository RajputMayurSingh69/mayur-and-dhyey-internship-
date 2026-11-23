package com.example.mayuranddhyeyinternshipproject.data.model

/**
 * Common medicine physical formulations.
 */
enum class MedicineForm(val displayName: String) {
    TABLET("Tablet"),
    CAPSULE("Capsule"),
    SYRUP("Syrup"),
    INJECTION("Injection"),
    DROPS("Drops"),
    INHALER("Inhaler"),
    OINTMENT("Ointment"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): MedicineForm {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) } ?: TABLET
        }
    }
}
