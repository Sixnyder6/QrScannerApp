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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.AuthManager
import kotlinx.coroutines.delay
import java.util.Calendar

private val ShiftGreen  = Color(0xFF22D380)
private val ShiftPurple = Color(0xFF6A5AE0)

@Composable
fun ShiftDialogContent(authManager: AuthManager) {
    val authState by authManager.authState.collectAsState()
    val isActive  = authState.isShiftActive
    val accent    = if (isActive) ShiftGreen else ShiftPurple

    if (!isActive) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.04f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text     = "Смена не начата",
                color    = Color.White.copy(alpha = 0.40f),
                fontSize = 13.sp
            )
        }
        return
    }

    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }

    val elapsedMs   = (nowMs - authState.shiftStartTime).coerceAtLeast(0L)
    val totalSec    = elapsedMs / 1000L
    val hours       = totalSec / 3600
    val minutes     = (totalSec % 3600) / 60
    val seconds     = totalSec % 60
    val timerText   = "%d:%02d:%02d".format(hours, minutes, seconds)
    val startText   = formatShiftStart(authState.shiftStartTime)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Живой таймер
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.07f))
                .border(0.5.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text          = timerText,
                    color         = Color.White,
                    fontSize      = 38.sp,
                    fontWeight    = FontWeight.Light,
                    letterSpacing = (-1).sp
                )
                Text(
                    text     = "с $startText",
                    color    = Color.White.copy(alpha = 0.38f),
                    fontSize = 11.sp
                )
            }
        }

        // Статус-строки
        ShiftStatusRow(label = "Смена начата",  done = true,  accent = accent)
        ShiftStatusRow(label = "Смена активна", done = true,  accent = accent)
        ShiftStatusRow(label = "Смена закрыта", done = false, accent = accent)
    }
}

@Composable
private fun ShiftStatusRow(label: String, done: Boolean, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text     = label,
            color    = Color.White.copy(alpha = if (done) 0.80f else 0.35f),
            fontSize = 13.sp
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (done) accent.copy(alpha = 0.18f)
                    else Color.White.copy(alpha = 0.06f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text      = if (done) "✓" else "–",
                color     = if (done) accent else Color.White.copy(alpha = 0.25f),
                fontSize  = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatShiftStart(startMs: Long): String {
    if (startMs <= 0L) return "—"
    val cal = Calendar.getInstance().apply { timeInMillis = startMs }
    val h   = cal.get(Calendar.HOUR_OF_DAY)
    val m   = cal.get(Calendar.MINUTE)
    return "%02d:%02d".format(h, m)
}