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
    val infoMessage: String? = null,
    val isSuccess: Boolean = false,
    val userProfile: UserProfile? = null,
    val switchToTab: AuthTab? = null,
    val prefillEmail: String? = null,
    val prefillName: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val securityPreferences: SecurityPreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    fun getCurrentUserId(): String? = authRepository.getCurrentUserId()

    /**
     * Signs in with email and password.
     * If user is not found, switches to Create Account tab and pre-fills email.
     * If profile in Firestore is missing, guides user to complete registration.
     */
    fun signIn(email: String, pass: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
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
                            errorMessage = null,
                            infoMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    val msg = error.localizedMessage ?: "Sign in failed"
                    when {
                        msg.startsWith("USER_NOT_FOUND:") -> {
                            val notFoundEmail = msg.removePrefix("USER_NOT_FOUND:")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "No account found with this email. Please fill in the details below to create an account.",
                                    switchToTab = AuthTab.CREATE_ACCOUNT,
                                    prefillEmail = notFoundEmail
                                )
                            }
                        }
                        msg.startsWith("USER_PROFILE_MISSING:") -> {
                            val missingEmail = msg.removePrefix("USER_PROFILE_MISSING:")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "No profile found for this account. Please complete registration below.",
                                    switchToTab = AuthTab.CREATE_ACCOUNT,
                                    prefillEmail = missingEmail
                                )
                            }
                        }
                        msg.startsWith("GOOGLE_ACCOUNT_ONLY:") -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "This email is registered with Google. Please tap 'Continue with Google' to sign in."
                                )
                            }
                        }
                        else -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = msg
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    /**
     * Creates a new user with full profile (Name, DOB, Phone, Email, Password).
     * If email already exists, switches to Sign In tab and pre-fills email.
     */
    fun signUp(
        fullName: String,
        dob: String,
        phone: String,
        email: String,
        pass: String
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
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
                            errorMessage = null,
                            infoMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    val msg = error.localizedMessage ?: "Sign up failed"
                    if (msg.contains("already exists", ignoreCase = true)) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "An account already exists with this email. Please enter your password to sign in.",
                                switchToTab = AuthTab.SIGN_IN,
                                prefillEmail = email
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = msg
                            )
                        }
                    }
                }
            )
        }
    }

    /**
     * Signs in with Google credential on the Sign In tab.
     * Verifies that the user already has a complete profile in Firestore.
     * If user does NOT have a profile, switches to Create Account tab and pre-fills Name and Email.
     */
    fun signInWithGoogle(
        credential: com.google.firebase.auth.AuthCredential,
        googleEmail: String = "",
        googleName: String = ""
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signInWithGoogleCredential(credential, googleEmail, googleName)
            result.fold(
                onSuccess = { profile ->
                    securityPreferences?.cachedUserId = profile.uid
                    securityPreferences?.cachedUserName = profile.fullName
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            userProfile = profile,
                            errorMessage = null,
                            infoMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    val msg = error.localizedMessage ?: "Google sign in failed"
                    if (msg.startsWith("GOOGLE_USER_NOT_REGISTERED:")) {
                        val parts = msg.removePrefix("GOOGLE_USER_NOT_REGISTERED:").split(":")
                        val targetEmail = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: googleEmail
                        val targetName = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: googleName
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                infoMessage = "No account found with $targetEmail. Please enter your Date of Birth, Phone, and Password to complete registration.",
                                switchToTab = AuthTab.CREATE_ACCOUNT,
                                prefillEmail = targetEmail,
                                prefillName = targetName
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = msg
                            )
                        }
                    }
                }
            )
        }
    }

    /**
     * Called when user taps "Continue with Google" while on the Create Account tab.
     * Checks if the Google email is already registered in Firebase.
     * If already registered: prompts user and switches to Sign In tab.
     * If NOT registered: pre-fills Name and Email into Create Account form, and prompts for DOB, Phone, Password.
     */
    fun handleGoogleInCreateAccount(email: String, name: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            val alreadyRegistered = authRepository.checkEmailRegistered(email)
            if (alreadyRegistered) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "An account already exists with $email. Please enter your password to sign in.",
                        switchToTab = AuthTab.SIGN_IN,
                        prefillEmail = email
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        infoMessage = "Google account connected ($email)! Please enter your Date of Birth, 10-digit Phone, and set an 8-character Password to complete registration.",
                        prefillName = name,
                        prefillEmail = email
                    )
                }
            }
        }
    }

    fun clearTabSwitch() {
        _uiState.update { it.copy(switchToTab = null, prefillEmail = null, prefillName = null) }
    }

    fun setErrorMessage(message: String) {
        _uiState.update { it.copy(errorMessage = message, infoMessage = null, isLoading = false) }
    }

    fun setInfoMessage(message: String) {
        _uiState.update { it.copy(infoMessage = message, errorMessage = null, isLoading = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearInfo() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }
}
