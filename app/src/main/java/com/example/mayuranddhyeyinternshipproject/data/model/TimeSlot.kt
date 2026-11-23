package com.example.mayuranddhyeyinternshipproject.data.model

/**
 * The 4 daily medicine reminder time slots.
 */
enum class TimeSlot(val title: String, val defaultHour: Int, val defaultMinute: Int) {
    MORNING("Morning", 8, 0),
    AFTERNOON("Afternoon", 13, 0),
    EVENING("Evening", 18, 0),
    NIGHT("Night", 21, 0);

    fun formatDefaultTime(): String {
        val amPm = if (defaultHour >= 12) "PM" else "AM"
        val displayHour = when {
            defaultHour == 0 -> 12
            defaultHour > 12 -> defaultHour - 12
            else -> defaultHour
        }
        return String.format("%02d:%02d %s", displayHour, defaultMinute, amPm)
    }

    companion object {
        fun fromString(value: String): TimeSlot {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MORNING
        }
    }
}
