package com.example.mayuranddhyeyinternshipproject.ui.screens.auth

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mayuranddhyeyinternshipproject.data.model.UserProfile
import com.example.mayuranddhyeyinternshipproject.data.repository.AuthRepository
import com.example.mayuranddhyeyinternshipproject.data.repository.SecurityPreferences
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PendingSignUpData(
    val fullName: String,
    val dob: String,
    val phone: String,
    val email: String
)

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isSuccess: Boolean = false,
    val isEmailVerificationPending: Boolean = false,
    val isOtpPending: Boolean = false,
    val verificationId: String? = null,
    val resendToken: PhoneAuthProvider.ForceResendingToken? = null,
    val pendingSignUpData: PendingSignUpData? = null,
    val forgotPasswordMessage: String? = null,
    val isForgotPasswordLoading: Boolean = false,
    val userProfile: UserProfile? = null,
    val switchToTab: AuthTab? = null,
    val prefillEmail: String? = null,
    val prefillName: String? = null,
    val pendingGoogleCredential: com.google.firebase.auth.AuthCredential? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val securityPreferences: SecurityPreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    fun isEmailVerified(): Boolean = authRepository.isEmailVerified()

    fun getCurrentUserId(): String? = authRepository.getCurrentUserId()

    /**
     * Signs in with email and password.
     * If user is unverified, marks isEmailVerificationPending = true.
     */
    fun signIn(email: String, pass: String) {
        val pendingCred = _uiState.value.pendingGoogleCredential
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signIn(email, pass, pendingCred)
            result.fold(
                onSuccess = { profile ->
                    securityPreferences?.cachedUserId = profile.uid
                    securityPreferences?.cachedUserName = profile.fullName

                    if (authRepository.isEmailVerified()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = true,
                                userProfile = profile,
                                pendingGoogleCredential = null,
                                errorMessage = null,
                                infoMessage = null
                            )
                        }
                    } else {
                        // User signed in but email is not verified yet
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isEmailVerificationPending = true,
                                userProfile = profile,
                                pendingGoogleCredential = null,
                                errorMessage = null,
                                infoMessage = null
                            )
                        }
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
     * Backward-compatible sign-up method for legacy callers.
     */
    fun signUp(
        fullName: String,
        dob: String,
        phone: String,
        email: String,
        pass: String,
        activity: Activity? = null
    ) {
        if (activity != null) {
            startSignUpWithPhoneOtp(activity, fullName, dob, phone, email, pass)
        } else {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            viewModelScope.launch {
                val createResult = authRepository.createAuthUser(email, pass)
                createResult.fold(
                    onSuccess = {
                        val profileResult = authRepository.completeSignUp(fullName, dob, phone, email)
                        profileResult.fold(
                            onSuccess = {
                                authRepository.sendEmailVerification()
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isSuccess = true,
                                        isEmailVerificationPending = true
                                    )
                                }
                            },
                            onFailure = { err ->
                                authRepository.cleanupPendingUser()
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        errorMessage = err.localizedMessage ?: "Registration failed."
                                    )
                                }
                            }
                        )
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = err.localizedMessage ?: "Failed to create account."
                            )
                        }
                    }
                )
            }
        }
    }

    /**
     * Phase 2A Sign-Up with Phone OTP:
     * 1. Validates inputs and creates the Auth user with email/password.
     * 2. Sends 6-digit OTP to user's phone number via PhoneAuthProvider.
     * 3. Sets isOtpPending = true to show OTP dialog.
     */
    fun startSignUpWithPhoneOtp(
        activity: Activity,
        fullName: String,
        dob: String,
        phone: String,
        email: String,
        pass: String
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            val createResult = authRepository.createAuthUser(email, pass)
            createResult.fold(
                onSuccess = {
                    val pendingData = PendingSignUpData(
                        fullName = fullName,
                        dob = dob,
                        phone = phone,
                        email = email
                    )
                    authRepository.sendPhoneOtp(
                        activity = activity,
                        phone = phone,
                        onCodeSent = { verificationId, token ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isOtpPending = true,
                                    verificationId = verificationId,
                                    resendToken = token,
                                    pendingSignUpData = pendingData
                                )
                            }
                        },
                        onVerificationCompleted = { credential ->
                            // Auto-verification on supported devices
                            completeRegistrationWithCredential(credential, pendingData)
                        },
                        onVerificationFailed = { ex ->
                            viewModelScope.launch {
                                // Fallback to completing signup directly with email and password so user is not blocked if SMS/phone auth is not configured
                                val profileResult = authRepository.completeSignUp(
                                    name = pendingData.fullName,
                                    dob = pendingData.dob,
                                    phone = pendingData.phone,
                                    email = pendingData.email
                                )
                                profileResult.fold(
                                    onSuccess = { profile ->
                                        authRepository.sendEmailVerification()
                                        securityPreferences?.cachedUserId = profile.uid
                                        securityPreferences?.cachedUserName = profile.fullName
                                        _uiState.update {
                                            it.copy(
                                                isLoading = false,
                                                isOtpPending = false,
                                                isEmailVerificationPending = true,
                                                userProfile = profile,
                                                errorMessage = null,
                                                infoMessage = "Account created! A verification link has been sent to your email."
                                            )
                                        }
                                    },
                                    onFailure = { err ->
                                        authRepository.cleanupPendingUser()
                                        _uiState.update {
                                            it.copy(
                                                isLoading = false,
                                                errorMessage = err.localizedMessage ?: ex.localizedMessage ?: "Phone verification failed. Please try again."
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    )
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
     * Submits the 6-digit OTP code entered by the user.
     */
    fun submitPhoneOtp(code: String) {
        val verificationId = _uiState.value.verificationId ?: return
        val pendingData = _uiState.value.pendingSignUpData ?: return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        completeRegistrationWithCredential(credential, pendingData)
    }

    private fun completeRegistrationWithCredential(
        credential: PhoneAuthCredential,
        pendingData: PendingSignUpData
    ) {
        viewModelScope.launch {
            val result = authRepository.linkPhoneAndCompleteSignUp(
                credential = credential,
                fullName = pendingData.fullName,
                dob = pendingData.dob,
                phone = pendingData.phone,
                email = pendingData.email
            )
            result.fold(
                onSuccess = { profile ->
                    securityPreferences?.cachedUserId = profile.uid
                    securityPreferences?.cachedUserName = profile.fullName
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOtpPending = false,
                            isEmailVerificationPending = true,
                            userProfile = profile,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOtpPending = false,
                            errorMessage = error.localizedMessage ?: "Registration failed. Please try again."
                        )
                    }
                }
            )
        }
    }

    /**
     * Resends the phone OTP using the force resending token.
     */
    fun resendPhoneOtp(activity: Activity) {
        val pendingData = _uiState.value.pendingSignUpData ?: return
        val token = _uiState.value.resendToken

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        authRepository.sendPhoneOtp(
            activity = activity,
            phone = pendingData.phone,
            token = token,
            onCodeSent = { verificationId, newToken ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        verificationId = verificationId,
                        resendToken = newToken,
                        infoMessage = "Verification code resent successfully."
                    )
                }
            },
            onVerificationCompleted = { credential ->
                completeRegistrationWithCredential(credential, pendingData)
            },
            onVerificationFailed = { ex ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ex.localizedMessage ?: "Failed to resend code."
                    )
                }
            }
        )
    }

    /**
     * Cancels pending phone OTP and deletes the half-made Auth user.
     */
    fun cancelPhoneOtp() {
        viewModelScope.launch {
            authRepository.cleanupPendingUser()
        }
        _uiState.update {
            it.copy(
                isOtpPending = false,
                verificationId = null,
                resendToken = null,
                pendingSignUpData = null,
                isLoading = false
            )
        }
    }

    /**
     * Checks if current user has verified their email via user.reload().
     */
    fun checkEmailVerification() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val isVerified = authRepository.reloadUser()
            if (isVerified) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEmailVerificationPending = false,
                        isSuccess = true,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Email not verified yet. Please click the link sent to your inbox, then tap 'I have verified'."
                    )
                }
            }
        }
    }

    /**
     * Resends email verification.
     */
    fun resendVerificationEmail() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.sendEmailVerification()
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Verification email sent. Please check your inbox and spam folder."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to send email."
                        )
                    }
                }
            )
        }
    }

    /**
     * Phase 2C: Sends password reset email.
     * Always shows uniform message to avoid revealing which emails exist.
     */
    fun sendPasswordResetEmail(email: String) {
        _uiState.update { it.copy(isForgotPasswordLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            result.fold(
                onSuccess = { message ->
                    _uiState.update {
                        it.copy(
                            isForgotPasswordLoading = false,
                            forgotPasswordMessage = message
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isForgotPasswordLoading = false,
                            errorMessage = error.localizedMessage
                        )
                    }
                }
            )
        }
    }

    fun clearForgotPasswordMessage() {
        _uiState.update { it.copy(forgotPasswordMessage = null) }
    }

    /**
     * Signs in with Google credential.
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
                    when {
                        msg.startsWith("GOOGLE_USER_NOT_REGISTERED:") -> {
                            val parts = msg.removePrefix("GOOGLE_USER_NOT_REGISTERED:").split(":")
                            val targetEmail = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: googleEmail
                            val targetName = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: googleName
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "No account found with this email. Please fill in all details below to create an account.",
                                    switchToTab = AuthTab.CREATE_ACCOUNT,
                                    prefillEmail = targetEmail,
                                    prefillName = targetName
                                )
                            }
                        }
                        msg.startsWith("ACCOUNT_EXISTS_WITH_PASSWORD:") -> {
                            val targetEmail = msg.removePrefix("ACCOUNT_EXISTS_WITH_PASSWORD:").takeIf { it.isNotBlank() } ?: googleEmail
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    pendingGoogleCredential = credential,
                                    errorMessage = "This account was registered with a password. Please enter your password to sign in and link Google.",
                                    switchToTab = AuthTab.SIGN_IN,
                                    prefillEmail = targetEmail
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

    fun signOut(context: Context? = null) {
        authRepository.signOut(context)
        _uiState.value = AuthUiState()
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
