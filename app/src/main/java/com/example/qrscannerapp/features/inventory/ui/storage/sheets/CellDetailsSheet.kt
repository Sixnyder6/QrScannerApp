package com.example.qrscannerapp.features.inventory.ui.storage.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.*
import com.example.qrscannerapp.features.inventory.data.export.StorageExportManager
import com.example.qrscannerapp.features.inventory.ui.distribution.getColorByProgress
import com.example.qrscannerapp.features.inventory.ui.storage.components.HighlightedText
import com.example.qrscannerapp.features.inventory.ui.storage.dialogs.StorageConfirmDialog
import com.example.qrscannerapp.features.inventory.ui.storage.terminal.CellOperationsTerminal
import com.example.qrscannerapp.features.inventory.ui.storage.utils.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellDetailsSheet(
    cell: StorageCell,
    sheetState: SheetState,
    viewModel: QrScannerViewModel,
    storageExportManager: StorageExportManager,
    searchQuery: String,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isDescriptionExpanded by remember { mutableStateOf(true) }
    val rotationAngle by animateFloatAsState(if (isDescriptionExpanded) 180f else 0f, label = "rotation", animationSpec = tween(300))
    var showExportSheet by remember { mutableStateOf(false) }
    val exportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var scooterToRemove by remember { mutableStateOf<String?>(null) }
    var showTerminal by remember { mutableStateOf(false) }
    val displayedItems = remember(cell.items, searchQuery) {
        if (searchQuery.isNotBlank()) {
            val (matches, nonMatches) = cell.items.partition { it.contains(searchQuery, ignoreCase = true) }
            matches.sorted() + nonMatches
        } else {
            cell.items.reversed()
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = StardustModalBg, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(Modifier.padding(bottom = 32.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Содержимое", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StardustTextPrimary)
                    Text(cell.name, fontSize = 14.sp, color = StardustTextSecondary)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = StardustGlassBg) {
                    Text("${cell.items.size}/${cell.capacity}", color = getColorByProgress(if (cell.capacity > 0) cell.items.size.toFloat() / cell.capacity else 0f), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = StardustGlassBg), border = BorderStroke(1.dp, StardustPrimary.copy(alpha = 0.1f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(StardustPrimary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Text(getInitials(cell.createdByName), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StardustPrimary)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(cell.createdByName ?: "—", fontSize = 14.sp, color = StardustTextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(getRoleLabel(cell.createdByRole), fontSize = 10.sp, color = getRoleColor(cell.createdByRole), fontWeight = FontWeight.Medium)
                        }
                    }
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(StardustItemBg))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(StardustSecondary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = StardustSecondary, modifier = Modifier.size(15.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Создана", fontSize = 10.sp, color = StardustTextSecondary.copy(alpha = 0.6f))
                            Text(formatAbsoluteDate(cell.createdAt), fontSize = 14.sp, color = StardustTextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Card(modifier = Modifier.padding(horizontal = 16.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = StardustItemBg)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().clickable { isDescriptionExpanded = !isDescriptionExpanded }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Описание", color = StardustTextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        IconButton(onClick = onEditClick, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Edit, contentDescription = null, tint = StardustTextSecondary) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = StardustTextSecondary, modifier = Modifier.rotate(rotationAngle))
                    }
                    AnimatedVisibility(visible = isDescriptionExpanded) {
                        Text(cell.description, color = StardustTextSecondary, modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), fontSize = 14.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabBgItems by animateColorAsState(if (!showTerminal) StardustPrimary.copy(alpha = 0.15f) else Color.Transparent, tween(200), label = "tab_items")
                val tabBgTerminal by animateColorAsState(if (showTerminal) TerminalGreen.copy(alpha = 0.15f) else Color.Transparent, tween(200), label = "tab_terminal")
                Surface(onClick = { showTerminal = false }, shape = RoundedCornerShape(10.dp), color = tabBgItems, border = BorderStroke(1.dp, if (!showTerminal) StardustPrimary.copy(alpha = 0.3f) else StardustItemBg), modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, tint = if (!showTerminal) StardustPrimary else StardustTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Список (${cell.items.size})", fontSize = 13.sp, color = if (!showTerminal) StardustPrimary else StardustTextSecondary, fontWeight = if (!showTerminal) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
                Surface(onClick = { showTerminal = true }, shape = RoundedCornerShape(10.dp), color = tabBgTerminal, border = BorderStroke(1.dp, if (showTerminal) TerminalGreen.copy(alpha = 0.3f) else StardustItemBg), modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = if (showTerminal) TerminalGreen else StardustTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Операции (${cell.operations.size})", fontSize = 13.sp, color = if (showTerminal) TerminalGreen else StardustTextSecondary, fontWeight = if (showTerminal) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Crossfade(targetState = showTerminal, label = "content_switch") { isTerminal ->
                if (isTerminal) {
                    CellOperationsTerminal(operations = cell.operations, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.55f).padding(horizontal = 16.dp))
                } else {
                    if (cell.items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Inbox, contentDescription = null, tint = StardustTextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Ячейка пуста", color = StardustTextSecondary, fontSize = 15.sp)
                            }
                        }
                    } else {
                        Column {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                Text(if (searchQuery.isNotBlank()) "Результаты поиска:" else "Список (сначала новые):", fontSize = 11.sp, color = StardustTextSecondary.copy(alpha = 0.6f))
                            }
                            LazyColumn(modifier = Modifier.fillMaxHeight(0.55f)) {
                                items(displayedItems, key = { it }) { scooterId ->
                                    val itemDirs = cell.stickerDirections?.get(scooterId)
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.QrCode2, contentDescription = null, tint = if (scooterId.contains(searchQuery, true) && searchQuery.isNotBlank()) StardustPrimary else StardustTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        HighlightedText(text = scooterId, highlight = searchQuery, color = StardustTextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                        if (!itemDirs.isNullOrEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(end = 4.dp)) {
                                                itemDirs.forEach { dirName ->
                                                    val dir = runCatching { com.example.qrscannerapp.features.scanner.domain.model.StickerDirection.valueOf(dirName) }.getOrNull()
                                                    if (dir != null) com.example.qrscannerapp.features.scanner.ui.components.DirectionBadge(dir)
                                                }
                                            }
                                        }
                                        IconButton(onClick = { scooterToRemove = scooterId }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Default.Clear, contentDescription = null, tint = StardustError.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { showExportSheet = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = StardustSuccess), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Экспорт", fontSize = 14.sp)
                }
                Button(onClick = onDeleteClick, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = StardustError.copy(alpha = 0.25f), contentColor = StardustError), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Удалить", fontSize = 14.sp)
                }
            }
        }
    }
    if (scooterToRemove != null) {
        val scooterId = scooterToRemove!!
        StorageConfirmDialog(
            title       = "Удалить самокат?",
            message     = "Самокат $scooterId будет удалён из ${cell.name}.",
            confirmText = "Удалить",
            accentColor = StardustError,
            onDismiss   = { scooterToRemove = null },
            onConfirm   = { viewModel.removeItemFromCell(cell, scooterId); scooterToRemove = null }
        )
    }
    if (showExportSheet) {
        CellExportSheet(cell = cell, sheetState = exportSheetState, storageExportManager = storageExportManager, onDismiss = {
            scope.launch { exportSheetState.hide() }.invokeOnCompletion { showExportSheet = false }
        })
    }
}
