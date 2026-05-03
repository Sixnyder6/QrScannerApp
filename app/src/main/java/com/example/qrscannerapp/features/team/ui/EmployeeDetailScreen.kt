package com.example.qrscannerapp.features.team.ui

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
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
    val activeDeviceId: String = "", // [НОВОЕ] Добавлено поле
    val appVersion: String = "",
    val lastSeen: Long = 0L,
    // GPS
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationTimestamp: Long = 0L,
    val isLoaded: Boolean = false
)

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
    onEdit: () -> Unit = {}
) {
    val profileViewModel: EmployeeProfileViewModel = hiltViewModel()
    val profileState by profileViewModel.uiState.collectAsState()

    var activitySummary by remember { mutableStateOf(ActivitySummary()) }
    var telemetry by remember { mutableStateOf(DeviceTelemetry()) }

    // ── Realtime listeners: internal_users (lastSeen) + device_telemetry (телеметрия) ──
    DisposableEffect(userId) {
        val db = Firebase.firestore
        var currentLastSeen = 0L
        var hasTelemetryDoc = false

        // Listener 1: device_telemetry — основной источник телеметрийных полей
        val telemetryReg: ListenerRegistration = db
            .collection("device_telemetry")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EmployeeDetail", "device_telemetry snapshot error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    hasTelemetryDoc = true
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
                        locationLat = snapshot.getDouble("locationLat"),
                        locationLng = snapshot.getDouble("locationLng"),
                        locationTimestamp = snapshot.getLong("locationTimestamp") ?: 0L,
                        isLoaded = true
                    )
                }
                // Если документа нет — internal_users listener заполнит через fallback
            }

        // Listener 2: internal_users — только lastSeen
        val internalReg: ListenerRegistration = db
            .collection("internal_users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EmployeeDetail", "internal_users snapshot error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    currentLastSeen = snapshot.getLong("lastSeen") ?: 0L
                    telemetry = telemetry.copy(lastSeen = currentLastSeen)
                }
            }

        onDispose {
            telemetryReg.remove()
            internalReg.remove()
            Log.d("EmployeeDetail", "Both listeners removed for $userId")
        }
    }

    // ── Разовая загрузка статистики сканов ───────────────────────────────
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
                    title = { Text("Профиль") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.3f),
                        titleContentColor = StardustTextPrimary,
                        navigationIconContentColor = StardustTextPrimary
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ШАПКА
                    item {
                        val profile = profileState.userProfile
                        ProfileHeader(
                            name = profile.name.takeIf { it != "Загрузка..." } ?: userName,
                            role = profile.role.takeIf { it.isNotBlank() } ?: userRole,
                            roleColor = roleColor,
                            isShiftActive = profile.isShiftActive
                        )
                    }

                    // МЕТРИКИ ЗА СЕГОДНЯ
                    item {
                        SectionLabel("Сегодня")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricCard("Сканов", activitySummary.scansToday.toString(), Icons.Default.QrCodeScanner, Modifier.weight(1f))
                            MetricCard("Партий", activitySummary.batchesToday.toString(), Icons.Default.Inventory, Modifier.weight(1f))
                            MetricCard("Скан/час", activitySummary.scanRatePerHour.toString(), Icons.Default.Speed, Modifier.weight(1f))
                            MetricCard("На смене", formatShiftDuration(activitySummary.shiftDurationMinutes), Icons.Default.Timer, Modifier.weight(1f))
                        }
                    }

                    // ГРАФИК ЗА НЕДЕЛЮ
                    if (activitySummary.weeklyScans.size == 7) {
                        item {
                            SectionLabel("Активность за неделю")
                            WeeklyChart(scans = activitySummary.weeklyScans)
                        }
                    }

                    // СТАТИСТИКА ОПЕРАЦИЙ
                    item {
                        InteractionStatsCard(
                            stats = profileState.interactionStats,
                            isLoading = profileState.isStatsLoading
                        )
                    }

                    // ПРОИЗВОДИТЕЛЬНОСТЬ
                    item {
                        DevicePerformanceCard(details = profileState.performanceDetails)
                    }

                    // УСТРОЙСТВО + GPS — живые данные realtime
                    if (telemetry.isLoaded) {
                        item {
                            SectionLabel("Устройство")
                            LiveDeviceCard(telemetry = telemetry)
                        }

                        // Карта показывается если есть свежие координаты (не старше 15 минут)
                        val hasLocation = telemetry.locationLat != null && telemetry.locationLng != null
                        val locationFresh = hasLocation &&
                                (System.currentTimeMillis() - telemetry.locationTimestamp) < 15 * 60 * 1000L

                        if (locationFresh) {
                            item {
                                SectionLabel("Местоположение")
                                LocationCard(
                                    lat = telemetry.locationLat!!,
                                    lng = telemetry.locationLng!!,
                                    locationTimestamp = telemetry.locationTimestamp,
                                    roleColor = roleColor,
                                    employeeName = profileState.userProfile.name.takeIf { it != "Загрузка..." } ?: userName
                                )
                            }
                        }
                    }

                    // ПУЛЬТ АДМИНА
                    item {
                        AdminControlCard(
                            profile = profileState.userProfile,
                            onForceEndShift = { profileViewModel.forceEndShift() },
                            onSetWorkAccess = { isAllowed -> profileViewModel.setWorkAccess(isAllowed) }
                        )
                    }

                    // ДЕЙСТВИЯ
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onWriteDm,
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Написать", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onEdit,
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, tint = StardustTextSecondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Редактировать", color = StardustTextSecondary)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// ============================================================================================
// КАРТА С ПОЗИЦИЕЙ СОТРУДНИКА
// ============================================================================================

