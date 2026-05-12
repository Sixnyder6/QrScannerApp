package com.example.qrscannerapp.features.inventory.ui.Warehouse

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.PresenceManager
import com.example.qrscannerapp.TelemetryManager
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.QuantityPickerDialog

// --- ЭКРАН СКЛАДА ВНУТРИ СКАНЕРА ---

@Composable
fun WarehouseScreen(
    viewModel: WarehouseViewModel = hiltViewModel()
) {
    val scannedItem by viewModel.scannedItem.collectAsState()
    val context = LocalContext.current

    val telemetryManager = remember { TelemetryManager(context) }
    val authManager = remember { AuthManager(context, PresenceManager(com.google.firebase.firestore.FirebaseFirestore.getInstance())) { telemetryManager } }
    val authState by authManager.authState.collectAsState()
    val canManage = authState.isAdmin
    val currentUserName = authState.userName ?: "Неизвестный"

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is WarehouseViewModel.UiEvent.ShowSnackbar -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. СОСТОЯНИЕ ПОКОЯ (Заглушка)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 60.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.QrCodeScanner,
                contentDescription = null,
                tint = StardustItemBg.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Наведите камеру\nна штрих-код запчасти",
                color = StardustTextSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Или выберите вкладку\n\"Самокаты\" для поиска техники",
                color = StardustTextSecondary.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }

        // 2. ДИАЛОГ НАЙДЕННОЙ ЗАПЧАСТИ
        if (scannedItem != null) {
            QuantityPickerDialog(
                item = scannedItem!!,
                isAdmin = canManage,
                onDismiss = {
                    viewModel.clearScannedItem()
                },
                // --- ИСПРАВЛЕНИЕ: Заменяем onConfirm на onTake и onAddToCart ---
                onTake = { item, quantity ->
                    // Основное действие при сканировании - "Взять"
                    viewModel.onTakeItem(item, quantity, "Скан: $currentUserName")
                    viewModel.clearScannedItem()
                    Toast.makeText(context, "Взято: $quantity ${item.unit}", Toast.LENGTH_SHORT).show()
                },
                onAddToCart = { item, quantity ->
                    // Дополнительное действие - "В заказ"
                    viewModel.onAddToCart(item, quantity)
                    viewModel.clearScannedItem()
                    Toast.makeText(context, "Добавлено в заказ: $quantity ${item.unit}", Toast.LENGTH_SHORT).show()
                },
                // --- КОНЕЦ ИСПРАВЛЕНИЯ ---
                onEdit = {
                    Toast.makeText(context, "Редактирование доступно в полном каталоге", Toast.LENGTH_SHORT).show()
                },
                onDelete = {
                    Toast.makeText(context, "Удаление доступно в полном каталоге", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}