package com.example.qrscannerapp.features.team.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.*
import com.example.qrscannerapp.common.ui.AnimatedCounter
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.common.ui.ScreenShell
import com.example.qrscannerapp.common.ui.SkeletonList

// ============================================================================================
// РОЛИ — утилиты
// ============================================================================================

private fun roleColor(role: String): Color = when (role) {
    "admin"             -> Color(0xFFEC407A)
    "inventory_manager" -> Color(0xFF4CAF50)
    "muver"             -> Color(0xFF29B6F6)
    "electrician"       -> Color(0xFFFFCA28)
    "technic"           -> Color(0xFFAB47BC)
    else                -> Color(0xFF78909C)
}

private fun roleDisplayName(role: String): String = when (role) {
    "admin"             -> "Админ"
    "inventory_manager" -> "Менеджер"
    "muver"             -> "Мувер"
    "electrician"       -> "Электрик"
    "technic"           -> "Техник"
    else                -> "Сотрудник"
}

private fun roleIcon(role: String): ImageVector = when (role) {
    "admin"             -> Icons.Default.AdminPanelSettings
    "inventory_manager" -> Icons.Default.Inventory2
    "muver"             -> Icons.Default.LocalShipping
    "electrician"       -> Icons.Default.ElectricBolt
    "technic"           -> Icons.Default.Construction
    else                -> Icons.Default.Person
}

private val warehouseNames = mapOf(
    "bestuzhevskaya_10" to "Бестужевская",
    "sklad_2"           to "Склад 2",
    "sklad_3"           to "Склад 3",
    "sklad_4"           to "Склад 4"
)

