package com.example.qrscannerapp.features.street_doctor.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private val FaBg        = Color(0xFF0A0A0F)
private val FaCard      = Color(0xFF14141C)
private val FaCardAlt   = Color(0xFF1A1A24)
private val FaBorder    = Color(0xFF252535)
private val FaPrimary   = Color(0xFF7C6FE0)
private val FaSuccess   = Color(0xFF22C55E)
private val FaWarning   = Color(0xFFF59E0B)
private val FaDanger    = Color(0xFFEF4444)
private val FaStorage   = Color(0xFF38BDF8)
private val FaHub       = Color(0xFF38BDF8)
private val FaTextMain  = Color(0xFFF5F5FF)
private val FaTextMuted = Color(0xFF6B6B85)
private val FaTextSec   = Color(0xFF9999B5)

@Composable
fun FieldRepairAdminScreen(
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenImport: () -> Unit,
    viewModel: FieldRepairAdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var expandedSessionId by remember { mutableStateOf<String?>(null) }
    var showImportSheet by remember { mutableStateOf(false) }
    var reportSession by remember { mutableStateOf<FieldSession?>(null) }

    if (showImportSheet) {
        FieldRepairImportSheet(onDismiss = { showImportSheet = false })
    }

    // Отчёт открывается поверх основного экрана
    reportSession?.let { session ->
        SessionReportScreen(
            session = session,
            onBack  = { reportSession = null }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(FaBg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Шапка ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(FaPrimary.copy(alpha = 0.15f), Color.Transparent)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 52.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(FaCard)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = FaTextMain, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ПОЛЕВОЙ РЕМОНТ", fontSize = 10.sp, color = FaPrimary, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        Text("Мониторинг", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = FaTextMain)
                    }
                }

                // Табы
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FaCard).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0 to "Активные · ${uiState.activeSessions.size}", 2 to "История · ${uiState.closedSessions.size}").forEach { (i, label) ->
                            val sel = selectedTab == i
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (sel) FaPrimary else Color.Transparent).clickable { selectedTab = i }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                                Text(label, fontSize = 12.sp, color = if (sel) Color.White else FaTextMuted, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val isSt = selectedTab == 1
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (isSt) FaDanger.copy(0.18f) else FaCard)
                                .border(1.dp, if (isSt) FaDanger.copy(0.5f) else FaBorder, RoundedCornerShape(12.dp))
                                .clickable { selectedTab = 1 }.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.Warehouse, null, tint = if (isSt) FaDanger else FaTextMuted, modifier = Modifier.size(14.dp))
                                Text("На склад · ${uiState.storageScooters.size}", fontSize = 12.sp, color = if (isSt) FaDanger else FaTextMuted, fontWeight = if (isSt) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                        val isHub = selectedTab == 3
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (isHub) Brush.linearGradient(listOf(FaHub.copy(0.20f), FaPrimary.copy(0.12f))) else Brush.linearGradient(listOf(FaCard, FaCard)))
                                .border(1.dp, if (isHub) FaHub.copy(0.5f) else FaBorder, RoundedCornerShape(12.dp))
                                .clickable { selectedTab = 3 }.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.Dashboard, null, tint = if (isHub) FaHub else FaTextMuted, modifier = Modifier.size(14.dp))
                                Text("Доска", fontSize = 12.sp, color = if (isHub) FaHub else FaTextMuted, fontWeight = if (isHub) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }

            // ── Контент ──
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val dir = if (targetState > initialState) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End
                    slideIntoContainer(dir, tween(250)) + fadeIn(tween(200)) togetherWith slideOutOfContainer(dir, tween(250)) + fadeOut(tween(150))
                },
                label = "tab_content"
            ) { tab ->
                when (tab) {
                    0 -> SessionsContent(sessions = uiState.activeSessions, isLoading = uiState.isLoading, emptyLabel = "Нет активных сессий", emptyIcon = true, expandedId = expandedSessionId, onToggle = { id -> expandedSessionId = if (expandedSessionId == id) null else id }, uiState = uiState, onStorageClick = { selectedTab = 1 }, onSessionClick = null)
                    1 -> StorageListScreen(scooters = uiState.storageScooters, isLoading = uiState.isLoading, onExportClick = {})
                    2 -> SessionsContent(sessions = uiState.closedSessions, isLoading = uiState.isLoading, emptyLabel = "История пуста", emptyIcon = false, expandedId = expandedSessionId, onToggle = { id -> expandedSessionId = if (expandedSessionId == id) null else id }, uiState = null, onStorageClick = {}, onSessionClick = { reportSession = it })
                    3 -> FieldHubScreen()
                    else -> Box(Modifier.fillMaxSize())
                }
            }
        }

        AnimatedVisibility(
            visible = selectedTab == 0 || selectedTab == 2,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it },
            exit  = fadeOut(tween(150)) + slideOutVertically(tween(150)) { it }
        ) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, FaBg))).padding(16.dp)) {
                Button(onClick = { showImportSheet = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = FaPrimary), contentPadding = PaddingValues(vertical = 14.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Новая сессия / Импорт", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun SessionsContent(
    sessions: List<FieldSession>,
    isLoading: Boolean,
    emptyLabel: String,
    emptyIcon: Boolean,
    expandedId: String?,
    onToggle: (String) -> Unit,
    uiState: FieldRepairAdminUiState?,
    onStorageClick: () -> Unit,
    onSessionClick: ((FieldSession) -> Unit)?
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {

        if (uiState != null) {
            item {
                if (isLoading) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = FaPrimary, modifier = Modifier.size(28.dp)) }
                } else {
                    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(FaPrimary.copy(0.2f), FaPrimary.copy(0.05f)))).border(1.dp, FaPrimary.copy(0.25f), RoundedCornerShape(16.dp)).padding(16.dp)) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("Всего сегодня", color = FaTextMuted, fontSize = 11.sp)
                                        Text(uiState.totalToday.toString(), color = FaTextMain, fontSize = 36.sp, fontWeight = FontWeight.Black)
                                        Text("самокатов", color = FaTextSec, fontSize = 12.sp)
                                    }
                                    if (uiState.totalToday > 0) {
                                        val doneF = uiState.doneToday.toFloat() / uiState.totalToday
                                        Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = FaBorder, strokeWidth = 7.dp, trackColor = Color.Transparent)
                                            CircularProgressIndicator(progress = { doneF }, modifier = Modifier.fillMaxSize(), color = FaSuccess, strokeWidth = 7.dp, trackColor = Color.Transparent)
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${(doneF * 100).toInt()}%", color = FaTextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("готово", color = FaTextMuted, fontSize = 9.sp) }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                if (uiState.totalToday > 0) {
                                    val t = uiState.totalToday.toFloat()
                                    Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                                        if (uiState.doneToday > 0)       Box(Modifier.weight(uiState.doneToday / t).fillMaxHeight().background(FaSuccess))
                                        if (uiState.toStorageToday > 0)  Box(Modifier.weight(uiState.toStorageToday / t).fillMaxHeight().background(FaStorage))
                                        if (uiState.notFoundToday > 0)   Box(Modifier.weight(uiState.notFoundToday / t).fillMaxHeight().background(FaDanger))
                                        if (uiState.inProgressToday > 0) Box(Modifier.weight(uiState.inProgressToday / t).fillMaxHeight().background(FaWarning))
                                        val newC = uiState.totalToday - uiState.doneToday - uiState.toStorageToday - uiState.notFoundToday - uiState.inProgressToday
                                        if (newC > 0) Box(Modifier.weight(newC / t).fillMaxHeight().background(FaBorder))
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusMiniCard("Готово", uiState.doneToday, FaSuccess, Modifier.weight(1f))
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(FaStorage.copy(0.08f)).border(0.5.dp, FaStorage.copy(0.2f), RoundedCornerShape(10.dp)).clickable { onStorageClick() }.padding(8.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(uiState.toStorageToday.toString(), color = FaStorage, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold); Text("На склад ›", color = FaStorage, fontSize = 9.sp, textAlign = TextAlign.Center) }
                            }
                            StatusMiniCard("Не найден", uiState.notFoundToday, FaDanger, Modifier.weight(1f))
                            StatusMiniCard("В работе", uiState.inProgressToday, FaWarning, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (onSessionClick != null && sessions.isNotEmpty()) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.TouchApp, null, tint = FaTextMuted, modifier = Modifier.size(14.dp))
                    Text("Нажми на сессию — откроется полный отчёт", color = FaTextMuted, fontSize = 11.sp)
                }
            }
        }

        if (sessions.isEmpty() && !isLoading) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(if (emptyIcon) Icons.Outlined.WorkOutline else Icons.Outlined.History, null, tint = FaTextMuted, modifier = Modifier.size(40.dp))
                    Text(emptyLabel, color = FaTextMuted, fontSize = 14.sp)
                    if (emptyIcon) Text("Нажмите «+ Новая сессия» внизу", color = FaTextMuted.copy(0.6f), fontSize = 12.sp)
                }
            }
        }

        items(sessions, key = { it.id }) { session ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SessionCard(session = session, isExpanded = expandedId == session.id, onToggle = { onToggle(session.id) }, onOpenReport = onSessionClick?.let { cb -> { cb(session) } })
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SessionCard(session: FieldSession, isExpanded: Boolean, onToggle: () -> Unit, onOpenReport: (() -> Unit)?) {
    val doneF    = if (session.totalCount > 0) session.doneCount.toFloat() / session.totalCount else 0f
    val animProg by animateFloatAsState(targetValue = doneF, animationSpec = spring(dampingRatio = 0.8f), label = "prog")
    val dateStr  = remember(session.createdAt) { if (session.createdAt > 0) java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(session.createdAt)) else "—" }

    Card(modifier = Modifier.fillMaxWidth().border(1.dp, if (isExpanded) FaPrimary.copy(0.4f) else FaBorder, RoundedCornerShape(14.dp)), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (isExpanded) FaCardAlt else FaCard)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(FaPrimary.copy(0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.DirectionsBike, null, tint = FaPrimary, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Сессия $dateStr", color = FaTextMain, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        if (session.status == "active") {
                            val pulse = rememberInfiniteTransition(label = "p")
                            val a by pulse.animateFloat(0.4f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(FaSuccess.copy(alpha = a)))
                        }
                    }
                    Text("${session.technicians.size} техников · ${session.totalCount} самокатов", color = FaTextMuted, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${session.doneCount}/${session.totalCount}", color = if (doneF >= 1f) FaSuccess else FaTextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = FaTextMuted, modifier = Modifier.size(18.dp))
                }
            }
            if (session.totalCount > 0) {
                LinearProgressIndicator(progress = { animProg }, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(4.dp).clip(RoundedCornerShape(2.dp)), color = FaSuccess, trackColor = FaBorder)
            }
            AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(top = 12.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (session.technicians.isEmpty()) {
                        Text("Нет данных по техникам", color = FaTextMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        session.technicians.forEach { tech -> TechnicianProgressRow(tech = tech) }
                    }
                    if (onOpenReport != null) {
                        OutlinedButton(onClick = onOpenReport, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = FaPrimary), border = androidx.compose.foundation.BorderStroke(1.dp, FaPrimary.copy(0.4f))) {
                            Icon(Icons.Default.Assessment, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Открыть полный отчёт", fontSize = 13.sp)
                        }
                    }
                }
            }
            if (!isExpanded) Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun TechnicianProgressRow(tech: FieldTechnicianProgress) {
    val techDoneF = if (tech.total > 0) tech.done.toFloat() / tech.total else 0f
    val initials  = tech.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(FaBg.copy(alpha = 0.5f)).padding(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(FaPrimary.copy(0.15f)), contentAlignment = Alignment.Center) { Text(initials, color = FaPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            Column(modifier = Modifier.weight(1f)) {
                Text(tech.name.split(" ").firstOrNull() ?: tech.name, color = FaTextMain, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("${tech.total} задан.", color = FaTextMuted, fontSize = 10.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (tech.done > 0)       MiniStatusDot(tech.done,       FaSuccess)
                if (tech.toStorage > 0)  MiniStatusDot(tech.toStorage,  FaStorage)
                if (tech.notFound > 0)   MiniStatusDot(tech.notFound,   FaDanger)
                if (tech.inProgress > 0) MiniStatusDot(tech.inProgress, FaWarning)
            }
            Text("${tech.done}/${tech.total}", color = if (techDoneF >= 1f) FaSuccess else FaTextSec, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(7.dp))
        if (tech.total > 0) {
            val t = tech.total.toFloat()
            Row(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(2.dp))) {
                if (tech.done > 0)       Box(Modifier.weight(tech.done / t).fillMaxHeight().background(FaSuccess))
                if (tech.toStorage > 0)  Box(Modifier.weight(tech.toStorage / t).fillMaxHeight().background(FaStorage))
                if (tech.notFound > 0)   Box(Modifier.weight(tech.notFound / t).fillMaxHeight().background(FaDanger))
                if (tech.inProgress > 0) Box(Modifier.weight(tech.inProgress / t).fillMaxHeight().background(FaWarning))
                val newCnt = tech.total - tech.done - tech.toStorage - tech.notFound - tech.inProgress
                if (newCnt > 0) Box(Modifier.weight(newCnt / t).fillMaxHeight().background(FaBorder))
            }
        }
    }
}

@Composable
private fun MiniStatusDot(count: Int, color: Color) {
    Row(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(3.dp))
        Text(count.toString(), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusMiniCard(label: String, value: Int, color: Color, modifier: Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.08f)).border(0.5.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = FaTextMuted, fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 12.sp)
    }
}