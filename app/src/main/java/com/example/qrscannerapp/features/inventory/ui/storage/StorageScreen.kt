package com.example.qrscannerapp.features.inventory.ui.storage

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.CellOperation
import com.example.qrscannerapp.QrScannerViewModel
import com.example.qrscannerapp.R
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
import com.example.qrscannerapp.features.inventory.ui.storage.utils.StorageFilter
import com.example.qrscannerapp.features.inventory.ui.storage.utils.formatAbsoluteDate
import com.example.qrscannerapp.features.inventory.ui.storage.utils.formatLogTime
import com.example.qrscannerapp.features.inventory.ui.storage.utils.formatLogTimestamp
import com.example.qrscannerapp.features.inventory.ui.storage.utils.formatRelativeTime
import com.example.qrscannerapp.features.inventory.ui.storage.utils.getInitials
import com.example.qrscannerapp.features.inventory.ui.storage.utils.getOperationVisuals
import com.example.qrscannerapp.features.inventory.ui.storage.utils.getRoleColor
import com.example.qrscannerapp.features.inventory.ui.storage.utils.getRoleLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// Терминальные цвета
private val TerminalGreen = Color(0xFF4AF626)
private val TerminalBg = Color(0xFF0D1117)
private val TerminalDimGreen = Color(0xFF2EA043)
private val TerminalAmber = Color(0xFFE3B341)
private val TerminalRed = Color(0xFFE5534B)

