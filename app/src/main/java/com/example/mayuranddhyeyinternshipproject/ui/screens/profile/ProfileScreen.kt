package com.example.mayuranddhyeyinternshipproject.ui.screens.profile

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mayuranddhyeyinternshipproject.data.model.TimeSlot
import com.example.mayuranddhyeyinternshipproject.ui.components.LoadingOverlay
import com.example.mayuranddhyeyinternshipproject.ui.components.MedMateTopAppBar
import com.example.mayuranddhyeyinternshipproject.ui.screens.home.BottomNavTab
import com.example.mayuranddhyeyinternshipproject.ui.screens.home.MedMateBottomNavigation
import com.example.mayuranddhyeyinternshipproject.ui.theme.MintAccent
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusError
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusErrorBg
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusSuccess
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusSuccessBg
import com.example.mayuranddhyeyinternshipproject.ui.theme.TealPrimary

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToMedicines: () -> Unit
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf("") }

    val defaultTimes = remember {
        mutableStateMapOf(
            TimeSlot.MORNING.name to "08:00",
            TimeSlot.AFTERNOON.name to "13:00",
            TimeSlot.EVENING.name to "18:00",
            TimeSlot.NIGHT.name to "21:00"
        )
    }

    var showSuccessToast by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.profile) {
        uiState.profile?.let { prof ->
            fullName = prof.fullName
            email = prof.email
            phone = prof.phone
            dob = prof.dob

            TimeSlot.entries.forEach { slot ->
                prof.defaultTimes[slot.name]?.let { defaultTimes[slot.name] = it }
            }
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            showSuccessToast = true
            profileViewModel.resetSaveSuccess()
        }
    }

    fun openTimePicker(slot: TimeSlot) {
        val currentStr = defaultTimes[slot.name] ?: ""
        val (initH, initM) = if (currentStr.contains(":")) {
            val parts = currentStr.split(":")
            Pair(parts[0].toIntOrNull() ?: slot.defaultHour, parts[1].toIntOrNull() ?: slot.defaultMinute)
        } else Pair(slot.defaultHour, slot.defaultMinute)

        TimePickerDialog(context, { _, h, m ->
            defaultTimes[slot.name] = String.format("%02d:%02d", h, m)
        }, initH, initM, true).show()
    }

    Scaffold(
        topBar = {
            MedMateTopAppBar(
                title = "Profile & Preferences",
                subtitle = "Manage personal info & app reminder times",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                showSettings = true,
                onSettingsClick = onNavigateToSettings
            )
        },
        bottomBar = {
            MedMateBottomNavigation(
                selectedTab = BottomNavTab.PROFILE,
                onNavigateToHome = onNavigateToHome,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToMedicines = onNavigateToMedicines,
                onNavigateToProfile = {}
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .imePadding()
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Avatar header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(MintAccent.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = fullName.ifBlank { "User" },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Success Message Banner
                AnimatedVisibility(visible = showSuccessToast) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusSuccessBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Profile and reminder preferences saved successfully!",
                            style = MaterialTheme.typography.bodyMedium.copy(color = StatusSuccess, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Error Banner
                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    uiState.errorMessage?.let { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
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

                // 1. Personal Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Personal Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TealPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Email (Account ID)") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TealPrimary) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) phone = it },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TealPrimary) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = dob,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date of Birth") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TealPrimary) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Default App Reminder Times Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Default Reminder Times",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Global default notification hours for medicine slots",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        TimeSlot.entries.forEach { slot ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = slot.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )

                                OutlinedButton(
                                    onClick = { openTimePicker(slot) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(defaultTimes[slot.name] ?: slot.formatDefaultTime())
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save Changes Button
                Button(
                    onClick = {
                        profileViewModel.updateProfile(
                            fullName = fullName,
                            dob = dob,
                            phone = phone,
                            defaultTimes = defaultTimes.toMap()
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !uiState.isSaving
                ) {
                    Text(
                        text = "Save Profile & Preferences",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))
            }

            LoadingOverlay(isLoading = uiState.isSaving)
        }
    }
}
