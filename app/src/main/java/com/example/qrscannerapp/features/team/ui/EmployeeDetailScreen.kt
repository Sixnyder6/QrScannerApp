package com.example.qrscannerapp.features.team.ui

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.qrscannerapp.*
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.features.profile.domain.model.*
import com.example.qrscannerapp.features.profile.ui.AdminControlCard
import com.example.qrscannerapp.features.profile.ui.DevicePerformanceCard
import com.example.qrscannerapp.features.profile.ui.InteractionStatsCard
import com.example.qrscannerapp.features.profile.ui.viewmodel.EmployeeProfileViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.concurrent.TimeUnit

// ============================================================================================
// DATA CLASSES
// ============================================================================================

private data class ActivitySummary(
    val scansToday: Int = 0,
    val batchesToday: Int = 0,
    val scanRatePerHour: Int = 0,
    val shiftDurationMinutes: Long = 0,
    val weeklyScans: List<Int> = emptyList(),
    val isLoaded: Boolean = false
)

private data class DeviceTelemetry(
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val batteryHealth: String = "",
    val isPowerSaveMode: Boolean = false,
    val networkState: String = "Unknown",
    val networkPing: String = "",
    val freeRam: String = "",
    val freeStorage: String = "",
    val deviceUptime: String = "",
    val deviceInfo: String = "",
    val activeDeviceId: String = "",
    val appVersion: String = "",
    val lastSeen: Long = 0L,
    val telemetryUpdatedAt: Long = 0L,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationTimestamp: Long = 0L,
    val isLoaded: Boolean = false
)

// ============================================================================================
// WIDGET CARD — универсальная обёртка с раскрытием в модал
// ============================================================================================

@Composable
private fun WidgetCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    previewContent: @Composable () -> Unit = {},
    expandedContent: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Анимации для карточки-виджета
    val cardScale by animateFloatAsState(
        targetValue = if (expanded) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "card_scale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (expanded) 0.7f else 1f,
        animationSpec = tween(200),
        label = "card_alpha"
    )

    // Виджет
    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                alpha = cardAlpha
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { expanded = true },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            StardustGlassBg.copy(alpha = 0.95f),
                            StardustGlassBg.copy(alpha = 0.85f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .drawBehind {
                    // Левая цветная полоска
                    drawRect(
                        color = accentColor,
                        topLeft = Offset(0f, size.height * 0.2f),
                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height * 0.6f)
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon, null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                title,
                                color = StardustTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (subtitle.isNotBlank()) {
                                Text(
                                    subtitle,
                                    color = StardustTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        null,
                        tint = accentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                previewContent()
            }
        }
    }

    // Модальное раскрытие
    if (expanded) {
        ExpandedWidgetModal(
            icon = icon,
            title = title,
            accentColor = accentColor,
            onDismiss = { expanded = false },
            content = expandedContent
        )
    }
}

// ============================================================================================
// EXPANDED MODAL — iOS-стиль раскрытие
// ============================================================================================

