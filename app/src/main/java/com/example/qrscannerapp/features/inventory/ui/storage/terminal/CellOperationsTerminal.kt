package com.example.qrscannerapp.features.inventory.ui.storage.terminal

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.CellOperation
import com.example.qrscannerapp.features.inventory.ui.storage.utils.*
import kotlinx.coroutines.delay

@Composable
fun CellOperationsTerminal(operations: List<CellOperation>, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = TerminalBg), border = BorderStroke(1.dp, TerminalGreen.copy(alpha = 0.15f))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().background(TerminalGreen.copy(alpha = 0.08f)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(TerminalRed.copy(alpha = 0.8f)))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(TerminalAmber.copy(alpha = 0.8f)))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(TerminalGreen.copy(alpha = 0.8f)))
                }
                Spacer(Modifier.width(10.dp))
                Text("cell_operations.log", fontSize = 11.sp, color = TerminalGreen.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
                Spacer(Modifier.weight(1f))
                Text("${operations.size} записей", fontSize = 10.sp, color = TerminalDimGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace)
            }
            if (operations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$ cat operations.log", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TerminalGreen.copy(alpha = 0.4f))
                        Spacer(Modifier.height(4.dp))
                        Text("// пусто", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TerminalDimGreen.copy(alpha = 0.3f))
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    itemsIndexed(operations) { index, op -> TerminalOperationLine(operation = op, index = index) }
                }
            }
        }
    }
}

@Composable
private fun TerminalOperationLine(operation: CellOperation, index: Int) {
    val lineColor = when (operation.action) {
        "CREATED" -> TerminalGreen
        "EDITED"  -> TerminalAmber
        "ITEMS_ADDED", "SCOOTERS_ADDED", "BULK_ADDED" -> TerminalGreen
        "ITEM_REMOVED", "DELETED" -> TerminalRed
        else -> TerminalDimGreen
    }
    val opSymbol = when (operation.action) {
        "CREATED" -> "+"; "EDITED" -> "~"
        "ITEMS_ADDED", "SCOOTERS_ADDED", "BULK_ADDED" -> "+"
        "ITEM_REMOVED", "DELETED" -> "-"
        else -> "·"
    }
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 30L); isVisible = true }
    val alpha by animateFloatAsState(if (isVisible) 1f else 0f, tween(220, easing = LinearEasing), label = "term_line_alpha")
    Row(modifier = Modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha }.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(formatLogTimestamp(operation.timestamp), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TerminalDimGreen.copy(alpha = 0.5f), modifier = Modifier.width(80.dp))
        Text(opSymbol, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = lineColor, modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(operation.details, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = lineColor.copy(alpha = 0.9f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(operation.userName, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TerminalDimGreen.copy(alpha = 0.4f))
        }
        if (operation.itemCount > 0) Text("${if (operation.action.contains("REMOVE") || operation.action == "DELETED") "-" else "+"}${operation.itemCount}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = lineColor.copy(alpha = 0.7f))
    }
}
