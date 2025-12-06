// Полное содержимое для ИСПРАВЛЕННОГО файла WarehouseAddItemScreen.kt

package com.example.qrscannerapp.features.inventory.ui.Warehouse // ИСПРАВЛЕНО: Пакет теперь соответствует структуре папок

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.qrscannerapp.*
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.NewItemData
import java.util.UUID

/**
 * Полноценный экран для добавления новой запчасти.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseAddItemScreen(
    existingCategories: List<String>,
    onNavigateUp: () -> Unit,
    onItemCreated: (NewItemData) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var shortName by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var totalStockText by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("шт.") }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var isCreatingNewCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val isFormValid = fullName.isNotBlank() &&
            shortName.isNotBlank() &&
            (if (isCreatingNewCategory) newCategoryName.isNotBlank() else selectedCategory.isNotBlank()) &&
            totalStockText.isNotBlank()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = StardustItemBg,
        unfocusedContainerColor = StardustItemBg,
        disabledContainerColor = StardustItemBg,
        cursorColor = StardustPrimary,
        focusedBorderColor = StardustPrimary,
        unfocusedBorderColor = Color.Transparent,
        focusedLabelColor = StardustTextSecondary,
        unfocusedLabelColor = StardustTextSecondary,
    )

    Scaffold(
        containerColor = Color.Black.copy(alpha = 0.5f),
        topBar = {
            TopAppBar(
                title = { Text("Новая запчасть", color = StardustTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = StardustTextPrimary)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val finalCategory = if (isCreatingNewCategory) newCategoryName else selectedCategory
                            val newItem = NewItemData(
                                fullName = fullName, shortName = shortName,
                                sku = sku.ifBlank { "N/A-${UUID.randomUUID().toString().take(6)}" },
                                category = finalCategory, unit = selectedUnit,
                                totalStock = totalStockText.toIntOrNull() ?: 0
                            )
                            onItemCreated(newItem)
                        },
                        enabled = isFormValid
                    ) {
                        Text("Сохранить", color = if (isFormValid) StardustPrimary else StardustTextSecondary.copy(alpha = 0.5f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            ImagePickerPlaceholder()
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Полное название") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = shortName,
                onValueChange = { shortName = it },
                label = { Text("Короткое название (для плитки)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = sku,
                onValueChange = { sku = it },
                label = { Text("Артикул (SKU)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = isCategoryExpanded,
                onExpandedChange = { isCategoryExpanded = !isCategoryExpanded }
            ) {
                OutlinedTextField(
                    value = if (isCreatingNewCategory) "Создание новой..." else selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Категория") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = textFieldColors
                )
                ExposedDropdownMenu(
                    expanded = isCategoryExpanded,
                    onDismissRequest = { isCategoryExpanded = false },
                    modifier = Modifier.background(StardustGlassBg)
                ) {
                    existingCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category, color = StardustTextPrimary) },
                            onClick = {
                                selectedCategory = category
                                isCreatingNewCategory = false
                                isCategoryExpanded = false
                            }
                        )
                    }
                    HorizontalDivider(color = StardustItemBg)
                    DropdownMenuItem(
                        text = { Text("＋ Новая категория...", color = StardustPrimary) },
                        onClick = {
                            isCreatingNewCategory = true
                            selectedCategory = ""
                            isCategoryExpanded = false
                        }
                    )
                }
            }
            if (isCreatingNewCategory) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Введите название новой категории") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = textFieldColors
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = totalStockText,
                    onValueChange = { totalStockText = it.filter { char -> char.isDigit() } },
                    label = { Text("Общее кол-во") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = textFieldColors
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("Ед. изм.", color = StardustTextSecondary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("шт.", "грамм", "пар").forEach { unit ->
                    val isSelected = selectedUnit == unit
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedUnit = unit },
                        label = { Text(unit) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = StardustItemBg,
                            selectedContainerColor = StardustPrimary.copy(alpha = 0.2f),
                            labelColor = StardustTextSecondary,
                            selectedLabelColor = StardustPrimary
                        ),
                        // V-- ИСПРАВЛЕННЫЙ БЛОК --V
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = StardustItemBg,
                            selectedBorderColor = StardustPrimary,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp,
                            enabled = true, // Добавлен параметр
                            selected = isSelected // Добавлен параметр
                        )
                        // ^-- КОНЕЦ ИСПРАВЛЕННОГО БЛОКА --^
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ImagePickerPlaceholder() {
    val stroke = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    val color = StardustTextSecondary

    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(StardustItemBg)
            .clickable { /* TODO: Логика выбора фото */ },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(color = color, style = stroke, cornerRadius = CornerRadius(18.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AddAPhoto, "Добавить фото", tint = StardustTextSecondary, modifier = Modifier.size(40.dp))
            Text("Нажмите, чтобы добавить фото", color = StardustTextSecondary, fontWeight = FontWeight.SemiBold)
        }
    }
}