package com.example.mayuranddhyeyinternshipproject.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
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
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusError
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusErrorBg
import com.example.mayuranddhyeyinternshipproject.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var fullName by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Local validation error messages
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var dobError by rememberSaveable { mutableStateOf<String?>(null) }
    var phoneError by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            authViewModel.resetState()
            onNavigateToHome()
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        dob = sdf.format(Date(millis))
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

            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Text(
                text = "Join MedMate to track your doses seamlessly",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Error Banner from ViewModel / Firebase
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

            // 1. Full Name (letters and spaces only)
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    nameError = if (it.isNotBlank() && !it.matches(Regex("^[a-zA-Z\\s]+$"))) {
                        "Name must contain letters and spaces only"
                    } else null
                    authViewModel.clearError()
                },
                label = { Text("Full Name") },
                placeholder = { Text("e.g. Mayur Singh") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = TealPrimary)
                },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it, color = StatusError) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Date of Birth (calendar icon opens DatePicker directly)
            OutlinedTextField(
                value = dob,
                onValueChange = {
                    dob = it
                    dobError = null
                },
                label = { Text("Date of Birth (YYYY-MM-DD)") },
                placeholder = { Text("Select date") },
                readOnly = true,
                leadingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Open Date Picker", tint = TealPrimary)
                    }
                },
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Pick", color = TealPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                isError = dobError != null,
                supportingText = dobError?.let { { Text(it, color = StatusError) } },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Phone Number (exactly 10 digits, digits only)
            OutlinedTextField(
                value = phone,
                onValueChange = { input ->
                    if (input.length <= 10 && input.all { it.isDigit() }) {
                        phone = input
                        phoneError = if (input.length in 1..9) "Phone must be exactly 10 digits" else null
                        authViewModel.clearError()
                    }
                },
                label = { Text("Phone Number (10 digits)") },
                placeholder = { Text("9876543210") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = TealPrimary)
                },
                isError = phoneError != null,
                supportingText = phoneError?.let { { Text(it, color = StatusError) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Email (must end with @gmail.com)
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = if (it.isNotBlank() && !it.matches(Regex("^[a-zA-Z0-9._%+-]+@gmail\\.com$"))) {
                        "Must be a valid @gmail.com address"
                    } else null
                    authViewModel.clearError()
                },
                label = { Text("Email (@gmail.com)") },
                placeholder = { Text("user@gmail.com") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = TealPrimary)
                },
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it, color = StatusError) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Password (exactly 8 chars, letters and numbers)
            OutlinedTextField(
                value = password,
                onValueChange = { input ->
                    if (input.length <= 8) {
                        password = input
                        val hasLetter = input.any { it.isLetter() }
                        val hasDigit = input.any { it.isDigit() }
                        passwordError = when {
                            input.isEmpty() -> null
                            input.length < 8 -> "Must be exactly 8 characters (${input.length}/8)"
                            !hasLetter || !hasDigit -> "Must contain both letters and numbers"
                            else -> null
                        }
                        authViewModel.clearError()
                    }
                },
                label = { Text("Password (8 chars: letters & numbers)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = TealPrimary)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    // Revalidate everything
                    var valid = true
                    if (fullName.isBlank() || !fullName.matches(Regex("^[a-zA-Z\\s]+$"))) {
                        nameError = "Letters and spaces only."
                        valid = false
                    }
                    if (dob.isBlank()) {
                        dobError = "Please select your date of birth."
                        valid = false
                    }
                    if (phone.length != 10) {
                        phoneError = "Phone must be exactly 10 digits."
                        valid = false
                    }
                    if (!email.matches(Regex("^[a-zA-Z0-9._%+-]+@gmail\\.com$"))) {
                        emailError = "Must be a valid @gmail.com email."
                        valid = false
                    }
                    if (password.length != 8 || !password.any { it.isLetter() } || !password.any { it.isDigit() }) {
                        passwordError = "Exactly 8 chars containing letters and numbers."
                        valid = false
                    }

                    if (valid) {
                        authViewModel.signUp(
                            fullName = fullName,
                            dob = dob,
                            phone = phone,
                            email = email,
                            pass = password
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
                    text = stringResource(R.string.sign_up),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Already have an account link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = {
                    authViewModel.resetState()
                    onNavigateToLogin()
                }) {
                    Text(
                        text = stringResource(R.string.sign_in),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        LoadingOverlay(isLoading = uiState.isLoading)
    }
}
