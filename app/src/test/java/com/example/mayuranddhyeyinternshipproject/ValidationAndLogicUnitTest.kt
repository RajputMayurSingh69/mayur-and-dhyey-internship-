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
 * Unit tests validating MedMate core validation rules and timing logic.
 */
class ValidationAndLogicUnitTest {

    @Test
    fun testFullNameValidation() {
        assertNull(ValidationUtils.validateFullName("Mayur Singh"))
        assertNull(ValidationUtils.validateFullName("Dhyey Patel"))
        assertNotNull(ValidationUtils.validateFullName(""))
        assertNotNull(ValidationUtils.validateFullName("Mayur123"))
        assertNotNull(ValidationUtils.validateFullName("Mayur@Singh"))
        assertNotNull(ValidationUtils.validateFullName("M"))
    }

    @Test
    fun testDateOfBirthValidation() {
        assertNull(ValidationUtils.validateDateOfBirth("2000-05-15"))
        assertNull(ValidationUtils.validateDateOfBirth("1995-12-01"))
        assertNotNull(ValidationUtils.validateDateOfBirth(""))
        assertNotNull(ValidationUtils.validateDateOfBirth("invalid-date"))
        assertNotNull(ValidationUtils.validateDateOfBirth("2099-01-01")) // Future date
    }

    @Test
    fun testPhoneValidation() {
        assertNull(ValidationUtils.validatePhone("9876543210"))
        assertNotNull(ValidationUtils.validatePhone("987654321")) // 9 digits
        assertNotNull(ValidationUtils.validatePhone("98765432100")) // 11 digits
        assertNotNull(ValidationUtils.validatePhone("987654321a")) // contains letters
        assertNotNull(ValidationUtils.validatePhone("+919876543210")) // contains symbols
    }

    @Test
    fun testEmailValidation() {
        assertNull(ValidationUtils.validateEmail("user@gmail.com"))
        assertNull(ValidationUtils.validateEmail("mayur.singh123@gmail.com"))
        assertNotNull(ValidationUtils.validateEmail("user@yahoo.com")) // Non-gmail
        assertNotNull(ValidationUtils.validateEmail("user@outlook.com")) // Non-gmail
        assertNotNull(ValidationUtils.validateEmail("invalid-email"))
        assertNotNull(ValidationUtils.validateEmail(""))
    }

    @Test
    fun testPasswordValidation() {
        // Must be exactly 8 chars and contain both letters and numbers
        assertNull(ValidationUtils.validatePassword("Medmate1"))
        assertNull(ValidationUtils.validatePassword("pass1234"))
        assertNotNull(ValidationUtils.validatePassword("Med1")) // 4 chars
        assertNotNull(ValidationUtils.validatePassword("Medmate123")) // 10 chars
        assertNotNull(ValidationUtils.validatePassword("abcdefgh")) // no numbers
        assertNotNull(ValidationUtils.validatePassword("12345678")) // no letters
    }

    @Test
    fun testDoseTimingEvaluation() {
        // Within +-15 minutes -> ON_TIME
        assertEquals(DoseStatus.ON_TIME, ValidationUtils.evaluateDoseStatus("08:00", "08:00"))
        assertEquals(DoseStatus.ON_TIME, ValidationUtils.evaluateDoseStatus("08:00", "08:10"))
        assertEquals(DoseStatus.ON_TIME, ValidationUtils.evaluateDoseStatus("08:00", "07:55"))
        assertEquals(DoseStatus.ON_TIME, ValidationUtils.evaluateDoseStatus("08:00", "08:15"))

        // 16 to 60 minutes late -> SLIGHTLY_LATE
        assertEquals(DoseStatus.SLIGHTLY_LATE, ValidationUtils.evaluateDoseStatus("08:00", "08:25"))
        assertEquals(DoseStatus.SLIGHTLY_LATE, ValidationUtils.evaluateDoseStatus("08:00", "08:45"))
        assertEquals(DoseStatus.SLIGHTLY_LATE, ValidationUtils.evaluateDoseStatus("08:00", "09:00"))

        // Over 60 minutes late -> VERY_LATE
        assertEquals(DoseStatus.VERY_LATE, ValidationUtils.evaluateDoseStatus("08:00", "09:15"))
        assertEquals(DoseStatus.VERY_LATE, ValidationUtils.evaluateDoseStatus("08:00", "11:00"))
    }

    @Test
    fun testTimeSlotResolution() {
        assertEquals(TimeSlot.MORNING, TimeSlot.fromString("MORNING"))
        assertEquals(TimeSlot.AFTERNOON, TimeSlot.fromString("afternoon"))
        assertEquals(TimeSlot.EVENING, TimeSlot.fromString("EVENING"))
        assertEquals(TimeSlot.NIGHT, TimeSlot.fromString("night"))
    }
}
