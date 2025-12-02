// Полное содержимое файла WarehouseCatalogComponents.kt

package com.example.qrscannerapp.features.inventory.ui.Warehouse.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.qrscannerapp.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.absoluteValue

// --- Данные импортируются из WarehouseCatalogData.kt ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseCatalogScreen() {
    var itemToTake by remember { mutableStateOf<DemoCatalogItem?>(null) }
    var showNewItemDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Все") }

    // State для Snackbar или Toast (имитация)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filteredCatalog = remember(searchQuery, selectedCategory) {
        warehouseCatalogItems.filter {
            val matchesCategory = selectedCategory == "Все" || it.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    it.fullName.contains(searchQuery, ignoreCase = true) ||
                    it.shortName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val frequentlyTakenItems = remember {
        warehouseCatalogItems.sortedByDescending { it.stockCount }.take(8)
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewItemDialog = true }) {
                Icon(Icons.Default.Add, "Добавить новую запчасть")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                placeholder = { Text("Поиск...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = StardustItemBg)
            )

            if (frequentlyTakenItems.isNotEmpty()) {
                FrequentlyTakenCarousel(
                    frequentlyTakenItems = frequentlyTakenItems,
                    onItemClick = { item -> itemToTake = item }
                )
            }

            val categories = listOf("Все") + warehouseCatalogItems.map { it.category }.distinct()
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = category == selectedCategory,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCatalog, key = { it.id }) { item ->
                    CatalogGridItem(
                        item = item,
                        onClick = { itemToTake = item },
                        onQuickAdd = {
                            // Логика быстрого добавления
                            println("Быстро взято: 1 шт. ${item.shortName}")
                            // Тут можно показать Snackbar, но пока просто принт
                        }
                    )
                }
            }
        }
    }

    if (itemToTake != null) {
        QuantityPickerDialog(
            item = itemToTake!!,
            onDismiss = { itemToTake = null },
            onConfirm = { item, quantity ->
                println("Взяли '${item.fullName}' в количестве $quantity ${item.unit}")
                itemToTake = null
            }
        )
    }

    if (showNewItemDialog) {
        NewItemDialog(
            onDismiss = { showNewItemDialog = false },
            onConfirm = { name, category, unit ->
                println("Создана новая запчасть: $name, Категория: $category, Ед. изм.: $unit")
                showNewItemDialog = false
            }
        )
    }
}

// --- КАРУСЕЛЬ ---
@Composable
fun FrequentlyTakenCarousel(
    frequentlyTakenItems: List<DemoCatalogItem>,
    onItemClick: (DemoCatalogItem) -> Unit
) {
    val carouselItems = remember { frequentlyTakenItems + frequentlyTakenItems }
    val scrollState = rememberLazyListState()
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        while (coroutineContext.isActive) {
            delay(3000)

            if (scrollState.firstVisibleItemIndex >= frequentlyTakenItems.size) {
                val resetIndex = scrollState.firstVisibleItemIndex - frequentlyTakenItems.size
                scrollState.scrollToItem(resetIndex, scrollState.firstVisibleItemScrollOffset)
            }

            val currentItemInfo = scrollState.layoutInfo.visibleItemsInfo
                .find { it.index == scrollState.firstVisibleItemIndex }

            if (currentItemInfo != null) {
                val itemSizePx = currentItemInfo.size
                val gapPx = with(density) { 12.dp.toPx() }
                val scrollDistance = itemSizePx + gapPx

                scrollState.animateScrollBy(
                    value = scrollDistance,
                    animationSpec = tween(
                        durationMillis = 2000,
                        easing = LinearOutSlowInEasing
                    )
                )
            } else {
                scrollState.scrollToItem(scrollState.firstVisibleItemIndex + 1)
            }
        }
    }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Outlined.StarOutline, "Часто берут", tint = StardustPrimary)
            Text(text = "Чаще всего берут", style = MaterialTheme.typography.titleMedium, color = StardustTextSecondary)
        }
        LazyRow(
            state = scrollState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = true
        ) {
            items(carouselItems.size, key = { index -> "${carouselItems[index].id}_$index" }) { index ->
                val item = carouselItems[index]
                FrequentlyTakenCarouselItem(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequentlyTakenCarouselItem(
    item: DemoCatalogItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustItemBg),
        border = BorderStroke(1.dp, StardustPrimary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageUrl = getImageUrl(item)
            if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.fullName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(item.shortName, color = StardustTextPrimary, fontWeight = FontWeight.SemiBold)
                Text("${item.stockCount} ${item.unit} в наличии", color = StardustTextSecondary, fontSize = 12.sp)
            }
        }
    }
}

// --- УТИЛИТА: Генерация красивого цвета из имени ---
fun generateColorFromName(name: String): Color {
    val hash = name.hashCode()
    // Генерируем насыщенные, но приятные цвета (не слишком темные, не слишком светлые)
    val red = (hash and 0xFF0000 shr 16) / 255f
    val green = (hash and 0x00FF00 shr 8) / 255f
    val blue = (hash and 0x0000FF) / 255f

    // Подкручиваем яркость и насыщенность
    return Color(
        red = (red * 0.5f + 0.3f).coerceIn(0f, 1f),
        green = (green * 0.5f + 0.3f).coerceIn(0f, 1f),
        blue = (blue * 0.5f + 0.3f).coerceIn(0f, 1f),
        alpha = 1f
    )
}

