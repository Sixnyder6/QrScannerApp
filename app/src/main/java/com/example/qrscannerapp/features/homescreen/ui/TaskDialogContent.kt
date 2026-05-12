package com.example.qrscannerapp.features.homescreen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import com.example.qrscannerapp.features.tasks.domain.model.Task
import com.example.qrscannerapp.features.tasks.domain.model.TaskPriority
import com.example.qrscannerapp.features.tasks.domain.model.TaskStatus
import com.example.qrscannerapp.features.tasks.ui.viewmodel.MyTasksViewModel

private val TaskAccent = Color(0xFFFBBF24)

@Composable
fun TaskDialogContent(viewModel: MyTasksViewModel) {
    val state by viewModel.uiState.collectAsState()

    val active = remember(state.tasks) {
        state.tasks
            .filter { it.status == TaskStatus.NEW || it.status == TaskStatus.IN_PROGRESS }
            .sortedByDescending { task ->
                when (task.priority) {
                    TaskPriority.HIGH.value   -> 3
                    TaskPriority.MEDIUM.value -> 2
                    else                      -> 1
                }
            }
    }

    val topTask = active.firstOrNull()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Счётчик
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text     = "${active.size} активных",
                color    = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp
            )
            if (active.size > 1) {
                Text(
                    text     = "ещё ${active.size - 1} ${pluralTask(active.size - 1)}",
                    color    = TaskAccent.copy(alpha = 0.65f),
                    fontSize = 11.sp
                )
            }
        }

        if (topTask == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.04f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = "Нет активных задач 🎉",
                    color    = Color.White.copy(alpha = 0.45f),
                    fontSize = 13.sp
                )
            }
        } else {
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.5.dp, priorityColor.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(priorityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text          = priorityLabel,
                        color         = priorityColor,
                        fontSize      = 9.sp,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                }
                Text(
                    text       = topTask.title,
                    color      = Color.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                val desc = topTask.description.trim()
                if (desc.isNotBlank()) {
                    Text(
                        text       = if (desc.length > 80) desc.take(80) + "…" else desc,
                        color      = Color.White.copy(alpha = 0.50f),
                        fontSize   = 11.sp,
                        lineHeight = 15.sp
                    )
                }
                val assignee = topTask.assigneeName.trim()
                if (assignee.isNotBlank()) {
                    Text(
                        text       = "→ $assignee",
                        color      = TaskAccent.copy(alpha = 0.80f),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun pluralTask(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11         -> "задача"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "задачи"
    else                                   -> "задач"
}