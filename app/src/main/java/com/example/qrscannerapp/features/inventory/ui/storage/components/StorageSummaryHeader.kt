package com.example.qrscannerapp.features.inventory.ui.storage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.*

@Composable
fun StorageSummaryHeader(totalCells: Int, totalScooters: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryChip(icon = Icons.Default.Inventory2, value = totalCells.toString(), label = "ячеек", modifier = Modifier.weight(1f))
        SummaryChip(icon = Icons.Default.ElectricScooter, value = totalScooters.toString(), label = "самокатов", modifier = Modifier.weight(1f))
    }
}

@Composable
fun SummaryChip(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), border = BorderStroke(1.dp, StardustPrimary.copy(alpha = 0.2f))) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(StardustPrimary.copy(alpha = 0.08f), StardustGlassBg))).padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(StardustPrimary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = StardustPrimary, modifier = Modifier.size(17.dp))
                }
                Column {
                    Text(value, color = StardustTextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 18.sp)
                    Text(label, color = StardustTextSecondary, fontSize = 11.sp, lineHeight = 11.sp)
                }
            }
        }
    }
}
