// Полное содержимое для ОБНОВЛЕННОГО файла WarehouseActivityComponents.kt

package com.example.qrscannerapp.features.inventory.ui.Warehouse.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.*
import com.example.qrscannerapp.features.inventory.data.WarehouseLog
import com.example.qrscannerapp.features.inventory.ui.Warehouse.Employee
import com.example.qrscannerapp.features.inventory.ui.Warehouse.generateColorForDashboard
import java.text.SimpleDateFormat
import java.util.*

// --- ИЗМЕНЕНО: Новые модели данных для сгруппированных операций из Firebase ---
// --- Старые Demo-модели и val demoActivities были УДАЛЕНЫ ---

/**
 * Представляет один взятый товар внутри сгруппированной операции.
 */
data class TakenItem(val itemName: String, val quantity: Int)

/**
 * Представляет одну сгруппированную операцию.
 * Может содержать несколько товаров, если они были взяты одним пользователем
 * в течение короткого промежутка времени.
 */
data class GroupedActivity(
    val userName: String,
    val items: List<TakenItem>,
    val timestamp: Long // Используется время последней транзакции в группе
)


// --- Компоненты ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogSheet(
    // ИЗМЕНЕНО: Теперь компонент принимает список реальных, сгруппированных операций
    activities: List<GroupedActivity>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // ИЗМЕНЕНО: Состояние теперь хранит объект нового типа GroupedActivity
    var selectedActivity by remember { mutableStateOf<GroupedActivity?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StardustModalBg,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                "Последние операции",
                style = MaterialTheme.typography.titleLarge,
                color = StardustTextPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ИЗМЕНЕНО: Используем переданный список 'activities' вместо 'demoActivities'
                items(activities) { activity ->
                    ActivityCard(
                        activity = activity,
                        onClick = { selectedActivity = activity }
                    )
                }
            }
        }
    }

    if (selectedActivity != null) {
        ActivityDetailSheet(
            activity = selectedActivity!!,
            onDismiss = { selectedActivity = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailSheet(
    // ИЗМЕНЕНО: Принимаем новый тип данных
    activity: GroupedActivity,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val time = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("ru")).format(Date(activity.timestamp))

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = StardustModalBg) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                "Детали операции: ${activity.userName}",
                style = MaterialTheme.typography.titleLarge,
                color = StardustTextPrimary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                time,
                color = StardustTextSecondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activity.items) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = StardustItemBg)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ИЗМЕНЕНО: Используем поле itemName из модели TakenItem
                            Text(item.itemName, color = StardustTextPrimary, modifier = Modifier.weight(1f))
                            // ИЗМЕНЕНО: Берем абсолютное значение, т.к. в логах оно отрицательное
                            Text("${kotlin.math.abs(item.quantity)} шт.", color = StardustPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun EmployeeHistorySheet(
    employees: List<Employee>,
    employeeHistory: List<WarehouseLog>,
    isLoading: Boolean,
    onSelectEmployee: (Employee) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }

    ModalBottomSheet(
        onDismissRequest = {
            selectedEmployee = null
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = StardustModalBg,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        AnimatedContent(
            targetState = selectedEmployee,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "emp_history"
        ) { employee ->
            if (employee == null) {
                EmployeeSelectionContent(employees = employees) { emp ->
                    selectedEmployee = emp
                    onSelectEmployee(emp)
                }
            } else {
                EmployeeHistoryContent(
                    employee = employee,
                    logs = employeeHistory,
                    isLoading = isLoading,
                    onBack = { selectedEmployee = null }
                )
            }
        }
    }
}

@Composable
private fun EmployeeSelectionContent(
    employees: List<Employee>,
    onSelect: (Employee) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "История по сотруднику",
            style = MaterialTheme.typography.titleLarge,
            color = StardustTextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        Text(
            "Выберите сотрудника для просмотра",
            color = StardustTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp)
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(employees) { emp ->
                Card(
                    onClick = { onSelect(emp) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StardustItemBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val avatarColor = generateColorForDashboard(emp.name)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(avatarColor.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                emp.name.firstOrNull()?.toString() ?: "?",
                                color = avatarColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Text(
                            emp.name,
                            color = StardustTextPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = StardustTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeHistoryContent(
    employee: Employee,
    logs: List<WarehouseLog>,
    isLoading: Boolean,
    onBack: () -> Unit
) {
    val dayFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale("ru")) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val groupedByDay = remember(logs) {
        logs.groupBy { dayFormat.format(it.timestamp.toDate()) }
    }
    val totalRecords = logs.size
    val totalQuantity = remember(logs) { logs.sumOf { kotlin.math.abs(it.quantityChange) } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = StardustTextPrimary)
            }
            Text(
                employee.name,
                style = MaterialTheme.typography.titleLarge,
                color = StardustTextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StardustPrimary)
                }
            }
            logs.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Нет записей", color = StardustTextPrimary, fontWeight = FontWeight.Medium)
                        Text(
                            "История появится после первых списаний",
                            color = StardustTextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = StardustPrimary.copy(alpha = 0.12f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "$totalRecords",
                                        color = StardustPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    )
                                    Text("операций", color = StardustTextSecondary, fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "$totalQuantity",
                                        color = StardustPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    )
                                    Text("позиций взято", color = StardustTextSecondary, fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${groupedByDay.size}",
                                        color = StardustPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    )
                                    Text("дней активн.", color = StardustTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    groupedByDay.forEach { (day, dayLogs) ->
                        item {
                            Text(
                                day,
                                color = StardustTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                        items(dayLogs) { log ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = StardustItemBg)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        log.itemName,
                                        color = StardustTextPrimary,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(StardustPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            "${kotlin.math.abs(log.quantityChange)} шт.",
                                            color = StardustPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        timeFormat.format(log.timestamp.toDate()),
                                        color = StardustTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityCard(
    // ИЗМЕНЕНО: Принимаем новый тип данных
    activity: GroupedActivity,
    onClick: () -> Unit
) {
    // Логика остается прежней, просто работает с новыми полями
    val firstItem = activity.items.first()
    val otherItemsCount = activity.items.size - 1

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustItemBg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp).background(StardustSecondary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(activity.userName.first().toString(), color = StardustTextPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(activity.userName, color = StardustTextPrimary, fontWeight = FontWeight.Medium)
                Text(
                    text = buildAnnotatedString {
                        // ИЗМЕНЕНО: Используем поле itemName и абсолютное значение количества
                        append("${firstItem.itemName} - ${kotlin.math.abs(firstItem.quantity)} шт.")
                        if (otherItemsCount > 0) {
                            withStyle(style = SpanStyle(color = StardustPrimary, fontWeight = FontWeight.Bold)) {
                                append(" и еще +$otherItemsCount")
                            }
                        }
                    },
                    color = StardustTextSecondary,
                    fontSize = 12.sp
                )
            }
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(activity.timestamp))
            Text(time, color = StardustTextSecondary, fontSize = 12.sp)
        }
    }
}