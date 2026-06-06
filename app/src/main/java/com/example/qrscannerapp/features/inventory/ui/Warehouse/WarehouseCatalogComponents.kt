package com.example.qrscannerapp.features.inventory.ui.Warehouse.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.qrscannerapp.features.scanner.ui.components.CameraView
import com.example.qrscannerapp.core.model.ScanEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.content.Context
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qrscannerapp.common.ui.AnimatedDialogWrapper
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.qrscannerapp.*
import com.example.qrscannerapp.UserRole
import com.example.qrscannerapp.features.inventory.data.OrderItem
import com.example.qrscannerapp.features.inventory.data.WarehouseItem
import com.example.qrscannerapp.features.inventory.ui.Warehouse.WarehouseViewModel
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val GITHUB_IMAGE_BASE_URL = "https://raw.githubusercontent.com/Sixnyder6/QrScannerApp/master/images/"

fun constructImageUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http")) return path
    val imageName = if (path.endsWith(".jpg") || path.endsWith(".png") || path.endsWith(".jpeg")) path else "$path.jpg"
    return GITHUB_IMAGE_BASE_URL + imageName
}

// Data classes for Navigation
data class NewItemData(
    val fullName: String,
    val shortName: String,
    val sku: String,
    val description: String?,
    val category: String,
    val unit: String,
    val totalStock: Int,
    val lowStockThreshold: Int
)

fun generateColorForCategory(categoryName: String): Color {
    val hash = categoryName.hashCode()
    val r = (hash and 0xFF0000 shr 16) / 255f
    val g = (hash and 0x00FF00 shr 8) / 255f
    val b = (hash and 0x0000FF) / 255f
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), hsv)
    hsv[1] = hsv[1].coerceIn(0.5f, 0.7f) // Насыщенность
    hsv[2] = hsv[2].coerceIn(0.8f, 0.95f) // Яркость
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun getContrastingTextColor(backgroundColor: Color): Color {
    val luminance = (0.299 * backgroundColor.red + 0.587 * backgroundColor.green + 0.114 * backgroundColor.blue)
    return if (luminance > 0.5) Color.Black else Color.White
}


@Composable
fun UploadConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Подтверждение") },
        text = { Text("Вы уверены, что хотите загрузить базу данных из файла?") },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text("Да")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun DeleteItemDialog(
    item: WarehouseItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StardustModalBg,
        titleContentColor = StardustTextPrimary,
        textContentColor = StardustTextSecondary,
        icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = StardustError) },
        title = { Text("Удалить запчасть?") },
        text = { Text("Вы уверены, что хотите удалить \"${item.shortName}\"? Это действие необратимо.") },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = StardustError)
            ) {
                Text("Удалить", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = StardustTextPrimary)
            }
        }
    )
}

