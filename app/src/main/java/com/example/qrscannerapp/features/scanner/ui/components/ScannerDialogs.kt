package com.example.qrscannerapp.features.scanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.qrscannerapp.BatterySearchResult
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustModalBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSuccess
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.features.scanner.domain.model.ScanSession

@Composable
fun SearchResultDialog(
    result: BatterySearchResult,
    onDismiss: () -> Unit,
    onNavigateToPallet: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = StardustModalBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StardustSuccess,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "АКБ Найден!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = StardustTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StardustItemBg, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoRow(label = "Серийный номер", value = result.batteryCode)
                    HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.2f))
                    InfoRow(label = "Тип АКБ", value = result.manufacturer)
                    HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.2f))
                    InfoRow(
                        label = "Палет №",
                        value = result.palletNumber.toString(),
                        valueColor = StardustPrimary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateToPallet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Перейти к палету", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss) {
                    Text("Закрыть", color = StardustTextSecondary)
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = StardustTextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = StardustTextSecondary, fontSize = 14.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun SaveSessionDialog(
    isSaving: Boolean,
    onDismissRequest: () -> Unit,
    onSave: (name: String?) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StardustModalBg)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Сохранить сессию",
                    color = StardustTextPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { newText: String -> text = newText },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название...") },
                    singleLine = true,
                    enabled = !isSaving,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = StardustItemBg,
                        unfocusedContainerColor = StardustItemBg,
                        focusedTextColor = StardustTextPrimary,
                        unfocusedTextColor = StardustTextPrimary,
                        cursorColor = StardustPrimary,
                        focusedIndicatorColor = StardustPrimary,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StardustItemBg)
                    ) {
                        Text("Отмена", color = StardustTextSecondary)
                    }
                    Button(
                        onClick = { onSave(text) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
fun SessionSavedDialog(
    savedSession: ScanSession,
    onDismiss: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onNavigateToHistory,
                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
            ) {
                Text("В историю")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = StardustTextSecondary)
            }
        },
        title = { Text("Сохранено!", color = StardustTextPrimary) },
        text = { Text("Сессия успешно сохранена.", color = StardustTextSecondary) },
        containerColor = StardustModalBg
    )
}