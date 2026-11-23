package com.example.mayuranddhyeyinternshipproject.data.repository

import com.example.mayuranddhyeyinternshipproject.data.model.UserProfile
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Handles Authentication, input validation, and user profile persistence in Cloud Firestore.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
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
     * Checks if email is already in use by Firebase Auth.
     */
    @Suppress("DEPRECATION")
    suspend fun isEmailRegistered(email: String): Boolean {
        return try {
            val methods = auth.fetchSignInMethodsForEmail(email.trim()).await()
            methods.signInMethods?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks whether phone number already exists in Firestore "users" collection.
     */
    suspend fun isPhoneRegistered(phone: String): Boolean {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("phone", phone.trim())
                .limit(1)
                .get()
                .await()
            !querySnapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Registers a new user following all pre-conditions and saving profile to Firestore.
     */
    suspend fun signUp(
        fullName: String,
        dob: String,
        phone: String,
        email: String,
        password: String
    ): Result<UserProfile> {
        // Enforce validations before any Firebase call
        validateFullName(fullName)?.let { return Result.failure(IllegalArgumentException(it)) }
        validateDateOfBirth(dob)?.let { return Result.failure(IllegalArgumentException(it)) }
        validatePhone(phone)?.let { return Result.failure(IllegalArgumentException(it)) }
        validateEmail(email)?.let { return Result.failure(IllegalArgumentException(it)) }
        validatePassword(password)?.let { return Result.failure(IllegalArgumentException(it)) }

        val cleanEmail = email.trim()
        val cleanPhone = phone.trim()
        val cleanName = fullName.trim()
        val cleanDob = dob.trim()

        return try {
            // Check 1: Email already exists in Auth
            if (isEmailRegistered(cleanEmail)) {
                return Result.failure(IllegalStateException("Account already exists, please sign in"))
            }

            // Create Firebase Auth user
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user ?: return Result.failure(IllegalStateException("User creation failed."))

            // Check 2: Phone number uniqueness verification (user is now authenticated, satisfying security rules)
            if (isPhoneRegistered(cleanPhone)) {
                try { user.delete().await() } catch (ignored: Exception) {}
                return Result.failure(IllegalStateException("Phone number already registered with another account"))
            }

            val profile = UserProfile(
                uid = user.uid,
                fullName = cleanName,
                email = cleanEmail,
                phone = cleanPhone,
                dob = cleanDob,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Save to Firestore "users/{uid}"
            firestore.collection("users").document(user.uid).set(profile).await()

            Result.success(profile)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(IllegalStateException("Account already exists, please sign in"))
        } catch (e: FirebaseNetworkException) {
            Result.failure(IllegalStateException("Network connection error. Please check your internet connection."))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(mapAuthError(e)))
        }
    }

    /**
     * Signs in user. Never auto-creates accounts for non-existent users.
     */
    suspend fun signIn(email: String, password: String): Result<UserProfile> {
        validateEmail(email)?.let { return Result.failure(IllegalArgumentException(it)) }
        if (password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Password cannot be empty."))
        }

        val cleanEmail = email.trim()

        return try {
            val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user ?: return Result.failure(IllegalStateException("Sign in failed."))

            // Fetch profile
            val doc = firestore.collection("users").document(user.uid).get().await()
            val profile = doc.toObject(UserProfile::class.java) ?: UserProfile(
                uid = user.uid,
                email = cleanEmail
            )

            Result.success(profile)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(IllegalStateException("No account found. Please create a new account"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(IllegalStateException("Invalid email or password. Please check your credentials."))
        } catch (e: FirebaseNetworkException) {
            Result.failure(IllegalStateException("Network error. Please verify your internet connection."))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(mapAuthError(e)))
        }
    }

    fun signOut() {
        auth.signOut()
    }

    /**
     * Signs in using a Google AuthCredential and saves/retrieves user profile in Firestore.
     */
    suspend fun signInWithGoogleCredential(credential: com.google.firebase.auth.AuthCredential): Result<UserProfile> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: return Result.failure(IllegalStateException("Google sign in failed."))

            val docRef = firestore.collection("users").document(user.uid)
            val doc = docRef.get().await()
            val existing = doc.toObject(UserProfile::class.java)

            val profile = if (existing != null) {
                existing
            } else {
                val newProfile = UserProfile(
                    uid = user.uid,
                    fullName = user.displayName ?: "MedMate User",
                    email = user.email ?: "",
                    phone = user.phoneNumber ?: "",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                docRef.set(newProfile).await()
                newProfile
            }
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(mapAuthError(e)))
        }
    }

    private fun mapAuthError(e: Exception): String {
        val msg = e.localizedMessage ?: ""
        return when {
            msg.contains("no user record", ignoreCase = true) -> "No account found. Please create a new account"
            msg.contains("password is invalid", ignoreCase = true) -> "Incorrect password. Please try again."
            msg.contains("network error", ignoreCase = true) -> "Network error. Please verify your connection."
            msg.contains("too many requests", ignoreCase = true) -> "Too many attempts. Please try again later."
            else -> "An error occurred while connecting. Please try again."
        }
    }
}
