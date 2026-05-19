package com.example.qrscannerapp.features.homescreen.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.features.chat.ui.ChatViewModel
import com.example.qrscannerapp.features.chat.ui.DirectInboxViewModel
import dev.chrisbanes.haze.HazeState

private val ChatAccent = Color(0xFF2DD4BF)
private val AvatarPalette = listOf(
    Color(0xFFA78BFA), Color(0xFFFCA5A5), Color(0xFF4ADE80),
    Color(0xFFFBBF24), Color(0xFF22D3EE)
)

@Composable
fun ChatWidget(
    chatViewModel: ChatViewModel,
    dmInboxViewModel: DirectInboxViewModel,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    sheenPhaseOffset: Float = 0f,
    onTap: () -> Unit
) {
    val totalUnreadChat by chatViewModel.totalUnread.collectAsState()
    val chatPreview     by chatViewModel.lastMessagePreview.collectAsState()
    val dmState         by dmInboxViewModel.uiState.collectAsState()

    val totalDmUnread = remember(dmState.items) { dmState.items.sumOf { it.unreadCount } }
    val totalUnread   = totalUnreadChat + totalDmUnread

    val recentSenders = remember(chatPreview, dmState.items) {
        val dm = dmState.items.filter { it.unreadCount > 0 }.take(3).map { it.peerName }
        val pub = chatPreview.takeIf { it.isNotBlank() }?.substringBefore(":")?.let { listOf(it) } ?: emptyList()
        (dm + pub).distinct().take(3)
    }

    val previewText = when {
        chatPreview.isNotBlank() -> chatPreview
        dmState.items.firstOrNull { it.unreadCount > 0 } != null -> {
            val item = dmState.items.first { it.unreadCount > 0 }
            "${item.peerName}: ${item.lastMessage}"
        }
        else -> "нет новых сообщений"
    }

    WidgetCard(
        accentColor      = ChatAccent,
        hazeState        = hazeState,
        modifier         = modifier,
        glowStrength     = if (totalUnread > 0) 0.85f else 0.3f,
        sheenPhaseOffset = sheenPhaseOffset,
        onClick          = onTap
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ЧАТ", color = ChatAccent.copy(alpha = 0.95f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                AvatarStack(names = recentSenders)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (totalUnread > 0) totalUnread.toString() else "0",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (totalUnread > 0) "новых" else "прочитано",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(text = previewText, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AvatarStack(names: List<String>) {
    if (names.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        names.forEachIndexed { idx, name ->
            val safeIdx = name.hashCode().rem(AvatarPalette.size).let { if (it < 0) it + AvatarPalette.size else it }
            Box(
                modifier = Modifier
                    .offset(x = if (idx == 0) 0.dp else (-6 * idx).dp)
                    .size(20.dp).clip(CircleShape)
                    .background(AvatarPalette[safeIdx])
                    .border(1.5.dp, Color(0xFF02020A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(name.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
    }
}