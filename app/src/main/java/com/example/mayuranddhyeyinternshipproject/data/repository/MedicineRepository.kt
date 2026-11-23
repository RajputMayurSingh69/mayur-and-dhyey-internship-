package com.example.mayuranddhyeyinternshipproject.data.repository

import com.example.mayuranddhyeyinternshipproject.data.model.Medicine
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Handles Firestore persistence for user's medicines under "users/{userId}/medicines".
 */
class MedicineRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun getMedicinesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("medicines")

    /**
     * Real-time stream of all medicines for a user.
     */
    fun getMedicinesStream(userId: String): Flow<List<Medicine>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = getMedicinesCollection(userId)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Medicine::class.java) } ?: emptyList()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Fetches a single medicine by id.
     */
    suspend fun getMedicine(userId: String, medicineId: String): Medicine? {
        return try {
            val doc = getMedicinesCollection(userId).document(medicineId).get().await()
            doc.toObject(Medicine::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Adds a new medicine with an auto-generated id.
     */
    suspend fun addMedicine(userId: String, medicine: Medicine): Result<String> {
        return try {
            val docRef = getMedicinesCollection(userId).document()
            val newMedicine = medicine.copy(
                id = docRef.id,
                userId = userId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            docRef.set(newMedicine).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing medicine.
     */
    suspend fun updateMedicine(userId: String, medicine: Medicine): Result<Unit> {
        return try {
            val updated = medicine.copy(
                userId = userId,
                updatedAt = System.currentTimeMillis()
            )
            getMedicinesCollection(userId).document(medicine.id).set(updated).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a medicine.
     */
    suspend fun deleteMedicine(userId: String, medicineId: String): Result<Unit> {
        return try {
            getMedicinesCollection(userId).document(medicineId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
