package com.example.qrscannerapp.features.inventory.ui.storage.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.*
import com.example.qrscannerapp.features.inventory.ui.distribution.getColorByProgress
import com.example.qrscannerapp.features.inventory.ui.storage.utils.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StorageCellTile(
    cell: StorageCell,
    searchQuery: String,
    isSelected: Boolean,
    tileAlpha: Float = 1f,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val progress = if (cell.capacity > 0) cell.items.size.toFloat() / cell.capacity.toFloat() else 0f
    val progressColor = getColorByProgress(progress)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(600), label = "progress")
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected      -> StardustPrimary
            progress >= 1f  -> StardustError.copy(alpha = 0.6f)
            progress >= 0.85f -> StardustSecondary.copy(alpha = 0.5f)
            else            -> StardustPrimary.copy(alpha = 0.08f)
        }, animationSpec = tween(300), label = "border"
    )
    val tileScale by animateFloatAsState(if (isSelected) 0.96f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")
    val matchingItem = remember(searchQuery, cell.items) { if (searchQuery.isNotBlank()) cell.items.firstOrNull { it.contains(searchQuery, ignoreCase = true) } else null }
    val isMatchInText = remember(searchQuery, cell.name, cell.description) { searchQuery.isNotBlank() && (cell.name.contains(searchQuery, ignoreCase = true) || cell.description.contains(searchQuery, ignoreCase = true)) }
    val showMatchingItem = matchingItem != null && !isMatchInText
    val cardBg = when {
        progress >= 1f    -> Brush.verticalGradient(listOf(StardustError.copy(alpha = 0.12f), StardustGlassBg))
        progress >= 0.85f -> Brush.verticalGradient(listOf(StardustSecondary.copy(alpha = 0.08f), StardustGlassBg))
        cell.items.isEmpty() -> Brush.verticalGradient(listOf(StardustGlassBg, StardustGlassBg.copy(alpha = 0.7f)))
        else              -> Brush.verticalGradient(listOf(StardustPrimary.copy(alpha = 0.06f), StardustGlassBg))
    }
    val todayOpsCount = remember(cell.operations) {
        val todayStart = System.currentTimeMillis() - (System.currentTimeMillis() % 86400000)
        cell.operations.count { it.timestamp >= todayStart }
    }
    Card(
        modifier = Modifier.aspectRatio(0.85f).scale(tileScale).graphicsLayer { alpha = tileAlpha }.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(cardBg)) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    HighlightedText(text = cell.name, highlight = searchQuery, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = StardustTextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!isSelected) {
                        val statusIcon = when { progress >= 1f -> Icons.Default.Lock; cell.items.isEmpty() -> Icons.Default.Inbox; else -> null }
                        if (statusIcon != null) Icon(statusIcon, contentDescription = null, tint = if (progress >= 1f) StardustError.copy(alpha = 0.7f) else StardustTextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                HighlightedText(text = cell.description, highlight = searchQuery, fontSize = 11.sp, color = StardustTextSecondary.copy(alpha = 0.7f), maxLines = 1, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(StardustPrimary.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                        Text(getInitials(cell.createdByName), fontSize = 7.sp, fontWeight = FontWeight.Bold, color = StardustPrimary, lineHeight = 7.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(cell.createdByName?.split(" ")?.firstOrNull() ?: "—", fontSize = 10.sp, color = StardustTextSecondary.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 50.dp))
                    Text(" · ${formatRelativeTime(cell.createdAt)}", fontSize = 10.sp, color = StardustTextSecondary.copy(alpha = 0.4f), maxLines = 1)
                }
                if (showMatchingItem) {
                    Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, tint = StardustSuccess, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        HighlightedText(text = matchingItem ?: "", highlight = searchQuery, fontSize = 11.sp, color = StardustSuccess.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                    Text("${cell.items.size}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = progressColor, lineHeight = 28.sp)
                    Text(" / ${cell.capacity}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = StardustTextSecondary, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(StardustItemBg)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedProgress.coerceIn(0f, 1f)).clip(CircleShape).background(Brush.horizontalGradient(listOf(progressColor.copy(alpha = 0.7f), progressColor))))
                }
                if (todayOpsCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = TerminalGreen.copy(alpha = 0.7f), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("$todayOpsCount сегодня", fontSize = 9.sp, color = TerminalGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace)
                    }
                }
            }
            val checkAlpha by animateFloatAsState(if (isSelected) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "check_alpha")
            val checkScale by animateFloatAsState(if (isSelected) 1f else 0.5f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "check_scale")
            Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).size(22.dp).graphicsLayer { alpha = checkAlpha; scaleX = checkScale; scaleY = checkScale }.clip(CircleShape).background(StardustPrimary), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}
