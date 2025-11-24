// File: features/electrician/ui/ElectricianProfileScreen.kt

package com.example.qrscannerapp.features.electrician.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.features.electrician.ui.viewmodel.HistoryViewModel
import com.example.qrscannerapp.features.electrician.ui.viewmodel.HistoryViewModelFactory
import com.example.qrscannerapp.StardustGlassBg
import com.example.qrscannerapp.StardustItemBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.common.ui.AppBackground

@Composable
fun ElectricianProfileScreen(
    authManager: AuthManager,
    historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(authManager)),
    onNavigateToHistory: () -> Unit
) {
    val uiState by historyViewModel.uiState.collectAsState()
    val authState by authManager.authState.collectAsState()

    // Архитектура уже корректна. AppBackground является корневым элементом,
    // а отступы применяются к внутреннему Column.
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileHeader(
                userName = authState.userName ?: "Загрузка...",
                registrationDate = uiState.registrationDate
            )
            Spacer(modifier = Modifier.height(24.dp))

            CollapsibleStatsSection(
                title = "Личная статистика",
                stats = listOf(
                    "Всего ремонтов" to uiState.totalRepairs.toString(),
                    "Ремонтов за 30 дней" to uiState.repairsLast30Days.toString(),
                    "Частая поломка" to uiState.mostCommonRepair,
                    "Частый производитель" to uiState.favoriteManufacturer
                ),
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToHistory),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "История ремонтов",
                        tint = StardustTextSecondary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "История ремонтов",
                        color = StardustTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = StardustTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(userName: String, registrationDate: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(StardustPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Аватар",
                tint = StardustPrimary,
                modifier = Modifier.size(50.dp)
            )
        }
        Text(
            text = userName,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = StardustTextPrimary
        )
        Text(
            text = "В системе с: $registrationDate",
            fontSize = 14.sp,
            color = StardustTextSecondary
        )
    }
}

@Composable
private fun CollapsibleStatsSection(
    title: String,
    stats: List<Pair<String, String>>,
    isLoading: Boolean
) {
    var isExpanded by remember { mutableStateOf(true) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "rotation"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StardustGlassBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StardustTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                    tint = StardustTextSecondary,
                    modifier = Modifier.rotate(rotationAngle)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = StardustPrimary)
                    }
                } else {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        stats.forEachIndexed { index, (label, value) ->
                            StatRow(label = label, value = value)
                            if (index < stats.lastIndex) {
                                HorizontalDivider(color = StardustItemBg, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = StardustTextSecondary
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = StardustTextPrimary
        )
    }
}