@Composable
private fun LocationCard(
    lat: Double,
    lng: Double,
    locationTimestamp: Long,
    roleColor: Color,
    employeeName: String
) {
    val context = LocalContext.current
    val minutesAgo = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - locationTimestamp)
    val timeText = when {
        minutesAgo < 1 -> "только что"
        minutesAgo < 60 -> "$minutesAgo мин. назад"
        else -> "${minutesAgo / 60} ч. назад"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = roleColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Последняя позиция",
                        color = StardustTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
                Text(timeText, color = StardustTextSecondary, fontSize = 12.sp)
            }

            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        onCreate(null)
                        onResume()
                        getMapAsync { map ->
                            setupMap(map, lat, lng, roleColor, employeeName)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "%.5f, %.5f".format(lat, lng),
                    color = StardustTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun setupMap(map: GoogleMap, lat: Double, lng: Double, roleColor: Color, name: String) {
    val position = LatLng(lat, lng)
    try {
        map.setMapStyle(
            MapStyleOptions("""
                [{"elementType":"geometry","stylers":[{"color":"#1d2c4d"}]},
                 {"elementType":"labels.text.fill","stylers":[{"color":"#8ec3b9"}]},
                 {"elementType":"labels.text.stroke","stylers":[{"color":"#1a3646"}]},
                 {"featureType":"road","elementType":"geometry","stylers":[{"color":"#304a7d"}]},
                 {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0e1626"}]}]
            """.trimIndent())
        )
    } catch (_: Exception) {}

    map.uiSettings.apply {
        isScrollGesturesEnabled = false
        isZoomGesturesEnabled = false
        isRotateGesturesEnabled = false
        isTiltGesturesEnabled = false
        isMapToolbarEnabled = false
        isZoomControlsEnabled = false
    }

    map.addCircle(
        CircleOptions()
            .center(position)
            .radius(50.0)
            .fillColor(android.graphics.Color.argb(40,
                (roleColor.red * 255).toInt(),
                (roleColor.green * 255).toInt(),
                (roleColor.blue * 255).toInt()
            ))
            .strokeColor(android.graphics.Color.argb(120,
                (roleColor.red * 255).toInt(),
                (roleColor.green * 255).toInt(),
                (roleColor.blue * 255).toInt()
            ))
            .strokeWidth(2f)
    )

    map.addMarker(
        MarkerOptions()
            .position(position)
            .title(name)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
    )

    map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
}

// ============================================================================================
// КАРТОЧКА УСТРОЙСТВА (realtime)
// ============================================================================================

@Composable
private fun LiveDeviceCard(telemetry: DeviceTelemetry) {
    val now = System.currentTimeMillis()
    val isOnline = telemetry.lastSeen > 0L && (now - telemetry.lastSeen) < ONLINE_THRESHOLD_MS

    // [НОВОЕ] Логика очистки имени устройства
    val displayDevice = remember(telemetry.activeDeviceId, telemetry.deviceInfo) {
        if (telemetry.activeDeviceId.isNotBlank() && telemetry.activeDeviceId.contains("_")) {
            telemetry.activeDeviceId.split("_").dropLast(1).joinToString(" ")
        } else if (telemetry.activeDeviceId.isNotBlank()) {
            telemetry.activeDeviceId
        } else {
            telemetry.deviceInfo
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Онлайн-статус
            if (telemetry.lastSeen > 0L) {
                val minutesAgo = TimeUnit.MILLISECONDS.toMinutes(now - telemetry.lastSeen)
                val pingText = when {
                    minutesAgo < 1 -> "только что"
                    minutesAgo < 60 -> "$minutesAgo мин. назад"
                    minutesAgo < 1440 -> "${minutesAgo / 60} ч. назад"
                    else -> "${minutesAgo / 1440} дн. назад"
                }
                val pingColor = when {
                    isOnline -> StardustSuccess
                    minutesAgo < 30 -> StardustWarning
                    else -> StardustError
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 1f, targetValue = if (isOnline) 0.3f else 1f,
                        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                        label = "pulse_alpha"
                    )
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(pingColor.copy(alpha = pulseAlpha)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isOnline) "Онлайн" else "Оффлайн",
                        color = pingColor, fontWeight = FontWeight.Bold,
                        fontSize = 14.sp, modifier = Modifier.weight(1f)
                    )
                    Text(pingText, color = StardustTextSecondary, fontSize = 12.sp)
                }
                HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
            }

            if (telemetry.appVersion.isNotBlank()) {
                DeviceRow("Версия", "v${telemetry.appVersion}", Icons.Default.PhoneAndroid, StardustSuccess)
                HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
            }

            // [ИЗМЕНЕНО] Используем чистое название iPhone
            if (displayDevice.isNotBlank() && displayDevice != "Created by Admin") {
                DeviceRow("Устройство", displayDevice, Icons.Default.Smartphone)
                HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
            }

            val batteryColor = when {
                telemetry.isCharging -> StardustSuccess
                telemetry.batteryLevel > 50 -> StardustSuccess
                telemetry.batteryLevel > 20 -> StardustWarning
                else -> StardustError
            }
            DeviceRow(
                "Батарея",
                "${telemetry.batteryLevel}%${if (telemetry.isCharging) " (зарядка)" else ""}",
                Icons.Default.BatteryStd, batteryColor
            )

            if (telemetry.batteryHealth.isNotBlank() && telemetry.batteryHealth != "N/A") {
                HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
                DeviceRow("Здоровье батареи", telemetry.batteryHealth, Icons.Default.Thermostat)
            }

            HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
            DeviceRow(
                "Энергосбережение",
                if (telemetry.isPowerSaveMode) "Включено" else "Выключено",
                Icons.Default.EnergySavingsLeaf,
                if (telemetry.isPowerSaveMode) StardustWarning else StardustTextPrimary
            )

            HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
            val netIcon = when {
                telemetry.networkState.contains("WiFi") -> Icons.Default.Wifi
                telemetry.networkState.contains("Cellular") -> Icons.Default.SignalCellularAlt
                else -> Icons.Default.WifiOff
            }
            DeviceRow(
                "Сеть", telemetry.networkState, netIcon,
                if (telemetry.networkState == "Offline") StardustError else StardustTextPrimary
            )

            if (telemetry.networkPing.isNotBlank() && telemetry.networkPing != "N/A") {
                HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
                DeviceRow("Пинг", telemetry.networkPing, Icons.Default.Speed)
            }

            if (telemetry.freeRam.isNotBlank() && telemetry.freeRam != "N/A") {
                HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
                DeviceRow("RAM (свободно)", telemetry.freeRam, Icons.Default.Memory)
            }

            if (telemetry.freeStorage.isNotBlank() && telemetry.freeStorage != "N/A") {
                HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
                DeviceRow("Память (свободно)", telemetry.freeStorage, Icons.Default.Storage)
            }

            if (telemetry.deviceUptime.isNotBlank() && telemetry.deviceUptime != "N/A") {
                HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
                DeviceRow("Uptime", telemetry.deviceUptime, Icons.Default.Update)
            }
        }
    }
}