// --- ГЛАВНЫЙ ЭЛЕМЕНТ: КАРТОЧКА СЕТКИ ---
@Composable
fun CatalogGridItem(
    item: DemoCatalogItem,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit
) {
    // Анимация нажатия (пружинка)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "scale"
    )

    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Квадрат
            .scale(scale) // Применяем пружинку
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Убираем стандартную волну, у нас своя анимация
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            val imageUrl = getImageUrl(item)

            if (imageUrl != null) {
                // 1. ФОТОГРАФИЯ
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.fullName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 2. ГЕНЕРАТИВНЫЙ "ЖИВОЙ" ФОН
                val seedColor = generateColorFromName(item.fullName)

                // Анимация переливания (Motion Parallax симуляция)
                val infiniteTransition = rememberInfiniteTransition(label = "gradient")
                val offsetAnim by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1000f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(10000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "offset"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(seedColor, seedColor.copy(alpha = 0.6f), seedColor),
                                start = Offset(0f, 0f),
                                end = Offset(offsetAnim, offsetAnim),
                                tileMode = TileMode.Mirror
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Первая буква названия (как аватарка)
                    Text(
                        text = item.shortName.take(1).uppercase(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.25f)
                    )
                }
            }

            // 3. ГРАДИЕНТ СНИЗУ (для читаемости текста)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
            )

            // 4. БЕЙДЖ ОСТАТКОВ (Верхний правый угол)
            val isLowStock = item.stockCount < 10
            val badgeColor = if (isLowStock) StardustError else Color.Black.copy(alpha = 0.5f)

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${item.stockCount}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // 5. ТЕКСТ (Снизу слева)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, end = 40.dp, bottom = 12.dp) // end padding чтобы не наехать на кнопку
            ) {
                Text(
                    text = item.shortName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                if (imageUrl == null) {
                    // Если нет фото, пишем полное имя мелко
                    Text(
                        text = item.fullName,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 6. КНОПКА "QUICK ADD" (Нижний правый угол)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(StardustPrimary) // Твой фирменный цвет
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Вибрация
                        onQuickAdd()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Взять 1",
                    tint = Color.Black, // Контрастный цвет иконки
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun StockLevelIndicator(
    stockCount: Int,
    modifier: Modifier = Modifier,
    maxStockReference: Int = 200
) {
    val stockPercentage = (stockCount.toFloat() / maxStockReference).coerceIn(0f, 1f)

    val barColor by animateColorAsState(
        targetValue = when {
            stockPercentage > 0.5f -> StardustSuccess
            stockPercentage > 0.15f -> StardustWarning
            else -> StardustError
        },
        animationSpec = tween(500),
        label = "stockColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth(0.6f)
            .height(8.dp)
            .clip(CircleShape)
            .background(StardustItemBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(stockPercentage)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(barColor)
        )
    }
}

@Composable
fun QuantityPickerDialog(
    item: DemoCatalogItem,
    onDismiss: () -> Unit,
    onConfirm: (DemoCatalogItem, Int) -> Unit
) {
    var stepperQuantity by remember { mutableStateOf(1) }
    var textQuantity by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = StardustModalBg)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val imageUrl = getImageUrl(item)
                if (imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
                        contentDescription = item.fullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                } else {
                    // Используем генеративный цвет и в диалоге тоже
                    val seedColor = generateColorFromName(item.fullName)
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(seedColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.shortName.take(1).uppercase(),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = item.fullName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = StardustTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.Inventory2, "В наличии", modifier = Modifier.size(16.dp), tint = StardustTextSecondary)
                    Text(
                        text = "В наличии: ${item.stockCount} ${item.unit}",
                        color = StardustTextSecondary,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                StockLevelIndicator(stockCount = item.stockCount)

                Spacer(modifier = Modifier.height(24.dp))

                if (item.unit == "грамм") {
                    OutlinedTextField(
                        value = textQuantity,
                        onValueChange = { textQuantity = it.filter { char -> char.isDigit() } },
                        label = { Text("Количество (грамм)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StardustPrimary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = StardustItemBg,
                            unfocusedContainerColor = StardustItemBg,
                            cursorColor = StardustPrimary,
                            focusedTextColor = StardustTextPrimary,
                            unfocusedTextColor = StardustTextPrimary,
                            focusedLabelColor = StardustTextSecondary,
                            unfocusedLabelColor = StardustTextSecondary
                        )
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        OutlinedIconButton(onClick = { if (stepperQuantity > 1) stepperQuantity-- }, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Default.Remove, "Уменьшить")
                        }
                        Text(stepperQuantity.toString(), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = StardustTextPrimary)
                        OutlinedIconButton(onClick = { stepperQuantity++ }, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Default.Add, "Увеличить")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val finalQuantity = if (item.unit == "грамм") textQuantity.toIntOrNull() ?: 0 else stepperQuantity
                            if (finalQuantity > 0) onConfirm(item, finalQuantity)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = (item.unit == "грамм" && textQuantity.isNotBlank()) || (item.unit != "грамм")
                    ) {
                        val quantityText = if (item.unit == "грамм") textQuantity else stepperQuantity.toString()
                        Text("Взять ${quantityText} ${item.unit}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NewItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("шт.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StardustModalBg,
        title = { Text("Новая запчасть", color = StardustTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Категория") })

                Text("Единица измерения:", color = StardustTextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("шт.", "грамм", "пар").forEach { unit ->
                        FilterChip(
                            selected = selectedUnit == unit,
                            onClick = { selectedUnit = unit },
                            label = { Text(unit) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, category, selectedUnit) },
                enabled = name.isNotBlank() && category.isNotBlank()
            ) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}