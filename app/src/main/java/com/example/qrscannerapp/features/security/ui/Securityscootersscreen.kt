package com.example.qrscannerapp.features.security.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================================
// ЦВЕТОВАЯ СХЕМА СБ
// ============================================================================================

object SecColors {
    val Accent        = Color(0xFFD85A30)
    val AccentDim     = Color(0xFFD85A30).copy(alpha = 0.18f)
    val Bg            = Color(0xFF111114)
    val Card          = Color(0xFF1A1A1E)
    val CardBorder    = Color(0xFF2A2A2E)
    val Divider       = Color(0xFF222226)
    val TextPrimary   = Color(0xFFF0F0F2)
    val TextSecondary = Color(0xFF7A7A82)
    val TextMuted     = Color(0xFF4A4A52)
    val Success       = Color(0xFF3DBE78)
    val Warning       = Color(0xFFE8A020)
    val Danger        = Color(0xFFE84040)
    val TagBg         = Color(0xFF242428)
}

// ============================================================================================
// ИКОНКИ И ЦВЕТА ТЕГОВ
// ============================================================================================

fun scooterTagIcon(tag: ScooterTag): ImageVector = when (tag) {
    ScooterTag.STOLEN     -> Icons.Default.GppBad
    ScooterTag.BURNED     -> Icons.Default.LocalFireDepartment
    ScooterTag.OPENED     -> Icons.Default.LockOpen
    ScooterTag.NO_BATTERY -> Icons.Default.BatteryAlert
    ScooterTag.DROWNED    -> Icons.Default.Water
    ScooterTag.FRAME      -> Icons.Default.Build
    ScooterTag.DAMAGED    -> Icons.Default.Warning
    ScooterTag.FOUND      -> Icons.Default.CheckCircle
}

fun scooterTagColor(tag: ScooterTag): Color = when (tag) {
    ScooterTag.STOLEN     -> Color(0xFFF44336)
    ScooterTag.BURNED     -> Color(0xFFFF5722)
    ScooterTag.OPENED     -> Color(0xFFFF9800)
    ScooterTag.NO_BATTERY -> Color(0xFF9E9E9E)
    ScooterTag.DROWNED    -> Color(0xFF2196F3)
    ScooterTag.FRAME      -> Color(0xFF607D8B)
    ScooterTag.DAMAGED    -> Color(0xFFE91E63)
    ScooterTag.FOUND      -> Color(0xFF4CAF50)
}

// ============================================================================================
// АНИМИРОВАННАЯ БАТАРЕЙКА
// ============================================================================================

