// Полное содержимое для ОБНОВЛЕННОГО файла WarehouseActivityComponents.kt

package com.example.qrscannerapp.features.inventory.ui.Warehouse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.*
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