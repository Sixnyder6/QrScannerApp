// File: AccountScreen.kt

package com.example.qrscannerapp

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qrscannerapp.common.ui.AppBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// =================================================================================
// ОСНОВНОЙ ЭКРАН (ТОЧКА ВХОДА)
// =================================================================================

@Composable
fun AccountScreen(authManager: AuthManager) {
    val authState by authManager.authState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(authState.error) {
        authState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authManager.clearError()
        }
    }

    // Архитектура уже корректна. AppBackground является корневым элементом.
    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                authState.isLoading -> {
                    CircularProgressIndicator(
                        color = StardustPrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                authState.isLoggedIn -> {
                    val viewModel: AccountViewModel = viewModel(factory = AccountViewModelFactory(authManager))
                    val uiState by viewModel.uiState.collectAsState()

                    when {
                        uiState.isLoading -> {
                            CircularProgressIndicator(
                                color = StardustPrimary,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        uiState.error != null -> {
                            Text(
                                text = uiState.error!!,
                                color = StardustError,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp)
                            )
                        }
                        else -> {
                            PersonalProfileScreen(
                                viewModel = viewModel,
                                state = uiState,
                                onLogout = { authManager.logout() }
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LoginFormComponent(authManager = authManager)
                    }
                }
            }
        }
    }
}

// =================================================================================
// ЭКРАН ПРОФИЛЯ С УПРАВЛЕНИЕМ СМЕНОЙ
// =================================================================================

@Composable
fun PersonalProfileScreen(
    viewModel: AccountViewModel,
    state: AccountUiState,
    onLogout: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val firstItemTranslationY by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                lazyListState.firstVisibleItemScrollOffset * 0.5f
            } else {
                0f
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ProfileHeader(
                userName = state.userName,
                isShiftActive = state.isShiftActive,
                onStartShift = { viewModel.startShift() },
                onEndShift = { viewModel.endShift() },
                modifier = Modifier.graphicsLayer {
                    translationY = firstItemTranslationY
                }
            )
        }

        item {
            AnimatedVisibility(
                visible = state.isShiftActive,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(500))
            ) {
                ShiftProgressBar(
                    modifier = Modifier.padding(top = 24.dp),
                    shiftStartTime = state.shiftStartTime,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Персональная статистика",
                color = StardustTextSecondary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
        item { AnimatedStatCard(icon = Icons.Default.QrCodeScanner, label = "Всего сканирований", value = state.totalScans.toString(), delay = 0, isNumeric = true) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { AnimatedStatCard(icon = Icons.Default.BarChart, label = "Всего партий", value = state.totalSessions.toString(), delay = 150, isNumeric = true) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { AnimatedStatCard(icon = Icons.Default.CalendarMonth, label = "Дата регистрации", value = state.registrationDate, delay = 300, isNumeric = false) }
        item {
            Spacer(modifier = Modifier.height(32.dp))
            val shape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(shape)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(width = 1.dp, color = StardustItemBg, shape = shape)
                    .clickable(onClick = onLogout),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Выйти", tint = StardustError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выйти", color = StardustError, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =================================================================================
// ОБНОВЛЕННЫЕ КОМПОНЕНТЫ
// =================================================================================

@Composable
fun ProfileHeader(
    userName: String,
    isShiftActive: Boolean,
    onStartShift: () -> Unit,
    onEndShift: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var animatingButton by remember { mutableStateOf<String?>(null) }

    val vibrantPurple = Color(0xFF8E2DE2)
    val deepPurple = Color(0xFF4A00E0)
    val brush = Brush.linearGradient(colors = listOf(vibrantPurple, deepPurple))

    val textStyleWithShadow = TextStyle(
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.25f),
            offset = Offset(2f, 2f),
            blurRadius = 4f
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(brush = brush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(imageVector = Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(60.dp), tint = Color.White)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = userName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, style = textStyleWithShadow)
                Text(text = "Сотрудник", fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f), style = textStyleWithShadow)
            }
            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PressableButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { if (animatingButton == null) { scope.launch { animatingButton = "start"; delay(200); onStartShift(); animatingButton = null } } },
                    text = "Начать смену",
                    icon = Icons.Default.PlayArrow,
                    isManuallyPressed = animatingButton == "start",
                    enabled = !isShiftActive && animatingButton == null,
                    activeColor = Color(0xFF00C853),
                    contentColor = Color.White
                )
                PressableButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { if (animatingButton == null) { scope.launch { animatingButton = "end"; delay(200); onEndShift(); animatingButton = null } } },
                    text = "Завершить смену",
                    icon = Icons.Default.Stop,
                    isManuallyPressed = animatingButton == "end",
                    enabled = isShiftActive && animatingButton == null,
                    activeColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                )
            }
        }
    }
}