// ============================================================================================
// ГЛАВНЫЙ ЭКРАН
// ============================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    viewModel: QrScannerViewModel,
    authManager: AuthManager,
    onNavigateBack: () -> Unit,
    setTopBarActions: (@Composable RowScope.() -> Unit) -> Unit
) {
    val uiState by viewModel.storageState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedCellForDetails by remember { mutableStateOf<StorageCell?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showLogSheet by remember { mutableStateOf(false) }
    val logSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var cellToEdit by remember { mutableStateOf<StorageCell?>(null) }
    var cellToDelete by remember { mutableStateOf<StorageCell?>(null) }
    var cellForBulkAdd by remember { mutableStateOf<StorageCell?>(null) }
    var foundScooterData by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showClearLogDialog by remember { mutableStateOf(false) }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }
    var cellForDistributionConfirm by remember { mutableStateOf<StorageCell?>(null) }
    var cellForContextMenu by remember { mutableStateOf<StorageCell?>(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedCellIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedFilter by remember { mutableStateOf(StorageFilter.ALL) }

    val storageExportManager = remember { StorageExportManager(context) }
    val authState by authManager.authState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedCellIds = emptySet()
    }

    BackHandler(enabled = isSelectionMode) { exitSelectionMode() }

    val filterCounts = remember(uiState.cells) {
        mapOf(
            StorageFilter.ALL to uiState.cells.size,
            StorageFilter.AVAILABLE to uiState.cells.count { it.items.size < it.capacity },
            StorageFilter.FULL to uiState.cells.count { it.items.size >= it.capacity },
            StorageFilter.EMPTY to uiState.cells.count { it.items.isEmpty() }
        )
    }

    val totalScootersInStorage = remember(uiState.cells) {
        uiState.cells.sumOf { it.items.size }
    }

    val filteredCells = remember(searchQuery, uiState.cells, selectedFilter) {
        var cells = uiState.cells
        cells = when (selectedFilter) {
            StorageFilter.ALL -> cells
            StorageFilter.AVAILABLE -> cells.filter { it.items.size < it.capacity }
            StorageFilter.FULL -> cells.filter { it.items.size >= it.capacity }
            StorageFilter.EMPTY -> cells.filter { it.items.isEmpty() }
        }
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim()
            cells = cells.filter { cell ->
                cell.name.contains(query, ignoreCase = true) ||
                        cell.description.contains(query, ignoreCase = true) ||
                        cell.items.any { it.contains(query, ignoreCase = true) }
            }
        }
        cells
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadStorageCells()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
            topBar = {
                Crossfade(targetState = isSelectionMode, label = "top_bar_anim") { selectionActive ->
                    if (selectionActive) {
                        SelectionTopAppBar(
                            selectedCount = selectedCellIds.size,
                            onCloseClick = { exitSelectionMode() },
                            onDeleteClick = { showBulkDeleteConfirmDialog = true }
                        )
                    } else {
                        TopAppBar(
                            title = { },
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = StardustTextPrimary)
                                }
                            },
                            actions = {
                                IconButton(onClick = { showLogSheet = true }) {
                                    Icon(Icons.Default.History, contentDescription = "История операций", tint = StardustTextPrimary)
                                }
                                IconButton(onClick = { storageExportManager.exportAllCellsToExcel(uiState.cells) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Экспортировать", tint = StardustTextPrimary)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = StardustTextPrimary,
                                actionIconContentColor = StardustTextPrimary
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = !isSelectionMode,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                    exit = scaleOut(tween(150))
                ) {
                    FloatingActionButton(
                        onClick = { showCreateDialog = true },
                        containerColor = StardustPrimary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Создать ячейку")
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                when {
                    uiState.isLoading && uiState.cells.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = StardustPrimary
                        )
                    }
                    uiState.error != null -> {
                        Text(
                            text = uiState.error!!,
                            color = StardustError,
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                Text(
                                    "Самокаты",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = StardustTextPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                                if (!isSelectionMode) {
                                    Text(
                                        "Удерживайте ячейку для действий",
                                        fontSize = 12.sp,
                                        color = StardustTextSecondary.copy(alpha = 0.6f),
                                        letterSpacing = 0.sp
                                    )
                                }
                            }

                            if (uiState.cells.isNotEmpty()) {
                                StorageSummaryHeader(
                                    totalCells = uiState.cells.size,
                                    totalScooters = totalScootersInStorage,
                                    modifier = Modifier.padding(bottom = 14.dp)
                                )
                            }

                            // Поиск
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Поиск по названию, номеру...", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                trailingIcon = {
                                    AnimatedVisibility(visible = searchQuery.isNotEmpty(), enter = scaleIn(), exit = scaleOut()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { }),
                                shape = RoundedCornerShape(14.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = StardustPrimary.copy(alpha = 0.6f),
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    focusedContainerColor = StardustGlassBg,
                                    unfocusedContainerColor = StardustGlassBg,
                                    focusedTextColor = StardustTextPrimary,
                                    unfocusedTextColor = StardustTextPrimary,
                                    focusedPlaceholderColor = StardustTextSecondary.copy(alpha = 0.5f),
                                    unfocusedPlaceholderColor = StardustTextSecondary.copy(alpha = 0.5f),
                                    focusedLeadingIconColor = StardustPrimary,
                                    unfocusedLeadingIconColor = StardustTextSecondary.copy(alpha = 0.5f)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Фильтры
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(StorageFilter.values()) { filter ->
                                    val isSelected = selectedFilter == filter
                                    val count = filterCounts[filter] ?: 0
                                    val labelText = if (filter == StorageFilter.ALL) filter.title
                                    else "${filter.title} ($count)"

                                    val chipBg by animateColorAsState(
                                        targetValue = if (isSelected) StardustPrimary else StardustGlassBg,
                                        animationSpec = tween(200), label = "chip_bg"
                                    )
                                    val chipText by animateColorAsState(
                                        targetValue = if (isSelected) Color.White else StardustTextSecondary,
                                        animationSpec = tween(200), label = "chip_text"
                                    )

                                    Surface(
                                        onClick = { selectedFilter = filter },
                                        shape = RoundedCornerShape(20.dp),
                                        color = chipBg,
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                                            Text(labelText, color = chipText, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val undistributedCount by remember {
                                derivedStateOf { viewModel.scooterCodes.size }
                            }

                            // Баннер буфера
                            AnimatedVisibility(
                                visible = undistributedCount > 0,
                                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    border = BorderStroke(1.dp, StardustSecondary.copy(alpha = 0.4f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(StardustSecondary.copy(alpha = 0.15f), StardustPrimary.copy(alpha = 0.1f))
                                                )
                                            )
                                            .padding(14.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier.size(36.dp).clip(CircleShape).background(StardustSecondary.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = StardustSecondary, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text("В буфере: $undistributedCount шт.", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Text("Нажмите на ячейку для распределения", color = StardustTextSecondary, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            if (filteredCells.isEmpty() && uiState.cells.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = StardustTextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Ничего не найдено", color = StardustTextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                        Text("Попробуйте другой запрос", color = StardustTextSecondary.copy(alpha = 0.5f), fontSize = 13.sp)
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 88.dp)
                                ) {
                                    itemsIndexed(filteredCells, key = { _, cell -> cell.id }) { index, cell ->
                                        val isSelected = selectedCellIds.contains(cell.id)

                                        val animAlpha by animateFloatAsState(
                                            targetValue = 1f,
                                            animationSpec = tween(durationMillis = 300, delayMillis = index * 40),
                                            label = "cell_alpha_$index"
                                        )

                                        StorageCellTile(
                                            cell = cell,
                                            searchQuery = searchQuery,
                                            isSelected = isSelected,
                                            tileAlpha = animAlpha,
                                            onClick = {
                                                if (isSelectionMode) {
                                                    selectedCellIds = if (isSelected) selectedCellIds - cell.id
                                                    else selectedCellIds + cell.id
                                                    if (selectedCellIds.isEmpty()) isSelectionMode = false
                                                } else {
                                                    if (undistributedCount > 0) cellForDistributionConfirm = cell
                                                    else selectedCellForDetails = cell
                                                }
                                            },
                                            onLongClick = {
                                                if (!isSelectionMode) cellForContextMenu = cell
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (uiState.isLoading && uiState.cells.isNotEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = StardustPrimary)
                }
            }
        }
    }

    // Контекстное меню
    if (cellForContextMenu != null) {
        val cell = cellForContextMenu!!
        CellContextMenuSheet(
            cell = cell,
            onDismiss = { cellForContextMenu = null },
            onOpen = { cellForContextMenu = null; selectedCellForDetails = cell },
            onEdit = { cellForContextMenu = null; cellToEdit = cell },
            onBulkAdd = { cellForContextMenu = null; cellForBulkAdd = cell },
            onSelect = { cellForContextMenu = null; isSelectionMode = true; selectedCellIds = setOf(cell.id) },
            onDelete = { cellForContextMenu = null; cellToDelete = cell }
        )
    }

    if (cellForDistributionConfirm != null) {
        val cell = cellForDistributionConfirm!!
        val count = viewModel.scooterCodes.size
        AlertDialog(
            onDismissRequest = { cellForDistributionConfirm = null },
            title = { Text("Распределить в ${cell.name}?", color = StardustTextPrimary) },
            text = { Text("Будет добавлено $count самокатов.\nСвободных мест: ${cell.capacity - cell.items.size}.", color = StardustTextSecondary) },
            confirmButton = { Button(onClick = { viewModel.distributeScootersToCell(cell); cellForDistributionConfirm = null }, colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) { Text("Добавить") } },
            dismissButton = { TextButton(onClick = { cellForDistributionConfirm = null }) { Text("Отмена", color = StardustTextSecondary) } },
            containerColor = StardustModalBg
        )
    }

    if (showLogSheet) {
        ModalBottomSheet(onDismissRequest = { showLogSheet = false }, sheetState = logSheetState, containerColor = StardustModalBg) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                StorageActivityLogFullView(logEntries = uiState.activityLog, isAdmin = authState.isAdmin, onClearLogClick = { showClearLogDialog = true })
            }
        }
    }

    if (showBulkDeleteConfirmDialog) {
        val selectedCellsToDelete = uiState.cells.filter { selectedCellIds.contains(it.id) }
        BulkDeleteConfirmDialog(count = selectedCellsToDelete.size, onDismiss = { showBulkDeleteConfirmDialog = false }, onConfirm = {
            viewModel.deleteCells(selectedCellsToDelete); showBulkDeleteConfirmDialog = false; exitSelectionMode()
        })
    }

    if (foundScooterData != null) {
        ScooterSearchResultDialog(scooterNumber = foundScooterData!!.first, locationName = foundScooterData!!.second, lastUser = "Система", onDismiss = { foundScooterData = null }, onNavigate = { foundScooterData = null })
    }

    if (showCreateDialog) {
        CreateCellDialog(onDismiss = { showCreateDialog = false }, onCreate = { description, capacity -> viewModel.createNewCell(description, capacity); showCreateDialog = false })
    }

    if (cellToEdit != null) {
        EditCellDialog(cell = cellToEdit!!, onDismiss = { cellToEdit = null }, onSave = { cellId, newDescription, newCapacity -> viewModel.updateCell(cellId, newDescription, newCapacity); cellToEdit = null })
    }

    if (cellForBulkAdd != null) {
        BulkAddScootersDialog(cell = cellForBulkAdd!!, onDismiss = { cellForBulkAdd = null }, onAdd = { cell, text -> viewModel.bulkAddScootersToCell(cell.id, text); cellForBulkAdd = null })
    }

    val currentSelectedCell = uiState.cells.find { it.id == selectedCellForDetails?.id }

    if (currentSelectedCell != null) {
        CellDetailsSheet(
            cell = currentSelectedCell, sheetState = bottomSheetState, viewModel = viewModel,
            storageExportManager = storageExportManager, searchQuery = searchQuery,
            onDismiss = { scope.launch { bottomSheetState.hide() }.invokeOnCompletion { selectedCellForDetails = null } },
            onEditClick = {
                scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                    if (!bottomSheetState.isVisible) { cellToEdit = currentSelectedCell; selectedCellForDetails = null }
                }
            },
            onDeleteClick = {
                scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                    if (!bottomSheetState.isVisible) { cellToDelete = currentSelectedCell; selectedCellForDetails = null }
                }
            }
        )
    }

    LaunchedEffect(selectedCellForDetails) {
        if (selectedCellForDetails != null) scope.launch { bottomSheetState.show() }
    }

    if (cellToDelete != null) {
        DeleteCellDialog(cell = cellToDelete!!, onDismiss = { cellToDelete = null }, onConfirm = { viewModel.deleteCell(it); cellToDelete = null })
    }

    if (showClearLogDialog) {
        ClearLogDialog(onDismiss = { showClearLogDialog = false }, onConfirm = { viewModel.clearStorageActivityLog(); showClearLogDialog = false })
    }
}

// ============================================================================================
// Summary Header
// ============================================================================================

@Composable
fun StorageSummaryHeader(totalCells: Int, totalScooters: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryChip(icon = Icons.Default.Inventory2, value = totalCells.toString(), label = "ячеек", modifier = Modifier.weight(1f))
        SummaryChip(icon = Icons.Default.ElectricScooter, value = totalScooters.toString(), label = "самокатов", modifier = Modifier.weight(1f))
    }
}

@Composable
fun SummaryChip(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, StardustPrimary.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(StardustPrimary.copy(alpha = 0.08f), StardustGlassBg)))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(StardustPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, contentDescription = null, tint = StardustPrimary, modifier = Modifier.size(17.dp)) }
                Column {
                    Text(value, color = StardustTextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 18.sp)
                    Text(label, color = StardustTextSecondary, fontSize = 11.sp, lineHeight = 11.sp)
                }
            }
        }
    }
}

// ============================================================================================
// УЛУЧШЕННЫЙ StorageCellTile — с датой создания и автором
// ============================================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StorageCellTile(
    cell: StorageCell,
    searchQuery: String,
    isSelected: Boolean,
    tileAlpha: Float = 1f,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val progress = if (cell.capacity > 0) cell.items.size.toFloat() / cell.capacity.toFloat() else 0f
    val progressColor = getColorByProgress(progress)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(600), label = "progress")

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> StardustPrimary
            progress >= 1f -> StardustError.copy(alpha = 0.6f)
            progress >= 0.85f -> StardustSecondary.copy(alpha = 0.5f)
            else -> StardustPrimary.copy(alpha = 0.08f)
        },
        animationSpec = tween(300), label = "border"
    )

    val tileScale by animateFloatAsState(
        targetValue = if (isSelected) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale"
    )

    val matchingItem = remember(searchQuery, cell.items) {
        if (searchQuery.isNotBlank()) cell.items.firstOrNull { it.contains(searchQuery, ignoreCase = true) } else null
    }
    val isMatchInText = remember(searchQuery, cell.name, cell.description) {
        searchQuery.isNotBlank() && (cell.name.contains(searchQuery, ignoreCase = true) || cell.description.contains(searchQuery, ignoreCase = true))
    }
    val showMatchingItem = matchingItem != null && !isMatchInText

    val cardBg = when {
        progress >= 1f -> Brush.verticalGradient(listOf(StardustError.copy(alpha = 0.12f), StardustGlassBg))
        progress >= 0.85f -> Brush.verticalGradient(listOf(StardustSecondary.copy(alpha = 0.08f), StardustGlassBg))
        cell.items.isEmpty() -> Brush.verticalGradient(listOf(StardustGlassBg, StardustGlassBg.copy(alpha = 0.7f)))
        else -> Brush.verticalGradient(listOf(StardustPrimary.copy(alpha = 0.06f), StardustGlassBg))
    }

    // Подсчёт операций за сегодня
    val todayOpsCount = remember(cell.operations) {
        val todayStart = System.currentTimeMillis() - (System.currentTimeMillis() % 86400000)
        cell.operations.count { it.timestamp >= todayStart }
    }

    Card(
        modifier = Modifier
            .aspectRatio(0.85f) // Чуть выше чтобы вместить метаданные
            .scale(tileScale)
            .graphicsLayer { alpha = tileAlpha }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(cardBg)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Имя ячейки + статус
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    HighlightedText(
                        text = cell.name,
                        highlight = searchQuery,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = StardustTextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isSelected) {
                        val statusIcon = when {
                            progress >= 1f -> Icons.Default.Lock
                            cell.items.isEmpty() -> Icons.Default.Inbox
                            else -> null
                        }
                        if (statusIcon != null) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = if (progress >= 1f) StardustError.copy(alpha = 0.7f) else StardustTextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Описание
                HighlightedText(
                    text = cell.description,
                    highlight = searchQuery,
                    fontSize = 11.sp,
                    color = StardustTextSecondary.copy(alpha = 0.7f),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                // <<< НОВОЕ: автор и дата создания
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Аватар-инициалы
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(StardustPrimary.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            getInitials(cell.createdByName),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = StardustPrimary,
                            lineHeight = 7.sp
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = cell.createdByName?.split(" ")?.firstOrNull() ?: "—",
                        fontSize = 10.sp,
                        color = StardustTextSecondary.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 50.dp)
                    )
                    Text(
                        text = " · ${formatRelativeTime(cell.createdAt)}",
                        fontSize = 10.sp,
                        color = StardustTextSecondary.copy(alpha = 0.4f),
                        maxLines = 1
                    )
                }

                // Найденный элемент при поиске
                if (showMatchingItem) {
                    Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.QrCode2, contentDescription = null, tint = StardustSuccess, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        HighlightedText(text = matchingItem ?: "", highlight = searchQuery, fontSize = 11.sp, color = StardustSuccess.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(Modifier.weight(1f))

                // Счётчик
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                    Text("${cell.items.size}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = progressColor, lineHeight = 28.sp)
                    Text(" / ${cell.capacity}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = StardustTextSecondary, modifier = Modifier.padding(bottom = 3.dp))
                }

                Spacer(Modifier.height(6.dp))

                // Прогресс-бар
                Box(
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(StardustItemBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(progressColor.copy(alpha = 0.7f), progressColor)))
                    )
                }

                // <<< НОВОЕ: бейдж операций за сегодня
                if (todayOpsCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = TerminalGreen.copy(alpha = 0.7f), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "$todayOpsCount сегодня",
                            fontSize = 9.sp,
                            color = TerminalGreen.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Галочка выбора
            val checkAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "check_alpha"
            )
            val checkScale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.5f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "check_scale"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(22.dp)
                    .graphicsLayer { alpha = checkAlpha; scaleX = checkScale; scaleY = checkScale }
                    .clip(CircleShape)
                    .background(StardustPrimary),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp)) }
        }
    }
}

