package com.example.qrscannerapp.features.homescreen.ui.widgets

// ─────────────────────── WeatherWidget ───────────────────────────────────────

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.WeatherData
import com.example.qrscannerapp.WeatherRepo
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WeatherAccent = Color(0xFF6A5AE0)

@Composable
fun WeatherWidget(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    sheenPhaseOffset: Float = 0f
) {
    var weather  by remember { mutableStateOf<WeatherData?>(null) }
    var dateText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val fmt = SimpleDateFormat("EEE, d MMM", Locale("ru"))
        while (true) { dateText = fmt.format(Date()).replaceFirstChar { it.uppercase() }; delay(60_000) }
    }
    LaunchedEffect(Unit) {
        while (true) { weather = WeatherRepo.load() ?: weather; delay(30 * 60 * 1000L) }
    }

    WidgetCard(
        accentColor      = WeatherAccent,
        hazeState        = hazeState,
        modifier         = modifier,
        glowStrength     = 0.6f,
        sheenPhaseOffset = sheenPhaseOffset,
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("САНКТ-ПЕТЕРБУРГ", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Text(weather?.let { "${it.tempC}°" } ?: "—", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp)
                Text(weather?.description ?: "загрузка...", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(weather?.emoji ?: "🌡", fontSize = 32.sp)
                Text(dateText, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
            }
        }
    }
}