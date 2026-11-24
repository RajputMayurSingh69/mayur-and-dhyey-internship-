package com.example.mayuranddhyeyinternshipproject.data.repository

import android.content.Context
import com.example.mayuranddhyeyinternshipproject.data.model.UserProfile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.util.Locale

/**
 * Handles Authentication, input validation, and user profile management.
 * In Phase 1: All client-side SHA-256 phone hashing and client writes to "phone_index" are removed.
 * Profile creation is delegated to the secure completeSignUp Cloud Function using HMAC-SHA256.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("asia-south1")
) {

    val currentUser get() = auth.currentUser

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

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
     * Registers a new user following strict security sequencing:
     * 1. Validates all inputs locally first.
     * 2. Checks if email is already registered in Firebase.
     * 3. Creates Firebase Auth user.
     * 4. Calls completeSignUp Cloud Function which verifies phone HMAC in phone_index
     *    and creates user profile in a single atomic transaction.
     * 5. If phone is already registered, Cloud Function automatically deletes the Auth user.
     */
    suspend fun signUp(
        fullName: String,
        dob: String,
        phone: String,
        email: String,
        password: String
    ): Result<UserProfile> {
        // 1. Validate all inputs locally first
        validateFullName(fullName)?.let { return Result.failure(IllegalArgumentException(it)) }
        validateDateOfBirth(dob)?.let { return Result.failure(IllegalArgumentException(it)) }
        validatePhone(phone)?.let { return Result.failure(IllegalArgumentException(it)) }
        validateEmail(email)?.let { return Result.failure(IllegalArgumentException(it)) }
        validatePassword(password)?.let { return Result.failure(IllegalArgumentException(it)) }

        val cleanEmail = email.trim()
        val cleanName = fullName.trim()
        val cleanDob = dob.trim()
        val cleanPhone = phone.filter { it.isDigit() }

        // 2. Check if email is already registered before attempting creation
        if (checkEmailRegistered(cleanEmail)) {
            return Result.failure(IllegalStateException("Account already exists with this email, please sign in"))
        }

        // 3. Call FirebaseAuth.createUserWithEmailAndPassword directly
        val authResult = try {
            auth.createUserWithEmailAndPassword(cleanEmail, password).await()
        } catch (e: FirebaseAuthUserCollisionException) {
            return Result.failure(IllegalStateException("Account already exists with this email, please sign in"))
        } catch (e: FirebaseNetworkException) {
            return Result.failure(IllegalStateException("Network connection error. Please check your internet connection."))
        } catch (e: Exception) {
            return Result.failure(IllegalStateException(mapAuthError(e)))
        }

        val user = authResult.user
            ?: return Result.failure(IllegalStateException("User creation failed. Please try again."))

        // 4. Call completeSignUp Cloud Function (Server-side HMAC verification and profile creation)
        val completeSignUpCallable = functions.getHttpsCallable("completeSignUp")
        val data = mapOf(
            "name" to cleanName,
            "dateOfBirth" to cleanDob,
            "phone" to cleanPhone,
            "email" to cleanEmail
        )

        return try {
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
     * Signs in user with email and password only. Never creates an account here.
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
                // Auth user exists but profile document is missing (e.g. deleted from Firestore)
                Result.failure(IllegalStateException("USER_PROFILE_MISSING:$cleanEmail"))
            }
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(IllegalStateException("USER_NOT_FOUND:$cleanEmail"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            // Check sign in methods to differentiate between non-existent account, Google-only account, or wrong password
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
     * Updates user's phone number via the secure updatePhone Cloud Function.
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
     * Verifies that the user already has a complete profile in Firestore.
     * If user does NOT have a profile in Firestore, signs out and returns GOOGLE_USER_NOT_REGISTERED.
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
                // User is fully registered in Firestore
                Result.success(existing)
            } else {
                // Incomplete or non-existent profile. Do not let user directly enter.
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

    /**
     * Maps Firebase errors to clean, friendly, human-readable messages. Never exposes raw exception text.
     */
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
