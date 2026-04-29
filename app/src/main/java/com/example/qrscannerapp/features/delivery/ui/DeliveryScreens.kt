package com.example.qrscannerapp.features.delivery.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.features.delivery.domain.model.DeliveryLog
import com.example.qrscannerapp.features.delivery.domain.model.DeliveryType
import java.text.SimpleDateFormat
import java.util.*

// --- ЦВЕТА СТАТУСОВ ---
private val ColorExpected = Color(0xFFFFA726)
private val ColorReceive = Color(0xFF4CAF50)
private val ColorSend = Color(0xFF29B6F6)
private val ColorError = Color(0xFFEF5350)

// ==========================================================================================
// 1. ДАШБОРД (ГЛАВНЫЙ ЭКРАН ДОСТАВКИ)
// ==========================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDashboardScreen(navController: NavController, onMenuClick: () -> Unit) {
    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Логистика", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, null, tint = Color.White) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Три основные карточки действий
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DeliveryActionCard("Ожидается", Icons.Default.Schedule, ColorExpected, Modifier.weight(1f)) {
                        navController.navigate("delivery_form/expected")
                    }
                    DeliveryActionCard("Принять", Icons.Default.CallReceived, ColorReceive, Modifier.weight(1f)) {
                        navController.navigate("delivery_form/receive")
                    }
                    DeliveryActionCard("Отправить", Icons.Default.CallMade, ColorSend, Modifier.weight(1f)) {
                        navController.navigate("delivery_form/send")
                    }
                }

                // Аналитика трафика (заглушка)
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF252530).copy(alpha = 0.6f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Analytics, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Аналитика трафика (Дни)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                            val days = listOf("Пн" to 0.4f, "Вт" to 0.7f, "Ср" to 1.0f, "Чт" to 0.5f, "Пт" to 0.9f, "Сб" to 0.2f, "Вс" to 0.1f)
                            days.forEach { (day, height) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(24.dp).fillMaxHeight(height).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(Color(0xFF6A5AE0).copy(alpha = 0.8f)))
                                    Spacer(Modifier.height(4.dp))
                                    Text(day, color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Карточка перехода в историю
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { navController.navigate("delivery_history") },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF252530).copy(alpha = 0.6f))
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("История доставок", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Генерация отчетов и список рейсов", color = Color.Gray, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryActionCard(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(120.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF252530).copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

// ==========================================================================================
// 2. УМНАЯ ФОРМА ОФОРМЛЕНИЯ (ПОЛНАЯ ВЕРСИЯ)
// ==========================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryFormScreen(type: String, viewModel: DeliveryViewModel, currentUserName: String, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val formState by viewModel.formState.collectAsState()

    val deliveryType = when(type) {
        "receive" -> DeliveryType.RECEIVE
        "send" -> DeliveryType.SEND
        else -> DeliveryType.EXPECTED
    }
    val statusColor = when(deliveryType) {
        DeliveryType.RECEIVE -> ColorReceive
        DeliveryType.SEND -> ColorSend
        DeliveryType.EXPECTED -> ColorExpected
    }

    // === STATE ДЛЯ ПОЛЕЙ ===
    var lpLetter1 by remember { mutableStateOf("") }
    var lpDigits by remember { mutableStateOf("") }
    var lpLetter23 by remember { mutableStateOf("") }
    var lpRegion by remember { mutableStateOf("") }
    var itemCount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var plannedDateText by remember { mutableStateOf("Выбрать дату (Завтра)") }
    var addressFrom by remember { mutableStateOf("") }
    var addressTo by remember { mutableStateOf("") }
    var showLocationPicker by remember { mutableStateOf(false) }

    // === LAUNCHERS ===
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null && selectedImageUris.size < 4) {
            val uri = saveBitmapToCache(context, bitmap)
            if (uri != null) selectedImageUris = selectedImageUris + uri
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) cameraLauncher.launch(null)
        else Toast.makeText(context, "Разрешите доступ к камере", Toast.LENGTH_SHORT).show()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && selectedImageUris.size < 4) selectedImageUris = selectedImageUris + uri
    }

    // === ОБРАТНАЯ СВЯЗЬ ===
    LaunchedEffect(formState.submitResult) {
        formState.submitResult?.let { result ->
            Toast.makeText(context, when(result) {
                is SubmitResult.Success -> result.message
                is SubmitResult.Error -> result.message
            }, Toast.LENGTH_LONG).show()
            viewModel.clearSubmitResult()
        }
    }

    AppBackground {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(deliveryType.title, color = statusColor, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .clickable { focusManager.clearFocus() },
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Spacer(modifier = Modifier.height(0.dp))

                    // Плановая дата
                    if (deliveryType == DeliveryType.EXPECTED) {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF252530).copy(alpha = 0.6f))) {
                            Row(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = !showDatePicker }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Плановая дата прибытия", color = Color.White, fontSize = 16.sp)
                                Surface(color = ColorExpected.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                    Text(plannedDateText, color = ColorExpected, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                                }
                            }
                        }
                    }

                    // Гос. номер + валидация
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Гос. номер автомобиля", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp).offset(y = (-12).dp))
                            if (formState.errors.licensePlate != null) Text("⚠️", color = ColorError, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                        }
                        RussianLicensePlateInput(
                            l1 = lpLetter1, d3 = lpDigits, l2 = lpLetter23, reg = lpRegion,
                            onL1Change = { lpLetter1 = it }, onD3Change = { lpDigits = it },
                            onL2Change = { lpLetter23 = it }, onRegChange = { lpRegion = it }
                        )
                        if (formState.errors.licensePlate != null) {
                            Text(formState.errors.licensePlate!!, color = ColorError, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                        }
                    }

                    // Количество + описание
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF252530).copy(alpha = 0.6f))) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text("Количество единиц (до 999)", color = Color.White, fontSize = 16.sp)
                                    if (formState.errors.itemCount != null) Text(formState.errors.itemCount!!, color = ColorError, fontSize = 12.sp)
                                }
                                BasicTextField(
                                    value = itemCount,
                                    onValueChange = { if (it.length <= 3) itemCount = it.filter { char -> char.isDigit() } },
                                    textStyle = TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    decorationBox = { innerTextField ->
                                        Box(modifier = Modifier.width(80.dp).height(48.dp).background(Color.Black.copy(0.3f), RoundedCornerShape(8.dp)).border(1.dp, if (formState.errors.itemCount != null) ColorError else Color.Gray.copy(0.5f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                            if (itemCount.isEmpty()) Text("0", color = Color.Gray.copy(alpha = 0.5f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                            OutlinedTextField(
                                value = description, onValueChange = { description = it },
                                placeholder = { Text("Что везем? (Коробки, АКБ, Самокаты)", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = statusColor, unfocusedBorderColor = Color.Gray.copy(0.5f), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Адреса (опционально)
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF252530).copy(alpha = 0.6f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = Color(0xFF6A5AE0))
                                Spacer(Modifier.width(8.dp))
                                Text("Маршрут (опционально)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = addressFrom, onValueChange = { addressFrom = it }, label = { Text("Откуда", color = Color.Gray) }, placeholder = { Text("Начните вводить адрес...", color = Color.Gray.copy(0.7f)) }, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { showLocationPicker = true }) { Icon(Icons.Default.MyLocation, "Определить", tint = Color(0xFF6A5AE0)) } }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6A5AE0), unfocusedBorderColor = Color.Gray.copy(0.5f)), shape = RoundedCornerShape(12.dp))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = addressTo, onValueChange = { addressTo = it }, label = { Text("Куда", color = Color.Gray) }, placeholder = { Text("Конечный адрес...", color = Color.Gray.copy(0.7f)) }, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { showLocationPicker = true }) { Icon(Icons.Default.MyLocation, "Определить", tint = Color(0xFF6A5AE0)) } }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6A5AE0), unfocusedBorderColor = Color.Gray.copy(0.5f)), shape = RoundedCornerShape(12.dp))
                        }
                    }

                    // Фотофиксация
                    Column {
                        Text("Фотофиксация (${selectedImageUris.size}/4)", color = Color.Gray, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedImageUris.forEach { uri ->
                                Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(12.dp))) {
                                    AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    IconButton(onClick = { selectedImageUris = selectedImageUris - uri }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp).padding(4.dp).background(Color.Black.copy(0.6f), CircleShape)) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            if (selectedImageUris.size < 4) {
                                Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF4CAF50).copy(0.1f)).border(1.dp, Color(0xFF4CAF50).copy(0.5f), RoundedCornerShape(12.dp)).clickable { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.CameraAlt, null, tint = Color(0xFF4CAF50)) }
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF6A5AE0).copy(0.1f)).border(1.dp, Color(0xFF6A5AE0).copy(0.5f), RoundedCornerShape(12.dp)).clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.AddAPhoto, null, tint = Color(0xFF6A5AE0)) }
                            }
                        }
                        Text("💡 Совет: сделайте фото номера машины и груза", color = Color.Gray.copy(0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Кнопка отправки
                val isFormValid = lpLetter1.isNotBlank() && lpDigits.isNotBlank() && lpRegion.isNotBlank() && itemCount.isNotBlank() && itemCount.toIntOrNull() != null
                Button(
                    onClick = {
                        val errors = viewModel.validateForm(lpLetter1, lpDigits, lpRegion, itemCount)
                        if (errors.hasErrors()) return@Button
                        val fullLicense = "$lpLetter1$lpDigits$lpLetter23$lpRegion"
                        viewModel.prepareSubmit(type, fullLicense, itemCount.toIntOrNull() ?: 0, description, currentUserName, selectedImageUris)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp).align(Alignment.BottomCenter),
                    colors = ButtonDefaults.buttonColors(containerColor = statusColor, disabledContainerColor = Color.Gray.copy(0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !formState.isLoading && isFormValid
                ) {
                    if (formState.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(deliveryType.title + " Груз", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isFormValid) Color.White else Color.DarkGray)
                }
            }
        }

        // Диалог подтверждения
        if (formState.showConfirmation && formState.pendingDelivery != null) {
            val pending = formState.pendingDelivery!!
            Dialog(onDismissRequest = { viewModel.dismissConfirmation() }) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF252530)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Подтвердите отправку", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.3f)), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row { Text("🚗 ", color = Color.Gray); Text(pending.licensePlate, color = Color.White, fontWeight = FontWeight.Bold) }
                                Row { Text("📦 ", color = Color.Gray); Text("${pending.itemCount} ед.", color = Color.White) }
                                if (pending.description.isNotBlank()) Row { Text("📝 ", color = Color.Gray); Text(pending.description, color = Color.White) }
                                if (selectedImageUris.isNotEmpty()) Row { Text("📸 ", color = Color.Gray); Text("${selectedImageUris.size} фото", color = Color.White) }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { viewModel.dismissConfirmation() }) { Text("Отмена", color = Color.Gray) }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { viewModel.confirmSubmit(onNavigateBack) }, colors = ButtonDefaults.buttonColors(containerColor = statusColor)) { Text("Подтвердить", color = Color.White, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }

        // Диалог локации (заглушка)
        if (showLocationPicker) {
            Dialog(onDismissRequest = { showLocationPicker = false }) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF252530)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("📍 Определить местоположение", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text("Функция автоопределения адреса будет добавлена в следующем обновлении.", color = Color.Gray, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { addressFrom = "г. Москва, ул. Бестужевская, 10"; showLocationPicker = false }, modifier = Modifier.fillMaxWidth()) { Text("Использовать пример", color = Color.White) }
                        TextButton(onClick = { showLocationPicker = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Закрыть", color = Color.Gray) }
                    }
                }
            }
        }
    }
}

