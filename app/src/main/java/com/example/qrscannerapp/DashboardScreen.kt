package com.example.qrscannerapp

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.common.ui.AnimatedDialogWrapper
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.common.ui.FloatingEntrance
import com.example.qrscannerapp.common.ui.SkeletonBlock
import com.example.qrscannerapp.features.tasks.domain.model.Task
import com.example.qrscannerapp.features.tasks.domain.model.TaskPriority
import com.example.qrscannerapp.features.tasks.domain.model.TaskStatus
import java.text.SimpleDateFormat
import java.util.*
private fun getAppVersionName(context: android.content.Context): String {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).versionName ?: "1.3.8"
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.3.8"
        }
    } catch (_: Exception) { "1.3.8" }
}

private enum class ExpandedWidget { NONE, SCANS, FIELD, TEAM, TASKS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToEmployeeProfile: (userId: String) -> Unit,
    onNavigateToAdminRepairLog: () -> Unit,
    onNavigateToTaskCreation: () -> Unit,
    onNavigateToVehicleReport: () -> Unit,
    onNavigateToTeam: () -> Unit = {},
    onNavigateToFieldRepairAdmin: () -> Unit = {},
    onNavigateToEmployeeDetail: (userId: String, userName: String, userRole: String) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currentAppVersion = remember { getAppVersionName(context) }

    var expandedWidget by remember { mutableStateOf(ExpandedWidget.NONE) }
    var showAlertDialog by remember { mutableStateOf(false) }
    val selectedTaskForDetails = remember { mutableStateOf<Task?>(null) }
    val taskDetailsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pendingRequests = uiState.pendingShiftRequests
    val outdatedEmployees = remember(uiState.allEmployees, currentAppVersion) {
        uiState.allEmployees.filter { it.appVersion != null && it.appVersion != currentAppVersion && it.isOnline }
    }
    val alertCount = pendingRequests.size + (if (outdatedEmployees.isNotEmpty()) 1 else 0)

    if (showAlertDialog) {
        AlertDetailsDialog(
            pendingRequests = pendingRequests, outdatedEmployees = outdatedEmployees,
            currentAppVersion = currentAppVersion,
            onApprove = { viewModel.approveShiftRequest(it) },
            onDeny = { viewModel.denyShiftRequest(it) },
            onDismiss = { showAlertDialog = false }
        )
    }

