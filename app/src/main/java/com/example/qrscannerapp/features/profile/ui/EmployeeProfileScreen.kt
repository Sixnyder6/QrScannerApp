package com.example.qrscannerapp.features.profile.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.*
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.features.profile.domain.model.DevicePerformanceDetails
import com.example.qrscannerapp.features.profile.domain.model.InteractionStats
import com.example.qrscannerapp.features.profile.domain.model.OperationStat
import com.example.qrscannerapp.features.profile.domain.model.PerformanceClass
import com.example.qrscannerapp.features.profile.domain.model.UserActivityLog
import com.example.qrscannerapp.features.profile.domain.model.UserProfile
import com.example.qrscannerapp.features.profile.ui.viewmodel.EmployeeProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

private val StardustGreen = Color(0xFF37B34A)
private val StardustYellow = Color(0xFFF9A825)

// Маппинг типа операции → отображение
private data class OperationMeta(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
)

private fun getOperationMeta(type: String): OperationMeta = when (type) {
    "WASHING"      -> OperationMeta("Мойка",          Icons.Outlined.WaterDrop,           Color(0xFF4FC3F7))
    "SANDING"      -> OperationMeta("Шлифовка",       Icons.Outlined.Carpenter,           Color(0xFFFFB74D))
    "DECALING"     -> OperationMeta("Декалирование",  Icons.Outlined.Style,               Color(0xFFBA68C8))
    "BATTERY_SWAP" -> OperationMeta("Замена АКБ",     Icons.Outlined.BatteryChargingFull, Color(0xFF81C784))
    else           -> OperationMeta(type,             Icons.Outlined.Build,               StardustTextSecondary)
}

// =================================================================================
// ОСНОВНОЙ ЭКРАН
// =================================================================================

