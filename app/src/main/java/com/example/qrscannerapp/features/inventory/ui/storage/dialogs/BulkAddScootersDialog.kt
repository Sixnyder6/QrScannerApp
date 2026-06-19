package com.example.qrscannerapp.features.inventory.ui.storage.dialogs

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.qrscannerapp.StardustGlassBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSuccess
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.StorageCell

@Composable
fun BulkAddScootersDialog(cell: StorageCell, onDismiss: () -> Unit, onAdd: (StorageCell, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val recognizedCount = remember(text) { text.lines().count { it.trim().isNotBlank() } }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val sheetScale by animateFloatAsState(if (visible) 1f else 0.90f, spring(0.75f, 400f), label = "s")
    val sheetAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(220), label = "a")
    fun handleDismiss() { visible = false; onDismiss() }

    Dialog(onDismissRequest = { handleDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f * sheetAlpha)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { handleDismiss() }, contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight().graphicsLayer { scaleX = sheetScale; scaleY = sheetScale; alpha = sheetAlpha }.clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1C1830), Color(0xFF12102A)), Offset(0f, 0f), Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
                .drawBehind { drawRect(color = StardustSuccess.copy(alpha = 0.45f), topLeft = Offset(size.width * 0.15f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 0.7f, 1.5.dp.toPx())) }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) { Box(modifier = Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(StardustTextSecondary.copy(alpha = 0.22f))) }
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(StardustSuccess.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = StardustSuccess, modifier = Modifier.size(20.dp)) }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Добавить в ${cell.name}", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Каждый номер с новой строки", color = StardustTextSecondary, fontSize = 11.sp)
                            }
                        }
                        Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(StardustTextSecondary.copy(alpha = 0.1f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { handleDismiss() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, null, tint = StardustTextSecondary, modifier = Modifier.size(14.dp)) }
                    }
                    HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 22.dp))
                    Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp)).background(StardustGlassBg).padding(14.dp)) {
                            BasicTextField(
                                value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxSize(),
                                textStyle = TextStyle(color = StardustTextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                                cursorBrush = Brush.verticalGradient(listOf(StardustPrimary, StardustPrimary)),
                                decorationBox = { inner -> Box { if (text.isEmpty()) Text("123456\n789012\n...", color = StardustTextSecondary.copy(alpha = 0.3f), fontSize = 14.sp, fontFamily = FontFamily.Monospace); inner() } }
                            )
                        }
                        AnimatedVisibility(visible = recognizedCount > 0, enter = fadeIn(tween(200)) + expandVertically(), exit = fadeOut(tween(150)) + shrinkVertically()) {
                            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(StardustSuccess.copy(alpha = 0.1f)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, null, tint = StardustSuccess, modifier = Modifier.size(16.dp))
                                Text("Распознано: $recognizedCount номеров", color = StardustSuccess, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 22.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp)).background(StardustGlassBg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { handleDismiss() }, contentAlignment = Alignment.Center) { Text("Отмена", color = StardustTextSecondary, fontWeight = FontWeight.SemiBold) }
                        val canAdd = text.isNotBlank()
                        val btnAlpha by animateFloatAsState(if (canAdd) 1f else 0.4f, tween(200), label = "btn")
                        Box(modifier = Modifier.weight(1.6f).height(50.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(StardustSuccess.copy(alpha = btnAlpha), StardustSuccess.copy(alpha = btnAlpha * 0.7f)))).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = canAdd) { if (canAdd) onAdd(cell, text) }, contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Add, null, tint = Color.White.copy(alpha = btnAlpha), modifier = Modifier.size(17.dp))
                                Text("Добавить", color = Color.White.copy(alpha = btnAlpha), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
