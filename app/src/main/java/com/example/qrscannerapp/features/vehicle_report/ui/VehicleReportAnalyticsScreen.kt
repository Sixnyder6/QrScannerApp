// File: features/vehicle_report/ui/VehicleReportAnalyticsScreen.kt

package com.example.qrscannerapp.features.vehicle_report.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import co.yml.charts.ui.linechart.LineChart
import com.example.qrscannerapp.*
import com.example.qrscannerapp.common.ui.AppBackground

@Composable
fun VehicleReportAnalyticsScreen(
    viewModel: VehicleReportAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // V-- НАЧАЛО ИЗМЕНЕНИЙ: ИНТЕГРАЦИЯ APPBACKGROUND --V
    // 1. AppBackground вызывается без модификаторов, чтобы он занял весь экран.
    AppBackground {
        // 2. Создаем Box-контейнер для контента, к которому применяем отступы.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center // Центрируем контент
        ) {
            Crossfade(targetState = uiState.hasEnoughData, label = "analytics_content") { hasData ->
                if (hasData && uiState.lineChartData != null) {
                    AnalyticsChart(lineChartData = uiState.lineChartData!!)
                } else {
                    InfoMessage()
                }
            }
        }
    }
    // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^
}

@Composable
private fun AnalyticsChart(lineChartData: co.yml.charts.ui.linechart.model.LineChartData) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Динамика количества самокатов", style = MaterialTheme.typography.titleMedium, color = StardustTextSecondary)
        Spacer(modifier = Modifier.height(24.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StardustGlassBg)) {
            Column(modifier = Modifier.padding(16.dp)) {
                LineChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    lineChartData = lineChartData
                )
                Spacer(modifier = Modifier.height(16.dp))
                ChartLegend()
            }
        }
    }
}

@Composable
private fun InfoMessage() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
            Icon(imageVector = Icons.Default.Info, contentDescription = "Недостаточно данных", tint = StardustTextSecondary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Недостаточно данных для построения графика. Необходимо как минимум два отчета в истории.", color = StardustTextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ChartLegend() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LegendItem(color = Color(0xFF4CAF50), label = "Готов к вывозу")
        LegendItem(color = Color(0xFF00BCD4), label = "Ожидает тестирования")
        LegendItem(color = Color(0xFF2196F3), label = "На хранении")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = StardustTextPrimary, style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showBackground = true)
@Composable
fun VehicleReportAnalyticsScreenPreview() {
    // Preview теперь использует AppBackground правильно: отступы применяются к контенту внутри.
    AppBackground {
        Box(modifier = Modifier.padding(16.dp)) {
            InfoMessage()
        }
    }
}