@Composable
fun QuickTakeConfirmationDialog(
    item: WarehouseItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StardustModalBg,
        titleContentColor = StardustTextPrimary,
        textContentColor = StardustTextSecondary,
        icon = { Icon(Icons.Rounded.ShoppingCart, contentDescription = null, tint = StardustPrimary) },
        title = { Text("Подтвердите списание") },
        text = { Text("Вы уверены, что хотите списать 1 ${item.unit} \"${item.shortName}\"?") },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
            ) {
                Text("Списать 1", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = StardustTextPrimary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemDialog(
    item: WarehouseItem,
    onDismiss: () -> Unit,
    onConfirm: (fullName: String, shortName: String, sku: String, description: String?, category: String, unit: String, totalStock: Int, lowStockThreshold: Int) -> Unit
) {
    var fullName by remember { mutableStateOf(item.fullName) }
    var shortName by remember { mutableStateOf(item.shortName) }
    var sku by remember { mutableStateOf(item.sku ?: "") }
    var description by remember { mutableStateOf(item.description ?: "") }
    var category by remember { mutableStateOf(item.category) }
    var unit by remember { mutableStateOf(item.unit) }
    var totalStockStr by remember { mutableStateOf(item.totalStock.toString()) }
    var lowStockThresholdStr by remember { mutableStateOf(item.lowStockThreshold.toString()) }

    AnimatedDialogWrapper(onDismiss = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = StardustModalBg)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Редактирование",
                    style = MaterialTheme.typography.headlineSmall,
                    color = StardustTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Полное название") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StardustTextPrimary,
                        unfocusedTextColor = StardustTextPrimary,
                        focusedBorderColor = StardustPrimary,
                        unfocusedBorderColor = StardustTextSecondary
                    )
                )

                OutlinedTextField(
                    value = shortName,
                    onValueChange = { shortName = it },
                    label = { Text("Короткое название (для плитки)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StardustTextPrimary,
                        unfocusedTextColor = StardustTextPrimary,
                        focusedBorderColor = StardustPrimary,
                        unfocusedBorderColor = StardustTextSecondary
                    )
                )

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("Артикул (SKU)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StardustTextPrimary,
                        unfocusedTextColor = StardustTextPrimary,
                        focusedBorderColor = StardustPrimary,
                        unfocusedBorderColor = StardustTextSecondary
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StardustTextPrimary,
                        unfocusedTextColor = StardustTextPrimary,
                        focusedBorderColor = StardustPrimary,
                        unfocusedBorderColor = StardustTextSecondary
                    )
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Категория") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StardustTextPrimary,
                        unfocusedTextColor = StardustTextPrimary,
                        focusedBorderColor = StardustPrimary,
                        unfocusedBorderColor = StardustTextSecondary
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = totalStockStr,
                        onValueChange = { totalStockStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Общее кол-во") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StardustTextPrimary,
                            unfocusedTextColor = StardustTextPrimary,
                            focusedBorderColor = StardustPrimary,
                            unfocusedBorderColor = StardustTextSecondary
                        )
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Ед. изм.") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StardustTextPrimary,
                            unfocusedTextColor = StardustTextPrimary,
                            focusedBorderColor = StardustPrimary,
                            unfocusedBorderColor = StardustTextSecondary
                        )
                    )
                }

                OutlinedTextField(
                    value = lowStockThresholdStr,
                    onValueChange = { lowStockThresholdStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Порог «мало» (шт.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StardustTextPrimary,
                        unfocusedTextColor = StardustTextPrimary,
                        focusedBorderColor = StardustPrimary,
                        unfocusedBorderColor = StardustTextSecondary
                    )
                )

                Text(
                    text = "Внимание: при сохранении текущий остаток будет сброшен до общего количества!",
                    style = MaterialTheme.typography.bodySmall,
                    color = StardustWarning,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = StardustTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val totalStock = totalStockStr.toIntOrNull() ?: 0
                            val threshold = lowStockThresholdStr.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            if (fullName.isNotBlank() && shortName.isNotBlank() && totalStock > 0) {
                                onConfirm(fullName, shortName, sku, description.ifBlank { null }, category, unit, totalStock, threshold)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
                    ) {
                        Text("Сохранить", color = Color.Black)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun WarehouseCatalogTopAppBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearchClicked: () -> Unit,
    onCloseSearchClicked: () -> Unit,
    onNavigateBack: () -> Unit,
    onUploadClicked: () -> Unit,
    onScanClicked: () -> Unit,
    isAdmin: Boolean
) {
    val focusRequester = remember { FocusRequester() }

    TopAppBar(
        windowInsets = WindowInsets(0),
        title = {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                }, label = "Title <-> Search Field"
            ) { searchActive ->
                if (searchActive) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = LocalTextStyle.current.copy(color = StardustTextPrimary),
                        cursorBrush = SolidColor(StardustPrimary),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                  Text("Поиск...", color = StardustTextSecondary)
                            }
                            innerTextField()
                        }
                    )
                } else {
                    Text("Каталог запчастей")
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = {
                if (isSearchActive) {
                    onCloseSearchClicked()
                } else {
                    onNavigateBack()
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
        },
        actions = {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> -width } + fadeOut())
                }, label = "Actions"
            ) { searchActive ->
                if (searchActive) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Очистить поиск")
                        }
                    }
                } else {
                    Row {
                        IconButton(onClick = onScanClicked) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Сканировать деталь")
                        }
                        IconButton(onClick = onSearchClicked) {
                            Icon(Icons.Default.Search, contentDescription = "Поиск по каталогу")
                        }
                        if (isAdmin) {
                            IconButton(onClick = onUploadClicked) {
                                Icon(Icons.Outlined.CloudUpload, contentDescription = "Загрузить базу")
                            }
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = StardustTextPrimary,
            actionIconContentColor = StardustTextPrimary,
            navigationIconContentColor = StardustTextPrimary
        )
    )

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(100)
            focusRequester.requestFocus()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChip(
    categoryName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val baseColor = remember(categoryName) { generateColorForCategory(categoryName) }

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) baseColor else baseColor.copy(alpha = 0.15f),
        label = "chipBgColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) getContrastingTextColor(baseColor) else baseColor,
        label = "chipTextColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color.Transparent else baseColor.copy(alpha = 0.5f),
        label = "chipBorderColor"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.height(FilterChipDefaults.Height)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = isSelected) {
                Icon(
                    Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                    tint = textColor
                )
            }
            Spacer(modifier = Modifier.width(if (isSelected) 8.dp else 0.dp))
            Text(
                text = categoryName,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}


