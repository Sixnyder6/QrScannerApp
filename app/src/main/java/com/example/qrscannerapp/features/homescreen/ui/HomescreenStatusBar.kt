package com.example.qrscannerapp.features.homescreen.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Battery3Bar
import androidx.compose.material.icons.outlined.Battery1Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.TelemetryManager
import kotlinx.coroutines.delay

// Акцентные цвета капсул
private val WarehouseAccent = Color(0xFF6A5AE0)
private val WifiAccent      = Color(0xFF2DD4BF)
private val BatteryHigh     = Color(0xFF22D380)
private val BatteryMed      = Color(0xFFFBBF24)
private val BatteryLow      = Color(0xFFF87171)

@Composable
fun HomescreenStatusBar(
    telemetryManager: TelemetryManager,
    warehouseName: String = "Бестужевская 10",
    modifier: Modifier = Modifier
) {
    var battery by remember { mutableStateOf<Int?>(null) }
    var isWifi  by remember { mutableStateOf(true) }
    var network by remember { mutableStateOf("WiFi") }

    LaunchedEffect(Unit) {
        while (true) {
            battery = telemetryManager.getBatteryLevel().takeIf { it >= 0 }
            val net = telemetryManager.getNetworkState()
            isWifi  = net.contains("WiFi", ignoreCase = true)
            network = when {
                net.contains("WiFi", ignoreCase = true) -> "WiFi"
                net.contains("5G",   ignoreCase = true) -> "5G"
                net.contains("4G",   ignoreCase = true) || net.contains("LTE", ignoreCase = true) -> "4G"
                net.contains("3G",   ignoreCase = true) -> "3G"
                else -> "—"
            }
            delay(30_000)
        }
    }

    val batteryColor = when {
        battery == null    -> BatteryHigh
        battery!! >= 50    -> BatteryHigh
        battery!! >= 20    -> BatteryMed
        else               -> BatteryLow
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── СКЛАД-КАПСУЛА ─────────────────────────────────────────────────
        GlowPill(
            text       = warehouseName,
            accentColor = WarehouseAccent,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Warehouse,
                    contentDescription = null,
                    tint = WarehouseAccent.copy(alpha = 0.9f),
                    modifier = Modifier.size(11.dp)
                )
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // ── БАТАРЕЯ-КАПСУЛА ───────────────────────────────────────────
            GlowPill(
                text        = battery?.let { "$it%" } ?: "—%",
                accentColor = batteryColor,
                leadingIcon = {
                    val icon = when {
                        battery == null    -> Icons.Outlined.BatteryFull
                        battery!! >= 50    -> Icons.Outlined.BatteryFull
                        battery!! >= 20    -> Icons.Outlined.Battery3Bar
                        else               -> Icons.Outlined.Battery1Bar
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = batteryColor.copy(alpha = 0.9f),
                        modifier = Modifier.size(11.dp)
                    )
                }
            )

            // ── WIFI-КАПСУЛА ──────────────────────────────────────────────
            GlowPill(
                text        = network,
                accentColor = if (isWifi) WifiAccent else BatteryLow,
                leadingIcon = {
                    Icon(
                        imageVector = if (isWifi) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
                        contentDescription = null,
                        tint = (if (isWifi) WifiAccent else BatteryLow).copy(alpha = 0.9f),
                        modifier = Modifier.size(11.dp)
                    )
                }
            )
        }
    }
}

/**
 * Универсальная капсула с glow-подсветкой.
 * Внешний glow = drawBehind (за пределами clip).
 * Внутри — тёмный фон с лёгким акцентным тинтом + gradient border.
 */
@Composable
private fun GlowPill(
    text: String,
    accentColor: Color,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .drawBehind {
                // Внешний glow под капсулой
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.35f),
                            accentColor.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.85f
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.maxDimension * 0.85f
                )
            }
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.12f),
                        Color(0xFF06060F).copy(alpha = 0.80f)
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(
                width = 0.8.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.60f),
                        accentColor.copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            leadingIcon?.invoke()
            Text(
                text       = text,
                color      = Color.White.copy(alpha = 0.90f),
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
        }
    }
}