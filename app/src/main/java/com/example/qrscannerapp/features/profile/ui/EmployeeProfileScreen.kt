// File: features/profile/ui/EmployeeProfileScreen.kt

package com.example.qrscannerapp.features.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qrscannerapp.StardustError
import com.example.qrscannerapp.StardustGlassBg
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.features.profile.domain.model.UserActivityLog
import com.example.qrscannerapp.features.profile.domain.model.UserProfile
import com.example.qrscannerapp.features.profile.ui.viewmodel.EmployeeProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EmployeeProfileScreen(
    viewModel: EmployeeProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Архитектура уже корректна. AppBackground является корневым элементом.
    AppBackground {
        // Контент располагается внутри BoxScope, предоставляемого AppBackground.
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = StardustPrimary)
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error!!,
                        color = StardustError,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            ProfileHeaderCard(profile = uiState.userProfile)
                        }

                        item {
                            Text(
                                "Последняя активность и телеметрия",
                                style = MaterialTheme.typography.titleMedium,
                                color = StardustTextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                            )
                        }
                        if (uiState.activityHistory.isEmpty()) {
                            item {
                                Text(
                                    text = "У этого сотрудника еще нет записей об активности.",
                                    color = StardustTextSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            items(uiState.activityHistory, key = { it.id }) { log ->
                                TelemetrySnapshotCard(log = log)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetrySnapshotCard(log: UserActivityLog) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy 'в' HH:mm", Locale.getDefault()) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when(log.activityType) {
                        "SESSION_SAVED" -> Icons.Default.Save
                        "COPY_ALL" -> Icons.Default.ContentCopy
                        else -> Icons.Default.History
                    },
                    contentDescription = "Action Type",
                    tint = StardustPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = when(log.activityType) {
                            "SESSION_SAVED" -> "Сохранение сессии"
                            "COPY_ALL" -> "Копирование списка"
                            else -> "Действие"
                        },
                        color = StardustTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = sdf.format(Date(log.timestamp)),
                        color = StardustTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Text("Активность", fontWeight = FontWeight.SemiBold, color = StardustTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            TelemetryRow(icon = Icons.Default.QrCodeScanner, label = "Количество", value = "${log.itemCount} шт.")
            if (log.manualEntryCount > 0) {
                TelemetryRow(icon = Icons.Default.Edit, label = "Вручную", value = "${log.manualEntryCount} шт.", valueColor = StardustError)
            }
            TelemetryRow(icon = Icons.Default.Timer, label = "Длительность", value = "~${log.durationSeconds} сек.")

            Spacer(modifier = Modifier.height(16.dp))

            Text("Устройство", fontWeight = FontWeight.SemiBold, color = StardustTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            TelemetryRow(icon = Icons.Default.PhoneAndroid, label = "Сеть", value = log.networkState)
            TelemetryRow(icon = Icons.Default.BatteryStd, label = "Батарея", value = "${log.lastBatteryLevel}% ${if(log.isCharging) "(зарядка)" else ""}")
            TelemetryRow(icon = Icons.Default.Memory, label = "RAM", value = log.freeRam)
            TelemetryRow(icon = Icons.Default.Storage, label = "Память", value = log.freeStorage)
            TelemetryRow(icon = Icons.Default.Update, label = "Uptime", value = log.deviceUptime)
        }
    }
}
@Composable
fun TelemetryRow(icon: ImageVector, label: String, value: String, valueColor: Color = StardustTextPrimary) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = StardustTextSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = StardustTextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(text = value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ProfileHeaderCard(profile: UserProfile) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(64.dp),
                    tint = StardustTextPrimary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(profile.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = StardustTextPrimary)
                    Text(profile.role, fontSize = 16.sp, color = StardustTextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = StardustItemBg)
            Spacer(modifier = Modifier.height(12.dp))
            ProfileInfoRow(icon = Icons.Default.AlternateEmail, label = "Логин", value = profile.username)
            ProfileInfoRow(icon = Icons.Default.Info, label = "Последняя версия", value = profile.appVersion)
            ProfileInfoRow(icon = Icons.Default.PhoneAndroid, label = "Последнее устройство", value = profile.deviceInfo)
        }
    }
}
@Composable
fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = StardustTextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = "$label:", color = StardustTextSecondary, fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, color = StardustTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
fun ActivityHistoryItem(log: UserActivityLog) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy 'в' HH:mm", Locale.getDefault()) }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg),
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = sdf.format(Date(log.timestamp)),
                color = StardustTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Скопировано номеров: ${log.itemCount}", color = StardustTextSecondary, fontSize = 14.sp)
                if (log.manualEntryCount > 0) {
                    Text(" (вручную: ${log.manualEntryCount})", color = StardustError, fontSize = 14.sp)
                }
            }
            Text("Затрачено времени: ~${log.durationSeconds} сек.", color = StardustTextSecondary, fontSize = 12.sp)
        }
    }
}