@Composable
private fun ScannerViewfinder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "vf")
    val progress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "scan_line"
    )
    Box(
        modifier = modifier.drawBehind {
            val cl = 36.dp.toPx()
            val sw = 3.dp.toPx()
            val r  = 10.dp.toPx()
            val w  = size.width
            val h  = size.height
            val stroke = Stroke(width = sw, cap = StrokeCap.Round)
            listOf(
                Path().apply { moveTo(0f, cl); lineTo(0f, r); arcTo(Rect(0f, 0f, r*2, r*2), 180f, 90f, false); lineTo(cl, 0f) },
                Path().apply { moveTo(w-cl, 0f); lineTo(w-r, 0f); arcTo(Rect(w-r*2, 0f, w, r*2), 270f, 90f, false); lineTo(w, cl) },
                Path().apply { moveTo(0f, h-cl); lineTo(0f, h-r); arcTo(Rect(0f, h-r*2, r*2, h), 180f, -90f, false); lineTo(cl, h) },
                Path().apply { moveTo(w-cl, h); lineTo(w-r, h); arcTo(Rect(w-r*2, h-r*2, w, h), 90f, -90f, false); lineTo(w, h-cl) }
            ).forEach { drawPath(it, StardustPrimary, style = stroke) }
            val y = h * progress
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, StardustSecondary.copy(alpha = 0.7f), Color.Transparent),
                    startX = 0f, endX = w
                ),
                start = Offset(0f, y), end = Offset(w, y),
                strokeWidth = 2.dp.toPx()
            )
        }
    )
}