// ============================================================================================
// TEAM SCREEN
// ============================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (userId: String, userName: String, userRole: String) -> Unit,
    onNavigateToDirectChat: (peerId: String, peerName: String, peerRole: String) -> Unit,
    isAdmin: Boolean,
    viewModel: TeamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddEditDialog    by remember { mutableStateOf(false) }
    var employeeToEdit       by remember { mutableStateOf<EmployeeInfo?>(null) }
    var showDeleteConfirm    by remember { mutableStateOf(false) }
    var employeeToDeleteId   by remember { mutableStateOf<String?>(null) }

    // ── диалог создания/редактирования ──────────────────────────────────
    if (showAddEditDialog) {
        AddEditEmployeeDialog(
            initialName        = employeeToEdit?.name ?: "",
            initialUsername    = "",
            initialRole        = if (employeeToEdit != null) UserRole.fromKey(employeeToEdit!!.role) else UserRole.MOVER,
            initialWarehouseId = employeeToEdit?.warehouseId ?: "bestuzhevskaya_10",
            isEditMode         = employeeToEdit != null,
            onDismiss          = { showAddEditDialog = false; employeeToEdit = null },
            onConfirm          = { name, username, pass, role, warehouseId ->
                if (employeeToEdit != null)
                    viewModel.updateEmployee(employeeToEdit!!.id, name, username, pass, role.key, warehouseId)
                else
                    viewModel.createEmployee(name, username, pass, role.key, warehouseId)
                showAddEditDialog = false; employeeToEdit = null
            }
        )
    }

    // ── диалог удаления ─────────────────────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; employeeToDeleteId = null },
            title   = { Text("Удалить сотрудника?", color = StardustTextPrimary) },
            text    = { Text("Сотрудник будет удалён из базы навсегда.", color = StardustTextSecondary) },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(StardustError)
                        .clickable {
                            employeeToDeleteId?.let { viewModel.deleteEmployee(it) }
                            showDeleteConfirm = false; employeeToDeleteId = null
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) { Text("Удалить", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(StardustGlassBg)
                        .clickable { showDeleteConfirm = false }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) { Text("Отмена", color = StardustTextSecondary, fontWeight = FontWeight.SemiBold) }
            },
            containerColor      = StardustModalBg,
            titleContentColor   = StardustTextPrimary,
            textContentColor    = StardustTextSecondary,
            shape               = RoundedCornerShape(24.dp)
        )
    }

    ScreenShell {
        AppBackground {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    // ── iOS-стиль TopBar ─────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        // Назад
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(StardustGlassBg)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onNavigateBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, "Назад",
                                tint = StardustTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Центр
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Команда",
                                color = StardustTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                "${uiState.onlineCount} на смене · ${uiState.totalCount} всего",
                                color = StardustTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // FAB (только для админа)
                        if (isAdmin) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            listOf(StardustPrimary, StardustPrimary.copy(alpha = 0.7f))
                                        )
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { employeeToEdit = null; showAddEditDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd, null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // ── Поиск ────────────────────────────────────────
                    SearchBar(
                        query    = uiState.searchQuery,
                        onChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    // ── Pill-фильтры ──────────────────────────────────
                    val roleFilters = listOf(
                        TeamRoleFilter.ALL        to "Все",
                        TeamRoleFilter.MUVER      to "Муверы",
                        TeamRoleFilter.ELECTRICIAN to "Электрики",
                        TeamRoleFilter.TECHNIC    to "Техники",
                        TeamRoleFilter.MANAGER    to "Менеджеры"
                    )
                    val roleFilterCounts = listOf(
                        uiState.totalCount,
                        uiState.muverCount,
                        uiState.electricianCount,
                        uiState.technicCount,
                        uiState.managerCount
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(roleFilters.size) { i ->
                            val (filter, label) = roleFilters[i]
                            val count           = roleFilterCounts[i]
                            val selected        = uiState.roleFilter == filter
                            PillFilterChip(
                                label    = label,
                                count    = count,
                                selected = selected,
                                color    = when (filter) {
                                    TeamRoleFilter.ALL        -> StardustPrimary
                                    TeamRoleFilter.MUVER      -> Color(0xFF29B6F6)
                                    TeamRoleFilter.ELECTRICIAN -> Color(0xFFFFCA28)
                                    TeamRoleFilter.TECHNIC    -> Color(0xFFAB47BC)
                                    TeamRoleFilter.MANAGER    -> Color(0xFF4CAF50)
                                },
                                onClick  = { viewModel.setRoleFilter(filter) }
                            )
                        }
                    }

                    // ── Сортировка ────────────────────────────────────
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    ) {
                        items(TeamSortMode.entries.size) { i ->
                            val mode     = TeamSortMode.entries[i]
                            val selected = uiState.sortMode == mode
                            SortChip(
                                label    = mode.displayName,
                                selected = selected,
                                onClick  = { viewModel.setSortMode(mode) }
                            )
                        }
                    }

                    // ── Список ────────────────────────────────────────
                    when {
                        uiState.isLoading -> {
                            SkeletonList(itemCount = 6, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        uiState.members.isEmpty() -> {
                            EmptyState(hasQuery = uiState.searchQuery.isNotBlank())
                        }
                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = uiState.members,
                                    key   = { it.info.id }
                                ) { member ->
                                    Box(modifier = Modifier.animateItem(
                                        fadeInSpec     = spring(0.95f, 180f),
                                        fadeOutSpec    = spring(0.95f, 180f),
                                        placementSpec  = spring(0.88f, 260f)
                                    )) {
                                        TeamMemberCard(
                                            member      = member,
                                            isAdmin     = isAdmin,
                                            onClick     = { onNavigateToProfile(member.info.id, member.info.displayName, member.info.role) },
                                            onChatClick = { onNavigateToDirectChat(member.info.id, member.info.displayName, member.info.role) },
                                            onEditClick = { employeeToEdit = member.info; showAddEditDialog = true },
                                            onDeleteClick = { employeeToDeleteId = member.info.id; showDeleteConfirm = true }
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(32.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================================
// ПОИСК — iOS-стиль
// ============================================================================================

@Composable
private fun SearchBar(
    query: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focused = remember { mutableStateOf(false) }
    val borderAlpha by animateFloatAsState(
        targetValue = if (focused.value) 1f else 0f,
        animationSpec = tween(200),
        label = "border"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StardustGlassBg)
            .drawBehind {
                if (borderAlpha > 0f) {
                    drawRoundRect(
                        color = StardustPrimary.copy(alpha = borderAlpha * 0.6f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search, null,
                tint = if (focused.value) StardustPrimary else StardustTextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicSearchField(
                value    = query,
                onChange = onChange,
                onFocus  = { focused.value = it },
                modifier = Modifier.weight(1f)
            )
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter   = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.8f),
                exit    = fadeOut(tween(100)) + scaleOut(tween(100), targetScale = 0.8f)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(StardustTextSecondary.copy(alpha = 0.2f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = StardustTextSecondary, modifier = Modifier.size(11.dp))
                }
            }
        }
    }
}

@Composable
private fun BasicSearchField(
    value: String,
    onChange: (String) -> Unit,
    onFocus: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = StardustTextPrimary,
            fontSize = 15.sp
        ),
        cursorBrush = Brush.verticalGradient(listOf(StardustPrimary, StardustPrimary)),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text("Поиск...", color = StardustTextSecondary.copy(alpha = 0.5f), fontSize = 15.sp)
                }
                inner()
            }
        }
    )
}

// ============================================================================================
// PILL FILTER CHIP
// ============================================================================================

@Composable
private fun PillFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "pill_bg"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "pill_scale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(
                if (selected) color.copy(alpha = 0.18f)
                else StardustGlassBg
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                label,
                color = if (selected) color else StardustTextSecondary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (selected) color.copy(alpha = 0.25f)
                        else StardustTextSecondary.copy(alpha = 0.1f)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    count.toString(),
                    color = if (selected) color else StardustTextSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============================================================================================
// SORT CHIP
// ============================================================================================

@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) StardustPrimary.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(200),
        label = "sort_bg"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            label,
            color = if (selected) StardustPrimary else StardustTextSecondary.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ============================================================================================
// EMPTY STATE
// ============================================================================================

@Composable
private fun EmptyState(hasQuery: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(StardustGlassBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (hasQuery) Icons.Default.SearchOff else Icons.Default.Group,
                    null,
                    tint = StardustTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (hasQuery) "Никого не нашли" else "Нет сотрудников",
                color = StardustTextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (hasQuery) "Попробуй другой запрос" else "Добавь первого сотрудника",
                color = StardustTextSecondary.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }
    }
}

// ============================================================================================
// КАРТОЧКА СОТРУДНИКА — iOS redesign
// ============================================================================================

@Composable
fun TeamMemberCard(
    member: TeamMember,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onChatClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isOnline = member.info.isOnline
    val rColor   = roleColor(member.info.role)

    // Press-анимация
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue   = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label         = "card_press"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
            .clip(RoundedCornerShape(20.dp))
            .background(StardustGlassBg)
            // Левая цветная полоска
            .drawBehind {
                drawRoundRect(
                    color        = rColor.copy(alpha = 0.7f),
                    topLeft      = Offset(0f, size.height * 0.15f),
                    size         = androidx.compose.ui.geometry.Size(3.5.dp.toPx(), size.height * 0.7f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

            // ── Верхняя строка: аватар + инфо + счётчик ──────────────
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Аватар
                Box(modifier = Modifier.size(50.dp)) {
                    val initials = member.info.displayName
                        .split(" ").take(2)
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .joinToString("")

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(rColor.copy(alpha = 0.25f), rColor.copy(alpha = 0.08f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials, color = rColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }

                    // Онлайн-индикатор
                    Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                        if (isOnline) {
                            val pulse = rememberInfiniteTransition(label = "pulse_${member.info.id}")
                            val pScale by pulse.animateFloat(
                                1f, 2.2f,
                                infiniteRepeatable(tween(1100), RepeatMode.Restart),
                                "psc"
                            )
                            val pAlpha by pulse.animateFloat(
                                0.5f, 0f,
                                infiniteRepeatable(tween(1100), RepeatMode.Restart),
                                "pal"
                            )
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .scale(pScale)
                                    .alpha(pAlpha)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(StardustSolidBg)
                                .padding(2.5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOnline) Color(0xFF4CAF50) else Color(0xFF444455)
                                )
                        )
                    }
                }

                Spacer(Modifier.width(13.dp))

                // Имя + роль + склад
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        member.info.displayName,
                        color       = StardustTextPrimary,
                        fontWeight  = FontWeight.SemiBold,
                        fontSize    = 15.sp,
                        maxLines    = 1,
                        overflow    = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Бейдж роли
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(rColor.copy(alpha = 0.14f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    roleIcon(member.info.role), null,
                                    tint     = rColor,
                                    modifier = Modifier.size(9.dp)
                                )
                                Text(
                                    roleDisplayName(member.info.role),
                                    color      = rColor,
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // Склад
                        Text(
                            warehouseNames[member.info.warehouseId] ?: "",
                            color    = StardustTextSecondary.copy(alpha = 0.55f),
                            fontSize = 10.sp
                        )
                    }
                }

                // Счётчик сканов
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedCounter(
                        value      = member.scansToday,
                        color      = if (member.scansToday > 0) StardustTextPrimary else StardustTextSecondary.copy(alpha = 0.3f),
                        fontSize   = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "сканов",
                        color    = StardustTextSecondary.copy(alpha = 0.4f),
                        fontSize = 9.sp
                    )
                    if (member.scanRatePerHour > 0) {
                        Spacer(Modifier.height(1.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.12f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                "${member.scanRatePerHour}/ч",
                                color      = Color(0xFF4CAF50),
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Нижняя строка: активность + кнопки ───────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Активность
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val activityText = when {
                        member.minutesSinceLastActivity < 0    -> "нет данных"
                        member.minutesSinceLastActivity < 1    -> "только что"
                        member.minutesSinceLastActivity < 60   -> "${member.minutesSinceLastActivity} мин."
                        member.minutesSinceLastActivity < 1440 -> "${member.minutesSinceLastActivity / 60} ч."
                        else                                   -> "${member.minutesSinceLastActivity / 1440} дн."
                    }
                    val dotColor = when {
                        member.minutesSinceLastActivity in 0..5   -> Color(0xFF4CAF50)
                        member.minutesSinceLastActivity in 6..30  -> Color(0xFFFFCA28)
                        else                                       -> StardustTextSecondary.copy(alpha = 0.3f)
                    }
                    Box(
                        modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor)
                    )
                    Text(activityText, color = StardustTextSecondary.copy(alpha = 0.55f), fontSize = 11.sp)
                    if (member.batchesToday > 0) {
                        Text(
                            "· ${member.batchesToday} партий",
                            color    = StardustTextSecondary.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Кнопки действий
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Чат
                    ActionButton(
                        icon    = Icons.AutoMirrored.Filled.Chat,
                        color   = StardustPrimary,
                        onClick = onChatClick
                    )
                    if (isAdmin) {
                        // Редактировать
                        ActionButton(
                            icon    = Icons.Default.Edit,
                            color   = StardustTextSecondary.copy(alpha = 0.5f),
                            onClick = onEditClick
                        )
                        // Удалить
                        ActionButton(
                            icon    = Icons.Default.Delete,
                            color   = StardustError.copy(alpha = 0.6f),
                            bgColor = StardustError.copy(alpha = 0.1f),
                            onClick = onDeleteClick
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================================
// ACTION BUTTON — маленькая кнопка-иконка
// ============================================================================================

@Composable
private fun ActionButton(
    icon: ImageVector,
    color: Color,
    bgColor: Color = color.copy(alpha = 0.1f),
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
        label         = "action_scale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = { pressed = true; onClick(); pressed = false }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
    }
}