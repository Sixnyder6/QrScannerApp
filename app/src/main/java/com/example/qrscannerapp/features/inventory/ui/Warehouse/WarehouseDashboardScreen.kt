package com.example.qrscannerapp.features.inventory.ui.Warehouse

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.qrscannerapp.*
import com.example.qrscannerapp.features.inventory.data.NewsItem
import com.example.qrscannerapp.features.inventory.data.OrderStatus
import com.example.qrscannerapp.features.inventory.data.WarehouseOrder
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.ActivityLogSheet
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.EmployeeHistorySheet
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.NewsEditSheet
import com.example.qrscannerapp.features.inventory.ui.distribution.AnimatedCounterText
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Locale

// --- Enum для статусов сотрудника ---
enum class EmployeeStatus(val displayName: String, val color: Color) {
    ON_SHIFT("На смене", StardustSuccess),
    BUSY("Занят", StardustError),
    AWAY("Не на месте", StardustWarning),
    ON_BREAK("Перерыв", Color(0xFF6A6AFF))
}

// --- Модель сотрудника ---
data class Employee(
    var id: String = "",
    val name: String = "",
    val imageUrl: String? = null
)

// --- Локальная "заглушка" ---
private const val GITHUB_EMPLOYEE_IMAGE_URL = "https://raw.githubusercontent.com/Sixnyder6/QrScannerApp/master/images/employees/"
val demoEmployees = listOf(
    Employee(
        id = "1",
        name = "Николай Никасов",
        imageUrl = "${GITHUB_EMPLOYEE_IMAGE_URL}nikasov.png"
    ),
    Employee(
        id = "2",
        name = "Михаил Ситников",
        imageUrl = "${GITHUB_EMPLOYEE_IMAGE_URL}sitnikov.png"
    )
)

// (Утилита генерации цвета)
fun generateColorForDashboard(name: String): Color {
    val hash = name.hashCode()
    val red = (hash and 0xFF0000 shr 16) / 255f
    val green = (hash and 0x00FF00 shr 8) / 255f
    val blue = (hash and 0x0000FF) / 255f
    return Color(
        red = (red * 0.5f + 0.3f).coerceIn(0f, 1f),
        green = (green * 0.5f + 0.3f).coerceIn(0f, 1f),
        blue = (blue * 0.5f + 0.3f).coerceIn(0f, 1f),
        alpha = 1f
    )
}

