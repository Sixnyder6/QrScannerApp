package com.example.qrscannerapp.features.homescreen.ui.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

private val TeamAccent = Color(0xFF2DD4BF)

@Composable
fun TeamOnlineWidget(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    sheenPhaseOffset: Float = 0f,
    onTap: () -> Unit
) {
    var onlineCount by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        while (true) {
            runCatching {
                val snap = db.collection("internal_users").get().await()
                val now  = System.currentTimeMillis()
                onlineCount = snap.documents.count { doc ->
                    val lastSeen: Long = when (val raw = doc.get("lastSeen")) {
                        is Long      -> raw
                        is Number    -> raw.toLong()
                        is Timestamp -> raw.toDate().time
                        else         -> 0L
                    }
                    lastSeen > 0 && (now - lastSeen) < 5 * 60 * 1000L
                }
            }
            delay(60_000)
        }
    }

    // Shimmer sweep — идёт слева направо, 2.2s цикл
    val shimmerTransition = rememberInfiniteTransition(label = "team_shimmer")
    val shimmerX by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue  =  2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )

    WidgetCard(
        accentColor      = TeamAccent,
        hazeState        = hazeState,
        modifier         = modifier,
        glowStrength     = 0.5f,
        sheenPhaseOffset = sheenPhaseOffset,
        onClick          = onTap
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Пульсирующая точка онлайн
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(TeamAccent)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(TeamAccent.copy(alpha = 0.6f), Color.Transparent),
                                radius = size.maxDimension * 1.5f
                            ),
                            radius = size.maxDimension * 1.5f
                        )
                    }
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onlineCount != null) {
                    // Данные загружены — показываем число
                    Text(
                        text       = onlineCount.toString(),
                        color      = Color.White,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text  = "онлайн",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                } else {
                    // Загрузка — shimmer-полоска вместо серого прямоугольника
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .drawWithContent {
                                // Тёмная подложка
                                drawRect(color = Color.White.copy(alpha = 0.06f))
                                // Бегущий блик
                                val centerX = shimmerX * size.width
                                val hw = size.width * 0.5f
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.18f),
                                            Color.White.copy(alpha = 0.32f),
                                            Color.White.copy(alpha = 0.18f),
                                            Color.Transparent
                                        ),
                                        start = Offset(centerX - hw, 0f),
                                        end   = Offset(centerX + hw, 0f)
                                    )
                                )
                            }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .drawWithContent {
                                drawRect(color = Color.White.copy(alpha = 0.06f))
                                val centerX = shimmerX * size.width
                                val hw = size.width * 0.5f
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.18f),
                                            Color.White.copy(alpha = 0.28f),
                                            Color.White.copy(alpha = 0.18f),
                                            Color.Transparent
                                        ),
                                        start = Offset(centerX - hw, 0f),
                                        end   = Offset(centerX + hw, 0f)
                                    )
                                )
                            }
                    )
                }
            }
        }
    }
}