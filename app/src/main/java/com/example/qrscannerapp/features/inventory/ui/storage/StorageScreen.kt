// Полная, окончательно исправленная версия файла: features/inventory/ui/storage/StorageScreen.kt

package com.example.qrscannerapp.features.inventory.ui.storage

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.FileProvider
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.QrScannerViewModel
import com.example.qrscannerapp.StardustError
import com.example.qrscannerapp.StardustGlassBg
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustModalBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSecondary
import com.example.qrscannerapp.StardustSuccess
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.StorageActivityLogEntry
import com.example.qrscannerapp.StorageCell
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.features.inventory.data.export.StorageExportManager
import com.example.qrscannerapp.features.inventory.ui.distribution.getColorByProgress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.TextUnit


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    viewModel: QrScannerViewModel,
    authManager: AuthManager,
    onNavigateBack: () -> Unit,
    setTopBarActions: (@Composable RowScope.() -> Unit) -> Unit
) {
    val uiState by viewModel.storageState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedCellForDetails by remember { mutableStateOf<StorageCell?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var cellToEdit by remember { mutableStateOf<StorageCell?>(null) }
    var cellToDelete by remember { mutableStateOf<StorageCell?>(null) }

    // --- V НОВЫЙ БЛОК: Состояние для диалога массового добавления V ---
    var cellForBulkAdd by remember { mutableStateOf<StorageCell?>(null) }
    // --- ^ КОНЕЦ НОВОГО БЛОКА ^ ---

    val storageExportManager = remember { StorageExportManager(context) }
    val authState by authManager.authState.collectAsState()
    var showClearLogDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }

    val filteredCells = remember(searchQuery, uiState.cells) {
        if (searchQuery.isBlank()) {
            uiState.cells
        } else {
            val query = searchQuery.trim()
            uiState.cells.filter { cell ->
                cell.name.contains(query, ignoreCase = true) ||
                        cell.description.contains(query, ignoreCase = true) ||
                        cell.items.any { scooterId -> scooterId.contains(query, ignoreCase = true) }
            }
        }
    }

    LaunchedEffect(Unit) {
        setTopBarActions {
            IconButton(onClick = {
                storageExportManager.exportAllCellsToExcel(uiState.cells)
            }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Экспортировать все ячейки"
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadStorageCells()
    }

    LaunchedEffect(uiState.distributionResult) {
        uiState.distributionResult?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearStorageDistributionResult()
            }
        }
    }

    AppBackground {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = StardustPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Создать ячейку")
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    uiState.isLoading && uiState.cells.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.error != null -> {
                        Text(
                            text = uiState.error!!,
                            color = StardustError,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            StorageActivityLogView(
                                logEntries = uiState.activityLog,
                                isAdmin = authState.isAdmin,
                                onClearLogClick = { showClearLogDialog = true }
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Поиск (название, описание, номер)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Очистить поиск")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    focusedContainerColor = StardustGlassBg,
                                    unfocusedContainerColor = StardustGlassBg,
                                    focusedTextColor = StardustTextPrimary,
                                    unfocusedTextColor = StardustTextPrimary,
                                    focusedLeadingIconColor = StardustTextSecondary,
                                    unfocusedLeadingIconColor = StardustTextSecondary,
                                    focusedTrailingIconColor = StardustTextSecondary,
                                    unfocusedTrailingIconColor = StardustTextSecondary,
                                    focusedLabelColor = StardustTextSecondary,
                                    unfocusedLabelColor = StardustTextSecondary,
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val undistributedCount by remember {
                                derivedStateOf { viewModel.scooterCodes.size }
                            }

                            Text(
                                "Не распределено: $undistributedCount шт.",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = StardustTextPrimary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            if (filteredCells.isEmpty() && uiState.cells.isNotEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "По вашему запросу ничего не найдено",
                                        color = StardustTextSecondary,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 32.dp)
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(filteredCells, key = { it.id }) { cell ->
                                        StorageCellTile(
                                            cell = cell,
                                            searchQuery = searchQuery,
                                            // <--- ИЗМЕНЕНИЕ №1: ВОЗВРАЩАЕМ СТАРУЮ ЛОГИКУ НА КЛИК
                                            onClick = {
                                                if (undistributedCount > 0) {
                                                    viewModel.distributeScootersToCell(cell)
                                                } else {
                                                    Toast.makeText(context, "Нет самокатов для распределения.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onLongClick = {
                                                selectedCellForDetails = cell
                                            },
                                            onEditClick = {
                                                cellToEdit = cell
                                            },
                                            // <--- ИЗМЕНЕНИЕ №2: ДОБАВЛЯЕМ НОВЫЙ ОБРАБОТЧИК
                                            onBulkAddClick = {
                                                cellForBulkAdd = cell
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (uiState.isLoading && uiState.cells.isNotEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCellDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { description, capacity ->
                viewModel.createNewCell(description, capacity)
                showCreateDialog = false
            }
        )
    }

    if (cellToEdit != null) {
        EditCellDialog(
            cell = cellToEdit!!,
            onDismiss = { cellToEdit = null },
            onSave = { cellId, newDescription, newCapacity ->
                viewModel.updateCell(cellId, newDescription, newCapacity)
                cellToEdit = null
            }
        )
    }

    if (cellForBulkAdd != null) {
        BulkAddScootersDialog(
            cell = cellForBulkAdd!!,
            onDismiss = { cellForBulkAdd = null },
            onAdd = { cell, text ->
                viewModel.bulkAddScootersToCell(cell.id, text)
                cellForBulkAdd = null
            }
        )
    }

    val currentSelectedCell = uiState.cells.find { it.id == selectedCellForDetails?.id }

    if (currentSelectedCell != null) {
        CellDetailsSheet(
            cell = currentSelectedCell,
            sheetState = bottomSheetState,
            viewModel = viewModel,
            searchQuery = searchQuery,
            onDismiss = {
                scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                    selectedCellForDetails = null
                }
            },
            onEditClick = {
                scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                    if (!bottomSheetState.isVisible) {
                        cellToEdit = currentSelectedCell
                        selectedCellForDetails = null
                    }
                }
            },
            onDeleteClick = {
                scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                    if (!bottomSheetState.isVisible) {
                        cellToDelete = currentSelectedCell
                        selectedCellForDetails = null
                    }
                }
            }
        )
    }

    LaunchedEffect(selectedCellForDetails) {
        if (selectedCellForDetails != null) {
            scope.launch {
                bottomSheetState.show()
            }
        }
    }

    if (cellToDelete != null) {
        DeleteCellDialog(
            cell = cellToDelete!!,
            onDismiss = { cellToDelete = null },
            onConfirm = {
                viewModel.deleteCell(it)
                cellToDelete = null
            }
        )
    }

    if (showClearLogDialog) {
        ClearLogDialog(
            onDismiss = { showClearLogDialog = false },
            onConfirm = {
                viewModel.clearStorageActivityLog()
                showClearLogDialog = false
            }
        )
    }
}

@Composable
fun BulkAddScootersDialog(
    cell: StorageCell,
    onDismiss: () -> Unit,
    onAdd: (StorageCell, String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StardustModalBg)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Добавить номера в ${cell.name}", style = MaterialTheme.typography.titleLarge, color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Вставьте скопированный столбец с номерами самокатов. Каждый номер с новой строки.", style = MaterialTheme.typography.bodyMedium, color = StardustTextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth().height(200.dp), label = { Text("Номера самокатов") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextSecondary, focusedContainerColor = StardustGlassBg,
                        unfocusedContainerColor = StardustGlassBg, focusedIndicatorColor = StardustPrimary, unfocusedIndicatorColor = StardustItemBg,
                        focusedLabelColor = StardustPrimary, unfocusedLabelColor = StardustTextSecondary,
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Отмена", color = StardustTextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onAdd(cell, text) }, enabled = text.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) {
                        Text("Добавить")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StorageCellTile(
    cell: StorageCell,
    searchQuery: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditClick: () -> Unit,
    // <--- ИЗМЕНЕНИЕ №3: ДОБАВЛЯЕМ НОВЫЙ ПАРАМЕТР В ФУНКЦИЮ
    onBulkAddClick: () -> Unit
) {
    val progress = if (cell.capacity > 0) cell.items.size.toFloat() / cell.capacity.toFloat() else 0f
    val matchingItem = remember(searchQuery, cell.items) {
        if (searchQuery.isNotBlank()) cell.items.firstOrNull { it.contains(searchQuery, ignoreCase = true) } else null
    }
    val isMatchInText = remember(searchQuery, cell.name, cell.description) {
        searchQuery.isNotBlank() && (cell.name.contains(searchQuery, ignoreCase = true) || cell.description.contains(searchQuery, ignoreCase = true))
    }
    val showMatchingItem = matchingItem != null && !isMatchInText

    Card(
        modifier = Modifier.aspectRatio(1f).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HighlightedText(
                        text = cell.name, highlight = searchQuery, fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, color = StardustTextPrimary, modifier = Modifier.weight(1f)
                    )
                    // <--- ИЗМЕНЕНИЕ №4: ДОБАВЛЯЕМ НОВУЮ ИКОНКУ И ОБОРАЧИВАЕМ В ROW
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd, // Иконка "список с плюсом"
                            contentDescription = "Массовое добавление",
                            tint = StardustTextSecondary,
                            modifier = Modifier
                                .size(24.dp) // Чуть больше для удобства нажатия
                                .clickable(onClick = onBulkAddClick)
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Edit, contentDescription = "Редактировать", tint = StardustTextSecondary,
                            modifier = Modifier.size(20.dp).clickable(onClick = onEditClick)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                HighlightedText(
                    text = cell.description, highlight = searchQuery, fontSize = 12.sp, color = StardustTextSecondary,
                    maxLines = 1, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis
                )
                AnimatedVisibility(visible = showMatchingItem) {
                    Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.QrCode2, contentDescription = "Найденный номер", tint = StardustSuccess, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        HighlightedText(text = matchingItem ?: "", highlight = searchQuery, fontSize = 12.sp, color = StardustTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("${cell.items.size} / ${cell.capacity}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = StardustTextPrimary)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = getColorByProgress(progress),
                    trackColor = StardustItemBg
                )
            }
        }
    }
}

@Composable
fun HighlightedText(
    text: String,
    highlight: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    if (highlight.isBlank()) {
        Text(text = text, modifier = modifier, color = color, fontSize = fontSize, fontWeight = fontWeight, textAlign = textAlign, maxLines = maxLines, overflow = overflow)
        return
    }
    val annotatedString = buildAnnotatedString {
        var startIndex = 0
        while (startIndex < text.length) {
            val index = text.indexOf(highlight, startIndex, ignoreCase = true)
            if (index == -1) {
                append(text.substring(startIndex))
                break
            }
            append(text.substring(startIndex, index))
            withStyle(style = SpanStyle(background = StardustSuccess.copy(alpha = 0.3f), fontWeight = fontWeight)) {
                append(text.substring(index, index + highlight.length))
            }
            startIndex = index + highlight.length
        }
    }
    Text(text = annotatedString, modifier = modifier, color = color, fontSize = fontSize, fontWeight = fontWeight, textAlign = textAlign, maxLines = maxLines, overflow = overflow)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellDetailsSheet(
    cell: StorageCell,
    sheetState: SheetState,
    viewModel: QrScannerViewModel,
    searchQuery: String,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    var isDescriptionExpanded by remember { mutableStateOf(true) }
    val rotationAngle by animateFloatAsState(targetValue = if (isDescriptionExpanded) 180f else 0f, label = "rotation", animationSpec = tween(300))
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = StardustModalBg) {
        Column(Modifier.padding(bottom = 32.dp)) {
            Text("Содержимое: ${cell.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StardustTextPrimary, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.padding(horizontal = 16.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = StardustItemBg)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isDescriptionExpanded = !isDescriptionExpanded }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Описание", color = StardustTextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = onEditClick, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = StardustTextSecondary) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Развернуть", tint = StardustTextSecondary, modifier = Modifier.rotate(rotationAngle))
                    }
                    AnimatedVisibility(visible = isDescriptionExpanded) {
                        Text(cell.description, color = StardustTextSecondary, modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (cell.items.isEmpty()) {
                Text("В ячейке нет самокатов", color = StardustTextSecondary, modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), textAlign = TextAlign.Center)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                    items(cell.items, key = { it }) { scooterId ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            HighlightedText(text = scooterId, highlight = searchQuery, color = StardustTextPrimary, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.removeItemFromCell(cell, scooterId) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Clear, contentDescription = "Удалить самокат", tint = StardustError) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { exportCellToExcel(context, cell) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) {
                    Icon(Icons.Default.Share, contentDescription = "Экспорт"); Spacer(Modifier.width(8.dp)); Text("Экспорт")
                }
                Button(onClick = onDeleteClick, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = StardustError.copy(alpha = 0.3f), contentColor = StardustError)) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить"); Spacer(Modifier.width(8.dp)); Text("Удалить")
                }
            }
        }
    }
}

@Composable
fun CreateCellDialog(onDismiss: () -> Unit, onCreate: (description: String, capacity: Int) -> Unit) {
    var description by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("600") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StardustModalBg)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Создать новую ячейку", style = MaterialTheme.typography.titleLarge, color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Описание ячейки") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = "Описание") }, singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextSecondary, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = StardustPrimary, unfocusedIndicatorColor = StardustItemBg, focusedLabelColor = StardustPrimary, unfocusedLabelColor = StardustTextSecondary,
                        focusedLeadingIconColor = StardustPrimary, unfocusedLeadingIconColor = StardustTextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = capacity, onValueChange = { if (it.all { char -> char.isDigit() }) capacity = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Ёмкость") },
                    leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = "Ёмкость") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextSecondary, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = StardustPrimary, unfocusedIndicatorColor = StardustItemBg, focusedLabelColor = StardustPrimary, unfocusedLabelColor = StardustTextSecondary,
                        focusedLeadingIconColor = StardustPrimary, unfocusedLeadingIconColor = StardustTextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Отмена", color = StardustTextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onCreate(description, capacity.toIntOrNull() ?: 600) }, enabled = description.isNotBlank() && capacity.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) {
                        Text("Создать")
                    }
                }
            }
        }
    }
}

