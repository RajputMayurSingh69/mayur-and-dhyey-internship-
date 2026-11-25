package com.example.mayuranddhyeyinternshipproject.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mayuranddhyeyinternshipproject.R
import com.example.mayuranddhyeyinternshipproject.ui.components.LoadingOverlay
import com.example.mayuranddhyeyinternshipproject.ui.theme.MintAccent
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusError
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusErrorBg
import com.example.mayuranddhyeyinternshipproject.ui.theme.TealPrimary
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AuthTab {
    SIGN_IN,
    CREATE_ACCOUNT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    initialTab: AuthTab = AuthTab.SIGN_IN,
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToVerifyEmail: () -> Unit = {},
    onNavigateToHome: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = context as? Activity

    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    // Sign In Fields
    var signInEmail by rememberSaveable { mutableStateOf("") }
    var signInPassword by rememberSaveable { mutableStateOf("") }
    var signInPasswordVisible by rememberSaveable { mutableStateOf(false) }

    // Forgot Password Dialog State (Phase 2C)
    var showForgotPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var forgotEmail by rememberSaveable { mutableStateOf("") }

    // Phone OTP Dialog State (Phase 2A)
    var enteredOtpCode by rememberSaveable { mutableStateOf("") }
    var otpResendCooldown by remember { mutableIntStateOf(60) }

    // Sign Up Fields
    var signUpFullName by rememberSaveable { mutableStateOf("") }
    var signUpDob by rememberSaveable { mutableStateOf("") }
    var signUpPhone by rememberSaveable { mutableStateOf("") }
    var signUpEmail by rememberSaveable { mutableStateOf("") }
    var signUpPassword by rememberSaveable { mutableStateOf("") }
    var signUpPasswordVisible by rememberSaveable { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Sign Up Validation Errors
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var dobError by rememberSaveable { mutableStateOf<String?>(null) }
    var phoneError by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }

    // OTP Resend Cooldown Countdown
    LaunchedEffect(uiState.isOtpPending, otpResendCooldown) {
        if (uiState.isOtpPending && otpResendCooldown > 0) {
            delay(1000L)
            otpResendCooldown -= 1
        }
    }

    // Automatic tab switching and field pre-filling based on ViewModel state
    LaunchedEffect(uiState.switchToTab, uiState.prefillEmail, uiState.prefillName) {
        uiState.switchToTab?.let { targetTab ->
            selectedTab = targetTab
            uiState.prefillEmail?.let { email ->
                if (targetTab == AuthTab.SIGN_IN) {
                    signInEmail = email
                } else {
                    signUpEmail = email
                    emailError = null
                }
            }
            uiState.prefillName?.let { name ->
                if (name.isNotBlank()) {
                    signUpFullName = name
                    nameError = null
                }
            }
            authViewModel.clearTabSwitch()
        }
    }

    // Navigation on Success or Email Verification Required (Phase 2B)
    LaunchedEffect(uiState.isSuccess, uiState.isEmailVerificationPending) {
        if (uiState.isEmailVerificationPending) {
            onNavigateToVerifyEmail()
        } else if (uiState.isSuccess) {
            if (authViewModel.isEmailVerified()) {
                authViewModel.resetState()
                onNavigateToHome()
            } else {
                onNavigateToVerifyEmail()
            }
        }
    }

    // Google Sign-In Activity Result Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                val email = account?.email ?: ""
                val name = account?.displayName ?: ""

                if (selectedTab == AuthTab.CREATE_ACCOUNT) {
                    // On Create Account tab, direct Google login is NOT allowed.
                    // Prefill email and name if available, and prompt user to fill all required details first.
                    if (email.isNotBlank()) {
                        signUpEmail = email
                        emailError = null
                    }
                    if (name.isNotBlank()) {
                        signUpFullName = name
                        nameError = null
                    }
                    authViewModel.setErrorMessage("Please fill in all details first to create an account.")
                } else {
                    // On Sign In tab, check Firebase for existing registration
                    if (idToken != null) {
                        val credential = GoogleAuthProvider.getCredential(idToken, null)
                        authViewModel.signInWithGoogle(credential, googleEmail = email, googleName = name)
                    } else {
                        account?.email?.let { unverifiedEmail ->
                            authViewModel.setErrorMessage("Google account ($unverifiedEmail) selected. Please ensure Google Provider & Web Client ID are enabled in Firebase Console.")
                        } ?: run {
                            authViewModel.setErrorMessage("Google Sign-In requires SHA-1 fingerprint configured in Firebase Console.")
                        }
                    }
                }
            } catch (e: Exception) {
                authViewModel.setErrorMessage(e.localizedMessage ?: "Google sign in was cancelled or failed.")
            }
        }
    }

    fun startGoogleSignIn() {
        authViewModel.clearError()
        authViewModel.clearInfo()
        try {
            val webClientId = try {
                val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                if (resId != 0) context.getString(resId) else null
            } catch (e: Exception) {
                null
            } ?: "93847664545-d6vj2485tfiiio2g8dqbapqatt09im73.apps.googleusercontent.com"

            val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()

            if (!webClientId.isNullOrBlank()) {
                gsoBuilder.requestIdToken(webClientId)
            }

            val client = GoogleSignIn.getClient(context, gsoBuilder.build())
            client.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(client.signInIntent)
            }
        } catch (e: Exception) {
            authViewModel.setErrorMessage("Google Sign-In is unavailable on this device.")
        }
    }

    // DatePicker Dialog for DOB
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        signUpDob = sdf.format(Date(millis))
                        dobError = null
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = TealPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // 2C: Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                authViewModel.clearForgotPasswordMessage()
            },
            title = {
                Text("Reset Password", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Enter your registered email address to receive a password reset link.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("example@gmail.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    uiState.forgotPasswordMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TealPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotEmail.isNotBlank()) {
                            authViewModel.sendPasswordResetEmail(forgotEmail)
                        }
                    },
                    enabled = forgotEmail.isNotBlank() && !uiState.isForgotPasswordLoading,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotPasswordDialog = false
                        authViewModel.clearForgotPasswordMessage()
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    // 2A: Phone OTP Verification Dialog
    if (uiState.isOtpPending) {
        AlertDialog(
            onDismissRequest = {
                authViewModel.cancelPhoneOtp()
                enteredOtpCode = ""
            },
            title = {
                Text("Verify Phone Number", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter the 6-digit verification code sent to your phone number.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = enteredOtpCode,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                enteredOtpCode = it
                            }
                        },
                        label = { Text("6-Digit Code") },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (otpResendCooldown > 0) "Resend code in ${otpResendCooldown}s" else "Didn't receive code?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (otpResendCooldown == 0 && activity != null) {
                            TextButton(onClick = {
                                otpResendCooldown = 60
                                authViewModel.resendPhoneOtp(activity)
                            }) {
                                Text("Resend", fontWeight = FontWeight.Bold, color = TealPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.submitPhoneOtp(enteredOtpCode)
                    },
                    enabled = enteredOtpCode.length == 6 && !uiState.isLoading,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Verify & Complete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    authViewModel.cancelPhoneOtp()
                    enteredOtpCode = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // MedMate Logo Header
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(MintAccent.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "MedMate",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Text(
                text = "Your daily medicine & wellness companion",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Tab-like Segmented Bar: [ Sign In ] [ Create Account ]
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Sign In Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = AuthTab.SIGN_IN
                                authViewModel.clearError()
                                authViewModel.clearInfo()
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == AuthTab.SIGN_IN) TealPrimary else Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedTab == AuthTab.SIGN_IN) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Create Account Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = AuthTab.CREATE_ACCOUNT
                                authViewModel.clearError()
                                authViewModel.clearInfo()
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == AuthTab.CREATE_ACCOUNT) TealPrimary else Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Create Account",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedTab == AuthTab.CREATE_ACCOUNT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Error Banner (Red)
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusErrorBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium.copy(color = StatusError),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Info / Guidance Banner (Mint/Teal)
            AnimatedVisibility(
                visible = uiState.infoMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.infoMessage?.let { info ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MintAccent.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TealPrimary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // FORM CONTENT BASED ON SELECTED TAB
            if (selectedTab == AuthTab.SIGN_IN) {
                // ==================== SIGN IN FORM ====================
                OutlinedTextField(
                    value = signInEmail,
                    onValueChange = {
                        signInEmail = it
                        authViewModel.clearError()
                        authViewModel.clearInfo()
                    },
                    label = { Text(stringResource(R.string.email_hint)) },
                    placeholder = { Text("example@gmail.com") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = TealPrimary)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = signInPassword,
                    onValueChange = {
                        signInPassword = it
                        authViewModel.clearError()
                        authViewModel.clearInfo()
                    },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { signInPasswordVisible = !signInPasswordVisible }) {
                            Icon(
                                imageVector = if (signInPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (signInPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (signInPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (signInEmail.isNotBlank() && signInPassword.isNotBlank()) {
                                authViewModel.signIn(signInEmail, signInPassword)
                            }
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 2C: Forgot Password Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        forgotEmail = signInEmail
                        showForgotPasswordDialog = true
                    }) {
                        Text(
                            text = "Forgot password?",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TealPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        authViewModel.signIn(signInEmail, signInPassword)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = signInEmail.isNotBlank() && signInPassword.isNotBlank() && !uiState.isLoading
                ) {
                    Text(
                        text = stringResource(R.string.sign_in),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                // ==================== CREATE ACCOUNT FORM ====================
                // Full Name
                OutlinedTextField(
                    value = signUpFullName,
                    onValueChange = {
                        signUpFullName = it
                        nameError = null
                        authViewModel.clearError()
                        authViewModel.clearInfo()
                    },
                    label = { Text(stringResource(R.string.full_name_hint)) },
                    placeholder = { Text("e.g. John Doe") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TealPrimary) },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = StatusError) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Date of Birth
                OutlinedTextField(
                    value = signUpDob,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.dob_hint)) },
                    placeholder = { Text("YYYY-MM-DD") },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TealPrimary) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date", tint = TealPrimary)
                        }
                    },
                    isError = dobError != null,
                    supportingText = dobError?.let { { Text(it, color = StatusError) } },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Phone Number
                OutlinedTextField(
                    value = signUpPhone,
                    onValueChange = {
                        if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                            signUpPhone = it
                            phoneError = null
                            authViewModel.clearError()
                            authViewModel.clearInfo()
                        }
                    },
                    label = { Text(stringResource(R.string.phone_hint)) },
                    placeholder = { Text("10 digit mobile number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TealPrimary) },
                    isError = phoneError != null,
                    supportingText = phoneError?.let { { Text(it, color = StatusError) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Email
                OutlinedTextField(
                    value = signUpEmail,
                    onValueChange = {
                        signUpEmail = it
                        emailError = null
                        authViewModel.clearError()
                        authViewModel.clearInfo()
                    },
                    label = { Text(stringResource(R.string.email_hint)) },
                    placeholder = { Text("username@gmail.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TealPrimary) },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it, color = StatusError) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Password
                OutlinedTextField(
                    value = signUpPassword,
                    onValueChange = {
                        signUpPassword = it
                        passwordError = null
                        authViewModel.clearError()
                        authViewModel.clearInfo()
                    },
                    label = { Text(stringResource(R.string.password_hint)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary) },
                    trailingIcon = {
                        IconButton(onClick = { signUpPasswordVisible = !signUpPasswordVisible }) {
                            Icon(
                                imageVector = if (signUpPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (signUpPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (signUpPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = passwordError != null,
                    supportingText = {
                        Text(
                            text = passwordError ?: "Exactly 8 characters with letters & numbers",
                            color = if (passwordError != null) StatusError else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Phase 2A: Button starts Phone OTP verification
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        var valid = true
                        if (signUpFullName.isBlank() || !signUpFullName.matches(Regex("^[a-zA-Z\\s]+$"))) {
                            nameError = "Letters and spaces only."
                            valid = false
                        }
                        if (signUpDob.isBlank()) {
                            dobError = "Please select your date of birth."
                            valid = false
                        }
                        if (signUpPhone.length != 10) {
                            phoneError = "Phone must be exactly 10 digits."
                            valid = false
                        }
                        if (!signUpEmail.matches(Regex("^[a-zA-Z0-9._%+-]+@gmail\\.com$"))) {
                            emailError = "Must be a valid @gmail.com email."
                            valid = false
                        }
                        if (signUpPassword.length != 8 || !signUpPassword.any { it.isLetter() } || !signUpPassword.any { it.isDigit() }) {
                            passwordError = "Exactly 8 chars containing letters and numbers."
                            valid = false
                        }

                        if (valid && activity != null) {
                            otpResendCooldown = 60
                            authViewModel.startSignUpWithPhoneOtp(
                                activity = activity,
                                fullName = signUpFullName,
                                dob = signUpDob,
                                phone = signUpPhone,
                                email = signUpEmail,
                                pass = signUpPassword
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = "Verify Phone & Create Account",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // ==================== "OR" DIVIDER ====================
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "  OR  ",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }
            Spacer(modifier = Modifier.height(18.dp))

            // ==================== CONTINUE WITH GOOGLE BUTTON ====================
            OutlinedButton(
                onClick = { startGoogleSignIn() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continue with Google",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        LoadingOverlay(isLoading = uiState.isLoading)
    }
}
