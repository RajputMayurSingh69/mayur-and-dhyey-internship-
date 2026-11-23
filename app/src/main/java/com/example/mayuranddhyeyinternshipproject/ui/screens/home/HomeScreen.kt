package com.example.mayuranddhyeyinternshipproject.ui.screens.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mayuranddhyeyinternshipproject.R
import com.example.mayuranddhyeyinternshipproject.data.model.DoseStatus
import com.example.mayuranddhyeyinternshipproject.data.model.TimeSlot
import com.example.mayuranddhyeyinternshipproject.ui.components.DoseFeedbackDialog
import com.example.mayuranddhyeyinternshipproject.ui.components.EmptyStateView
import com.example.mayuranddhyeyinternshipproject.ui.components.ExitConfirmationDialog
import com.example.mayuranddhyeyinternshipproject.ui.components.MedMateTopAppBar
import com.example.mayuranddhyeyinternshipproject.ui.theme.MintAccent
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusError
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusPending
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusPendingBg
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusSuccess
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusSuccessBg
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusWarning
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusWarningBg
import com.example.mayuranddhyeyinternshipproject.ui.theme.TealPrimary
import com.example.mayuranddhyeyinternshipproject.ui.theme.TealSecondary

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToMedicineList: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onAddMedicine: () -> Unit,
    onExitApp: () -> Unit
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showExitDialog by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    // Intercept back button for exit confirmation
    BackHandler {
        showExitDialog = true
    }

    // Permission Launcher for Android 13+ Notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                // If denied, we still allow app usage smoothly
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Automatically schedule alarms whenever medicines list is loaded or updated
    LaunchedEffect(uiState.medicines) {
        if (uiState.medicines.isNotEmpty()) {
            homeViewModel.rescheduleAllAlarms(context)
        }
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = {
                showExitDialog = false
                onExitApp()
            },
            onDismiss = { showExitDialog = false }
        )
    }

    if (uiState.showFeedbackDialog && uiState.lastRecordedStatus != null) {
        DoseFeedbackDialog(
            status = uiState.lastRecordedStatus!!,
            onDismiss = { homeViewModel.dismissFeedbackDialog() }
        )
    }

    Scaffold(
        topBar = {
            MedMateTopAppBar(
                title = "Hello, ${uiState.userName} 👋",
                subtitle = "Stay consistent with your medicines",
                showSettings = false,
                showNotificationIcon = true,
                onNotificationClick = {
                    Toast.makeText(context, "Medicine reminder notifications are active", Toast.LENGTH_SHORT).show()
                }
            )
        },
        bottomBar = {
            MedMateBottomNavigation(
                selectedTab = BottomNavTab.HOME,
                onNavigateToHome = {},
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToMedicines = onNavigateToMedicineList,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Today's Summary Progress Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                TodaySummaryCard(
                    totalDoses = uiState.totalDosesToday,
                    takenDoses = uiState.takenDosesToday,
                    onAddMedicineClick = onAddMedicine,
                    context = context
                )
            }

            // 2. Reminder Slots Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Schedule",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    TextButton(onClick = onAddMedicine) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Med", color = TealPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // If empty medicine list
            if (uiState.medicines.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Medication,
                        title = "No Medicines Added Yet",
                        description = "Add your prescribed medicines to receive daily reminders.",
                        actionButtonText = "Add Your First Medicine",
                        onActionClick = onAddMedicine
                    )
                }
            } else {
                // Time Slots (Morning, Afternoon, Evening, Night)
                TimeSlot.entries.forEach { slot ->
                    val medsInSlot = uiState.slotMedicines[slot] ?: emptyList()
                    if (medsInSlot.isNotEmpty()) {
                        item {
                            SlotCard(
                                slot = slot,
                                items = medsInSlot,
                                onMarkAsTaken = { item ->
                                    homeViewModel.markAsTaken(item)
                                }
                            )
                        }
                    }
                }
            }

            // 3. Recent Activity Records Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent Activity Log",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            if (uiState.todayRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "No doses taken yet today. Tap 'Mark as Taken' once you take your dose.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(uiState.todayRecords) { record ->
                    RecentActivityItem(record = record)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Today's summary card with clean progress indicator.
 */