@Composable
private fun WarehouseScannerOverlay(
    hasPermission: Boolean,
    scanEventFlow: kotlinx.coroutines.flow.Flow<ScanEvent>,
    isTorchOn: Boolean,
    onTorchChange: (Boolean) -> Unit,
    onCodeScanned: (String) -> Unit,
    cartSize: Int,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Камера
        CameraView(
            isSearchMode = false,
            hasPermission = hasPermission,
            scanEventFlow = scanEventFlow,
            isTorchOn = isTorchOn,
            onTorchChange = onTorchChange,
            onCodeScanned = onCodeScanned,
            onStatusUpdate = { _, _ -> }
        )

        // Рамка видоискателя
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ScannerViewfinder(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(1f)
            )
        }

        // Верхняя плашка управления (заголовок, фонарик, закрыть)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть сканер",
                    tint = Color.White
                )
            }

            Text(
                text = "Сканирование деталей",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            IconButton(onClick = { onTorchChange(!isTorchOn) }) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Фонарик",
                    tint = if (isTorchOn) StardustWarning else Color.White
                )
            }
        }

        // Нижняя панель подсказки
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Наведите камеру на штрихкод детали",
                    color = StardustTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                if (cartSize > 0) {
                    Surface(
                        color = StardustPrimary.copy(alpha = 0.2f),
                        contentColor = StardustSecondary,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StardustPrimary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "В корзине: $cartSize поз.",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseCatalogScreen(
    items: List<WarehouseItem>,
    // --- Параметры для корзины ---
    cart: List<OrderItem>,
    onAddToCart: (WarehouseItem, Int) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onUpdateCartItemQuantity: (String, Int) -> Unit,
    onClearCart: () -> Unit,
    onSubmitOrder: () -> Unit,
    // ----------------------------
    onNavigateToAddItem: () -> Unit,
    onTakeItem: (WarehouseItem, Int) -> Unit, // Взять сразу (для всех теперь)
    isAdmin: Boolean,
    userRole: UserRole = UserRole.USER,
    onNavigateBack: () -> Unit
) {
    val canManage = isAdmin || userRole == UserRole.INVENTORY_MANAGER

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showUploadDialog by remember { mutableStateOf(false) }

    var itemToTake by remember { mutableStateOf<WarehouseItem?>(null) }
    var itemToEdit by remember { mutableStateOf<WarehouseItem?>(null) }
    var itemToDelete by remember { mutableStateOf<WarehouseItem?>(null) }
    var itemToQuickTake by remember { mutableStateOf<WarehouseItem?>(null) }

    var isCartExpanded by remember { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf("Все") }

    val snackbarHostState = remember { SnackbarHostState() }
    val viewModel: WarehouseViewModel = viewModel()

    var showScannerDialog by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }
    val scanEventFlow = remember { MutableSharedFlow<ScanEvent>() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(showScannerDialog) {
        if (showScannerDialog && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

    DisposableEffect(Unit) {
        onDispose {
            toneGen.release()
        }
    }

    // Debounce to prevent scanning same item multiple times in rapid succession
    var lastScannedCode by remember { mutableStateOf("") }
    var lastScannedTime by remember { mutableLongStateOf(0L) }

    val onCodeScanned = { code: String ->
        val now = System.currentTimeMillis()
        if (code != lastScannedCode || now - lastScannedTime > 2000L) {
            lastScannedCode = code
            lastScannedTime = now

            val foundItem = items.find { item ->
                item.sku == code ||
                item.id == code ||
                (item.sku != null && code.contains(item.sku))
            }

            if (foundItem != null) {
                // Success: Add to cart
                onAddToCart(foundItem, 1)

                // Sound & Haptic
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }

                scope.launch {
                    scanEventFlow.emit(ScanEvent.Success)
                }
            } else {
                // Error: item not found in database
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                scope.launch {
                    delay(150)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    snackbarHostState.showSnackbar("Товар не найден: $code")
                }
            }
        }
    }

    if (showUploadDialog) {
        UploadConfirmationDialog(
            onConfirm = { viewModel.uploadInitialData() },
            onDismiss = { showUploadDialog = false }
        )
    }

    val filteredCatalog = remember(searchQuery, selectedCategory, items) {
        items.filter {
            val matchesCategory = selectedCategory == "Все" || it.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    it.fullName.contains(searchQuery, ignoreCase = true) ||
                    it.shortName.contains(searchQuery, ignoreCase = true) ||
                    it.sku?.contains(searchQuery, ignoreCase = true) == true
            matchesCategory && matchesSearch
        }
    }

    val frequentlyTakenItems = remember(items) {
        items.sortedByDescending { it.stockCount }.take(8)
    }

    val quickConsumables = remember(items) {
        items.filter { it.category == "Расходники" }
    }

    val shelfHeight = if (!isCartExpanded && quickConsumables.isNotEmpty()) 80.dp else 0.dp


    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val targetHeight = remember(cart.size, isCartExpanded, canManage) {
        when {
            cart.isEmpty() || canManage -> 0.dp
            isCartExpanded -> screenHeight * 0.5f
            else -> 96.dp
        }
    }
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cartPanelHeight"
    )

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            WarehouseCatalogTopAppBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearchClicked = { isSearchActive = true },
                onCloseSearchClicked = {
                    isSearchActive = false
                    searchQuery = ""
                },
                onNavigateBack = onNavigateBack,
                onUploadClicked = { showUploadDialog = true },
                onScanClicked = { showScannerDialog = true },
                isAdmin = canManage
            )
        },
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(onClick = onNavigateToAddItem) {
                    Icon(Icons.Default.Add, "Добавить новую запчасть")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(padding)) {
                AnimatedVisibility(
                    visible = !isSearchActive,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Column {
                        if (frequentlyTakenItems.isNotEmpty()) {
                            FrequentlyTakenCarousel(
                                frequentlyTakenItems = frequentlyTakenItems,
                                onItemClick = { item -> itemToTake = item }
                            )
                        }

                        val categories = listOf("Все") + items.map { it.category }.distinct()
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(categories) { category ->
                                if (category == "Все") {
                                    FilterChip(
                                        selected = category == selectedCategory,
                                        onClick = { selectedCategory = category },
                                        label = { Text(category) }
                                    )
                                } else {
                                    CategoryChip(
                                        categoryName = category,
                                        isSelected = category == selectedCategory,
                                        onClick = { selectedCategory = category }
                                    )
                                }
                            }
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = padding.calculateBottomPadding() + animatedHeight + shelfHeight + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCatalog, key = { it.id }) { item ->
                        CatalogGridItem(
                            item = item,
                            onClick = { itemToTake = item },
                            onQuickAdd = {
                                if (!canManage) {
                                    onAddToCart(item, 1)
                                } else {
                                    itemToQuickTake = item
                                }
                            }
                        )
                    }
                }
            }

            if (!isCartExpanded && quickConsumables.isNotEmpty()) {
                QuickConsumablesShelf(
                    consumables = quickConsumables,
                    onAddConsumable = { item ->
                        if (!canManage) {
                            onAddToCart(item, 1)
                        } else {
                            itemToQuickTake = item
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = animatedHeight)
                )
            }

            BottomCartPanel(
                cart = cart,
                isExpanded = isCartExpanded,
                onExpandToggle = { isCartExpanded = it },
                onUpdateQuantity = onUpdateCartItemQuantity,
                onRemoveItem = onRemoveFromCart,
                onClearCart = onClearCart,
                onSubmit = onSubmitOrder,
                animatedHeight = animatedHeight,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

        }
    }

    // ДИАЛОГ ВЫБОРА КОЛИЧЕСТВА
    if (itemToTake != null) {
        QuantityPickerDialog(
            item = itemToTake!!,
            isAdmin = canManage,
            onDismiss = { itemToTake = null },
            onTake = { item, quantity ->
                onTakeItem(item, quantity)
                itemToTake = null
            },
            onAddToCart = { item, quantity ->
                onAddToCart(item, quantity)
                itemToTake = null
            },
            onEdit = {
                itemToEdit = itemToTake
                itemToTake = null
            },
            onDelete = {
                itemToDelete = itemToTake
                itemToTake = null
            }
        )
    }

    if (itemToEdit != null) {
        EditItemDialog(
            item = itemToEdit!!,
            onDismiss = { itemToEdit = null },
            onConfirm = { full, short, sku, desc, cat, unit, total, threshold ->
                viewModel.onEditItem(itemToEdit!!, full, short, sku, desc, cat, unit, total, threshold)
                itemToEdit = null
            }
        )
    }

    if (itemToDelete != null) {
        DeleteItemDialog(
            item = itemToDelete!!,
            onConfirm = {
                viewModel.onDeleteItem(itemToDelete!!)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    if (itemToQuickTake != null) {
        QuickTakeConfirmationDialog(
            item = itemToQuickTake!!,
            onConfirm = {
                onTakeItem(itemToQuickTake!!, 1)
            },
            onDismiss = { itemToQuickTake = null }
        )
    }

    AnimatedVisibility(
        visible = showScannerDialog,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
        exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it }
    ) {
        WarehouseScannerOverlay(
            hasPermission = hasCameraPermission,
            scanEventFlow = scanEventFlow,
            isTorchOn = isTorchOn,
            onTorchChange = { isTorchOn = it },
            onCodeScanned = onCodeScanned,
            cartSize = cart.size,
            onClose = { showScannerDialog = false }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(
    cartItems: List<OrderItem>,
    onDismiss: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onSubmitOrder: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StardustModalBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = StardustTextSecondary) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "Ваш заказ",
                style = MaterialTheme.typography.headlineSmall,
                color = StardustTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (cartItems.isEmpty()) {
                Text("Корзина пуста", color = StardustTextSecondary)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(cartItems) { item ->
                        CartItemRow(item, onRemove = { onRemoveItem(item.itemId) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSubmitOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary),
                enabled = cartItems.isNotEmpty()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Отправить заказ", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CartItemRow(item: OrderItem, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StardustItemBg, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.itemImageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(item.itemImageUrl).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.size(48.dp).background(StardustTextSecondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.itemName.take(1), color = StardustTextSecondary)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.itemName, color = StardustTextPrimary, fontWeight = FontWeight.Bold)
            Text("${item.quantity} ${item.unit}", color = StardustPrimary)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Outlined.Delete, "Удалить", tint = StardustError)
        }
    }
}


@Composable
fun FrequentlyTakenCarousel(
    frequentlyTakenItems: List<WarehouseItem>,
    onItemClick: (WarehouseItem) -> Unit
) {
    val carouselItems = remember(frequentlyTakenItems) {
        if (frequentlyTakenItems.size > 2) frequentlyTakenItems + frequentlyTakenItems else frequentlyTakenItems
    }
    val scrollState = rememberLazyListState()
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        while (coroutineContext.isActive && frequentlyTakenItems.isNotEmpty()) {
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
                    animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing)
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
    item: WarehouseItem,
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
            val finalImageUrl = constructImageUrl(item.imageUrl)

            if (finalImageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(finalImageUrl)
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

@Composable
fun CatalogGridItem(
    item: WarehouseItem,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit
) {
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
            .aspectRatio(1f)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val finalImageUrl = constructImageUrl(item.imageUrl)

            if (finalImageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(finalImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.fullName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val seedColor = generateColorFromName(item.fullName)
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
                    Text(
                        text = item.shortName.take(1).uppercase(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.25f)
                    )
                }
            }
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
            val isLowStock = item.stockCount < item.lowStockThreshold
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
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, end = 40.dp, bottom = 12.dp)
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
                if (finalImageUrl == null) {
                    Text(
                        text = item.fullName,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(StardustPrimary)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQuickAdd()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Взять 1",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==========================================
// УЛУЧШЕННЫЕ КОМПОНЕНТЫ UI (STOCK & DIALOG)
// ==========================================

@Composable
fun StockLevelIndicator(
    currentStock: Int,
    maxStock: Int,
    modifier: Modifier = Modifier
) {
    val stockPercentage = if (maxStock > 0) {
        (currentStock.toFloat() / maxStock).coerceIn(0f, 1f)
    } else {
        0f
    }

    val targetColor = when {
        stockPercentage > 0.5f -> StardustSuccess
        stockPercentage > 0.2f -> StardustWarning
        else -> StardustError
    }

    val barColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(500),
        label = "stockColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth(0.8f)
            .height(10.dp)
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
    item: WarehouseItem,
    isAdmin: Boolean = false,
    onDismiss: () -> Unit,
    onTake: (WarehouseItem, Int) -> Unit, // Callback для "Взять сразу"
    onAddToCart: (WarehouseItem, Int) -> Unit, // Callback для "В заказ"
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var stepperQuantity by remember { mutableIntStateOf(1) }
    var textQuantity by remember { mutableStateOf("") }

    AnimatedDialogWrapper(onDismiss = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = StardustModalBg)
        ) {
            Box {
                if (isAdmin) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = StardustTextSecondary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = StardustError.copy(alpha = 0.7f))
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val finalImageUrl = constructImageUrl(item.imageUrl)

                    if (finalImageUrl != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, StardustTextSecondary.copy(alpha = 0.2f)),
                            color = Color.White,
                            modifier = Modifier.size(140.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(finalImageUrl).crossfade(true).build(),
                                contentDescription = item.fullName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        val seedColor = generateColorFromName(item.fullName)
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(seedColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.shortName.take(1).uppercase(),
                                fontSize = 50.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = item.shortName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = StardustTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.fullName,
                        style = MaterialTheme.typography.bodySmall,
                        color = StardustTextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!item.sku.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = StardustTextSecondary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "SKU: ${item.sku}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = StardustTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Icon(Icons.Outlined.Inventory2, null, modifier = Modifier.size(18.dp).padding(bottom = 2.dp), tint = StardustTextSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
                                    append("${item.stockCount}")
                                }
                                withStyle(SpanStyle(color = StardustTextSecondary, fontSize = 14.sp)) {
                                    append(" из ${item.totalStock} ${item.unit}")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    StockLevelIndicator(currentStock = item.stockCount, maxStock = item.totalStock)

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
                                unfocusedBorderColor = StardustTextSecondary.copy(alpha = 0.5f),
                                focusedContainerColor = StardustItemBg,
                                unfocusedContainerColor = StardustItemBg,
                                focusedTextColor = StardustTextPrimary,
                                unfocusedTextColor = StardustTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Surface(
                                onClick = { if (stepperQuantity > 1) stepperQuantity-- },
                                shape = CircleShape,
                                color = StardustItemBg,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Remove, "Меньше", tint = StardustTextPrimary)
                                }
                            }

                            Text(
                                text = stepperQuantity.toString(),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StardustTextPrimary,
                                modifier = Modifier.widthIn(min = 60.dp),
                                textAlign = TextAlign.Center
                            )

                            Surface(
                                onClick = { stepperQuantity++ },
                                shape = CircleShape,
                                color = StardustItemBg,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Add, "Больше", tint = StardustTextPrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- КНОПКИ ДЕЙСТВИЯ ---

                    if (!isAdmin) {
                        // Для техников: Основное действие - "Добавить в заказ", второстепенное - "Взять сразу" (прямое списание)
                        Button(
                            onClick = {
                                val finalQuantity = if (item.unit == "грамм") textQuantity.toIntOrNull() ?: 0 else stepperQuantity
                                if (finalQuantity > 0) onAddToCart(item, finalQuantity)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StardustPrimary,
                                contentColor = Color.Black
                            ),
                            enabled = (item.unit == "грамм" && textQuantity.isNotBlank()) || (item.unit != "грамм")
                        ) {
                            Icon(Icons.Outlined.AddShoppingCart, null, modifier = Modifier.size(20.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Добавить в заказ (${if(item.unit == "грамм") textQuantity else stepperQuantity} ${item.unit})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                val finalQuantity = if (item.unit == "грамм") textQuantity.toIntOrNull() ?: 0 else stepperQuantity
                                if (finalQuantity > 0) onTake(item, finalQuantity)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, StardustPrimary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = StardustPrimary
                            ),
                            enabled = (item.unit == "грамм" && textQuantity.isNotBlank()) || (item.unit != "грамм")
                        ) {
                            Text(
                                text = "Взять сразу (списать)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Для админов: только одна кнопка прямого списания/приема товара
                        Button(
                            onClick = {
                                val finalQuantity = if (item.unit == "грамм") textQuantity.toIntOrNull() ?: 0 else stepperQuantity
                                if (finalQuantity > 0) onTake(item, finalQuantity)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StardustPrimary,
                                contentColor = Color.Black
                            ),
                            enabled = (item.unit == "грамм" && textQuantity.isNotBlank()) || (item.unit != "грамм")
                        ) {
                            Text(
                                text = "Взять ${if(item.unit == "грамм") textQuantity else stepperQuantity} ${item.unit}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

fun generateColorFromName(name: String): Color {
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

@Composable
fun OverlappingCardFan(
    cartItems: List<OrderItem>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val maxDisplayed = 7
        val displayedItems = cartItems.take(maxDisplayed)
        val extraCount = cartItems.size - maxDisplayed

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            displayedItems.forEachIndexed { index, item ->
                val cardSize = 56.dp
                val overlapOffset = (-16).dp
                
                val midIndex = (displayedItems.size - 1) / 2f
                val rotation = (index - midIndex) * 5f
                val translationY = kotlin.math.abs(index - midIndex) * 3f

                Card(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color.White),
                    colors = CardDefaults.cardColors(containerColor = StardustItemBg),
                    modifier = Modifier
                        .size(cardSize)
                        .graphicsLayer {
                            rotationZ = rotation
                            this.translationY = translationY.dp.toPx()
                        }
                        .padding(horizontal = 0.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val imgUrl = constructImageUrl(item.itemImageUrl)
                        if (imgUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imgUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.itemName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val seedColor = generateColorFromName(item.itemName)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(seedColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.itemName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(StardustPrimary)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${item.quantity}",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                if (index < displayedItems.size - 1) {
                    Spacer(modifier = Modifier.width(overlapOffset))
                }
            }

            if (extraCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StardustPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+$extraCount",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandedCartItemRow(
    item: OrderItem,
    onUpdateQuantity: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StardustItemBg, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val imgUrl = constructImageUrl(item.itemImageUrl)
        if (imgUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imgUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.itemName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            val seedColor = generateColorFromName(item.itemName)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(seedColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.itemName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.itemName,
                color = StardustTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.unit,
                color = StardustTextSecondary,
                fontSize = 11.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { onUpdateQuantity(item.quantity - 1) },
                modifier = Modifier
                    .size(28.dp)
                    .background(StardustModalBg, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Меньше",
                    tint = StardustTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "${item.quantity}",
                color = StardustTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.widthIn(min = 24.dp),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = { onUpdateQuantity(item.quantity + 1) },
                modifier = Modifier
                    .size(28.dp)
                    .background(StardustModalBg, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Больше",
                    tint = StardustTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Удалить",
                tint = StardustError.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun BottomCartPanel(
    cart: List<OrderItem>,
    isExpanded: Boolean,
    onExpandToggle: (Boolean) -> Unit,
    onUpdateQuantity: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onSubmit: () -> Unit,
    animatedHeight: Dp,
    modifier: Modifier = Modifier
) {
    if (cart.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = StardustModalBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        border = BorderStroke(1.dp, StardustPrimary.copy(alpha = 0.3f))
    ) {
        if (!isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onExpandToggle(true) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "Выбрано для списания",
                        color = StardustTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${cart.size} поз. (${cart.sumOf { it.quantity }} шт.)",
                        color = StardustTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                OverlappingCardFan(
                    cartItems = cart,
                    modifier = Modifier
                        .weight(2f)
                        .padding(horizontal = 4.dp)
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Развернуть",
                    tint = StardustPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Выбранные детали (${cart.size})",
                        color = StardustTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { onExpandToggle(false) }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Свернуть",
                            tint = StardustTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cart, key = { it.itemId }) { item ->
                        ExpandedCartItemRow(
                            item = item,
                            onUpdateQuantity = { qty -> onUpdateQuantity(item.itemId, qty) },
                            onRemove = { onRemoveItem(item.itemId) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onClearCart()
                            onExpandToggle(false)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StardustError.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StardustError)
                    ) {
                        Text("Отмена", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onSubmit()
                            onExpandToggle(false)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Забрать детали", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConsumableMiniCard(
    item: WarehouseItem,
    onAdd: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = StardustItemBg),
        border = BorderStroke(1.dp, StardustPrimary.copy(alpha = 0.2f)),
        modifier = Modifier
            .width(150.dp)
            .height(44.dp)
            .clickable { onAdd() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val imgUrl = constructImageUrl(item.imageUrl)
            if (imgUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imgUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.shortName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                val seedColor = generateColorFromName(item.shortName)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(seedColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.shortName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = item.shortName,
                color = StardustTextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                lineHeight = 11.sp
            )

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Добавить",
                tint = StardustPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun QuickConsumablesShelf(
    consumables: List<WarehouseItem>,
    onAddConsumable: (WarehouseItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                )
            )
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = StardustPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Быстрые расходники",
                color = StardustTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(consumables, key = { it.id }) { item ->
                ConsumableMiniCard(item = item, onAdd = { onAddConsumable(item) })
            }
        }
    }
}
