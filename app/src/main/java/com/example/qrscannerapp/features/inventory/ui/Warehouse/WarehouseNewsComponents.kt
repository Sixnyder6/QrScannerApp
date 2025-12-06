// Полное содержимое для ИСПРАВЛЕННОГО файла WarehouseNewsComponents.kt

package com.example.qrscannerapp.features.inventory.ui.Warehouse.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.qrscannerapp.*
import com.example.qrscannerapp.features.inventory.data.NewsItem
import com.example.qrscannerapp.features.inventory.data.NewsTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsEditSheet(
    newsItem: NewsItem?,
    onSave: (NewsItem) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditMode = newsItem != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf(NewsTag.TODAY) }

    LaunchedEffect(newsItem) {
        if (isEditMode && newsItem != null) {
            title = newsItem.title
            content = newsItem.content
            selectedTag = newsItem.tag
        }
    }

    val isFormValid = title.isNotBlank() && content.isNotBlank()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = StardustGlassBg,
        unfocusedContainerColor = StardustGlassBg,
        disabledContainerColor = StardustGlassBg,
        cursorColor = StardustPrimary,
        focusedBorderColor = StardustPrimary,
        unfocusedBorderColor = Color.Transparent,
        focusedLabelColor = StardustTextSecondary,
        unfocusedLabelColor = StardustTextSecondary,
        // --- ИСПРАВЛЕНИЕ: Добавляем явный цвет для вводимого текста ---
        focusedTextColor = StardustTextPrimary,
        unfocusedTextColor = StardustTextPrimary
        // --- КОНЕЦ ИСПРАВЛЕНИЯ ---
    )


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StardustModalBg
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (isEditMode) "Редактировать новость" else "Новая новость",
                style = MaterialTheme.typography.titleLarge,
                color = StardustTextPrimary,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Заголовок") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Содержание") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text("Категория", color = StardustTextSecondary, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NewsTag.values().forEach { tag ->
                    val isSelected = tag == selectedTag
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTag = tag },
                        label = { Text(tag.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = StardustItemBg,
                            selectedContainerColor = tag.color.copy(alpha = 0.2f),
                            labelColor = StardustTextSecondary,
                            selectedLabelColor = tag.color
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = StardustItemBg,
                            selectedBorderColor = tag.color,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isEditMode && newsItem != null) {
                    OutlinedButton(
                        onClick = {
                            onDelete(newsItem.id)
                            onDismiss()
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Удалить")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Button(
                    onClick = {
                        val finalItem = (newsItem ?: NewsItem()).copy(
                            title = title,
                            content = content,
                            tag = selectedTag
                        )
                        onSave(finalItem)
                        onDismiss()
                    },
                    enabled = isFormValid,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Сохранить", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}