@Composable
fun EmployeeProfileScreen(
    viewModel: EmployeeProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = StardustPrimary
                    )
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error!!,
                        color = StardustError,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            ProfileHeaderCard(profile = uiState.userProfile)
                        }

                        item {
                            AdminControlCard(
                                profile = uiState.userProfile,
                                onForceEndShift = { viewModel.forceEndShift() },
                                onSetWorkAccess = { isAllowed -> viewModel.setWorkAccess(isAllowed) }
                            )
                        }

                        // [НОВОЕ] Статистика операций
                        item {
                            InteractionStatsCard(
                                stats = uiState.interactionStats,
                                isLoading = uiState.isStatsLoading
                            )
                        }

                        item {
                            DevicePerformanceCard(details = uiState.performanceDetails)
                        }

                        item {
                            Text(
                                "Последняя активность и телеметрия",
                                style = MaterialTheme.typography.titleMedium,
                                color = StardustTextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                            )
                        }

                        if (uiState.activityHistory.isEmpty()) {
                            item {
                                Text(
                                    text = "У этого сотрудника еще нет записей об активности.",
                                    color = StardustTextSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            items(uiState.activityHistory, key = { it.id }) { log ->
                                TelemetrySnapshotCard(log = log)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =================================================================================
// [НОВОЕ] КАРТОЧКА СТАТИСТИКИ ОПЕРАЦИЙ
// =================================================================================

@Composable
fun InteractionStatsCard(
    stats: InteractionStats,
    isLoading: Boolean
) {
    val sdf = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📊 Активность операций",
                    color = StardustTextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                if (!isLoading && stats.totalAllTime > 0) {
                    Surface(
                        color = StardustPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Всего: ${stats.totalAllTime}",
                            color = StardustPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = StardustItemBg)

            when {
                isLoading -> {
                    // Скелетон загрузки
                    repeat(3) {
                        OperationStatRowSkeleton()
                    }
                }
                stats.stats.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.WorkOff,
                                contentDescription = null,
                                tint = StardustTextSecondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Нет операций",
                                color = StardustTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                else -> {
                    // Строки по каждому типу операции
                    val maxCount = stats.stats.maxOfOrNull { it.totalCount } ?: 1
                    stats.stats.forEach { stat ->
                        OperationStatRow(
                            stat = stat,
                            maxCount = maxCount,
                            lastDate = if (stat.lastTimestamp > 0)
                                sdf.format(Date(stat.lastTimestamp)) else "-"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationStatRow(
    stat: OperationStat,
    maxCount: Int,
    lastDate: String
) {
    val meta = getOperationMeta(stat.operationType)

    // Анимация прогресс-бара
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    val animatedProgress by animateFloatAsState(
        targetValue = if (isVisible) stat.totalCount.toFloat() / maxCount else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "bar_${stat.operationType}"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка операции
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(meta.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    meta.icon,
                    contentDescription = null,
                    tint = meta.color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))

            // Название
            Text(
                text = meta.displayName,
                color = StardustTextPrimary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )

            // Правая часть: кол-во + дата
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${stat.totalCount} шт.",
                    color = meta.color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "посл. $lastDate",
                    color = StardustTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Прогресс-бар относительно максимума
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(meta.color.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(CircleShape)
                    .background(meta.color.copy(alpha = 0.7f))
            )
        }

        // Подпись сессий
        Text(
            text = "${stat.sessionCount} ${pluralSessions(stat.sessionCount)}",
            color = StardustTextSecondary,
            fontSize = 11.sp
        )
    }
}

// Скелетон пока грузится
@Composable
private fun OperationStatRowSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(StardustItemBg.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(StardustItemBg.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(StardustItemBg.copy(alpha = alpha))
        )
    }
}

private fun pluralSessions(count: Int): String = when {
    count % 100 in 11..19 -> "сессий"
    count % 10 == 1       -> "сессия"
    count % 10 in 2..4    -> "сессии"
    else                  -> "сессий"
}

// =================================================================================
// ПУЛЬТ УПРАВЛЕНИЯ АДМИНА
// =================================================================================

@Composable
fun AdminControlCard(
    profile: UserProfile,
    onForceEndShift: () -> Unit,
    onSetWorkAccess: (Boolean) -> Unit
) {
    var showConfirmEndShiftDialog by remember { mutableStateOf(false) }

    if (showConfirmEndShiftDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmEndShiftDialog = false },
            title = { Text("Завершить смену?", color = StardustTextPrimary) },
            text = {
                Text(
                    "Смена сотрудника ${profile.name} будет принудительно закрыта.",
                    color = StardustTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onForceEndShift()
                        showConfirmEndShiftDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StardustError)
                ) { Text("Завершить") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmEndShiftDialog = false }) {
                    Text("Отмена", color = StardustTextSecondary)
                }
            },
            containerColor = StardustModalBg,
            titleContentColor = StardustTextPrimary
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🛡️ Управление доступом",
                color = StardustTextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider(color = StardustItemBg)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (profile.isShiftActive) Color(0xFF4CAF50) else Color.Gray
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (profile.isShiftActive) "Смена активна" else "Не на смене",
                        color = StardustTextPrimary,
                        fontSize = 15.sp
                    )
                }
                if (profile.isShiftActive) {
                    Button(
                        onClick = { showConfirmEndShiftDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StardustError),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Завершить смену", fontSize = 13.sp)
                    }
                }
            }

            HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Разрешить работу", color = StardustTextPrimary, fontSize = 15.sp)
                    Text(
                        text = when (profile.shiftRequestStatus) {
                            "PENDING"  -> "⏳ Ожидает одобрения"
                            "APPROVED" -> "✅ Одобрено"
                            "DENIED"   -> "❌ Отклонено"
                            else       -> "Запроса нет"
                        },
                        color = StardustTextSecondary,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = profile.isAllowedToWork,
                    onCheckedChange = { onSetWorkAccess(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = StardustSuccess,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = StardustItemBg
                    )
                )
            }
        }
    }
}

// =================================================================================
// ОСТАЛЬНЫЕ КОМПОНЕНТЫ
// =================================================================================

