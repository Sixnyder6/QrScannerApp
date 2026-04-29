package com.example.qrscannerapp.features.chat.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.qrscannerapp.*
import java.text.SimpleDateFormat
import java.util.*

private fun dmRoleColorInbox(role: String): Color = when (role) {
    "admin"             -> Color(0xFFEC407A)
    "inventory_manager" -> Color(0xFF4CAF50)
    "muver"             -> Color(0xFF29B6F6)
    "electrician"       -> Color(0xFFFFCA28)
    "technic"           -> Color(0xFFAB47BC)
    else                -> Color(0xFF78909C)
}

// ============================================================================================
// ЭКРАН СПИСКА ДИАЛОГОВ ЛС
// ============================================================================================

@Composable
fun DirectInboxScreen(
    onBack: () -> Unit,
    onOpenChat: (peerId: String, peerName: String, peerRole: String) -> Unit,
    viewModel: DirectInboxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalUnread = uiState.items.sumOf { it.unreadCount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StardustSolidBg)
    ) {
        // ── Цветная полоска + Топбар ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF29B6F6), Color(0xFF29B6F6).copy(alpha = 0.3f)))
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A22))
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = StardustTextSecondary, modifier = Modifier.size(20.dp))
            }
            Text(
                "Личные сообщения",
                color = StardustTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            if (totalUnread > 0) {
                Badge(containerColor = Color(0xFF29B6F6)) {
                    Text(
                        if (totalUnread > 99) "99+" else totalUnread.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

        // ── Контент ──────────────────────────────────────────────────────────
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF29B6F6), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Загружаем диалоги...", color = StardustTextSecondary, fontSize = 13.sp)
                    }
                }
            }
            uiState.items.isEmpty() -> {
                InboxEmptyState()
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.items, key = { it.conversationId }) { item ->
                        InboxItemRow(
                            item = item,
                            onClick = { onOpenChat(item.peerId, item.peerName, item.peerRole) }
                        )
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.04f),
                            modifier = Modifier.padding(start = 76.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================================
// СТРОКА ДИАЛОГА
// ============================================================================================

@Composable
private fun InboxItemRow(
    item: DirectInboxItem,
    onClick: () -> Unit
) {
    val accentColor = dmRoleColorInbox(item.peerRole)
    val timeStr = item.lastTimestamp?.let { formatInboxTime(it) } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Аватар с онлайн-индикатором
        Box(modifier = Modifier.size(52.dp)) {
            if (!item.peerAvatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.peerAvatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, accentColor.copy(alpha = 0.4f), CircleShape)
                )
            } else {
                val initials = item.peerName.split(" ").take(2)
                    .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                    .joinToString("").ifBlank { "?" }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(accentColor.copy(alpha = 0.35f), accentColor.copy(alpha = 0.15f))
                            )
                        )
                        .border(1.5.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initials,
                        color = accentColor,
                        fontSize = if (initials.length > 1) 16.sp else 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Онлайн индикатор
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(StardustSolidBg)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(if (item.isPeerOnline) Color(0xFF4CAF50) else Color(0xFF4A4A58))
            )
        }

        // Имя + превью
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    item.peerName,
                    color = StardustTextPrimary,
                    fontWeight = if (item.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (item.isPeerOnline) {
                    val infiniteTransition = rememberInfiniteTransition(label = "online_${item.peerId}")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.5f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                        label = "oa"
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = alpha))
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (item.lastSenderName.isNotBlank() && item.lastMessage.isNotBlank())
                    "${item.lastSenderName}: ${item.lastMessage}"
                else item.lastMessage,
                color = if (item.unreadCount > 0) StardustTextPrimary.copy(alpha = 0.85f) else StardustTextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (item.unreadCount > 0) FontWeight.Medium else FontWeight.Normal
            )
        }

        // Время + баджик непрочитанных
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                timeStr,
                color = if (item.unreadCount > 0) accentColor else StardustTextSecondary.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = if (item.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
            )
            if (item.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (item.unreadCount > 9) "9+" else item.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

// ============================================================================================
// ПУСТОЙ ЭКРАН
// ============================================================================================

@Composable
private fun InboxEmptyState() {
    val infiniteTransition = rememberInfiniteTransition(label = "inbox_float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fy"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset(y = floatY.dp)
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF29B6F6).copy(alpha = 0.1f))
                    .border(1.dp, Color(0xFF29B6F6).copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    null,
                    tint = Color(0xFF29B6F6).copy(alpha = 0.5f),
                    modifier = Modifier.size(38.dp)
                )
            }
            Text("Нет личных диалогов", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                "Напишите коллеге из списка участников в чате",
                color = StardustTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

// ============================================================================================
// УТИЛИТА: форматирование времени для инбокса
// ============================================================================================

private fun formatInboxTime(date: Date): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { time = date }
    return when {
        now.get(Calendar.DATE) == then.get(Calendar.DATE) &&
                now.get(Calendar.MONTH) == then.get(Calendar.MONTH) ->
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) ->
            SimpleDateFormat("d MMM", Locale("ru")).format(date)
        else ->
            SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(date)
    }
}