@Composable
private fun ExpandedWidgetModal(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val backdropAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "backdrop"
    )
    val sheetOffsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "sheet_y"
    )
    val sheetScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "sheet_scale"
    )

    fun handleDismiss() {
        visible = false
    }

    // Следим за завершением анимации закрытия
    val sheetOffsetYState by rememberUpdatedState(sheetOffsetY)
    LaunchedEffect(visible) {
        if (!visible) {
            kotlinx.coroutines.delay(350)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { handleDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f * backdropAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { handleDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .graphicsLayer {
                        scaleX = sheetScale
                        scaleY = sheetScale
                        translationY = (1f - (1f - sheetOffsetY)) * 200f
                        alpha = 1f - sheetOffsetY * 0.8f
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* блокируем закрытие при клике внутрь */ }
                    .shadow(32.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1C1830),
                                Color(0xFF12102A)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .drawBehind {
                        // Тонкая цветная граница сверху
                        drawRect(
                            color = accentColor.copy(alpha = 0.6f),
                            topLeft = Offset(size.width * 0.1f, 0f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.8f, 1.5.dp.toPx())
                        )
                    }
            ) {
                Column {
                    // Хэндл
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(StardustTextSecondary.copy(alpha = 0.3f))
                        )
                    }

                    // Заголовок модала
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accentColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                title,
                                color = StardustTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                        IconButton(
                            onClick = { handleDismiss() },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(StardustTextSecondary.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                Icons.Default.Close, null,
                                tint = StardustTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = StardustTextSecondary.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    // Контент
                    Box(modifier = Modifier.padding(20.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

// ============================================================================================
// SCREEN
// ============================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDetailScreen(
    userId: String,
    userName: String,
    userRole: String,
    onBack: () -> Unit,
    onWriteDm: () -> Unit,
    onEdit: () -> Unit = {},
    isAdmin: Boolean = false
) {
    val profileViewModel: EmployeeProfileViewModel = hiltViewModel()
    val profileState by profileViewModel.uiState.collectAsState()

    var activitySummary by remember { mutableStateOf(ActivitySummary()) }
    var telemetry by remember { mutableStateOf(DeviceTelemetry()) }
    var employeeShiftStartTime by remember { mutableStateOf(0L) }

    DisposableEffect(userId) {
        val db = Firebase.firestore
        var currentLastSeen = 0L

        val telemetryReg: ListenerRegistration = db
            .collection("device_telemetry")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("EmployeeDetail", "device_telemetry error", error); return@addSnapshotListener }
                if (snapshot != null && snapshot.exists()) {
                    telemetry = DeviceTelemetry(
                        batteryLevel = snapshot.getLong("lastBatteryLevel")?.toInt() ?: 0,
                        isCharging = snapshot.getBoolean("isCharging") ?: false,
                        batteryHealth = snapshot.getString("batteryHealth") ?: "",
                        isPowerSaveMode = snapshot.getBoolean("isPowerSaveMode") ?: false,
                        networkState = snapshot.getString("networkState") ?: "Unknown",
                        networkPing = snapshot.getString("networkPing") ?: "",
                        freeRam = snapshot.getString("freeRam") ?: "",
                        freeStorage = snapshot.getString("freeStorage") ?: "",
                        deviceUptime = snapshot.getString("deviceUptime") ?: "",
                        deviceInfo = snapshot.getString("deviceInfo") ?: "",
                        activeDeviceId = snapshot.getString("activeDeviceId") ?: "",
                        appVersion = snapshot.getString("appVersion") ?: "",
                        lastSeen = currentLastSeen,
                        telemetryUpdatedAt = snapshot.getLong("updatedAt") ?: 0L,
                        locationLat = snapshot.getDouble("locationLat"),
                        locationLng = snapshot.getDouble("locationLng"),
                        locationTimestamp = snapshot.getLong("locationTimestamp") ?: 0L,
                        isLoaded = true
                    )
                }
            }

        val internalReg: ListenerRegistration = db
            .collection("internal_users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("EmployeeDetail", "internal_users error", error); return@addSnapshotListener }
                if (snapshot != null && snapshot.exists()) {
                    currentLastSeen = snapshot.getLong("lastSeen") ?: 0L
                    employeeShiftStartTime = snapshot.getLong("shiftStartTime") ?: 0L
                    telemetry = telemetry.copy(lastSeen = currentLastSeen)
                }
            }

        onDispose { telemetryReg.remove(); internalReg.remove() }
    }

    LaunchedEffect(userId) {
        val db = Firebase.firestore
        try {
            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val todayLogs = db.collection("activity_log")
                .whereEqualTo("creatorId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startOfToday)
                .get().await()

            val scansToday = todayLogs.documents.sumOf { it.getLong("itemCount")?.toInt() ?: 0 }
            val batchesToday = todayLogs.documents.size
            val now = System.currentTimeMillis()
            val hoursElapsed = ((now - startOfToday) / 3_600_000.0).coerceAtLeast(1.0)
            val scanRate = if (scansToday > 0) (scansToday / hoursElapsed).toInt() else 0
            val firstLogTime = todayLogs.documents.mapNotNull { it.getLong("timestamp") }.minOrNull()
            val shiftMinutes = if (firstLogTime != null) TimeUnit.MILLISECONDS.toMinutes(now - firstLogTime) else 0L

            val startOfWeek = startOfToday - 6 * 24 * 60 * 60 * 1000L
            val weekLogs = db.collection("activity_log")
                .whereEqualTo("creatorId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startOfWeek)
                .get().await()

            val dailyCounts = IntArray(7)
            weekLogs.documents.forEach { doc ->
                val timestamp = doc.getLong("timestamp") ?: return@forEach
                val itemCount = doc.getLong("itemCount")?.toInt() ?: 0
                val dayIndex = ((timestamp - startOfWeek) / (24 * 60 * 60 * 1000L)).toInt().coerceIn(0, 6)
                dailyCounts[dayIndex] += itemCount
            }

            activitySummary = ActivitySummary(
                scansToday = scansToday, batchesToday = batchesToday,
                scanRatePerHour = scanRate, shiftDurationMinutes = shiftMinutes,
                weeklyScans = dailyCounts.toList(), isLoaded = true
            )
        } catch (e: Exception) {
            Log.e("EmployeeDetail", "Error loading activity", e)
            activitySummary = activitySummary.copy(isLoaded = true)
        }
    }

    val roleColor = when (userRole) {
        "admin" -> Color(0xFFEC407A)
        "inventory_manager" -> Color(0xFF4CAF50)
        "muver" -> Color(0xFF29B6F6)
        "electrician" -> Color(0xFFFFCA28)
        "technic" -> Color(0xFFAB47BC)
        "supervisor" -> Color(0xFFFF7043)
        "security" -> Color(0xFF78909C)
        else -> Color(0xFF78909C)
    }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0),
                    title = {
                        Text(
                            "Профиль",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StardustTextSecondary.copy(alpha = 0.1f))
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, "Назад",
                                tint = StardustTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = StardustTextPrimary
                    )
                )
            }
        ) { innerPadding ->
            if (profileState.isLoading && !activitySummary.isLoaded) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StardustPrimary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ── ШАПКА ──
                    item {
                        val profile = profileState.userProfile
                        ProfileHeader(
                            name = profile.name.takeIf { it != "Загрузка..." } ?: userName,
                            role = profile.role.takeIf { it.isNotBlank() } ?: UserRole.fromKey(userRole).displayName,
                            roleColor = roleColor,
                            isShiftActive = profile.isShiftActive,
                            photoUrl = profile.photoUrl
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // ── МЕТРИКИ СЕГОДНЯ — виджет ──
                    item {
                        val shiftDuration = if (profileState.userProfile.isShiftActive && employeeShiftStartTime > 0L)
                            TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - employeeShiftStartTime)
                        else activitySummary.shiftDurationMinutes

                        WidgetCard(
                            icon = Icons.Default.Today,
                            title = "Сегодня",
                            subtitle = "${activitySummary.scansToday} сканов · ${activitySummary.batchesToday} партий",
                            accentColor = StardustPrimary,
                            previewContent = {
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    MiniMetric("Сканов", activitySummary.scansToday.toString(), Icons.Default.QrCodeScanner, StardustPrimary)
                                    MiniMetric("Партий", activitySummary.batchesToday.toString(), Icons.Default.Inventory, Color(0xFF4CAF50))
                                    MiniMetric("Скан/час", activitySummary.scanRatePerHour.toString(), Icons.Default.Speed, Color(0xFFFFCA28))
                                    MiniMetric("Смена", formatShiftDuration(shiftDuration), Icons.Default.Timer, Color(0xFFFF7043))
                                }
                            }
                        ) {
                            // Expanded content
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MetricCard("Сканов", activitySummary.scansToday.toString(), Icons.Default.QrCodeScanner, Modifier.weight(1f))
                                    MetricCard("Партий", activitySummary.batchesToday.toString(), Icons.Default.Inventory, Modifier.weight(1f))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MetricCard("Скан/час", activitySummary.scanRatePerHour.toString(), Icons.Default.Speed, Modifier.weight(1f))
                                    MetricCard("На смене", formatShiftDuration(shiftDuration), Icons.Default.Timer, Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // ── АКТИВНОСТЬ ЗА НЕДЕЛЮ — виджет ──
                    if (activitySummary.weeklyScans.size == 7) {
                        item {
                            WidgetCard(
                                icon = Icons.Default.BarChart,
                                title = "Активность за неделю",
                                subtitle = "${activitySummary.weeklyScans.sum()} сканов за 7 дней",
                                accentColor = Color(0xFF4CAF50),
                                previewContent = {
                                    Spacer(Modifier.height(10.dp))
                                    MiniWeekChart(scans = activitySummary.weeklyScans)
                                }
                            ) {
                                WeeklyChart(scans = activitySummary.weeklyScans)
                            }
                        }
                    }

                    // ── СТАТИСТИКА ОПЕРАЦИЙ — виджет ──
                    item {
                        WidgetCard(
                            icon = Icons.Default.Analytics,
                            title = "Активность операций",
                            subtitle = "Статистика взаимодействий",
                            accentColor = Color(0xFFAB47BC)
                        ) {
                            InteractionStatsCard(
                                stats = profileState.interactionStats,
                                isLoading = profileState.isStatsLoading
                            )
                        }
                    }

                    // ── ПРОИЗВОДИТЕЛЬНОСТЬ — виджет ──
                    item {
                        WidgetCard(
                            icon = Icons.Default.Speed,
                            title = "Производительность",
                            subtitle = "Характеристики устройства",
                            accentColor = Color(0xFFFFCA28)
                        ) {
                            DevicePerformanceCard(details = profileState.performanceDetails)
                        }
                    }

                    // ── УСТРОЙСТВО + GPS — только для администраторов ──
                    if (isAdmin && telemetry.isLoaded) {
                        item {
                            val now = System.currentTimeMillis()
                            val isOnline = telemetry.lastSeen > 0L && (now - telemetry.lastSeen) < 3 * 60 * 1000L
                            val statusText = if (isOnline) "Онлайн" else {
                                val mins = TimeUnit.MILLISECONDS.toMinutes(now - telemetry.lastSeen)
                                when {
                                    mins < 60 -> "$mins мин. назад"
                                    mins < 1440 -> "${mins / 60} ч. назад"
                                    else -> "${mins / 1440} дн. назад"
                                }
                            }

                            val displayDevice = remember(telemetry.activeDeviceId, telemetry.deviceInfo) {
                                if (telemetry.activeDeviceId.isNotBlank() && telemetry.activeDeviceId.contains("_")) {
                                    telemetry.activeDeviceId.split("_").dropLast(1).joinToString(" ")
                                } else if (telemetry.activeDeviceId.isNotBlank()) {
                                    telemetry.activeDeviceId
                                } else {
                                    telemetry.deviceInfo
                                }
                            }

                            WidgetCard(
                                icon = Icons.Default.PhoneAndroid,
                                title = "Устройство",
                                subtitle = statusText,
                                accentColor = if (isOnline) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                previewContent = {
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Статус-точка
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val infiniteTransition = rememberInfiniteTransition(label = "pulse2")
                                            val pulseAlpha by infiniteTransition.animateFloat(
                                                1f, if (isOnline) 0.3f else 1f,
                                                infiniteRepeatable(tween(900), RepeatMode.Reverse), "p2"
                                            )
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                                                .background((if (isOnline) Color(0xFF4CAF50) else Color(0xFFFF5252)).copy(alpha = pulseAlpha)))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                if (isOnline) "Онлайн" else "Оффлайн",
                                                color = if (isOnline) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        // Батарея
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.BatteryStd, null,
                                                tint = when {
                                                    telemetry.batteryLevel > 50 -> Color(0xFF4CAF50)
                                                    telemetry.batteryLevel > 20 -> Color(0xFFFFCA28)
                                                    else -> Color(0xFFFF5252)
                                                },
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "${telemetry.batteryLevel}%",
                                                color = StardustTextPrimary,
                                                fontSize = 12.sp, fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (telemetry.appVersion.isNotBlank()) {
                                            Text(
                                                "v${telemetry.appVersion}",
                                                color = StardustTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            ) {
                                LiveDeviceCardContent(telemetry = telemetry)
                            }
                        }

                        // GPS виджет
                        val hasLocation = telemetry.locationLat != null && telemetry.locationLng != null
                        val locationFresh = hasLocation &&
                                (System.currentTimeMillis() - telemetry.locationTimestamp) < 15 * 60 * 1000L

                        if (locationFresh) {
                            item {
                                val minutesAgo = TimeUnit.MILLISECONDS.toMinutes(
                                    System.currentTimeMillis() - telemetry.locationTimestamp
                                )
                                val timeText = when {
                                    minutesAgo < 1 -> "только что"
                                    minutesAgo < 60 -> "$minutesAgo мин. назад"
                                    else -> "${minutesAgo / 60} ч. назад"
                                }
                                WidgetCard(
                                    icon = Icons.Default.LocationOn,
                                    title = "Местоположение",
                                    subtitle = timeText,
                                    accentColor = roleColor
                                ) {
                                    LocationCardContent(
                                        lat = telemetry.locationLat!!,
                                        lng = telemetry.locationLng!!,
                                        locationTimestamp = telemetry.locationTimestamp,
                                        roleColor = roleColor,
                                        employeeName = profileState.userProfile.name.takeIf { it != "Загрузка..." } ?: userName
                                    )
                                }
                            }
                        }
                    }

                    // ── ПУЛЬТ АДМИНА — виджет ──
                    if (isAdmin) {
                        item {
                            WidgetCard(
                                icon = Icons.Default.AdminPanelSettings,
                                title = "Управление доступом",
                                subtitle = if (profileState.userProfile.isShiftActive) "Смена активна" else "Смена завершена",
                                accentColor = Color(0xFFEC407A)
                            ) {
                                AdminControlCard(
                                    profile = profileState.userProfile,
                                    onForceEndShift = { profileViewModel.forceEndShift() },
                                    onSetWorkAccess = { isAllowed -> profileViewModel.setWorkAccess(isAllowed) }
                                )
                            }
                        }
                    }

                    // ── КНОПКИ ДЕЙСТВИЙ ──
                    item {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Кнопка "Написать" — главная
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                StardustPrimary,
                                                StardustPrimary.copy(red = StardustPrimary.red * 0.8f)
                                            )
                                        )
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onWriteDm() },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Chat, null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Написать",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            // Кнопка "Редактировать" — вторичная
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(StardustGlassBg)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onEdit() },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Edit, null,
                                        tint = StardustTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Редактировать",
                                        color = StardustTextSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// ============================================================================================
// МИНИ-ПРЕВЬЮ ВИДЖЕТОВ
// ============================================================================================

@Composable
private fun MiniMetric(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(2.dp))
        Text(value, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(label, color = StardustTextSecondary, fontSize = 9.sp)
    }
}

@Composable
private fun MiniWeekChart(scans: List<Int>) {
    val maxScans = scans.maxOrNull()?.takeIf { it > 0 } ?: 1
    Row(
        modifier = Modifier.fillMaxWidth().height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        scans.forEachIndexed { index, count ->
            val fraction = count.toFloat() / maxScans
            val isToday = index == 6
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(StardustPrimary.copy(alpha = 0.08f))
                )
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isToday) StardustPrimary
                                else Color(0xFF4CAF50).copy(alpha = 0.7f)
                            )
                    )
                }
            }
        }
    }
}

