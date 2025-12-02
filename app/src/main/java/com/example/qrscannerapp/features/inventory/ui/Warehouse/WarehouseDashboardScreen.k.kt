// Полное содержимое для ИСПРАВЛЕННОГО файла WarehouseDashboardScreen.kt

package com.example.qrscannerapp.features.inventory.ui.Warehouse

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.qrscannerapp.*
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.ActivityLogSheet
import com.example.qrscannerapp.features.inventory.ui.distribution.AnimatedCounterText
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// --- Демо-данные Запчастей ---
data class DemoSparePart(val name: String, val category: String, val stock: Int, val lowStockThreshold: Int = 10, val imageUrl: String? = null)
val demoParts = listOf(
    DemoSparePart("Стойка амортизатора", "Ходовая", 68),
    DemoSparePart("Контроллер V3.1", "Электроника", 15),
    DemoSparePart("Ручка тормоза (левая)", "Механика", 112),
    DemoSparePart("Покрышка 10-дюймовая", "Колеса", 45),
    DemoSparePart("Аккумулятор 12Ah", "Батареи", 8, lowStockThreshold = 5), // Critical
    DemoSparePart("Болт M6x20", "Крепеж", 5, lowStockThreshold = 50)     // Critical
)

// --- Данные для Новостей ---
data class WarehouseNewsItem(val title: String, val date: String, val content: String, val color: Color)
val newsList = listOf(
    WarehouseNewsItem("Новая поставка", "Завтра", "Ожидаем 500 покрышек и 200 камер.", StardustSuccess),
    WarehouseNewsItem("Инвентаризация", "Сегодня", "Проверка секции 'Электроника'.", StardustPrimary),
    WarehouseNewsItem("Дефицит!", "Срочно", "Закончились BMS платы.", StardustError)
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
fun WarehouseDashboardScreen(navController: NavController) {
    var showActivityLog by remember { mutableStateOf(false) }

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
            // 1. Сводная карточка (С Новостным виджетом внутри)
            item {
                WarehouseSummaryCardWithNews(
                    news = newsList,
                    employeeOnShift = "Николай С.",
                    lowStockItemsCount = lowStockItemsCount,
                    takenTodayCount = 12,
                    onLowStockClick = { },
                    onTakenTodayClick = { },
                    onAnalyticsClick = { },
                    onExportClick = { }
                )
            }

            // 2. Блок навигации
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

            // 3. Заголовок списка + Поиск
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Остатки на складе",
                        style = MaterialTheme.typography.titleLarge,
                        color = StardustTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Поиск по названию или коду...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = StardustPrimary,
                            unfocusedContainerColor = StardustItemBg,
                            focusedContainerColor = StardustItemBg
                        )
                    )
                }
            }

            // 4. Список запчастей
            items(demoParts) { part ->
                SparePartListTile(part = part, onClick = { /* Детали */ })
            }
        }
    }

    if (showActivityLog) {
        ActivityLogSheet(onDismiss = { showActivityLog = false })
    }
}

