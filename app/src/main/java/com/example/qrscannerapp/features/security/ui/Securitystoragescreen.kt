package com.example.qrscannerapp.features.security.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

// ============================================================================================
// SECURITY STORAGE SCREEN
// ============================================================================================

private enum class StorageTab(val label: String) {
    BESTUZH("Бестужевская"),
    SOFIY("Ферма"),
    ALL("Все ячейки")
}

@Composable
fun SecurityStorageScreen(
    viewModel: SecurityViewModel,
    onMenuClick: () -> Unit
) {
    val state       by viewModel.storageState.collectAsState()
    val isOperating by viewModel.isOperating.collectAsState()
    var activeTab        by remember { mutableStateOf(StorageTab.ALL) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var expandedCellId   by remember { mutableStateOf<String?>(null) }
    var searchQuery      by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearStorageMessage()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearStorageMessage()
        }
    }

    val filteredCells = remember(activeTab, state.cells, searchQuery) {
        val base = when (activeTab) {
            StorageTab.BESTUZH -> state.cells.filter { it.hub == SecurityHubs.BESTUZH }
            StorageTab.SOFIY   -> state.cells.filter { it.hub == SecurityHubs.SOFIY }
            StorageTab.ALL     -> state.cells
        }
        if (searchQuery.isBlank()) base
        else base.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.items.any { code -> code.contains(searchQuery, ignoreCase = true) }
        }
    }

    val totalItems    = filteredCells.sumOf { it.items.size }
    val totalCapacity = filteredCells.sumOf { it.capacity }

    Box(modifier = Modifier.fillMaxSize().background(SecColors.Bg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Шапка
            SecTopBar(
                title       = "Склад СБ",
                subtitle    = "$totalItems / $totalCapacity ед. занято",
                onMenuClick = onMenuClick,
                trailingContent = {
                    IconButton(
                        onClick  = { showCreateDialog = true },
                        modifier = Modifier.size(38.dp).background(SecColors.AccentDim, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, null, tint = SecColors.Accent, modifier = Modifier.size(20.dp))
                    }
                }
            )

            // Вкладки
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .background(SecColors.Bg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StorageTab.entries.forEach { tab ->
                    val isActive = tab == activeTab
                    val badge = when (tab) {
                        StorageTab.BESTUZH -> state.cells.count { it.hub == SecurityHubs.BESTUZH }
                        StorageTab.SOFIY   -> state.cells.count { it.hub == SecurityHubs.SOFIY }
                        StorageTab.ALL     -> state.cells.size
                    }
                    val tabIcon = when (tab) {
                        StorageTab.BESTUZH -> Icons.Default.Apartment
                        StorageTab.SOFIY   -> Icons.Outlined.Warehouse
                        StorageTab.ALL     -> Icons.Default.GridView
                    }
                    Surface(
                        onClick  = { activeTab = tab },
                        shape    = RoundedCornerShape(10.dp),
                        color    = if (isActive) SecColors.Accent else SecColors.Card,
                        border   = if (!isActive) BorderStroke(1.dp, SecColors.CardBorder) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier              = Modifier.padding(horizontal = 6.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(
                                tabIcon, null,
                                tint     = if (isActive) Color.White else SecColors.TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text       = tab.label,
                                fontSize   = 12.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color      = if (isActive) Color.White else SecColors.TextSecondary,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis
                            )
                            if (badge > 0) {
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    modifier         = Modifier
                                        .background(
                                            if (isActive) Color.White.copy(alpha = 0.3f) else SecColors.Accent,
                                            CircleShape
                                        )
                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(badge.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Суммарная статистика
            StorageSummaryBar(cells = filteredCells)

            // Поиск
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(SecColors.Card, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = SecColors.TextMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine    = true,
                    textStyle     = TextStyle(color = SecColors.TextPrimary, fontSize = 14.sp),
                    cursorBrush   = SolidColor(SecColors.Accent),
                    decorationBox = { inner ->
                        Box {
                            if (searchQuery.isEmpty()) Text("Ячейка или код оборудования...", color = SecColors.TextMuted, fontSize = 14.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f).padding(vertical = 10.dp)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = SecColors.TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(color = SecColors.Divider)

            // Список
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> SecLoadingState()
                    filteredCells.isEmpty() -> SecEmptyState(
                        text    = "Ячеек нет",
                        subtext = "Нажмите + чтобы создать ячейку"
                    )
                    else -> LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items = filteredCells, key = { it.id }) { cell ->
                            StorageCellCard(
                                cell         = cell,
                                isExpanded   = expandedCellId == cell.id,
                                onExpand     = { expandedCellId = if (expandedCellId == cell.id) null else cell.id },
                                onAddItem    = { code -> viewModel.addItemToStorageCell(cell.id, code) },
                                onRemoveItem = { code -> viewModel.removeItemFromStorageCell(cell.id, code) },
                                onDelete     = { viewModel.deleteStorageCell(cell.id) },
                                isOperating  = isOperating
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }

    if (showCreateDialog) {
        CreateStorageCellDialog(
            isLoading = isOperating,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, hub, capacity, description ->
                viewModel.createStorageCell(name, hub, capacity, description)
                showCreateDialog = false
            }
        )
    }
}

// ============================================================================================
// СУММАРНАЯ СТАТИСТИКА
// ============================================================================================

@Composable
private fun StorageSummaryBar(cells: List<SecurityStorageCell>) {
    val total     = cells.sumOf { it.items.size }
    val capacity  = cells.sumOf { it.capacity }
    val fillPct   = if (capacity > 0) total.toFloat() / capacity else 0f
    val fillColor = when {
        fillPct > 0.9f -> SecColors.Danger
        fillPct > 0.7f -> SecColors.Warning
        else           -> SecColors.Success
    }

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(SecColors.Card)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Заполнено", color = SecColors.TextMuted, fontSize = 11.sp)
                Text("$total / $capacity", color = fillColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(SecColors.CardBorder, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillPct.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(fillColor.copy(alpha = 0.7f), fillColor)),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(cells.size.toString(), color = SecColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("Ячеек", color = SecColors.TextMuted, fontSize = 10.sp)
        }
    }
}

// ============================================================================================
// КАРТОЧКА ЯЧЕЙКИ
// ============================================================================================

@Composable
private fun StorageCellCard(
    cell: SecurityStorageCell,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onAddItem: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onDelete: () -> Unit,
    isOperating: Boolean
) {
    val fillPct   = if (cell.capacity > 0) cell.items.size.toFloat() / cell.capacity else 0f
    val fillColor = when {
        fillPct > 0.9f -> SecColors.Danger
        fillPct > 0.7f -> SecColors.Warning
        else           -> SecColors.Success
    }
    val hubIcon = if (cell.hub == SecurityHubs.BESTUZH) Icons.Default.Apartment else Icons.Outlined.Warehouse

    var addItemText      by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = SecColors.Card,
        border   = BorderStroke(1.dp, if (isExpanded) SecColors.Accent.copy(alpha = 0.4f) else SecColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Заголовок
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clickable { onExpand() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier
                        .size(40.dp)
                        .background(SecColors.AccentDim, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Inventory, null, tint = SecColors.Accent, modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = cell.name,
                            color      = SecColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                            modifier   = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        // Хаб-бейдж с иконкой
                        Surface(
                            shape  = RoundedCornerShape(5.dp),
                            color  = SecColors.TagBg,
                            border = BorderStroke(1.dp, SecColors.CardBorder)
                        ) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(hubIcon, null, tint = SecColors.TextMuted, modifier = Modifier.size(10.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    SecurityHubs.displayName(cell.hub).split(" ").first(),
                                    color    = SecColors.TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(3.dp)
                                .background(SecColors.CardBorder, RoundedCornerShape(2.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fillPct.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(fillColor, RoundedCornerShape(2.dp))
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("${cell.items.size}/${cell.capacity}", color = SecColors.TextSecondary, fontSize = 12.sp)
                        if (!cell.description.isNullOrBlank()) {
                            Text(" · ${cell.description}", color = SecColors.TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint     = SecColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Раскрытое содержимое
            AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 14.dp)
                ) {
                    HorizontalDivider(color = SecColors.Divider)
                    Spacer(Modifier.height(12.dp))

                    // Поле добавления
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value         = addItemText,
                            onValueChange = { addItemText = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                            modifier      = Modifier.weight(1f),
                            placeholder   = { Text("Код оборудования...", color = SecColors.TextMuted, fontSize = 13.sp) },
                            singleLine    = true,
                            shape         = RoundedCornerShape(10.dp),
                            colors        = storageFieldColors(),
                            textStyle     = LocalTextStyle.current.copy(fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        )
                        FilledIconButton(
                            onClick  = {
                                if (addItemText.isNotBlank()) { onAddItem(addItemText.trim()); addItemText = "" }
                            },
                            enabled  = addItemText.isNotBlank() && !isOperating,
                            shape    = RoundedCornerShape(10.dp),
                            colors   = IconButtonDefaults.filledIconButtonColors(containerColor = SecColors.Accent),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Список позиций
                    if (cell.items.isEmpty()) {
                        Box(
                            modifier         = Modifier
                                .fillMaxWidth()
                                .background(SecColors.TagBg, RoundedCornerShape(10.dp))
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Inventory, null, tint = SecColors.TextMuted, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Ячейка пуста", color = SecColors.TextMuted, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            Icon(Icons.Outlined.QrCode2, null, tint = SecColors.TextMuted, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "СОДЕРЖИМОЕ (${cell.items.size})",
                                color         = SecColors.TextMuted,
                                fontSize      = 11.sp,
                                fontWeight    = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            cell.items.forEach { code ->
                                StorageItemRow(code = code, onRemove = { onRemoveItem(code) })
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Удалить ячейку
                    if (showDeleteConfirm) {
                        Surface(
                            shape  = RoundedCornerShape(10.dp),
                            color  = SecColors.Danger.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, SecColors.Danger.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier              = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DeleteForever, null, tint = SecColors.Danger, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Удалить ячейку?", color = SecColors.Danger, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                        Text("Нет", color = SecColors.TextSecondary)
                                    }
                                    Button(
                                        onClick  = { onDelete(); showDeleteConfirm = false },
                                        colors   = ButtonDefaults.buttonColors(containerColor = SecColors.Danger),
                                        shape    = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Text("Да", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick        = { showDeleteConfirm = true },
                            modifier       = Modifier.fillMaxWidth(),
                            shape          = RoundedCornerShape(10.dp),
                            border         = BorderStroke(1.dp, SecColors.Danger.copy(alpha = 0.4f)),
                            colors         = ButtonDefaults.outlinedButtonColors(contentColor = SecColors.Danger),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Удалить ячейку", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================================
// СТРОКА ПОЗИЦИИ
// ============================================================================================

@Composable
private fun StorageItemRow(code: String, onRemove: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(SecColors.TagBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.QrCode2, null, tint = SecColors.TextMuted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text       = code,
            color      = SecColors.TextPrimary,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            modifier   = Modifier.weight(1f)
        )
        if (showConfirm) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showConfirm = false }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, tint = SecColors.TextMuted, modifier = Modifier.size(14.dp))
                }
                IconButton(
                    onClick  = { onRemove(); showConfirm = false },
                    modifier = Modifier.size(28.dp).background(SecColors.Danger.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.Check, null, tint = SecColors.Danger, modifier = Modifier.size(14.dp))
                }
            }
        } else {
            IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.RemoveCircleOutline, null, tint = SecColors.TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ============================================================================================
// ДИАЛОГ СОЗДАНИЯ ЯЧЕЙКИ
// ============================================================================================

@Composable
private fun CreateStorageCellDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, hub: String, capacity: Int, description: String?) -> Unit
) {
    var name        by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedHub by remember { mutableStateOf(SecurityHubs.BESTUZH) }
    var capacityStr by remember { mutableStateOf("50") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor   = SecColors.Card,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Inventory, null, tint = SecColors.Accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Новая ячейка", color = SecColors.TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Название ячейки", color = SecColors.TextSecondary) },
                    placeholder   = { Text("Шкаф А-1", color = SecColors.TextMuted) },
                    leadingIcon   = { Icon(Icons.Outlined.Inventory, null, tint = SecColors.TextMuted) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = storageAlertFieldColors(),
                    modifier      = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value         = description,
                    onValueChange = { description = it },
                    label         = { Text("Описание (опционально)", color = SecColors.TextSecondary) },
                    leadingIcon   = { Icon(Icons.Default.Notes, null, tint = SecColors.TextMuted) },
                    maxLines      = 2,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = storageAlertFieldColors(),
                    modifier      = Modifier.fillMaxWidth()
                )

                // Хаб — иконки вместо эмодзи
                Text("ХАБ", color = SecColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        SecurityHubs.BESTUZH to Icons.Default.Apartment,
                        SecurityHubs.SOFIY   to Icons.Outlined.Warehouse
                    ).forEach { (hub, icon) ->
                        val isSelected = hub == selectedHub
                        Surface(
                            onClick  = { selectedHub = hub },
                            shape    = RoundedCornerShape(12.dp),
                            color    = if (isSelected) SecColors.Accent.copy(alpha = 0.15f) else SecColors.TagBg,
                            border   = BorderStroke(1.dp, if (isSelected) SecColors.Accent.copy(alpha = 0.5f) else SecColors.CardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    icon, null,
                                    tint     = if (isSelected) SecColors.Accent else SecColors.TextMuted,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    SecurityHubs.displayName(hub).split(" ").first(),
                                    color      = if (isSelected) SecColors.Accent else SecColors.TextSecondary,
                                    fontSize   = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign  = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value         = capacityStr,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) capacityStr = it },
                    label         = { Text("Ёмкость (макс. кол-во)", color = SecColors.TextSecondary) },
                    leadingIcon   = { Icon(Icons.Default.Storage, null, tint = SecColors.TextMuted) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = storageAlertFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = {
                    onConfirm(name.trim(), selectedHub, capacityStr.toIntOrNull() ?: 50, description.ifBlank { null })
                },
                enabled  = name.isNotBlank() && !isLoading,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SecColors.Accent)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Создать", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Отмена", color = SecColors.TextSecondary)
            }
        }
    )
}

// ============================================================================================
// ЦВЕТА ПОЛЕЙ
// ============================================================================================

@Composable
private fun storageFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = SecColors.Accent,
    unfocusedBorderColor    = SecColors.CardBorder,
    focusedContainerColor   = SecColors.Bg,
    unfocusedContainerColor = SecColors.Bg,
    cursorColor             = SecColors.Accent,
    focusedTextColor        = SecColors.TextPrimary,
    unfocusedTextColor      = SecColors.TextPrimary
)

@Composable
private fun storageAlertFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = SecColors.Accent,
    unfocusedBorderColor    = SecColors.CardBorder,
    focusedContainerColor   = SecColors.TagBg,
    unfocusedContainerColor = SecColors.TagBg,
    cursorColor             = SecColors.Accent,
    focusedTextColor        = SecColors.TextPrimary,
    unfocusedTextColor      = SecColors.TextPrimary,
    focusedLabelColor       = SecColors.Accent,
    unfocusedLabelColor     = SecColors.TextSecondary
)