@Composable
private fun DeviceRow(label: String, value: String, icon: ImageVector, valueColor: Color = StardustTextPrimary) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = StardustTextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = StardustTextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ============================================================================================
// ШАПКА
// ============================================================================================

@Composable
private fun ProfileHeader(name: String, role: String, roleColor: Color, isShiftActive: Boolean) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(80.dp)) {
            val initials = name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(roleColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = roleColor, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            }
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).size(20.dp)
                    .clip(CircleShape).background(StardustSolidBg)
                    .padding(3.dp).clip(CircleShape)
                    .background(if (isShiftActive) Color(0xFF4CAF50) else Color.Gray)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(name, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Surface(color = roleColor.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
            Text(
                UserRole.fromKey(role).displayName, color = roleColor,
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        if (isShiftActive) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Сейчас на смене", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ============================================================================================
// МЕТРИКА
// ============================================================================================

@Composable
private fun MetricCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = StardustGlassBg)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = StardustPrimary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, color = StardustTextSecondary, fontSize = 10.sp)
        }
    }
}

// ============================================================================================
// ГРАФИК НЕДЕЛИ
// ============================================================================================

@Composable
private fun WeeklyChart(scans: List<Int>) {
    if (scans.size < 7) return
    val maxScans = scans.maxOrNull()?.takeIf { it > 0 } ?: 1
    val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val animProgress = remember(scans) { Animatable(0f) }
    LaunchedEffect(scans) { animProgress.snapTo(0f); animProgress.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val progress by animProgress.asState()
    val peakIndex = scans.indexOf(scans.max())

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StardustGlassBg)) {
        Column(modifier = Modifier.padding(16.dp)) {
            val weekTotal = scans.sum()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Всего за неделю", color = StardustTextSecondary, fontSize = 12.sp)
                Text("$weekTotal сканов", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                scans.forEachIndexed { index, count ->
                    val fraction = (count.toFloat() / maxScans) * progress
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        if (count > 0) { Text(count.toString(), color = StardustTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Medium); Spacer(modifier = Modifier.height(2.dp)) }
                        Box(modifier = Modifier.fillMaxWidth().height(70.dp), contentAlignment = Alignment.BottomCenter) {
                            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(StardustPrimary.copy(alpha = 0.1f)))
                            if (fraction > 0f) {
                                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction).clip(RoundedCornerShape(4.dp))
                                    .background(if (index == peakIndex) StardustPrimary else StardustPrimary.copy(alpha = 0.6f)))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(dayNames[index], color = StardustTextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ============================================================================================
// УТИЛИТЫ
// ============================================================================================

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = StardustTextSecondary.copy(alpha = 0.6f), fontSize = 11.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 4.dp))
}

private fun formatShiftDuration(minutes: Long): String {
    return if (minutes <= 0) "—"
    else { val h = minutes / 60; val m = minutes % 60; if (h > 0) "${h}ч ${m}м" else "${m}м" }
}
