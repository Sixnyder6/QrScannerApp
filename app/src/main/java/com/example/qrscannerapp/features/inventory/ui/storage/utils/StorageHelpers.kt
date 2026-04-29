package com.example.qrscannerapp.features.inventory.ui.storage.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import com.example.qrscannerapp.StardustError
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSecondary
import com.example.qrscannerapp.StardustSuccess
import com.example.qrscannerapp.StardustTextSecondary

fun getRoleLabel(role: String?): String {
    return when (role) {
        "admin" -> "Админ"
        "worker" -> "Работник"
        else -> "Работник"
    }
}

fun getRoleColor(role: String?): Color {
    return when (role) {
        "admin" -> StardustError.copy(alpha = 0.8f)
        else -> StardustPrimary.copy(alpha = 0.7f)
    }
}

fun getOperationVisuals(action: String): Pair<ImageVector, Color> {
    return when (action) {
        "CREATED" -> Icons.Default.AddCircle to StardustSuccess
        "EDITED" -> Icons.Default.Edit to StardustSecondary
        "ITEMS_ADDED", "SCOOTERS_ADDED", "BULK_ADDED" -> Icons.Default.Add to StardustSuccess
        "ITEM_REMOVED" -> Icons.Default.Clear to StardustError
        "DELETED" -> Icons.Default.Delete to StardustError
        else -> Icons.Default.Info to StardustTextSecondary
    }
}

fun getInitials(name: String?): String {
    if (name.isNullOrBlank()) return "?"
    val parts = name.trim().split(" ")
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        else -> parts[0].take(2).uppercase()
    }
}

