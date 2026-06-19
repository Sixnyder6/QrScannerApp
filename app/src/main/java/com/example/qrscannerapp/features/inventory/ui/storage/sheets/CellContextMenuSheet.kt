package com.example.qrscannerapp.features.inventory.ui.storage.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.*
import com.example.qrscannerapp.features.inventory.ui.distribution.getColorByProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellContextMenuSheet(
    cell: StorageCell,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onBulkAdd: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = StardustModalBg, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(cell.name, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(cell.description, color = StardustTextSecondary, fontSize = 13.sp)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = StardustGlassBg) {
                    Text("${cell.items.size}/${cell.capacity}", color = getColorByProgress(if (cell.capacity > 0) cell.items.size.toFloat() / cell.capacity else 0f), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
            HorizontalDivider(color = StardustItemBg, modifier = Modifier.padding(bottom = 4.dp))
            ContextMenuItem(icon = Icons.AutoMirrored.Filled.OpenInNew, label = "Открыть содержимое", tint = StardustTextPrimary, onClick = onOpen)
            ContextMenuItem(icon = Icons.Default.Edit, label = "Редактировать ячейку", tint = StardustPrimary, onClick = onEdit)
            ContextMenuItem(icon = Icons.AutoMirrored.Filled.PlaylistAdd, label = "Добавить номера списком", tint = StardustSuccess, onClick = onBulkAdd)
            ContextMenuItem(icon = Icons.Default.CheckBox, label = "Выбрать для удаления", tint = StardustSecondary, onClick = onSelect)
            HorizontalDivider(color = StardustItemBg, modifier = Modifier.padding(vertical = 4.dp))
            ContextMenuItem(icon = Icons.Default.Delete, label = "Удалить ячейку", tint = StardustError, onClick = onDelete)
        }
    }
}

@Composable
private fun ContextMenuItem(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, color = tint, fontSize = 15.sp, fontWeight = if (tint == StardustError) FontWeight.SemiBold else FontWeight.Normal)
    }
}
