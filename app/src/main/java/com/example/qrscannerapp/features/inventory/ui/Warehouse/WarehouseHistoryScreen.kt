package com.example.qrscannerapp.features.inventory.ui.Warehouse

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.qrscannerapp.*
import com.example.qrscannerapp.features.inventory.data.WarehouseLog
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.generateColorForCategory
import java.text.SimpleDateFormat
import java.util.*

enum class HistoryViewType { ALL, MY }
enum class TimePeriod { TODAY, ALL_TIME, PERIOD }

data class HistoryStats(
    val totalOperations: Int,
    val totalTaken: Int,
    val totalAdded: Int,
    val topParts: List<Pair<String, Int>>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseHistoryScreen(
    navController: NavController,
    userId: String,
    viewModel: WarehouseViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // --- Сбор данных ---
    val logs by viewModel.logs.collectAsState()
    val items by viewModel.items.collectAsState()

    // --- Состояния фильтров ---
    var viewType by remember { mutableStateOf(HistoryViewType.ALL) }
    var timePeriod by remember { mutableStateOf(TimePeriod.ALL_TIME) }
    var searchQuery by remember { mutableStateOf("") }

    // Диапазон дат (по умолчанию последние 7 дней для кастомного периода)
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis() - 7 * 24 * 3600 * 1000L) }
    var endDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Карта категорий
    val itemCategoryMap = remember(items) {
        items.associate { it.id to it.category }
    }

    // --- Логика фильтрации дат ---
    val filteredLogs = remember(logs, searchQuery, viewType, timePeriod, startDate, endDate, userId) {
        val todayStart = getStartOfDay(System.currentTimeMillis())
        val todayEnd = getEndOfDay(System.currentTimeMillis())

        logs.filter { log ->
            // 1. Фильтр Вкладки: Все / Мои
            val matchesUser = if (viewType == HistoryViewType.MY) log.userId == userId else true

            // 2. Фильтр по Периоду
            val matchesPeriod = when (timePeriod) {
                TimePeriod.TODAY -> log.timestamp.toDate().time in todayStart..todayEnd
                TimePeriod.PERIOD -> log.timestamp.toDate().time in getStartOfDay(startDate)..getEndOfDay(endDate)
                TimePeriod.ALL_TIME -> true
            }

            // 3. Поиск по деталям или сотрудникам
            val matchesSearch = if (searchQuery.isNotBlank()) {
                log.itemName.contains(searchQuery, ignoreCase = true) ||
                        log.userName.contains(searchQuery, ignoreCase = true)
            } else true

            matchesUser && matchesPeriod && matchesSearch
        }
    }

    // --- Расчет статистики ---
    val stats = remember(filteredLogs) {
        val totalOps = filteredLogs.size
        val totalTaken = filteredLogs.filter { it.quantityChange < 0 }.sumOf { -it.quantityChange }
        val totalAdded = filteredLogs.filter { it.quantityChange > 0 }.sumOf { it.quantityChange }

        val topParts = filteredLogs.filter { it.quantityChange < 0 }
            .groupBy { it.itemName }
            .mapValues { entry -> entry.value.sumOf { -it.quantityChange } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        HistoryStats(totalOps, totalTaken, totalAdded, topParts)
    }

    // Группировка логов по дням для списка
    val dayFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale("ru")) }
    val groupedLogs = remember(filteredLogs) {
        filteredLogs.groupBy { dayFormat.format(it.timestamp.toDate()) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("История склада", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ─── Вкладки: Все / Мои ───
            item(key = "tabs") {
                TabRow(
                    selectedTabIndex = viewType.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = StardustTextPrimary,
                    divider = { HorizontalDivider(color = StardustItemBg) },
                    indicator = { tabPositions ->
                        if (viewType.ordinal < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[viewType.ordinal]),
                                color = StardustPrimary
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = viewType == HistoryViewType.ALL,
                        onClick = { viewType = HistoryViewType.ALL },
                        text = { Text("Все списания", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = viewType == HistoryViewType.MY,
                        onClick = { viewType = HistoryViewType.MY },
                        text = { Text("Мои списания", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // ─── Строка поиска ───
            item(key = "search_field") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Поиск запчасти или сотрудника...", color = StardustTextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = StardustTextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null, tint = StardustTextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StardustPrimary,
                        unfocusedBorderColor = StardustItemBg,
                        focusedContainerColor = StardustGlassBg,
                        unfocusedContainerColor = StardustGlassBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ─── Периоды списаний и календари ───
            item(key = "periods") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            TimePeriod.ALL_TIME to "Все время",
                            TimePeriod.TODAY to "Сегодня",
                            TimePeriod.PERIOD to "За период"
                        ).forEach { (period, label) ->
                            val isSelected = timePeriod == period
                            FilterChip(
                                selected = isSelected,
                                onClick = { timePeriod = period },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StardustPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = StardustPrimary,
                                    selectedLeadingIconColor = StardustPrimary,
                                    containerColor = StardustGlassBg,
                                    labelColor = StardustTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    selectedBorderColor = StardustPrimary.copy(alpha = 0.5f),
                                    borderColor = Color.Transparent
                                )
                            )
                        }
                    }

                    // Календари (для "За период")
                    AnimatedVisibility(
                        visible = timePeriod == TimePeriod.PERIOD,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DateSelectButton(
                                    label = "От",
                                    timestamp = startDate,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    showDatePicker(context, startDate) { startDate = it }
                                }
                                DateSelectButton(
                                    label = "До",
                                    timestamp = endDate,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    showDatePicker(context, endDate) { endDate = it }
                                }
                            }
                        }
                    }
                }
            }

            // ─── Секция статистики ───
            item(key = "stats") {
                StatsPanel(stats = stats)
            }

            // ─── Лента логов ───
            if (groupedLogs.isEmpty()) {
                item(key = "empty_logs") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.History, null, tint = StardustTextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Нет операций за этот период", color = StardustTextSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                groupedLogs.forEach { (day, dayLogs) ->
                    item(key = day) {
                        Text(
                            text = day,
                            color = StardustSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(dayLogs, key = { it.id }) { log ->
                        val resolvedCategory = itemCategoryMap[log.itemId] ?: "Общее"
                        LogItemRow(log = log, category = resolvedCategory)
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSelectButton(
    label: String,
    timestamp: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dateStr = remember(timestamp) {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = StardustGlassBg,
        border = BorderStroke(1.dp, StardustItemBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$label: $dateStr", color = StardustTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Outlined.CalendarMonth, null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun StatsPanel(stats: HistoryStats) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg),
        border = BorderStroke(1.dp, StardustItemBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Всего операций
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Транзакции", color = StardustTextSecondary, fontSize = 11.sp)
                    Text("${stats.totalOperations}", color = StardustPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                // Списано деталей
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Списано", color = StardustTextSecondary, fontSize = 11.sp)
                    Text("${stats.totalTaken} шт", color = StardustSecondary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                // Принято/Вернуто
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Поступления", color = StardustTextSecondary, fontSize = 11.sp)
                    Text("${stats.totalAdded} шт", color = StardustSuccess, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (stats.topParts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = StardustItemBg)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Топ списываемых деталей:", color = StardustTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                stats.topParts.forEach { (partName, qty) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = partName,
                            color = StardustTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$qty шт",
                            color = StardustSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItemRow(log: WarehouseLog, category: String) {
    val isTake = log.quantityChange < 0
    val indicatorColor = if (isTake) StardustSecondary else StardustSuccess
    val timeStr = remember(log.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(log.timestamp.toDate())
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StardustItemBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка направления
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(indicatorColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isTake) Icons.Default.Remove else Icons.Default.Add,
                    contentDescription = null,
                    tint = indicatorColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Текстовая информация
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.itemName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Тег категории
                    val catColor = generateColorForCategory(category)
                    Box(
                        modifier = Modifier
                            .background(catColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(category, color = catColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("•", color = StardustTextSecondary.copy(alpha = 0.5f), fontSize = 10.sp)
                    Text(log.userName, color = StardustTextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Количество и время
            Column(horizontalAlignment = Alignment.End) {
                val qtySign = if (isTake) "-" else "+"
                val qtyColor = if (isTake) StardustSecondary else StardustSuccess
                Text(
                    text = "$qtySign${kotlin.math.abs(log.quantityChange)} шт",
                    color = qtyColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(timeStr, color = StardustTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

// ─── Вспомогательные функции для расчета дат ───
private fun getStartOfDay(timeMs: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timeMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun getEndOfDay(timeMs: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timeMs
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}

private fun showDatePicker(context: Context, initialDate: Long, onDateSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialDate }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            onDateSelected(selectedCalendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
