package com.example.mayuranddhyeyinternshipproject.ui.screens.medicine

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mayuranddhyeyinternshipproject.data.model.MedicineForm
import com.example.mayuranddhyeyinternshipproject.data.model.PharmacyInfo
import com.example.mayuranddhyeyinternshipproject.data.model.TimeSlot
import com.example.mayuranddhyeyinternshipproject.ui.components.LoadingOverlay
import com.example.mayuranddhyeyinternshipproject.ui.components.MedMateTopAppBar
import com.example.mayuranddhyeyinternshipproject.ui.theme.MintAccent
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusError
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusErrorBg
import com.example.mayuranddhyeyinternshipproject.ui.theme.TealPrimary

@Composable
fun MedicineDetailsScreen(
    medicineId: String,
    medicineViewModel: MedicineViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by medicineViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val isNew = medicineId.isBlank() || medicineId == "new"

    var name by rememberSaveable { mutableStateOf("") }
    var dosage by rememberSaveable { mutableStateOf("") }
    var selectedForm by rememberSaveable { mutableStateOf(MedicineForm.TABLET) }
    var notes by rememberSaveable { mutableStateOf("") }

    // Map of Slot -> "HH:mm" (or empty if disabled)
    val slotTimes = remember {
        mutableStateMapOf(
            TimeSlot.MORNING.name to "08:00",
            TimeSlot.AFTERNOON.name to "",
            TimeSlot.EVENING.name to "",
            TimeSlot.NIGHT.name to "21:00"
        )
    }

    var pharmacyName by rememberSaveable { mutableStateOf("") }
    var pharmacyContact by rememberSaveable { mutableStateOf("") }
    var doctorName by rememberSaveable { mutableStateOf("") }
    var doctorContact by rememberSaveable { mutableStateOf("") }

    var showPharmacyPicker by remember { mutableStateOf(false) }

    // Load initial medicine if editing
    LaunchedEffect(medicineId) {
        medicineViewModel.loadMedicine(medicineId)
    }

    // Populate fields when medicine is loaded
    LaunchedEffect(uiState.currentMedicine) {
        uiState.currentMedicine?.let { med ->
            name = med.name
            dosage = med.dosage
            selectedForm = MedicineForm.fromString(med.form)
            notes = med.notes
            pharmacyName = med.pharmacyName
            pharmacyContact = med.pharmacyContact
            doctorName = med.doctorName
            doctorContact = med.doctorContact

            TimeSlot.entries.forEach { slot ->
                slotTimes[slot.name] = med.reminderTimes[slot.name] ?: ""
            }
        }
    }

    // If successfully saved, go back
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            medicineViewModel.resetSaveSuccess()
            onNavigateBack()
        }
    }

    fun openTimePicker(slot: TimeSlot) {
        val currentStr = slotTimes[slot.name]
        val (initHour, initMin) = if (!currentStr.isNullOrBlank() && currentStr.contains(":")) {
            val parts = currentStr.split(":")
            Pair(parts[0].toIntOrNull() ?: slot.defaultHour, parts[1].toIntOrNull() ?: slot.defaultMinute)
        } else {
            Pair(slot.defaultHour, slot.defaultMinute)
        }

        TimePickerDialog(
            context,
            { _, hour, minute ->
                slotTimes[slot.name] = String.format("%02d:%02d", hour, minute)
            },
            initHour,
            initMin,
            true
        ).show()
    }

    Scaffold(
        topBar = {
            MedMateTopAppBar(
                title = if (isNew) "Add Medicine" else "Edit Medicine",
                subtitle = "Set dosage, timings and doctor info",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
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

                // Error Banner
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

                // 1. Basic Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Medicine Information",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                medicineViewModel.clearError()
                            },
                            label = { Text("Medicine Name *") },
                            placeholder = { Text("e.g. Paracetamol, Metformin") },
                            leadingIcon = {
                                Icon(Icons.Default.Medication, contentDescription = null, tint = TealPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = dosage,
                            onValueChange = {
                                dosage = it
                                medicineViewModel.clearError()
                            },
                            label = { Text("Dosage *") },
                            placeholder = { Text("e.g. 500 mg, 1 tablet, 5 ml") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Formulation",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MedicineForm.entries.forEach { form ->
                                FilterChip(
                                    selected = selectedForm == form,
                                    onClick = { selectedForm = form },
                                    label = { Text(form.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes / Instructions") },
                            placeholder = { Text("Take after meals with water") },
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Reminder Time Slots Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Reminder Schedule",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Enable slots and adjust reminder times for this medicine",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        TimeSlot.entries.forEach { slot ->
                            val currentSlotTime = slotTimes[slot.name]
                            val isEnabled = !currentSlotTime.isNullOrBlank()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isEnabled,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                slotTimes[slot.name] = String.format("%02d:%02d", slot.defaultHour, slot.defaultMinute)
                                            } else {
                                                slotTimes[slot.name] = ""
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
                                    )
                                    Column {
                                        Text(
                                            text = slot.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isEnabled) currentSlotTime ?: "" else "Disabled",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isEnabled) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isEnabled) {
                                    OutlinedButton(
                                        onClick = { openTimePicker(slot) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Change Time")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Linked Medical Facility & Doctor Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Medical Facility & Doctor",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { showPharmacyPicker = !showPharmacyPicker },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocalPharmacy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = TealPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (showPharmacyPicker) "Hide" else "Pharmacies",
                                    color = TealPrimary,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        // Nearby Pharmacy Selection List
                        AnimatedVisibility(visible = showPharmacyPicker) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Tap a pharmacy to link it:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                uiState.nearbyPharmacies.forEach { p ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                pharmacyName = p.name
                                                pharmacyContact = p.phone
                                                showPharmacyPicker = false
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (pharmacyName == p.name) MintAccent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(p.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                Text("${p.distanceKm} km away • ${p.phone}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (pharmacyName == p.name) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = TealPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = pharmacyName,
                            onValueChange = { pharmacyName = it },
                            label = { Text("Pharmacy Name") },
                            leadingIcon = { Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = TealPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = pharmacyContact,
                            onValueChange = { pharmacyContact = it },
                            label = { Text("Pharmacy Phone") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TealPrimary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = doctorName,
                            onValueChange = { doctorName = it },
                            label = { Text("Prescribing Doctor Name") },
                            placeholder = { Text("Dr. Sharma") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TealPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = doctorContact,
                            onValueChange = { doctorContact = it },
                            label = { Text("Doctor Phone / Clinic Contact") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TealPrimary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        val activeTimes = slotTimes.filter { it.value.isNotBlank() }
                        medicineViewModel.saveMedicine(
                            context = context,
                            id = if (isNew) "" else medicineId,
                            name = name,
                            dosage = dosage,
                            form = selectedForm,
                            notes = notes,
                            reminderTimes = activeTimes,
                            pharmacyName = pharmacyName,
                            pharmacyContact = pharmacyContact,
                            doctorName = doctorName,
                            doctorContact = doctorContact
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !uiState.isSaving
                ) {
                    Text(
                        text = if (isNew) "Save Medicine" else "Update Medicine",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (!isNew) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            medicineViewModel.deleteMedicine(context, medicineId)
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Medicine", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))
            }

            LoadingOverlay(isLoading = uiState.isSaving)
        }
    }
}
