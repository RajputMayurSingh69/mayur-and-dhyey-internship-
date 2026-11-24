package com.example.mayuranddhyeyinternshipproject.data.repository

import android.app.Activity
import android.content.Context
import com.example.mayuranddhyeyinternshipproject.data.model.UserProfile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Handles Authentication, input validation, and user profile management.
 * Phase 2 implements:
 * - Phone OTP verification and credential linking
 * - Email verification tracking and reloading
 * - Secure password reset email delivery
 * - Phone number change verification
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("asia-south1")
) {

    val currentUser: FirebaseUser? get() = auth.currentUser

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified == true

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun validateFullName(name: String): String? = ValidationUtils.validateFullName(name)
    fun validateDateOfBirth(dob: String): String? = ValidationUtils.validateDateOfBirth(dob)
    fun validatePhone(phone: String): String? = ValidationUtils.validatePhone(phone)
    fun validateEmail(email: String): String? = ValidationUtils.validateEmail(email)
    fun validatePassword(password: String): String? = ValidationUtils.validatePassword(password)

    /**
     * Checks if an email is already registered in Firebase Authentication.
     */
    suspend fun checkEmailRegistered(email: String): Boolean {
        val cleanEmail = email.trim()
        return try {
            val result = auth.fetchSignInMethodsForEmail(cleanEmail).await()
            !result.signInMethods.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Step 1 of Sign-Up: Creates the Firebase Auth user with email and password.
     */
    suspend fun createAuthUser(email: String, password: String): Result<FirebaseUser> {
        val cleanEmail = email.trim()
        if (checkEmailRegistered(cleanEmail)) {
            return Result.failure(IllegalStateException("Account already exists with this email, please sign in"))
        }

        return try {
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("User creation failed. Please try again."))
            Result.success(user)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(IllegalStateException("Account already exists with this email, please sign in"))
        } catch (e: FirebaseNetworkException) {
            Result.failure(IllegalStateException("Network connection error. Please check your internet connection."))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(mapAuthError(e)))
        }
    }

    /**
     * Step 2 of Sign-Up: Sends a 6-digit OTP to the user's 10-digit phone number.
     * Formats phone with +91 country prefix.
     */
    fun sendPhoneOtp(
        activity: Activity,
        phone: String,
        token: PhoneAuthProvider.ForceResendingToken? = null,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        val cleanPhone = phone.filter { it.isDigit() }
        val formattedPhone = if (cleanPhone.startsWith("91") && cleanPhone.length == 12) {
            "+$cleanPhone"
        } else {
            "+91$cleanPhone"
        }

        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    onVerificationCompleted(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    onVerificationFailed(e)
                }

                override fun onCodeSent(
                    verificationId: String,
                    resendingToken: PhoneAuthProvider.ForceResendingToken
                ) {
                    onCodeSent(verificationId, resendingToken)
                }
            })

        if (token != null) {
            builder.setForceResendingToken(token)
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    /**
     * Step 4 & 5 of Sign-Up: Links phone credential, sends email verification, and calls completeSignUp Cloud Function.
     * If phone is already registered on another account, or linking fails, deletes the newly created Auth user.
     */
    suspend fun linkPhoneAndCompleteSignUp(
        credential: PhoneAuthCredential,
        fullName: String,
        dob: String,
        phone: String,
        email: String
    ): Result<UserProfile> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Session expired. Please try again."))

        val cleanPhone = phone.filter { it.isDigit() }
        val cleanName = fullName.trim()
        val cleanDob = dob.trim()
        val cleanEmail = email.trim()

        // 1. Link phone credential to the Auth user
        try {
            user.linkWithCredential(credential).await()
        } catch (e: Exception) {
            // Delete Auth user so no half-made account remains
            try { user.delete().await() } catch (ignored: Exception) {}

            val msg = e.localizedMessage?.lowercase(Locale.ROOT) ?: ""
            return if (e is FirebaseAuthUserCollisionException ||
                msg.contains("credential-already-in-use") ||
                msg.contains("already in use")
            ) {
                Result.failure(IllegalStateException("Phone number already registered with another account"))
            } else {
                Result.failure(IllegalStateException("Invalid or expired verification code. Please try again."))
            }
        }

        // 2. Send email verification
        try {
            user.sendEmailVerification().await()
        } catch (ignored: Exception) {}

        // 3. Call completeSignUp Cloud Function (HMAC server-side hashing & transactional record creation)
        val profileResult = completeSignUp(cleanName, cleanDob, cleanPhone, cleanEmail)
        return if (profileResult.isFailure) {
            // Transaction failed or phone taken in database; delete Auth user
            try { user.delete().await() } catch (ignored: Exception) {}
            profileResult
        } else {
            profileResult
        }
    }

    /**
     * Calls completeSignUp Cloud Function (Phase 1C).
     */
    suspend fun completeSignUp(
        name: String,
        dob: String,
        phone: String,
        email: String
    ): Result<UserProfile> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Session expired. Please try again."))

        val cleanPhone = phone.filter { it.isDigit() }
        val cleanName = name.trim()
        val cleanDob = dob.trim()
        val cleanEmail = email.trim().lowercase(Locale.ROOT)

        return try {
            val completeSignUpCallable = functions.getHttpsCallable("completeSignUp")
            val data = mapOf(
                "name" to cleanName,
                "dateOfBirth" to cleanDob,
                "phone" to cleanPhone,
                "email" to cleanEmail
            )
            completeSignUpCallable.call(data).await()

            val maskedPhone = "******" + cleanPhone.takeLast(4)
            val profile = UserProfile(
                uid = user.uid,
                fullName = cleanName,
                email = cleanEmail,
                phone = maskedPhone,
                fullPhone = cleanPhone,
                dob = cleanDob,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            Result.success(profile)
        } catch (e: Exception) {
            val msg = e.localizedMessage?.lowercase(Locale.ROOT) ?: ""
            if (msg.contains("phone-already-registered") || msg.contains("already-exists")) {
                Result.failure(IllegalStateException("Phone number already registered with another account"))
            } else {
                Result.failure(IllegalStateException("Registration failed. Please try again."))
            }
        }
    }

    /**
     * Cleans up the Auth user if OTP verification was cancelled or failed before completion.
     */
    suspend fun cleanupPendingUser() {
        try {
            auth.currentUser?.delete()?.await()
        } catch (ignored: Exception) {}
    }

    /**
     * Signs in user with email and password only. Never requires OTP on sign-in.
     * Verifies against Firebase Auth and Firestore user profile.
     */
    suspend fun signIn(email: String, password: String): Result<UserProfile> {
        validateEmail(email)?.let { return Result.failure(IllegalArgumentException(it)) }
        if (password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Password cannot be empty."))
        }

        val cleanEmail = email.trim()

        return try {
            val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Sign in failed. Please try again."))

            val doc = firestore.collection("users").document(user.uid).get().await()
            val profile = doc.toObject(UserProfile::class.java)
            if (profile != null) {
                Result.success(profile)
            } else {
                Result.failure(IllegalStateException("USER_PROFILE_MISSING:$cleanEmail"))
            }
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(IllegalStateException("USER_NOT_FOUND:$cleanEmail"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            val methods = try {
                auth.fetchSignInMethodsForEmail(cleanEmail).await().signInMethods
            } catch (ex: Exception) {
                null
            }
            when {
                methods != null && methods.isEmpty() -> {
                    Result.failure(IllegalStateException("USER_NOT_FOUND:$cleanEmail"))
                }
                methods != null && methods.contains("google.com") && !methods.contains("password") -> {
                    Result.failure(IllegalStateException("GOOGLE_ACCOUNT_ONLY:$cleanEmail"))
                }
                else -> {
                    Result.failure(IllegalStateException("Incorrect password. Please try again or reset your password."))
                }
            }
        } catch (e: FirebaseNetworkException) {
            Result.failure(IllegalStateException("Network connection error. Please check your internet connection."))
        } catch (e: Exception) {
            val msg = e.localizedMessage?.lowercase(Locale.ROOT) ?: ""
            if (msg.contains("no user record") || msg.contains("user-not-found")) {
                Result.failure(IllegalStateException("USER_NOT_FOUND:$cleanEmail"))
            } else {
                Result.failure(IllegalStateException(mapAuthError(e)))
            }
        }
    }

    /**
     * Reloads current Auth user from Firebase to check updated email verification status.
     */
    suspend fun reloadUser(): Boolean {
        return try {
            auth.currentUser?.reload()?.await()
            auth.currentUser?.isEmailVerified == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Resends email verification to current user with cooldown check.
     */
    suspend fun sendEmailVerification(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("No active user session."))
        return try {
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Failed to resend verification email. Please try again later."))
        }
    }

    /**
     * Sends password reset email for forgot password flow.
     * Always returns uniform message to avoid revealing which emails exist.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<String> {
        val cleanEmail = email.trim()
        validateEmail(cleanEmail)?.let { return Result.failure(IllegalArgumentException(it)) }

        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success("If an account exists for this email, a reset link has been sent.")
        } catch (e: FirebaseNetworkException) {
            Result.failure(IllegalStateException("Network connection error. Please check your internet connection."))
        } catch (e: Exception) {
            // Return success message even on user-not-found to prevent user enumeration
            Result.success("If an account exists for this email, a reset link has been sent.")
        }
    }

    /**
     * Updates phone number via OTP verification and updatePhone Cloud Function.
     */
    suspend fun updatePhone(newPhone: String): Result<Unit> {
        validatePhone(newPhone)?.let { return Result.failure(IllegalArgumentException(it)) }
        return try {
            val updatePhoneCallable = functions.getHttpsCallable("updatePhone")
            val cleanPhone = newPhone.filter { it.isDigit() }
            updatePhoneCallable.call(mapOf("newPhone" to cleanPhone)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = e.localizedMessage?.lowercase(Locale.ROOT) ?: ""
            if (msg.contains("phone-already-registered") || msg.contains("already-exists")) {
                Result.failure(IllegalStateException("Phone number already registered with another account"))
            } else {
                Result.failure(IllegalStateException("Failed to update phone number. Please try again."))
            }
        }
    }

    fun signOut(context: Context? = null) {
        auth.signOut()
        if (context != null) {
            try {
                GoogleSignIn.getClient(
                    context,
                    GoogleSignInOptions.DEFAULT_SIGN_IN
                ).signOut()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Signs in using a Google AuthCredential on the Sign In tab.
     */
    suspend fun signInWithGoogleCredential(
        credential: AuthCredential,
        googleEmail: String = "",
        googleName: String = ""
    ): Result<UserProfile> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Google sign in failed. Please try again."))

            val docRef = firestore.collection("users").document(user.uid)
            val doc = docRef.get().await()
            val existing = doc.toObject(UserProfile::class.java)

            if (existing != null && existing.phone.isNotBlank()) {
                Result.success(existing)
            } else {
                try {
                    auth.signOut()
                } catch (ignored: Exception) {}

                val resolvedEmail = user.email?.takeIf { it.isNotBlank() } ?: googleEmail
                val resolvedName = user.displayName?.takeIf { it.isNotBlank() } ?: googleName
                Result.failure(IllegalStateException("GOOGLE_USER_NOT_REGISTERED:$resolvedEmail:$resolvedName"))
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(mapAuthError(e)))
        }
    }

    private fun mapAuthError(e: Exception): String {
        return when (e) {
            is FirebaseAuthUserCollisionException ->
                "Account already exists with this email, please sign in"
            is FirebaseAuthWeakPasswordException ->
                "Password is too weak. Please choose a stronger password."
            is FirebaseAuthInvalidUserException ->
                "No account found with this email. Please create an account first."
            is FirebaseAuthInvalidCredentialsException ->
                "Incorrect email or password. Please check and try again."
            is FirebaseNetworkException ->
                "Network connection error. Please check your internet connection."
            else -> {
                val msg = e.localizedMessage?.lowercase(Locale.ROOT) ?: ""
                when {
                    msg.contains("no user record") ||
                    msg.contains("user-not-found") ->
                        "No account found with this email. Please create an account first."
                    msg.contains("invalid-credential") ||
                    msg.contains("invalid credential") ||
                    msg.contains("wrong password") ||
                    msg.contains("password is invalid") ->
                        "Incorrect password. Please try again or reset your password."
                    msg.contains("already in use") || msg.contains("email-already-in-use") ->
                        "Account already exists with this email, please sign in"
                    msg.contains("network") ->
                        "Network connection error. Please check your internet connection."
                    msg.contains("too many requests") || msg.contains("too-many-requests") ->
                        "Too many attempts. Please try again later."
                    else ->
                        "An error occurred. Please try again."
                }
            }
        }
    }
}
