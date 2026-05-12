package com.example.qrscannerapp.features.homescreen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.features.tasks.domain.model.TaskPriority
import com.example.qrscannerapp.features.tasks.domain.model.TaskStatus
import com.example.qrscannerapp.features.tasks.ui.viewmodel.MyTasksViewModel

private val TaskAccent = Color(0xFFFBBF24)

/**
 * Quick-look sheet content — shown when the user taps the Задачи icon tile
 * or the TasksWidget. Displays the first active task with key info, then
 * offers two CTAs: open full list, or dismiss.
 *
 * Designed to answer "what's the most urgent thing right now?" in under a second.
 */
@Composable
fun TaskQuickLookSheet(
    viewModel: MyTasksViewModel,
    onOpenAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val active = remember(state.tasks) {
        state.tasks
            .filter { it.status == TaskStatus.NEW || it.status == TaskStatus.IN_PROGRESS }
            .sortedWith(
                compareByDescending<com.example.qrscannerapp.features.tasks.domain.model.Task> {
                    when (it.priority) {
                        TaskPriority.HIGH.value   -> 3
                        TaskPriority.MEDIUM.value -> 2
                        else                      -> 1
                    }
                }
            )
    }

    val topTask = active.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 20.dp)
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(TaskAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.TaskAlt,
                    contentDescription = null,
                    tint = TaskAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "Задачи",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${active.size} активных",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (topTask == null) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.04f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Нет активных задач 🎉",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        } else {
            // Task card
            val priorityColor = when (topTask.priority) {
                TaskPriority.HIGH.value   -> Color(0xFFF87171)
                TaskPriority.MEDIUM.value -> Color(0xFFFBBF24)
                else                      -> Color(0xFF4ADE80)
            }
            val priorityLabel = when (topTask.priority) {
                TaskPriority.HIGH.value   -> "СРОЧНО"
                TaskPriority.MEDIUM.value -> "СРЕДНИЙ"
                else                      -> "НИЗКИЙ"
            }
            val statusLabel = when (topTask.status) {
                TaskStatus.IN_PROGRESS -> "В работе"
                else                   -> "Новая"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = 0.5.dp,
                        color = priorityColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Priority pill + status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(priorityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = priorityLabel,
                            color = priorityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Text(
                        text = statusLabel,
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp
                    )
                }

                // Task title
                Text(
                    text = topTask.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                // Description excerpt (if any)
                val desc = topTask.description.orEmpty().trim()
                if (desc.isNotBlank()) {
                    Text(
                        text = if (desc.length > 120) desc.take(120) + "…" else desc,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }

                // Assignee (if present)
                val assignee = topTask.assigneeName.orEmpty()
                if (assignee.isNotBlank()) {
                    Text(
                        text = "→ $assignee",
                        color = TaskAccent.copy(alpha = 0.80f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // If more tasks exist, show a count hint
            if (active.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ещё ${active.size - 1} ${pluralTask(active.size - 1)}",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CTAs
        Button(
            onClick = onOpenAll,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = TaskAccent.copy(alpha = 0.15f),
                contentColor = TaskAccent
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Открыть все задачи",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Закрыть",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp
            )
        }
    }
}

private fun pluralTask(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "задача"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "задачи"
    else -> "задач"
}