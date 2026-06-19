package com.example.qrscannerapp.features.inventory.ui.storage.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.*
import com.example.qrscannerapp.features.inventory.ui.storage.utils.formatLogTime

@Composable
fun StorageActivityLogFullView(logEntries: List<StorageActivityLogEntry>, isAdmin: Boolean, onClearLogClick: () -> Unit) {
    Column(modifier = Modifier.padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("История операций", fontWeight = FontWeight.Bold, color = StardustTextPrimary, fontSize = 18.sp, modifier = Modifier.weight(1f))
            if (isAdmin) { IconButton(onClick = onClearLogClick) { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = StardustError) } }
        }
        if (logEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("История пуста", color = StardustTextSecondary) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp)) { items(logEntries) { entry -> StorageLogEntryItem(entry = entry) } }
        }
    }
}

@Composable
fun StorageLogEntryItem(entry: StorageActivityLogEntry) {
    val actionText = buildAnnotatedString {
        append("${formatLogTime(entry.timestamp)} ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = StardustError)) { append(entry.userName) }
        append(" ${entry.details}")
    }
    val (icon, color) = remember(entry.action) {
        when (entry.action) {
            "CREATED"       -> Icons.Default.AddCircle to StardustSuccess
            "DELETED"       -> Icons.Default.Delete    to StardustError
            "EDITED"        -> Icons.Default.Edit      to StardustSecondary
            "SCOOTERS_ADDED" -> Icons.Default.Add      to StardustSuccess
            "ITEM_REMOVED"  -> Icons.Default.Clear     to StardustError
            else            -> Icons.Default.Info      to StardustTextSecondary
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = actionText, color = StardustTextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
    }
}
