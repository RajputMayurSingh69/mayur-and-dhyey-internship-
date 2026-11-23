package com.example.mayuranddhyeyinternshipproject.data.model

/**
 * Status of medicine intake.
 * - ON_TIME: Taken within ±15 minutes of scheduled time
 * - SLIGHTLY_LATE: Taken between 15 and 60 minutes after scheduled time
 * - VERY_LATE: Taken > 60 minutes after scheduled time
 * - PENDING: Not yet taken for the day
 * - MISSED: Slot passed without being taken
 */
enum class DoseStatus(val label: String) {
    ON_TIME("On Time"),
    SLIGHTLY_LATE("Slightly Late"),
    VERY_LATE("Very Late"),
    PENDING("Pending"),
    MISSED("Missed");

    companion object {
        fun fromString(value: String): DoseStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}
