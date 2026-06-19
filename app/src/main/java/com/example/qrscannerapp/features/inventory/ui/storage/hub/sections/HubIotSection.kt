package com.example.qrscannerapp.features.inventory.ui.storage.hub.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.features.inventory.domain.model.HubEntry
import com.example.qrscannerapp.features.inventory.domain.model.HubEntryType
import com.example.qrscannerapp.features.inventory.ui.storage.hub.components.HubEntryCard
import com.example.qrscannerapp.features.inventory.ui.storage.hub.components.HubIotLoggingDialog

@Composable
fun HubIotSection(
    history: List<HubEntry>,
    onSaveEntry: (HubEntry) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val iotHistory = remember(history) { history.filter { it.type == HubEntryType.IOT } }

    Box(Modifier.fillMaxSize()) {
        if (iotHistory.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("История замен IOT пуста", color = StardustTextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp)
            ) {
                items(iotHistory) { entry ->
                    HubEntryCard(entry)
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = StardustPrimary
        ) {
            Icon(Icons.Default.Add, null)
        }
    }

    if (showDialog) {
        HubIotLoggingDialog(
            onDismiss = { showDialog = false },
            onConfirm = {
                onSaveEntry(it)
                showDialog = false
            }
        )
    }
}
