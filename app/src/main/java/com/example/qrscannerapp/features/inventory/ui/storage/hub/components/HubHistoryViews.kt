package com.example.qrscannerapp.features.inventory.ui.storage.hub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.*
import com.example.qrscannerapp.features.inventory.domain.model.HubEntry
import com.example.qrscannerapp.features.inventory.domain.model.HubEntryType
import com.example.qrscannerapp.features.inventory.ui.storage.utils.formatHubDate

@Composable
fun HubEntryCard(entry: HubEntry) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (entry.type == HubEntryType.FRAME) Icons.Default.DirectionsBike else Icons.Default.Memory,
                    contentDescription = null,
                    tint = StardustPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(entry.scooterId, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Text(formatHubDate(entry.timestamp), color = StardustTextSecondary, fontSize = 12.sp)
            }
            
            Spacer(Modifier.height(12.dp))
            
            if (entry.type == HubEntryType.FRAME) {
                HubValueRow("Старая рама", entry.oldFrameId ?: "-")
                HubValueRow("Новая рама", entry.newFrameId ?: "-")
                HubValueRow("Пробег", "${entry.mileage ?: 0}")
            } else {
                HubValueRow("Старый IMEI", entry.oldImei ?: "-")
                HubValueRow("Новый IMEI", entry.newImei ?: "-")
            }
            
            if (!entry.comment.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(entry.comment, color = StardustTextSecondary.copy(alpha = 0.7f), fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
            
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = StardustItemBg.copy(alpha = 0.3f))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = StardustTextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(entry.userName, color = StardustTextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun HubValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = StardustTextSecondary, fontSize = 14.sp)
        Text(value, color = StardustTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
