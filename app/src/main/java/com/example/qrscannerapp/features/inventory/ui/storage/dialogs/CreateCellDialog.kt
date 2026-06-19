package com.example.qrscannerapp.features.inventory.ui.storage.dialogs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.qrscannerapp.StardustGlassBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.features.inventory.ui.storage.components.StorageInputField

@Composable
fun CreateCellDialog(onDismiss: () -> Unit, onCreate: (description: String, capacity: Int) -> Unit) {
    var description by remember { mutableStateOf("") }
    var capacity    by remember { mutableStateOf("600") }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val sheetScale by animateFloatAsState(if (visible) 1f else 0.90f, spring(0.75f, 400f), label = "s")
    val sheetAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(220), label = "a")
    fun handleDismiss() { visible = false; onDismiss() }

    Dialog(onDismissRequest = { handleDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f * sheetAlpha)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { handleDismiss() }, contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight().graphicsLayer { scaleX = sheetScale; scaleY = sheetScale; alpha = sheetAlpha }.clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1C1830), Color(0xFF12102A)), Offset(0f, 0f), Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
                .drawBehind { drawRect(color = StardustPrimary.copy(alpha = 0.45f), topLeft = Offset(size.width * 0.15f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 0.7f, 1.5.dp.toPx())) }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) { Box(modifier = Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(StardustTextSecondary.copy(alpha = 0.22f))) }
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(StardustPrimary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Inventory2, null, tint = StardustPrimary, modifier = Modifier.size(20.dp)) }
                            Spacer(Modifier.width(12.dp))
                            Text("Новая ячейка", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(StardustTextSecondary.copy(alpha = 0.1f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { handleDismiss() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, null, tint = StardustTextSecondary, modifier = Modifier.size(14.dp)) }
                    }
                    HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 22.dp))
                    Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        StorageInputField(value = description, onChange = { description = it }, placeholder = "Описание ячейки", icon = Icons.Default.Description)
                        StorageInputField(value = capacity, onChange = { val filtered = it.filter { c -> c.isDigit() }; capacity = filtered }, placeholder = "Ёмкость", icon = Icons.Default.Numbers, keyboardType = KeyboardType.Number)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(300, 500, 600, 1000).forEach { preset ->
                                val isActive = capacity == preset.toString()
                                Box(modifier = Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(10.dp)).background(if (isActive) StardustPrimary.copy(alpha = 0.18f) else StardustGlassBg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { capacity = preset.toString() }, contentAlignment = Alignment.Center) {
                                    Text(preset.toString(), color = if (isActive) StardustPrimary else StardustTextSecondary.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 22.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp)).background(StardustGlassBg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { handleDismiss() }, contentAlignment = Alignment.Center) { Text("Отмена", color = StardustTextSecondary, fontWeight = FontWeight.SemiBold) }
                        val canCreate = description.isNotBlank() && capacity.isNotBlank()
                        val btnAlpha by animateFloatAsState(if (canCreate) 1f else 0.4f, tween(200), label = "btn")
                        Box(modifier = Modifier.weight(1.6f).height(50.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(StardustPrimary.copy(alpha = btnAlpha), StardustPrimary.copy(alpha = btnAlpha * 0.7f)))).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = canCreate) { if (canCreate) onCreate(description, capacity.toIntOrNull() ?: 600) }, contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Add, null, tint = Color.White.copy(alpha = btnAlpha), modifier = Modifier.size(17.dp))
                                Text("Создать", color = Color.White.copy(alpha = btnAlpha), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
