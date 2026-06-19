package com.example.qrscannerapp.features.inventory.ui.storage.hub.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.qrscannerapp.StardustTextSecondary

@Composable
fun HubListsSection() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.FilterList, null, modifier = Modifier.size(64.dp), tint = StardustTextSecondary.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))
            Text("Модульные списки и фильтры", color = StardustTextSecondary)
            Text("В разработке", color = StardustTextSecondary.copy(alpha = 0.6f))
        }
    }
}