@Composable
fun BatteryIndicator(
    charge: Int,
    modifier: Modifier = Modifier
) {
    val batteryColor = when {
        charge <= 25 -> SecColors.Danger
        charge <= 45 -> SecColors.Warning
        charge <= 70 -> Color(0xFFFFEB3B)
        else         -> SecColors.Success
    }
    val fillFraction = (charge.coerceIn(0, 100) / 100f)

    // Анимируем заполнение
    val animFill by animateFloatAsState(
        targetValue   = fillFraction,
        animationSpec = tween(800, easing = EaseOutCubic),
        label         = "battery_fill"
    )

    // Мигание если критический заряд
    val alpha by if (charge <= 10) {
        rememberInfiniteTransition(label = "blink").animateFloat(
            initialValue  = 0.4f,
            targetValue   = 1f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label         = "blink_alpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Canvas(modifier = modifier.alpha(alpha)) {
        val w = size.width
        val h = size.height
        val bodyW = w * 0.88f
        val capW  = w * 0.08f
        val capH  = h * 0.4f
        val strokePx = 1.5.dp.toPx()
        val radius = 3.dp.toPx()

        // Корпус батарейки — контур
        drawRoundRect(
            color       = batteryColor.copy(alpha = 0.4f),
            topLeft     = Offset(0f, 0f),
            size        = Size(bodyW, h),
            cornerRadius = CornerRadius(radius),
            style       = androidx.compose.ui.graphics.drawscope.Stroke(strokePx)
        )

        // Заполнение
        val fillW = (bodyW - strokePx * 2) * animFill
        if (fillW > 0f) {
            drawRoundRect(
                color        = batteryColor,
                topLeft      = Offset(strokePx, strokePx),
                size         = Size(fillW, h - strokePx * 2),
                cornerRadius = CornerRadius((radius - strokePx).coerceAtLeast(0f))
            )
        }

        // Контакт батарейки (плюсик)
        drawRoundRect(
            color        = batteryColor.copy(alpha = 0.6f),
            topLeft      = Offset(bodyW + 1.dp.toPx(), (h - capH) / 2f),
            size         = Size(capW, capH),
            cornerRadius = CornerRadius(1.dp.toPx())
        )
    }
}

// ============================================================================================
// БЛОК ДАННЫХ ФЛИТА В ПАСПОРТЕ
// ============================================================================================

@Composable
fun FleetDataSection(passport: ScooterPassport) {
    val charge = passport.fleetCharge
    val lat    = passport.fleetLat
    val lon    = passport.fleetLon
    val model  = passport.fleetModel

    // Показываем только если есть хоть какие-то данные флита
    if (charge == null && lat == null && model == null) return

    PassportSection(title = "Данные флита", icon = Icons.Default.DirectionsBike) {

        // Модель и ID
        if (!model.isNullOrBlank()) {
            PassportInfoRow("Модель", model, Icons.Default.TwoWheeler)
        }

        // Статус — Живой / Не живой
        val isAlive = (charge ?: 0) > 0
        Row(
            modifier          = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isAlive) Icons.Default.Wifi else Icons.Default.WifiOff,
                null,
                tint     = SecColors.TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Статус", color = SecColors.TextMuted, fontSize = 13.sp, modifier = Modifier.width(110.dp))
            Text(
                if (isAlive) "Живой — пингует" else "Не живой — нет пинга",
                color      = if (isAlive) SecColors.Success else SecColors.Danger,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Заряд с батарейкой
        if (charge != null) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier          = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.BatteryChargingFull, null, tint = SecColors.TextMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("Заряд", color = SecColors.TextMuted, fontSize = 13.sp, modifier = Modifier.width(110.dp))
                // Батарейка + процент
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BatteryIndicator(
                        charge   = charge,
                        modifier = Modifier.width(52.dp).height(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    val chargeColor = when {
                        charge <= 25 -> SecColors.Danger
                        charge <= 45 -> SecColors.Warning
                        charge <= 70 -> Color(0xFFFFEB3B)
                        else         -> SecColors.Success
                    }
                    Text(
                        "$charge%",
                        color      = chargeColor,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Координаты — кликабельные, открывают карты
        if (lat != null && lon != null && lat != 0.0 && lon != 0.0) {
            Spacer(Modifier.height(4.dp))
            val context = LocalContext.current
            Surface(
                shape    = RoundedCornerShape(10.dp),
                color    = SecColors.TagBg,
                border   = BorderStroke(1.dp, SecColors.CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Открываем chooser карт — работает с Яндекс, Google, 2GIS и любыми другими
                        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        // chooser покажет все установленные карты
                        context.startActivity(Intent.createChooser(intent, "Открыть в картах"))
                    }
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn, null,
                        tint     = if (isAlive) SecColors.Success else SecColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${"%.6f".format(lat)}, ${"%.6f".format(lon)}",
                            color      = SecColors.TextPrimary,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            if (isAlive) "Актуальные · нажмите для навигации" else "Последние известные · нажмите для навигации",
                            color    = if (isAlive) SecColors.Success.copy(alpha = 0.7f) else SecColors.TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        Icons.Default.OpenInNew, null,
                        tint     = SecColors.TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}



// ============================================================================================
// SECURITY SCOOTERS SCREEN
// ============================================================================================

enum class ScooterTab(val label: String) {
    LOST("Розыск"),
    FOUND("Найдены"),
    HISTORY("История")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScootersScreen(
    viewModel: SecurityViewModel,
    onMenuClick: () -> Unit,
    onOpenPassport: (String) -> Unit
) {
    val state       by viewModel.scootersState.collectAsState()
    val isOperating by viewModel.isOperating.collectAsState()
    var activeTab   by remember { mutableStateOf(ScooterTab.LOST) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery   by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearScootersMessage()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearScootersMessage()
        }
    }

    val filteredList = remember(activeTab, state.lostScooters, state.foundScooters, state.historyAll, searchQuery) {
        val base = when (activeTab) {
            ScooterTab.LOST    -> state.lostScooters
            ScooterTab.FOUND   -> state.foundScooters
            ScooterTab.HISTORY -> state.historyAll
        }
        if (searchQuery.isBlank()) base
        else base.filter { it.scooterId.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize().background(SecColors.Bg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            SecTopBar(
                title    = "Самокаты",
                subtitle = "${state.lostScooters.size} в розыске",
                onMenuClick = onMenuClick,
                trailingContent = {
                    if (state.lostScooters.isNotEmpty()) PulsingBadge(count = state.lostScooters.size)
                }
            )

            // Вкладки
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .background(SecColors.Bg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScooterTab.entries.forEach { tab ->
                    val isActive = tab == activeTab
                    val badge = when (tab) {
                        ScooterTab.LOST  -> state.lostScooters.size
                        ScooterTab.FOUND -> state.foundScooters.size
                        else             -> 0
                    }
                    Surface(
                        onClick  = { activeTab = tab },
                        shape    = RoundedCornerShape(10.dp),
                        color    = if (isActive) SecColors.Accent else SecColors.Card,
                        border   = if (!isActive) BorderStroke(1.dp, SecColors.CardBorder) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier              = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text       = tab.label,
                                fontSize   = 13.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color      = if (isActive) Color.White else SecColors.TextSecondary,
                                maxLines   = 1
                            )
                            if (badge > 0) {
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier         = Modifier
                                        .background(
                                            if (isActive) Color.White.copy(alpha = 0.3f) else SecColors.Accent,
                                            CircleShape
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(badge.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Поиск
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(SecColors.Card, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = SecColors.TextMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine    = true,
                    textStyle     = TextStyle(color = SecColors.TextPrimary, fontSize = 14.sp),
                    cursorBrush   = SolidColor(SecColors.Accent),
                    decorationBox = { inner ->
                        Box {
                            if (searchQuery.isEmpty()) Text("Номер самоката...", color = SecColors.TextMuted, fontSize = 14.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f).padding(vertical = 10.dp)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = SecColors.TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(color = SecColors.Divider)

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> SecLoadingState()
                    filteredList.isEmpty() -> SecEmptyState(
                        text    = when (activeTab) {
                            ScooterTab.LOST    -> "Нет самокатов в розыске"
                            ScooterTab.FOUND   -> "Найденных пока нет"
                            ScooterTab.HISTORY -> "История пуста"
                        },
                        subtext = if (activeTab == ScooterTab.LOST) "Нажмите + чтобы добавить" else null
                    )
                    else -> LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items = filteredList, key = { it.scooterId }) { passport ->
                            ScooterPassportCard(
                                passport     = passport,
                                onClick      = { onOpenPassport(passport.scooterId) },
                                onFoundClick = if (activeTab == ScooterTab.LOST) ({ onOpenPassport(passport.scooterId) }) else null
                            )
                        }
                    }
                }

                if (activeTab == ScooterTab.LOST) {
                    ExtendedFloatingActionButton(
                        text           = { Text("Добавить", fontWeight = FontWeight.Bold) },
                        icon           = { Icon(Icons.Default.Add, null) },
                        onClick        = { showAddDialog = true },
                        modifier       = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                        containerColor = SecColors.Accent,
                        contentColor   = Color.White
                    )
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showAddDialog) {
        AddLostScooterDialog(
            isLoading = isOperating,
            onDismiss = { showAddDialog = false },
            onConfirm = { scooterId, tags, notes ->
                viewModel.addLostScooter(scooterId, tags, notes)
                showAddDialog = false
            }
        )
    }
}

// ============================================================================================
// КАРТОЧКА ПАСПОРТА В СПИСКЕ
// ============================================================================================

@Composable
fun ScooterPassportCard(
    passport: ScooterPassport,
    onClick: () -> Unit,
    onFoundClick: (() -> Unit)? = null
) {
    val isLost  = passport.status == "lost"
    val charge  = passport.fleetCharge

    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(14.dp),
        color    = SecColors.Card,
        border   = BorderStroke(
            1.dp,
            if (isLost) SecColors.Accent.copy(alpha = 0.3f) else SecColors.Success.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Верхняя строка: иконка + номер + статус-чип
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (isLost) Icons.Default.GppBad else Icons.Default.VerifiedUser,
                        null,
                        tint     = if (isLost) SecColors.Accent else SecColors.Success,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        passport.scooterId.removePrefix("SCOOTERSNUMBER"),
                        fontWeight    = FontWeight.Bold,
                        color         = SecColors.TextPrimary,
                        fontSize      = 15.sp,
                        letterSpacing = 0.5.sp,
                        fontFamily    = FontFamily.Monospace
                    )
                    // Мини-батарейка если есть данные флита
                    if (charge != null) {
                        Spacer(Modifier.width(10.dp))
                        BatteryIndicator(charge = charge, modifier = Modifier.width(32.dp).height(14.dp))
                        Spacer(Modifier.width(4.dp))
                        val chargeColor = when {
                            charge <= 25 -> SecColors.Danger
                            charge <= 45 -> SecColors.Warning
                            charge <= 70 -> Color(0xFFFFEB3B)
                            else         -> SecColors.Success
                        }
                        Text("$charge%", color = chargeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(8.dp))
                SecStatusChip(isLost = isLost)
            }

            // Теги
            if (passport.tags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                TagsRow(tagKeys = passport.tags)
            }

            // Источник — флит или СБ
            if (passport.isFromFleet) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Source, null, tint = SecColors.TextMuted, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Источник: выгрузка флита", color = SecColors.TextMuted, fontSize = 11.sp)
                }
            }

            // Мета-строка
            Spacer(Modifier.height(8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RadioButtonChecked, null, tint = SecColors.Accent, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("В розыске", fontSize = 12.sp, color = SecColors.Accent.copy(alpha = 0.8f))
                }
                Text(formatPassportDate(passport.updatedAt), fontSize = 11.sp, color = SecColors.TextMuted)
            }

            // Кнопка найден
            if (onFoundClick != null && isLost) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick  = onFoundClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    border   = BorderStroke(1.dp, SecColors.Success.copy(alpha = 0.5f)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = SecColors.Success)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Самокат найден", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ============================================================================================
// ДИАЛОГ ДОБАВЛЕНИЯ
// ============================================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddLostScooterDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (scooterId: String, tags: List<ScooterTag>, notes: String?) -> Unit
) {
    var scooterId by remember { mutableStateOf("") }
    var notes     by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<ScooterTag>() }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor   = SecColors.Card,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GppBad, null, tint = SecColors.Accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Добавить в розыск", color = SecColors.TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value         = scooterId,
                    onValueChange = { scooterId = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                    label         = { Text("Номер самоката", color = SecColors.TextSecondary) },
                    placeholder   = { Text("HE600A...", color = SecColors.TextMuted) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = secTextFieldColors(),
                    modifier      = Modifier.fillMaxWidth()
                )

                Text("КЛАССИФИКАЦИЯ", color = SecColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScooterTag.entries.forEach { tag ->
                        val selected = tag in selectedTags
                        val color    = scooterTagColor(tag)
                        FilterChip(
                            selected = selected,
                            onClick  = { if (selected) selectedTags.remove(tag) else selectedTags.add(tag) },
                            label    = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(scooterTagIcon(tag), null, modifier = Modifier.size(14.dp), tint = if (selected) color else SecColors.TextMuted)
                                    Spacer(Modifier.width(5.dp))
                                    Text(tag.label, fontSize = 12.sp)
                                }
                            },
                            shape  = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.15f),
                                selectedLabelColor     = color,
                                containerColor         = SecColors.TagBg,
                                labelColor             = SecColors.TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = selected,
                                selectedBorderColor = color.copy(alpha = 0.4f),
                                borderColor = SecColors.CardBorder
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("Оперативные заметки", color = SecColors.TextSecondary) },
                    maxLines      = 3,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = secTextFieldColors(),
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { onConfirm(scooterId.trim(), selectedTags.toList(), notes.ifBlank { null }) },
                enabled  = scooterId.isNotBlank() && !isLoading,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SecColors.Accent)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.GppBad, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Объявить в розыск", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Отмена", color = SecColors.TextSecondary)
            }
        }
    )
}

// ============================================================================================
// SCOOTER PASSPORT SCREEN
// ============================================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScooterPassportScreen(
    scooterId: String,
    viewModel: SecurityViewModel,
    onBack: () -> Unit
) {
    val cleanId     = remember(scooterId) { scooterId.removePrefix("SCOOTERSNUMBER") }
    val passport    by viewModel.passportState.collectAsState()
    val isOperating by viewModel.isOperating.collectAsState()

    LaunchedEffect(cleanId) { viewModel.watchPassport(cleanId) }
    DisposableEffect(Unit) { onDispose { viewModel.stopWatchingPassport() } }

    var showMarkFoundDialog by remember { mutableStateOf(false) }
    var showEditTagsDialog   by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize().background(SecColors.Bg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Шапка
            Surface(color = SecColors.Bg) {
                Column {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SecColors.TextPrimary)
                        }
                        Spacer(Modifier.width(4.dp))

                        val statusIcon  = if (passport?.status == "lost") Icons.Default.GppBad else Icons.Default.VerifiedUser
                        val statusColor = if (passport?.status == "lost") SecColors.Accent else SecColors.Success
                        Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text          = cleanId,
                                color         = SecColors.TextPrimary,
                                fontWeight    = FontWeight.Bold,
                                fontSize      = 18.sp,
                                letterSpacing = 1.sp,
                                fontFamily    = FontFamily.Monospace
                            )
                            passport?.let {
                                Text(
                                    text          = if (it.status == "lost") "В РОЗЫСКЕ" else "НАЙДЕН",
                                    color         = statusColor,
                                    fontSize      = 11.sp,
                                    fontWeight    = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }

                        if (isOperating) {
                            CircularProgressIndicator(color = SecColors.Accent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                    HorizontalDivider(color = SecColors.Divider)
                }
            }

            when (val p = passport) {
                null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.FolderOff, null, tint = SecColors.TextMuted, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Паспорт не найден", color = SecColors.TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Text("Объект не зарегистрирован в базе СБ", color = SecColors.TextMuted, fontSize = 12.sp)
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = { viewModel.addLostScooter(cleanId) },
                                colors  = ButtonDefaults.buttonColors(containerColor = SecColors.Accent),
                                shape   = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.GppBad, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Объявить в розыск", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier            = Modifier.weight(1f)
                    ) {

                        // ── ДАННЫЕ ФЛИТА (батарейка, координаты, статус живой) ──────────
                        item {
                            FleetDataSection(passport = p)
                        }

                        // ── КЛАССИФИКАЦИЯ ─────────────────────────────────────────────────
                        item {
                            PassportSection(title = "Классификация", icon = Icons.Default.Policy) {
                                if (p.tags.isEmpty()) {
                                    Text("Классификация не присвоена", color = SecColors.TextMuted, fontSize = 13.sp)
                                } else {
                                    TagsRow(tagKeys = p.tags)
                                }
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick  = { showEditTagsDialog = true },
                                    shape    = RoundedCornerShape(10.dp),
                                    border   = BorderStroke(1.dp, SecColors.CardBorder),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = SecColors.TextSecondary)
                                ) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Изменить классификацию", fontSize = 13.sp)
                                }
                            }
                        }

                        // ── МЕСТО ОБНАРУЖЕНИЯ ─────────────────────────────────────────────
                        if (p.status == "found") {
                            item {
                                PassportSection(title = "Место обнаружения", icon = Icons.Default.PinDrop) {
                                    PassportInfoRow("Адрес", p.foundAt ?: "—", Icons.Outlined.LocationOn)
                                    p.deliveredToHub?.let {
                                        PassportInfoRow("Доставлен в", SecurityHubs.displayName(it), Icons.Outlined.Warehouse)
                                    }
                                    p.foundByName?.let {
                                        PassportInfoRow("Оперативник", it, Icons.Default.Badge)
                                    }
                                }
                            }
                        }

                        // ── ЗАМЕТКИ ───────────────────────────────────────────────────────
                        if (!p.notes.isNullOrBlank()) {
                            item {
                                PassportSection(title = "Оперативные заметки", icon = Icons.Default.Notes) {
                                    Text(p.notes, color = SecColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                                }
                            }
                        }

                        // ── ЖУРНАЛ ОПЕРАЦИЙ ───────────────────────────────────────────────
                        if (p.historyLog.isNotEmpty()) {
                            item {
                                PassportSection(title = "Журнал операций", icon = Icons.Default.History) {
                                    val sorted = p.historyLog.sortedByDescending { it.timestamp }
                                    sorted.forEachIndexed { idx, entry ->
                                        HistoryEntryRow(entry = entry)
                                        if (idx < sorted.lastIndex) {
                                            HorizontalDivider(color = SecColors.Divider, modifier = Modifier.padding(vertical = 8.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // ── ДЕЙСТВИЯ ──────────────────────────────────────────────────────
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (p.status == "lost") {
                                    Button(
                                        onClick  = { showMarkFoundDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape    = RoundedCornerShape(12.dp),
                                        colors   = ButtonDefaults.buttonColors(containerColor = SecColors.Success)
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Объект обнаружен", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    OutlinedButton(
                                        onClick  = { viewModel.logSearchAttempt(cleanId) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape    = RoundedCornerShape(12.dp),
                                        border   = BorderStroke(1.dp, SecColors.Warning.copy(alpha = 0.5f)),
                                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = SecColors.Warning)
                                    ) {
                                        Icon(Icons.Default.ManageSearch, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Зафиксировать выезд на поиск", fontSize = 13.sp)
                                    }
                                }
                                OutlinedButton(
                                    onClick  = { viewModel.deletePassport(cleanId); onBack() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape    = RoundedCornerShape(12.dp),
                                    border   = BorderStroke(1.dp, SecColors.Danger.copy(alpha = 0.4f)),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = SecColors.Danger)
                                ) {
                                    Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Удалить из базы", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showMarkFoundDialog) {
        MarkFoundDialog(
            scooterId = cleanId,
            isLoading = isOperating,
            onDismiss = { showMarkFoundDialog = false },
            onConfirm = { foundAt, hub, coords, tags, notes ->
                viewModel.markScooterFound(cleanId, foundAt, hub, coords, tags, notes)
                showMarkFoundDialog = false
            }
        )
    }

    if (showEditTagsDialog && passport != null) {
        EditTagsDialog(
            currentTags = passport!!.tags.mapNotNull { ScooterTag.fromKey(it) },
            onDismiss   = { showEditTagsDialog = false },
            onConfirm   = { tags -> viewModel.updateScooterTags(cleanId, tags); showEditTagsDialog = false }
        )
    }
}

// ============================================================================================
// ДИАЛОГ — ОБЪЕКТ ОБНАРУЖЕН
// ============================================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarkFoundDialog(
    scooterId: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (foundAt: String, hub: String, coords: ScooterCoords?, tags: List<ScooterTag>, notes: String?) -> Unit
) {
    var foundAt     by remember { mutableStateOf("") }
    var notes       by remember { mutableStateOf("") }
    var selectedHub by remember { mutableStateOf(SecurityHubs.BESTUZH) }
    val selectedTags = remember { mutableStateListOf<ScooterTag>() }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor   = SecColors.Card,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, null, tint = SecColors.Success, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Объект обнаружен: $scooterId", color = SecColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value         = foundAt,
                    onValueChange = { foundAt = it },
                    label         = { Text("Место обнаружения", color = SecColors.TextSecondary) },
                    leadingIcon   = { Icon(Icons.Default.PinDrop, null, tint = SecColors.TextMuted) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = secTextFieldColors(),
                    modifier      = Modifier.fillMaxWidth()
                )

                Text("ПЕРЕДАТЬ В ХАБ", color = SecColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(SecurityHubs.BESTUZH, SecurityHubs.SOFIY).forEach { hub ->
                        val isSelected = hub == selectedHub
                        Surface(
                            onClick  = { selectedHub = hub },
                            shape    = RoundedCornerShape(10.dp),
                            color    = if (isSelected) SecColors.Accent else SecColors.TagBg,
                            border   = if (!isSelected) BorderStroke(1.dp, SecColors.CardBorder) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Warehouse, null, tint = if (isSelected) Color.White else SecColors.TextMuted, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(SecurityHubs.displayName(hub).split(" ").first(), color = if (isSelected) Color.White else SecColors.TextSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Text("СОСТОЯНИЕ ОБЪЕКТА", color = SecColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScooterTag.entries.forEach { tag ->
                        val selected = tag in selectedTags
                        val color    = scooterTagColor(tag)
                        FilterChip(
                            selected = selected,
                            onClick  = { if (selected) selectedTags.remove(tag) else selectedTags.add(tag) },
                            label    = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(scooterTagIcon(tag), null, modifier = Modifier.size(13.dp), tint = if (selected) color else SecColors.TextMuted)
                                    Spacer(Modifier.width(4.dp))
                                    Text(tag.label, fontSize = 11.sp)
                                }
                            },
                            shape  = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color.copy(alpha = 0.15f), selectedLabelColor = color, containerColor = SecColors.TagBg, labelColor = SecColors.TextSecondary),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, selectedBorderColor = color.copy(alpha = 0.4f), borderColor = SecColors.CardBorder)
                        )
                    }
                }

                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("Рапорт / заметки", color = SecColors.TextSecondary) },
                    maxLines      = 3,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = secTextFieldColors(),
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { onConfirm(foundAt.trim(), selectedHub, null, selectedTags.toList(), notes.ifBlank { null }) },
                enabled  = foundAt.isNotBlank() && !isLoading,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SecColors.Success)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else {
                    Icon(Icons.Default.VerifiedUser, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Подтвердить обнаружение", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Отмена", color = SecColors.TextSecondary) }
        }
    )
}

// ============================================================================================
// ДИАЛОГ РЕДАКТИРОВАНИЯ ТЕГОВ
// ============================================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTagsDialog(
    currentTags: List<ScooterTag>,
    onDismiss: () -> Unit,
    onConfirm: (List<ScooterTag>) -> Unit
) {
    val selected = remember { mutableStateListOf(*currentTags.toTypedArray()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SecColors.Card,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Policy, null, tint = SecColors.Accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Классификация объекта", color = SecColors.TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScooterTag.entries.forEach { tag ->
                    val isSelected = tag in selected
                    val color      = scooterTagColor(tag)
                    FilterChip(
                        selected = isSelected,
                        onClick  = { if (isSelected) selected.remove(tag) else selected.add(tag) },
                        label    = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(scooterTagIcon(tag), null, modifier = Modifier.size(14.dp), tint = if (isSelected) color else SecColors.TextMuted)
                                Spacer(Modifier.width(5.dp))
                                Text(tag.label, fontSize = 12.sp)
                            }
                        },
                        shape  = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color.copy(alpha = 0.15f), selectedLabelColor = color, containerColor = SecColors.TagBg, labelColor = SecColors.TextSecondary),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, selectedBorderColor = color.copy(alpha = 0.4f), borderColor = SecColors.CardBorder)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected.toList()) }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SecColors.Accent)) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Сохранить", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = SecColors.TextSecondary) }
        }
    )
}

// ============================================================================================
// ОБЩИЕ КОМПОНЕНТЫ
// ============================================================================================

@Composable
fun SecTopBar(
    title: String,
    subtitle: String? = null,
    onMenuClick: () -> Unit,
    trailingContent: @Composable () -> Unit = {}
) {
    Surface(color = SecColors.Bg, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier          = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick, modifier = Modifier.size(40.dp).background(SecColors.Card, CircleShape)) {
                    Icon(Icons.Default.Menu, null, tint = SecColors.TextPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier         = Modifier.size(36.dp).background(Brush.radialGradient(listOf(SecColors.Accent, SecColors.Accent.copy(alpha = 0.4f))), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Shield, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = SecColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (subtitle != null) Text(subtitle, color = SecColors.Accent.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                trailingContent()
            }
            HorizontalDivider(color = SecColors.Divider)
        }
    }
}

@Composable
fun TagsRow(tagKeys: List<String>) {
    val tags = tagKeys.mapNotNull { ScooterTag.fromKey(it) }
    if (tags.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tags.forEach { tag ->
            val color = scooterTagColor(tag)
            Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f), border = BorderStroke(1.dp, color.copy(alpha = 0.3f))) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(scooterTagIcon(tag), null, tint = color, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tag.label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
                }
            }
        }
    }
}

@Composable
fun SecStatusChip(isLost: Boolean) {
    val bg    = if (isLost) SecColors.Accent.copy(alpha = 0.12f) else SecColors.Success.copy(alpha = 0.12f)
    val fg    = if (isLost) SecColors.Accent else SecColors.Success
    val text  = if (isLost) "В РОЗЫСКЕ" else "НАЙДЕН"
    val icon  = if (isLost) Icons.Default.GppBad else Icons.Default.VerifiedUser
    Surface(shape = RoundedCornerShape(6.dp), color = bg, border = BorderStroke(1.dp, fg.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg, letterSpacing = 0.8.sp)
        }
    }
}

