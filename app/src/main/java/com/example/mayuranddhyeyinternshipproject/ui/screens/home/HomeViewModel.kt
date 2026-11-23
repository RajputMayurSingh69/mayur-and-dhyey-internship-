package com.example.mayuranddhyeyinternshipproject.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mayuranddhyeyinternshipproject.data.model.DoseRecord
import com.example.mayuranddhyeyinternshipproject.data.model.DoseStatus
import com.example.mayuranddhyeyinternshipproject.data.model.Medicine
import com.example.mayuranddhyeyinternshipproject.data.model.TimeSlot
import com.example.mayuranddhyeyinternshipproject.data.model.UserProfile
import com.example.mayuranddhyeyinternshipproject.data.repository.AuthRepository
import com.example.mayuranddhyeyinternshipproject.data.repository.HistoryRepository
import com.example.mayuranddhyeyinternshipproject.data.repository.MedicineRepository
import com.example.mayuranddhyeyinternshipproject.data.repository.UserRepository
import com.example.mayuranddhyeyinternshipproject.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduledSlotMedicine(
    val medicine: Medicine,
    val slot: TimeSlot,
    val scheduledTime: String,
    val isTaken: Boolean,
    val doseRecord: DoseRecord? = null,
    val status: DoseStatus = DoseStatus.PENDING
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String = "Friend",
    val userProfile: UserProfile? = null,
    val medicines: List<Medicine> = emptyList(),
    val todayRecords: List<DoseRecord> = emptyList(),
    val slotMedicines: Map<TimeSlot, List<ScheduledSlotMedicine>> = emptyMap(),
    val totalDosesToday: Int = 0,
    val takenDosesToday: Int = 0,
    val lastRecordedStatus: DoseStatus? = null,
    val showFeedbackDialog: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val medicineRepository: MedicineRepository = MedicineRepository(),
    private val historyRepository: HistoryRepository = HistoryRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val currentUserId: String get() = authRepository.getCurrentUserId() ?: ""

    init {
        loadData()
    }

    fun loadData() {
        val uid = currentUserId
        if (uid.isEmpty()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            combine(
                userRepository.getUserProfileStream(uid),
                medicineRepository.getMedicinesStream(uid),
                historyRepository.getTodayRecordsStream(uid)
            ) { profile, medicines, records ->
                val name = profile?.fullName?.takeIf { it.isNotBlank() } ?: "Friend"

                // Group medicines by slot
                val slotMap = mutableMapOf<TimeSlot, MutableList<ScheduledSlotMedicine>>()
                TimeSlot.entries.forEach { slotMap[it] = mutableListOf() }

                var totalDoses = 0
                var takenDoses = 0

                medicines.forEach { med ->
                    TimeSlot.entries.forEach { slot ->
                        val time = med.getTimeForSlot(slot)
                        if (!time.isNullOrBlank()) {
                            totalDoses++
                            // Check if this medicine and slot has been recorded today
                            val record = records.firstOrNull {
                                it.medicineId == med.id && it.slot.equals(slot.name, ignoreCase = true)
                            }
                            val isTaken = record != null
                            if (isTaken) takenDoses++

                            val status = when {
                                record != null -> DoseStatus.fromString(record.status)
                                else -> DoseStatus.PENDING
                            }

                            slotMap[slot]?.add(
                                ScheduledSlotMedicine(
                                    medicine = med,
                                    slot = slot,
                                    scheduledTime = time,
                                    isTaken = isTaken,
                                    doseRecord = record,
                                    status = status
                                )
                            )
                        }
                    }
                }

                HomeUiState(
                    isLoading = false,
                    userName = name,
                    userProfile = profile,
                    medicines = medicines,
                    todayRecords = records,
                    slotMedicines = slotMap,
                    totalDosesToday = totalDoses,
                    takenDosesToday = takenDoses,
                    errorMessage = null
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun markAsTaken(item: ScheduledSlotMedicine) {
        val uid = currentUserId
        if (uid.isEmpty()) return

        viewModelScope.launch {
            val result = historyRepository.recordDoseTaken(
                userId = uid,
                medicine = item.medicine,
                slot = item.slot,
                scheduledTime = item.scheduledTime
            )

            result.fold(
                onSuccess = { record ->
                    _uiState.update { current ->
                        val updatedRecords = listOf(record) + current.todayRecords.filter { it.id != record.id }
                        val updatedSlotMap = current.slotMedicines.toMutableMap().mapValues { (slot, list) ->
                            list.map { slotMed ->
                                if (slotMed.medicine.id == item.medicine.id && slotMed.slot == item.slot) {
                                    slotMed.copy(
                                        isTaken = true,
                                        doseRecord = record,
                                        status = DoseStatus.fromString(record.status)
                                    )
                                } else {
                                    slotMed
                                }
                            }
                        }
                        val newTakenDoses = updatedSlotMap.values.flatten().count { it.isTaken }

                        current.copy(
                            todayRecords = updatedRecords,
                            slotMedicines = updatedSlotMap,
                            takenDosesToday = newTakenDoses,
                            lastRecordedStatus = DoseStatus.fromString(record.status),
                            showFeedbackDialog = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.localizedMessage ?: "Failed to record dose.")
                    }
                }
            )
        }
    }

    fun dismissFeedbackDialog() {
        _uiState.update { it.copy(showFeedbackDialog = false, lastRecordedStatus = null) }
    }

    fun rescheduleAllAlarms(context: Context) {
        val scheduler = ReminderScheduler(context)
        _uiState.value.medicines.forEach { medicine ->
            scheduler.scheduleMedicineReminders(medicine)
        }
    }
}
