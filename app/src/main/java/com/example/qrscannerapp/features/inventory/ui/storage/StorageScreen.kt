package com.example.qrscannerapp.features.inventory.ui.storage

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.QrScannerViewModel
import com.example.qrscannerapp.StardustGlassBg
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustModalBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.StorageCell
import com.example.qrscannerapp.UserRole
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.common.ui.ScreenShell
import com.example.qrscannerapp.features.inventory.data.export.StorageExportManager
import com.example.qrscannerapp.features.inventory.ui.storage.components.*
import com.example.qrscannerapp.features.inventory.ui.storage.dialogs.*
import com.example.qrscannerapp.features.inventory.ui.storage.hub.StorageHubScreen
import com.example.qrscannerapp.features.inventory.ui.storage.sheets.*
import com.example.qrscannerapp.features.inventory.ui.storage.utils.*
import kotlinx.coroutines.launch

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
    var cellForContextMenu by remember { mutableStateOf<StorageCell?>(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedCellIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedFilter by remember { mutableStateOf(StorageFilter.ALL) }

    val storageExportManager = remember { StorageExportManager(context) }
    val authState by authManager.authState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val isAdmin = authState.role == UserRole.ADMIN
    var selectedTab by remember { mutableStateOf(StorageTab.CELLS) }

    fun exitSelectionMode() { isSelectionMode = false; selectedCellIds = emptySet() }
    BackHandler(isSelectionMode) { exitSelectionMode() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> 
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadStorageCells() 
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val filteredCells = remember(uiState.cells, selectedFilter, searchQuery) {
        uiState.cells.filter { cell ->
            val matchFilter = when (selectedFilter) {
                StorageFilter.ALL -> true
                StorageFilter.AVAILABLE -> cell.items.size < cell.capacity
                StorageFilter.FULL -> cell.items.size >= cell.capacity
                StorageFilter.EMPTY -> cell.items.isEmpty()
            }
            val matchSearch = if (searchQuery.isBlank()) true else {
                cell.name.contains(searchQuery, true) || 
                cell.description.contains(searchQuery, true) || 
                cell.items.any { it.contains(searchQuery, true) }
            }
            matchFilter && matchSearch
        }.sortedByDescending { it.createdAt }
    }

    LaunchedEffect(isSelectionMode, selectedTab) {
        if (isSelectionMode || selectedTab != StorageTab.CELLS) {
            setTopBarActions { }
        }
    }

    LaunchedEffect(viewModel.statusMessage) {
        viewModel.statusMessage.collect { message ->
            if (message.isNotBlank()) {
                snackbarHostState.showSnackbar(message)
                viewModel.clearStatusMessage()
            }
        }
    }

    ScreenShell {
        AppBackground {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    if (isSelectionMode) {
                        SelectionTopAppBar(
                            selectedCount = selectedCellIds.size,
                            onCloseClick = { exitSelectionMode() },
                            onDeleteClick = { showBulkDeleteConfirmDialog = true }
                        )
                    } else {
                        CenterAlignedTopAppBar(
                            title = { Text("Склад", color = StardustTextPrimary, fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null, tint = StardustTextPrimary) }
                            },
                            actions = {
                                if (!isSelectionMode && selectedTab == StorageTab.CELLS) {
                                    IconButton(onClick = { showLogSheet = true }) {
                                        Icon(Icons.Default.History, null, tint = StardustTextPrimary)
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                        )
                    }
                },
                floatingActionButton = {
                    if (!isSelectionMode && selectedTab == StorageTab.CELLS) {
                        FloatingActionButton(
                            onClick = { showCreateDialog = true },
                            containerColor = StardustPrimary,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(18.dp),
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 12.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Добавить ячейку",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    // Tab Row
                    TabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = StardustPrimary,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                                color = StardustPrimary
                            )
                        }
                    ) {
                        StorageTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.title, color = if (selectedTab == tab) StardustTextPrimary else StardustTextSecondary) }
                            )
                        }
                    }

                    if (selectedTab == StorageTab.CELLS) {
                        // Header
                        StorageSummaryHeader(
                            totalCells = uiState.cells.size,
                            totalScooters = uiState.cells.sumOf { it.items.size },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        // Search and Filter
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            StorageInputField(
                                value = searchQuery,
                                onChange = { searchQuery = it },
                                placeholder = "Поиск ячейки или самоката...",
                                icon = Icons.Default.Search
                            )
                            Spacer(Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(StorageFilter.entries) { filter ->
                                    val isSelected = selectedFilter == filter
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedFilter = filter },
                                        label = { Text(filter.title) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = StardustPrimary.copy(alpha = 0.2f),
                                            selectedLabelColor = StardustPrimary,
                                            containerColor = StardustGlassBg,
                                            labelColor = StardustTextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = StardustItemBg,
                                            selectedBorderColor = StardustPrimary,
                                            enabled = true, selected = isSelected
                                        )
                                    )
                                }
                            }
                        }

                        // Grid
                        if (uiState.cells.isEmpty() && !uiState.isLoading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Inventory2, null, modifier = Modifier.size(64.dp), tint = StardustTextSecondary.copy(alpha = 0.2f))
                                    Text("Склад пуст", color = StardustTextSecondary)
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(filteredCells, key = { it.id }) { cell ->
                                    val isSelected = selectedCellIds.contains(cell.id)
                                    StorageCellTile(
                                        cell = cell,
                                        searchQuery = searchQuery,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (isSelectionMode) {
                                                selectedCellIds = if (isSelected) selectedCellIds - cell.id else selectedCellIds + cell.id
                                                if (selectedCellIds.isEmpty()) isSelectionMode = false
                                            } else {
                                                selectedCellForDetails = cell
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelectionMode) {
                                                isSelectionMode = true
                                                selectedCellIds = setOf(cell.id)
                                            } else {
                                                cellForContextMenu = cell
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        StorageHubScreen(viewModel = viewModel)
                    }
                }
            }

            // Overlay Loading
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StardustPrimary)
                }
            }
        }
    }

    // Dialogs & Sheets
    if (showCreateDialog) {
        CreateCellDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { desc, cap -> viewModel.createNewCell(desc, cap); showCreateDialog = false }
        )
    }

    if (selectedCellForDetails != null) {
        CellDetailsSheet(
            cell = selectedCellForDetails!!,
            sheetState = bottomSheetState,
            viewModel = viewModel,
            storageExportManager = storageExportManager,
            searchQuery = searchQuery,
            onDismiss = { scope.launch { bottomSheetState.hide() }.invokeOnCompletion { selectedCellForDetails = null } },
            onEditClick = { cellToEdit = selectedCellForDetails; scope.launch { bottomSheetState.hide() }.invokeOnCompletion { selectedCellForDetails = null } },
            onDeleteClick = { cellToDelete = selectedCellForDetails; scope.launch { bottomSheetState.hide() }.invokeOnCompletion { selectedCellForDetails = null } }
        )
    }

    if (showLogSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLogSheet = false },
            sheetState = logSheetState,
            containerColor = StardustModalBg,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            StorageActivityLogFullView(
                logEntries = uiState.activityLog,
                isAdmin = isAdmin,
                onClearLogClick = { showClearLogDialog = true }
            )
        }
    }

    if (cellToEdit != null) {
        EditCellDialog(
            cell = cellToEdit!!,
            onDismiss = { cellToEdit = null },
            onSave = { id, desc, cap -> viewModel.updateCell(id, desc, cap); cellToEdit = null }
        )
    }

    if (cellToDelete != null) {
        DeleteCellDialog(
            cell = cellToDelete!!,
            onDismiss = { cellToDelete = null },
            onConfirm = { viewModel.deleteCell(it); cellToDelete = null }
        )
    }

    if (cellForBulkAdd != null) {
        BulkAddScootersDialog(
            cell = cellForBulkAdd!!,
            onDismiss = { cellForBulkAdd = null },
            onAdd = { cell, text -> viewModel.bulkAddScooters(cell, text); cellForBulkAdd = null }
        )
    }

    if (showClearLogDialog) {
        ClearLogDialog(
            onDismiss = { showClearLogDialog = false },
            onConfirm = { viewModel.clearStorageActivityLog(); showClearLogDialog = false }
        )
    }

    if (showBulkDeleteConfirmDialog) {
        BulkDeleteConfirmDialog(
            count = selectedCellIds.size,
            onDismiss = { showBulkDeleteConfirmDialog = false },
            onConfirm = {
                val cellsToDeleteList = uiState.cells.filter { selectedCellIds.contains(it.id) }
                viewModel.deleteCells(cellsToDeleteList)
                showBulkDeleteConfirmDialog = false
                exitSelectionMode()
            }
        )
    }

    if (cellForContextMenu != null) {
        CellContextMenuSheet(
            cell = cellForContextMenu!!,
            onDismiss = { cellForContextMenu = null },
            onOpen = { selectedCellForDetails = cellForContextMenu; cellForContextMenu = null },
            onEdit = { cellToEdit = cellForContextMenu; cellForContextMenu = null },
            onBulkAdd = { cellForBulkAdd = cellForContextMenu; cellForContextMenu = null },
            onSelect = { 
                if (!isSelectionMode) isSelectionMode = true
                selectedCellIds = selectedCellIds + cellForContextMenu!!.id
                cellForContextMenu = null 
            },
            onDelete = { cellToDelete = cellForContextMenu; cellForContextMenu = null }
        )
    }

    if (foundScooterData != null) {
        ScooterSearchResultDialog(
            scooterNumber = foundScooterData!!.first,
            locationName = foundScooterData!!.second,
            lastUser = "Система",
            onDismiss = { foundScooterData = null },
            onNavigate = { foundScooterData = null }
        )
    }
}
