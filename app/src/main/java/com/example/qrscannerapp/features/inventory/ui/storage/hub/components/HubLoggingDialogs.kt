package com.example.qrscannerapp.features.inventory.ui.storage.hub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.qrscannerapp.*
import com.example.qrscannerapp.features.inventory.domain.model.HubEntry
import com.example.qrscannerapp.features.inventory.domain.model.HubEntryType
import com.example.qrscannerapp.features.inventory.ui.storage.components.StorageInputField

@Composable
fun HubFrameLoggingDialog(
    onDismiss: () -> Unit,
    onConfirm: (HubEntry) -> Unit
) {
    var scooterId by remember { mutableStateOf("") }
    var oldFrameId by remember { mutableStateOf("") }
    var newFrameId by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StardustModalBg, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text("Замена рамы", color = StardustTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            StorageInputField(value = scooterId, onChange = { scooterId = it }, placeholder = "ID Самоката (напр. HA227T)", icon = Icons.Default.DirectionsBike)
            Spacer(Modifier.height(12.dp))
            StorageInputField(value = oldFrameId, onChange = { oldFrameId = it }, placeholder = "Старая рама", icon = Icons.Default.History)
            Spacer(Modifier.height(12.dp))
            StorageInputField(value = newFrameId, onChange = { newFrameId = it }, placeholder = "Новая рама", icon = Icons.Default.FiberNew)
            Spacer(Modifier.height(12.dp))
            StorageInputField(value = mileage, onChange = { mileage = it }, placeholder = "Пробег", icon = Icons.Default.Speed, keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(12.dp))
            StorageInputField(value = comment, onChange = { comment = it }, placeholder = "Комментарий (необяз.)", icon = Icons.Default.Comment)

            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = StardustGlassBg, contentColor = StardustTextPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Отмена") }
                
                Button(
                    onClick = {
                        onConfirm(HubEntry(
                            type = HubEntryType.FRAME,
                            scooterId = scooterId,
                            oldFrameId = oldFrameId,
                            newFrameId = newFrameId,
                            mileage = mileage.toIntOrNull() ?: 0,
                            comment = comment.ifBlank { null }
                        ))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = scooterId.isNotBlank() && newFrameId.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Сохранить") }
            }
        }
    }
}

@Composable
fun HubIotLoggingDialog(
    onDismiss: () -> Unit,
    onConfirm: (HubEntry) -> Unit
) {
    var scooterId by remember { mutableStateOf("") }
    var oldImei by remember { mutableStateOf("") }
    var newImei by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StardustModalBg, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text("Замена IOT", color = StardustTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            StorageInputField(value = scooterId, onChange = { scooterId = it }, placeholder = "ID Самоката", icon = Icons.Default.DirectionsBike)
            Spacer(Modifier.height(12.dp))
            StorageInputField(value = oldImei, onChange = { oldImei = it }, placeholder = "Старый IMEI", icon = Icons.Default.Memory)
            Spacer(Modifier.height(12.dp))
            StorageInputField(value = newImei, onChange = { newImei = it }, placeholder = "Новый IMEI", icon = Icons.Default.VpnKey)
            Spacer(Modifier.height(12.dp))
            StorageInputField(value = comment, onChange = { comment = it }, placeholder = "Комментарий (необяз.)", icon = Icons.Default.Comment)

            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = StardustGlassBg, contentColor = StardustTextPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Отмена") }
                
                Button(
                    onClick = {
                        onConfirm(HubEntry(
                            type = HubEntryType.IOT,
                            scooterId = scooterId,
                            oldImei = oldImei,
                            newImei = newImei,
                            comment = comment.ifBlank { null }
                        ))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = scooterId.isNotBlank() && newImei.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Сохранить") }
            }
        }
    }
}
