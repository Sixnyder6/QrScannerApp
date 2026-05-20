package com.example.qrscannerapp.features.scanner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.StardustGlassBg
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.features.scanner.domain.model.StickerDirection
import com.example.qrscannerapp.features.scanner.domain.model.StickerItem

@Composable
fun NumberModePanel(
    items: List<StickerItem>,
    expandedCode: String?,
    onExpandToggle: (String) -> Unit,
    onDirectionToggle: (String, StickerDirection) -> Unit,
    onRemoveItem: (String) -> Unit,
    onSelectAllDirections: (String) -> Unit,
    onSendToStorage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.code }) { item ->
                StickerCard(
                    item = item,
                    isExpanded = expandedCode == item.code,
                    onExpandToggle = { onExpandToggle(item.code) },
                    onDirectionToggle = { dir -> onDirectionToggle(item.code, dir) },
                    onRemove = { onRemoveItem(item.code) },
                    onSelectAll = { onSelectAllDirections(item.code) }
                )
            }
        }

        Button(
            onClick = onSendToStorage,
            enabled = items.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Отправить на хранение (${items.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun StickerCard(
    item: StickerItem,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onDirectionToggle: (StickerDirection) -> Unit,
    onRemove: () -> Unit,
    onSelectAll: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandToggle)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.code, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (item.directions.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 2.dp)) {
                            item.directions.forEach { dir ->
                                DirectionBadge(dir)
                            }
                        }
                    } else {
                        Text("Направления не выбраны", color = StardustTextSecondary, fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Удалить", tint = StardustTextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(150)) + fadeOut(tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StardustItemBg.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    DirectionSelector(
                        selected = item.directions,
                        onToggle = onDirectionToggle
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onSelectAll,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp), tint = StardustPrimary)
                        Spacer(Modifier.width(4.dp))
                        Text("Все", color = StardustPrimary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DirectionSelector(
    selected: Set<StickerDirection>,
    onToggle: (StickerDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    // Крест из 4 стрелок: верх по центру, лево-право в строке, низ по центру
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DirectionButton(StickerDirection.UP, Icons.Default.ArrowUpward, selected, onToggle)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            DirectionButton(StickerDirection.LEFT, Icons.Default.ArrowBack, selected, onToggle)
            DirectionButton(StickerDirection.RIGHT, Icons.Default.ArrowForward, selected, onToggle)
        }
        DirectionButton(StickerDirection.DOWN, Icons.Default.ArrowDownward, selected, onToggle)
    }
}

@Composable
private fun DirectionButton(
    dir: StickerDirection,
    icon: ImageVector,
    selected: Set<StickerDirection>,
    onToggle: (StickerDirection) -> Unit
) {
    val isSelected = dir in selected
    val bg by animateColorAsState(
        targetValue = if (isSelected) StardustPrimary else StardustGlassBg,
        animationSpec = tween(150),
        label = "dir_bg_$dir"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) Color.White else StardustTextSecondary,
        animationSpec = tween(150),
        label = "dir_tint_$dir"
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable { onToggle(dir) },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = dir.name, tint = iconTint, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun DirectionBadge(dir: StickerDirection, modifier: Modifier = Modifier) {
    val icon = when (dir) {
        StickerDirection.UP -> Icons.Default.ArrowUpward
        StickerDirection.DOWN -> Icons.Default.ArrowDownward
        StickerDirection.LEFT -> Icons.Default.ArrowBack
        StickerDirection.RIGHT -> Icons.Default.ArrowForward
    }
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(StardustPrimary.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = StardustPrimary, modifier = Modifier.size(12.dp))
    }
}