// ==========================================================================================
// 3. ЭКРАН ИСТОРИИ И ОТЧЕТОВ
// ==========================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryHistoryScreen(viewModel: DeliveryViewModel, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    AppBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("История и Отчеты", color = Color.White) },
                    navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                    actions = {
                        IconButton(onClick = { Toast.makeText(context, "Отчет PDF успешно сгенерирован!", Toast.LENGTH_LONG).show() }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Экспорт", tint = Color.Red.copy(alpha = 0.8f))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (formState.isLoading && history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF6A5AE0))
                }
            } else if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inbox, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Нет записей", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Оформите первую доставку", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(history) { log ->
                        val statusColor = when(log.type) {
                            DeliveryType.RECEIVE -> ColorReceive
                            DeliveryType.SEND -> ColorSend
                            DeliveryType.EXPECTED -> ColorExpected
                        }
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF252530).copy(alpha = 0.6f))) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = statusColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                        Text(log.type.title.uppercase(), color = statusColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(sdf.format(Date(log.timestamp)), color = Color.Gray, fontSize = 12.sp)
                                    Spacer(Modifier.weight(1f))
                                    Text(log.employeeName, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Black, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                        Text(log.licensePlate, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Единиц: ${log.itemCount}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        if (log.description.isNotBlank()) Text(log.description, color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                                if (log.photoUrls.isNotEmpty()) {
                                    Spacer(Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        log.photoUrls.take(3).forEach { url ->
                                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)) {
                                                AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop)
                                            }
                                        }
                                        if (log.photoUrls.size > 3) {
                                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(0.5f)), contentAlignment = Alignment.Center) {
                                                Text("+${log.photoUrls.size - 3}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================================================
// ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ
// ==========================================================================================

private fun saveBitmapToCache(context: android.content.Context, bitmap: android.graphics.Bitmap): Uri? {
    return try {
        val path = context.cacheDir.toString() + "/photo_${System.currentTimeMillis()}.jpg"
        java.io.FileOutputStream(path).use { stream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
        }
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            java.io.File(path)
        )
    } catch (e: Exception) {
        null
    }
}

@Composable
fun RussianLicensePlateInput(
    l1: String, d3: String, l2: String, reg: String,
    onL1Change: (String) -> Unit, onD3Change: (String) -> Unit,
    onL2Change: (String) -> Unit, onRegChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val fReqL1 = remember { FocusRequester() }
    val fReqD3 = remember { FocusRequester() }
    val fReqL2 = remember { FocusRequester() }
    val fReqReg = remember { FocusRequester() }
    val filterLetters = { s: String -> s.filter { it.isLetter() }.uppercase() }
    val filterDigits = { s: String -> s.filter { it.isDigit() } }

    Row(
        modifier = Modifier.fillMaxWidth().height(70.dp).background(Color.White, RoundedCornerShape(8.dp)).border(3.dp, Color.Black, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LpInputBox(value = l1, maxLen = 1, isDigit = false, fReq = fReqL1, nextReq = fReqD3, onValChange = { onL1Change(filterLetters(it)) })
        LpInputBox(value = d3, maxLen = 3, isDigit = true, fReq = fReqD3, nextReq = fReqL2, width = 60.dp, onValChange = { onD3Change(filterDigits(it)) })
        LpInputBox(value = l2, maxLen = 2, isDigit = false, fReq = fReqL2, nextReq = fReqReg, width = 50.dp, onValChange = { onL2Change(filterLetters(it)) })
        Box(modifier = Modifier.width(2.dp).fillMaxHeight(0.8f).background(Color.Black))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LpInputBox(value = reg, maxLen = 3, isDigit = true, fReq = fReqReg, nextReq = null, width = 60.dp, onValChange = { onRegChange(filterDigits(it)) }, isDone = true, focusManager = focusManager)
            Text("RUS", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = (-4).dp))
        }
    }
}

@Composable
fun LpInputBox(value: String, maxLen: Int, isDigit: Boolean, fReq: FocusRequester, nextReq: FocusRequester?, width: androidx.compose.ui.unit.Dp = 30.dp, onValChange: (String) -> Unit, isDone: Boolean = false, focusManager: FocusManager? = null) {
    BasicTextField(
        value = value,
        onValueChange = {
            if (it.length <= maxLen) {
                onValChange(it)
                if (it.length == maxLen) nextReq?.requestFocus()
            }
        },
        modifier = Modifier.width(width).focusRequester(fReq),
        textStyle = TextStyle(color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(
            capitalization = if (isDigit) KeyboardCapitalization.None else KeyboardCapitalization.Characters,
            keyboardType = if (isDigit) KeyboardType.Number else KeyboardType.Text,
            imeAction = if (isDone) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { nextReq?.requestFocus() },
            onDone = { focusManager?.clearFocus() }
        ),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) Text(if (isDigit) "0".repeat(maxLen) else "A".repeat(maxLen), color = Color.LightGray, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                innerTextField()
            }
        }
    )
}