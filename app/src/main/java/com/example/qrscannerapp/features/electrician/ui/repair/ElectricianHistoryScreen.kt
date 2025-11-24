// File: features/electrician/ui/repair/ElectricianHistoryScreen.kt

package com.example.qrscannerapp.features.electrician.ui.repair

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.StardustError
import com.example.qrscannerapp.StardustGlassBg
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.features.electrician.domain.model.BatteryRepairLog
import com.example.qrscannerapp.features.electrician.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экран, отображающий историю ремонтов для текущего электрика.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ElectricianHistoryScreen(viewModel: HistoryViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Архитектура уже корректна. AppBackground является корневым элементом.
    AppBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(color = StardustPrimary)
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error!!,
                        color = StardustError,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                uiState.groupedRepairLogs.isEmpty() -> {
                    Text(
                        text = "История ваших ремонтов пуста.",
                        color = StardustTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.groupedRepairLogs.forEach { (date, logsForDate) ->
                            stickyHeader(key = date) {
                                DateHeader(date = date, count = logsForDate.size)
                            }
                            items(items = logsForDate, key = { it.id }) { log ->
                                HistoryLogItem(log = log)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Компонент для отображения заголовка с датой и количеством записей.
 */
@Composable
private fun DateHeader(date: String, count: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg),
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date,
                color = StardustTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$count шт.",
                color = StardustTextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Компонент для отображения одной записи о ремонте в истории.
 */
@Composable
private fun HistoryLogItem(log: BatteryRepairLog) {
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.batteryId,
                    color = StardustTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                if (log.manufacturer.isNotBlank()) {
                    Text(
                        text = log.manufacturer,
                        color = StardustTextPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(StardustItemBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = log.repairs.joinToString(", "),
                color = StardustTextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = StardustGlassBg,
                thickness = 1.dp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Выполнено в ${sdf.format(Date(log.timestamp))}",
                color = StardustTextSecondary.copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}