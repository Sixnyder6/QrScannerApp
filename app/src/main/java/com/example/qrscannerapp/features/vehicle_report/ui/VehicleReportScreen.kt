// Полная, исправленная версия файла: features/vehicle_report/ui/VehicleReportScreen.kt

package com.example.qrscannerapp.features.vehicle_report.ui

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// V-- ИЗМЕНЕНИЕ 1 из 2: Возвращаем импорт hiltViewModel --V
import androidx.hilt.navigation.compose.hiltViewModel
// ^-- КОНЕЦ ИЗМЕНЕНИЯ 1 из 2 --^
import com.example.qrscannerapp.*
import com.example.qrscannerapp.common.ui.AppBackground

private data class StatInfo(
    val title: String,
    val value: Int,
    val icon: ImageVector,
    val color: Color,
    val numbers: List<String>,
    val isSubCategory: Boolean = false
)

@Composable
fun VehicleReportScreen(
    // V-- ИЗМЕНЕНИЕ 2 из 2: Возвращаем создание ViewModel внутри Composable-функции --V
    viewModel: VehicleReportViewModel = hiltViewModel(),
    // ^-- КОНЕЦ ИЗМЕНЕНИЯ 2 из 2 --^
    onNavigateToHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedStat by remember { mutableStateOf<StatInfo?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            var displayName: String? = null
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex)
                    }
                }
            }
            viewModel.processExcelFile(it, displayName)
        }
    }

    if (selectedStat != null) {
        ScooterListDialog(
            statInfo = selectedStat!!,
            onDismiss = { selectedStat = null }
        )
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FileSelectionHeader(
                uiState = uiState,
                onSelectFileClick = {
                    filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                },
                onHistoryClick = onNavigateToHistory
            )
            Spacer(modifier = Modifier.height(24.dp))
            AnimatedContent(
                targetState = uiState,
                label = "content_animation"
            ) { state ->
                when {
                    state.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = StardustPrimary)
                        }
                    }
                    state.errorMessage != null -> {
                        InfoBlock(Icons.Default.ErrorOutline, state.errorMessage, StardustError)
                    }
                    state.fileName == null -> {
                        InfoBlock(Icons.Default.CloudUpload, "Нажмите кнопку выше, чтобы выбрать и загрузить файл .xlsx для анализа.")
                    }
                    else -> {
                        val stats = mutableListOf<StatInfo>()
                        stats.add(StatInfo("Готов к вывозу", state.readyForExportList.size, Icons.Default.CheckCircle, Color(0xFF4CAF50), state.readyForExportList))
                        stats.add(StatInfo("На хранении", state.storageList.size, Icons.Default.Inventory2, Color(0xFF2196F3), state.storageList))
                        stats.add(StatInfo("Ожидает ремонта", state.awaitingRepairList.size, Icons.Default.Build, Color(0xFFFFC107), state.awaitingRepairList))

                        stats.add(StatInfo("Ожидает тестирования", state.awaitingTestingList.size, Icons.Default.Science, Color(0xFF00BCD4), state.awaitingTestingList))
                        stats.add(StatInfo("Заряжен (>=70%)", state.testingChargedList.size, Icons.Default.BatteryChargingFull, Color(0xFFFF9800), state.testingChargedList, isSubCategory = true))
                        stats.add(StatInfo("Разряжен (<50%)", state.testingDischargedList.size, Icons.Default.BatteryAlert, Color(0xFFF44336), state.testingDischargedList, isSubCategory = true))

                        ResultsGrid(
                            stats = stats.filter { it.value > 0 },
                            onStatClick = { stat ->
                                if (stat.numbers.isNotEmpty()) {
                                    selectedStat = stat
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScooterListDialog(statInfo: StatInfo, onDismiss: () -> Unit) {
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
        title = { Text("${statInfo.title} (${statInfo.value})") },
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
private fun FileSelectionHeader(uiState: VehicleReportUiState, onSelectFileClick: () -> Unit, onHistoryClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onHistoryClick) {
                    Icon(Icons.Default.History, contentDescription = "История отчетов", tint = StardustTextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSelectFileClick,
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Выбрать файл", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выбрать Excel-файл")
                }
                Spacer(modifier = Modifier.width(48.dp))
            }

            if (uiState.fileName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Файл: ${uiState.fileName}",
                    color = StardustTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun InfoBlock(icon: ImageVector, text: String, iconTint: Color = StardustTextSecondary) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = iconTint)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = text, color = StardustTextSecondary, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ResultsGrid(stats: List<StatInfo>, onStatClick: (StatInfo) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp)
    ) {
        items(
            items = stats,
            key = { it.title },
            span = { stat ->
                GridItemSpan(if (stat.isSubCategory) 1 else 2)
            }
        ) { stat ->
            if (stat.isSubCategory) {
                SubStatCard(statInfo = stat, onClick = { onStatClick(stat) })
            } else {
                ResultStatCard(statInfo = stat, onClick = { onStatClick(stat) })
            }
        }
    }
}

@Composable
private fun ResultStatCard(statInfo: StatInfo, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg),
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = statInfo.icon, contentDescription = statInfo.title, tint = statInfo.color, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = statInfo.value.toString(), color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = statInfo.title, color = StardustTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun SubStatCard(statInfo: StatInfo, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StardustItemBg),
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = statInfo.icon, contentDescription = statInfo.title, tint = statInfo.color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = statInfo.value.toString(), color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(text = statInfo.title, color = StardustTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Start, lineHeight = 14.sp)
            }
        }
    }
}