// ============================================================================================
// Контекстное меню
// ============================================================================================

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StardustModalBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(cell.name, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(cell.description, color = StardustTextSecondary, fontSize = 13.sp)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = StardustGlassBg) {
                    Text(
                        text = "${cell.items.size}/${cell.capacity}",
                        color = getColorByProgress(if (cell.capacity > 0) cell.items.size.toFloat() / cell.capacity else 0f),
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            HorizontalDivider(color = StardustItemBg, modifier = Modifier.padding(bottom = 4.dp))

            ContextMenuItem(icon = Icons.Default.OpenInNew, label = "Открыть содержимое", tint = StardustTextPrimary, onClick = onOpen)
            ContextMenuItem(icon = Icons.Default.Edit, label = "Редактировать ячейку", tint = StardustPrimary, onClick = onEdit)
            ContextMenuItem(icon = Icons.Default.PlaylistAdd, label = "Добавить номера списком", tint = StardustSuccess, onClick = onBulkAdd)
            ContextMenuItem(icon = Icons.Default.CheckBox, label = "Выбрать для удаления", tint = StardustSecondary, onClick = onSelect)

            HorizontalDivider(color = StardustItemBg, modifier = Modifier.padding(vertical = 4.dp))

            ContextMenuItem(icon = Icons.Default.Delete, label = "Удалить ячейку", tint = StardustError, onClick = onDelete)
        }
    }
}