@Composable
fun EditCellDialog(cell: StorageCell, onDismiss: () -> Unit, onSave: (cellId: String, newDescription: String, newCapacity: Int) -> Unit) {
    var description by remember { mutableStateOf(cell.description) }
    var capacity by remember { mutableStateOf(cell.capacity.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Редактировать ${cell.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Описание ячейки") }, singleLine = true)
                OutlinedTextField(value = capacity, onValueChange = { if (it.all { char -> char.isDigit() }) capacity = it }, label = { Text("Ёмкость") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onSave(cell.id, description, capacity.toIntOrNull() ?: 700) }, enabled = description.isNotBlank() && capacity.isNotBlank()) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        containerColor = StardustModalBg, titleContentColor = StardustTextPrimary, textContentColor = StardustTextSecondary
    )
}

@Composable
fun DeleteCellDialog(cell: StorageCell, onDismiss: () -> Unit, onConfirm: (StorageCell) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Удалить ${cell.name}?") },
        text = { Text("Все ${cell.items.size} самокатов в этой ячейке снова станут доступны для распределения. Это действие необратимо.") },
        confirmButton = { Button(onClick = { onConfirm(cell) }, colors = ButtonDefaults.buttonColors(containerColor = StardustError)) { Text("Удалить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        containerColor = StardustModalBg, titleContentColor = StardustTextPrimary, textContentColor = StardustTextSecondary
    )
}

fun exportCellToExcel(context: Context, cell: StorageCell) {
    if (cell.items.isEmpty()) {
        Toast.makeText(context, "Ячейка пуста, нечего экспортировать.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(cell.name.replace(" ", "_"))
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue(cell.name)
        val descriptionRow = sheet.createRow(1)
        descriptionRow.createCell(0).setCellValue(cell.description)
        val headerStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply { this.bold = true }
            this.setFont(font)
        }
        headerRow.getCell(0).cellStyle = headerStyle
        sheet.setColumnWidth(0, 20 * 256)
        val dataHeaderRow = sheet.createRow(3)
        dataHeaderRow.createCell(0).setCellValue("Номер самоката")
        dataHeaderRow.getCell(0).cellStyle = headerStyle
        cell.items.forEachIndexed { index, scooterId ->
            val row = sheet.createRow(index + 4)
            row.createCell(0).setCellValue(scooterId)
        }
        val sdf = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault())
        val timestamp = sdf.format(Date())
        val fileName = "export_${cell.name.replace(" ", "_")}_$timestamp.xlsx"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Экспорт ячейки ${cell.name}"))
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun StorageActivityLogView(
    logEntries: List<StorageActivityLogEntry>,
    isAdmin: Boolean,
    onClearLogClick: () -> Unit
) {
    Card(modifier = Modifier.heightIn(max = 150.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StardustGlassBg)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("История операций:", fontWeight = FontWeight.Bold, color = StardustTextPrimary, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
                if (isAdmin) {
                    IconButton(onClick = onClearLogClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Очистить историю операций", tint = StardustTextSecondary)
                    }
                }
            }
            if (logEntries.isEmpty()) {
                Text("Нет недавних операций.", color = StardustTextSecondary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(logEntries) { entry -> StorageLogEntryItem(entry = entry) }
                }
            }
        }
    }
}

private fun formatLogTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun StorageLogEntryItem(entry: StorageActivityLogEntry) {
    val actionText = buildAnnotatedString {
        append("${formatLogTime(entry.timestamp)} ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = StardustError)) { append(entry.userName) }
        append(" ${entry.details}")
    }
    val (icon, color) = remember(entry.action) {
        when(entry.action) {
            "CREATED" -> Icons.Default.AddCircle to StardustSuccess
            "DELETED" -> Icons.Default.Delete to StardustError
            "EDITED" -> Icons.Default.Edit to StardustSecondary
            "SCOOTERS_ADDED" -> Icons.Default.Add to StardustSuccess
            "ITEM_REMOVED" -> Icons.Default.Clear to StardustError
            else -> Icons.Default.Info to StardustTextSecondary
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = actionText, color = StardustTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
fun ClearLogDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Очистить историю?") },
        text = { Text("Вы уверены, что хотите полностью очистить историю операций на этом экране? Это действие необратимо.") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = StardustError)) { Text("Очистить") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Отмена") } },
        containerColor = StardustModalBg, titleContentColor = StardustTextPrimary, textContentColor = StardustTextSecondary
    )
}