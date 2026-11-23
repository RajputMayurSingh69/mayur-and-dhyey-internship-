package com.example.mayuranddhyeyinternshipproject.data.repository

import com.example.mayuranddhyeyinternshipproject.data.model.DoseStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Pure Kotlin utilities for input validation and dose status timing logic.
 * Independent of Android/Firebase SDK to ensure instant, deterministic unit testing.
 */
object ValidationUtils {

    /**
     * Validates full name: required, letters and spaces only.
     */
    fun validateFullName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Full name is required."
        if (!trimmed.matches(Regex("^[a-zA-Z\\s]+$"))) {
            return "Name must contain letters and spaces only."
        }
        if (trimmed.length < 2) return "Name is too short."
        return null
    }

    /**
     * Validates date of birth: required, valid past date (YYYY-MM-DD), user must be at least 1 year old.
     */
    fun validateDateOfBirth(dob: String): String? {
        val trimmed = dob.trim()
        if (trimmed.isEmpty()) return "Date of birth is required."
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val date = try {
            sdf.parse(trimmed)
        } catch (e: Exception) {
            return "Invalid date format. Please select a valid date."
        } ?: return "Invalid date."

        val dobCal = Calendar.getInstance().apply { time = date }
        val today = Calendar.getInstance()

        if (dobCal.after(today)) {
            return "Date of birth cannot be in the future."
        }

        var age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        if (age < 1) {
            return "User must be at least 1 year old."
        }
        return null
    }

    /**
     * Validates phone: exactly 10 digits, digits only. Reject letters and symbols.
     */
    fun validatePhone(phone: String): String? {
        val trimmed = phone.trim()
        if (trimmed.isEmpty()) return "Phone number is required."
        if (!trimmed.matches(Regex("^[0-9]{10}$"))) {
            return "Phone number must be exactly 10 digits without symbols."
        }
        return null
    }

    /**
     * Validates email: must end with "@gmail.com" and have a valid format.
     */
    fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return "Email address is required."
        if (!trimmed.matches(Regex("^[a-zA-Z0-9._%+-]+@gmail\\.com$"))) {
            return "Email must be a valid @gmail.com address."
        }
        return null
    }

    /**
     * Validates password: exactly 8 characters, must contain both letters and numbers.
     */
    fun validatePassword(password: String): String? {
        if (password.length != 8) {
            return "Password must be exactly 8 characters."
        }
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        if (!hasLetter || !hasDigit) {
            return "Password must contain both letters and numbers."
        }
        return null
    }

    /**
     * Evaluates dose status according to scheduled time and actual intake time.
     * - Handles midnight wrap-around safely (e.g. scheduled at 23:55, taken at 00:05).
     * - ON_TIME: within ±15 minutes
     * - SLIGHTLY_LATE: 16–60 minutes late
     * - VERY_LATE: > 60 minutes late
     */
    fun evaluateDoseStatus(scheduledTime: String, takenTime: String): DoseStatus {
        val sdf = SimpleDateFormat("HH:mm", Locale.US).apply { isLenient = false }
        return try {
            val schedDate = sdf.parse(scheduledTime) ?: return DoseStatus.ON_TIME
            val takenDate = sdf.parse(takenTime) ?: return DoseStatus.ON_TIME

            var diffMinutes = (takenDate.time - schedDate.time) / (1000 * 60)

            // Normalize midnight wrap-around:
            // If taken after midnight for a late night dose (e.g. scheduled 23:55, taken 00:05 -> diff is -1430 -> +10)
            if (diffMinutes < -720) {
                diffMinutes += 1440
            } else if (diffMinutes > 720) {
                diffMinutes -= 1440
            }

            when {
                diffMinutes in -15..15 -> DoseStatus.ON_TIME
                diffMinutes in 16..60 -> DoseStatus.SLIGHTLY_LATE
                diffMinutes > 60 -> DoseStatus.VERY_LATE
                else -> DoseStatus.ON_TIME // Taken safely ahead of time
            }
        } catch (e: Exception) {
            DoseStatus.ON_TIME
        }
    }
}
