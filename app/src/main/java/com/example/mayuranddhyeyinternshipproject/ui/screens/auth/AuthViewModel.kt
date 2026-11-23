package com.example.mayuranddhyeyinternshipproject.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mayuranddhyeyinternshipproject.data.model.UserProfile
import com.example.mayuranddhyeyinternshipproject.data.repository.AuthRepository
import com.example.mayuranddhyeyinternshipproject.data.repository.SecurityPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val userProfile: UserProfile? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val securityPreferences: SecurityPreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    fun getCurrentUserId(): String? = authRepository.getCurrentUserId()

    fun signIn(email: String, pass: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signIn(email, pass)
            result.fold(
                onSuccess = { profile ->
                    securityPreferences?.cachedUserId = profile.uid
                    securityPreferences?.cachedUserName = profile.fullName
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            userProfile = profile,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Sign in failed"
                        )
                    }
                }
            )
        }
    }

    fun signUp(
        fullName: String,
        dob: String,
        phone: String,
        email: String,
        pass: String
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signUp(
                fullName = fullName,
                dob = dob,
                phone = phone,
                email = email,
                password = pass
            )
            result.fold(
                onSuccess = { profile ->
                    securityPreferences?.cachedUserId = profile.uid
                    securityPreferences?.cachedUserName = profile.fullName
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            userProfile = profile,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Sign up failed"
                        )
                    }
                }
            )
        }
    }

    fun signInWithGoogle(credential: com.google.firebase.auth.AuthCredential) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signInWithGoogleCredential(credential)
            result.fold(
                onSuccess = { profile ->
                    securityPreferences?.cachedUserId = profile.uid
                    securityPreferences?.cachedUserName = profile.fullName
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            userProfile = profile,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Google sign in failed. Please try again."
                        )
                    }
                }
            )
        }
    }

    fun setErrorMessage(message: String) {
        _uiState.update { it.copy(errorMessage = message, isLoading = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }
}