// ============================================================================================
// КОНТЕНТ ДЛЯ EXPANDED МОДАЛОВ
// ============================================================================================

@Composable
private fun LiveDeviceCardContent(telemetry: DeviceTelemetry) {
    val now = System.currentTimeMillis()
    val isOnline = telemetry.lastSeen > 0L && (now - telemetry.lastSeen) < 3 * 60 * 1000L

    val displayDevice = remember(telemetry.activeDeviceId, telemetry.deviceInfo) {
        if (telemetry.activeDeviceId.isNotBlank() && telemetry.activeDeviceId.contains("_")) {
            telemetry.activeDeviceId.split("_").dropLast(1).joinToString(" ")
        } else if (telemetry.activeDeviceId.isNotBlank()) {
            telemetry.activeDeviceId
        } else {
            telemetry.deviceInfo
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (telemetry.lastSeen > 0L) {
            val minutesAgo = TimeUnit.MILLISECONDS.toMinutes(now - telemetry.lastSeen)
            val pingText = when {
                minutesAgo < 1 -> "только что"
                minutesAgo < 60 -> "$minutesAgo мин. назад"
                minutesAgo < 1440 -> "${minutesAgo / 60} ч. назад"
                else -> "${minutesAgo / 1440} дн. назад"
            }
            val pingColor = when {
                isOnline -> Color(0xFF4CAF50)
                minutesAgo < 30 -> Color(0xFFFFCA28)
                else -> Color(0xFFFF5252)
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    1f, if (isOnline) 0.3f else 1f,
                    infiniteRepeatable(tween(900), RepeatMode.Reverse), "pulse_alpha"
                )
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(pingColor.copy(alpha = pulseAlpha)))
                Spacer(Modifier.width(8.dp))
                Text(if (isOnline) "Онлайн" else "Оффлайн", color = pingColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(pingText, color = StardustTextSecondary, fontSize = 12.sp)
            }
            HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.1f))
        }
        if (telemetry.appVersion.isNotBlank()) {
            DeviceRow("Версия", "v${telemetry.appVersion}", Icons.Default.PhoneAndroid, Color(0xFF4CAF50))
            HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.1f))
        }
        if (displayDevice.isNotBlank() && displayDevice != "Created by Admin") {
            DeviceRow("Устройство", displayDevice, Icons.Default.Smartphone)
            HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.1f))
        }
        val batteryColor = when {
            telemetry.isCharging -> Color(0xFF4CAF50)
            telemetry.batteryLevel > 50 -> Color(0xFF4CAF50)
            telemetry.batteryLevel > 20 -> Color(0xFFFFCA28)
            else -> Color(0xFFFF5252)
        }
        DeviceRow("Батарея", "${telemetry.batteryLevel}%${if (telemetry.isCharging) " (зарядка)" else ""}", Icons.Default.BatteryStd, batteryColor)
        if (telemetry.batteryHealth.isNotBlank() && telemetry.batteryHealth != "N/A") {
            HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.1f))
            DeviceRow("Здоровье батареи", telemetry.batteryHealth, Icons.Default.Thermostat)
        }
        HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.1f))
        DeviceRow("Энергосбережение", if (telemetry.isPowerSaveMode) "Включено" else "Выключено", Icons.Default.EnergySavingsLeaf,
            if (telemetry.isPowerSaveMode) Color(0xFFFFCA28) else StardustTextPrimary)
        HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.1f))
        val netIcon = when {
            telemetry.networkState.contains("WiFi") -> Icons.Default.Wifi
            telemetry.networkState.contains("Cellular") -> Icons.Default.SignalCellularAlt
            else -> Icons.Default.WifiOff
        }
        DeviceRow("Сеть", telemetry.networkState, netIcon, if (telemetry.networkState == "Offline") Color(0xFFFF5252) else StardustTextPrimary)
        if (telemetry.networkPing.isNotBlank() && telemetry.networkPing != "N/A") {
            HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.1f))
            DeviceRow("Пинг", telemetry.networkPing, Icons.Default.Speed)
        }
        if (telemetry.freeRam.isNotBlank() && telemetry.freeRam != "N/A") {
            HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.1f))
            DeviceRow("RAM (свободно)", telemetry.freeRam, Icons.Default.Memory)
        }
        if (telemetry.freeStorage.isNotBlank() && telemetry.freeStorage != "N/A") {
            HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.1f))
            DeviceRow("Память (свободно)", telemetry.freeStorage, Icons.Default.Storage)
        }
        if (telemetry.deviceUptime.isNotBlank() && telemetry.deviceUptime != "N/A") {
            HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.1f))
            DeviceRow("Uptime", telemetry.deviceUptime, Icons.Default.Update)
        }
        if (telemetry.telemetryUpdatedAt > 0L) {
            HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.1f))
            val minsSinceUpdate = TimeUnit.MILLISECONDS.toMinutes(now - telemetry.telemetryUpdatedAt)
            val updatedText = when {
                minsSinceUpdate < 1 -> "только что"
                minsSinceUpdate < 60 -> "$minsSinceUpdate мин. назад"
                minsSinceUpdate < 1440 -> "${minsSinceUpdate / 60} ч. назад"
                else -> "${minsSinceUpdate / 1440} дн. назад"
            }
            val freshColor = when {
                minsSinceUpdate < 5 -> Color(0xFF4CAF50)
                minsSinceUpdate < 15 -> Color(0xFFFFCA28)
                else -> Color(0xFFFF5252)
            }
            DeviceRow("Данные обновлены", updatedText, Icons.Default.Sync, freshColor)
        }
    }
}

