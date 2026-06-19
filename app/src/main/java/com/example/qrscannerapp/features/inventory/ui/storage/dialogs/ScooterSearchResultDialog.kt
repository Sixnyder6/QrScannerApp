package com.example.qrscannerapp.features.inventory.ui.storage.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.qrscannerapp.R
import com.example.qrscannerapp.StardustModalBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSecondary
import com.example.qrscannerapp.StardustSuccess
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.common.ui.AnimatedDialogWrapper

@Composable
fun ScooterSearchResultDialog(scooterNumber: String, locationName: String, lastUser: String, onDismiss: () -> Unit, onNavigate: () -> Unit) {
    AnimatedDialogWrapper(onDismiss = onDismiss) {
        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxWidth().height(460.dp)) {
            Image(painter = painterResource(id = R.drawable.scooter), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(280.dp).align(Alignment.TopCenter).offset(y = 20.dp).zIndex(1f))
            Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = StardustModalBg), modifier = Modifier.fillMaxWidth().height(300.dp).align(Alignment.BottomCenter)) {
                Column(modifier = Modifier.fillMaxSize().padding(top = 90.dp, start = 24.dp, end = 24.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Самокат найден!", style = MaterialTheme.typography.titleMedium, color = StardustSuccess, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(scooterNumber, style = MaterialTheme.typography.headlineLarge, color = StardustTextPrimary, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Place, null, tint = StardustSecondary, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(6.dp)); Text(locationName, color = StardustTextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Person, null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Добавил: $lastUser", color = StardustTextSecondary, fontSize = 14.sp) }
                    }
                    Button(onClick = onNavigate, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary), shape = RoundedCornerShape(14.dp)) { Text("Перейти к месту", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
