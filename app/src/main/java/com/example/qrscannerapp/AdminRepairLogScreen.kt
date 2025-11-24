// File: AdminRepairLogScreen.kt

package com.example.qrscannerapp

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.features.electrician.domain.model.BatteryRepairLog
import com.example.qrscannerapp.features.electrician.utils.pdf.PdfExporter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminRepairLogScreen() {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val viewModel: AdminRepairLogViewModel = viewModel(
        factory = AdminRepairLogViewModelFactory(authManager)
    )

    val uiState by viewModel.uiState.collectAsState()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.exportHistoryToPdf(context, uri)
            }
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDateRangePickerState()

    // Архитектура уже корректна. AppBackground является корневым элементом.
    AppBackground {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Журнал ремонтов",
                    color = StardustTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Выбрать дату", tint = StardustTextSecondary)
                }
                OutlinedButton(
                    onClick = {
                        val adminName = authManager.authState.value.userName ?: "Admin"
                        PdfExporter.launchCreatePdfIntent(createDocumentLauncher, adminName)
                    },
                    border = BorderStroke(1.dp, StardustItemBg)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Экспорт", tint = StardustTextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Экспорт", color = StardustTextSecondary)
                }
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDatePicker = false
                                val startDate = datePickerState.selectedStartDateMillis
                                val endDate = datePickerState.selectedEndDateMillis
                                if (startDate != null && endDate != null) {
                                    viewModel.loadLogsForDateRange(startDate, endDate)
                                }
                            }
                        ) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
                    }
                ) {
                    DateRangePicker(state = datePickerState)
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(color = StardustPrimary)
                    }
                    uiState.error != null -> {
                        Text(
                            text = uiState.error!!,
                            color = StardustError,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    uiState.groupedRepairLogs.isEmpty() -> {
                        Text(
                            text = "За выбранный период записей не найдено.",
                            color = StardustTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            uiState.groupedRepairLogs.forEach { (date, logsForDate) ->
                                item(key = date) {
                                    val isExpanded = uiState.expandedDate == date
                                    Column {
                                        AdminDateHeader(
                                            date = date,
                                            count = logsForDate.size,
                                            isExpanded = isExpanded,
                                            onClick = { viewModel.onDateHeaderClick(date) }
                                        )
                                        AnimatedVisibility(
                                            visible = isExpanded,
                                            enter = expandVertically(animationSpec = tween(300)),
                                            exit = shrinkVertically(animationSpec = tween(300))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(top = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                logsForDate.forEach { log ->
                                                    AdminHistoryLogItem(log = log)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminDateHeader(date: String, count: Int, isExpanded: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date,
                color = StardustTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$count шт.",
                color = StardustTextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AdminHistoryLogItem(log: BatteryRepairLog) {
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg),
        modifier = Modifier.padding(start = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.batteryId,
                    color = StardustTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = log.manufacturer,
                    color = StardustTextPrimary,
                    modifier = Modifier
                        .background(StardustItemBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = log.repairs.joinToString(", "),
                color = StardustTextSecondary,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = StardustItemBg)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = log.electricianName,
                    color = StardustTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "в ${sdf.format(Date(log.timestamp))}",
                    color = StardustTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}