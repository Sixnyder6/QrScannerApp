package com.example.qrscannerapp.features.street_doctor.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

private val SdBg         = Color(0xFF14141A)
private val SdBorder     = Color(0xFF2A2A35)
private val SdPrimary    = Color(0xFF6A5AE0)
private val SdPrimaryDim = Color(0xFF6A5AE0).copy(alpha = 0.13f)
private val SdTextMuted  = Color(0xFF4A4A60)
private val SdSuccess    = Color(0xFF4CAF50)

enum class StreetDoctorTab {
    SCANNER, TASKS, STORAGE
}

data class SdNavTab(
    val tab: StreetDoctorTab,
    val label: String,
    val icon: ImageVector,
    val iconActive: ImageVector
)

private val NAV_TABS = listOf(
    SdNavTab(StreetDoctorTab.SCANNER, "Сканер",   Icons.Outlined.DocumentScanner, Icons.Filled.DocumentScanner),
    SdNavTab(StreetDoctorTab.TASKS,   "Задания",  Icons.Outlined.TaskAlt,         Icons.Filled.TaskAlt),
    SdNavTab(StreetDoctorTab.STORAGE, "Хранение", Icons.Outlined.Inventory2,      Icons.Filled.Inventory2),
)

@Composable
fun StreetDoctorBottomNav(
    currentTab: StreetDoctorTab,
    userName: String,
    isOnShift: Boolean,
    onTabSelected: (StreetDoctorTab) -> Unit,
    onMenuClick: () -> Unit
) {
    val initials = userName
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifBlank { "ТХ" }

    NavigationBar(
        containerColor = SdBg,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        // Три основных таба
        NAV_TABS.forEach { navTab ->
            val selected = currentTab == navTab.tab
            val iconColor by animateColorAsState(
                targetValue = if (selected) SdPrimary else SdTextMuted,
                animationSpec = spring(),
                label = "iconColor"
            )

            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(navTab.tab) },
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) SdPrimaryDim else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                if (selected) navTab.iconActive else navTab.icon,
                                contentDescription = navTab.label,
                                tint = iconColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(SdPrimary)
                            )
                        }
                    }
                },
                label = {
                    Text(
                        navTab.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = iconColor
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = SdPrimary,
                    unselectedIconColor = SdTextMuted,
                    selectedTextColor   = SdPrimary,
                    unselectedTextColor = SdTextMuted,
                    indicatorColor      = Color.Transparent
                )
            )
        }

        // Кнопка Меню с аватаром
        NavigationBarItem(
            selected = false,
            onClick = onMenuClick,
            icon = {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isOnShift) SdSuccess.copy(alpha = 0.15f)
                            else SdPrimaryDim
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initials,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOnShift) SdSuccess else SdPrimary
                    )
                    if (isOnShift) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SdSuccess)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            },
            label = {
                Text(
                    "Меню",
                    fontSize = 11.sp,
                    color = SdTextMuted
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.Transparent
            )
        )
    }
}