@Composable
fun PulsingBadge(count: Int) {
    val alpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha"
    )
    Box(modifier = Modifier.size(36.dp).background(SecColors.Accent.copy(alpha = alpha), CircleShape), contentAlignment = Alignment.Center) {
        Text(count.toString(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    }
}

@Composable
fun SecLoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SecColors.Accent, strokeWidth = 2.dp)
            Spacer(Modifier.height(12.dp))
            Text("Загрузка данных...", color = SecColors.TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
fun SecEmptyState(text: String, subtext: String? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.FolderOff, null, tint = SecColors.TextMuted, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(12.dp))
            Text(text, color = SecColors.TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtext != null) { Spacer(Modifier.height(4.dp)); Text(subtext, color = SecColors.TextMuted, fontSize = 12.sp) }
        }
    }
}

@Composable
fun PassportSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = SecColors.Card, border = BorderStroke(1.dp, SecColors.CardBorder)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = SecColors.TextMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(title.uppercase(), color = SecColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun PassportInfoRow(label: String, value: String, icon: ImageVector? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(icon, null, tint = SecColors.TextMuted, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(8.dp)) }
        Text(label, color = SecColors.TextMuted, fontSize = 13.sp, modifier = Modifier.width(110.dp))
        Text(value, color = SecColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
fun HistoryEntryRow(entry: ScooterHistoryEntry) {
    val sdf = remember { SimpleDateFormat("dd.MM.yy HH:mm", Locale.forLanguageTag("ru")) }
    val (icon, color) = when (entry.action) {
        "ADDED_LOST", "PASSPORT_CREATED" -> Icons.Default.GppBad      to SecColors.Accent
        "MARKED_FOUND"                   -> Icons.Default.VerifiedUser  to SecColors.Success
        "SEARCH_ATTEMPT"                 -> Icons.Default.ManageSearch  to SecColors.Warning
        "TAGS_UPDATED"                   -> Icons.Default.Policy        to SecColors.TextSecondary
        else                             -> Icons.Outlined.Info         to SecColors.TextMuted
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(28.dp).background(color.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.byUserName.ifBlank { "СБ" }, color = SecColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(if (entry.timestamp > 0L) sdf.format(Date(entry.timestamp)) else "", color = SecColors.TextMuted, fontSize = 11.sp)
            }
            Text(
                when (entry.action) {
                    "ADDED_LOST"       -> "Объявлен в розыск"
                    "MARKED_FOUND"     -> "Объект обнаружен"
                    "SEARCH_ATTEMPT"   -> "Выезд на поиск"
                    "TAGS_UPDATED"     -> "Классификация обновлена"
                    "PASSPORT_CREATED" -> "Паспорт создан"
                    else               -> entry.action
                },
                color = SecColors.TextSecondary, fontSize = 12.sp
            )
            if (!entry.note.isNullOrBlank()) { Spacer(Modifier.height(2.dp)); Text(entry.note, color = SecColors.TextMuted, fontSize = 11.sp) }
        }
    }
}

// ============================================================================================
// УТИЛИТЫ
// ============================================================================================

@Composable
fun secTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = SecColors.Accent,
    unfocusedBorderColor    = SecColors.CardBorder,
    focusedContainerColor   = SecColors.TagBg,
    unfocusedContainerColor = SecColors.TagBg,
    cursorColor             = SecColors.Accent,
    focusedTextColor        = SecColors.TextPrimary,
    unfocusedTextColor      = SecColors.TextPrimary,
    focusedLabelColor       = SecColors.Accent,
    unfocusedLabelColor     = SecColors.TextSecondary
)

private fun formatPassportDate(ts: Long): String {
    if (ts == 0L) return "—"
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000         -> "только что"
        diff < 3_600_000      -> "${diff / 60_000} мин назад"
        diff < 86_400_000     -> "${diff / 3_600_000} ч назад"
        diff < 2 * 86_400_000 -> "вчера"
        else -> SimpleDateFormat("d MMM", Locale.forLanguageTag("ru")).format(Date(ts))
    }
}

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)