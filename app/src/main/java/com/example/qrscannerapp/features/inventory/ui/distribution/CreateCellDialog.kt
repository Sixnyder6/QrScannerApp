package com.example.qrscannerapp.features.inventory.ui.distribution

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.qrscannerapp.StardustError
import com.example.qrscannerapp.StardustGlassBg
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustModalBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSuccess
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.features.inventory.domain.model.CellGroup
import com.example.qrscannerapp.features.inventory.domain.model.CellType

/**
 * Данные из диалога создания ячейки.
 */
data class CreateCellData(
    val displayName: String?,
    val cellType: CellType?,
    val capacity: Int,
    val address: String?
)

/**
 * Диалог создания новой ячейки.
 * Позволяет задать: название, тип АКБ, ёмкость, адрес.
 */
@Composable
fun CreateCellDialog(
    onDismiss: () -> Unit,
    onCreate: (CreateCellData) -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var selectedCellType by remember { mutableStateOf<CellType?>(null) }
    var capacityText by remember { mutableStateOf("500") }
    var address by remember { mutableStateOf("") }
    var showCapacityError by remember { mutableStateOf(false) }

    val capacity = capacityText.toIntOrNull()
    val isCapacityValid = capacity != null && capacity in 1..9999

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = StardustModalBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // --- Заголовок ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Новая ячейка",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StardustTextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Закрыть",
                            tint = StardustTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- Название ячейки ---
                SectionLabel(icon = Icons.Outlined.Label, text = "Название (необязательно)")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Напр. «Основная FUJIAN»", color = StardustTextSecondary.copy(alpha = 0.4f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = cellTextFieldColors()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // --- Тип АКБ ---
                SectionLabel(icon = Icons.Default.Factory, text = "Тип АКБ")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Ячейка будет принимать только выбранный тип",
                    color = StardustTextSecondary.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // WIND 4.0
                Text(
                    "WIND 4.0",
                    color = StardustTextSecondary.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CellType.entries.filter { it.group == CellGroup.WIND_40 }.forEach { type ->
                        CellTypeChipSelectable(
                            type = type,
                            isSelected = selectedCellType == type,
                            onClick = {
                                selectedCellType = if (selectedCellType == type) null else type
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // WIND 5.0
                Text(
                    "WIND 5.0 / Ninebot",
                    color = StardustTextSecondary.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CellType.entries.filter { it.group == CellGroup.WIND_50 }.forEach { type ->
                        CellTypeChipSelectable(
                            type = type,
                            isSelected = selectedCellType == type,
                            onClick = {
                                selectedCellType = if (selectedCellType == type) null else type
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Индикатор "Универсальная"
                AnimatedVisibility(
                    visible = selectedCellType == null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(
                                StardustTextSecondary.copy(alpha = 0.08f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "Тип не выбран — ячейка будет универсальной",
                            color = StardustTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- Ёмкость ---
                SectionLabel(icon = Icons.Default.Inventory2, text = "Ёмкость (шт.)")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { newValue ->
                        capacityText = newValue.filter { it.isDigit() }.take(4)
                        showCapacityError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("500", color = StardustTextSecondary.copy(alpha = 0.4f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = cellTextFieldColors(),
                    isError = showCapacityError,
                    supportingText = if (showCapacityError) {
                        { Text("Укажите число от 1 до 9999", color = StardustError) }
                    } else null
                )

                // Быстрые кнопки ёмкости
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100, 250, 500, 1000).forEach { preset ->
                        val isActive = capacityText == preset.toString()
                        Surface(
                            onClick = { capacityText = preset.toString() },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isActive) StardustPrimary.copy(alpha = 0.2f) else StardustItemBg,
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    preset.toString(),
                                    color = if (isActive) StardustPrimary else StardustTextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- Адрес ---
                SectionLabel(icon = Icons.Default.LocationOn, text = "Адрес склада (необязательно)")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Напр. «Бестужевская 10»", color = StardustTextSecondary.copy(alpha = 0.4f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = cellTextFieldColors()
                )

                Spacer(modifier = Modifier.height(28.dp))

                // --- Кнопка создания ---
                Button(
                    onClick = {
                        if (!isCapacityValid) {
                            showCapacityError = true
                            return@Button
                        }
                        onCreate(
                            CreateCellData(
                                displayName = displayName.takeIf { it.isNotBlank() },
                                cellType = selectedCellType,
                                capacity = capacity!!,
                                address = address.takeIf { it.isNotBlank() }
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StardustPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "Создать ячейку",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// --- Вспомогательные компоненты ---

@Composable
private fun SectionLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = StardustTextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            color = StardustTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CellTypeChipSelectable(
    type: CellType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = colorForCellType(type)
    val bgColor = if (isSelected) color.copy(alpha = 0.25f) else StardustItemBg
    val borderColor = if (isSelected) color.copy(alpha = 0.6f) else Color.Transparent
    val textColor = if (isSelected) color else StardustTextSecondary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, borderColor) else null,
        modifier = modifier.height(56.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                type.shortLabel,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                type.group.displayName,
                color = textColor.copy(alpha = 0.6f),
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun cellTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = StardustPrimary,
    unfocusedBorderColor = StardustTextSecondary.copy(alpha = 0.2f),
    focusedContainerColor = StardustItemBg.copy(alpha = 0.5f),
    unfocusedContainerColor = StardustItemBg.copy(alpha = 0.3f),
    cursorColor = StardustPrimary,
    focusedTextColor = StardustTextPrimary,
    unfocusedTextColor = StardustTextPrimary
)