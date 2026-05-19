package com.example.qrscannerapp.features.homescreen.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.AccountViewModel
import dev.chrisbanes.haze.HazeState

private val TodayAccent = Color(0xFFF472B6)

@Composable
fun TodayStatsWidget(
    viewModel: AccountViewModel,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    sheenPhaseOffset: Float = 0f,
    onTap: () -> Unit
) {
    val state          by viewModel.uiState.collectAsState()
    val scansToday     = state.scansToday
    val sessionsToday  = state.sessionsToday
    val weeklyScans    = state.weeklyScans
    val personalRecord = state.personalRecord

    val progressFraction = remember(scansToday, personalRecord) {
        if (personalRecord <= 0) 0f
        else (scansToday.toFloat() / personalRecord.toFloat()).coerceIn(0f, 1f)
    }

    WidgetCard(
        accentColor      = TodayAccent,
        hazeState        = hazeState,
        modifier         = modifier,
        glowStrength     = if (scansToday > 0) 0.6f else 0.3f,
        sheenPhaseOffset = sheenPhaseOffset,
        onClick          = onTap
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("СЕГОДНЯ",          color = TodayAccent.copy(alpha = 0.95f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Text(scansToday.toString(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text("сканов · $sessionsToday партий", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                }
                ProgressRing(fraction = progressFraction, accent = TodayAccent)
            }
            WeekBars(
                values = remember(weeklyScans) { weeklyScans.map { it.count } },
                accent = TodayAccent
            )
        }
    }
}

@Composable
private fun ProgressRing(fraction: Float, accent: Color) {
    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(40.dp)) {
            val stroke = 3.dp.toPx()
            val inset  = stroke / 2f
            drawArc(
                color      = Color.White.copy(alpha = 0.10f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft    = Offset(inset, inset),
                size       = Size(size.width - stroke, size.height - stroke),
                style      = Stroke(width = stroke)
            )
            if (fraction > 0f) {
                drawArc(
                    color      = accent,
                    startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false,
                    topLeft    = Offset(inset, inset),
                    size       = Size(size.width - stroke, size.height - stroke),
                    style      = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Text("${(fraction * 100).toInt()}%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WeekBars(values: List<Int>, accent: Color) {
    val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { idx, v ->
            val h       = (v.toFloat() / max).coerceIn(0.05f, 1f)
            val isToday = idx == values.lastIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((16 * h).dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (isToday) accent else accent.copy(alpha = 0.3f + 0.08f * idx))
            )
        }
    }
}