package com.example.qrscannerapp.features.team.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.*
import com.example.qrscannerapp.common.ui.AppBackground

// ============================================================================================
// ЦВЕТА РОЛЕЙ
// ============================================================================================

private fun roleColor(role: String): Color = when (role) {
    "admin" -> Color(0xFFEC407A)
    "inventory_manager" -> Color(0xFF4CAF50)
    "muver" -> Color(0xFF29B6F6)
    "electrician" -> Color(0xFFFFCA28)
    "technic" -> Color(0xFFAB47BC)
    else -> Color(0xFF78909C)
}

private fun roleDisplayName(role: String): String = when (role) {
    "admin" -> "Админ"
    "inventory_manager" -> "Менеджер"
    "muver" -> "Мувер"
    "electrician" -> "Электрик"
    "technic" -> "Техник"
    else -> "Сотрудник"
}

private val warehouseNames = mapOf(
    "bestuzhevskaya_10" to "Бестужевская",
    "sklad_2" to "Склад 2",
    "sklad_3" to "Склад 3",
    "sklad_4" to "Склад 4"
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

    var showAddEditDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<EmployeeInfo?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var employeeToDeleteId by remember { mutableStateOf<String?>(null) }

    // Диалог добавления/редактирования
    if (showAddEditDialog) {
        AddEditEmployeeDialog(
            initialName = employeeToEdit?.name ?: "",
            initialUsername = "",
            initialRole = if (employeeToEdit != null) UserRole.fromKey(employeeToEdit!!.role) else UserRole.MOVER,
            initialWarehouseId = employeeToEdit?.warehouseId ?: "bestuzhevskaya_10",
            isEditMode = employeeToEdit != null,
            onDismiss = { showAddEditDialog = false; employeeToEdit = null },
            onConfirm = { name: String, username: String, pass: String, role: UserRole, warehouseId: String ->
                if (employeeToEdit != null) {
                    viewModel.updateEmployee(employeeToEdit!!.id, name, username, pass, role.key, warehouseId)
                } else {
                    viewModel.createEmployee(name, username, pass, role.key, warehouseId)
                }
                showAddEditDialog = false; employeeToEdit = null
            }
        )
    }

    // Диалог подтверждения удаления
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false; employeeToDeleteId = null },
            title = { Text("Удалить сотрудника?") },
            text = { Text("Сотрудник будет удален из базы навсегда.") },
            confirmButton = {
                Button(
                    onClick = {
                        employeeToDeleteId?.let { viewModel.deleteEmployee(it) }
                        showDeleteConfirmDialog = false; employeeToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StardustError)
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Отмена") }
            },
            containerColor = StardustModalBg,
            titleContentColor = StardustTextPrimary,
            textContentColor = StardustTextPrimary
        )
    }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Команда")
                            Text(
                                "${uiState.onlineCount} на смене из ${uiState.totalCount}",
                                fontSize = 12.sp,
                                color = StardustTextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.3f),
                        titleContentColor = StardustTextPrimary,
                        navigationIconContentColor = StardustTextPrimary
                    )
                )
            },
            floatingActionButton = {
                if (isAdmin) {
                    FloatingActionButton(
                        onClick = { employeeToEdit = null; showAddEditDialog = true },
                        containerColor = StardustPrimary
                    ) {
                        Icon(Icons.Default.PersonAdd, "Добавить сотрудника")
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Поиск
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Поиск по имени...", color = StardustTextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = StardustTextSecondary) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, "Очистить", tint = StardustTextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StardustPrimary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = StardustGlassBg,
                        unfocusedContainerColor = StardustItemBg,
                        cursorColor = StardustPrimary,
                        focusedTextColor = StardustTextPrimary,
                        unfocusedTextColor = StardustTextPrimary
                    )
                )

                // Табы по ролям
                val roleFilters = listOf(
                    TeamRoleFilter.ALL to "Все ${uiState.totalCount}",
                    TeamRoleFilter.MUVER to "Муверы ${uiState.muverCount}",
                    TeamRoleFilter.ELECTRICIAN to "Электрики ${uiState.electricianCount}",
                    TeamRoleFilter.TECHNIC to "Техники ${uiState.technicCount}",
                    TeamRoleFilter.MANAGER to "Менеджеры ${uiState.managerCount}"
                )

                val selectedIndex = roleFilters.indexOfFirst { it.first == uiState.roleFilter }.coerceAtLeast(0)

                ScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = Color.Transparent,
                    contentColor = StardustTextPrimary,
                    edgePadding = 12.dp,
                    indicator = { tabPositions ->
                        if (selectedIndex in tabPositions.indices) {
                            Box(
                                modifier = Modifier
                                    .tabIndicatorOffset(tabPositions[selectedIndex])
                                    .height(3.dp)
                                    .background(StardustPrimary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            )
                        }
                    },
                    divider = { HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f)) }
                ) {
                    roleFilters.forEach { (filter, label) ->
                        Tab(
                            selected = uiState.roleFilter == filter,
                            onClick = { viewModel.setRoleFilter(filter) },
                            text = { Text(label, fontSize = 13.sp) }
                        )
                    }
                }

                // Сортировка
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TeamSortMode.entries.forEach { mode ->
                        val isSelected = uiState.sortMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSortMode(mode) },
                            label = { Text(mode.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StardustPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = StardustPrimary,
                                containerColor = StardustItemBg,
                                labelColor = StardustTextSecondary
                            ),
                            border = null,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Список
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = StardustPrimary)
                        }
                    }
                    uiState.members.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.SearchOff, null,
                                    tint = StardustTextSecondary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    if (uiState.searchQuery.isNotBlank()) "Никого не нашли"
                                    else "Нет сотрудников",
                                    color = StardustTextSecondary,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = uiState.members,
                                key = { member -> member.info.id }
                            ) { member ->
                                TeamMemberCard(
                                    member = member,
                                    isAdmin = isAdmin,
                                    onClick = {
                                        onNavigateToProfile(
                                            member.info.id,
                                            member.info.displayName,
                                            member.info.role
                                        )
                                    },
                                    onChatClick = {
                                        onNavigateToDirectChat(
                                            member.info.id,
                                            member.info.displayName,
                                            member.info.role
                                        )
                                    },
                                    onEditClick = {
                                        employeeToEdit = member.info
                                        showAddEditDialog = true
                                    },
                                    onDeleteClick = {
                                        employeeToDeleteId = member.info.id
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================================
// КАРТОЧКА СОТРУДНИКА
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
    val rColor = roleColor(member.info.role)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(46.dp)) {
                    val initials = member.info.displayName
                        .split(" ").take(2)
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .joinToString("")
                    Box(
                        modifier = Modifier.size(46.dp).clip(CircleShape)
                            .background(rColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials, color = rColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).size(14.dp)
                            .clip(CircleShape).background(StardustSolidBg)
                            .padding(2.dp).clip(CircleShape)
                            .background(if (isOnline) Color(0xFF4CAF50) else Color.Gray)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        member.info.displayName, color = StardustTextPrimary,
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Surface(color = rColor.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                roleDisplayName(member.info.role), color = rColor,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        val warehouse = warehouseNames[member.info.warehouseId] ?: "Неизвестно"
                        Text(warehouse, color = StardustTextSecondary.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        member.scansToday.toString(),
                        color = if (member.scansToday > 0) StardustTextPrimary else StardustTextSecondary,
                        fontWeight = FontWeight.Bold, fontSize = 22.sp
                    )
                    if (member.scanRatePerHour > 0) {
                        Text("${member.scanRatePerHour}/час", color = StardustSuccess.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    } else if (!isOnline) {
                        Text("оффлайн", color = StardustTextSecondary.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activityText = when {
                        member.minutesSinceLastActivity < 0 -> "нет данных"
                        member.minutesSinceLastActivity < 1 -> "только что"
                        member.minutesSinceLastActivity < 60 -> "${member.minutesSinceLastActivity} мин."
                        member.minutesSinceLastActivity < 1440 -> "${member.minutesSinceLastActivity / 60} ч."
                        else -> "${member.minutesSinceLastActivity / 1440} дн."
                    }
                    Text(activityText, color = StardustTextSecondary.copy(alpha = 0.6f), fontSize = 11.sp)
                    if (member.batchesToday > 0) {
                        Text("· ${member.batchesToday} партий", color = StardustTextSecondary.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onChatClick, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Chat, "Написать", tint = StardustPrimary.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                    if (isAdmin) {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Edit, "Редактировать", tint = StardustTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Delete, "Удалить", tint = StardustError.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}