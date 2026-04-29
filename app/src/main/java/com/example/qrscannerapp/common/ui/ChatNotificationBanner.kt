// app/src/main/java/com/example/qrscannerapp/common/ui/ChatNotificationBanner.kt

package com.example.qrscannerapp.common.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.qrscannerapp.StardustModalBg
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import kotlinx.coroutines.delay

data class ChatNotification(
    val senderName: String,
    val message: String,
    val roomName: String,
    val roomEmoji: String,
    val accentColor: Color,
    val isMention: Boolean = false,
    val id: Long = System.currentTimeMillis()
)

@Composable
fun ChatNotificationBanner(
    notification: ChatNotification?,
    onDismiss: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Автоскрытие через 4 секунды
    LaunchedEffect(notification?.id) {
        if (notification != null) {
            delay(4000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically { -it } + fadeIn(tween(250)),
        exit = slideOutVertically { -it } + fadeOut(tween(200)),
        modifier = modifier
            .fillMaxWidth()
            .zIndex(100f)
            .statusBarsPadding()         // правильный отступ под статусбар
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        notification?.let { notif ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()   // ИСПРАВЛЕНО: высота по контенту, не на весь экран
                    .clickable { onTap(); onDismiss() },
                shape = RoundedCornerShape(16.dp),
                color = StardustModalBg.copy(alpha = 0.97f),
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                // Внутренний Row — фиксированная высота, всё компактно
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(start = 0.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Цветная полоска слева — ФИКСИРОВАННАЯ высота, не fillMaxHeight
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(42.dp)   // ИСПРАВЛЕНО: фиксированная высота вместо fillMaxHeight
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (notif.isMention)
                                    Brush.verticalGradient(listOf(Color(0xFFFF6B6B), notif.accentColor))
                                else
                                    Brush.verticalGradient(listOf(notif.accentColor, notif.accentColor.copy(alpha = 0.4f)))
                            )
                    )

                    // Аватар — эмодзи комнаты
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(notif.accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(notif.roomEmoji, fontSize = 17.sp)
                    }

                    // Текст
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                notif.senderName,
                                color = StardustTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (notif.isMention) {
                                Surface(
                                    color = Color(0xFFFF6B6B).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "@упомянул",
                                        color = Color(0xFFFF6B6B),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                "${notif.roomEmoji} ${notif.roomName}",
                                color = notif.accentColor.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                        Text(
                            notif.message,
                            color = StardustTextSecondary,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp
                        )
                    }

                    // Кнопка закрыть
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = StardustTextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}