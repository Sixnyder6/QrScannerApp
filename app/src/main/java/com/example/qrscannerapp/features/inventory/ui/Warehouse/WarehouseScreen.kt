package com.example.qrscannerapp.features.inventory.ui.Warehouse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.EmptyState
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSolidBg
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.features.inventory.domain.model.SparePartItem

// --- 1. ГЛАВНЫЙ КОМПОНЕНТ ЭКРАНА ---

@Composable
fun WarehouseScreen(
    viewModel: WarehouseViewModel = hiltViewModel()
) {
    val parts by viewModel.scannedParts.collectAsState()

    // Используем Box для размещения FAB-кнопки поверх списка
    Box(modifier = Modifier
        .fillMaxSize()
        .background(StardustSolidBg)) {

        if (parts.isEmpty()) {
            EmptyState(text = "Отсканируйте запчасть")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp) // Оставляем место для FAB
            ) {
                items(
                    items = parts,
                    key = { it.id } // Ключ для эффективной перерисовки списка
                ) { part ->
                    SparePartListItem(
                        part = part,
                        onQuantityChange = { newQuantity ->
                            viewModel.updateQuantity(part.code, newQuantity)
                        }
                    )
                }
            }
        }

        // Кнопка очистки списка, как на других экранах
        if (parts.isNotEmpty()) {
            SmallFloatingActionButton(
                onClick = { viewModel.clearList() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                containerColor = StardustItemBg,
                contentColor = StardustTextSecondary
            ) {
                Icon(Icons.Outlined.Delete, "Очистить список")
            }
        }
    }
}


// --- 2. ЭЛЕМЕНТ СПИСКА ДЛЯ ЗАПЧАСТИ ---

@Composable
fun SparePartListItem(
    part: SparePartItem,
    onQuantityChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка слева
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Запчасть",
                tint = StardustTextSecondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Название и код
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = part.name,
                    color = StardustTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = part.code,
                    color = StardustTextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Счетчик количества
            QuantityCounter(
                quantity = part.quantity,
                onQuantityChange = onQuantityChange
            )
        }
        HorizontalDivider(color = StardustItemBg, thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))
    }
}


// --- 3. КОМПОНЕНТ СЧЕТЧИКА КОЛИЧЕСТВА ---

@Composable
fun QuantityCounter(
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Кнопка "-"
        OutlinedIconButton(
            onClick = { onQuantityChange(quantity - 1) },
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(8.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Уменьшить", tint = StardustTextSecondary)
        }

        // Текст с количеством
        Text(
            text = quantity.toString(),
            color = StardustPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.widthIn(min = 28.dp)
        )

        // Кнопка "+"
        OutlinedIconButton(
            onClick = { onQuantityChange(quantity + 1) },
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(8.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Увеличить", tint = StardustTextSecondary)
        }
    }
}