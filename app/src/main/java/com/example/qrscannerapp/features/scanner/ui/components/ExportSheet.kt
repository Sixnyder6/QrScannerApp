package com.example.qrscannerapp.features.scanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustModalBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSecondary
import com.example.qrscannerapp.StardustSuccess
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.core.model.ActiveTab
import com.example.qrscannerapp.features.scanner.domain.model.ScanItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    listToExport: List<ScanItem>,
    sheetState: SheetState,
    activeTab: ActiveTab,
    onDismiss: () -> Unit,
    onCopyAll: (List<ScanItem>) -> Unit,
    onShare: (List<ScanItem>) -> Unit,
    onSaveSession: () -> Unit,
    onSort: () -> Unit,
    onNavigateToPalletDistribution: () -> Unit,
    onNavigateToStorage: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StardustModalBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Экспорт данных",
                color = StardustTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (activeTab == ActiveTab.BATTERIES || activeTab == ActiveTab.NEW_BATTERIES) {
                Button(
                    onClick = onNavigateToPalletDistribution,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StardustSuccess.copy(alpha = 0.3f),
                        contentColor = StardustSuccess
                    )
                ) {
                    Text("Приемка на склад", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (activeTab == ActiveTab.SCOOTERS) {
                Button(
                    onClick = onNavigateToStorage,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StardustSuccess.copy(alpha = 0.3f),
                        contentColor = StardustSuccess
                    )
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = "Хранение")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Отправить на хранение", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onSaveSession,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StardustSecondary)
            ) {
                Text("Сохранить сессию в историю", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onCopyAll(listToExport) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
            ) {
                Text("Копировать в буфер обмена", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onShare(listToExport) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
            ) {
                Text("Поделиться (как текст)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSort,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StardustItemBg)
            ) {
                Icon(Icons.Default.Sort, contentDescription = null, tint = StardustTextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Сортировать список", color = StardustTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}