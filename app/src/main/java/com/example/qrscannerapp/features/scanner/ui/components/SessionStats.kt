package com.example.qrscannerapp.features.scanner.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.BatchConfig
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSolidBg
import com.example.qrscannerapp.StardustSuccess
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.StardustWarning
import com.example.qrscannerapp.core.model.ActiveTab

@Composable
fun SessionStatsRow(
    sessionSeconds: Long,
    duplicateCount: Int,
    scanRate: Int,
    isBatchActive: Boolean,
    onBatchClick: () -> Unit
) {
    val timeText = remember(sessionSeconds) {
        if (sessionSeconds == 0L) "--:--"
        else {
            val m = sessionSeconds / 60
            val s = sessionSeconds % 60
            "%02d:%02d".format(m, s)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StardustSolidBg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timer, null, tint = StardustTextSecondary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(timeText, color = StardustTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        if (scanRate > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, null, tint = StardustSuccess.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$scanRate/мин", color = StardustSuccess.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        if (duplicateCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ContentCopy, null, tint = StardustWarning, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$duplicateCount дубл.", color = StardustWarning, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            onClick = onBatchClick,
            color = if (isBatchActive) StardustPrimary.copy(alpha = 0.2f) else StardustItemBg,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Inventory,
                    null,
                    tint = if (isBatchActive) StardustPrimary else StardustTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Партия",
                    color = if (isBatchActive) StardustPrimary else StardustTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isBatchActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }

    HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
}

@Composable
fun BatchProgressBar(
    config: BatchConfig,
    currentCount: Int,
    onCancelBatch: () -> Unit
) {
    val progress = if (config.targetCount > 0) currentCount.toFloat() / config.targetCount else 0f
    val isNearlyDone = currentCount >= config.targetCount - 5 && currentCount < config.targetCount
    val isComplete = currentCount >= config.targetCount

    val progressColor = when {
        isComplete -> StardustSuccess
        isNearlyDone -> StardustWarning
        else -> StardustPrimary
    }

    val infiniteTransition = rememberInfiniteTransition(label = "batch_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulse"
    )

    val typeLabel = when (config.sessionType) {
        ActiveTab.SCOOTERS -> "Самокаты"
        ActiveTab.BATTERIES -> "АКБ WIND 4.0"
        ActiveTab.NEW_BATTERIES -> "АКБ WIND 5.0"
        else -> "Партия"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(progressColor.copy(alpha = 0.15f), Color.Transparent)
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Inventory,
                    null,
                    tint = progressColor.copy(alpha = if (isNearlyDone || isComplete) pulseAlpha else 1f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = typeLabel,
                        color = StardustTextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (isComplete) "✓ Партия готова!" else "$currentCount / ${config.targetCount}",
                        color = progressColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isComplete) {
                    Text(
                        text = "осталось ${config.targetCount - currentCount}",
                        color = StardustTextSecondary,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onCancelBatch, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = progressColor,
            trackColor = StardustItemBg
        )
    }

    HorizontalDivider(color = progressColor.copy(alpha = 0.3f))
}