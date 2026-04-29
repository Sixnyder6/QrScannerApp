package com.example.qrscannerapp

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FieldGreen   = Color(0xFF22C55E)
private val FieldRed     = Color(0xFFEF4444)
private val FieldBlue    = Color(0xFF38BDF8)
private val FieldGray    = Color(0xFF6B7280)
private val FieldPurple  = Color(0xFF7C3AED)
private val FieldAmber   = Color(0xFFF59E0B)
private val FieldCard    = Color(0xFF14141E)
private val FieldBorder  = Color(0xFF2A2A3A)
private val FieldText    = Color(0xFFF3F4F6)
private val FieldMuted   = Color(0xFF8E8E9F)

/**
 * Блок статистики полевого ремонта для роли TECHNIC.
 * Вставляется в PersonalProfileScreen вместо блока сканов.
 */
@Composable
fun TechnicFieldStatsCard(
    stats: FieldRepairStats,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Заголовок секции ──
        Text(
            "Полевой ремонт",
            color = FieldText,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(FieldCard),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = FieldPurple,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
            return
        }

        // ── Сегодня ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(FieldCard)
                .border(1.dp, FieldBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "СЕГОДНЯ",
                color = FieldMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Прогресс-бар
            if (stats.totalToday > 0) {
                val t = stats.totalToday.toFloat()
                val doneF = (stats.doneToday + stats.toStorageToday) / t
                val animProg by animateFloatAsState(doneF, spring(0.8f), label = "prog")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${stats.doneToday + stats.toStorageToday} / ${stats.totalToday}",
                        color = FieldText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "${(doneF * 100).toInt()}%",
                        color = FieldGreen,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Составной прогресс-бар
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                ) {
                    if (stats.doneToday > 0)
                        Box(Modifier.weight(stats.doneToday / t).fillMaxHeight().background(FieldGreen))
                    if (stats.toStorageToday > 0)
                        Box(Modifier.weight(stats.toStorageToday / t).fillMaxHeight().background(FieldRed))
                    if (stats.notFoundToday > 0)
                        Box(Modifier.weight(stats.notFoundToday / t).fillMaxHeight().background(FieldGray))
                    if (stats.inProgressToday > 0)
                        Box(Modifier.weight(stats.inProgressToday / t).fillMaxHeight().background(FieldAmber))
                    val newCnt = stats.totalToday - stats.doneToday - stats.toStorageToday - stats.notFoundToday - stats.inProgressToday
                    if (newCnt > 0)
                        Box(Modifier.weight(newCnt / t).fillMaxHeight().background(FieldBorder))
                }
            } else {
                Text(
                    "Заданий на сегодня нет",
                    color = FieldMuted,
                    fontSize = 14.sp
                )
            }

            // Чипы статусов
            if (stats.totalToday > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (stats.doneToday > 0)
                        FieldChip("✓ ${stats.doneToday} готово", FieldGreen)
                    if (stats.toStorageToday > 0)
                        FieldChip("▲ ${stats.toStorageToday} склад", FieldRed)
                    if (stats.notFoundToday > 0)
                        FieldChip("? ${stats.notFoundToday} не найден", FieldGray)
                    if (stats.inProgressToday > 0)
                        FieldChip("⟳ ${stats.inProgressToday}", FieldAmber)
                }
            }
        }

        // ── За всё время + среднее время ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Всего сделал
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FieldCard)
                    .border(1.dp, FieldBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(FieldGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = FieldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    stats.doneAllTime.toString(),
                    color = FieldText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Завершено всего", color = FieldMuted, fontSize = 11.sp)
            }

            // Среднее время
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FieldCard)
                    .border(1.dp, FieldBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(FieldPurple.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Timer,
                        null,
                        tint = FieldPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (stats.avgMinutesPerScooter > 0)
                        "${stats.avgMinutesPerScooter} мин"
                    else "—",
                    color = FieldText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Среднее на самокат", color = FieldMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun FieldChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}