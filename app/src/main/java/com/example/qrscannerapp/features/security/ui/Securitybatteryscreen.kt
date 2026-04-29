package com.example.qrscannerapp.features.security.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================================
// ТИПЫ ОБОРУДОВАНИЯ
// ============================================================================================

private enum class BatteryTab(val label: String) {
    ACTIVE("Активные"),
    RETURNED("Возвращённые")
}

private enum class EquipmentType(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    BATTERY("battery", "АКБ",      Icons.Default.BatteryChargingFull, Color(0xFF4FC3F7)),
    REVIVER("reviver", "Ревайвер",  Icons.Default.ElectricBolt,        Color(0xFFFFCA28))
}

// ============================================================================================
// SECURITY BATTERY SCREEN
// ============================================================================================

@Composable
fun SecurityBatteryScreen(
    viewModel: SecurityViewModel,
    onMenuClick: () -> Unit
) {
    val state       by viewModel.batteryState.collectAsState()
    val isOperating by viewModel.isOperating.collectAsState()
    var activeTab      by remember { mutableStateOf(BatteryTab.ACTIVE) }
    var showIssueDialog by remember { mutableStateOf(false) }
    var searchQuery    by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearBatteryMessage()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearBatteryMessage()
        }
    }

    val displayList = remember(activeTab, state.activeIssues, state.returnedIssues, searchQuery) {
        val base = when (activeTab) {
            BatteryTab.ACTIVE   -> state.activeIssues
            BatteryTab.RETURNED -> state.returnedIssues
        }
        if (searchQuery.isBlank()) base
        else base.filter {
            it.issuedToName.contains(searchQuery, ignoreCase = true) ||
                    it.items.any { code -> code.contains(searchQuery, ignoreCase = true) }
        }
    }

    val activeBatteries = state.activeIssues.filter { it.type == EquipmentType.BATTERY.key }.sumOf { it.items.size }
    val activeRevivers  = state.activeIssues.filter { it.type == EquipmentType.REVIVER.key }.sumOf { it.items.size }

    Box(modifier = Modifier.fillMaxSize().background(SecColors.Bg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Шапка
            SecTopBar(
                title       = "Оборудование",
                subtitle    = "На руках: $activeBatteries АКБ · $activeRevivers ревайверов",
                onMenuClick = onMenuClick,
                trailingContent = {
                    IconButton(
                        onClick  = { showIssueDialog = true },
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
                BatteryTab.entries.forEach { tab ->
                    val isActive = tab == activeTab
                    val badge = when (tab) {
                        BatteryTab.ACTIVE   -> state.activeIssues.size
                        BatteryTab.RETURNED -> state.returnedIssues.size
                    }
                    Surface(
                        onClick  = { activeTab = tab },
                        shape    = RoundedCornerShape(10.dp),
                        color    = if (isActive) SecColors.Accent else SecColors.Card,
                        border   = if (!isActive) BorderStroke(1.dp, SecColors.CardBorder) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier              = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text       = tab.label,
                                fontSize   = 13.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color      = if (isActive) Color.White else SecColors.TextSecondary,
                                maxLines   = 1
                            )
                            if (badge > 0) {
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier         = Modifier
                                        .background(
                                            if (isActive) Color.White.copy(alpha = 0.3f) else SecColors.Accent,
                                            CircleShape
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(badge.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Сводка
            AnimatedVisibility(
                visible = activeTab == BatteryTab.ACTIVE && state.activeIssues.isNotEmpty(),
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                BatterySummaryRow(
                    batteries = activeBatteries,
                    revivers  = activeRevivers,
                    total     = state.activeIssues.size
                )
            }

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
                            if (searchQuery.isEmpty()) Text("Сотрудник или код АКБ...", color = SecColors.TextMuted, fontSize = 14.sp)
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
                    displayList.isEmpty() -> SecEmptyState(
                        text    = if (activeTab == BatteryTab.ACTIVE) "Нет активных выдач" else "Нет возвращённых",
                        subtext = if (activeTab == BatteryTab.ACTIVE) "Нажмите + чтобы выдать оборудование" else null
                    )
                    else -> LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items = displayList, key = { it.id }) { issue ->
                            EquipmentIssueCard(
                                issue       = issue,
                                showReturn  = activeTab == BatteryTab.ACTIVE,
                                onReturn    = { viewModel.returnEquipment(issue.id) },
                                isOperating = isOperating
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }

                if (activeTab == BatteryTab.ACTIVE) {
                    ExtendedFloatingActionButton(
                        text           = { Text("Выдать", fontWeight = FontWeight.Bold) },
                        icon           = { Icon(Icons.Default.AddCircle, null) },
                        onClick        = { showIssueDialog = true },
                        modifier       = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                        containerColor = SecColors.Accent,
                        contentColor   = Color.White
                    )
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
    }

    if (showIssueDialog) {
        IssueEquipmentDialog(
            isLoading = isOperating,
            onDismiss = { showIssueDialog = false },
            onConfirm = { type, items, toId, toName, hub, notes ->
                viewModel.issueEquipment(type, items, toId, toName, hub, notes)
                showIssueDialog = false
            }
        )
    }
}

// ============================================================================================
// СВОДНАЯ СТРОКА
// ============================================================================================

@Composable
private fun BatterySummaryRow(batteries: Int, revivers: Int, total: Int) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(SecColors.Card)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        BatterySummaryPill(
            icon     = Icons.Default.BatteryChargingFull,
            label    = "АКБ на руках",
            value    = batteries.toString(),
            color    = Color(0xFF4FC3F7),
            modifier = Modifier.weight(1f)
        )
        VerticalDivider(
            modifier = Modifier.height(40.dp).padding(horizontal = 16.dp),
            color    = SecColors.Divider
        )
        BatterySummaryPill(
            icon     = Icons.Default.ElectricBolt,
            label    = "Ревайверы",
            value    = revivers.toString(),
            color    = Color(0xFFFFCA28),
            modifier = Modifier.weight(1f)
        )
        VerticalDivider(
            modifier = Modifier.height(40.dp).padding(horizontal = 16.dp),
            color    = SecColors.Divider
        )
        BatterySummaryPill(
            icon     = Icons.Default.Badge,
            label    = "Сотрудников",
            value    = total.toString(),
            color    = SecColors.Accent,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BatterySummaryPill(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(label, color = SecColors.TextMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

// ============================================================================================
// КАРТОЧКА ВЫДАЧИ
// ============================================================================================

@Composable
private fun EquipmentIssueCard(
    issue: EquipmentIssue,
    showReturn: Boolean,
    onReturn: () -> Unit,
    isOperating: Boolean
) {
    val eqType      = EquipmentType.entries.find { it.key == issue.type } ?: EquipmentType.BATTERY
    val isActive    = issue.status == "active"
    var expanded    by remember { mutableStateOf(false) }
    var confirmReturn by remember { mutableStateOf(false) }
    val elapsed     = rememberElapsed(issue.issuedAt)
    val isOverdue   = isActive && issue.issuedAt > 0L &&
            System.currentTimeMillis() - issue.issuedAt > 12 * 3_600_000L

    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = SecColors.Card,
        border   = BorderStroke(1.dp, if (isActive) eqType.color.copy(alpha = 0.3f) else SecColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Заголовок
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier
                        .size(42.dp)
                        .background(eqType.color.copy(alpha = 0.12f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(eqType.icon, null, tint = eqType.color, modifier = Modifier.size(22.dp))
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text       = issue.issuedToName,
                            color      = SecColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                            modifier   = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = eqType.color.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, eqType.color.copy(alpha = 0.3f))
                        ) {
                            Text(
                                eqType.label,
                                color      = eqType.color,
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${issue.items.size} ед.", color = SecColors.TextSecondary, fontSize = 12.sp)
                        Text("·", color = SecColors.TextMuted, fontSize = 12.sp)
                        // Время — красное если просрочено
                        if (isOverdue) {
                            Icon(Icons.Default.Warning, null, tint = SecColors.Danger, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(2.dp))
                        }
                        Text(
                            elapsed,
                            color    = if (isOverdue) SecColors.Danger else SecColors.TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                        )
                        Text("·", color = SecColors.TextMuted, fontSize = 12.sp)
                        Text(SecurityHubs.displayName(issue.hub).split(" ").first(), color = SecColors.TextMuted, fontSize = 12.sp)
                    }
                }

                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint     = SecColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Раскрытое
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 14.dp)
                ) {
                    HorizontalDivider(color = SecColors.Divider)
                    Spacer(Modifier.height(12.dp))

                    IssueMetaRow("Выдал",    issue.issuedByName, Icons.Default.Badge)
                    IssueMetaRow("Выдано",   formatIssueDate(issue.issuedAt), Icons.Outlined.Schedule)
                    if (!isActive && issue.returnedAt != null) {
                        IssueMetaRow("Возвращено", formatIssueDate(issue.returnedAt), Icons.Default.AssignmentReturn)
                    }
                    if (!issue.notes.isNullOrBlank()) {
                        IssueMetaRow("Заметки", issue.notes, Icons.Default.Notes)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Заголовок списка
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.QrCode2, null, tint = SecColors.TextMuted, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "ПОЗИЦИИ (${issue.items.size})",
                            color         = SecColors.TextMuted,
                            fontSize      = 11.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        issue.items.forEach { code ->
                            Row(
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .background(SecColors.TagBg, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.QrCode2, null, tint = eqType.color.copy(alpha = 0.6f), modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(code, color = SecColors.TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    // Кнопка возврата
                    if (showReturn) {
                        Spacer(Modifier.height(14.dp))
                        if (confirmReturn) {
                            Surface(
                                shape  = RoundedCornerShape(10.dp),
                                color  = SecColors.Success.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, SecColors.Success.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier              = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.HelpOutline, null, tint = SecColors.Success, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Подтвердить возврат?", color = SecColors.Success, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { confirmReturn = false }) {
                                            Text("Нет", color = SecColors.TextSecondary)
                                        }
                                        Button(
                                            onClick  = { onReturn(); confirmReturn = false },
                                            enabled  = !isOperating,
                                            colors   = ButtonDefaults.buttonColors(containerColor = SecColors.Success),
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
                                onClick  = { confirmReturn = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(10.dp),
                                border   = BorderStroke(1.dp, SecColors.Success.copy(alpha = 0.5f)),
                                colors   = ButtonDefaults.outlinedButtonColors(contentColor = SecColors.Success),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.AssignmentReturn, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Зафиксировать возврат", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueMetaRow(label: String, value: String, icon: ImageVector? = null) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = SecColors.TextMuted, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = SecColors.TextMuted, fontSize = 12.sp, modifier = Modifier.width(90.dp))
        Text(value, color = SecColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ============================================================================================
// ДИАЛОГ ВЫДАЧИ
// ============================================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IssueEquipmentDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (type: String, items: List<String>, toId: String, toName: String, hub: String, notes: String?) -> Unit
) {
    var selectedType by remember { mutableStateOf(EquipmentType.BATTERY) }
    var toName       by remember { mutableStateOf("") }
    var toId         by remember { mutableStateOf("") }
    var notes        by remember { mutableStateOf("") }
    var selectedHub  by remember { mutableStateOf(SecurityHubs.BESTUZH) }
    var itemInput    by remember { mutableStateOf("") }
    val itemsList    = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor   = SecColors.Card,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddCircle, null, tint = SecColors.Accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Выдать оборудование", color = SecColors.TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier            = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                // Тип оборудования
                Text("ТИП ОБОРУДОВАНИЯ", color = SecColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EquipmentType.entries.forEach { type ->
                        val isSelected = type == selectedType
                        Surface(
                            onClick  = { selectedType = type },
                            shape    = RoundedCornerShape(12.dp),
                            color    = if (isSelected) type.color.copy(alpha = 0.15f) else SecColors.TagBg,
                            border   = BorderStroke(1.dp, if (isSelected) type.color.copy(alpha = 0.5f) else SecColors.CardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    type.icon, null,
                                    tint     = if (isSelected) type.color else SecColors.TextMuted,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    type.label,
                                    color      = if (isSelected) type.color else SecColors.TextSecondary,
                                    fontSize   = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Кому
                OutlinedTextField(
                    value         = toName,
                    onValueChange = { toName = it },
                    label         = { Text("Имя сотрудника", color = SecColors.TextSecondary) },
                    leadingIcon   = { Icon(Icons.Default.Badge, null, tint = SecColors.TextMuted) },
                    placeholder   = { Text("Иванов Иван", color = SecColors.TextMuted) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = secTextFieldColors(),
                    modifier      = Modifier.fillMaxWidth()
                )

                // ID
                OutlinedTextField(
                    value         = toId,
                    onValueChange = { toId = it },
                    label         = { Text("ID сотрудника (опционально)", color = SecColors.TextSecondary) },
                    leadingIcon   = { Icon(Icons.Outlined.Person, null, tint = SecColors.TextMuted) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = secTextFieldColors(),
                    modifier      = Modifier.fillMaxWidth()
                )

                // Хаб
                Text("ХАБ", color = SecColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(SecurityHubs.BESTUZH, SecurityHubs.SOFIY).forEach { hub ->
                        val isSelected = hub == selectedHub
                        Surface(
                            onClick  = { selectedHub = hub },
                            shape    = RoundedCornerShape(10.dp),
                            color    = if (isSelected) SecColors.Accent else SecColors.TagBg,
                            border   = if (!isSelected) BorderStroke(1.dp, SecColors.CardBorder) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Warehouse, null,
                                    tint     = if (isSelected) Color.White else SecColors.TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    SecurityHubs.displayName(hub).split(" ").first(),
                                    color      = if (isSelected) Color.White else SecColors.TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize   = 13.sp
                                )
                            }
                        }
                    }
                }

                // Коды оборудования
                Text(
                    "КОДЫ ОБОРУДОВАНИЯ${if (itemsList.isNotEmpty()) " (${itemsList.size})" else ""}",
                    color         = SecColors.TextMuted,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value         = itemInput,
                        onValueChange = { itemInput = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                        placeholder   = { Text("Код АКБ / ревайвера...", color = SecColors.TextMuted, fontSize = 12.sp) },
                        singleLine    = true,
                        shape         = RoundedCornerShape(10.dp),
                        colors        = secTextFieldColors(),
                        modifier      = Modifier.weight(1f)
                    )
                    FilledIconButton(
                        onClick  = {
                            val code = itemInput.trim()
                            if (code.isNotBlank() && code !in itemsList) { itemsList.add(code); itemInput = "" }
                        },
                        enabled  = itemInput.isNotBlank(),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = IconButtonDefaults.filledIconButtonColors(containerColor = SecColors.Accent),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                    }
                }

                if (itemsList.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        itemsList.toList().forEach { code ->
                            Row(
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .background(SecColors.TagBg, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.QrCode2, null, tint = selectedType.color.copy(alpha = 0.6f), modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(code, color = selectedType.color, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                                IconButton(onClick = { itemsList.remove(code) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, tint = SecColors.TextMuted, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                // Заметки
                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("Заметки (опционально)", color = SecColors.TextSecondary) },
                    leadingIcon   = { Icon(Icons.Default.Notes, null, tint = SecColors.TextMuted) },
                    maxLines      = 2,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = secTextFieldColors(),
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { onConfirm(selectedType.key, itemsList.toList(), toId.trim(), toName.trim(), selectedHub, notes.ifBlank { null }) },
                enabled  = toName.isNotBlank() && itemsList.isNotEmpty() && !isLoading,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SecColors.Accent)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.AssignmentReturn, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Выдать (${itemsList.size} ед.)", color = Color.White, fontWeight = FontWeight.Bold)
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
// УТИЛИТЫ
// ============================================================================================

@Composable
private fun rememberElapsed(issuedAt: Long): String {
    var elapsed by remember { mutableStateOf(calcElapsed(issuedAt)) }
    LaunchedEffect(issuedAt) {
        while (true) {
            elapsed = calcElapsed(issuedAt)
            kotlinx.coroutines.delay(60_000)
        }
    }
    return elapsed
}

private fun calcElapsed(issuedAt: Long): String {
    if (issuedAt == 0L) return "—"
    val diff = System.currentTimeMillis() - issuedAt
    return when {
        diff < 3_600_000  -> "${diff / 60_000} мин"
        diff < 86_400_000 -> "${diff / 3_600_000} ч"
        else              -> "${diff / 86_400_000} д"
    }
}

private fun formatIssueDate(ts: Long): String {
    if (ts == 0L) return "—"
    return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru")).format(Date(ts))
}