@Composable
private fun LocationCardContent(
    lat: Double,
    lng: Double,
    locationTimestamp: Long,
    roleColor: Color,
    employeeName: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    onCreate(null)
                    onResume()
                    getMapAsync { map -> setupMap(map, lat, lng, roleColor, employeeName) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, null, tint = roleColor, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("%.5f, %.5f".format(lat, lng), color = StardustTextSecondary, fontSize = 11.sp)
        }
    }
}

// ============================================================================================
// КАРТА
// ============================================================================================

private fun setupMap(map: GoogleMap, lat: Double, lng: Double, roleColor: Color, name: String) {
    val position = LatLng(lat, lng)
    try {
        map.setMapStyle(MapStyleOptions("""
            [{"elementType":"geometry","stylers":[{"color":"#1d2c4d"}]},
             {"elementType":"labels.text.fill","stylers":[{"color":"#8ec3b9"}]},
             {"elementType":"labels.text.stroke","stylers":[{"color":"#1a3646"}]},
             {"featureType":"road","elementType":"geometry","stylers":[{"color":"#304a7d"}]},
             {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0e1626"}]}]
        """.trimIndent()))
    } catch (_: Exception) {}

    map.uiSettings.apply {
        isScrollGesturesEnabled = false; isZoomGesturesEnabled = false
        isRotateGesturesEnabled = false; isTiltGesturesEnabled = false
        isMapToolbarEnabled = false; isZoomControlsEnabled = false
    }
    map.addCircle(CircleOptions().center(position).radius(50.0)
        .fillColor(android.graphics.Color.argb(40, (roleColor.red*255).toInt(), (roleColor.green*255).toInt(), (roleColor.blue*255).toInt()))
        .strokeColor(android.graphics.Color.argb(120, (roleColor.red*255).toInt(), (roleColor.green*255).toInt(), (roleColor.blue*255).toInt()))
        .strokeWidth(2f))
    map.addMarker(MarkerOptions().position(position).title(name)
        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
}

// ============================================================================================
// ШАПКА
// ============================================================================================

@Composable
private fun ProfileHeader(name: String, role: String, roleColor: Color, isShiftActive: Boolean, photoUrl: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        roleColor.copy(alpha = 0.15f),
                        roleColor.copy(alpha = 0.05f)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(88.dp)) {
            val context = LocalContext.current
            if (photoUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(photoUrl).crossfade(true).build(),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                )
            } else {
                val initials = name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(roleColor.copy(alpha = 0.3f), roleColor.copy(alpha = 0.08f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = roleColor, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D0D1A))
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(if (isShiftActive) Color(0xFF4CAF50) else Color(0xFF555566))
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(name, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = roleColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    role, color = roleColor,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
            if (isShiftActive) {
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(6.dp).clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("На смене", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ============================================================================================
// МЕТРИКА (для expanded)
// ============================================================================================

@Composable
private fun MetricCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StardustTextSecondary.copy(alpha = 0.07f))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = StardustPrimary.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(label, color = StardustTextSecondary, fontSize = 11.sp)
        }
    }
}

// ============================================================================================
// ГРАФИК НЕДЕЛИ (для expanded)
// ============================================================================================

@Composable
private fun WeeklyChart(scans: List<Int>) {
    if (scans.size < 7) return
    val maxScans = scans.maxOrNull()?.takeIf { it > 0 } ?: 1
    val dayNames = remember {
        val dayMap = mapOf(
            Calendar.MONDAY to "Пн", Calendar.TUESDAY to "Вт", Calendar.WEDNESDAY to "Ср",
            Calendar.THURSDAY to "Чт", Calendar.FRIDAY to "Пт",
            Calendar.SATURDAY to "Сб", Calendar.SUNDAY to "Вс"
        )
        val cal = Calendar.getInstance()
        (0..6).map { i ->
            cal.timeInMillis = System.currentTimeMillis() - (6 - i) * 24 * 60 * 60 * 1000L
            dayMap[cal.get(Calendar.DAY_OF_WEEK)] ?: "?"
        }
    }
    val animProgress = remember(scans) { Animatable(0f) }
    LaunchedEffect(scans) { animProgress.snapTo(0f); animProgress.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val progress by animProgress.asState()
    val peakIndex = scans.indexOf(scans.max())

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Всего за неделю", color = StardustTextSecondary, fontSize = 12.sp)
            Text("${scans.sum()} сканов", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            scans.forEachIndexed { index, count ->
                val fraction = (count.toFloat() / maxScans) * progress
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    if (count > 0) { Text(count.toString(), color = StardustTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Medium); Spacer(Modifier.height(2.dp)) }
                    Box(modifier = Modifier.fillMaxWidth().height(70.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(StardustPrimary.copy(alpha = 0.1f)))
                        if (fraction > 0f) {
                            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction).clip(RoundedCornerShape(4.dp))
                                .background(if (index == peakIndex) StardustPrimary else Color(0xFF4CAF50).copy(alpha = 0.7f)))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(dayNames[index], color = StardustTextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// ============================================================================================
// DEVICE ROW
// ============================================================================================

@Composable
private fun DeviceRow(label: String, value: String, icon: ImageVector, valueColor: Color = StardustTextPrimary) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = StardustTextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = StardustTextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ============================================================================================
// УТИЛИТЫ
// ============================================================================================

private fun formatShiftDuration(minutes: Long): String {
    return if (minutes <= 0) "—"
    else { val h = minutes / 60; val m = minutes % 60; if (h > 0) "${h}ч ${m}м" else "${m}м" }
}