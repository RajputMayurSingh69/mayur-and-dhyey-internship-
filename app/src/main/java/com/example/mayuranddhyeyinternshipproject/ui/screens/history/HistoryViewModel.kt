package com.example.mayuranddhyeyinternshipproject.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mayuranddhyeyinternshipproject.data.model.DoseRecord
import com.example.mayuranddhyeyinternshipproject.data.repository.AuthRepository
import com.example.mayuranddhyeyinternshipproject.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class HistoryDateFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days")
}

data class HistoryUiState(
    val isLoading: Boolean = true,
    val allRecords: List<DoseRecord> = emptyList(),
    val filteredRecords: List<DoseRecord> = emptyList(),
    val availableMedicines: List<String> = emptyList(),
    val selectedMedicineFilter: String = "All",
    val selectedDateFilter: HistoryDateFilter = HistoryDateFilter.ALL
)

class HistoryViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val historyRepository: HistoryRepository = HistoryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        val uid = authRepository.getCurrentUserId()
        if (uid.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            historyRepository.getHistoryStream(uid).collect { records ->
                val medNames = listOf("All") + records.map { it.medicineName }.distinct().sorted()
                _uiState.update { state ->
                    val filtered = applyFilters(records, state.selectedMedicineFilter, state.selectedDateFilter)
                    state.copy(
                        isLoading = false,
                        allRecords = records,
                        filteredRecords = filtered,
                        availableMedicines = medNames
                    )
                }
            }
        }
    }

    fun setMedicineFilter(medicine: String) {
        _uiState.update { state ->
            val filtered = applyFilters(state.allRecords, medicine, state.selectedDateFilter)
            state.copy(
                selectedMedicineFilter = medicine,
                filteredRecords = filtered
            )
        }
    }

    fun setDateFilter(filter: HistoryDateFilter) {
        _uiState.update { state ->
            val filtered = applyFilters(state.allRecords, state.selectedMedicineFilter, filter)
            state.copy(
                selectedDateFilter = filter,
                filteredRecords = filtered
            )
        }
    }

    private fun applyFilters(
        records: List<DoseRecord>,
        medicineFilter: String,
        dateFilter: HistoryDateFilter
    ): List<DoseRecord> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val cal = Calendar.getInstance()

        return records.filter { record ->
            val matchesMed = medicineFilter == "All" || record.medicineName.equals(medicineFilter, ignoreCase = true)
            val matchesDate = when (dateFilter) {
                HistoryDateFilter.ALL -> true
                HistoryDateFilter.TODAY -> record.date == todayStr
                HistoryDateFilter.LAST_7_DAYS -> {
                    val daysAgo7 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                    record.timestamp >= daysAgo7.timeInMillis
                }
                HistoryDateFilter.LAST_30_DAYS -> {
                    val daysAgo30 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
                    record.timestamp >= daysAgo30.timeInMillis
                }
            }
            matchesMed && matchesDate
        }
    }
}
