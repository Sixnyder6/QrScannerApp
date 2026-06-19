package com.example.qrscannerapp.features.inventory.ui.storage.sheets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.*
import com.example.qrscannerapp.features.inventory.data.export.StorageExportManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellExportSheet(cell: StorageCell, sheetState: SheetState, storageExportManager: StorageExportManager, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun dismiss() { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = StardustModalBg, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Экспорт: ${cell.name}", color = StardustTextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${cell.items.size} самокатов", color = StardustTextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(20.dp))
            ExportOptionButton(icon = Icons.Default.ContentCopy, title = "Копировать список", subtitle = "Как хранится, без сортировки", containerColor = StardustItemBg, contentColor = StardustTextPrimary) {
                val text = cell.items.reversed().joinToString("\n") { scooterId ->
                    val arrows = getDirectionArrows(cell, scooterId)
                    if (arrows.isNotEmpty()) "$scooterId \t$arrows" else scooterId
                }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Cell Items", text))
                Toast.makeText(context, "Скопировано ${cell.items.size} номеров", Toast.LENGTH_SHORT).show(); dismiss()
            }
            Spacer(modifier = Modifier.height(8.dp))
            ExportOptionButton(icon = Icons.AutoMirrored.Filled.Sort, title = "Копировать отсортированный", subtitle = "По возрастанию номера", containerColor = StardustItemBg, contentColor = StardustTextPrimary) {
                val text = cell.items.sorted().joinToString("\n") { scooterId ->
                    val arrows = getDirectionArrows(cell, scooterId)
                    if (arrows.isNotEmpty()) "$scooterId \t$arrows" else scooterId
                }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Cell Items Sorted", text))
                Toast.makeText(context, "Скопировано ${cell.items.size} номеров (сортировка)", Toast.LENGTH_SHORT).show(); dismiss()
            }
            Spacer(modifier = Modifier.height(8.dp))
            ExportOptionButton(icon = Icons.Default.TableChart, title = "Excel — без сортировки", subtitle = "Порядок как в ячейке", containerColor = StardustPrimary.copy(alpha = 0.12f), contentColor = StardustPrimary) { storageExportManager.exportCellAsIs(cell); dismiss() }
            Spacer(modifier = Modifier.height(8.dp))
            ExportOptionButton(icon = Icons.Default.Print, title = "Excel — на печать", subtitle = "Столбцами по 50, отсортировано", containerColor = StardustSuccess.copy(alpha = 0.12f), contentColor = StardustSuccess) { storageExportManager.exportCellForPrinting(cell); dismiss() }
        }
    }
}

/**
 * Возвращает строку со стрелочками направления для кода самоката.
 * Пример: "↑←" для UP и LEFT, "↓" для DOWN, "" если направлений нет.
 */
private fun getDirectionArrows(cell: StorageCell, scooterId: String): String {
    val directions = cell.stickerDirections?.get(scooterId) ?: return ""
    return directions.joinToString("") { dir ->
        when (dir.uppercase()) {
            "UP" -> "\u2191"      // ↑
            "DOWN" -> "\u2193"    // ↓
            "LEFT" -> "\u2190"    // ←
            "RIGHT" -> "\u2192"   // →
            else -> ""
        }
    }
}

@Composable
fun ExportOptionButton(icon: ImageVector, title: String, subtitle: String, containerColor: Color, contentColor: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = contentColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = contentColor.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = contentColor.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        }
    }
}