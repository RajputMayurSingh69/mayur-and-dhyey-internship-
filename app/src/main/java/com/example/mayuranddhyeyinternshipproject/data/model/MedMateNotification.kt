package com.example.mayuranddhyeyinternshipproject.data.model

import androidx.annotation.Keep

@Keep
data class MedMateNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val slotName: String = "",
    val scheduledTime: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
