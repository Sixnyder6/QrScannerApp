package com.example.qrscannerapp.features.inventory.ui.passport

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.qrscannerapp.StardustTextPrimary

@Composable
fun BatteryPassportScreen(
    batteryId: String,
    navController: NavHostController
) {
    // Временная заглушка
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Здесь будет Паспорт для АКБ:\n$batteryId",
            color = StardustTextPrimary,
            textAlign = TextAlign.Center
        )
    }
}