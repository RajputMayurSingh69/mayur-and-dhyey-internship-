package com.example.mayuranddhyeyinternshipproject.data.repository

import com.example.mayuranddhyeyinternshipproject.data.model.DoseRecord
import com.example.mayuranddhyeyinternshipproject.data.model.DoseStatus
import com.example.mayuranddhyeyinternshipproject.data.model.Medicine
import com.example.mayuranddhyeyinternshipproject.data.model.TimeSlot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Handles dose intake records, dose evaluation (On Time, Slightly Late, Very Late),
 * and history queries with filters.
 */
class HistoryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun getHistoryCollection(userId: String) =
        firestore.collection("users").document(userId).collection("history")

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getCurrentTimeString(): String {
        return SimpleDateFormat("HH:mm", Locale.US).format(Date())
    }

    /**
     * Real-time stream of all history records for a user ordered by timestamp descending.
     */
    fun getHistoryStream(userId: String): Flow<List<DoseRecord>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = getHistoryCollection(userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(DoseRecord::class.java) } ?: emptyList()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Real-time stream of records for today derived from user's history stream.
     */
    fun getTodayRecordsStream(userId: String): Flow<List<DoseRecord>> {
        val today = getTodayDateString()
        return getHistoryStream(userId).map { list ->
            list.filter { it.date == today }
        }
    }

    /**
     * Determines dose status according to the app rules:
     * - On time: within ±15 minutes of scheduled time
     * - Slightly late: 15–60 minutes late
     * - Very late: > 60 minutes late
     */
    fun evaluateDoseStatus(scheduledTime: String, takenTime: String): DoseStatus =
        ValidationUtils.evaluateDoseStatus(scheduledTime, takenTime)

    /**
     * Marks a dose as taken and logs it in Firestore history.
     */
    suspend fun recordDoseTaken(
        userId: String,
        medicine: Medicine,
        slot: TimeSlot,
        scheduledTime: String
    ): Result<DoseRecord> {
        return try {
            val nowTime = getCurrentTimeString()
            val todayDate = getTodayDateString()
            val status = evaluateDoseStatus(scheduledTime, nowTime)

            val docRef = getHistoryCollection(userId).document()
            val record = DoseRecord(
                id = docRef.id,
                userId = userId,
                medicineId = medicine.id,
                medicineName = medicine.name,
                dosage = medicine.dosage,
                slot = slot.name,
                scheduledTime = scheduledTime,
                takenTime = nowTime,
                date = todayDate,
                status = status.name,
                timestamp = System.currentTimeMillis()
            )

            docRef.set(record).await()
            Result.success(record)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
