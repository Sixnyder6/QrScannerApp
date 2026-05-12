package com.example.qrscannerapp.common.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.SignalWifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

// ─── Модели ──────────────────────────────────────────────────────────────────

data class ChatNotification(
    val senderName: String,
    val message: String,
    val roomName: String,
    val roomEmoji: String,
    val accentColor: Color,
    val isMention: Boolean = false,
    val id: Long = System.currentTimeMillis()
)

data class SystemAlert(
    val id: String,
    val title: String,
    val message: String,
    val icon: ImageVector,
    val accentColor: Color,
    val autoDismissMs: Long = 6000L
) {
    companion object {
        fun lowBattery(level: Int) = SystemAlert(
            id            = "battery_low",
            title         = "Низкий заряд батареи",
            message       = "Осталось $level% — найди розетку",
            icon          = Icons.Outlined.BatteryAlert,
            accentColor   = Color(0xFFEF4444),
            autoDismissMs = 8000L
        )
        fun highPing(ms: Long) = SystemAlert(
            id            = "ping_high",
            title         = "Высокий пинг",
            message       = "${ms}ms — возможны задержки",
            icon          = Icons.Outlined.NetworkCheck,
            accentColor   = Color(0xFFF59E0B),
            autoDismissMs = 6000L
        )
        fun noConnection() = SystemAlert(
            id            = "no_connection",
            title         = "Нет соединения",
            message       = "Проверь WiFi или мобильный интернет",
            icon          = Icons.Outlined.SignalWifiOff,
            accentColor   = Color(0xFFEF4444),
            autoDismissMs = 0L
        )
    }
}

// ─── ChatNotificationBanner ───────────────────────────────────────────────────

@Composable
fun ChatNotificationBanner(
    notification: ChatNotification?,
    onDismiss: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(notification?.id) {
        if (notification != null) {
            delay(5000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = notification != null,
        enter   = slideInVertically(
            initialOffsetY = { -it },
            animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        ) + fadeIn(tween(200)),
        exit    = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(250)) + fadeOut(tween(200)),
        modifier = modifier
            .fillMaxWidth()
            .zIndex(100f)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        notification?.let { notif ->
            AlertCard(accentColor = notif.accentColor, onClick = onTap, onDismiss = onDismiss) {
                GlowCircle(accentColor = notif.accentColor) {
                    Text(notif.roomEmoji, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text       = notif.senderName,
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                            modifier   = Modifier.weight(1f, fill = false)
                        )
                        if (notif.isMention) MentionBadge()
                        Text(
                            text     = "${notif.roomEmoji} ${notif.roomName}",
                            color    = notif.accentColor.copy(alpha = 0.75f),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                    Text(
                        text       = notif.message,
                        color      = Color.White.copy(alpha = 0.60f),
                        fontSize   = 12.sp,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// ─── SystemAlertBanner ────────────────────────────────────────────────────────

@Composable
fun SystemAlertBanner(
    alert: SystemAlert?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(alert?.id) {
        if (alert != null && alert.autoDismissMs > 0) {
            delay(alert.autoDismissMs)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible  = alert != null,
        enter    = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        ) + fadeIn(tween(200)),
        exit     = slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(250)) + fadeOut(tween(200)),
        modifier = modifier
            .fillMaxWidth()
            .zIndex(99f)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        alert?.let { a ->
            AlertCard(accentColor = a.accentColor, onClick = {}, onDismiss = onDismiss) {
                GlowCircle(accentColor = a.accentColor) {
                    Icon(
                        imageVector = a.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text       = a.title,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp
                    )
                    Text(
                        text       = a.message,
                        color      = Color.White.copy(alpha = 0.60f),
                        fontSize   = 12.sp,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// ─── Общие компоненты ─────────────────────────────────────────────────────────

@Composable
private fun AlertCard(
    accentColor: Color,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable RowScope.() -> Unit   // ← RowScope чтобы weight работал
) {
    val cornerDp = 20.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .drawBehind {
                // Внешний glow под карточкой
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.28f),
                            accentColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.85f),
                        radius = size.width * 0.65f
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.85f),
                    radius = size.width * 0.65f
                )
            }
            .clip(RoundedCornerShape(cornerDp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF12121E), Color(0xFF0A0A15))
                )
            )
            // Gradient border через Compose Path — без native canvas
            .drawWithContent {
                drawContent()
                val r = cornerDp.toPx()
                val sw = 1.5f
                val borderPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left   = sw / 2f,
                            top    = sw / 2f,
                            right  = size.width - sw / 2f,
                            bottom = size.height - sw / 2f,
                            cornerRadius = CornerRadius(r, r)
                        )
                    )
                }
                drawPath(
                    path  = borderPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            accentColor.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.10f)
                        ),
                        start = Offset(0f, 0f),
                        end   = Offset(size.width, size.height)
                    ),
                    style = Stroke(width = sw)
                )
            }
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = {
                content()
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun GlowCircle(
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.55f),
                            accentColor.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        radius = size.maxDimension * 0.90f
                    ),
                    radius = size.maxDimension * 0.90f
                )
            }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.90f),
                        accentColor.copy(alpha = 0.60f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun MentionBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFEF4444).copy(alpha = 0.20f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text       = "@упомянул",
            color      = Color(0xFFEF4444),
            fontSize   = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── ShiftRequestBanner ───────────────────────────────────────────────────────

data class ShiftRequestNotification(
    val employeeName: String,
    val id: Long = System.currentTimeMillis()
)

@Composable
fun ShiftRequestBanner(
    notification: ShiftRequestNotification?,
    onDismiss: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(notification?.id) {
        if (notification != null) {
            delay(8000)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible  = notification != null,
        enter    = slideInVertically(
            initialOffsetY = { -it },
            animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        ) + fadeIn(tween(200)),
        exit     = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(250)) + fadeOut(tween(200)),
        modifier = modifier
            .fillMaxWidth()
            .zIndex(101f)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        notification?.let { notif ->
            AlertCard(accentColor = Color(0xFFF59E0B), onClick = onTap, onDismiss = onDismiss) {
                GlowCircle(accentColor = Color(0xFFF59E0B)) {
                    Text("🔔", fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text       = notif.employeeName,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        text     = "Запрашивает доступ к смене",
                        color    = Color.White.copy(alpha = 0.60f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}