package com.example.qrscannerapp.features.street_doctor.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.qrscannerapp.common.upload.CloudinaryUploader
import com.example.qrscannerapp.common.upload.UploadFolder
import com.example.qrscannerapp.features.street_doctor.domain.model.RepairJob
import com.example.qrscannerapp.features.street_doctor.domain.model.ScooterFieldStatus
import com.example.qrscannerapp.features.street_doctor.domain.model.StreetScooter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val SdBg            = Color(0xFF0F0F13)
private val SdCard          = Color(0xFF1C1C22)
private val SdCardBorder    = Color(0xFF2A2A35)
private val SdPrimary       = Color(0xFF6C5CE7)
private val SdPrimaryDim    = Color(0xFF6C5CE7).copy(alpha = 0.12f)
private val SdTextMain      = Color(0xFFFFFFFF)
private val SdTextMuted     = Color(0xFF8E8E93)
private val SdTextSecondary = Color(0xFF7A7A90)
private val SdSuccess       = Color(0xFF4CAF50)
private val SdDanger        = Color(0xFFF44336)
private val SdStatusDone    = Color(0xFF10B981)

private val REPAIR_TYPES = listOf(
    "brakes"  to "Тормоза",
    "wheel"   to "Колесо",
    "battery" to "АКБ",
    "lock"    to "Замок",
    "display" to "Дисплей",
    "frame"   to "Рама",
    "cable"   to "Кабель",
    "other"   to "Другое"
)

private val STATUSES = listOf(
    ScooterFieldStatus.DONE       to ("Готов"     to SdSuccess),
    ScooterFieldStatus.NOT_FOUND  to ("Не найден" to SdTextMuted),
    ScooterFieldStatus.TO_STORAGE to ("На склад"  to SdDanger),
)

private fun fmtDate(ts: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(ts))

private fun statusLabel(status: ScooterFieldStatus) = when (status) {
    ScooterFieldStatus.DONE        -> "Готов"
    ScooterFieldStatus.TO_STORAGE  -> "На склад"
    ScooterFieldStatus.NOT_FOUND   -> "Не найден"
    ScooterFieldStatus.IN_PROGRESS -> "В работе"
    ScooterFieldStatus.NEW         -> "Новый"
}

