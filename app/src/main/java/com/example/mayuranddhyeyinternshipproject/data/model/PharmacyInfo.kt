package com.example.mayuranddhyeyinternshipproject.data.model

/**
 * Nearby Pharmacy entity with location and contact details.
 */
data class PharmacyInfo(
    val id: String,
    val name: String,
    val phone: String,
    val address: String,
    val distanceKm: Double,
    val is24Hours: Boolean = false
)