// --- НОВЫЙ ВАРИАНТ ГЛАВНОЙ КАРТОЧКИ (С ВИДЖЕТОМ ВМЕСТО КРУГА) ---
@Composable
fun WarehouseSummaryCardWithNews(
    news: List<WarehouseNewsItem>, // Список новостей
    employeeOnShift: String,
    lowStockItemsCount: Int,
    takenTodayCount: Int,
    onLowStockClick: () -> Unit,
    onTakenTodayClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ВЕРХНЯЯ ЧАСТЬ
            Row(
                modifier = Modifier.height(IntrinsicSize.Min), // Чтобы высоты совпадали
                verticalAlignment = Alignment.Top
            ) {
                // ЛЕВАЯ ЧАСТЬ: ТЕПЕРЬ ТУТ APPLE WIDGET
                Box(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                    EmbeddedNewsWidget(news = news)
                }

                Spacer(modifier = Modifier.width(16.dp))

                // ПРАВАЯ ЧАСТЬ: СТАРЫЕ ВИДЖЕТЫ (БЕЗ ИЗМЕНЕНИЙ)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val lowStockColor = if (lowStockItemsCount > 0) StardustError else StardustSuccess
                    DashboardWidget(
                        title = "Заканчивается",
                        count = lowStockItemsCount,
                        icon = Icons.Outlined.WarningAmber,
                        color = lowStockColor,
                        onClick = onLowStockClick
                    )
                    DashboardWidget(
                        title = "Взято сегодня",
                        count = takenTodayCount,
                        icon = Icons.AutoMirrored.Outlined.TrendingUp,
                        color = StardustPrimary,
                        prefix = "+",
                        onClick = onTakenTodayClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // НИЖНЯЯ ПАНЕЛЬ (СОТРУДНИК) - БЕЗ ИЗМЕНЕНИЙ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(StardustTextSecondary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(employeeOnShift.take(1), color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("На смене", style = MaterialTheme.typography.labelSmall, color = StardustTextSecondary)
                        Text(employeeOnShift, style = MaterialTheme.typography.labelMedium, color = StardustTextPrimary)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onAnalyticsClick) { Icon(Icons.Outlined.Analytics, null, tint = StardustSecondary) }
                    IconButton(onClick = onExportClick) { Icon(Icons.Outlined.UploadFile, null, tint = StardustSecondary) }
                }
            }
        }
    }
}

// --- ВСТРАИВАЕМЫЙ ВИДЖЕТ НОВОСТЕЙ (ДЛЯ ЛЕВОЙ КОЛОНКИ) ---
@Composable
fun EmbeddedNewsWidget(news: List<WarehouseNewsItem>) {
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(5000)
            currentIndex = (currentIndex + 1) % news.size
        }
    }

    // Карточка внутри карточки - делаем её без Elevation, чтобы она выглядела как часть
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent) // Фон рисуется внутри
    ) {
        AnimatedContent(
            targetState = news[currentIndex],
            transitionSpec = {
                (fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.95f, animationSpec = tween(600)))
                    .togetherWith(fadeOut(animationSpec = tween(400)))
            },
            label = "NewsTransition"
        ) { item ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Градиентный фон
                AnimatedGradientBackground(baseColor = item.color)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Верх
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(item.date, fontSize = 9.sp, color = Color.White)
                        }
                    }

                    // Текст
                    Text(
                        text = item.content,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 3,
                        lineHeight = 18.sp
                    )

                    // Точки (индикаторы)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        news.forEachIndexed { index, _ ->
                            val width = if (index == currentIndex) 12.dp else 4.dp
                            Box(
                                modifier = Modifier
                                    .height(4.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = if (index == currentIndex) 1f else 0.3f))
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ (СТАРЫЕ) ---

@Composable
fun DashboardWidget(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    prefix: String = "",
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(65.dp),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, color = StardustTextSecondary, fontSize = 11.sp)
                AnimatedCounterText(count = count, prefix = prefix, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    iconTint: Color,
    borderColor: Color? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        border = if (borderColor != null) BorderStroke(1.dp, borderColor) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StardustItemBg)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватарка
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(StardustGlassBg),
            contentAlignment = Alignment.Center
        ) {
            if (part.imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(part.imageUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val seedColor = generateColorForDashboard(part.name)
                Box(
                    modifier = Modifier.fillMaxSize().background(seedColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(part.name.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White.copy(alpha = 0.5f))
                }
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
            Box(Modifier.background(StardustError.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("${part.stock} шт.", color = StardustError, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        } else {
            Text(text = "${part.stock}", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(text = " шт.", color = StardustTextSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
        }
    }
}

@Composable
fun AnimatedGradientBackground(baseColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(baseColor, baseColor.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.8f)),
                start = Offset(0f, 0f),
                end = Offset(size.width + offset, size.height + offset)
            )
        )
    }
}