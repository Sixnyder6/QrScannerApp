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
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.ActivityLogSheet
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.NewsEditSheet
import com.example.qrscannerapp.features.inventory.ui.distribution.AnimatedCounterText
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// --- Демо-данные Запчастей (остаются для примера) ---
data class DemoSparePart(val name: String, val category: String, val stock: Int, val lowStockThreshold: Int = 10, val imageUrl: String? = null)
val demoParts = listOf(
    DemoSparePart("Стойка амортизатора", "Ходовая", 68),
    DemoSparePart("Контроллер V3.1", "Электроника", 15)
)

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
    userRole: UserRole, // ДОБАВЛЕНО: Принимаем роль пользователя
    viewModel: WarehouseViewModel = hiltViewModel()
) {
    // --- Состояния UI ---
    var showActivityLog by remember { mutableStateOf(false) }
    var showNewsEditSheet by remember { mutableStateOf(false) }
    var newsItemToEdit by remember { mutableStateOf<NewsItem?>(null) }
    var showEmployeeSheet by remember { mutableStateOf(false) }

    // --- ЛОГИКА ПРАВ ДОСТУПА ---
    // Админ ИЛИ Кладовщик имеют право управлять новостями и сотрудниками
    val canManageWarehouse = isAdmin || userRole == UserRole.INVENTORY_MANAGER

    // --- Подписка на данные из ViewModel ---
    val groupedActivities by viewModel.groupedActivities.collectAsState()
    val newsItems by viewModel.newsItems.collectAsState()
    val shiftState by viewModel.shiftState.collectAsState()
    val allEmployees by viewModel.employees.collectAsState()


    val lowStockItemsCount = demoParts.count { it.stock <= it.lowStockThreshold }

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
            item {
                WarehouseSummaryCardWithNews(
                    news = newsItems,
                    shiftState = shiftState,
                    lowStockItemsCount = lowStockItemsCount,
                    takenTodayCount = 12,
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
                    // ПЕРЕДАЕМ ПРАВА ДОСТУПА
                    canManage = canManageWarehouse
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.History,
                        title = "История",
                        subtitle = "Последние операции",
                        color = StardustItemBg,
                        iconTint = StardustTextSecondary,
                        onClick = { showActivityLog = true }
                    )
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        title = "Каталог",
                        subtitle = "Все запчасти",
                        color = StardustPrimary.copy(alpha = 0.15f),
                        iconTint = StardustPrimary,
                        borderColor = StardustPrimary.copy(alpha = 0.3f),
                        onClick = { navController.navigate(Screen.WarehouseCatalog.route) }
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Остатки на складе", style = MaterialTheme.typography.titleLarge, color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = "", onValueChange = {}, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Поиск...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, focusedBorderColor = StardustPrimary, unfocusedContainerColor = StardustItemBg, focusedContainerColor = StardustItemBg))
                }
            }
            items(demoParts) { part ->
                SparePartListTile(part = part, onClick = { /* Детали */ })
            }
        }
    }

    if (showActivityLog) {
        ActivityLogSheet(
            activities = groupedActivities,
            onDismiss = { showActivityLog = false }
        )
    }

    // Разрешаем открывать диалог редактирования, только если есть права
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
                // Только управляющий может менять сотрудника на смене?
                // Если да, добавить проверку: if (canManageWarehouse) ...
                // Пока оставим доступным всем, как было в коде, или ограничим:
                if (canManageWarehouse) viewModel.onEmployeeSelected(employee)
            },
            onStatusSelected = { status ->
                if (canManageWarehouse) viewModel.onStatusSelected(status)
                showEmployeeSheet = false
            },
            onDismiss = { showEmployeeSheet = false },
            canSelect = canManageWarehouse // Передаем флаг для UI
        )
    }
}


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
    canManage: Boolean // Заменили isAdmin на более общий флаг
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
    canSelect: Boolean // Флаг для блокировки UI если нет прав
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
            // Если прав нет, показываем только информацию, без возможности клика

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
    canManage: Boolean // Заменили isAdmin
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

// ... Остальные виджеты без изменений (DashboardWidget, ActionCard, SparePartListTile, AnimatedGradientBackground)
// Я их пропустил для краткости, они должны остаться как были.
// Если ты копируешь весь файл, убедись, что они на месте (или скопируй их из прошлого моего ответа).
// Для удобства я их добавлю снова, чтобы можно было копировать целиком.

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
fun SparePartListTile(part: DemoSparePart, onClick: () -> Unit) {
    val isLowStock = part.stock <= part.lowStockThreshold
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StardustItemBg).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(StardustGlassBg), contentAlignment = Alignment.Center) {
            if (part.imageUrl != null) {
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(part.imageUrl).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                val seedColor = generateColorForDashboard(part.name)
                Box(modifier = Modifier.fillMaxSize().background(seedColor), contentAlignment = Alignment.Center) { Text(part.name.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White.copy(alpha = 0.5f)) }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(part.name, color = StardustTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(StardustTextSecondary, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(part.category, color = StardustTextSecondary, fontSize = 13.sp)
            }
        }
        if (isLowStock) {
            Box(Modifier.background(StardustError.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) { Text("${part.stock} шт.", color = StardustError, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        } else {
            Text(text = "${part.stock}", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(text = " шт.", color = StardustTextSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
        }
    }
}

@Composable
fun AnimatedGradientBackground(baseColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val offset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1000f, animationSpec = infiniteRepeatable(animation = tween(20000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "offset")
    Canvas(modifier = Modifier.fillMaxSize()) { drawRect(brush = Brush.linearGradient(colors = listOf(baseColor, baseColor.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.8f)), start = Offset(0f, 0f), end = Offset(size.width + offset, size.height + offset))) }
}