// --- Главный экран ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseDashboardScreen(
    navController: NavController,
    isAdmin: Boolean,
    userRole: UserRole,
    userId: String,
    currentWarehouseId: String = "bestuzhevskaya_10",
    viewModel: WarehouseViewModel = hiltViewModel()
) {
    // --- Состояния UI ---
    var showActivityLog by remember { mutableStateOf(false) }
    var showNewsEditSheet by remember { mutableStateOf(false) }
    var newsItemToEdit by remember { mutableStateOf<NewsItem?>(null) }
    var showEmployeeSheet by remember { mutableStateOf(false) }
    var showEmployeeHistory by remember { mutableStateOf(false) }
    var syncIsBusy by remember { mutableStateOf(false) }
    var syncSuccess by remember { mutableStateOf(false) }

    // Используем ID заказа для открытия деталей, чтобы данные всегда были свежими
    var selectedOrderId by remember { mutableStateOf<String?>(null) }

    val canManageWarehouse = isAdmin || userRole == UserRole.INVENTORY_MANAGER

    // --- Подписка на данные из ViewModel ---
    val groupedActivities by viewModel.groupedActivities.collectAsState()
    val newsItems by viewModel.newsItems.collectAsState()
    val shiftState by viewModel.shiftState.collectAsState()
    val allEmployees by viewModel.employees.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val employeeHistory by viewModel.employeeHistory.collectAsState()
    val isEmployeeHistoryLoading by viewModel.isEmployeeHistoryLoading.collectAsState()

    LaunchedEffect(syncIsBusy) {
        if (syncIsBusy) {
            delay(2000)
            syncIsBusy = false
            syncSuccess = true
        }
    }
    LaunchedEffect(syncSuccess) {
        if (syncSuccess) {
            delay(3000)
            syncSuccess = false
        }
    }

    // Подписка на заказы при входе на экран
    LaunchedEffect(key1 = userId, key2 = canManageWarehouse) {
        viewModel.subscribeToOrders(userId = userId, isAdmin = canManageWarehouse)
    }

    // Находим актуальный объект заказа из списка по ID
    val activeOrderForSheet = remember(selectedOrderId, orders) {
        orders.find { it.id == selectedOrderId }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Навигация на сканер */ },
                containerColor = StardustPrimary,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Rounded.QrCodeScanner, null, modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // === БЛОК С НОВОСТЯМИ И СВОДКОЙ ===
            item {
                WarehouseSummaryCardWithNews(
                    news = newsItems,
                    shiftState = shiftState,
                    lowStockItemsCount = 0,
                    takenTodayCount = 0,
                    onLowStockClick = { },
                    onTakenTodayClick = { },
                    onAnalyticsClick = { },
                    onExportClick = { },
                    onAddNews = {
                        newsItemToEdit = null
                        showNewsEditSheet = true
                    },
                    onEditNews = { itemToEdit ->
                        newsItemToEdit = itemToEdit
                        showNewsEditSheet = true
                    },
                    onChangeEmployeeClick = { showEmployeeSheet = true },
                    canManage = canManageWarehouse
                )
            }

            // === БЛОК АКТИВНЫХ ЗАКАЗОВ ===
            item {
                AnimatedVisibility(
                    visible = orders.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (canManageWarehouse) "Входящие заявки" else "Мои заказы",
                            style = MaterialTheme.typography.titleLarge,
                            color = StardustTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        orders.forEach { order ->
                            OrderCard(
                                order = order,
                                onClick = { selectedOrderId = order.id }
                            )
                        }
                    }
                }
            }


            // === КНОПКИ БЫСТРОГО ДОСТУПА ===
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.History,
                        title = "История",
                        subtitle = "Все операции",
                        color = StardustItemBg,
                        iconTint = StardustTextSecondary,
                        onClick = { showActivityLog = true }
                    )
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        title = "Каталог",
                        subtitle = if (canManageWarehouse) "Управление" else "Взять / Заказать",
                        color = StardustPrimary.copy(alpha = 0.15f),
                        iconTint = StardustPrimary,
                        borderColor = StardustPrimary.copy(alpha = 0.3f),
                        onClick = { navController.navigate(Screen.WarehouseCatalog.route) }
                    )
                }
            }

            // === ИНСТРУМЕНТЫ КЛАДОВЩИКА ===
            if (canManageWarehouse) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showEmployeeHistory = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                StardustTextSecondary.copy(alpha = 0.3f)
                            )
                        ) {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = StardustTextSecondary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "По сотруднику",
                                color = StardustTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        val syncColor = when {
                            syncSuccess -> Color(0xFF4CAF50)
                            syncIsBusy -> StardustPrimary.copy(alpha = 0.5f)
                            else -> Color(0xFF2A4A35)
                        }
                        Button(
                            onClick = {
                                if (!syncIsBusy && !syncSuccess) {
                                    syncIsBusy = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = syncColor),
                            enabled = !syncIsBusy
                        ) {
                            if (syncIsBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    if (syncSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (syncSuccess) "Готово" else "Синхр. 1С",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    // --- МОДАЛЬНЫЕ ОКНА ---

    if (showActivityLog) {
        ActivityLogSheet(
            activities = groupedActivities,
            onDismiss = { showActivityLog = false }
        )
    }

    if (showEmployeeHistory && canManageWarehouse) {
        EmployeeHistorySheet(
            employees = allUsers,
            employeeHistory = employeeHistory,
            isLoading = isEmployeeHistoryLoading,
            onSelectEmployee = { emp -> viewModel.loadEmployeeHistory(emp.id) },
            onDismiss = {
                showEmployeeHistory = false
                viewModel.clearEmployeeHistory()
            }
        )
    }

    if (showNewsEditSheet && canManageWarehouse) {
        NewsEditSheet(
            newsItem = newsItemToEdit,
            onSave = { item ->
                if (item.id.isBlank()) {
                    viewModel.onAddNewsItem(item.title, item.content, item.tag)
                } else {
                    viewModel.onUpdateNewsItem(item)
                }
            },
            onDelete = { itemId -> viewModel.onDeleteNewsItem(itemId) },
            onDismiss = { showNewsEditSheet = false }
        )
    }

    if (showEmployeeSheet) {
        EmployeeSelectionSheet(
            allEmployees = allEmployees,
            allStatuses = EmployeeStatus.values().toList(),
            currentShiftState = shiftState,
            onEmployeeSelected = { employee ->
                if (canManageWarehouse) viewModel.onEmployeeSelected(employee)
            },
            onStatusSelected = { status ->
                if (canManageWarehouse) viewModel.onStatusSelected(status)
                showEmployeeSheet = false
            },
            onDismiss = { showEmployeeSheet = false },
            canSelect = canManageWarehouse
        )
    }

    // Если заказ был удален или выдан, окно закроется само (activeOrderForSheet станет null)
    if (activeOrderForSheet != null) {
        OrderDetailsSheet(
            order = activeOrderForSheet!!,
            isAdmin = canManageWarehouse,
            onDismiss = { selectedOrderId = null },
            onAccept = { viewModel.onAcceptOrder(activeOrderForSheet!!) },
            onReady = { viewModel.onMarkOrderReady(activeOrderForSheet!!) },
            onFinish = { order ->
                viewModel.onFinishOrder(order, shiftState.employee.name)
                selectedOrderId = null
            },
            onCancel = {
                viewModel.onCancelOrder(activeOrderForSheet!!)
                selectedOrderId = null
            }
        )
    }
}

// ==========================================
// КОМПОНЕНТЫ ДЛЯ ЗАКАЗОВ (ORDERS UI)
// ==========================================

@Composable
fun OrderCard(
    order: WarehouseOrder,
    onClick: () -> Unit
) {
    val dateStr = remember(order.createdAt) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(order.createdAt.toDate())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustItemBg),
        border = BorderStroke(1.dp, order.status.color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(order.status.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(order.status) {
                        OrderStatus.CREATED -> Icons.Outlined.NewReleases
                        OrderStatus.PROCESSING -> Icons.Outlined.Build
                        OrderStatus.READY -> Icons.Outlined.CheckCircle
                        OrderStatus.COMPLETED -> Icons.Outlined.DoneAll
                        OrderStatus.CANCELLED -> Icons.Outlined.Cancel
                    },
                    contentDescription = null,
                    tint = order.status.color
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.userName.ifBlank { "Неизвестный" },
                    style = MaterialTheme.typography.titleMedium,
                    color = StardustTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${order.totalItemsCount} позиций • $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = StardustTextSecondary
                )
            }

            Surface(
                color = order.status.color,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = order.status.displayName,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.Black,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsSheet(
    order: WarehouseOrder,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onReady: () -> Unit,
    onFinish: (WarehouseOrder) -> Unit,
    onCancel: () -> Unit
) {
    // Состояние галочек (хранится, пока открыто окно)
    val checkedItems = remember(order.id) { mutableStateMapOf<String, Boolean>() }

    // Проверка: все ли запчасти отмечены галочками
    val allItemsPicked = remember(checkedItems.size, order.items.size) {
        order.items.isNotEmpty() && order.items.all { checkedItems[it.itemId] == true }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StardustModalBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = StardustTextSecondary) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Заголовок и статус
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Заказ #${order.id.takeLast(4).uppercase()}", style = MaterialTheme.typography.headlineSmall, color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                    Text("от ${order.userName}", style = MaterialTheme.typography.bodyMedium, color = StardustTextSecondary)
                }
                Surface(
                    color = order.status.color.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, order.status.color)
                ) {
                    Text(
                        text = order.status.displayName,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = order.status.color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Список товаров с логикой галочек
            Text("Состав заказа:", color = StardustTextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(order.items) { item ->
                    val isChecked = checkedItems[item.itemId] ?: false

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isChecked) StardustPrimary.copy(alpha = 0.1f) else StardustItemBg)
                            .clickable(enabled = order.status == OrderStatus.PROCESSING) {
                                checkedItems[item.itemId] = !isChecked
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Показываем галочку ТОЛЬКО в режиме сборки (PROCESSING)
                        if (order.status == OrderStatus.PROCESSING) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checkedItems[item.itemId] = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = StardustPrimary,
                                    uncheckedColor = StardustTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (item.itemImageUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(item.itemImageUrl).crossfade(true).build(),
                                contentDescription = null,
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.size(44.dp).background(Color.Gray.copy(alpha=0.3f), RoundedCornerShape(8.dp)))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.itemName,
                                color = if (isChecked) StardustPrimary else StardustTextPrimary,
                                fontWeight = FontWeight.Medium,
                                textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            )
                        }
                        Text("${item.quantity} ${item.unit}", color = StardustPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // КНОПКИ ДЕЙСТВИЙ (Только для Админа/Кладовщика)
            if (isAdmin) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when(order.status) {
                        OrderStatus.CREATED -> {
                            Button(
                                onClick = onAccept,
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.Black)
                                Spacer(Modifier.width(8.dp))
                                Text("Принять в работу", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                        OrderStatus.PROCESSING -> {
                            // Кнопка активна ТОЛЬКО если все галочки проставлены
                            Button(
                                onClick = onReady,
                                enabled = allItemsPicked,
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StardustSuccess,
                                    disabledContainerColor = StardustSuccess.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Check, null, tint = Color.Black)
                                Spacer(Modifier.width(8.dp))
                                Text("Всё собрано", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            if (!allItemsPicked) {
                                Text(
                                    "Отметьте все запчасти галочками, чтобы завершить сборку",
                                    color = StardustWarning,
                                    fontSize = 11.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                        OrderStatus.READY -> {
                            Button(
                                onClick = { onFinish(order) },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.DoneAll, null, tint = Color.Black)
                                Spacer(Modifier.width(8.dp))
                                Text("Выдать и Завершить", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {}
                    }

                    if (order.status != OrderStatus.COMPLETED && order.status != OrderStatus.CANCELLED) {
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Отменить заказ", color = StardustError) }
                    }
                }
            } else {
                // Для пользователя (Техника)
                if (order.status == OrderStatus.READY) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StardustSuccess.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, StardustSuccess)
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = StardustSuccess)
                            Spacer(Modifier.width(12.dp))
                            Text("Ваш заказ готов! Можно забирать.", color = StardustSuccess, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- Остальные компоненты (Summary, DashboardWidget и др.) ---

@Composable
fun WarehouseSummaryCardWithNews(
    news: List<NewsItem>,
    shiftState: ShiftState,
    lowStockItemsCount: Int,
    takenTodayCount: Int,
    onLowStockClick: () -> Unit,
    onTakenTodayClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onExportClick: () -> Unit,
    onAddNews: () -> Unit,
    onEditNews: (NewsItem) -> Unit,
    onChangeEmployeeClick: () -> Unit,
    canManage: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                    EmbeddedNewsWidget(
                        news = news,
                        onAddClick = onAddNews,
                        onEditClick = onEditNews,
                        canManage = canManage
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val lowStockColor = if (lowStockItemsCount > 0) StardustError else StardustSuccess
                    DashboardWidget(title = "Заканчивается", count = lowStockItemsCount, icon = Icons.Outlined.WarningAmber, color = lowStockColor, onClick = onLowStockClick)
                    DashboardWidget(title = "Взято сегодня", count = takenTodayCount, icon = Icons.AutoMirrored.Outlined.TrendingUp, color = StardustPrimary, prefix = "+", onClick = onTakenTodayClick)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            val employeeRowModifier = if (canManage) {
                Modifier.clickable(onClick = onChangeEmployeeClick)
            } else {
                Modifier
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .then(employeeRowModifier)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(shiftState.employee.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Фото сотрудника",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(shiftState.employee.name, style = MaterialTheme.typography.titleSmall, color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                        StatusTag(status = shiftState.status)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onAnalyticsClick) { Icon(Icons.Outlined.Analytics, null, tint = StardustSecondary) }
                    IconButton(onClick = onExportClick) { Icon(Icons.Outlined.UploadFile, null, tint = StardustSecondary) }
                    if (canManage) {
                        Icon(Icons.Outlined.Edit, null, tint = StardustTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp).align(Alignment.CenterVertically))
                    }
                }
            }
        }
    }
}

@Composable
fun StatusTag(status: EmployeeStatus) {
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .background(status.color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(status.color, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = status.displayName,
            color = status.color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeSelectionSheet(
    allEmployees: List<Employee>,
    allStatuses: List<EmployeeStatus>,
    currentShiftState: ShiftState,
    onEmployeeSelected: (Employee) -> Unit,
    onStatusSelected: (EmployeeStatus) -> Unit,
    onDismiss: () -> Unit,
    canSelect: Boolean
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StardustGlassBg,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            Text(
                "Сотрудник на смене",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = StardustTextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            allEmployees.forEach { employee ->
                ListItem(
                    headlineContent = { Text(employee.name, color = StardustTextPrimary) },
                    modifier = Modifier.clickable(enabled = canSelect) { onEmployeeSelected(employee) },
                    leadingContent = {
                        AsyncImage(
                            model = employee.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    },
                    trailingContent = {
                        if (employee.id == currentShiftState.employee.id) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Выбран",
                                tint = StardustSuccess
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp), color = StardustTextSecondary.copy(alpha = 0.2f))

            Text(
                "Установить статус",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = StardustTextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(allStatuses) { status ->
                    StatusSelectionChip(
                        status = status,
                        isSelected = status == currentShiftState.status,
                        onClick = { if (canSelect) onStatusSelected(status) }
                    )
                }
            }
        }
    }
}


@Composable
fun StatusSelectionChip(
    status: EmployeeStatus,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) status.color else status.color.copy(alpha = 0.15f),
        label = "bgColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else status.color,
        label = "textColor"
    )

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = CircleShape,
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Box(modifier = Modifier
                .size(8.dp)
                .background(if (isSelected) Color.Black.copy(alpha = 0.5f) else status.color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(status.displayName, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
fun EmbeddedNewsWidget(
    news: List<NewsItem>,
    onAddClick: () -> Unit,
    onEditClick: (NewsItem) -> Unit,
    canManage: Boolean
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentItem = news.getOrNull(currentIndex)

    LaunchedEffect(news.size) {
        while (isActive) {
            delay(5000)
            if (news.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % news.size
            }
        }
    }

    Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        AnimatedContent(
            targetState = currentItem,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.95f)).togetherWith(fadeOut()) },
            label = "NewsTransition"
        ) { item ->
            if (item != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedGradientBackground(baseColor = item.tag.color)
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = item.title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                            Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Text(item.tag.displayName, fontSize = 9.sp, color = Color.White)
                            }
                        }
                        Text(text = item.content, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 3, lineHeight = 18.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            news.forEachIndexed { index, _ ->
                                Box(modifier = Modifier.height(4.dp).width(if (index == currentIndex) 12.dp else 4.dp).clip(CircleShape).background(Color.White.copy(alpha = if (index == currentIndex) 1f else 0.3f)))
                            }
                        }
                    }
                    if (canManage) {
                        Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                            IconButton(onClick = { onEditClick(item) }, enabled = news.isNotEmpty()) {
                                Icon(Icons.Outlined.Edit, "Редактировать", tint = Color.White.copy(alpha = 0.7f))
                            }
                            IconButton(onClick = onAddClick) {
                                Icon(Icons.Outlined.Add, "Добавить", tint = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(StardustGlassBg), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.AddComment, null, tint = StardustTextSecondary)
                        Text("Нет новостей", color = StardustTextSecondary)
                        if (canManage) {
                            Button(onClick = onAddClick) { Text("Добавить первую") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardWidget(title: String, count: Int, icon: ImageVector, color: Color, prefix: String = "", onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().height(65.dp), shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.12f), border = BorderStroke(1.dp, color.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(title, color = StardustTextSecondary, fontSize = 11.sp)
                AnimatedCounterText(count = count, prefix = prefix, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun ActionCard(modifier: Modifier = Modifier, icon: ImageVector, title: String, subtitle: String, color: Color, iconTint: Color, borderColor: Color? = null, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(90.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = color), border = if (borderColor != null) BorderStroke(1.dp, borderColor) else null) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.Start) {
            Icon(icon, null, tint = iconTint)
            Column {
                Text(title, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = StardustTextSecondary, fontSize = 10.sp, lineHeight = 10.sp)
            }
        }
    }
}

@Composable
fun AnimatedGradientBackground(baseColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val offset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1000f, animationSpec = infiniteRepeatable(animation = tween(20000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "offset")
    Canvas(modifier = Modifier.fillMaxSize()) { drawRect(brush = Brush.linearGradient(colors = listOf(baseColor, baseColor.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.8f)), start = Offset(0f, 0f), end = Offset(size.width + offset, size.height + offset))) }
}