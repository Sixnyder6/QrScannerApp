package com.example.qrscannerapp.features.inventory.ui.storage.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.qrscannerapp.StardustError
import com.example.qrscannerapp.StardustGlassBg
import com.example.qrscannerapp.StardustTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(selectedCount: Int, onCloseClick: () -> Unit, onDeleteClick: () -> Unit) {
    TopAppBar(
        title = { Text("Выбрано: $selectedCount", color = StardustTextPrimary, fontWeight = FontWeight.SemiBold) },
        navigationIcon = { IconButton(onClick = onCloseClick) { Icon(Icons.Default.Close, contentDescription = null, tint = StardustTextPrimary) } },
        actions = { IconButton(onClick = onDeleteClick, enabled = selectedCount > 0) { Icon(Icons.Default.Delete, contentDescription = null, tint = if (selectedCount > 0) StardustError else Color.Gray) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = StardustGlassBg.copy(alpha = 0.9f))
    )
}
