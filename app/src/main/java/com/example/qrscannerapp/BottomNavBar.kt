package com.example.qrscannerapp

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom navigation bar — минимализм Apple-стиль.
 *
 * 4 таба:
 *   1. Сканер   — главная функция, открывается первым
 *   2. Самокаты — Storage screen
 *   3. Настройки
 *   4. Меню     — HomescreenScreen (все остальные разделы)
 *
 * Дизайн:
 *   - Фон: #0A0A0A с тонкой верхней линией разделителя
 *   - Активный таб: белая иконка + белый текст
 *   - Неактивный: серый #636366 (Apple tertiary label)
 *   - Никаких индикаторов, пилюль, подчёркиваний — только цвет
 */

enum class BottomTab {
    SCANNER, STORAGE, WAREHOUSE, MENU
}

private val InactiveColor = Color(0xFF636366)  // Apple tertiary label
private val ActiveColor   = Color(0xFFFFFFFF)
private val BarBg         = Color(0xFF0A0A0A)
private val DividerColor  = Color(0xFF2C2C2E)

private data class TabItem(
    val tab: BottomTab,
    val icon: ImageVector,
    val label: String
)

private val tabs = listOf(
    TabItem(BottomTab.SCANNER,   Icons.Outlined.DocumentScanner, "Сканер"),
    TabItem(BottomTab.STORAGE,   Icons.Outlined.Inventory2,       "Самокаты"),
    TabItem(BottomTab.WAREHOUSE, Icons.Outlined.Warehouse,        "Склад"),
    TabItem(BottomTab.MENU,      Icons.Outlined.GridView,         "Меню")
)

@Composable
fun BottomNavBar(
    currentTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BarBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color       = DividerColor,
                        start       = Offset(0f, 0f),
                        end         = Offset(size.width, 0f),
                        strokeWidth = 0.5f
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEach { item ->
                    val isActive = currentTab == item.tab
                    val color = if (isActive) ActiveColor else InactiveColor

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(item.tab) {
                                detectTapGestures(onTap = { onTabSelected(item.tab) })
                            }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector        = item.icon,
                            contentDescription = item.label,
                            tint               = color,
                            modifier           = Modifier.size(24.dp)
                        )
                        Text(
                            text       = item.label,
                            color      = color,
                            fontSize   = 10.sp,
                            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}