@Composable
private fun ContextMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, color = tint, fontSize = 15.sp, fontWeight = if (tint == StardustError) FontWeight.SemiBold else FontWeight.Normal)
    }
}

// ============================================================================================
// УЛУЧШЕННЫЙ CellDetailsSheet — с метаданными и терминальным логом
// ============================================================================================

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
    val rotationAngle by animateFloatAsState(targetValue = if (isDescriptionExpanded) 180f else 0f, label = "rotation", animationSpec = tween(300))
    var showExportSheet by remember { mutableStateOf(false) }
    val exportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var scooterToRemove by remember { mutableStateOf<String?>(null) }

    // Переключатель: список самокатов / терминал операций
    var showTerminal by remember { mutableStateOf(false) }

    val displayedItems = remember(cell.items, searchQuery) {
        if (searchQuery.isNotBlank()) {
            val (matches, nonMatches) = cell.items.partition { it.contains(searchQuery, ignoreCase = true) }
            matches.sorted() + nonMatches
        } else cell.items.reversed()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = StardustModalBg, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(Modifier.padding(bottom = 32.dp)) {
            // Заголовок шторки
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Содержимое", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StardustTextPrimary)
                    Text(cell.name, fontSize = 14.sp, color = StardustTextSecondary)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = StardustGlassBg) {
                    Text(
                        "${cell.items.size}/${cell.capacity}",
                        color = getColorByProgress(if (cell.capacity > 0) cell.items.size.toFloat() / cell.capacity else 0f),
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // <<< НОВОЕ: Метаданные создания
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = StardustGlassBg),
                border = BorderStroke(1.dp, StardustPrimary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Автор + роль
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(StardustPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                getInitials(cell.createdByName),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StardustPrimary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                cell.createdByName ?: "—",
                                fontSize = 14.sp,
                                color = StardustTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Бейдж роли
                            Text(
                                getRoleLabel(cell.createdByRole),
                                fontSize = 10.sp,
                                color = getRoleColor(cell.createdByRole),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Разделитель
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(StardustItemBg)
                    )

                    // Дата создания — абсолютная
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(StardustSecondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = StardustSecondary, modifier = Modifier.size(15.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Создана", fontSize = 10.sp, color = StardustTextSecondary.copy(alpha = 0.6f))
                            Text(
                                formatAbsoluteDate(cell.createdAt),
                                fontSize = 14.sp,
                                color = StardustTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Описание (сворачиваемое)
            Card(modifier = Modifier.padding(horizontal = 16.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = StardustItemBg)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().clickable { isDescriptionExpanded = !isDescriptionExpanded }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Описание", color = StardustTextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        IconButton(onClick = onEditClick, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = StardustTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = StardustTextSecondary, modifier = Modifier.rotate(rotationAngle))
                    }
                    AnimatedVisibility(visible = isDescriptionExpanded) {
                        Text(cell.description, color = StardustTextSecondary, modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // <<< НОВОЕ: Переключатель "Список" / "Терминал"
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabBgItems by animateColorAsState(
                    if (!showTerminal) StardustPrimary.copy(alpha = 0.15f) else Color.Transparent,
                    tween(200), label = "tab_items"
                )
                val tabBgTerminal by animateColorAsState(
                    if (showTerminal) TerminalGreen.copy(alpha = 0.15f) else Color.Transparent,
                    tween(200), label = "tab_terminal"
                )

                Surface(
                    onClick = { showTerminal = false },
                    shape = RoundedCornerShape(10.dp),
                    color = tabBgItems,
                    border = BorderStroke(1.dp, if (!showTerminal) StardustPrimary.copy(alpha = 0.3f) else StardustItemBg),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, tint = if (!showTerminal) StardustPrimary else StardustTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Список (${cell.items.size})", fontSize = 13.sp, color = if (!showTerminal) StardustPrimary else StardustTextSecondary, fontWeight = if (!showTerminal) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }

                Surface(
                    onClick = { showTerminal = true },
                    shape = RoundedCornerShape(10.dp),
                    color = tabBgTerminal,
                    border = BorderStroke(1.dp, if (showTerminal) TerminalGreen.copy(alpha = 0.3f) else StardustItemBg),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = if (showTerminal) TerminalGreen else StardustTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Операции (${cell.operations.size})", fontSize = 13.sp, color = if (showTerminal) TerminalGreen else StardustTextSecondary, fontWeight = if (showTerminal) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Контент: список ИЛИ терминал
            Crossfade(targetState = showTerminal, label = "content_switch") { isTerminal ->
                if (isTerminal) {
                    // <<< ТЕРМИНАЛЬНЫЙ ЛОГ ОПЕРАЦИЙ
                    CellOperationsTerminal(
                        operations = cell.operations,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.55f)
                            .padding(horizontal = 16.dp)
                    )
                } else {
                    // Список самокатов
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
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.QrCode2, contentDescription = null, tint = if (scooterId.contains(searchQuery, true) && searchQuery.isNotBlank()) StardustPrimary else StardustTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        HighlightedText(text = scooterId, highlight = searchQuery, color = StardustTextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
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
                    Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Экспорт", fontSize = 14.sp)
                }
                Button(onClick = onDeleteClick, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = StardustError.copy(alpha = 0.25f), contentColor = StardustError), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Удалить", fontSize = 14.sp)
                }
            }
        }
    }

    if (scooterToRemove != null) {
        val scooterId = scooterToRemove!!
        AlertDialog(
            onDismissRequest = { scooterToRemove = null },
            title = { Text("Удалить самокат?", color = StardustTextPrimary) },
            text = { Text("Самокат $scooterId будет удалён из ${cell.name}.", color = StardustTextSecondary) },
            confirmButton = { Button(onClick = { viewModel.removeItemFromCell(cell, scooterId); scooterToRemove = null }, colors = ButtonDefaults.buttonColors(containerColor = StardustError)) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { scooterToRemove = null }) { Text("Отмена", color = StardustTextSecondary) } },
            containerColor = StardustModalBg
        )
    }

    if (showExportSheet) {
        CellExportSheet(cell = cell, sheetState = exportSheetState, storageExportManager = storageExportManager, onDismiss = {
            scope.launch { exportSheetState.hide() }.invokeOnCompletion { showExportSheet = false }
        })
    }
}

// ============================================================================================
// <<< НОВОЕ: Терминальный лог операций
// ============================================================================================

@Composable
fun CellOperationsTerminal(
    operations: List<CellOperation>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalBg),
        border = BorderStroke(1.dp, TerminalGreen.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Заголовок терминала
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalGreen.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кружочки "окна" терминала
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(TerminalRed.copy(alpha = 0.8f)))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(TerminalAmber.copy(alpha = 0.8f)))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(TerminalGreen.copy(alpha = 0.8f)))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "cell_operations.log",
                    fontSize = 11.sp,
                    color = TerminalGreen.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${operations.size} записей",
                    fontSize = 10.sp,
                    color = TerminalDimGreen.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace
                )
            }

            if (operations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$ cat operations.log",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TerminalGreen.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "// пусто",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TerminalDimGreen.copy(alpha = 0.3f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(operations) { index, op ->
                        TerminalOperationLine(operation = op, index = index)
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalOperationLine(operation: CellOperation, index: Int) {
    val (_, opColor) = getOperationVisuals(operation.action)

    // Цвет строки в стиле терминала
    val lineColor = when (operation.action) {
        "CREATED" -> TerminalGreen
        "EDITED" -> TerminalAmber
        "ITEMS_ADDED", "SCOOTERS_ADDED", "BULK_ADDED" -> TerminalGreen
        "ITEM_REMOVED" -> TerminalRed
        "DELETED" -> TerminalRed
        else -> TerminalDimGreen
    }

    // Символ операции
    val opSymbol = when (operation.action) {
        "CREATED" -> "+"
        "EDITED" -> "~"
        "ITEMS_ADDED", "SCOOTERS_ADDED", "BULK_ADDED" -> "+"
        "ITEM_REMOVED" -> "-"
        "DELETED" -> "×"
        else -> "·"
    }

    // Анимация появления строки
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 30L)
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(200, easing = LinearEasing),
        label = "term_line_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp
        Text(
            text = formatLogTimestamp(operation.timestamp),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = TerminalDimGreen.copy(alpha = 0.5f),
            modifier = Modifier.width(80.dp)
        )

        // Символ операции
        Text(
            text = opSymbol,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = lineColor,
            modifier = Modifier.width(14.dp)
        )

        // Контент
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = operation.details,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = lineColor.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = operation.userName,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = TerminalDimGreen.copy(alpha = 0.4f)
            )
        }

        // Количество элементов
        if (operation.itemCount > 0) {
            Text(
                text = "${if (operation.action.contains("REMOVE") || operation.action == "DELETED") "-" else "+"}${operation.itemCount}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = lineColor.copy(alpha = 0.7f)
            )
        }
    }
}

