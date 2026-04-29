package com.example.qrscannerapp.features.security.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.features.security.ui.fleet.FleetImportSheet
import com.example.qrscannerapp.features.security.ui.fleet.FleetImportViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SecurityDashboardScreen(
    viewModel: SecurityViewModel,
    onNavigateToScooters: () -> Unit,
    onNavigateToBattery: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onMenuClick: () -> Unit,
    onNavigateToScanner: () -> Unit
) {
    val state by viewModel.dashboardState.collectAsState()
    var showFleetImport by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(SecColors.Bg)) {
        if (state.isLoading) {
            SecLoadingState()
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item { DashboardHeader(state = state, onMenuClick = onMenuClick) }

                // ── Кнопка полевого сканера ────────────────────────────────
                item {
                    ScannerButton(onNavigateToScanner = onNavigateToScanner)
                }

                // ── Кнопка импорта флита ───────────────────────────────────
                item {
                    FleetImportButton(onClick = { showFleetImport = true })
                }

                item {
                    QuickStatsRow(
                        state                = state,
                        onNavigateToScooters = onNavigateToScooters,
                        onNavigateToBattery  = onNavigateToBattery,
                        onNavigateToStorage  = onNavigateToStorage
                    )
                }

                item { TodaySection(state = state) }

                if (state.tagBreakdown.isNotEmpty()) {
                    item {
                        DashSection(title = "По типам", emoji = "🏷") {
                            TagBreakdownChart(breakdown = state.tagBreakdown)
                        }
                    }
                }

                if (state.hubBreakdown.isNotEmpty()) {
                    item {
                        DashSection(title = "По хабам", emoji = "🏢") {
                            HubBreakdownRow(breakdown = state.hubBreakdown)
                        }
                    }
                }

                if (state.topFinders.isNotEmpty()) {
                    item {
                        DashSection(title = "Топ находильщиков", emoji = "🏆") {
                            TopFindersList(finders = state.topFinders)
                        }
                    }
                }

                if (state.recentActivity.isNotEmpty()) {
                    item {
                        DashSection(title = "Последняя активность", emoji = "⏱") {
                            RecentActivityList(items = state.recentActivity, onTap = onNavigateToScooters)
                        }
                    }
                }
            }
        }
    }

    // ── BottomSheet импорта флита ──────────────────────────────────────────
    if (showFleetImport) {
        FleetImportSheet(onDismiss = { showFleetImport = false })
    }
}

// ============================================================================================
// КНОПКА ПОЛЕВОГО СКАНЕРА
// ============================================================================================

