package com.example.qrscannerapp.features.homescreen.ui.widgets

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

    // Shimmer: получаем общий таймер из ProvideShimmerPhase (15fps, нет InfiniteTransition)
    val shimmerTime = LocalShimmerPhase.current

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
                    // Загрузка — GPU shimmer-скелетон (angle=0 горизонталь, period=2.2s)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .shimmerSheen(shimmerTime, phaseOffset = 0f,    period = 2.2f, angle = 0f, intensity = 0.32f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .shimmerSheen(shimmerTime, phaseOffset = 0.15f, period = 2.2f, angle = 0f, intensity = 0.28f)
                    )
                }
            }
        }
    }
}