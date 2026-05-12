package com.example.qrscannerapp.features.homescreen.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.features.tasks.domain.model.TaskPriority
import com.example.qrscannerapp.features.tasks.domain.model.TaskStatus
import com.example.qrscannerapp.features.tasks.ui.viewmodel.MyTasksViewModel
import dev.chrisbanes.haze.HazeState

private val TasksAccent  = Color(0xFFFBBF24)
private val PriorityHigh = Color(0xFFF87171)
private val PriorityMed  = Color(0xFFFBBF24)
private val PriorityLow  = Color(0xFF4ADE80)

@Composable
fun TasksWidget(
    viewModel: MyTasksViewModel,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    sheenFraction: Float = 0f,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    val active = remember(state.tasks) {
        state.tasks.filter {
            it.status == TaskStatus.NEW || it.status == TaskStatus.IN_PROGRESS
        }
    }
    val highCount    = active.count { it.priority == TaskPriority.HIGH.value }
    val medCount     = active.count { it.priority == TaskPriority.MEDIUM.value }
    val lowCount     = active.count { it.priority == TaskPriority.LOW.value }
    val nearestTitle = active.firstOrNull()?.title.orEmpty()

    // Жесты обрабатываем ОДНИМ pointerInput — tap и long press вместе.
    // Передаём в WidgetCard через modifier, onClick = null чтобы WidgetCard
    // не создавал свой pointerInput поверх нашего.
    val gestureModifier = Modifier.pointerInput(onTap, onLongPress) {
        detectTapGestures(
            onTap       = { onTap() },
            onLongPress = { onLongPress?.invoke() }
        )
    }

    WidgetCard(
        accentColor  = TasksAccent,
        hazeState    = hazeState,
        modifier     = modifier.then(gestureModifier),
        glowStrength = if (active.isNotEmpty()) 0.7f else 0.3f,
        sheenFraction   = sheenFraction,
        onClick      = null   // жесты уже обработаны выше
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text          = "ЗАДАЧИ",
                    color         = TasksAccent.copy(alpha = 0.95f),
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text       = active.size.toString(),
                    color      = Color.White,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.height(22.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                repeat(highCount.coerceAtMost(5)) { PriorityBar(PriorityHigh, 1.00f) }
                repeat(medCount.coerceAtMost(5))  { PriorityBar(PriorityMed,  0.70f) }
                repeat(lowCount.coerceAtMost(5))  { PriorityBar(PriorityLow,  0.50f) }
                if (active.isEmpty()) {
                    Text(
                        text  = "нет активных",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
            }

            if (nearestTitle.isNotBlank()) {
                Text(
                    text     = nearestTitle,
                    color    = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text  = "тапни чтобы открыть",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun PriorityBar(color: Color, fillFraction: Float) {
    Box(
        modifier = Modifier
            .width(4.dp)
            .height((22 * fillFraction).dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}