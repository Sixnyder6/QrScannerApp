package com.example.qrscannerapp.features.vehicle_report.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.features.vehicle_report.data.local.dao.VehicleReportHistoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class AnalyticsUiState(
    val lineChartData: LineChartData? = null,
    val hasEnoughData: Boolean = false
)

@HiltViewModel
class VehicleReportAnalyticsViewModel @Inject constructor(
    historyDao: VehicleReportHistoryDao
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> =
        historyDao.getAllReports()
            .map { reports ->
                if (reports.size < 2) {
                    return@map AnalyticsUiState(hasEnoughData = false)
                }
                val chronologicalReports = reports.reversed()
                val sdf = SimpleDateFormat("dd.MM", Locale.getDefault())
                val pointsReady = mutableListOf<Point>()
                val pointsStorage = mutableListOf<Point>()
                val pointsAwaiting = mutableListOf<Point>()
                val xAxisLabels = mutableListOf<String>()
                chronologicalReports.forEachIndexed { index, report ->
                    val x = index.toFloat()
                    pointsReady.add(Point(x, report.readyForExportCount.toFloat()))
                    pointsStorage.add(Point(x, report.storageCount.toFloat()))
                    pointsAwaiting.add(Point(x, report.awaitingTestingCount.toFloat()))
                    xAxisLabels.add(sdf.format(Date(report.timestamp)))
                }
                val maxCount = chronologicalReports.maxOfOrNull {
                    maxOf(it.readyForExportCount, it.storageCount, it.awaitingTestingCount)
                } ?: 0
                val yAxisMaxRange = if (maxCount == 0) 10 else ((maxCount / 10) + 1) * 10
                val scalingLine = Line(
                    listOf(Point(0f, yAxisMaxRange.toFloat())),
                    LineStyle(color = Color.Transparent)
                )
                val lines = listOf(
                    scalingLine,
                    Line(pointsReady, LineStyle(color = Color(0xFF4CAF50))),
                    Line(pointsAwaiting, LineStyle(color = Color(0xFF00BCD4))),
                    Line(pointsStorage, LineStyle(color = Color(0xFF2196F3)))
                )
                val xAxisData = AxisData.Builder()
                    .axisStepSize(100.dp)
                    .steps(xAxisLabels.size - 1)
                    .labelData { index -> xAxisLabels.getOrElse(index) { "" } }
                    .axisLineColor(StardustTextSecondary)
                    .axisLabelColor(StardustTextSecondary)
                    .build()
                val yAxisData = AxisData.Builder()
                    .steps(5)
                    .labelAndAxisLinePadding(20.dp)
                    .labelData { value -> value.toString() }
                    .axisLineColor(StardustTextSecondary)
                    .axisLabelColor(StardustTextSecondary)
                    .build()
                val lineChartData = LineChartData(
                    linePlotData = LinePlotData(lines = lines),
                    xAxisData = xAxisData,
                    yAxisData = yAxisData,
                    gridLines = GridLines(color = StardustItemBg),
                    backgroundColor = Color.Transparent,
                    paddingTop = 30.dp,
                    bottomPadding = 20.dp,
                    containerPaddingEnd = 15.dp
                )
                AnalyticsUiState(
                    lineChartData = lineChartData,
                    hasEnoughData = true
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = AnalyticsUiState()
            )
}