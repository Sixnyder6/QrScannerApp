// Полное содержимое для ИСПРАВЛЕННОГО файла WarehouseScreen.kt

package com.example.qrscannerapp.features.inventory.ui.Warehouse

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.WarehouseCatalogScreen

// --- ГЛАВНЫЙ КОМПОНЕНТ ЭКРАНА СКЛАДА ВНУТРИ СКАНЕРА ---

@Composable
fun WarehouseScreen(
    viewModel: WarehouseViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    val scannedItem by viewModel.scannedItem.collectAsState()
    val context = LocalContext.current

    val authManager = remember { AuthManager(context) }
    val authState by authManager.authState.collectAsState()

    LaunchedEffect(scannedItem) {
        scannedItem?.let { item ->
            Toast.makeText(context, "Сканер нашел: ${item.shortName}", Toast.LENGTH_SHORT).show()
            viewModel.clearScannedItem()
        }
    }

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

    WarehouseCatalogScreen(
        items = items,
        onNavigateToAddItem = {
            Toast.makeText(context, "Добавление доступно через главное меню", Toast.LENGTH_SHORT).show()
        },
        onTakeItem = { item, quantity ->
            viewModel.onTakeItem(item, quantity, "Сканер (Быстрое действие)")
        },
        isAdmin = authState.isAdmin,
        // --- ИЗМЕНЕНИЕ: Добавляем onNavigateBack и убираем searchQuery ---
        onNavigateBack = {
            // Здесь навигация назад не нужна, так как экран встроенный
        }
    )
}