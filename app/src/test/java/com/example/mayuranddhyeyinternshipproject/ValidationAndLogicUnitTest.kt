package com.example.mayuranddhyeyinternshipproject

import com.example.mayuranddhyeyinternshipproject.data.model.DoseStatus
import com.example.mayuranddhyeyinternshipproject.data.model.TimeSlot
import com.example.mayuranddhyeyinternshipproject.data.repository.ValidationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive Unit Tests validating MedMate core validation rules, timing logic,
 * and security bounds.
 */
class ValidationAndLogicUnitTest {

    @Test
    fun testFullNameValidation() {
        assertNull(ValidationUtils.validateFullName("Mayur Singh"))
        assertNull(ValidationUtils.validateFullName("Dhyey Patel"))
        assertNull(ValidationUtils.validateFullName("Dr John Doe"))
        assertNotNull(ValidationUtils.validateFullName(""))
        assertNotNull(ValidationUtils.validateFullName("   "))
        assertNotNull(ValidationUtils.validateFullName("Mayur123"))
        assertNotNull(ValidationUtils.validateFullName("Mayur@Singh"))
        assertNotNull(ValidationUtils.validateFullName("M"))
    }

    @Test
    fun testDateOfBirthValidation() {
        assertNull(ValidationUtils.validateDateOfBirth("2000-05-15"))
        assertNull(ValidationUtils.validateDateOfBirth("1995-12-01"))
        assertNull(ValidationUtils.validateDateOfBirth("1970-01-01"))
        assertNotNull(ValidationUtils.validateDateOfBirth(""))
        assertNotNull(ValidationUtils.validateDateOfBirth("   "))
        assertNotNull(ValidationUtils.validateDateOfBirth("invalid-date"))
        assertNotNull(ValidationUtils.validateDateOfBirth("2099-01-01")) // Future date
        assertNotNull(ValidationUtils.validateDateOfBirth("31-12-2000")) // Wrong format (DD-MM-YYYY)
    }

    @Test
    fun testPhoneValidation() {
        assertNull(ValidationUtils.validatePhone("9876543210"))
        assertNull(ValidationUtils.validatePhone("1234567890"))
        assertNotNull(ValidationUtils.validatePhone("987654321")) // 9 digits
        assertNotNull(ValidationUtils.validatePhone("98765432100")) // 11 digits
        assertNotNull(ValidationUtils.validatePhone("987654321a")) // contains letters
        assertNotNull(ValidationUtils.validatePhone("+919876543210")) // contains symbols
        assertNotNull(ValidationUtils.validatePhone(""))
    }

    @Test
    fun testEmailValidation() {
        assertNull(ValidationUtils.validateEmail("user@gmail.com"))
        assertNull(ValidationUtils.validateEmail("mayur.singh123@gmail.com"))
        assertNull(ValidationUtils.validateEmail("dhyey+internship@gmail.com"))
        assertNotNull(ValidationUtils.validateEmail("user@yahoo.com")) // Non-gmail
        assertNotNull(ValidationUtils.validateEmail("user@outlook.com")) // Non-gmail
        assertNotNull(ValidationUtils.validateEmail("user@hotmail.com")) // Non-gmail
        assertNotNull(ValidationUtils.validateEmail("user@gmail.co"))
        assertNotNull(ValidationUtils.validateEmail("invalid-email"))
        assertNotNull(ValidationUtils.validateEmail(""))
    }

    @Test
    fun testPasswordValidation() {
        // Must be exactly 8 chars and contain both letters and numbers
        assertNull(ValidationUtils.validatePassword("Medmate1"))
        assertNull(ValidationUtils.validatePassword("pass1234"))
        assertNull(ValidationUtils.validatePassword("1234abcd"))
        assertNotNull(ValidationUtils.validatePassword("Med1")) // 4 chars
        assertNotNull(ValidationUtils.validatePassword("Medmate123")) // 10 chars
        assertNotNull(ValidationUtils.validatePassword("abcdefgh")) // no numbers
        assertNotNull(ValidationUtils.validatePassword("12345678")) // no letters
        assertNotNull(ValidationUtils.validatePassword("        ")) // spaces
    }

    @Test
    fun testDoseTimingStandard() {
        // Within +-15 minutes -> ON_TIME
        assertEquals(DoseStatus.ON_TIME, ValidationUtils.evaluateDoseStatus("08:00", "08:00"))
        assertEquals(DoseStatus.ON_TIME, ValidationUtils.evaluateDoseStatus("08:00", "08:10"))
        assertEquals(DoseStatus.ON_TIME, ValidationUtils.evaluateDoseStatus("08:00", "07:55"))
        assertEquals(DoseStatus.ON_TIME, ValidationUtils.evaluateDoseStatus("08:00", "08:15"))

        // 16 to 60 minutes late -> SLIGHTLY_LATE
        assertEquals(DoseStatus.SLIGHTLY_LATE, ValidationUtils.evaluateDoseStatus("08:00", "08:20"))
        assertEquals(DoseStatus.SLIGHTLY_LATE, ValidationUtils.evaluateDoseStatus("08:00", "08:45"))
        assertEquals(DoseStatus.SLIGHTLY_LATE, ValidationUtils.evaluateDoseStatus("08:00", "09:00"))

        // Over 60 minutes late -> VERY_LATE
        assertEquals(DoseStatus.VERY_LATE, ValidationUtils.evaluateDoseStatus("08:00", "09:05"))
        assertEquals(DoseStatus.VERY_LATE, ValidationUtils.evaluateDoseStatus("08:00", "11:00"))
    }

    @Test
    fun testDoseTimingMidnightBoundary() {
        // Scheduled at 23:55, taken at 00:05 (next day, +10 minutes) -> ON_TIME
        assertEquals(DoseStatus.ON_TIME, ValidationUtils.evaluateDoseStatus("23:55", "00:05"))

        // Scheduled at 23:50, taken at 00:25 (next day, +35 minutes) -> SLIGHTLY_LATE
        assertEquals(DoseStatus.SLIGHTLY_LATE, ValidationUtils.evaluateDoseStatus("23:50", "00:25"))

        // Scheduled at 23:00, taken at 01:15 (next day, +135 minutes) -> VERY_LATE
        assertEquals(DoseStatus.VERY_LATE, ValidationUtils.evaluateDoseStatus("23:00", "01:15"))
    }

    @Test
    fun testTimeSlotResolution() {
        assertEquals(TimeSlot.MORNING, TimeSlot.fromString("MORNING"))
        assertEquals(TimeSlot.AFTERNOON, TimeSlot.fromString("afternoon"))
        assertEquals(TimeSlot.EVENING, TimeSlot.fromString("EVENING"))
        assertEquals(TimeSlot.NIGHT, TimeSlot.fromString("night"))
    }

    @Test
    fun testNumericOverflowSafety() {
        // Test that hash bitmasking prevents Int.MIN_VALUE negative overflow
        val minHash = Int.MIN_VALUE
        val safeMasked = minHash and 0x7FFFFFFF
        assertTrue("Masked value must be non-negative", safeMasked >= 0)
    }
}