// ============================================================================================
// Шторка экспорта
// ============================================================================================

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
                val text = cell.items.reversed().joinToString("\n")
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Cell Items", text))
                Toast.makeText(context, "Скопировано ${cell.items.size} номеров", Toast.LENGTH_SHORT).show(); dismiss()
            }
            Spacer(modifier = Modifier.height(8.dp))
            ExportOptionButton(icon = Icons.Default.Sort, title = "Копировать отсортированный", subtitle = "По возрастанию номера", containerColor = StardustItemBg, contentColor = StardustTextPrimary) {
                val text = cell.items.sorted().joinToString("\n")
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

@Composable
fun ExportOptionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, containerColor: Color, contentColor: Color, onClick: () -> Unit) {
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

// ============================================================================================
// Остальные компоненты
// ============================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(selectedCount: Int, onCloseClick: () -> Unit, onDeleteClick: () -> Unit) {
    TopAppBar(
        title = { Text("Выбрано: $selectedCount", color = StardustTextPrimary, fontWeight = FontWeight.SemiBold) },
        navigationIcon = { IconButton(onClick = onCloseClick) { Icon(Icons.Default.Close, contentDescription = null, tint = StardustTextPrimary) } },
        actions = { IconButton(onClick = onDeleteClick, enabled = selectedCount > 0) { Icon(Icons.Default.Delete, contentDescription = null, tint = if (selectedCount > 0) StardustError else Color.Gray) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = StardustGlassBg.copy(alpha = 0.9f))
    )
}

@Composable
fun BulkDeleteConfirmDialog(count: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Удалить $count ячеек?") }, text = { Text("Все самокаты в этих ячейках снова станут доступны для распределения. Это действие необратимо.") }, confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = StardustError)) { Text("Удалить") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = StardustTextSecondary) } }, containerColor = StardustModalBg, titleContentColor = StardustTextPrimary, textContentColor = StardustTextSecondary)
}

@Composable
fun ScooterSearchResultDialog(scooterNumber: String, locationName: String, lastUser: String, onDismiss: () -> Unit, onNavigate: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxWidth().height(460.dp)) {
            Image(painter = painterResource(id = R.drawable.scooter), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(280.dp).align(Alignment.TopCenter).offset(y = 20.dp).zIndex(1f))
            Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = StardustModalBg), modifier = Modifier.fillMaxWidth().height(300.dp).align(Alignment.BottomCenter)) {
                Column(modifier = Modifier.fillMaxSize().padding(top = 90.dp, start = 24.dp, end = 24.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Самокат найден!", style = MaterialTheme.typography.titleMedium, color = StardustSuccess, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(scooterNumber, style = MaterialTheme.typography.headlineLarge, color = StardustTextPrimary, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Place, null, tint = StardustSecondary, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(6.dp)); Text(locationName, color = StardustTextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Person, null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Добавил: $lastUser", color = StardustTextSecondary, fontSize = 14.sp) }
                    }
                    Button(onClick = onNavigate, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary), shape = RoundedCornerShape(14.dp)) { Text("Перейти к месту", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun BulkAddScootersDialog(cell: StorageCell, onDismiss: () -> Unit, onAdd: (StorageCell, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val recognizedCount = remember(text) { text.lines().count { it.trim().isNotBlank() } }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = StardustModalBg)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Добавить в ${cell.name}", style = MaterialTheme.typography.titleLarge, color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Вставьте номера самокатов, каждый с новой строки.", style = MaterialTheme.typography.bodyMedium, color = StardustTextSecondary)
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth().height(200.dp), label = { Text("Номера самокатов") }, colors = TextFieldDefaults.colors(focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextSecondary, focusedContainerColor = StardustGlassBg, unfocusedContainerColor = StardustGlassBg, focusedIndicatorColor = StardustPrimary, unfocusedIndicatorColor = StardustItemBg, focusedLabelColor = StardustPrimary, unfocusedLabelColor = StardustTextSecondary))
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedVisibility(visible = recognizedCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StardustSuccess, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Распознано: $recognizedCount номеров", color = StardustSuccess, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Отмена", color = StardustTextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onAdd(cell, text) }, enabled = text.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary), shape = RoundedCornerShape(12.dp)) { Text("Добавить") }
                }
            }
        }
    }
}

