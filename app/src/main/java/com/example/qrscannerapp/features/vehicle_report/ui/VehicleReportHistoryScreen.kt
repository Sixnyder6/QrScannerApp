// Полная, ФИНАЛЬНАЯ версия файла: features/vehicle_report/ui/VehicleReportHistoryScreen.kt
// Реализовано: Отображение Snackbar с сообщениями об ошибках и успехе

package com.example.qrscannerapp.features.vehicle_report.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.*
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.features.vehicle_report.domain.model.VehicleReportHistory
import java.text.SimpleDateFormat
import java.util.*

// Класс для передачи данных в диалоговое окно
private data class DialogStatInfo(
    val title: String,
    val numbers: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleReportHistoryScreen(
    viewModel: VehicleReportHistoryViewModel = hiltViewModel(),
    onNavigateToAnalytics: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val reports = uiState.reports

    // V-- НОВОЕ: Получаем сообщение из ViewModel --V
    val userMessage = uiState.userMessage
    // Состояние для управления Snackbar (всплывающим уведомлением)
    val snackbarHostState = remember { SnackbarHostState() }
    // ^-- КОНЕЦ НОВОГО --^

    var reportToDelete by remember { mutableStateOf<VehicleReportHistory?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    // Состояние для управления диалоговым окном со списком номеров
    var selectedStat by remember { mutableStateOf<DialogStatInfo?>(null) }

    // V-- НОВОЕ: Обработка сообщений --V
    // Следим за userMessage. Если оно меняется и не null — показываем Snackbar
    LaunchedEffect(userMessage) {
        if (userMessage != null) {
            snackbarHostState.showSnackbar(
                message = userMessage,
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
            // Сообщаем ViewModel, что сообщение показано, чтобы сбросить его
            viewModel.messageShown()
        }
    }
    // ^-- КОНЕЦ НОВОГО --^

    // Показываем диалог, если `selectedStat` не null
    if (selectedStat != null) {
        ScooterListDialog(
            statInfo = selectedStat!!,
            onDismiss = { selectedStat = null }
        )
    }

    if (reportToDelete != null) {
        AlertDialog(
            onDismissRequest = { reportToDelete = null },
            title = { Text("Удалить отчет?") },
            text = { Text("Вы уверены, что хотите удалить запись отчета для файла \"${reportToDelete!!.fileName}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReport(reportToDelete!!)
                        reportToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StardustError)
                ) { Text("Удалить") }
            },
            dismissButton = { OutlinedButton(onClick = { reportToDelete = null }) { Text("Отмена") } },
            containerColor = StardustModalBg,
            titleContentColor = StardustTextPrimary,
            textContentColor = StardustTextSecondary
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = "Удалить все", tint = StardustError) },
            title = { Text("Удалить всю историю?") },
            text = { Text("Это действие необратимо удалит все отчеты с этого устройства и из облачного хранилища. Вы уверены?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllReports()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StardustError, contentColor = Color.White)
                ) { Text("Да, удалить все") }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteAllDialog = false }) { Text("Отмена") } },
            containerColor = StardustModalBg,
            titleContentColor = StardustTextPrimary,
            textContentColor = StardustTextSecondary
        )
    }

    AppBackground {
        Scaffold(
            // V-- НОВОЕ: Подключаем SnackbarHost --V
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // ^-- КОНЕЦ НОВОГО --^

            topBar = {
                TopAppBar(
                    title = { Text("История отчетов") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = StardustTextPrimary
                    ),
                    actions = {
                        if (reports.isNotEmpty()) {
                            IconButton(onClick = { showDeleteAllDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Удалить всю историю",
                                    tint = StardustTextSecondary
                                )
                            }
                        }
                    }
                )
            },
            containerColor = Color.Transparent,
            floatingActionButton = {
                if (reports.size >= 2) {
                    ExtendedFloatingActionButton(
                        onClick = onNavigateToAnalytics,
                        containerColor = StardustPrimary,
                        icon = { Icon(Icons.Default.Analytics, contentDescription = "Аналитика") },
                        text = { Text("Аналитика") }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                if (reports.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, contentDescription = "История пуста", modifier = Modifier.size(64.dp), tint = StardustTextSecondary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("История отчетов пуста. Загрузите Excel-файл, чтобы увидеть здесь запись.", color = StardustTextSecondary, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(reports, key = { it.id }) { report ->
                            ReportHistoryItem(
                                report = report,
                                onDeleteClick = { reportToDelete = report },
                                onStatClick = { stat -> selectedStat = stat }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Composable-функция для диалогового окна
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScooterListDialog(statInfo: DialogStatInfo, onDismiss: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    val textToCopy = statInfo.numbers.joinToString("\n")
                    clipboardManager.setText(AnnotatedString(textToCopy))
                    Toast.makeText(context, "Список скопирован", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("Копировать")
            }
        },
        title = { Text("${statInfo.title} (${statInfo.numbers.size})") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(statInfo.numbers) { number ->
                    Text(text = number, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), fontFamily = FontFamily.Monospace)
                }
            }
        },
        containerColor = StardustModalBg,
        titleContentColor = StardustTextPrimary,
        textContentColor = StardustTextSecondary
    )
}

@Composable
private fun ReportHistoryItem(
    report: VehicleReportHistory,
    onDeleteClick: () -> Unit,
    onStatClick: (DialogStatInfo) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = sdf.format(Date(report.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = "Файл", tint = StardustTextSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = report.fileName, fontWeight = FontWeight.SemiBold, color = StardustTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = formattedDate, fontSize = 12.sp, color = StardustTextSecondary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Удалить отчет", tint = StardustTextSecondary)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = StardustItemBg)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatRow(icon = Icons.Default.CheckCircle, label = "Готов к вывозу", count = report.readyForExportCount, color = Color(0xFF4CAF50),
                    onClick = { onStatClick(DialogStatInfo("Готов к вывозу", report.readyForExportList)) }
                )
                StatRow(icon = Icons.Default.Inventory2, label = "На хранении", count = report.storageCount, color = Color(0xFF2196F3),
                    onClick = { onStatClick(DialogStatInfo("На хранении", report.storageList)) }
                )
                if (report.awaitingRepairCount > 0) {
                    StatRow(icon = Icons.Default.Build, label = "Ожидает ремонта", count = report.awaitingRepairCount, color = Color(0xFFFFC107),
                        onClick = { onStatClick(DialogStatInfo("Ожидает ремонта", report.awaitingRepairList)) }
                    )
                }
                StatRow(icon = Icons.Default.Science, label = "Ожидает тестирования", count = report.awaitingTestingCount, color = Color(0xFF00BCD4),
                    onClick = { onStatClick(DialogStatInfo("Ожидает тестирования", report.awaitingTestingList)) }
                )
                StatRow(icon = Icons.Default.BatteryChargingFull, label = "Заряжен (>=70%)", count = report.testingChargedCount, color = Color(0xFFFF9800),
                    onClick = { onStatClick(DialogStatInfo("Заряжен (>=70%)", report.testingChargedList)) }
                )
                StatRow(icon = Icons.Default.BatteryAlert, label = "Разряжен (<50%)", count = report.testingDischargedCount, color = Color(0xFFF44336),
                    onClick = { onStatClick(DialogStatInfo("Разряжен (<50%)", report.testingDischargedList)) }
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    icon: ImageVector,
    label: String,
    count: Int,
    color: Color,
    onClick: () -> Unit
) {
    val modifier = if (count > 0) {
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    } else {
        Modifier.padding(vertical = 4.dp)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = StardustTextSecondary, modifier = Modifier.weight(1f))
        Text(text = count.toString(), color = StardustTextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable
fun VehicleReportHistoryScreenPreview() {
    val fakeReport = VehicleReportHistory(
        id = 1,
        timestamp = System.currentTimeMillis(),
        fileName = "example_report_2025.xlsx",
        readyForExportCount = 89,
        storageCount = 1,
        awaitingTestingCount = 71,
        testingChargedCount = 47,
        testingDischargedCount = 19,
        awaitingRepairCount = 15,
        readyForExportList = List(89) { "S$it" },
        awaitingRepairList = List(15) { "R$it" }
    )

    AppBackground {
        Box(modifier = Modifier.padding(16.dp)) {
            ReportHistoryItem(report = fakeReport, onDeleteClick = {}, onStatClick = {})
        }
    }
}