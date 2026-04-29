package com.example.qrscannerapp.features.street_doctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SdBg        = Color(0xFF14141A)
private val SdBorder    = Color(0xFF2A2A35)
private val SdPrimary   = Color(0xFF6A5AE0)
private val SdPrimaryDim= Color(0xFF6A5AE0).copy(alpha = 0.13f)
private val SdTextMain  = Color(0xFFF0F0F5)
private val SdTextMuted = Color(0xFF4A4A60)
private val SdTextSec   = Color(0xFF7A7A90)
private val SdSuccess   = Color(0xFF4CAF50)
private val SdDanger    = Color(0xFFF44336)
private val SdDangerDim = Color(0xFFF44336).copy(alpha = 0.1f)

data class SdMenuItem(
    val key: String,
    val label: String,
    val sub: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreetDoctorMenuSheet(
    userName: String,
    userRole: String,
    isOnShift: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    onNavigateToAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val initials = userName
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifBlank { "ТХ" }

    val menuItems = listOf(
        SdMenuItem(
            key   = "warehouse",
            label = "Склад запчастей",
            sub   = "Каталог и списание",
            icon  = Icons.Outlined.Warehouse,
            color = Color(0xFF7E57C2)
        ),
        SdMenuItem(
            key   = "chat",
            label = "Чат",
            sub   = "Общий чат команды",
            icon  = Icons.Outlined.Chat,
            color = Color(0xFF26A69A)
        ),
        SdMenuItem(
            key   = "settings",
            label = "Настройки",
            sub   = "Параметры приложения",
            icon  = Icons.Outlined.Settings,
            color = Color(0xFF78909C)
        ),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SdBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(SdBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Профиль ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .clickable {
                        onDismiss()
                        onNavigateToAccount()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isOnShift) SdSuccess.copy(alpha = 0.15f) else SdPrimaryDim),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initials,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOnShift) SdSuccess else SdPrimary
                    )
                    if (isOnShift) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(SdSuccess)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        userName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SdTextMain
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isOnShift) SdSuccess else SdTextSec)
                        )
                        Text(
                            if (isOnShift) "На смене · $userRole" else "Не в сети · $userRole",
                            fontSize = 12.sp,
                            color = SdTextSec
                        )
                    }
                }
                Icon(
                    Icons.Outlined.ChevronRight,
                    null,
                    tint = SdTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = SdBorder, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // ── Пункты меню ──
            menuItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDismiss()
                            onNavigate(item.key)
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(item.color.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            item.icon,
                            null,
                            tint = item.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            item.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SdTextMain
                        )
                        Text(
                            item.sub,
                            fontSize = 12.sp,
                            color = SdTextMuted
                        )
                    }
                    Icon(
                        Icons.Outlined.ChevronRight,
                        null,
                        tint = SdTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = SdBorder, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // ── Выйти ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onDismiss()
                        onLogout()
                    }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SdDangerDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Logout, null, tint = SdDanger, modifier = Modifier.size(20.dp))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "Выйти из системы",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SdDanger
                    )
                    Text("Завершить сессию", fontSize = 12.sp, color = SdTextMuted)
                }
            }
        }
    }
}