@Composable
fun DevicePerformanceCard(details: DevicePerformanceDetails) {
    val (icon, text, color) = when (details.performanceClass) {
        PerformanceClass.LOW     -> Triple(Icons.Default.DirectionsCar,  "Низкая",    StardustError)
        PerformanceClass.MEDIUM  -> Triple(Icons.Default.LocalShipping,  "Средняя",   StardustYellow)
        PerformanceClass.HIGH    -> Triple(Icons.Default.RocketLaunch,   "Высокая",   StardustGreen)
        PerformanceClass.UNKNOWN -> Triple(Icons.Default.QuestionMark,   "Нет данных",StardustTextSecondary)
    }
    val totalRamFormatted = if (details.totalRamGb > 0)
        "%.1f GB RAM".format(Locale.US, details.totalRamGb) else "N/A"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, "Performance Class", tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Производительность устройства", color = StardustTextSecondary, fontSize = 12.sp)
                Text(text, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text(totalRamFormatted, color = StardustTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun TelemetrySnapshotCard(log: UserActivityLog) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy 'в' HH:mm", Locale.getDefault()) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (log.activityType) {
                        "SESSION_SAVED" -> Icons.Default.Save
                        "COPY_ALL"      -> Icons.Default.ContentCopy
                        else            -> Icons.Default.History
                    },
                    contentDescription = "Action Type",
                    tint = StardustPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = when (log.activityType) {
                            "SESSION_SAVED" -> "Сохранение сессии"
                            "COPY_ALL"      -> "Копирование списка"
                            else            -> "Действие"
                        },
                        color = StardustTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(sdf.format(Date(log.timestamp)), color = StardustTextSecondary, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Text("Активность", fontWeight = FontWeight.SemiBold, color = StardustTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            TelemetryRow(Icons.Default.QrCodeScanner, "Количество", "${log.itemCount} шт.")
            if (log.manualEntryCount > 0) {
                TelemetryRow(Icons.Default.Edit, "Вручную", "${log.manualEntryCount} шт.", StardustError)
            }
            TelemetryRow(Icons.Default.Timer, "Длительность", "~${log.durationSeconds} сек.")

            Spacer(modifier = Modifier.height(16.dp))
            Text("Устройство", fontWeight = FontWeight.SemiBold, color = StardustTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            TelemetryRow(Icons.Default.BatteryStd, "Батарея", "${log.lastBatteryLevel}% ${if (log.isCharging) "(зарядка)" else ""}")
            TelemetryRow(Icons.Default.Thermostat, "Здоровье батареи", log.batteryHealth)
            TelemetryRow(Icons.Default.EnergySavingsLeaf, "Энергосбережение",
                if (log.isPowerSaveMode) "Включено" else "Выключено",
                if (log.isPowerSaveMode) StardustYellow else StardustTextPrimary)
            TelemetryRow(Icons.Default.SignalCellularAlt, "Сеть", log.networkState)
            TelemetryRow(Icons.Default.Speed, "Пинг", log.networkPing)
            TelemetryRow(Icons.Default.Memory, "RAM (свободно)", log.freeRam)
            TelemetryRow(Icons.Default.Storage, "Память (свободно)", log.freeStorage)
            TelemetryRow(Icons.Default.Update, "Uptime", log.deviceUptime)
        }
    }
}

@Composable
fun TelemetryRow(icon: ImageVector, label: String, value: String, valueColor: Color = StardustTextPrimary) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, label, tint = StardustTextSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = StardustTextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// =================================================================================
// ШАПКА ПРОФИЛЯ (С ИЗМЕНЕННЫМ УСТРОЙСТВОМ)
// =================================================================================

@Composable
fun ProfileHeaderCard(profile: UserProfile) {

    // Очищаем activeDeviceId: "Apple_iPhone 16 Pro Max_1776935440205" -> "Apple iPhone 16 Pro Max"
    val displayDevice = remember(profile.activeDeviceId, profile.deviceInfo) {
        if (profile.activeDeviceId.isNotBlank() && profile.activeDeviceId.contains("_")) {
            // Разбиваем по "_", убираем последний элемент (цифры) и соединяем пробелом
            profile.activeDeviceId.split("_").dropLast(1).joinToString(" ")
        } else if (profile.activeDeviceId.isNotBlank()) {
            profile.activeDeviceId
        } else {
            // Если activeDeviceId пустой, показываем обычный deviceInfo (эмулятор или старый андроид)
            profile.deviceInfo
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccountCircle, "Profile Picture",
                    modifier = Modifier.size(64.dp), tint = StardustTextPrimary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(profile.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = StardustTextPrimary)
                    Text(profile.role, fontSize = 16.sp, color = StardustTextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = StardustItemBg)
            Spacer(modifier = Modifier.height(12.dp))
            ProfileInfoRow(Icons.Default.AlternateEmail, "Логин", profile.username)
            ProfileInfoRow(Icons.Default.Info, "Последняя версия", profile.appVersion)
            // ИСПОЛЬЗУЕМ ОЧИЩЕННОЕ ИМЯ УСТРОЙСТВА ИЗ activeDeviceId
            ProfileInfoRow(Icons.Default.PhoneAndroid, "Устройство", displayDevice)
        }
    }
}

@Composable
fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = StardustTextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text("$label:", color = StardustTextSecondary, fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, color = StardustTextPrimary, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}