@Composable
fun HighlightedText(text: String, highlight: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified, fontSize: TextUnit = TextUnit.Unspecified, fontWeight: FontWeight? = null, textAlign: TextAlign? = null, maxLines: Int = Int.MAX_VALUE, overflow: TextOverflow = TextOverflow.Clip) {
    if (highlight.isBlank()) { Text(text = text, modifier = modifier, color = color, fontSize = fontSize, fontWeight = fontWeight, textAlign = textAlign, maxLines = maxLines, overflow = overflow); return }
    val annotatedString = buildAnnotatedString {
        var startIndex = 0
        while (startIndex < text.length) {
            val index = text.indexOf(highlight, startIndex, ignoreCase = true)
            if (index == -1) { append(text.substring(startIndex)); break }
            append(text.substring(startIndex, index))
            withStyle(style = SpanStyle(background = StardustSuccess.copy(alpha = 0.3f), fontWeight = fontWeight)) { append(text.substring(index, index + highlight.length)) }
            startIndex = index + highlight.length
        }
    }
    Text(text = annotatedString, modifier = modifier, color = color, fontSize = fontSize, fontWeight = fontWeight, textAlign = textAlign, maxLines = maxLines, overflow = overflow)
}

@Composable
fun CreateCellDialog(onDismiss: () -> Unit, onCreate: (description: String, capacity: Int) -> Unit) {
    var description by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("600") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = StardustModalBg)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Новая ячейка", style = MaterialTheme.typography.titleLarge, color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Описание") }, leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = TextFieldDefaults.colors(focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextSecondary, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = StardustPrimary, unfocusedIndicatorColor = StardustItemBg, focusedLabelColor = StardustPrimary, unfocusedLabelColor = StardustTextSecondary, focusedLeadingIconColor = StardustPrimary, unfocusedLeadingIconColor = StardustTextSecondary))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = capacity, onValueChange = { if (it.all { char -> char.isDigit() }) capacity = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Ёмкость") }, leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(12.dp), colors = TextFieldDefaults.colors(focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextSecondary, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = StardustPrimary, unfocusedIndicatorColor = StardustItemBg, focusedLabelColor = StardustPrimary, unfocusedLabelColor = StardustTextSecondary, focusedLeadingIconColor = StardustPrimary, unfocusedLeadingIconColor = StardustTextSecondary))
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Отмена", color = StardustTextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onCreate(description, capacity.toIntOrNull() ?: 600) }, enabled = description.isNotBlank() && capacity.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary), shape = RoundedCornerShape(12.dp)) { Text("Создать") }
                }
            }
        }
    }
}

