// Полная и обновленная версия файла: features/inventory/ui/distribution/PalletDistributionScreen.kt

package com.example.qrscannerapp.features.inventory.ui.distribution

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.QrScannerViewModel
import com.example.qrscannerapp.StardustError
import com.example.qrscannerapp.StardustGlassBg
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustModalBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSecondary
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.features.inventory.data.export.PalletExportManager
import com.example.qrscannerapp.features.inventory.data.export.PalletSummaryPdfGenerator
import com.example.qrscannerapp.features.inventory.domain.model.StoragePallet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalletDistributionScreen(
    viewModel: QrScannerViewModel,
    authManager: AuthManager,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.palletDistributionState.collectAsState()
    val highlightedPalletId by viewModel.highlightedPalletId.collectAsState()
    val analysisResults by viewModel.palletAnalysisResults.collectAsState()
    val showAnalysisReport by viewModel.showAnalysisReport.collectAsState()
    val distributionReport by viewModel.distributionReport.collectAsState()

    val batteryCodesInScanner = viewModel.batteryCodes

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val exportManager = remember { PalletExportManager(context) }

    var selectedPalletForDetails by remember { mutableStateOf<StoragePallet?>(null) }
    var showErrorsOnlyInDetails by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var palletToDelete by remember { mutableStateOf<StoragePallet?>(null) }
    var showExportOptions by remember { mutableStateOf(false) }
    var showClearLogDialog by remember { mutableStateOf(false) }
    var showBufferDialog by remember { mutableStateOf(false) }

    val authState by authManager.authState.collectAsState()
    val isAdmin = authState.isAdmin
    val currentUser = if (isAdmin) "Администратор" else "Сотрудник"

    var palletToEditManufacturer by remember { mutableStateOf<StoragePallet?>(null) }
    val gridState = rememberLazyGridState()

    // Лаунчеры
    val saveExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        exportManager.writeMasterListToStream(uiState.pallets, outputStream)
                    }
                    Toast.makeText(context, "Excel сохранен!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        PalletSummaryPdfGenerator.writePdfToStream(context, uiState.pallets, currentUser, outputStream)
                    }
                    Toast.makeText(context, "PDF сохранен!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onNavigateToPalletDistribution()
    }

    LaunchedEffect(highlightedPalletId, uiState.pallets) {
        highlightedPalletId?.let { id ->
            val sortedPallets = uiState.pallets.sortedByDescending { it.palletNumber }
            val index = sortedPallets.indexOfFirst { it.id == id }
            if (index != -1) {
                launch { gridState.animateScrollToItem(index + 1) }
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    AppBackground {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                if (uiState.isLoading && uiState.pallets.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. СВОДКА (Дашборд)
                        val totalInPallets = uiState.pallets.sumOf { it.items.size }
                        val totalOnStock = totalInPallets + uiState.undistributedItemCount
                        val fujianCount = uiState.pallets.filter { it.manufacturer == "FUJIAN" }.sumOf { it.items.size }
                        val bydCount = uiState.pallets.filter { it.manufacturer == "BYD" }.sumOf { it.items.size }

                        InventorySummaryCard(
                            totalCount = totalOnStock,
                            undistributedCount = uiState.undistributedItemCount,
                            todayCount = 0,
                            fujianCount = fujianCount,
                            bydCount = bydCount,
                            onBufferClick = { showBufferDialog = true }
                        )

                        // 2. ПАНЕЛЬ ИНСТРУМЕНТОВ (Вместо плавающих кнопок)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Кнопка "Анализ" (Самая важная)
                            OutlinedButton(
                                onClick = { viewModel.runPalletAnalysis() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = StardustGlassBg,
                                    contentColor = StardustSecondary
                                ),
                                border = null
                            ) {
                                Icon(Icons.Default.FactCheck, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Анализ", fontWeight = FontWeight.Bold)
                            }

                            // Кнопка "Обновить" (Квадратная)
                            FilledIconButton(
                                onClick = {
                                    viewModel.onNavigateToPalletDistribution()
                                    Toast.makeText(context, "Обновлено", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = StardustGlassBg,
                                    contentColor = StardustTextPrimary
                                )
                            ) {
                                Icon(Icons.Default.Refresh, "Обновить")
                            }

                            // Кнопка "Экспорт" (Цветная, Акцентная)
                            Button(
                                onClick = {
                                    if (uiState.pallets.any { it.items.isNotEmpty() }) showExportOptions = true
                                    else Toast.makeText(context, "Нет данных", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StardustPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Экспорт", fontWeight = FontWeight.Bold)
                            }
                        }

                        // 3. ЛОГ ОПЕРАЦИЙ
                        Box(modifier = Modifier.fillMaxWidth()) {
                            PalletActivityLogView(
                                logEntries = uiState.activityLog,
                                isAdmin = isAdmin,
                                onClearLogClick = { showClearLogDialog = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 4. СЕТКА ПАЛЕТ
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = 165.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.height(500.dp)
                        ) {
                            item { NewPalletTile(onClick = { viewModel.createNewPallet() }) }

                            items(
                                items = uiState.pallets.sortedByDescending { it.palletNumber },
                                key = { it.id }
                            ) { pallet ->
                                val isHighlighted = pallet.id == highlightedPalletId
                                val errorItems = analysisResults[pallet.id] ?: emptyList()
                                val errorCount = errorItems.size

                                Box(modifier = Modifier) {
                                    PalletTile(
                                        pallet = pallet,
                                        isHighlighted = isHighlighted,
                                        onClick = {
                                            if (uiState.undistributedItemCount > 0) {
                                                viewModel.distributeBatteriesToPallet(pallet)
                                            }
                                        },
                                        onLongClick = {
                                            selectedPalletForDetails = pallet
                                            showErrorsOnlyInDetails = false
                                            scope.launch { bottomSheetState.show() }
                                        },
                                        onDeleteClick = { palletToDelete = pallet },
                                        onEditManufacturerClick = { palletToEditManufacturer = pallet }
                                    )

                                    if (errorCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 8.dp, end = 8.dp)
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(StardustError)
                                                .zIndex(1f)
                                                .clickable {
                                                    selectedPalletForDetails = pallet
                                                    showErrorsOnlyInDetails = true
                                                    scope.launch { bottomSheetState.show() }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("$errorCount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.isDistributing || (uiState.isLoading && uiState.pallets.isNotEmpty())) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = StardustPrimary)
                    }
                }
            }
        }
    }

    // --- ДИАЛОГИ ---

    if (distributionReport != null) {
        DistributionReportDialog(
            report = distributionReport!!,
            onDismiss = { viewModel.dismissDistributionReport() }
        )
    }

    if (showBufferDialog) {
        BufferDetailsDialog(
            items = batteryCodesInScanner.map { it.code },
            onDismiss = { showBufferDialog = false },
            onDeleteItem = { code ->
                val itemToDelete = batteryCodesInScanner.find { it.code == code }
                if (itemToDelete != null) viewModel.removeCode(itemToDelete)
            }
        )
    }

    if (showAnalysisReport) {
        AnalysisResultDialog(
            pallets = uiState.pallets,
            analysisResults = analysisResults,
            onDismiss = { viewModel.dismissAnalysisReport() },
            onPalletClick = { pallet ->
                viewModel.dismissAnalysisReport()
                selectedPalletForDetails = pallet
                showErrorsOnlyInDetails = true
                scope.launch { bottomSheetState.show() }
            }
        )
    }

    if (selectedPalletForDetails != null) {
        val errorItems = analysisResults[selectedPalletForDetails!!.id] ?: emptyList()
        PalletDetailsSheet(
            pallet = selectedPalletForDetails!!,
            sheetState = bottomSheetState,
            viewModel = viewModel,
            userName = currentUser,
            errorItems = errorItems,
            initialFilterErrors = showErrorsOnlyInDetails,
            onDismiss = { selectedPalletForDetails = null }
        )
    }

    if (palletToDelete != null) {
        PalletDeleteDialog(
            pallet = palletToDelete!!,
            onDismiss = { palletToDelete = null },
            onConfirmDelete = { viewModel.deletePallet(it); palletToDelete = null }
        )
    }

    if (showClearLogDialog) {
        AlertDialog(
            onDismissRequest = { showClearLogDialog = false },
            title = { Text("Очистить историю?") },
            text = { Text("Вы уверены? Это действие необратимо.") },
            confirmButton = { Button(onClick = { viewModel.clearPalletActivityLog(); showClearLogDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = StardustError)) { Text("Очистить") } },
            dismissButton = { Button(onClick = { showClearLogDialog = false }) { Text("Отмена") } },
            containerColor = StardustModalBg, titleContentColor = StardustTextPrimary, textContentColor = StardustTextSecondary
        )
    }

    if (palletToEditManufacturer != null) {
        ManufacturerSelectionDialog(
            pallet = palletToEditManufacturer!!,
            onDismiss = { palletToEditManufacturer = null },
            onManufacturerSelected = { pallet, manufacturer ->
                viewModel.setPalletManufacturer(pallet.id, manufacturer)
                palletToEditManufacturer = null
            }
        )
    }

    if (showExportOptions) {
        ExportOptionsDialog(
            onDismiss = { showExportOptions = false },
            onShareExcel = { showExportOptions = false; exportManager.shareMasterList(uiState.pallets) },
            onSaveExcel = { showExportOptions = false; saveExcelLauncher.launch("master_pallet_export.xlsx") },
            onSharePdf = { showExportOptions = false; PalletSummaryPdfGenerator.generateAndShare(context, uiState.pallets, currentUser) },
            onSavePdf = { showExportOptions = false; savePdfLauncher.launch("Svodniy_Otchet.pdf") }
        )
    }
}

@Composable
fun AnalysisResultDialog(
    pallets: List<StoragePallet>,
    analysisResults: Map<String, List<String>>,
    onDismiss: () -> Unit,
    onPalletClick: (StoragePallet) -> Unit
) {
    val problemPallets = pallets.filter { analysisResults.containsKey(it.id) }
    val totalErrors = analysisResults.values.sumOf { it.size }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StardustModalBg,
        titleContentColor = StardustTextPrimary,
        textContentColor = StardustTextSecondary,
        title = {
            Column {
                Text("Результаты анализа", fontWeight = FontWeight.Bold)
                Text("Найдено ошибок: $totalErrors", style = MaterialTheme.typography.bodyMedium, color = StardustError)
            }
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(problemPallets) { pallet ->
                    val errorCount = analysisResults[pallet.id]?.size ?: 0
                    Card(colors = CardDefaults.cardColors(containerColor = StardustSecondary.copy(alpha = 0.1f)), modifier = Modifier.fillMaxWidth().clickable { onPalletClick(pallet) }) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Палет №${pallet.palletNumber}", fontWeight = FontWeight.Bold, color = StardustTextPrimary)
                                Text("Производитель: ${pallet.manufacturer ?: "Н/Д"}", style = MaterialTheme.typography.bodySmall, color = StardustTextSecondary)
                            }
                            Box(modifier = Modifier.background(StardustError.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("$errorCount чужих", color = StardustError, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть", color = StardustTextPrimary) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PalletDetailsSheet(
    pallet: StoragePallet,
    sheetState: SheetState,
    viewModel: QrScannerViewModel,
    userName: String,
    errorItems: List<String> = emptyList(),
    initialFilterErrors: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isExportMenuExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showErrorsOnly by remember { mutableStateOf(initialFilterErrors) }

    val filteredItems = remember(pallet.items, searchQuery, showErrorsOnly, errorItems) {
        var items = pallet.items.asReversed()
        if (showErrorsOnly) items = items.filter { it in errorItems }
        if (searchQuery.isNotEmpty()) items = items.filter { it.contains(searchQuery, ignoreCase = true) }
        items
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = StardustModalBg, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Scaffold(containerColor = Color.Transparent, snackbarHost = { SnackbarHost(snackbarHostState) }, bottomBar = { Spacer(modifier = Modifier.height(32.dp)) }) { innerPadding ->
            Column(Modifier.padding(innerPadding).verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Палет №${pallet.palletNumber}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = StardustTextPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            pallet.manufacturer?.let {
                                Text("Производитель: $it", style = MaterialTheme.typography.bodyMedium, color = StardustTextSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.size(4.dp).background(StardustTextSecondary, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Всего: ${pallet.items.size} шт.", style = MaterialTheme.typography.bodyMedium, color = StardustPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box {
                        IconButton(onClick = { isExportMenuExpanded = true }, modifier = Modifier.background(StardustGlassBg, CircleShape)) { Icon(Icons.Default.Share, "Экспорт", tint = StardustPrimary) }
                        DropdownMenu(expanded = isExportMenuExpanded, onDismissRequest = { isExportMenuExpanded = false }, modifier = Modifier.background(StardustGlassBg)) {
                            DropdownMenuItem(text = { Text("Excel", color = StardustTextPrimary) }, leadingIcon = { Icon(Icons.Default.TableChart, null, tint = StardustSecondary) }, onClick = { isExportMenuExpanded = false; exportPalletToExcel(context, pallet) })
                            DropdownMenuItem(text = { Text("PDF", color = StardustTextPrimary) }, leadingIcon = { Icon(Icons.Default.Description, null, tint = StardustPrimary) }, onClick = { isExportMenuExpanded = false; PalletSummaryPdfGenerator.generateAndShare(context, listOf(pallet), userName) })
                        }
                    }
                }
                Column(Modifier.padding(horizontal = 16.dp)) {
                    if (errorItems.isNotEmpty()) {
                        FilterChip(selected = showErrorsOnly, onClick = { showErrorsOnly = !showErrorsOnly }, label = { Text("Только ошибки (${errorItems.size})") }, leadingIcon = { if (showErrorsOnly) Icon(Icons.Default.FactCheck, null, modifier = Modifier.size(16.dp)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StardustError.copy(alpha = 0.2f), selectedLabelColor = StardustError, labelColor = StardustTextSecondary))
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Поиск по ID...", color = StardustTextSecondary.copy(alpha = 0.5f)) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = StardustTextSecondary) }, trailingIcon = if (searchQuery.isNotEmpty()) { { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null, tint = StardustTextSecondary) } } } else null, singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = StardustPrimary, unfocusedBorderColor = StardustSecondary.copy(alpha = 0.3f), cursorColor = StardustPrimary, focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextPrimary))
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (filteredItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text(if (showErrorsOnly) "Ошибок нет" else "Ничего не найдено", color = StardustTextSecondary) }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        itemsIndexed(filteredItems) { index, batteryId ->
                            val displayIndex = if (searchQuery.isEmpty() && !showErrorsOnly) pallet.items.size - index else index + 1
                            val backgroundColor = if (index % 2 == 0) Color.Transparent else StardustTextSecondary.copy(alpha = 0.05f)
                            val isErrorItem = batteryId in errorItems
                            val textColor = if (isErrorItem) StardustError else StardustTextPrimary
                            val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) { viewModel.removeItemFromPallet(pallet.id, batteryId); scope.launch { val res = snackbarHostState.showSnackbar("АКБ удален", "ВЕРНУТЬ"); if (res == SnackbarResult.ActionPerformed) viewModel.distributeSpecificItemToPallet(pallet, batteryId) }; true } else false
                            })
                            SwipeToDismissBox(state = dismissState, backgroundContent = {
                                val color by animateColorAsState(if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) StardustError else Color.Transparent)
                                Box(modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 24.dp), contentAlignment = Alignment.CenterEnd) { Icon(Icons.Default.Delete, "Удалить", tint = Color.White) }
                            }, content = {
                                Row(modifier = Modifier.fillMaxWidth().background(StardustModalBg).background(backgroundColor).combinedClickable(onClick = {}, onLongClick = { clipboardManager.setText(AnnotatedString(batteryId)); Toast.makeText(context, "ID скопирован", Toast.LENGTH_SHORT).show() }).padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("#$displayIndex", color = StardustTextSecondary, fontSize = 12.sp, modifier = Modifier.width(40.dp))
                                    Column(modifier = Modifier.weight(1f)) { Text(batteryId, color = textColor, fontSize = 16.sp, fontWeight = if(isErrorItem) FontWeight.Bold else FontWeight.Medium); if (isErrorItem) Text("Чужой АКБ", color = StardustError, fontSize = 10.sp) }
                                    if (isErrorItem) Icon(Icons.Default.Warning, null, tint = StardustError, modifier = Modifier.size(20.dp).padding(end = 8.dp))
                                    IconButton(onClick = { viewModel.removeItemFromPallet(pallet.id, batteryId); scope.launch { val res = snackbarHostState.showSnackbar("АКБ удален", "ВЕРНУТЬ"); if (res == SnackbarResult.ActionPerformed) viewModel.distributeSpecificItemToPallet(pallet, batteryId) } }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = StardustError.copy(alpha = 0.7f)) }
                                }
                                HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.1f), thickness = 0.5.dp)
                            })
                        }
                    }
                }
            }
        }
    }
}

fun exportPalletToExcel(context: Context, pallet: StoragePallet) {
    try {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Палет №${pallet.palletNumber}")
        var rowIndex = 0
        pallet.manufacturer?.let { val manufacturerRow = sheet.createRow(rowIndex++); manufacturerRow.createCell(0).setCellValue("Производитель: $it"); rowIndex++ }
        val headerRow = sheet.createRow(rowIndex++); headerRow.createCell(0).setCellValue("ID Аккумулятора")
        pallet.items.forEachIndexed { index, batteryId -> val row = sheet.createRow(rowIndex + index); row.createCell(0).setCellValue(batteryId) }
        sheet.autoSizeColumn(0)
        val fileName = "pallet_${pallet.palletNumber}_export.xlsx"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        context.startActivity(Intent.createChooser(shareIntent, "Экспорт палета"))
    } catch (e: Exception) { Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show(); e.printStackTrace() }
}