@Composable
private fun ScannerButton(onNavigateToScanner: () -> Unit) {
    Surface(
        onClick  = onNavigateToScanner,
        shape    = RoundedCornerShape(16.dp),
        color    = SecColors.Accent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Сканировать объект",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
                Text(
                    "Поиск или создание паспорта",
                    color    = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Default.ChevronRight, null,
                tint     = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================================================================
// КНОПКА ИМПОРТА ФЛИТА
// ============================================================================================

@Composable
private fun FleetImportButton(onClick: () -> Unit) {
    val fleetVm: FleetImportViewModel = hiltViewModel()
    val fleetState by fleetVm.uiState.collectAsState()

    val hasLastImport = fleetState.lastImportedAt != null
    val sdf           = remember { SimpleDateFormat("dd.MM HH:mm", Locale("ru")) }
    val lastDateStr   = fleetState.lastImportedAt?.let { sdf.format(Date(it)) }

    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(16.dp),
        color    = SecColors.Card,
        border   = BorderStroke(
            1.dp,
            if (fleetState.canImport) Color(0xFF4FC3F7).copy(alpha = 0.4f)
            else SecColors.CardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .background(
                        if (fleetState.canImport) Color(0xFF4FC3F7).copy(alpha = 0.15f)
                        else SecColors.TagBg,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.UploadFile, null,
                    tint     = if (fleetState.canImport) Color(0xFF4FC3F7) else SecColors.TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Выгрузка флита",
                    color      = SecColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                )
                if (hasLastImport && lastDateStr != null) {
                    Text(
                        "Последний: $lastDateStr · ${fleetState.lastImportCount} шт",
                        color    = SecColors.TextMuted,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        "Загрузить .xlsx из системы",
                        color    = SecColors.TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Бейдж статуса
            if (fleetState.canImport) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF4FC3F7).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Обновить",
                        color      = Color(0xFF4FC3F7),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .background(SecColors.TagBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "✓ Актуально",
                        color    = SecColors.Success,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ============================================================================================
// ШАПКА ДАШБОРДА
// ============================================================================================

@Composable
private fun DashboardHeader(state: SecurityDashboardUiState, onMenuClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(SecColors.Accent.copy(alpha = 0.18f), SecColors.Bg))
            )
    ) {
        Column {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick  = onMenuClick,
                    modifier = Modifier.size(40.dp).background(SecColors.Card, CircleShape)
                ) {
                    Icon(Icons.Default.Menu, null, tint = SecColors.TextPrimary)
                }

                Spacer(Modifier.width(12.dp))

                Box(
                    modifier         = Modifier
                        .size(36.dp)
                        .background(
                            Brush.radialGradient(listOf(SecColors.Accent, SecColors.Accent.copy(alpha = 0.4f))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Служба безопасности",
                        color      = SecColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 17.sp
                    )
                    Text("Дашборд", color = SecColors.TextSecondary, fontSize = 12.sp)
                }

                if (state.totalLost > 0) PulsingBadge(count = state.totalLost)
            }

            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text          = "${state.totalLost}",
                    color         = if (state.totalLost > 0) SecColors.Accent else SecColors.Success,
                    fontSize      = 72.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = (-2).sp
                )
                Text(
                    text     = if (state.totalLost == 0) "В розыске нет самокатов" else "самокатов в розыске",
                    color    = SecColors.TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = "Всего найдено: ${state.totalFound}",
                    color      = SecColors.Success.copy(alpha = 0.8f),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ============================================================================================
// БЫСТРЫЕ КАРТОЧКИ
// ============================================================================================

@Composable
private fun QuickStatsRow(
    state: SecurityDashboardUiState,
    onNavigateToScooters: () -> Unit,
    onNavigateToBattery: () -> Unit,
    onNavigateToStorage: () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickCard("🔴", "Розыск",     state.totalLost.toString(),             SecColors.Accent,        onNavigateToScooters, Modifier.weight(1f))
        QuickCard("🔋", "АКБ выдано", state.activeEquipmentIssues.toString(), Color(0xFF4FC3F7),       onNavigateToBattery,  Modifier.weight(1f))
        QuickCard("📦", "Склад",      "→",                                    SecColors.TextSecondary, onNavigateToStorage,  Modifier.weight(1f))
    }
}

@Composable
private fun QuickCard(
    emoji: String,
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(14.dp),
        color    = SecColors.Card,
        border   = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                label,
                color    = SecColors.TextMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================================
// СЕГОДНЯ
// ============================================================================================

@Composable
private fun TodaySection(state: SecurityDashboardUiState) {
    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = SecColors.Card,
        border   = BorderStroke(1.dp, SecColors.CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp)
    ) {
        Row(modifier = Modifier.padding(18.dp)) {
            TodayPill("Найдено сегодня",      state.foundToday.toString(), SecColors.Success, "✅", Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp)
                    .background(SecColors.Divider)
                    .align(Alignment.CenterVertically)
            )
            TodayPill("Добавлено в розыск", state.lostToday.toString(), SecColors.Accent, "🔴", Modifier.weight(1f))
        }
    }
}

@Composable
private fun TodayPill(label: String, value: String, color: Color, emoji: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = SecColors.TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 14.sp)
    }
}

// ============================================================================================
// BREAKDOWN ПО ТЕГАМ
// ============================================================================================

@Composable
private fun TagBreakdownChart(breakdown: Map<ScooterTag, Int>) {
    val total  = breakdown.values.sum().takeIf { it > 0 } ?: 1
    val sorted = breakdown.entries.sortedByDescending { it.value }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sorted.forEach { (tag, count) ->
            val pct   = count.toFloat() / total
            val color = Color(android.graphics.Color.parseColor(tag.colorHex))
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tag.emoji, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    tag.label,
                    color    = SecColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.width(90.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(SecColors.CardBorder, RoundedCornerShape(4.dp))
                ) {
                    val animPct by animateFloatAsState(
                        targetValue   = pct,
                        animationSpec = tween(600, easing = EaseOutCubic),
                        label         = "bar_${tag.key}"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animPct)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color)),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    count.toString(),
                    color     = color,
                    fontSize  = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier  = Modifier.width(24.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// ============================================================================================
// BREAKDOWN ПО ХАБАМ
// ============================================================================================

@Composable
private fun HubBreakdownRow(breakdown: Map<String, Int>) {
    val total  = breakdown.values.sum().takeIf { it > 0 } ?: 1
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        breakdown.entries.sortedByDescending { it.value }.forEach { (hub, count) ->
            val pct   = count.toFloat() / total
            val emoji = when (hub) {
                SecurityHubs.BESTUZH -> "🏢"
                SecurityHubs.SOFIY   -> "🌾"
                else                 -> "📍"
            }
            val name = SecurityHubs.displayName(hub).split(" ").first()

            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = SecColors.TagBg,
                border   = BorderStroke(1.dp, SecColors.CardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier            = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(emoji, fontSize = 24.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(count.toString(), color = SecColors.Accent, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text(name, color = SecColors.TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(SecColors.CardBorder, RoundedCornerShape(2.dp))
                    ) {
                        val animPct by animateFloatAsState(
                            targetValue   = pct,
                            animationSpec = tween(700, easing = EaseOutCubic),
                            label         = "hub_$hub"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animPct)
                                .fillMaxHeight()
                                .background(SecColors.Accent, RoundedCornerShape(2.dp))
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${(pct * 100).toInt()}%", color = SecColors.TextMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

// ============================================================================================
// ТОП НАХОДИЛЬЩИКОВ
// ============================================================================================

@Composable
private fun TopFindersList(finders: List<Pair<String, Int>>) {
    val medals = listOf("🥇", "🥈", "🥉")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        finders.forEachIndexed { index, (name, count) ->
            val isTop = index < 3
            Surface(
                shape  = RoundedCornerShape(12.dp),
                color  = if (isTop) SecColors.Accent.copy(alpha = if (index == 0) 0.15f else 0.07f) else SecColors.TagBg,
                border = BorderStroke(
                    1.dp,
                    if (isTop) SecColors.Accent.copy(alpha = 0.3f) else SecColors.CardBorder
                )
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier         = Modifier
                            .size(32.dp)
                            .background(
                                if (isTop) SecColors.Accent.copy(alpha = 0.2f) else SecColors.CardBorder,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isTop) Text(medals[index], fontSize = 14.sp)
                        else Text("${index + 1}", color = SecColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        name,
                        color      = if (isTop) SecColors.TextPrimary else SecColors.TextSecondary,
                        fontWeight = if (index == 0) FontWeight.ExtraBold else if (isTop) FontWeight.Bold else FontWeight.Normal,
                        fontSize   = 14.sp,
                        modifier   = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                if (isTop) SecColors.Accent.copy(alpha = 0.2f) else SecColors.TagBg,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "$count ✅",
                            color      = if (isTop) SecColors.Accent else SecColors.TextMuted,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================================
// ПОСЛЕДНЯЯ АКТИВНОСТЬ
// ============================================================================================

@Composable
private fun RecentActivityList(items: List<ScooterPassport>, onTap: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd.MM HH:mm", Locale.forLanguageTag("ru")) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.take(8).forEach { passport ->
            val isLost = passport.status == "lost"
            val color  = if (isLost) SecColors.Accent else SecColors.Success
            val emoji  = if (isLost) "🔴" else "✅"
            val tags   = passport.tags.mapNotNull { ScooterTag.fromKey(it) }

            Surface(
                onClick = onTap,
                shape   = RoundedCornerShape(10.dp),
                color   = SecColors.TagBg,
                border  = BorderStroke(1.dp, color.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji, fontSize = 14.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            passport.scooterId,
                            color      = SecColors.TextPrimary,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (tags.isNotEmpty()) {
                            Text(tags.take(3).joinToString(" ") { it.emoji }, fontSize = 11.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (isLost) "Розыск" else "Найден",
                            color      = color,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (passport.updatedAt > 0L) {
                            Text(
                                sdf.format(Date(passport.updatedAt)),
                                color    = SecColors.TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        if (items.size > 8) {
            TextButton(onClick = onTap, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Смотреть все (${items.size}) →",
                    color      = SecColors.Accent,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ============================================================================================
// ОБЁРТКА СЕКЦИИ
// ============================================================================================

@Composable
private fun DashSection(title: String, emoji: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.padding(bottom = 12.dp)
        ) {
            Text(emoji, fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                title.uppercase(),
                color         = SecColors.TextMuted,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
        Surface(
            shape    = RoundedCornerShape(14.dp),
            color    = SecColors.Card,
            border   = BorderStroke(1.dp, SecColors.CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)