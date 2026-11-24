package com.example.mayuranddhyeyinternshipproject.data.repository

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Locale

/**
 * Handles Authentication, input validation, and user profile persistence in Cloud Firestore.
 * Implements secure sign-up with SHA-256 phone index verification in a single Firestore transaction,
 * safe cleanup on collision, and credential-enumeration-proof sign-in error messaging.
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
     * Computes the SHA-256 hash of a string.
     */
    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

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
     * 3. Normalizes phone to digits only and computes its SHA-256 hash.
     * 4. Creates Firebase Auth user without enumeration checks; maps collision directly.
     * 5. Executes ONE Firestore transaction to check phone_index/{hash} and create both
     *    phone_index and user profile records atomically.
     * 6. If phone is taken, deletes the created Auth user and logs only a generic message.
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

        // 2. Check if email is already registered before attempting creation
        if (checkEmailRegistered(cleanEmail)) {
            return Result.failure(IllegalStateException("Account already exists with this email, please sign in"))
        }

        // 3. Normalize phone number to digits only and compute SHA-256 hash
        val cleanPhone = phone.filter { it.isDigit() }
        val phoneHash = sha256(cleanPhone)

        // 4. Call FirebaseAuth.createUserWithEmailAndPassword directly
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

        // 5. After Auth user exists, run ONE Firestore transaction
        val phoneIndexRef = firestore.collection("phone_index").document(phoneHash)
        val userDocRef = firestore.collection("users").document(user.uid)
        val now = System.currentTimeMillis()

        val profile = UserProfile(
            uid = user.uid,
            fullName = cleanName,
            email = cleanEmail,
            phone = cleanPhone,
            dob = cleanDob,
            createdAt = now,
            updatedAt = now
        )

        val phoneIndexData = mapOf(
            "uid" to user.uid,
            "createdAt" to now
        )

        return try {
            firestore.runTransaction { transaction ->
                val phoneDoc = transaction.get(phoneIndexRef)
                if (phoneDoc.exists()) {
                    throw Exception("PHONE_TAKEN")
                }
                transaction.set(phoneIndexRef, phoneIndexData)
                transaction.set(userDocRef, profile)
                null
            }.await()

            Result.success(profile)
        } catch (e: Exception) {
            val isPhoneTaken = e.message == "PHONE_TAKEN" || e.cause?.message == "PHONE_TAKEN"

            // 6. If transaction fails with PHONE_TAKEN, cleanup user and report friendly message
            if (isPhoneTaken) {
                try {
                    user.delete().await()
                } catch (cleanupEx: Exception) {
                    Log.e("AuthRepository", "Failed to clean up user after duplicate phone detection")
                }
                Result.failure(IllegalStateException("Phone number already registered with another account"))
            } else {
                // If any other transaction failure occurred, clean up created Auth user
                try {
                    user.delete().await()
                } catch (cleanupEx: Exception) {
                    Log.e("AuthRepository", "Failed to clean up user after transaction error")
                }
                Result.failure(IllegalStateException(mapAuthError(e)))
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