@Composable
fun EditCellDialog(cell: StorageCell, onDismiss: () -> Unit, onSave: (cellId: String, newDescription: String, newCapacity: Int) -> Unit) {
    var description by remember { mutableStateOf(cell.description) }
    var capacity by remember { mutableStateOf(cell.capacity.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Редактировать ${cell.name}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Описание") }, singleLine = true); OutlinedTextField(value = capacity, onValueChange = { if (it.all { char -> char.isDigit() }) capacity = it }, label = { Text("Ёмкость") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) } }, confirmButton = { Button(onClick = { onSave(cell.id, description, capacity.toIntOrNull() ?: 700) }, enabled = description.isNotBlank() && capacity.isNotBlank(), shape = RoundedCornerShape(12.dp)) { Text("Сохранить") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }, containerColor = StardustModalBg, titleContentColor = StardustTextPrimary, textContentColor = StardustTextSecondary)
}

@Composable
fun DeleteCellDialog(cell: StorageCell, onDismiss: () -> Unit, onConfirm: (StorageCell) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Удалить ${cell.name}?") }, text = { Text("Все ${cell.items.size} самокатов в этой ячейке снова станут доступны для распределения. Это действие необратимо.") }, confirmButton = { Button(onClick = { onConfirm(cell) }, colors = ButtonDefaults.buttonColors(containerColor = StardustError), shape = RoundedCornerShape(12.dp)) { Text("Удалить") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }, containerColor = StardustModalBg, titleContentColor = StardustTextPrimary, textContentColor = StardustTextSecondary)
}

@Composable
fun StorageActivityLogFullView(logEntries: List<StorageActivityLogEntry>, isAdmin: Boolean, onClearLogClick: () -> Unit) {
    Column(modifier = Modifier.padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("История операций", fontWeight = FontWeight.Bold, color = StardustTextPrimary, fontSize = 18.sp, modifier = Modifier.weight(1f))
            if (isAdmin) { IconButton(onClick = onClearLogClick) { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = StardustError) } }
        }
        if (logEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("История пуста", color = StardustTextSecondary) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp)) { items(logEntries) { entry -> StorageLogEntryItem(entry = entry) } }
        }
    }
}

@Composable
fun StorageLogEntryItem(entry: StorageActivityLogEntry) {
    val actionText = buildAnnotatedString {
        append("${formatLogTime(entry.timestamp)} ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = StardustError)) { append(entry.userName) }
        append(" ${entry.details}")
    }
    val (icon, color) = remember(entry.action) {
        when (entry.action) {
            "CREATED" -> Icons.Default.AddCircle to StardustSuccess
            "DELETED" -> Icons.Default.Delete to StardustError
            "EDITED" -> Icons.Default.Edit to StardustSecondary
            "SCOOTERS_ADDED" -> Icons.Default.Add to StardustSuccess
            "ITEM_REMOVED" -> Icons.Default.Clear to StardustError
            else -> Icons.Default.Info to StardustTextSecondary
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = actionText, color = StardustTextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
fun ClearLogDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Очистить историю?") }, text = { Text("Вы уверены? Это действие необратимо.") }, confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = StardustError), shape = RoundedCornerShape(12.dp)) { Text("Очистить") } }, dismissButton = { Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("Отмена") } }, containerColor = StardustModalBg, titleContentColor = StardustTextPrimary, textContentColor = StardustTextSecondary)
}