@Composable
fun TodaySummaryCard(
    totalDoses: Int,
    takenDoses: Int,
    onAddMedicineClick: () -> Unit,
    context: android.content.Context
) {
    val progress = if (totalDoses > 0) (takenDoses.toFloat() / totalDoses.toFloat()).coerceIn(0f, 1f) else 0f
    val percent = (progress * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Progress",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (totalDoses > 0) "$takenDoses of $totalDoses doses taken today" else "No doses scheduled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(MintAccent.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TealPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = TealPrimary,
                trackColor = MintAccent.copy(alpha = 0.25f)
            )

            if (percent == 100 && totalDoses > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "All medications completed for today! Well done!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = StatusSuccess
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = {
                        val scheduler = com.example.mayuranddhyeyinternshipproject.reminder.ReminderScheduler(context)
                        scheduler.scheduleTestAlarm("Paracetamol", "500 mg", 5)
                        Toast.makeText(context, "Test alarm set! Ringing in 5 seconds with sound and popup.", Toast.LENGTH_LONG).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Alarm (5s)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

/**
 * Card for each time slot (Morning, Afternoon, Evening, Night).
 */
@Composable
fun SlotCard(
    slot: TimeSlot,
    items: List<ScheduledSlotMedicine>,
    onMarkAsTaken: (ScheduledSlotMedicine) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(TealSecondary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = slot.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${items.count { it.isTaken }}/${items.size} Taken",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            items.forEachIndexed { index, item ->
                SlotMedicineRow(
                    item = item,
                    onMarkAsTaken = { onMarkAsTaken(item) }
                )
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Individual medicine row inside a slot.
 */
@Composable
fun SlotMedicineRow(
    item: ScheduledSlotMedicine,
    onMarkAsTaken: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = item.medicine.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.medicine.dosage} • ${item.scheduledTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DoseStatusChip(status = item.status)

                if (item.isTaken) {
                    Surface(
                        shape = CircleShape,
                        color = StatusSuccessBg,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Taken",
                                tint = StatusSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onMarkAsTaken,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.mark_as_taken),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pill status chip showing Taken / Pending / Late (Locked horizontally, never wraps vertically).
 */
@Composable
fun DoseStatusChip(status: DoseStatus) {
    val (bgColor, textColor, label) = when (status) {
        DoseStatus.ON_TIME -> Triple(StatusSuccessBg, StatusSuccess, "On Time")
        DoseStatus.SLIGHTLY_LATE -> Triple(StatusWarningBg, StatusWarning, "Late")
        DoseStatus.VERY_LATE -> Triple(StatusError.copy(alpha = 0.15f), StatusError, "Very Late")
        DoseStatus.PENDING -> Triple(StatusPendingBg, StatusPending, "Pending")
        DoseStatus.MISSED -> Triple(StatusError.copy(alpha = 0.15f), StatusError, "Missed")
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Recent Activity item row.
 */
@Composable
fun RecentActivityItem(record: com.example.mayuranddhyeyinternshipproject.data.model.DoseRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(StatusSuccessBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = record.medicineName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Taken at ${record.takenTime} (${record.slot})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DoseStatusChip(status = DoseStatus.fromString(record.status))
        }
    }
}

enum class BottomNavTab {
    HOME, HISTORY, MEDICINES, PROFILE
}

@Composable
fun MedMateBottomNavigation(
    selectedTab: BottomNavTab,
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToMedicines: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.HOME,
            onClick = onNavigateToHome,
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(indicatorColor = MintAccent.copy(alpha = 0.3f))
        )
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.HISTORY,
            onClick = onNavigateToHistory,
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text("History") },
            colors = NavigationBarItemDefaults.colors(indicatorColor = MintAccent.copy(alpha = 0.3f))
        )
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.MEDICINES,
            onClick = onNavigateToMedicines,
            icon = { Icon(Icons.Default.Medication, contentDescription = "Medicines") },
            label = { Text("Meds") },
            colors = NavigationBarItemDefaults.colors(indicatorColor = MintAccent.copy(alpha = 0.3f))
        )
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.PROFILE,
            onClick = onNavigateToProfile,
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            colors = NavigationBarItemDefaults.colors(indicatorColor = MintAccent.copy(alpha = 0.3f))
        )
    }
}
