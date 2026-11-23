package com.example.mayuranddhyeyinternshipproject.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mayuranddhyeyinternshipproject.data.model.TimeSlot
import com.example.mayuranddhyeyinternshipproject.data.model.UserProfile
import com.example.mayuranddhyeyinternshipproject.data.repository.AuthRepository
import com.example.mayuranddhyeyinternshipproject.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val currentUserId: String get() = authRepository.getCurrentUserId() ?: ""

    init {
        loadProfile()
    }

    fun loadProfile() {
        val uid = currentUserId
        if (uid.isEmpty()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            userRepository.getUserProfileStream(uid).collect { prof ->
                _uiState.update { it.copy(isLoading = false, profile = prof) }
            }
        }
    }

    fun updateProfile(
        fullName: String,
        dob: String,
        phone: String,
        defaultTimes: Map<String, String>,
        pharmacyName: String,
        pharmacyContact: String,
        doctorName: String,
        doctorContact: String
    ) {
        val current = _uiState.value.profile ?: return
        if (fullName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Full name cannot be empty.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val updated = current.copy(
                fullName = fullName.trim(),
                dob = dob.trim(),
                phone = phone.trim(),
                defaultTimes = defaultTimes,
                pharmacyName = pharmacyName.trim(),
                pharmacyContact = pharmacyContact.trim(),
                doctorName = doctorName.trim(),
                doctorContact = doctorContact.trim()
            )

            val result = userRepository.updateProfile(updated)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true, profile = updated) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = error.localizedMessage ?: "Failed to update profile.") }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
