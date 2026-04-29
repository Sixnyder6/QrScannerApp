package com.example.qrscannerapp.features.scanner.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.ColorByd
import com.example.qrscannerapp.ColorFujian
import com.example.qrscannerapp.ColorWind50
import com.example.qrscannerapp.ColorWind50Old
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSecondary
import com.example.qrscannerapp.StardustSolidBg
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.features.scanner.domain.model.ScanItem
import kotlinx.coroutines.delay

@Composable
fun ScanListItem(
    item: ScanItem,
    isNew: Boolean,
    modifier: Modifier = Modifier,
    onItemShown: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isNew) StardustPrimary.copy(alpha = 0.3f) else StardustSolidBg,
        animationSpec = tween(durationMillis = 500), label = "BgAnim"
    )
    LaunchedEffect(isNew) {
        if (isNew) {
            delay(1500L); onItemShown()
        }
    }

    val badgeInfo: Pair<String, androidx.compose.ui.graphics.Color>? = when {
        item.code.startsWith("5BB") -> "WIND 5.0" to ColorWind50
        item.code.startsWith("SF") -> "WIND 5.0 · SF" to ColorWind50Old
        item.code.startsWith("4BB") -> "WIND 4.0 · FJ" to ColorFujian
        item.code.startsWith("4BZ") -> "WIND 4.0 · BYD" to ColorByd
        else -> null
    }

    Column(modifier = modifier.fillMaxWidth().background(backgroundColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.QrCode2, null, tint = StardustSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.code,
                color = StardustTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            if (badgeInfo != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .background(badgeInfo.second.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeInfo.first,
                        color = badgeInfo.second,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        HorizontalDivider(
            color = StardustItemBg,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun EmptyState(text: String = "Ожидание сканирования...", modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.QrCodeScanner,
                null,
                tint = StardustItemBg,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text, color = StardustTextSecondary, fontSize = 16.sp)
        }
    }
}