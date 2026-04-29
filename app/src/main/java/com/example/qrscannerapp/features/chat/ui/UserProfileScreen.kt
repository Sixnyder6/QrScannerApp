package com.example.qrscannerapp.features.chat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.qrscannerapp.*

@Composable
fun UserProfileScreen(
    userId: String,
    peerName: String,
    peerRole: String,
    onBack: () -> Unit,
    onWriteDm: () -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currentUserId = viewModel.currentUserId
    val isOwnProfile = userId == currentUserId

    LaunchedEffect(userId) { viewModel.loadProfile(userId, peerName, peerRole) }

    // Пикер фото для своего профиля
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadAvatar(context, it, userId) }
    }

    // Снэкбар при ошибке загрузки
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.avatarUploadError) {
        uiState.avatarUploadError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAvatarError()
        }
    }

    val accentColor = when (peerRole) {
        "admin"             -> Color(0xFFEC407A)
        "inventory_manager" -> Color(0xFF4CAF50)
        "muver"             -> Color(0xFF29B6F6)
        "electrician"       -> Color(0xFFFFCA28)
        "technic"           -> Color(0xFFAB47BC)
        else                -> Color(0xFF78909C)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { _ ->
        Column(
            modifier = Modifier.fillMaxSize().background(StardustSolidBg)
        ) {
            // Топбар
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(
                Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.3f)))
            ))
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A22)).padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = StardustTextSecondary, modifier = Modifier.size(20.dp))
                }
                Text("Профиль", color = StardustTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor, strokeWidth = 2.dp)
                }
            } else {
                val info = uiState.profile ?: UserProfileInfo(displayName = peerName, role = peerRole)
                val displayAccent = when (info.role) {
                    "admin"             -> Color(0xFFEC407A)
                    "inventory_manager" -> Color(0xFF4CAF50)
                    "muver"             -> Color(0xFF29B6F6)
                    "electrician"       -> Color(0xFFFFCA28)
                    "technic"           -> Color(0xFFAB47BC)
                    else                -> Color(0xFF78909C)
                }
                val displayRoleLabel = when (info.role) {
                    "admin"             -> "Админ"
                    "inventory_manager" -> "Кладовщик"
                    "muver"             -> "Мувёр"
                    "electrician"       -> "Электрик"
                    "technic"           -> "Техник"
                    else                -> "Сотрудник"
                }

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Шапка с градиентом
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(displayAccent.copy(alpha = 0.2f), Color.Transparent)))
                            .padding(top = 32.dp, bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {

                            // Аватар — кликабельный если это свой профиль
                            Box(modifier = Modifier.size(90.dp)) {
                                if (!info.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = info.avatarUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(86.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, displayAccent.copy(alpha = 0.5f), CircleShape)
                                            .then(if (isOwnProfile) Modifier.clickable { avatarPickerLauncher.launch("image/*") } else Modifier)
                                    )
                                } else {
                                    val initials = info.displayName.split(" ").take(2)
                                        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                                        .joinToString("").ifBlank { "?" }
                                    Box(
                                        modifier = Modifier
                                            .size(86.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(displayAccent.copy(alpha = 0.4f), displayAccent.copy(alpha = 0.2f))))
                                            .border(2.dp, displayAccent.copy(alpha = 0.5f), CircleShape)
                                            .then(if (isOwnProfile) Modifier.clickable { avatarPickerLauncher.launch("image/*") } else Modifier),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(initials, color = displayAccent, fontSize = if (initials.length > 1) 28.sp else 34.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }

                                // Индикатор онлайн
                                Box(
                                    modifier = Modifier.size(18.dp).align(Alignment.BottomEnd).clip(CircleShape)
                                        .background(StardustSolidBg).padding(2.dp).clip(CircleShape)
                                        .background(if (info.isOnline) Color(0xFF4CAF50) else Color(0xFF4A4A58))
                                )

                                // Иконка камеры поверх аватара если свой профиль
                                if (isOwnProfile) {
                                    if (uiState.isUploadingAvatar) {
                                        Box(
                                            modifier = Modifier.size(86.dp).clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = displayAccent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .align(Alignment.BottomEnd)
                                                .offset(x = (-2).dp, y = (-2).dp)
                                                .clip(CircleShape)
                                                .background(displayAccent)
                                                .clickable { avatarPickerLauncher.launch("image/*") },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            Text(info.displayName, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)

                            Surface(color = displayAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                                Text(displayRoleLabel, color = displayAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                            }

                            ProfileOnlineStatus(isOnline = info.isOnline, lastSeen = info.lastSeen, accentColor = displayAccent)
                        }
                    }

                    // Кнопка написать — только для чужих
                    if (!isOwnProfile) {
                        Button(
                            onClick = onWriteDm,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = displayAccent)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Написать сообщение", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    } else {
                        // Для своего профиля — подсказка что можно нажать на аватар
                        Text(
                            "Нажми на фото чтобы сменить аватар",
                            color = StardustTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                        )
                    }

                    // Карточки с инфо
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (info.bio.isNotBlank()) {
                            ProfileInfoCard(icon = Icons.Default.Info, label = "О себе", value = info.bio, accentColor = displayAccent)
                        }
                        if (info.phone.isNotBlank()) {
                            ProfileInfoCard(icon = Icons.Default.Phone, label = "Телефон", value = info.phone, accentColor = displayAccent)
                        }
                        ProfileInfoCard(icon = Icons.Default.Badge, label = "Должность", value = displayRoleLabel, accentColor = displayAccent)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileOnlineStatus(isOnline: Boolean, lastSeen: Long, accentColor: Color) {
    if (isOnline) {
        val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
        val alpha by infiniteTransition.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "sa")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF4CAF50).copy(alpha = alpha)))
            Text("онлайн", color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    } else if (lastSeen > 0L) {
        val diff = System.currentTimeMillis() - lastSeen
        val timeText = when {
            diff < 60_000L     -> "был(а) только что"
            diff < 3_600_000L  -> "был(а) ${diff / 60_000} мин назад"
            diff < 86_400_000L -> "был(а) ${diff / 3_600_000} ч назад"
            else               -> "был(а) ${diff / 86_400_000} дн назад"
        }
        Text(text = timeText, color = StardustTextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun ProfileInfoCard(icon: ImageVector, label: String, value: String, accentColor: Color) {
    Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, color = StardustTextSecondary, fontSize = 11.sp)
                Text(text = value, color = StardustTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}