    if (selectedTaskForDetails.value != null) {
        DashboardTaskDetailsSheet(
            task = selectedTaskForDetails.value!!,
            sheetState = taskDetailsSheetState,
            onDismiss = { selectedTaskForDetails.value = null },
            onDeleteTask = { taskId ->
                viewModel.deleteTask(taskId)
                selectedTaskForDetails.value = null
            }
        )
    }

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> DashboardSkeleton()
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        // ── Заголовок ──
                        item(key = "header") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Сегодня", style = MaterialTheme.typography.titleMedium, color = StardustTextSecondary)
                                if (alertCount > 0) {
                                    BadgedBox(badge = { Badge(containerColor = StardustError) { Text(alertCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }) {
                                        IconButton(onClick = { showAlertDialog = true }, modifier = Modifier.size(36.dp).background(StardustError.copy(alpha = 0.15f), CircleShape)) {
                                            Icon(Icons.Default.NotificationsActive, "Алерты", tint = StardustError, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // ── 4 виджета 2×2 + раскрытая панель ──
                        item(key = "widgets") {
                            FloatingEntrance(delayMs = 60) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DashWidget(
                                        title = "Сканы", value = uiState.scansToday.toString(), icon = Icons.Default.QrCodeScanner,
                                        sub = if (uiState.scansYesterday > 0) { val d = uiState.scansToday - uiState.scansYesterday; "${if (d >= 0) "↑" else "↓"} ${kotlin.math.abs(d)} vs вчера" } else null,
                                        subColor = if (uiState.scansToday >= uiState.scansYesterday) StardustSuccess else StardustError,
                                        isExpanded = expandedWidget == ExpandedWidget.SCANS, modifier = Modifier.weight(1f),
                                        onClick = { expandedWidget = if (expandedWidget == ExpandedWidget.SCANS) ExpandedWidget.NONE else ExpandedWidget.SCANS }
                                    )
                                    DashWidget(
                                        title = "Поле",
                                        value = if (uiState.fieldTotalToday > 0) "${uiState.fieldDoneToday}/${uiState.fieldTotalToday}" else "—",
                                        icon = Icons.Default.TwoWheeler,
                                        sub = if (uiState.fieldTotalToday > 0) "${(uiState.fieldDoneToday * 100f / uiState.fieldTotalToday).toInt()}% готово" else "нет сессии",
                                        subColor = if (uiState.fieldTotalToday > 0) StardustSuccess else StardustTextSecondary,
                                        isExpanded = expandedWidget == ExpandedWidget.FIELD, modifier = Modifier.weight(1f),
                                        onClick = { expandedWidget = if (expandedWidget == ExpandedWidget.FIELD) ExpandedWidget.NONE else ExpandedWidget.FIELD }
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DashWidget(
                                        title = "Команда", value = "${uiState.employeesOnShift}/${uiState.allEmployees.size}", icon = Icons.Default.Groups,
                                        sub = "на смене", subColor = StardustTextSecondary,
                                        isExpanded = expandedWidget == ExpandedWidget.TEAM, modifier = Modifier.weight(1f),
                                        onClick = { expandedWidget = if (expandedWidget == ExpandedWidget.TEAM) ExpandedWidget.NONE else ExpandedWidget.TEAM }
                                    )
                                    DashWidget(
                                        title = "Задачи", value = uiState.activeTasks.size.toString(), icon = Icons.Default.TaskAlt,
                                        sub = run { val n = uiState.activeTasks.count { it.status == TaskStatus.NEW }; val p = uiState.activeTasks.count { it.status == TaskStatus.IN_PROGRESS }; if (n > 0 || p > 0) "${n} новых · ${p} в работе" else "всё спокойно" },
                                        subColor = if (uiState.activeTasks.any { it.status == TaskStatus.IN_PROGRESS }) StardustWarning else StardustTextSecondary,
                                        isExpanded = expandedWidget == ExpandedWidget.TASKS, modifier = Modifier.weight(1f),
                                        onClick = { expandedWidget = if (expandedWidget == ExpandedWidget.TASKS) ExpandedWidget.NONE else ExpandedWidget.TASKS }
                                    )
                                }

                                // Раскрытая панель
                                AnimatedContent(
                                    targetState = expandedWidget,
                                    transitionSpec = { fadeIn(tween(200)) + expandVertically(tween(250)) togetherWith fadeOut(tween(150)) + shrinkVertically(tween(200)) },
                                    label = "expand"
                                ) { widget ->
                                    when (widget) {
                                        ExpandedWidget.SCANS -> ScansExpandPanel(
                                            employeeActivities = uiState.employeeActivities,
                                            scansToday = uiState.scansToday,
                                            scansYesterday = uiState.scansYesterday,
                                            hourlyActivity = uiState.hourlyActivity
                                        )
                                        ExpandedWidget.FIELD -> FieldExpandPanel(
                                            total = uiState.fieldTotalToday, done = uiState.fieldDoneToday,
                                            inProgress = uiState.fieldInProgressToday, toStorage = uiState.fieldToStorageToday,
                                            notFound = uiState.fieldNotFoundToday, techs = uiState.fieldTechnicianProgress,
                                            onOpenAdmin = onNavigateToFieldRepairAdmin
                                        )
                                        ExpandedWidget.TEAM -> TeamExpandPanel(
                                            employees = uiState.employeesOnShiftDetails, allEmployees = uiState.allEmployees,
                                            onEmployeeClick = { emp -> val info = uiState.allEmployees.find { it.id == emp.id }; if (info != null) onNavigateToEmployeeDetail(info.id, info.displayName, info.role) }
                                        )
                                        ExpandedWidget.TASKS -> TasksExpandPanel(
                                            tasks = uiState.activeTasks, onTaskClick = { selectedTaskForDetails.value = it },
                                            onCreateTask = onNavigateToTaskCreation
                                        )
                                        ExpandedWidget.NONE -> Box(Modifier.fillMaxWidth().height(0.dp))
                                    }
                                }
                            } }
                        }

                        // ── График активности ──
                        item(key = "activity_chart") {
                            FloatingEntrance(delayMs = 160) {
                            val chartPoints = when (uiState.chartMode) {
                                ActivityChartMode.TODAY -> uiState.hourlyActivity
                                ActivityChartMode.YESTERDAY -> uiState.yesterdayActivity
                                ActivityChartMode.WEEK -> uiState.weekActivity
                            }
                            HourlyActivityCard(points = chartPoints, chartMode = uiState.chartMode, onModeChange = { viewModel.setChartMode(it) })
                            }
                        }

                        // ── Инструменты ──
                        item(key = "tools") {
                            FloatingEntrance(delayMs = 260) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Инструменты", style = MaterialTheme.typography.titleMedium, color = StardustTextSecondary)
                                ToolCard(
                                    icon = Icons.Default.TwoWheeler, iconColor = Color(0xFF6C5CE7),
                                    title = "Полевой ремонт",
                                    subtitle = if (uiState.fieldTotalToday > 0) "Активно: ${uiState.fieldTotalToday} · Готово: ${uiState.fieldDoneToday}" else "Мониторинг · импорт заданий",
                                    badge = if (uiState.fieldInProgressToday > 0) uiState.fieldInProgressToday.toString() else null,
                                    badgeColor = StardustWarning, onClick = onNavigateToFieldRepairAdmin
                                )
                                ToolCard(
                                    icon = Icons.Default.Build, iconColor = Color(0xFF38BDF8),
                                    title = "Журнал ремонтов", subtitle = "Ремонты АКБ · по электрикам",
                                    badge = if (uiState.repairsToday > 0) uiState.repairsToday.toString() else null,
                                    badgeColor = Color(0xFF38BDF8), onClick = onNavigateToAdminRepairLog
                                )
                                ToolCard(
                                    icon = Icons.Default.Summarize, iconColor = StardustPrimary,
                                    title = "Сводка по самокатам", subtitle = "Загрузить и проанализировать Excel",
                                    onClick = onNavigateToVehicleReport
                                )
                            } }
                        }

                        item(key = "bottom_spacer") { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ── Виджет ───────────────────────────────────────────────────────────────────

@Composable
private fun DashWidget(title: String, value: String, icon: ImageVector, sub: String?, subColor: Color, isExpanded: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.border(1.dp, if (isExpanded) StardustPrimary.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(14.dp)).clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 90.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
        ) {
            Icon(icon, null, tint = if (isExpanded) StardustPrimary else StardustPrimary.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            Text(value, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, color = StardustTextSecondary, fontSize = 10.sp, maxLines = 1)
            if (sub != null) Text(sub, color = subColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(modifier = Modifier.size(width = 20.dp, height = 2.dp).clip(RoundedCornerShape(1.dp)).background(if (isExpanded) StardustPrimary else StardustTextSecondary.copy(alpha = 0.2f)))
        }
    }
}

// ── Раскрытые панели ─────────────────────────────────────────────────────────

@Composable
private fun ScansExpandPanel(employeeActivities: List<EmployeeActivity>, scansToday: Int, scansYesterday: Int, hourlyActivity: List<HourlyActivityPoint>) {
    val delta = scansToday - scansYesterday; val isUp = delta >= 0
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(StardustGlassBg).border(0.5.dp, StardustPrimary.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(scansToday.toString(), color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            val dc = if (isUp) StardustSuccess else StardustError
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(dc.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("${if (isUp) "↑" else "↓"} ${kotlin.math.abs(delta)} vs вчера", color = dc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (hourlyActivity.isNotEmpty() && hourlyActivity.any { it.scans > 0 }) {
            val maxS = hourlyActivity.maxOf { it.scans }.takeIf { it > 0 } ?: 1
            Row(modifier = Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                hourlyActivity.forEach { pt ->
                    val f = (pt.scans.toFloat() / maxS).coerceIn(0f, 1f)
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(f.coerceAtLeast(0.06f)).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)).background(StardustPrimary.copy(alpha = 0.4f + f * 0.5f)))
                }
            }
        }
        HorizontalDivider(color = StardustItemBg)
        Text("По сотрудникам", color = StardustTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        if (employeeActivities.isEmpty()) {
            Text("Активности пока нет", color = StardustTextSecondary, fontSize = 12.sp)
        } else {
            employeeActivities.sortedByDescending { it.totalScans }.take(5).forEach { activity ->
                val fraction = if (scansToday > 0) activity.totalScans.toFloat() / scansToday else 0f
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (activity.status == "online") StardustSuccess else StardustTextSecondary))
                    Text(activity.name.split(" ").firstOrNull() ?: activity.name, color = StardustTextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.width(60.dp).height(3.dp).clip(RoundedCornerShape(2.dp)), color = StardustPrimary, trackColor = StardustItemBg)
                    Text(activity.totalScans.toString(), color = StardustTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun FieldExpandPanel(total: Int, done: Int, inProgress: Int, toStorage: Int, notFound: Int, techs: List<TechnicianStat>, onOpenAdmin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(StardustGlassBg).border(0.5.dp, Color(0xFF6C5CE7).copy(alpha = 0.3f), RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (total == 0) { Text("Нет активной сессии", color = StardustTextSecondary, fontSize = 13.sp); return@Column }
        val t = total.toFloat()
        Row(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))) {
            if (done > 0)       Box(Modifier.weight(done / t).fillMaxHeight().background(StardustSuccess))
            if (toStorage > 0)  Box(Modifier.weight(toStorage / t).fillMaxHeight().background(Color(0xFF38BDF8)))
            if (notFound > 0)   Box(Modifier.weight(notFound / t).fillMaxHeight().background(StardustError))
            if (inProgress > 0) Box(Modifier.weight(inProgress / t).fillMaxHeight().background(StardustWarning))
            val newCnt = total - done - toStorage - notFound - inProgress
            if (newCnt > 0) Box(Modifier.weight(newCnt / t).fillMaxHeight().background(StardustItemBg))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (done > 0)       ExpandChip("✓ $done", StardustSuccess)
            if (toStorage > 0)  ExpandChip("▲ $toStorage", Color(0xFF38BDF8))
            if (notFound > 0)   ExpandChip("? $notFound", StardustError)
            if (inProgress > 0) ExpandChip("⟳ $inProgress", StardustWarning)
        }
        if (techs.isNotEmpty()) {
            HorizontalDivider(color = StardustItemBg)
            techs.take(3).forEach { tech ->
                val tf = if (tech.total > 0) tech.done.toFloat() / tech.total else 0f
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val initials = tech.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF6C5CE7).copy(0.15f)), contentAlignment = Alignment.Center) {
                        Text(initials, color = Color(0xFF6C5CE7), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(tech.name.split(" ").firstOrNull() ?: tech.name, color = StardustTextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    LinearProgressIndicator(progress = { tf }, modifier = Modifier.width(50.dp).height(3.dp).clip(RoundedCornerShape(2.dp)), color = if (tf >= 1f) StardustSuccess else Color(0xFF6C5CE7), trackColor = StardustItemBg)
                    Text("${tech.done}/${tech.total}", color = StardustTextSecondary, fontSize = 10.sp)
                }
            }
        }
        OutlinedButton(
            onClick = onOpenAdmin, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6C5CE7).copy(alpha = 0.4f)),
            contentPadding = PaddingValues(vertical = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6C5CE7))
        ) { Text("Открыть мониторинг →", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun TeamExpandPanel(employees: List<EmployeeActivity>, allEmployees: List<EmployeeInfo>, onEmployeeClick: (EmployeeActivity) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(StardustGlassBg).border(0.5.dp, StardustPrimary.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (employees.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.NightShelter, null, tint = StardustTextSecondary, modifier = Modifier.size(18.dp))
                Text("Никого нет на смене", color = StardustTextSecondary, fontSize = 13.sp)
            }
            return@Column
        }
        val onShift  = allEmployees.filter { emp -> employees.any { it.id == emp.id } }
        val offShift = allEmployees.filter { emp -> employees.none { it.id == emp.id } }
        if (onShift.isNotEmpty()) {
            Text("На смене · ${onShift.size}", color = StardustSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            onShift.forEach { info ->
                val activity = employees.find { it.id == info.id }
                val roleColor = when (info.role) { "admin" -> Color(0xFFEC407A); "inventory_manager" -> Color(0xFF4CAF50); "muver" -> Color(0xFF29B6F6); "electrician" -> Color(0xFFFFCA28); "technic" -> Color(0xFFAB47BC); else -> Color(0xFF78909C) }
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { activity?.let { onEmployeeClick(it) } }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(roleColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Text(info.displayName.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString(""), color = roleColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(info.displayName, color = StardustTextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(UserRole.fromKey(info.role).displayName, color = StardustTextSecondary, fontSize = 10.sp)
                    }
                    if (activity != null && activity.totalScans > 0) Text("${activity.totalScans} ск.", color = StardustSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (offShift.isNotEmpty()) {
            HorizontalDivider(color = StardustItemBg)
            Text("Не на смене · ${offShift.size}", color = StardustTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            offShift.take(4).forEach { info ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(StardustTextSecondary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Text(info.displayName.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString(""), color = StardustTextSecondary, fontSize = 8.sp)
                    }
                    Text(info.displayName, color = StardustTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (offShift.size > 4) Text("ещё ${offShift.size - 4}...", color = StardustTextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TasksExpandPanel(tasks: List<Task>, onTaskClick: (Task) -> Unit, onCreateTask: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StardustGlassBg)
            .border(0.5.dp, StardustPrimary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет активных задач", color = StardustTextSecondary, fontSize = 13.sp)
            }
        } else {
            tasks.take(5).forEach { task ->
                val timeString = remember(task.createdAt) {
                    task.createdAt?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: "--:--"
                }
                val statusColor = when (task.status) {
                    TaskStatus.NEW -> StardustPrimary
                    TaskStatus.IN_PROGRESS -> StardustWarning
                    TaskStatus.COMPLETED -> StardustSuccess
                    TaskStatus.CANCELED -> StardustError
                    else -> StardustTextSecondary
                }
                val priorityColor = when (TaskPriority.fromInt(task.priority)) {
                    TaskPriority.HIGH -> StardustError
                    TaskPriority.MEDIUM -> StardustPrimary
                    TaskPriority.LOW -> StardustTextSecondary
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StardustItemBg)
                        .clickable { onTaskClick(task) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(priorityColor)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            task.title,
                            color = StardustTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (task.assigneeName.isNotBlank()) {
                            Text(
                                task.assigneeName,
                                color = StardustTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when (task.status) {
                                    TaskStatus.NEW -> "Новая"
                                    TaskStatus.IN_PROGRESS -> "В работе"
                                    TaskStatus.COMPLETED -> "Готово"
                                    TaskStatus.CANCELED -> "Отменена"
                                    else -> "—"
                                },
                                color = statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(timeString, color = StardustTextSecondary, fontSize = 10.sp)
                    }
                }
            }
            if (tasks.size > 5) {
                Text(
                    "ещё ${tasks.size - 5} задач",
                    color = StardustTextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }
        }
        HorizontalDivider(color = StardustItemBg, modifier = Modifier.padding(vertical = 4.dp))
        Button(
            onClick = onCreateTask,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StardustPrimary.copy(alpha = 0.15f),
                contentColor = StardustPrimary
            ),
            contentPadding = PaddingValues(vertical = 0.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Создать задачу", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Инструмент-кнопка ─────────────────────────────────────────────────────────

@Composable
private fun ToolCard(icon: ImageVector, iconColor: Color, title: String, subtitle: String, badge: String? = null, badgeColor: Color = StardustWarning, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = StardustGlassBg), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = StardustTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                Text(subtitle, color = StardustTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (badge != null) Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(badgeColor.copy(0.15f)).padding(horizontal = 7.dp, vertical = 3.dp)) { Text(badge, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            Icon(Icons.Default.ChevronRight, null, tint = StardustTextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

// ── Вспомогалки ───────────────────────────────────────────────────────────────

@Composable
private fun ExpandChip(label: String, color: Color) {
    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.1f)).padding(horizontal = 7.dp, vertical = 3.dp)) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DashboardSkeleton() {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.3f).height(16.dp), cornerRadius = 6)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { SkeletonBlock(modifier = Modifier.weight(1f).height(90.dp)); SkeletonBlock(modifier = Modifier.weight(1f).height(90.dp)) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { SkeletonBlock(modifier = Modifier.weight(1f).height(90.dp)); SkeletonBlock(modifier = Modifier.weight(1f).height(90.dp)) }
        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(150.dp))
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.3f).height(14.dp), cornerRadius = 6)
        repeat(3) { SkeletonBlock(modifier = Modifier.fillMaxWidth().height(52.dp)) }
    }
}

// ── Диалоги ───────────────────────────────────────────────────────────────────

@Composable
private fun AlertDetailsDialog(pendingRequests: List<EmployeeInfo>, outdatedEmployees: List<EmployeeInfo>, currentAppVersion: String, onApprove: (String) -> Unit, onDeny: (String) -> Unit, onDismiss: () -> Unit) {
    AnimatedDialogWrapper(onDismiss = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = StardustModalBg), modifier = Modifier.fillMaxWidth().wrapContentHeight().clip(RoundedCornerShape(20.dp))) {
            Column(modifier = Modifier.padding(20.dp).defaultMinSize(minHeight = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Warning, null, tint = StardustError, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(10.dp)); Text("Требует внимания", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f)) }
                HorizontalDivider(color = StardustItemBg)
                if (pendingRequests.isNotEmpty()) {
                    Text("Запросы на смену", color = StardustTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    pendingRequests.forEach { employee ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) { Text(employee.displayName, color = StardustTextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(UserRole.fromKey(employee.role).displayName, color = StardustTextSecondary, fontSize = 11.sp) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledIconButton(onClick = { onApprove(employee.id) }, modifier = Modifier.size(32.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = StardustSuccess.copy(alpha = 0.2f))) { Icon(Icons.Default.Check, null, tint = StardustSuccess, modifier = Modifier.size(18.dp)) }
                                FilledIconButton(onClick = { onDeny(employee.id) }, modifier = Modifier.size(32.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = StardustError.copy(alpha = 0.2f))) { Icon(Icons.Default.Close, null, tint = StardustError, modifier = Modifier.size(18.dp)) }
                            }
                        }
                    }
                }
                if (outdatedEmployees.isNotEmpty()) {
                    if (pendingRequests.isNotEmpty()) HorizontalDivider(color = StardustItemBg)
                    Text("Устаревшая версия (v$currentAppVersion)", color = StardustWarning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    outdatedEmployees.forEach { emp ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(emp.displayName, color = StardustTextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(color = StardustWarning.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) { Text("v${emp.appVersion ?: "?"}", color = StardustWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Закрыть", color = StardustPrimary) }
            }
        }
    }
}

@Composable
fun HourlyActivityCard(points: List<HourlyActivityPoint>, chartMode: ActivityChartMode, onModeChange: (ActivityChartMode) -> Unit) {
    val hasData = points.any { it.scans > 0 }
    val maxScans = points.maxOfOrNull { it.scans }?.takeIf { it > 0 } ?: 1
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(chartMode) { animProgress.snapTo(0f); animProgress.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 200f)) }
    val progress by animProgress.asState()
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val chartHeight = if (screenWidth > 400) 120.dp else 100.dp

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StardustGlassBg), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Активность", color = StardustTextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(ActivityChartMode.TODAY to "Сегодня", ActivityChartMode.YESTERDAY to "Вчера", ActivityChartMode.WEEK to "Неделя").forEach { (mode, label) ->
                        FilterChip(selected = chartMode == mode, onClick = { onModeChange(mode) }, label = { Text(label, fontSize = 10.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StardustPrimary, selectedLabelColor = Color.White, containerColor = StardustItemBg, labelColor = StardustTextSecondary), border = null, shape = RoundedCornerShape(8.dp), modifier = Modifier.height(28.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedContent(targetState = chartMode, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) }, label = "chart") { mode ->
                if (points.isEmpty() || !hasData) {
                    Box(modifier = Modifier.fillMaxWidth().height(chartHeight), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.BarChart, null, tint = StardustTextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(28.dp)); Spacer(Modifier.height(4.dp)); Text("Пока нет активности", color = StardustTextSecondary, fontSize = 12.sp) }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth().height(chartHeight), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                        points.forEach { point ->
                            val frac = ((point.scans.toFloat() / maxScans) * progress).coerceIn(0f, 1f)
                            Column(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                                    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight().clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(StardustPrimary.copy(alpha = 0.08f)))
                                    if (frac > 0f) Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(frac).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(StardustPrimary.copy(alpha = (0.5f + frac * 0.5f).coerceIn(0f, 1f))))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val label = if (mode == ActivityChartMode.WEEK) listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").getOrElse(point.hour) { "" } else "${point.hour}"
                                Text(label, color = StardustTextSecondary, fontSize = 9.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTaskDetailsSheet(
    task: Task,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onDeleteTask: (String) -> Unit = {}
) {
    val timeString = remember(task.createdAt) {
        task.createdAt?.let { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(it) } ?: "—"
    }
    val priority = remember(task.priority) { TaskPriority.fromInt(task.priority) }
    val priorityColor = when (priority) {
        TaskPriority.HIGH -> StardustError
        TaskPriority.MEDIUM -> StardustPrimary
        TaskPriority.LOW -> StardustTextSecondary
    }
    val priorityLabel = when (priority) {
        TaskPriority.HIGH -> "Высокий"
        TaskPriority.MEDIUM -> "Средний"
        TaskPriority.LOW -> "Низкий"
    }
    val statusColor = when (task.status) {
        TaskStatus.NEW -> StardustPrimary
        TaskStatus.IN_PROGRESS -> StardustWarning
        TaskStatus.COMPLETED -> StardustSuccess
        TaskStatus.CANCELED -> StardustError
        else -> StardustTextSecondary
    }
    val statusLabel = when (task.status) {
        TaskStatus.NEW -> "Новая"
        TaskStatus.IN_PROGRESS -> "В работе"
        TaskStatus.COMPLETED -> "Завершена"
        TaskStatus.CANCELED -> "Отменена"
        else -> "Неизвестно"
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить задачу?", color = StardustTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("«${task.title}» будет удалена без возможности восстановления.", color = StardustTextSecondary) },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDeleteTask(task.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = StardustError)
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена", color = StardustTextSecondary) }
            },
            containerColor = StardustModalBg,
            titleContentColor = StardustTextPrimary,
            textContentColor = StardustTextSecondary
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StardustModalBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            // Priority accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(priorityColor.copy(alpha = 0.7f))
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Header
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            task.title,
                            color = StardustTextPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Status chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(statusLabel, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StardustItemBg)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = StardustTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // Description
                if (task.description.isNotBlank()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = StardustItemBg),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Text(
                            task.description,
                            color = StardustTextPrimary.copy(alpha = 0.85f),
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Info card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StardustItemBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TaskSheetInfoRow("Приоритет") {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(priorityColor))
                                Text(priorityLabel, color = priorityColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        HorizontalDivider(color = StardustGlassBg)
                        TaskSheetInfoRow("Назначен", if (task.assigneeName.isNotBlank()) task.assigneeName else "—")
                        HorizontalDivider(color = StardustGlassBg)
                        TaskSheetInfoRow("Создал", if (task.creatorName.isNotBlank()) task.creatorName else "—")
                        HorizontalDivider(color = StardustGlassBg)
                        TaskSheetInfoRow("Создана", timeString)
                    }
                }

                // Delete button
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StardustError.copy(alpha = 0.12f),
                        contentColor = StardustError
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Удалить задачу", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TaskSheetInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = StardustTextSecondary, fontSize = 12.sp, modifier = Modifier.weight(0.45f), fontWeight = FontWeight.Medium)
        Text(value, color = StardustTextPrimary, fontSize = 13.sp, modifier = Modifier.weight(0.55f), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TaskSheetInfoRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = StardustTextSecondary, fontSize = 12.sp, modifier = Modifier.weight(0.45f), fontWeight = FontWeight.Medium)
        Row(modifier = Modifier.weight(0.55f)) { content() }
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = StardustTextSecondary, modifier = modifier.padding(bottom = 12.dp))
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEmployeeDialog(
    initialName: String = "",
    initialUsername: String = "",
    initialRole: UserRole = UserRole.MOVER,
    initialWarehouseId: String = "bestuzhevskaya_10",
    isEditMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (name: String, username: String, password: String, role: UserRole, warehouseId: String) -> Unit
) {
    var name        by remember { mutableStateOf(initialName) }
    var username    by remember { mutableStateOf(initialUsername) }
    var password    by remember { mutableStateOf("") }
    var showPass    by remember { mutableStateOf(false) }
    var selectedRole        by remember { mutableStateOf(initialRole) }
    var isRoleExpanded      by remember { mutableStateOf(false) }
    var selectedWarehouseId by remember { mutableStateOf(initialWarehouseId) }
    var isWarehouseExpanded by remember { mutableStateOf(false) }

    val roles = UserRole.getSelectableRoles()
    val warehouses = listOf(
        "bestuzhevskaya_10" to "Бестужевская 10",
        "sklad_2"           to "Склад 2",
        "sklad_3"           to "Склад 3",
        "sklad_4"           to "Склад 4"
    )
    val selectedWarehouseName = warehouses.find { it.first == selectedWarehouseId }?.second ?: "—"

    val canConfirm = if (isEditMode) name.isNotBlank()
    else name.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    // ── Вход с анимацией ────────────────────────────────────────────────
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val sheetScale by animateFloatAsState(
        targetValue   = if (visible) 1f else 0.88f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label         = "dialog_scale"
    )
    val sheetAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(220),
        label         = "dialog_alpha"
    )

    fun handleDismiss() {
        visible = false
        onDismiss()
    }

    Dialog(
        onDismissRequest = { handleDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f * sheetAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null
                ) { handleDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .graphicsLayer { scaleX = sheetScale; scaleY = sheetScale; alpha = sheetAlpha }
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF1C1830), Color(0xFF12102A)),
                            start  = Offset(0f, 0f),
                            end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .drawBehind {
                        // тонкая линия сверху
                        drawRect(
                            color     = StardustPrimary.copy(alpha = 0.5f),
                            topLeft   = Offset(size.width * 0.15f, 0f),
                            size      = androidx.compose.ui.geometry.Size(size.width * 0.7f, 1.5.dp.toPx())
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null
                    ) { /* блок закрытия */ }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // ── Хэндл ───────────────────────────────────────
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp).height(4.dp)
                                .clip(CircleShape)
                                .background(StardustTextSecondary.copy(alpha = 0.25f))
                        )
                    }

                    // ── Заголовок ────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StardustPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isEditMode) Icons.Default.Edit else Icons.Default.PersonAdd,
                                    null,
                                    tint     = StardustPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (isEditMode) "Редактировать" else "Новый сотрудник",
                                color      = StardustTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 18.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(StardustTextSecondary.copy(alpha = 0.1f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null
                                ) { handleDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, null, tint = StardustTextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }

                    HorizontalDivider(
                        color    = StardustTextSecondary.copy(alpha = 0.07f),
                        modifier = Modifier.padding(horizontal = 22.dp)
                    )

                    // ── Поля ─────────────────────────────────────────
                    Column(
                        modifier              = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                        verticalArrangement   = Arrangement.spacedBy(10.dp)
                    ) {
                        // Имя и Фамилия
                        DialogField(
                            value       = name,
                            onChange    = { name = it },
                            placeholder = "Имя и Фамилия",
                            icon        = Icons.Default.Person
                        )

                        // Логин
                        DialogField(
                            value       = username,
                            onChange    = { username = it },
                            placeholder = if (isEditMode) "Новый логин (необязательно)" else "Логин",
                            icon        = Icons.Default.AlternateEmail
                        )

                        // Пароль
                        DialogField(
                            value       = password,
                            onChange    = { password = it },
                            placeholder = if (isEditMode) "Новый пароль (необязательно)" else "Пароль",
                            icon        = Icons.Default.Lock,
                            isPassword  = true,
                            showPassword = showPass,
                            onTogglePassword = { showPass = !showPass }
                        )

                        // Склад
                        DialogDropdown(
                            label        = "Склад",
                            value        = selectedWarehouseName,
                            icon         = Icons.Default.Warehouse,
                            expanded     = isWarehouseExpanded,
                            onToggle     = { isWarehouseExpanded = !isWarehouseExpanded },
                            onDismiss    = { isWarehouseExpanded = false }
                        ) {
                            warehouses.forEach { (id, wName) ->
                                DropdownItem(
                                    label     = wName,
                                    selected  = selectedWarehouseId == id,
                                    onClick   = { selectedWarehouseId = id; isWarehouseExpanded = false }
                                )
                            }
                        }

                        // Роль
                        DialogDropdown(
                            label     = "Роль",
                            value     = selectedRole.displayName,
                            icon      = Icons.Default.Badge,
                            expanded  = isRoleExpanded,
                            onToggle  = { isRoleExpanded = !isRoleExpanded },
                            onDismiss = { isRoleExpanded = false }
                        ) {
                            roles.forEach { role ->
                                DropdownItem(
                                    label    = role.displayName,
                                    selected = selectedRole == role,
                                    onClick  = { selectedRole = role; isRoleExpanded = false }
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color    = StardustTextSecondary.copy(alpha = 0.07f),
                        modifier = Modifier.padding(horizontal = 22.dp)
                    )

                    // ── Кнопки ────────────────────────────────────────
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Отмена
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(StardustGlassBg)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null
                                ) { handleDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Отмена",
                                color      = StardustTextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 15.sp
                            )
                        }

                        // Подтвердить
                        val confirmAlpha by animateFloatAsState(
                            targetValue   = if (canConfirm) 1f else 0.4f,
                            animationSpec = tween(200),
                            label         = "confirm_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1.6f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            StardustPrimary.copy(alpha = confirmAlpha),
                                            StardustPrimary.copy(alpha = confirmAlpha * 0.75f)
                                        )
                                    )
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null,
                                    enabled           = canConfirm
                                ) {
                                    if (canConfirm)
                                        onConfirm(name, username, password, selectedRole, selectedWarehouseId)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (isEditMode) Icons.Default.Check else Icons.Default.PersonAdd,
                                    null,
                                    tint     = Color.White.copy(alpha = confirmAlpha),
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    if (isEditMode) "Сохранить" else "Создать",
                                    color      = Color.White.copy(alpha = confirmAlpha),
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================================
// ПОЛЕ ВВОДА — iOS стиль
// ============================================================================================

@Composable
private fun DialogField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue   = if (focused) StardustPrimary.copy(alpha = 0.7f) else Color.Transparent,
        animationSpec = tween(200),
        label         = "field_border"
    )
    val iconColor by animateColorAsState(
        targetValue   = if (focused) StardustPrimary else StardustTextSecondary.copy(alpha = 0.4f),
        animationSpec = tween(200),
        label         = "icon_color"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StardustGlassBg)
            .drawBehind {
                if (focused) {
                    drawRoundRect(
                        color        = borderColor,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                        style        = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
    ) {
        Row(
            modifier          = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(10.dp))
            androidx.compose.foundation.text.BasicTextField(
                value         = value,
                onValueChange = onChange,
                singleLine    = true,
                modifier      = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused },
                textStyle     = androidx.compose.ui.text.TextStyle(
                    color    = StardustTextPrimary,
                    fontSize = 15.sp
                ),
                visualTransformation = if (isPassword && !showPassword)
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                else
                    androidx.compose.ui.text.input.VisualTransformation.None,
                cursorBrush   = Brush.verticalGradient(listOf(StardustPrimary, StardustPrimary)),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                color    = StardustTextSecondary.copy(alpha = 0.4f),
                                fontSize = 15.sp
                            )
                        }
                        inner()
                    }
                }
            )
            if (isPassword && onTogglePassword != null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = onTogglePassword
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null,
                        tint     = StardustTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================================
// ДРОПДАУН — iOS стиль
// ============================================================================================

@Composable
private fun DialogDropdown(
    label: String,
    value: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue   = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label         = "arrow"
    )

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(StardustGlassBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onToggle
                )
        ) {
            Row(
                modifier          = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon, null,
                    tint     = if (expanded) StardustPrimary else StardustTextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, color = StardustTextSecondary.copy(alpha = 0.5f), fontSize = 10.sp)
                    Text(value, color = StardustTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Icon(
                    Icons.Default.KeyboardArrowDown, null,
                    tint     = StardustTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = arrowRotation }
                )
            }
        }

        // Раскрывающийся список
        AnimatedVisibility(
            visible = expanded,
            enter   = fadeIn(tween(150)) + expandVertically(
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
            ),
            exit    = fadeOut(tween(100)) + shrinkVertically(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1A1830))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun DropdownItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(
        targetValue   = if (selected) 1f else 0f,
        animationSpec = tween(150),
        label         = "item_bg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StardustPrimary.copy(alpha = bgAlpha * 0.12f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color      = if (selected) StardustPrimary else StardustTextPrimary,
            fontSize   = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (selected) {
            Icon(
                Icons.Default.Check, null,
                tint     = StardustPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}