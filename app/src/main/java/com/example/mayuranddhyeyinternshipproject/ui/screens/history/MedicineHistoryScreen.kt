package com.example.mayuranddhyeyinternshipproject.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mayuranddhyeyinternshipproject.data.model.DoseRecord
import com.example.mayuranddhyeyinternshipproject.data.model.DoseStatus
import com.example.mayuranddhyeyinternshipproject.ui.components.EmptyStateView
import com.example.mayuranddhyeyinternshipproject.ui.components.MedMateTopAppBar
import com.example.mayuranddhyeyinternshipproject.ui.screens.home.BottomNavTab
import com.example.mayuranddhyeyinternshipproject.ui.screens.home.DoseStatusChip
import com.example.mayuranddhyeyinternshipproject.ui.screens.home.MedMateBottomNavigation
import com.example.mayuranddhyeyinternshipproject.ui.theme.MintAccent
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusError
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusErrorBg
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusSuccess
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusSuccessBg
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusWarning
import com.example.mayuranddhyeyinternshipproject.ui.theme.StatusWarningBg
import com.example.mayuranddhyeyinternshipproject.ui.theme.TealPrimary

@Composable
fun MedicineHistoryScreen(
    historyViewModel: HistoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToMedicines: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val uiState by historyViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            MedMateTopAppBar(
                title = "Medicine History",
                subtitle = "Past doses and intake compliance",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                showNotificationIcon = true
            )
        },
        bottomBar = {
            MedMateBottomNavigation(
                selectedTab = BottomNavTab.HISTORY,
                onNavigateToHome = onNavigateToHome,
                onNavigateToHistory = {},
                onNavigateToMedicines = onNavigateToMedicines,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filters section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Date Range filter chips
                Text(
                    text = "Timeframe",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryDateFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.selectedDateFilter == filter,
                            onClick = { historyViewModel.setDateFilter(filter) },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Medicine Name filter chips
                if (uiState.availableMedicines.size > 2) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Medicine",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.availableMedicines.forEach { medName ->
                            FilterChip(
                                selected = uiState.selectedMedicineFilter == medName,
                                onClick = { historyViewModel.setMedicineFilter(medName) },
                                label = { Text(medName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // History Records List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TealPrimary)
                }
            } else if (uiState.filteredRecords.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.History,
                    title = "No History Records Found",
                    description = "Take your scheduled medicines on time to build your health log."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredRecords, key = { it.id }) { record ->
                        HistoryCard(record = record)
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(record: DoseRecord) {
    val status = DoseStatus.fromString(record.status)
    val (icon, iconTint, iconBg) = when (status) {
        DoseStatus.ON_TIME -> Triple(Icons.Default.CheckCircle, StatusSuccess, StatusSuccessBg)
        DoseStatus.SLIGHTLY_LATE -> Triple(Icons.Default.Info, StatusWarning, StatusWarningBg)
        DoseStatus.VERY_LATE -> Triple(Icons.Default.Warning, StatusError, StatusErrorBg)
        else -> Triple(Icons.Default.CheckCircle, TealPrimary, MintAccent.copy(alpha = 0.2f))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = record.medicineName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${record.date} • Taken at ${record.takenTime} (${record.slot})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (record.scheduledTime.isNotBlank()) {
                        Text(
                            text = "Scheduled: ${record.scheduledTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            DoseStatusChip(status = status)
        }
    }
}