@Composable
private fun InfoRow(label: String, value: String, mono: Boolean = false, accent: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SdTextSecondary, fontSize = 13.sp)
        Text(
            value,
            color = if (accent) SdPrimary else SdTextMain,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 200.dp)
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title.uppercase(),
            fontSize = 10.sp, fontWeight = FontWeight.Bold,
            color = SdTextMuted, letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SdCard)
                .border(0.5.dp, SdCardBorder, RoundedCornerShape(14.dp))
        ) { content() }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(color = SdCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SelectChip(label: String, selected: Boolean, color: Color = SdPrimary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color.copy(alpha = 0.15f) else SdCard)
            .border(0.5.dp, if (selected) color.copy(alpha = 0.5f) else SdCardBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (selected) Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 12.sp, color = if (selected) color else SdTextSecondary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun PassportScreen(
    scooter: StreetScooter,
    viewModel: StreetDoctorViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // Для загрузки фото — инжектируем напрямую (singleton)
    val cloudinaryUploader = remember { CloudinaryUploader() }

    var selectedRepairs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedStatus  by remember { mutableStateOf<ScooterFieldStatus?>(null) }
    var batteryPct      by remember { mutableStateOf<String?>(null) }
    var notes           by remember { mutableStateOf("") }
    var photoUris       by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Состояние загрузки
    var isUploading by remember { mutableStateOf(false) }
    var isSaved     by remember { mutableStateOf(false) }

    LaunchedEffect(isSaved) {
        if (isSaved) { delay(1000); onBack() }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        photoUris = (photoUris + uris).take(5)
    }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraUri != null) photoUris = (photoUris + cameraUri!!).take(5)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && cameraUri != null) cameraLauncher.launch(cameraUri!!)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short); viewModel.clearToast() }
    }

    val historyForScooter = emptyList<RepairJob>()

    Scaffold(containerColor = SdBg, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 40.dp)) {

            // ── Шапка ──
            item {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(SdPrimary.copy(alpha = 0.2f), Color.Transparent)))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(SdCard)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SdTextMain, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(scooter.code, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = SdTextMain, letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace)
                            Text("Паспорт самоката", fontSize = 11.sp, color = SdTextMuted)
                        }
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(SdPrimaryDim).border(1.dp, SdPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Text("SD", color = SdPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── Большой код ──
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(scooter.code, fontSize = 42.sp, fontWeight = FontWeight.Black, color = SdTextMain, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(SdSuccess))
                        Text(buildString { append("Активен"); if (scooter.model.isNotBlank()) append(" · ${scooter.model}") }, color = SdTextSecondary, fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = SdCardBorder, thickness = 0.5.dp)
                Spacer(Modifier.height(20.dp))
            }

            // ── Местоположение ──
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionCard("Местоположение") {
                        InfoRow("Адрес", scooter.address.ifBlank { "—" })
                        if (scooter.lat != 0.0) {
                            Divider()
                            InfoRow("Координаты", "%.5f, %.5f".format(scooter.lat, scooter.lon), mono = true)
                        }
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                val uri = android.net.Uri.parse("geo:${scooter.lat},${scooter.lon}?q=${scooter.lat},${scooter.lon}(${scooter.code})")
                                context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_VIEW, uri), "Открыть в картах"))
                            }.padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SdPrimary))
                            Text("Открыть на карте", color = SdPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = SdPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Характеристики ──
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionCard("Характеристики") {
                        InfoRow("Номер", scooter.code, mono = true, accent = true)
                        if (scooter.model.isNotBlank()) { Divider(); InfoRow("Модель", scooter.model) }
                        Divider()
                        InfoRow("Заряд (из задания)", "${scooter.batteryPct}%", accent = true)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Осмотр на месте ──
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionCard("Осмотр на месте") {
                        Column(modifier = Modifier.padding(16.dp)) {

                            // Заряд АКБ
                            Text("ЗАРЯД АКБ (факт)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SdTextMuted, letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("0", "25", "50", "75", "99").forEach { v ->
                                    val sel = batteryPct == v
                                    Box(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                            .background(if (sel) SdPrimaryDim else Color(0xFF1E1E28))
                                            .border(0.5.dp, if (sel) SdPrimary.copy(0.5f) else SdCardBorder, RoundedCornerShape(8.dp))
                                            .clickable { batteryPct = if (batteryPct == v) null else v }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$v%", fontSize = 12.sp, color = if (sel) SdPrimary else SdTextMuted, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Виды работ
                            Text("ВИДЫ РАБОТ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SdTextMuted, letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                REPAIR_TYPES.forEach { (key, label) ->
                                    SelectChip(label = label, selected = key in selectedRepairs) {
                                        selectedRepairs = if (key in selectedRepairs) selectedRepairs - key else selectedRepairs + key
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Фото
                            Text("ФОТО", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SdTextMuted, letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))

                            if (photoUris.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(88.dp)) {
                                    items(photoUris) { uri ->
                                        Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp))) {
                                            AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                            Box(
                                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.65f)).clickable { photoUris = photoUris - uri },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            if (photoUris.size < 5) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val photoFile = java.io.File(context.cacheDir, "sd_photo_${System.currentTimeMillis()}.jpg")
                                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
                                                cameraUri = uri
                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                    cameraLauncher.launch(uri)
                                                } else {
                                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                }
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SdTextMain),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, SdCardBorder)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Камера", fontSize = 13.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SdTextMain),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, SdCardBorder)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Галерея", fontSize = 13.sp)
                                    }
                                }
                                if (photoUris.isNotEmpty()) {
                                    Text("${photoUris.size} / 5", color = SdTextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Заметка
                            Text("ЗАМЕТКА", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SdTextMuted, letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E1E28)).border(0.5.dp, SdCardBorder, RoundedCornerShape(10.dp)).padding(12.dp)) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = SdTextMain, fontSize = 13.sp),
                                    decorationBox = { inner ->
                                        if (notes.isEmpty()) Text("Что сделано, что заметили...", color = SdTextMuted, fontSize = 13.sp)
                                        inner()
                                    }
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            // Итоговый статус
                            Text("ИТОГОВЫЙ СТАТУС", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SdTextMuted, letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                STATUSES.forEach { (status, pair) ->
                                    val (label, color) = pair
                                    SelectChip(label = label, selected = selectedStatus == status, color = color) { selectedStatus = status }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Кнопка завершить ──
            item {
                val canSave = selectedStatus != null && !isUploading && !isSaved

                Button(
                    onClick = {
                        if (!canSave) return@Button
                        scope.launch {
                            isUploading = true
                            try {
                                val uploadedUrls = photoUris.mapNotNull { uri ->
                                    cloudinaryUploader.uploadImage(context, uri, UploadFolder.REPAIR)
                                }

                                if (photoUris.isNotEmpty() && uploadedUrls.isEmpty()) {
                                    snackbarHostState.showSnackbar("Не удалось загрузить фото — проверьте интернет")
                                } else if (uploadedUrls.size < photoUris.size) {
                                    snackbarHostState.showSnackbar("Загружено ${uploadedUrls.size} из ${photoUris.size} фото")
                                }

                                val repairList = selectedRepairs.toList()
                                val battInt    = batteryPct?.toIntOrNull()

                                when (selectedStatus) {
                                    ScooterFieldStatus.DONE ->
                                        viewModel.completeRepair(
                                            scooterId      = scooter.id,
                                            repairTypes    = repairList,
                                            batteryPctReal = battInt,
                                            notes          = notes.trim(),
                                            photoUrls      = uploadedUrls
                                        )
                                    ScooterFieldStatus.TO_STORAGE ->
                                        viewModel.sendToStorage(
                                            scooterId      = scooter.id,
                                            repairTypes    = repairList,
                                            batteryPctReal = battInt,
                                            notes          = notes.trim(),
                                            photoUrls      = uploadedUrls
                                        )
                                    ScooterFieldStatus.NOT_FOUND ->
                                        viewModel.markNotFound(scooter.id)
                                    else -> {}
                                }
                                isSaved = true
                            } finally {
                                isUploading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isSaved     -> SdSuccess
                            isUploading -> SdPrimary.copy(alpha = 0.6f)
                            canSave     -> SdPrimary
                            else        -> SdCard
                        },
                        contentColor = Color.White
                    ),
                    enabled = canSave
                ) {
                    when {
                        isSaved -> {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Сохранено ✓", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, modifier = Modifier.padding(vertical = 6.dp))
                        }
                        isUploading -> {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (photoUris.isEmpty()) "Сохраняем..." else "Загружаем фото...",
                                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        else -> {
                            Text("Завершить осмотр", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, modifier = Modifier.padding(vertical = 6.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── История ремонтов ──
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionCard("История ремонтов") {
                        if (historyForScooter.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("История пуста", color = SdTextMuted, fontSize = 13.sp)
                            }
                        } else {
                            historyForScooter.forEachIndexed { i, job ->
                                if (i > 0) Divider()
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(job.repairTypes.joinToString(", ") { key -> REPAIR_TYPES.find { it.first == key }?.second ?: key }.ifBlank { "Осмотр" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SdTextMain)
                                        Text(fmtDate(job.timestamp), fontSize = 11.sp, color = SdTextMuted, fontFamily = FontFamily.Monospace)
                                    }
                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(job.technicianName, fontSize = 12.sp, color = SdTextSecondary)
                                        val statusColor = when (job.status) { ScooterFieldStatus.DONE -> SdStatusDone; ScooterFieldStatus.TO_STORAGE -> SdDanger; else -> SdTextMuted }
                                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(statusColor.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                            Text(statusLabel(job.status), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
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