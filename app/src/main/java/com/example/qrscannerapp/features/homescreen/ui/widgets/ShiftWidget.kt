package com.example.qrscannerapp.features.homescreen.ui.widgets

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.AuthManager
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

private val ShiftAccent = Color(0xFF22D380)        // green when active
private val ShiftAccentInactive = Color(0xFF6A5AE0) // purple when off-shift

/**
 * 2x2 widget — the centerpiece of the homescreen.
 *
 * On shift: pulsing dot, big elapsed time (H:MM), segmented progress out of 12h cap,
 * tap → opens Account screen for full shift detail.
 * Off shift: dimmed, single CTA "Начать смену".
 */
@Composable
fun ShiftWidget(
    authManager: AuthManager,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    sheenPhaseOffset: Float = 0f,
    onTap: () -> Unit
) {
    val authState by authManager.authState.collectAsState()
    val isActive = authState.isShiftActive
    val accent = if (isActive) ShiftAccent else ShiftAccentInactive

    // Tick the elapsed time every second so the widget feels alive. Using a
    // recomposing state instead of System.currentTimeMillis() inline avoids
    // recomposing the whole bento grid every frame.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isActive) {
        while (isActive) {
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }

    val elapsedMs = if (isActive && authState.shiftStartTime > 0L) {
        (nowMs - authState.shiftStartTime).coerceAtLeast(0L)
    } else 0L

    val totalMinutes = (elapsedMs / 60_000L).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val timeText = if (isActive) "%d:%02d".format(hours, minutes) else "—"

    // 12-hour cap is the WorkManager auto-end window in your shift system.
    val progressFraction = (totalMinutes / (12f * 60f)).coerceIn(0f, 1f)

    WidgetCard(
        accentColor      = accent,
        hazeState        = hazeState,
        modifier         = modifier,
        glowStrength     = if (isActive) 1f else 0.4f,
        sheenPhaseOffset = sheenPhaseOffset,
        onClick          = onTap
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row — status pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isActive) PulsingDot(accent) else StaticDot(accent)
                Text(
                    text = if (isActive) "СМЕНА" else "ОФФЛАЙН",
                    color = accent.copy(alpha = 0.95f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }

            // Big time
            Column {
                Text(
                    text = timeText,
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = if (isActive) {
                        "с ${formatStartTime(authState.shiftStartTime)}"
                    } else {
                        "тапни чтобы начать"
                    },
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp
                )
            }

            // Bottom — segmented progress bar (4 segments = 3h chunks of a 12h shift)
            SegmentedProgress(
                fraction = progressFraction,
                accent = accent,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "shift_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    Box(modifier = Modifier.size(10.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size((10 * scale).dp)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha))
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.55f), Color.Transparent),
                            radius = size.maxDimension * 1.5f
                        ),
                        radius = size.maxDimension * 1.5f
                    )
                }
        )
    }
}

@Composable
private fun StaticDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.5f))
    )
}

@Composable
private fun SegmentedProgress(
    fraction: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 4 segments. Each is "filled" proportionally to its position in the bar.
        // Segment N is fully on if fraction > (N+1)/4, partially if it's between
        // N/4 and (N+1)/4, off otherwise. weight() is implicitly available here
        // because we are inside a Row's RowScope.
        repeat(4) { idx ->
            val segStart = idx / 4f
            val segEnd = (idx + 1) / 4f
            val fill = ((fraction - segStart) / (segEnd - segStart)).coerceIn(0f, 1f)
            val color = accent.copy(alpha = (0.20f + fill * 0.55f))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

private fun formatStartTime(startMs: Long): String {
    if (startMs <= 0L) return "—"
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMs }
    val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val m = cal.get(java.util.Calendar.MINUTE)
    return "%02d:%02d".format(h, m)
}