@Composable
fun PressableButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isManuallyPressed: Boolean = false,
    activeColor: Color,
    contentColor: Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val showPressedState = (isPressed || isManuallyPressed) && enabled
    val shape = RoundedCornerShape(50)

    val brightCenterColor = activeColor
    val darkEdgeColor = androidx.compose.ui.graphics.lerp(activeColor, Color.Black, 0.4f)
    val pressedCenterColor = androidx.compose.ui.graphics.lerp(activeColor, Color.Black, 0.2f)

    val animatedCenterColor by animateColorAsState(
        targetValue = if (showPressedState) pressedCenterColor else brightCenterColor,
        animationSpec = tween(durationMillis = 150),
        label = "button_color_animation"
    )

    val disabledBackgroundColor = Color.Black.copy(alpha = 0.2f)
    val disabledContentColor = Color.White.copy(alpha = 0.3f)

    val backgroundBrush = if (enabled) {
        Brush.radialGradient(
            colors = listOf(animatedCenterColor, darkEdgeColor),
            radius = 250f
        )
    } else {
        SolidColor(disabledBackgroundColor)
    }

    val currentContentColor = if (enabled) contentColor else disabledContentColor

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = backgroundBrush)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (enabled) 0.2f else 0.1f),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = text, tint = currentContentColor)
            Text(text, color = currentContentColor, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}


@Composable
fun StatCard(icon: ImageVector, label: String, value: String, isNumeric: Boolean) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.2f))
            .border(width = 1.dp, color = StardustItemBg, shape = shape)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowingIcon(icon = icon, contentDescription = label)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, color = StardustTextSecondary, fontSize = 14.sp)
                if (isNumeric) {
                    AnimatedCounter(count = value.toIntOrNull() ?: 0, style = MaterialTheme.typography.titleMedium.copy(color = StardustTextPrimary, fontWeight = FontWeight.SemiBold))
                } else {
                    Text(text = value, color = StardustTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ShiftProgressBar(modifier: Modifier = Modifier, shiftStartTime: Long) {
    val totalShiftDuration = 12 * 60 * 60 * 1000L; var progress by remember { mutableFloatStateOf(0f) }; var elapsedTimeText by remember { mutableStateOf("00:00:00") }
    LaunchedEffect(key1 = shiftStartTime) {
        if (shiftStartTime == 0L) { progress = 0f; elapsedTimeText = "00:00:00"; return@LaunchedEffect }
        while (true) {
            val elapsedMillis = (System.currentTimeMillis() - shiftStartTime).coerceAtLeast(0)
            progress = (elapsedMillis.toFloat() / totalShiftDuration).coerceIn(0f, 1f)
            val hours = elapsedMillis / 3600000; val minutes = (elapsedMillis / 60000) % 60; val seconds = (elapsedMillis / 1000) % 60
            elapsedTimeText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            delay(1000)
        }
    }
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(durationMillis = 1000, easing = LinearEasing), label = "progressAnimation")
    val progressBrush = Brush.linearGradient(colors = listOf(Color(0xFF00C2FF), Color(0xFF8E2DE2)))

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val shape = CircleShape
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(shape)
                .background(Color.Black.copy(alpha = 0.2f))
                .border(1.dp, color = StardustItemBg, shape)
        ) {
            Box(modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(shape)
                .background(progressBrush))
            Row(modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = elapsedTimeText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(2f, 2f), 4f)))
                Text(text = "${(animatedProgress * 100).toInt()}%", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(2f, 2f), 4f)))
            }
        }
        Text(text = "Длительность смены", color = StardustTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun AnimatedStatCard(icon: ImageVector, label: String, value: String, delay: Int, isNumeric: Boolean) {
    var isVisible by remember { mutableStateOf(false) }; LaunchedEffect(Unit) { delay(delay.toLong()); isVisible = true }
    AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))) {
        StatCard(icon = icon, label = label, value = value, isNumeric = isNumeric)
    }
}

@Composable
private fun GlowingIcon(icon: ImageVector, contentDescription: String?) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = Color.White,
        modifier = Modifier
            .size(48.dp)
            .background(StardustPrimary.copy(alpha = 0.1f), CircleShape)
            .padding(12.dp)
    )
}

@Composable
fun AnimatedCounter(count: Int, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current) {
    val animatedCount by animateIntAsState(targetValue = count, animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing), label = "animatedCounter"); Text(text = animatedCount.toString(), modifier = modifier, style = style)
}

@Composable
private fun LoginFormComponent(authManager: AuthManager) {
    var username by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope(); val focusManager = LocalFocusManager.current; val passwordFocusRequester = remember { FocusRequester() }
    Text(text = "Вход в аккаунт", fontSize = 22.sp, textAlign = TextAlign.Center, color = StardustTextPrimary, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(32.dp))
    OutlinedTextField(value = username, onValueChange = { username = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Логин") }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = StardustPrimary, unfocusedBorderColor = StardustItemBg, focusedLabelColor = StardustPrimary, unfocusedLabelColor = StardustTextSecondary, cursorColor = StardustPrimary, focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextPrimary, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier
        .fillMaxWidth()
        .focusRequester(passwordFocusRequester), label = { Text("Пароль") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = StardustPrimary, unfocusedBorderColor = StardustItemBg, focusedLabelColor = StardustPrimary, unfocusedLabelColor = StardustTextSecondary, cursorColor = StardustPrimary, focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextPrimary, focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)))
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = { focusManager.clearFocus(); scope.launch { authManager.login(username.trim(), password.trim()) } }, enabled = username.isNotBlank() && password.isNotBlank(), modifier = Modifier
        .fillMaxWidth()
        .height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) {
        Text("Войти", fontWeight = FontWeight.Bold)
    }
}