package com.example.qrscannerapp.features.inventory.ui.distribution

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.qrscannerapp.StardustWarning
import com.example.qrscannerapp.features.inventory.data.export.PalletSummaryPdfGenerator
import com.example.qrscannerapp.features.inventory.domain.model.CellStatus
import com.example.qrscannerapp.features.inventory.domain.model.PalletActivityLogEntry
import com.example.qrscannerapp.features.inventory.domain.model.StoragePallet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private enum class DetailTab(val label: String, val icon: ImageVector) {
    LIST("Список", Icons.Outlined.ViewList),
    OPERATIONS("Операции", Icons.Outlined.History),
    PASSPORT("Паспорт", Icons.Outlined.Badge)
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

    var activeTab by remember { mutableStateOf(DetailTab.LIST) }
    var isExportMenuExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showErrorsOnly by remember { mutableStateOf(initialFilterErrors) }

    // Загрузка лога при открытии
    val cellLog by viewModel.cellActivityLog.collectAsState()
    val isCellLogLoading by viewModel.isCellLogLoading.collectAsState()

    LaunchedEffect(pallet.id) {
        viewModel.loadCellActivityLog(pallet.id)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.stopCellActivityLog() }
    }

    val filteredItems = remember(pallet.items, searchQuery, showErrorsOnly, errorItems) {
        var items = pallet.items.asReversed()
        if (showErrorsOnly) items = items.filter { it in errorItems }
        if (searchQuery.isNotEmpty()) items = items.filter { it.contains(searchQuery, ignoreCase = true) }
        items
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StardustModalBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // Нижняя панель — Экспорт / Удалить
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StardustModalBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { isExportMenuExpanded = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Экспорт", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StardustError.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StardustError)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Удалить", fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
            ) {
                // ====================================================================
                // ШАПКА — Название + счётчик
                // ====================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Содержимое",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = StardustTextPrimary
                        )
                        Text(
                            pallet.resolvedDisplayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = StardustTextSecondary
                        )
                    }
                    // Счётчик items/capacity
                    val fillColor = when {
                        pallet.fillProgress >= 1f -> StardustError
                        pallet.fillProgress >= 0.8f -> StardustWarning
                        else -> StardustSuccess
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = fillColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, fillColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            "${pallet.items.size}/${pallet.capacity}",
                            color = fillColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // ====================================================================
                // КАРТОЧКА СОЗДАТЕЛЯ + ДАТА (как на скриншоте)
                // ====================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .background(StardustGlassBg, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Аватар-инициалы
                    val initials = pallet.creatorName
                        ?.split(" ")
                        ?.take(2)
                        ?.map { it.firstOrNull()?.uppercase() ?: "" }
                        ?.joinToString("")
                        ?: "?"
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StardustPrimary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            initials,
                            color = StardustPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            pallet.creatorName ?: "Неизвестно",
                            color = StardustTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            "Создатель",
                            color = StardustTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Разделитель
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(StardustTextSecondary.copy(alpha = 0.2f))
                    )
                    Spacer(Modifier.width(12.dp))

                    // Дата создания
                    Icon(
                        Icons.Default.CalendarToday,
                        null,
                        tint = StardustTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            "Создана",
                            color = StardustTextSecondary,
                            fontSize = 10.sp
                        )
                        Text(
                            pallet.createdAt?.let {
                                SimpleDateFormat("dd.MM в HH:mm", Locale.getDefault()).format(it)
                            } ?: "—",
                            color = StardustTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ====================================================================
                // ВКЛАДКИ: Список | Операции | Паспорт
                // ====================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailTab.entries.forEach { tab ->
                        val isSelected = activeTab == tab
                        val count = when (tab) {
                            DetailTab.LIST -> pallet.items.size
                            DetailTab.OPERATIONS -> cellLog.size
                            DetailTab.PASSPORT -> null
                        }
                        Surface(
                            onClick = { activeTab = tab },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) StardustPrimary.copy(alpha = 0.2f) else StardustItemBg,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                1.dp, StardustPrimary.copy(alpha = 0.4f)
                            ) else null,
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    tab.icon, null,
                                    tint = if (isSelected) StardustPrimary else StardustTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (count != null) "${tab.label} ($count)" else tab.label,
                                    color = if (isSelected) StardustPrimary else StardustTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ====================================================================
                // КОНТЕНТ ВКЛАДОК
                // ====================================================================
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_content"
                ) { tab ->
                    when (tab) {
                        DetailTab.LIST -> ListTabContent(
                            pallet = pallet,
                            filteredItems = filteredItems,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            showErrorsOnly = showErrorsOnly,
                            onToggleErrorsOnly = { showErrorsOnly = !showErrorsOnly },
                            errorItems = errorItems,
                            viewModel = viewModel,
                            snackbarHostState = snackbarHostState,
                            scope = scope
                        )
                        DetailTab.OPERATIONS -> OperationsTabContent(
                            cellLog = cellLog,
                            isLoading = isCellLogLoading
                        )
                        DetailTab.PASSPORT -> PassportTabContent(
                            pallet = pallet,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    // Export dropdown
    Box {
        DropdownMenu(
            expanded = isExportMenuExpanded,
            onDismissRequest = { isExportMenuExpanded = false },
            modifier = Modifier.background(StardustGlassBg)
        ) {
            DropdownMenuItem(
                text = { Text("Excel", color = StardustTextPrimary) },
                leadingIcon = { Icon(Icons.Default.TableChart, null, tint = StardustSecondary) },
                onClick = { isExportMenuExpanded = false; exportPalletToExcel(context, pallet) }
            )
            DropdownMenuItem(
                text = { Text("PDF", color = StardustTextPrimary) },
                leadingIcon = { Icon(Icons.Default.Description, null, tint = StardustPrimary) },
                onClick = {
                    isExportMenuExpanded = false
                    PalletSummaryPdfGenerator.generateAndShare(context, listOf(pallet), userName)
                }
            )
        }
    }
}

// =============================================================================
// ВКЛАДКА 1: СПИСОК АКБ
// =============================================================================
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ListTabContent(
    pallet: StoragePallet,
    filteredItems: List<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showErrorsOnly: Boolean,
    onToggleErrorsOnly: () -> Unit,
    errorItems: List<String>,
    viewModel: QrScannerViewModel,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(Modifier.padding(horizontal = 16.dp)) {
        if (errorItems.isNotEmpty()) {
            FilterChip(
                selected = showErrorsOnly,
                onClick = onToggleErrorsOnly,
                label = { Text("Только ошибки (${errorItems.size})") },
                leadingIcon = {
                    if (showErrorsOnly) Icon(Icons.Default.FactCheck, null, modifier = Modifier.size(16.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StardustError.copy(alpha = 0.2f),
                    selectedLabelColor = StardustError,
                    labelColor = StardustTextSecondary
                )
            )
            Spacer(Modifier.height(4.dp))
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Поиск по ID...", color = StardustTextSecondary.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = StardustTextSecondary) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                { IconButton(onClick = { onSearchQueryChange("") }) { Icon(Icons.Default.Clear, null, tint = StardustTextSecondary) } }
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
        Spacer(Modifier.height(8.dp))

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (showErrorsOnly) "Ошибок нет" else "Ничего не найдено",
                    color = StardustTextSecondary
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                itemsIndexed(filteredItems) { index, batteryId ->
                    val displayIndex = if (searchQuery.isEmpty() && !showErrorsOnly) pallet.items.size - index else index + 1
                    val backgroundColor = if (index % 2 == 0) Color.Transparent else StardustTextSecondary.copy(alpha = 0.05f)
                    val isErrorItem = batteryId in errorItems
                    val textColor = if (isErrorItem) StardustError else StardustTextPrimary

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundColor)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    clipboardManager.setText(AnnotatedString(batteryId))
                                    Toast.makeText(context, "ID скопирован", Toast.LENGTH_SHORT).show()
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.QrCode2, null,
                            tint = StardustTextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            batteryId,
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = if (isErrorItem) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isErrorItem) {
                            Icon(
                                Icons.Default.Warning, null,
                                tint = StardustError,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        IconButton(
                            onClick = {
                                viewModel.removeItemFromPallet(pallet.id, batteryId)
                                scope.launch {
                                    val res = snackbarHostState.showSnackbar("АКБ удален", "ВЕРНУТЬ")
                                    if (res == SnackbarResult.ActionPerformed)
                                        viewModel.distributeSpecificItemToPallet(pallet, batteryId)
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = StardustError.copy(alpha = 0.5f))
                        }
                    }
                    HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.08f), thickness = 0.5.dp)
                }
            }
        }
    }
}

// =============================================================================
// ВКЛАДКА 2: ОПЕРАЦИИ (индивидуальный лог ячейки)
// =============================================================================
@Composable
private fun OperationsTabContent(
    cellLog: List<PalletActivityLogEntry>,
    isLoading: Boolean
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        // Заголовок в стиле терминала
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StardustItemBg, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Три точки (красная, жёлтая, зелёная)
            Box(Modifier.size(8.dp).clip(CircleShape).background(StardustError))
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(StardustWarning))
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(StardustSuccess))
            Spacer(Modifier.width(10.dp))
            Text(
                "cell_operations.log",
                color = StardustTextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${cellLog.size} записей",
                color = StardustTextSecondary.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }

        // Контент лога
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFF0D1117),
                    RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                )
                .heightIn(min = 150.dp, max = 400.dp)
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = StardustPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                cellLog.isEmpty() -> {
                    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Операций пока нет",
                            color = StardustTextSecondary.copy(alpha = 0.4f),
                            fontSize = 13.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(cellLog) { _, entry ->
                            CellLogEntry(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CellLogEntry(entry: PalletActivityLogEntry) {
    val (actionText, actionColor, countText) = when (entry.action) {
        "CREATED" -> Triple("+ Создал ячейку", StardustSuccess, "")
        "DISTRIBUTED" -> Triple(
            "+ Добавил ${entry.itemCount ?: 0} АКБ",
            StardustSuccess,
            "+${entry.itemCount ?: 0}"
        )
        "REMOVED_ITEM" -> Triple(
            "- Удалил АКБ",
            StardustError,
            "-1"
        )
        "RESTORED_ITEM" -> Triple(
            "+ Восстановил АКБ",
            StardustWarning,
            "+1"
        )
        "DELETED" -> Triple("× Удалил ячейку", StardustError, "")
        else -> Triple(entry.action, StardustTextSecondary, "")
    }

    val dateStr = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        .format(Date(entry.timestamp))

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$dateStr  $actionText",
                color = actionColor,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            if (countText.isNotEmpty()) {
                Text(
                    countText,
                    color = actionColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Text(
            entry.userName ?: "System",
            color = StardustTextSecondary.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

// =============================================================================
// ВКЛАДКА 3: ПАСПОРТ
// =============================================================================
@Composable
private fun PassportTabContent(
    pallet: StoragePallet,
    viewModel: QrScannerViewModel
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Основная информация
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                PassportRow(
                    icon = Icons.Outlined.Tag,
                    label = "Номер ячейки",
                    value = "№${pallet.palletNumber}"
                )
                PassportDivider()
                PassportRow(
                    icon = Icons.Outlined.Label,
                    label = "Название",
                    value = pallet.displayName ?: "Не задано",
                    valueColor = if (pallet.displayName != null) StardustTextPrimary else StardustTextSecondary.copy(alpha = 0.4f)
                )
                PassportDivider()
                PassportRow(
                    icon = Icons.Default.Factory,
                    label = "Тип АКБ",
                    value = pallet.resolvedCellType?.displayName ?: "Универсальная",
                    valueColor = pallet.resolvedCellType?.let { colorForCellType(it) } ?: StardustTextSecondary
                )
                PassportDivider()
                PassportRow(
                    icon = Icons.Default.Inventory2,
                    label = "Ёмкость",
                    value = "${pallet.items.size} / ${pallet.capacity} шт."
                )
                PassportDivider()

                // Статус с возможностью смены
                val status = pallet.resolvedStatus
                var showStatusMenu by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStatusMenu = true }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.FlagCircle, null,
                        tint = StardustTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Статус", color = StardustTextSecondary, fontSize = 11.sp)
                        Text(
                            "${status.emoji} ${status.displayName}",
                            color = StardustPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight, null,
                        tint = StardustTextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        CellStatus.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.emoji} ${s.displayName}") },
                                onClick = {
                                    viewModel.setCellStatus(pallet.id, s)
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }
                PassportDivider()
                PassportRow(
                    icon = Icons.Outlined.Person,
                    label = "Создатель",
                    value = pallet.creatorName ?: "Неизвестно"
                )
                PassportDivider()
                PassportRow(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "Дата создания",
                    value = pallet.createdAt?.let {
                        SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru")).format(it)
                    } ?: "—"
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Карта / Адрес
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn, null,
                        tint = StardustTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Адрес склада", color = StardustTextSecondary, fontSize = 11.sp)
                        Text(
                            pallet.address ?: "Не указан",
                            color = if (pallet.address != null) StardustTextPrimary else StardustTextSecondary.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }

                // Кнопка "Открыть на карте"
                if (!pallet.address.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        onClick = {
                            try {
                                val encodedAddress = Uri.encode(pallet.address)
                                val gmmIntentUri = Uri.parse("geo:0,0?q=$encodedAddress")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    // Fallback — открыть в браузере
                                    val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedAddress")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Не удалось открыть карту", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = StardustPrimary.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Map, null,
                                tint = StardustPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Открыть на карте",
                                color = StardustPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp)) // Отступ под bottom bar
    }
}

@Composable
private fun PassportRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = StardustTextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint = StardustTextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = StardustTextSecondary, fontSize = 11.sp)
            Text(
                value,
                color = valueColor,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PassportDivider() {
    HorizontalDivider(
        color = StardustTextSecondary.copy(alpha = 0.08f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 30.dp)
    )
}