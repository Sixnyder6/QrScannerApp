// File: HistoryScreen.kt

package com.example.qrscannerapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
// import androidx.compose.ui.graphics.Brush // <-- ЭТОТ ИМПОРТ БОЛЬШЕ НЕ НУЖЕН
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.qrscannerapp.common.ui.AppBackground
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import com.example.qrscannerapp.core.model.SessionType
import com.example.qrscannerapp.features.scanner.domain.model.ScanItem
import com.example.qrscannerapp.features.scanner.domain.model.ScanSession

@Composable
fun HistoryScreen(viewModel: QrScannerViewModel, navController: NavHostController) {
    val sessions by viewModel.scanSessions.collectAsState()

    // V-- НАЧАЛО ИЗМЕНЕНИЙ: ИНТЕГРАЦИЯ APPBACKGROUND --V
    AppBackground {
        if (sessions.isEmpty()) {
            PlaceholderScreen(text = "Общая история сканирований пуста.\n\nСохраните сессию на главном экране, чтобы она появилась здесь.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = sessions, key = { it.id }) { session ->
                    SessionListItem(
                        session = session,
                        onClick = {
                            navController.navigate("session_detail/${session.id}")
                        }
                    )
                }
            }
        }
    }
    // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^
}

@Composable
private fun SessionListItem(session: ScanSession, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val sessionType = when (session.type) {
                    SessionType.SCOOTERS -> "Самокаты"
                    SessionType.BATTERIES -> "АКБ"
                }
                val title = session.name?.takeIf { it.isNotBlank() } ?: "$sessionType (${session.items.size} шт.)"
                Text(
                    text = title,
                    color = StardustTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))

                val creatorInfo = session.creatorName ?: "Неизвестный"
                Text(
                    text = "${formatTimestamp(session.timestamp)} • $creatorInfo",
                    color = StardustTextSecondary,
                    fontSize = 14.sp
                )

                if (session.name?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$sessionType: найдено ${session.items.size} шт.",
                        color = StardustTextSecondary.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Перейти к деталям",
                tint = StardustTextSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(sessionId: String, viewModel: QrScannerViewModel, navController: NavHostController) {
    val sessions by viewModel.scanSessions.collectAsState()
    val session = remember(sessions, sessionId) {
        sessions.find { it.id == sessionId }
    }
    LaunchedEffect(session) {
        if (session == null) {
            navController.popBackStack()
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    val exportSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    session?.let { currentSession ->
        // V-- НАЧАЛО ИЗМЕНЕНИЙ: ДЕЛАЕМ SCAFFOLD ПРОЗРАЧНЫМ --V
        AppBackground {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent, // <-- Изменено
                bottomBar = {
                    BottomAppBar(
                        containerColor = StardustGlassBg,
                        contentColor = StardustTextSecondary,
                    ) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Удалить сессию")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = { showExportSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
                        ) {
                            Text("Экспорт / Поделиться")
                        }
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp)
                ) {
                    items(items = currentSession.items, key = { it.id }) { item ->
                        HistoryScanListItem(
                            item = item,
                            onDelete = {
                                viewModel.deleteItemFromSession(sessionId, item)
                            },
                            onCopy = { code ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("QR Code", code))
                                Toast.makeText(context, "Код скопирован!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
        // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Удалить сессию?") },
                text = { Text("Это действие нельзя будет отменить. Сессия будет удалена для всех пользователей.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSession(sessionId)
                            showDeleteDialog = false
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("Удалить") }
                },
                dismissButton = {
                    Button(onClick = { showDeleteDialog = false }) {
                        Text("Отмена")
                    }
                },
                containerColor = StardustModalBg,
                titleContentColor = StardustTextPrimary,
                textContentColor = StardustTextSecondary
            )
        }

        if (showExportSheet) {
            SessionExportSheet(
                listToExport = currentSession.items,
                sheetState = exportSheetState,
                onDismiss = {
                    scope.launch { exportSheetState.hide() }.invokeOnCompletion { if (!exportSheetState.isVisible) showExportSheet = false }
                },
                onCopyAll = { list ->
                    val allCodes = list.joinToString("\n") { it.code }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("All Codes", allCodes))
                    Toast.makeText(context, "Все коды скопированы!", Toast.LENGTH_SHORT).show()
                    scope.launch { exportSheetState.hide() }.invokeOnCompletion { if (!exportSheetState.isVisible) showExportSheet = false }
                },
                onShare = { list ->
                    val allCodes = list.joinToString("\n") { it.code }
                    val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, allCodes); type = "text/plain" }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                    scope.launch { exportSheetState.hide() }.invokeOnCompletion { if (!exportSheetState.isVisible) showExportSheet = false }
                },
                onNavigateToPalletDistribution = {
                    scope.launch { exportSheetState.hide() }.invokeOnCompletion {
                        if (!exportSheetState.isVisible) {
                            showExportSheet = false
                            viewModel.onNavigateToPalletDistribution()
                            navController.navigate(Screen.PalletDistribution.route)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun HistoryScanListItem(
    item: ScanItem,
    onDelete: () -> Unit,
    onCopy: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item.thumbnail?.let { byteArray ->
                val bitmap = remember(byteArray) { BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size) }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Scan thumbnail",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = item.code,
                color = StardustTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { onCopy(item.code) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", tint = StardustTextSecondary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Удалить", tint = StardustTextSecondary)
                }
            }
        }
        HorizontalDivider(color = StardustItemBg, thickness = 1.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionExportSheet(
    listToExport: List<ScanItem>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onCopyAll: (List<ScanItem>) -> Unit,
    onShare: (List<ScanItem>) -> Unit,
    onNavigateToPalletDistribution: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StardustModalBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Экспорт сессии", color = StardustTextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNavigateToPalletDistribution,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StardustSuccess.copy(alpha = 0.3f), contentColor = StardustSuccess)
            ) {
                Text("Приемка на склад", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = { onCopyAll(listToExport) }, modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) {
                Text("Копировать в буфер обмена", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { onShare(listToExport) }, modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) {
                Text("Поделиться (как текст)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StardustItemBg)
            ) {
                Text("Отмена", color = StardustTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy 'в' HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = StardustTextPrimary, fontSize = 24.sp, textAlign = TextAlign.Center)
    }
}