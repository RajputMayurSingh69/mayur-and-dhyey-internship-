package com.example.mayuranddhyeyinternshipproject.ui.screens.profile

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mayuranddhyeyinternshipproject.data.model.UserProfile
import com.example.mayuranddhyeyinternshipproject.data.repository.AuthRepository
import com.example.mayuranddhyeyinternshipproject.data.repository.UserRepository
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isPhoneOtpPending: Boolean = false,
    val phoneOtpVerificationId: String? = null,
    val pendingNewPhone: String? = null,
    val pendingProfileUpdate: UserProfile? = null
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

    /**
     * Updates user profile.
     * If phone has changed, requires OTP verification to the new phone number first (Phase 2A).
     */
    fun updateProfile(
        activity: Activity?,
        fullName: String,
        dob: String,
        phone: String,
        defaultTimes: Map<String, String>,
        pharmacyName: String = _uiState.value.profile?.pharmacyName ?: "",
        pharmacyContact: String = _uiState.value.profile?.pharmacyContact ?: "",
        doctorName: String = _uiState.value.profile?.doctorName ?: "",
        doctorContact: String = _uiState.value.profile?.doctorContact ?: ""
    ) {
        val current = _uiState.value.profile ?: return
        if (fullName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Full name cannot be empty.") }
            return
        }

        val cleanPhone = phone.filter { it.isDigit() }
        val currentCleanPhone = (current.fullPhone.ifBlank { current.phone }).filter { it.isDigit() }

        val updated = current.copy(
            fullName = fullName.trim(),
            dob = dob.trim(),
            phone = if (cleanPhone == currentCleanPhone) current.phone else "******" + cleanPhone.takeLast(4),
            fullPhone = cleanPhone,
            defaultTimes = defaultTimes,
            pharmacyName = pharmacyName.trim(),
            pharmacyContact = pharmacyContact.trim(),
            doctorName = doctorName.trim(),
            doctorContact = doctorContact.trim()
        )

        // Check if phone has changed and needs OTP verification
        if (cleanPhone.length == 10 && cleanPhone != currentCleanPhone && activity != null) {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            authRepository.sendPhoneOtp(
                activity = activity,
                phone = cleanPhone,
                onCodeSent = { vId, _ ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isPhoneOtpPending = true,
                            phoneOtpVerificationId = vId,
                            pendingNewPhone = cleanPhone,
                            pendingProfileUpdate = updated
                        )
                    }
                },
                onVerificationCompleted = { credential ->
                    verifyPhoneChangeWithCredential(credential, cleanPhone, updated)
                },
                onVerificationFailed = { ex ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = ex.localizedMessage ?: "Phone OTP verification failed."
                        )
                    }
                }
            )
            return
        }

        // Phone unchanged: update directly
        saveProfileDirectly(updated)
    }

    fun submitPhoneChangeOtp(code: String) {
        val vId = _uiState.value.phoneOtpVerificationId ?: return
        val newPhone = _uiState.value.pendingNewPhone ?: return
        val updated = _uiState.value.pendingProfileUpdate ?: return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        val credential = PhoneAuthProvider.getCredential(vId, code)
        verifyPhoneChangeWithCredential(credential, newPhone, updated)
    }

    private fun verifyPhoneChangeWithCredential(
        credential: com.google.firebase.auth.PhoneAuthCredential,
        newPhone: String,
        updated: UserProfile
    ) {
        viewModelScope.launch {
            try {
                authRepository.currentUser?.updatePhoneNumber(credential)?.await()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Phone verification failed: ${e.localizedMessage}"
                    )
                }
                return@launch
            }

            // Call updatePhone Cloud Function to update phone_index and profile
            val phoneUpdateResult = authRepository.updatePhone(newPhone)
            phoneUpdateResult.fold(
                onSuccess = {
                    saveProfileDirectly(updated)
                    _uiState.update {
                        it.copy(
                            isPhoneOtpPending = false,
                            phoneOtpVerificationId = null,
                            pendingNewPhone = null,
                            pendingProfileUpdate = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.localizedMessage ?: "Failed to update phone index."
                        )
                    }
                }
            )
        }
    }

    fun cancelPhoneOtp() {
        _uiState.update {
            it.copy(
                isPhoneOtpPending = false,
                phoneOtpVerificationId = null,
                pendingNewPhone = null,
                pendingProfileUpdate = null
            )
        }
    }

    private fun saveProfileDirectly(profile: UserProfile) {
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val result = userRepository.updateProfile(profile)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true, profile = profile) }
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
