// Полная версия файла: features/inventory/ui/distribution/PalletDistributionScreen.kt

package com.example.qrscannerapp.features.inventory.ui.distribution

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.QrScannerViewModel
import com.example.qrscannerapp.StardustError
import com.example.qrscannerapp.StardustGlassBg
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
    // Получаем коды из буфера напрямую из ViewModel (это список ScanItem, но мы возьмем коды)
    val batteryCodesInScanner = viewModel.batteryCodes

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val exportManager = remember { PalletExportManager(context) }
    var selectedPalletForDetails by remember { mutableStateOf<StoragePallet?>(null) }
    val bottomSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var palletToDelete by remember { mutableStateOf<StoragePallet?>(null) }
    var showExportOptions by remember { mutableStateOf(false) }
    var showClearLogDialog by remember { mutableStateOf(false) }

    // Состояние для диалога буфера
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

    LaunchedEffect(uiState.distributionResult) {
        uiState.distributionResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDistributionResult()
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
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            PalletActivityLogView(
                                logEntries = uiState.activityLog,
                                isAdmin = isAdmin,
                                onClearLogClick = { showClearLogDialog = true }
                            )
                            PulsatingShareButton(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 4.dp, bottom = 4.dp),
                                enabled = uiState.pallets.isNotEmpty(),
                                onClick = {
                                    if (uiState.pallets.any { it.items.isNotEmpty() }) {
                                        showExportOptions = true
                                    } else {
                                        Toast.makeText(context, "Нет АКБ для экспорта.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val totalInPallets = uiState.pallets.sumOf { it.items.size }
                        val totalOnStock = totalInPallets + uiState.undistributedItemCount
                        val fujianCount = uiState.pallets.filter { it.manufacturer == "FUJIAN" }.sumOf { it.items.size }
                        val bydCount = uiState.pallets.filter { it.manufacturer == "BYD" }.sumOf { it.items.size }

                        InventorySummaryCard(
                            totalCount = totalOnStock,
                            undistributedCount = uiState.undistributedItemCount,
                            fujianCount = fujianCount,
                            bydCount = bydCount,
                            onBufferClick = { showBufferDialog = true } // Открываем диалог
                        )

                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                NewPalletTile(onClick = { viewModel.createNewPallet() })
                            }

                            items(
                                items = uiState.pallets.sortedByDescending { it.palletNumber },
                                key = { it.id }
                            ) { pallet ->
                                val isHighlighted = pallet.id == highlightedPalletId
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
                                        scope.launch { bottomSheetState.show() }
                                    },
                                    onDeleteClick = { palletToDelete = pallet },
                                    onEditManufacturerClick = { palletToEditManufacturer = pallet }
                                )
                            }
                        }
                    }
                }

                if (uiState.isDistributing || (uiState.isLoading && uiState.pallets.isNotEmpty())) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = StardustPrimary)
                    }
                }
            }
        }
    }

    // --- ДИАЛОГИ И ШТОРКИ ---

    if (showBufferDialog) {
        BufferDetailsDialog(
            items = batteryCodesInScanner.map { it.code }, // Передаем список строк
            onDismiss = { showBufferDialog = false },
            onDeleteItem = { code ->
                // Ищем объект ScanItem по коду и удаляем
                val itemToDelete = batteryCodesInScanner.find { it.code == code }
                if (itemToDelete != null) {
                    viewModel.removeCode(itemToDelete)
                    // Обновляем счетчик в UI стейте (хотя он и так реактивен, но для синхронизации с компонентом)
                    // removeCode уже обновляет batteryCodes, а UI перерисуется сам
                }
            }
        )
    }

    if (selectedPalletForDetails != null) {
        PalletDetailsSheet(
            pallet = selectedPalletForDetails!!,
            sheetState = bottomSheetState,
            viewModel = viewModel,
            userName = currentUser,
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
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearPalletActivityLog()
                        showClearLogDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StardustError)
                ) { Text("Очистить") }
            },
            dismissButton = {
                Button(onClick = { showClearLogDialog = false }) { Text("Отмена") }
            },
            containerColor = StardustModalBg,
            titleContentColor = StardustTextPrimary,
            textContentColor = StardustTextSecondary
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
            onShareExcel = {
                showExportOptions = false
                exportManager.shareMasterList(uiState.pallets)
            },
            onSaveExcel = {
                showExportOptions = false
                saveExcelLauncher.launch("master_pallet_export.xlsx")
            },
            onSharePdf = {
                showExportOptions = false
                PalletSummaryPdfGenerator.generateAndShare(context, uiState.pallets, currentUser)
            },
            onSavePdf = {
                showExportOptions = false
                savePdfLauncher.launch("Svodniy_Otchet.pdf")
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PalletDetailsSheet(
    pallet: StoragePallet,
    sheetState: SheetState,
    viewModel: QrScannerViewModel,
    userName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // Снэкбар внутри шторки
    val snackbarHostState = remember { SnackbarHostState() }

    var isExportMenuExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(pallet.items, searchQuery) {
        val reversedItems = pallet.items.asReversed()
        if (searchQuery.isEmpty()) {
            reversedItems
        } else {
            reversedItems.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StardustModalBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        // Scaffold внутри BottomSheet для поддержки Snackbar
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = { Spacer(modifier = Modifier.height(32.dp)) }
        ) { innerPadding ->
            Column(Modifier.padding(innerPadding)) {
                // Заголовок
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Палет №${pallet.palletNumber}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = StardustTextPrimary
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            pallet.manufacturer?.let {
                                Text(
                                    "Производитель: $it",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StardustTextSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Точка разделитель
                                Box(modifier = Modifier.size(4.dp).background(StardustTextSecondary, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                "Всего: ${pallet.items.size} шт.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StardustPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { isExportMenuExpanded = true },
                            modifier = Modifier.background(StardustGlassBg, CircleShape)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Экспорт", tint = StardustPrimary)
                        }
                        DropdownMenu(
                            expanded = isExportMenuExpanded,
                            onDismissRequest = { isExportMenuExpanded = false },
                            modifier = Modifier.background(StardustGlassBg)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Excel (Список)", color = StardustTextPrimary) },
                                leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, tint = StardustSecondary) },
                                onClick = {
                                    isExportMenuExpanded = false
                                    exportPalletToExcel(context, pallet)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("PDF (Справка)", color = StardustTextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = StardustPrimary) },
                                onClick = {
                                    isExportMenuExpanded = false
                                    PalletSummaryPdfGenerator.generateAndShare(context, listOf(pallet), userName)
                                }
                            )
                        }
                    }
                }

                // Поиск
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Поиск по ID...", color = StardustTextSecondary.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = StardustTextSecondary) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null, tint = StardustTextSecondary)
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StardustPrimary,
                        unfocusedBorderColor = StardustSecondary.copy(alpha = 0.3f),
                        cursorColor = StardustPrimary,
                        focusedTextColor = StardustTextPrimary,
                        unfocusedTextColor = StardustTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // --- СПИСОК АКБ ---
                if (filteredItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Ничего не найдено", color = StardustTextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        itemsIndexed(filteredItems) { index, batteryId ->
                            // Нумерация: если поиск не активен, считаем от общего кол-ва вниз (как в палете)
                            val displayIndex = if (searchQuery.isEmpty()) pallet.items.size - index else index + 1

                            // Зебра: подкрашиваем четные строки
                            val backgroundColor = if (index % 2 == 0) Color.Transparent else StardustTextSecondary.copy(alpha = 0.05f)

                            // --- СВАЙП ДЛЯ УДАЛЕНИЯ ---
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                        viewModel.removeItemFromPallet(pallet.id, batteryId)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "АКБ удален",
                                                actionLabel = "ВЕРНУТЬ",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.distributeSpecificItemToPallet(pallet, batteryId)
                                            }
                                        }
                                        true
                                    } else false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val color by animateColorAsState(
                                        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) StardustError else Color.Transparent,
                                        label = "bgColor"
                                    )
                                    val scale by animateFloatAsState(
                                        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1.2f else 0.8f,
                                        label = "iconScale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color)
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(Icons.Default.Delete, "Удалить", tint = Color.White, modifier = Modifier.scale(scale))
                                    }
                                },
                                content = {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(StardustModalBg)
                                            .background(backgroundColor)
                                            .combinedClickable(
                                                onClick = {},
                                                onLongClick = {
                                                    clipboardManager.setText(AnnotatedString(batteryId))
                                                    Toast.makeText(context, "ID скопирован", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                            .padding(horizontal = 24.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#$displayIndex",
                                            color = StardustTextSecondary,
                                            fontSize = 12.sp,
                                            modifier = Modifier.width(40.dp)
                                        )

                                        Text(
                                            text = batteryId,
                                            color = StardustTextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Кнопка удаления
                                        IconButton(
                                            onClick = {
                                                viewModel.removeItemFromPallet(pallet.id, batteryId)
                                                scope.launch {
                                                    val res = snackbarHostState.showSnackbar("АКБ удален", "ВЕРНУТЬ")
                                                    if (res == SnackbarResult.ActionPerformed) viewModel.distributeSpecificItemToPallet(pallet, batteryId)
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = StardustError.copy(alpha = 0.7f))
                                        }
                                    }
                                    HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.1f), thickness = 0.5.dp)
                                }
                            )
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

        pallet.manufacturer?.let {
            val manufacturerRow = sheet.createRow(rowIndex++)
            manufacturerRow.createCell(0).setCellValue("Производитель: $it")
            rowIndex++
        }

        val headerRow = sheet.createRow(rowIndex++)
        headerRow.createCell(0).setCellValue("ID Аккумулятора")

        pallet.items.forEachIndexed { index, batteryId ->
            val row = sheet.createRow(rowIndex + index)
            row.createCell(0).setCellValue(batteryId)
        }

        sheet.autoSizeColumn(0)

        val fileName = "pallet_${pallet.palletNumber}_export.xlsx"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use {
            workbook.write(it)
        }
        workbook.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Экспорт палета"))

    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}