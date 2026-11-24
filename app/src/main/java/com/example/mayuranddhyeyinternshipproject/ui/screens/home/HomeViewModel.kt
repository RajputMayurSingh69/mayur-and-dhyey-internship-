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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Data class representing the UI state of a single scheduled dose on the Home screen.
 */
data class DoseUiState(
    val medicine: Medicine,
    val slot: TimeSlot,
    val scheduledTime: String,
    val isTaken: Boolean,
    val isPendingWrite: Boolean = false,
    val doseRecord: DoseRecord? = null,
    val status: DoseStatus = DoseStatus.PENDING
) {
    val id: String get() = "${medicine.id}_${slot.name}"
}

typealias ScheduledSlotMedicine = DoseUiState

/**
 * One-time UI events emitted by HomeViewModel.
 */
sealed interface HomeUiEvent {
    data class ShowSnackbar(val message: String) : HomeUiEvent
}

/**
 * Overall UI state for the Home screen.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String = "Friend",
    val userProfile: UserProfile? = null,
    val medicines: List<Medicine> = emptyList(),
    val todayRecords: List<DoseRecord> = emptyList(),
    val slotMedicines: Map<TimeSlot, List<DoseUiState>> = emptyMap(),
    val totalDosesToday: Int = 0,
    val takenDosesToday: Int = 0,
    val pendingDoseIds: Set<String> = emptySet(),
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

    private val _eventFlow = MutableSharedFlow<HomeUiEvent>()
    val eventFlow: SharedFlow<HomeUiEvent> = _eventFlow.asSharedFlow()

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
            // ONE single Firestore snapshot listener on the user's records ordered by timestamp
            combine(
                userRepository.getUserProfileStream(uid),
                medicineRepository.getMedicinesStream(uid),
                historyRepository.getHistoryStream(uid)
            ) { profile, medicines, historyRecords ->
                val name = profile?.fullName?.takeIf { it.isNotBlank() } ?: "Friend"

                // Filter today's records in memory for Home
                val today = historyRepository.getTodayDateString()
                val todayRecords = historyRecords.filter { it.date == today }

                val currentPending = _uiState.value.pendingDoseIds

                // Group medicines by time slot
                val slotMap = mutableMapOf<TimeSlot, MutableList<DoseUiState>>()
                TimeSlot.entries.forEach { slotMap[it] = mutableListOf() }

                var totalDoses = 0
                var takenDoses = 0

                medicines.forEach { med ->
                    TimeSlot.entries.forEach { slot ->
                        val time = med.getTimeForSlot(slot)
                        if (!time.isNullOrBlank()) {
                            totalDoses++
                            val doseId = "${med.id}_${slot.name}"
                            val isPending = currentPending.contains(doseId)

                            // Check if this medicine and slot has been recorded today in Firestore
                            val record = todayRecords.firstOrNull {
                                it.medicineId == med.id && it.slot.equals(slot.name, ignoreCase = true)
                            }

                            // Consider taken if recorded in Firestore or if optimistically marked taken while pending
                            val isTaken = record != null || isPending
                            if (isTaken) takenDoses++

                            val status = when {
                                record != null -> DoseStatus.fromString(record.status)
                                isPending -> {
                                    _uiState.value.slotMedicines[slot]
                                        ?.firstOrNull { it.id == doseId }?.status
                                        ?: DoseStatus.ON_TIME
                                }
                                else -> DoseStatus.PENDING
                            }

                            slotMap[slot]?.add(
                                DoseUiState(
                                    medicine = med,
                                    slot = slot,
                                    scheduledTime = time,
                                    isTaken = isTaken,
                                    isPendingWrite = isPending,
                                    doseRecord = record,
                                    status = status
                                )
                            )
                        }
                    }
                }

                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        userName = name,
                        userProfile = profile,
                        medicines = medicines,
                        todayRecords = todayRecords,
                        slotMedicines = slotMap,
                        totalDosesToday = totalDoses,
                        takenDosesToday = takenDoses,
                        errorMessage = null
                    )
                }
            }.collect()
        }
    }

    /**
     * Marks a dose as taken with immediate optimistic UI update and background Firestore sync.
     * Rolls back safely on failure and emits a one-time snackbar event.
     */
    fun markAsTaken(item: DoseUiState) {
        val uid = currentUserId
        if (uid.isEmpty()) return

        val doseId = item.id

        // Prevent duplicate records: if already taken or a write is currently pending, ignore tap
        if (item.isTaken || _uiState.value.pendingDoseIds.contains(doseId)) {
            return
        }

        // 1. Save previous state of that dose only
        val previousDose = item
        val nowTime = historyRepository.getCurrentTimeString()
        val optimisticStatus = historyRepository.evaluateDoseStatus(item.scheduledTime, nowTime)
        val optimisticRecord = DoseRecord(
            id = "temp_$doseId",
            userId = uid,
            medicineId = item.medicine.id,
            medicineName = item.medicine.name,
            dosage = item.medicine.dosage,
            slot = item.slot.name,
            scheduledTime = item.scheduledTime,
            takenTime = nowTime,
            date = historyRepository.getTodayDateString(),
            status = optimisticStatus.name,
            timestamp = System.currentTimeMillis()
        )

        // 2. Update UI state immediately (optimistic update)
        _uiState.update { current ->
            val updatedSlotMap = current.slotMedicines.toMutableMap().mapValues { (_, list) ->
                list.map { slotMed ->
                    if (slotMed.id == doseId) {
                        slotMed.copy(
                            isTaken = true,
                            isPendingWrite = true,
                            status = optimisticStatus,
                            doseRecord = optimisticRecord
                        )
                    } else {
                        slotMed
                    }
                }
            }
            val newTakenDoses = updatedSlotMap.values.flatten().count { it.isTaken }
            val updatedRecords = listOf(optimisticRecord) + current.todayRecords.filter { it.id != optimisticRecord.id }

            current.copy(
                slotMedicines = updatedSlotMap,
                todayRecords = updatedRecords,
                takenDosesToday = newTakenDoses,
                pendingDoseIds = current.pendingDoseIds + doseId,
                lastRecordedStatus = optimisticStatus,
                showFeedbackDialog = true
            )
        }

        // 3. Write to Firestore in the background
        viewModelScope.launch {
            val result = historyRepository.recordDoseTaken(
                userId = uid,
                medicine = item.medicine,
                slot = item.slot,
                scheduledTime = item.scheduledTime
            )

            result.fold(
                onSuccess = { record ->
                    // On success: keep the UI state; confirmed by Firestore snapshot listener
                    _uiState.update { current ->
                        val updatedSlotMap = current.slotMedicines.toMutableMap().mapValues { (_, list) ->
                            list.map { slotMed ->
                                if (slotMed.id == doseId) {
                                    slotMed.copy(
                                        isTaken = true,
                                        isPendingWrite = false,
                                        doseRecord = record,
                                        status = DoseStatus.fromString(record.status)
                                    )
                                } else {
                                    slotMed
                                }
                            }
                        }
                        val updatedRecords = listOf(record) + current.todayRecords.filter { it.id != optimisticRecord.id && it.id != record.id }

                        current.copy(
                            slotMedicines = updatedSlotMap,
                            todayRecords = updatedRecords,
                            pendingDoseIds = current.pendingDoseIds - doseId
                        )
                    }
                },
                onFailure = { error ->
                    // On failure: restore the previous state for that dose only and emit one-time Snackbar event
                    _uiState.update { current ->
                        val revertedSlotMap = current.slotMedicines.toMutableMap().mapValues { (_, list) ->
                            list.map { slotMed ->
                                if (slotMed.id == doseId) {
                                    previousDose.copy(isPendingWrite = false)
                                } else {
                                    slotMed
                                }
                            }
                        }
                        val revertedTakenDoses = revertedSlotMap.values.flatten().count { it.isTaken }
                        val revertedRecords = current.todayRecords.filter { it.id != optimisticRecord.id }

                        current.copy(
                            slotMedicines = revertedSlotMap,
                            todayRecords = revertedRecords,
                            takenDosesToday = revertedTakenDoses,
                            pendingDoseIds = current.pendingDoseIds - doseId,
                            showFeedbackDialog = false,
                            lastRecordedStatus = null
                        )
                    }
                    val snackbarMsg = if (error.message?.contains("Too many actions", ignoreCase = true) == true ||
                        error.message?.contains("permission", ignoreCase = true) == true ||
                        error.message?.contains("rate", ignoreCase = true) == true
                    ) {
                        "Too many actions today. Please try again later."
                    } else {
                        "Could not save, please try again"
                    }
                    _eventFlow.emit(HomeUiEvent.ShowSnackbar(snackbarMsg))
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
