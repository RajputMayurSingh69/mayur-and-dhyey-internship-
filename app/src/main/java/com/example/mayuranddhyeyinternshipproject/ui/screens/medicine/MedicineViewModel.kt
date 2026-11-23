package com.example.mayuranddhyeyinternshipproject.ui.screens.medicine

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mayuranddhyeyinternshipproject.data.model.Medicine
import com.example.mayuranddhyeyinternshipproject.data.model.MedicineForm
import com.example.mayuranddhyeyinternshipproject.data.model.PharmacyInfo
import com.example.mayuranddhyeyinternshipproject.data.model.TimeSlot
import com.example.mayuranddhyeyinternshipproject.data.repository.AuthRepository
import com.example.mayuranddhyeyinternshipproject.data.repository.MedicineRepository
import com.example.mayuranddhyeyinternshipproject.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MedicineUiState(
    val isLoading: Boolean = false,
    val medicines: List<Medicine> = emptyList(),
    val currentMedicine: Medicine? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val nearbyPharmacies: List<PharmacyInfo> = listOf(
        PharmacyInfo("1", "Apollo Pharmacy 24/7", "+91 9876543210", "Main Street, Central Block", 0.8, true),
        PharmacyInfo("2", "MedPlus Chemist & Druggist", "+91 9876543211", "Park View Avenue, Sector 4", 1.2, false),
        PharmacyInfo("3", "Wellness Forever Day & Night", "+91 9876543212", "Ring Road, Near City Mall", 2.1, true),
        PharmacyInfo("4", "Care Pharmacy & Surgical", "+91 9876543213", "Civil Lines, Hospital Road", 3.0, false),
        PharmacyInfo("5", "City Health Medical Store", "+91 9876543214", "Station Road, Market Yard", 3.8, false)
    )
)

class MedicineViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val medicineRepository: MedicineRepository = MedicineRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicineUiState())
    val uiState: StateFlow<MedicineUiState> = _uiState.asStateFlow()

    val currentUserId: String get() = authRepository.getCurrentUserId() ?: ""

    init {
        loadMedicines()
    }

    fun loadMedicines() {
        val uid = currentUserId
        if (uid.isEmpty()) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            medicineRepository.getMedicinesStream(uid).collect { list ->
                _uiState.update { it.copy(isLoading = false, medicines = list) }
            }
        }
    }

    fun loadMedicine(medicineId: String) {
        val uid = currentUserId
        if (medicineId.isBlank() || medicineId == "new") {
            _uiState.update { it.copy(currentMedicine = null, saveSuccess = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val med = medicineRepository.getMedicine(uid, medicineId)
            _uiState.update { it.copy(isLoading = false, currentMedicine = med, saveSuccess = false) }
        }
    }

    fun saveMedicine(
        context: Context,
        id: String,
        name: String,
        dosage: String,
        form: MedicineForm,
        notes: String,
        reminderTimes: Map<String, String>,
        pharmacyName: String,
        pharmacyContact: String,
        doctorName: String,
        doctorContact: String
    ) {
        val uid = currentUserId
        if (uid.isEmpty()) return

        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Medicine name is required.") }
            return
        }
        if (dosage.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Dosage is required (e.g., 500 mg, 1 tablet).") }
            return
        }
        if (reminderTimes.values.none { it.isNotBlank() }) {
            _uiState.update { it.copy(errorMessage = "Please enable at least one reminder time slot.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val scheduler = ReminderScheduler(context)

            val medicineToSave = Medicine(
                id = id,
                userId = uid,
                name = name.trim(),
                dosage = dosage.trim(),
                form = form.name,
                notes = notes.trim(),
                reminderTimes = reminderTimes,
                pharmacyName = pharmacyName.trim(),
                pharmacyContact = pharmacyContact.trim(),
                doctorName = doctorName.trim(),
                doctorContact = doctorContact.trim()
            )

            val result = if (id.isBlank() || id == "new") {
                medicineRepository.addMedicine(uid, medicineToSave)
            } else {
                medicineRepository.updateMedicine(uid, medicineToSave).map { id }
            }

            result.fold(
                onSuccess = { savedId ->
                    val fullMedicine = medicineToSave.copy(id = savedId)
                    scheduler.scheduleMedicineReminders(fullMedicine)
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true, errorMessage = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = error.localizedMessage ?: "Failed to save medicine.") }
                }
            )
        }
    }

    fun deleteMedicine(context: Context, medicineId: String) {
        val uid = currentUserId
        if (uid.isEmpty() || medicineId.isBlank()) return

        viewModelScope.launch {
            val scheduler = ReminderScheduler(context)
            scheduler.cancelMedicineReminders(medicineId)

            medicineRepository.deleteMedicine(uid, medicineId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
