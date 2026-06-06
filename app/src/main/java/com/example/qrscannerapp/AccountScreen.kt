package com.example.qrscannerapp

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.qrscannerapp.common.ui.AnimatedProgressBar
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.common.ui.DotsLoader
import com.example.qrscannerapp.common.ui.MorphButton
import com.example.qrscannerapp.common.ui.shake
import com.example.qrscannerapp.features.street_doctor.domain.model.FieldRepairStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// =================================================================================
// ПАЛИТРА
// =================================================================================
object CorporateColors {
    val Background    = Color.Transparent
    val CardSurface   = StardustGlassBg
    val CardBorder    = Color(0xFF1E1E2E)
    val TextPrimary   = Color(0xFFF3F4F6)
    val TextSecondary = Color(0xFF6B6B80)
    val AccentGreen   = Color(0xFF10B981)
    val AccentPurple  = Color(0xFF7C3AED)
    val AccentRed     = Color(0xFFEF4444)
    val AccentAmber   = Color(0xFFF59E0B)
}

// =================================================================================
// ГЛАВНЫЙ ЭКРАН
// =================================================================================

@Composable
fun AccountScreen(authManager: AuthManager) {
    val authState  by authManager.authState.collectAsState()
    val context    = LocalContext.current
    var showForceUpdateDialog by remember { mutableStateOf(false) }
    var loginErrorKey by remember { mutableIntStateOf(0) }

    val updateManager: UpdateManager = androidx.hilt.navigation.compose.hiltViewModel()
    val updateState by updateManager.updateState.collectAsState()

    LaunchedEffect(authState.error) {
        authState.error?.let {
            loginErrorKey++
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authManager.clearError()
        }
    }
    LaunchedEffect(authState.versionError) {
        if (authState.versionError) { showForceUpdateDialog = true; updateManager.checkForUpdates() }
    }
    LaunchedEffect(updateState) {
        if (updateState is UpdateState.ReadyToInstall) {
            updateManager.installApk((updateState as UpdateState.ReadyToInstall).uri); updateManager.resetState()
        }
    }

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                authState.isLoading -> CircularProgressIndicator(color = CorporateColors.AccentPurple, modifier = Modifier.align(Alignment.Center))
                authState.isLoggedIn && !authState.versionError -> {
                    val viewModel: AccountViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    val uiState by viewModel.uiState.collectAsState()
                    when {
                        uiState.isLoading && uiState.userName == "Загрузка..." ->
                            CircularProgressIndicator(color = CorporateColors.AccentPurple, modifier = Modifier.align(Alignment.Center))
                        uiState.error != null ->
                            Text(uiState.error!!, color = CorporateColors.AccentRed, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(16.dp))
                        else ->
                            PersonalProfileScreen(viewModel = viewModel, state = uiState, authManager = authManager)
                    }
                }
                !authState.isLoggedIn && !authState.versionError -> {
                    LoginScreen(authManager = authManager, loginErrorKey = loginErrorKey)
                }
            }
        }
    } // AppBackground

    if (showForceUpdateDialog) {
        ForceUpdateDialog(
            message     = authState.error ?: "Ваша версия устарела. Обновитесь.",
            updateState = updateState,
            onUpdate    = { val info = (updateState as? UpdateState.UpdateAvailable)?.info; if (info != null) updateManager.startUpdate(info) else updateManager.startCheckForUpdates() },
            onExit      = { android.os.Process.killProcess(android.os.Process.myPid()) }
        )
    }
}

// =================================================================================
// ЭКРАН ВХОДА — iOS стиль
// =================================================================================

@Composable
private fun LoginScreen(authManager: AuthManager, loginErrorKey: Int) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }

    var shakeTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(loginErrorKey) { if (loginErrorKey > 0) shakeTrigger = !shakeTrigger }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        // Фоновое свечение
        Box(
            modifier = Modifier.size(300.dp).align(Alignment.Center).blur(80.dp)
                .background(CorporateColors.AccentPurple.copy(alpha = 0.12f), CircleShape)
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp).shake(trigger = shakeTrigger),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Иконка
            Box(
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(CorporateColors.AccentPurple.copy(alpha = 0.3f), CorporateColors.AccentPurple.copy(alpha = 0.1f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, null, tint = CorporateColors.AccentPurple, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Вход в систему", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CorporateColors.TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text("Stardust Field Operations", fontSize = 13.sp, color = CorporateColors.TextSecondary)
            Spacer(Modifier.height(32.dp))

            // Поле email
            AccountInputField(
                value       = email,
                onChange    = { email = it },
                placeholder = "Email",
                icon        = Icons.Default.Email,
                onNext      = { passwordFocusRequester.requestFocus() }
            )
            Spacer(Modifier.height(10.dp))

            // Поле пароля
            AccountInputField(
                value        = password,
                onChange     = { password = it },
                placeholder  = "Пароль",
                icon         = Icons.Default.Lock,
                isPassword   = true,
                showPassword = showPass,
                onTogglePass = { showPass = !showPass },
                focusRequester = passwordFocusRequester,
                onDone       = { focusManager.clearFocus() }
            )

            Spacer(Modifier.height(24.dp))

            // Кнопка войти
            val canLogin = email.isNotBlank() && password.isNotBlank()
            val btnAlpha by animateFloatAsState(if (canLogin) 1f else 0.45f, tween(200), label = "btn")
            Box(
                modifier = Modifier.fillMaxWidth().height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(CorporateColors.AccentPurple.copy(alpha = btnAlpha), CorporateColors.AccentPurple.copy(alpha = btnAlpha * 0.7f))
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = canLogin
                    ) { focusManager.clearFocus(); scope.launch { authManager.login(email.trim(), password.trim()) } },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Login, null, tint = Color.White.copy(alpha = btnAlpha), modifier = Modifier.size(18.dp))
                    Text("Войти", color = Color.White.copy(alpha = btnAlpha), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun AccountInputField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePass: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    onNext: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        if (focused) CorporateColors.AccentPurple.copy(alpha = 0.7f) else Color.Transparent,
        tween(180), label = "border"
    )
    val iconColor by animateColorAsState(
        if (focused) CorporateColors.AccentPurple else CorporateColors.TextSecondary.copy(alpha = 0.5f),
        tween(180), label = "icon"
    )

    var mod = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp))
        .background(StardustGlassBg)
        .drawBehind {
            drawRoundRect(
                color        = borderColor,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                style        = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
            )
        }

    Box(modifier = mod) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(10.dp))
            var fMod = Modifier.weight(1f).onFocusChanged { focused = it.isFocused }
            if (focusRequester != null) fMod = fMod.then(Modifier.focusRequester(focusRequester))

            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                modifier = fMod,
                textStyle = TextStyle(color = CorporateColors.TextPrimary, fontSize = 15.sp),
                visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
                    imeAction    = if (onNext != null) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onNext?.invoke() },
                    onDone = { onDone?.invoke() }
                ),
                cursorBrush = Brush.verticalGradient(listOf(CorporateColors.AccentPurple, CorporateColors.AccentPurple)),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) Text(placeholder, color = CorporateColors.TextSecondary.copy(alpha = 0.4f), fontSize = 15.sp)
                        inner()
                    }
                }
            )
            if (isPassword && onTogglePass != null) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTogglePass() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null, tint = CorporateColors.TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

// =================================================================================
// ПРОФИЛЬ
// =================================================================================

@Composable
fun PersonalProfileScreen(viewModel: AccountViewModel, state: AccountUiState, authManager: AuthManager) {
    val photoUrl = state.photoUrl
    val scope = rememberCoroutineScope()
    val isTechnic = state.userRoleEnum == UserRole.TECHNIC
    val context = LocalContext.current

    var showEndShiftDialog by remember { mutableStateOf(false) }
    var showLogoutDialog   by remember { mutableStateOf(false) }
    var showHistorySheet   by remember { mutableStateOf(false) }

    // ImagePicker для аватара
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadPhoto(context, it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Градиентный фон за шапкой ────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CorporateColors.AccentPurple.copy(alpha = 0.22f),
                            CorporateColors.AccentPurple.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Боковое свечение
        Box(
            modifier = Modifier.size(200.dp).align(Alignment.TopStart).offset((-40).dp, (-40).dp).blur(60.dp)
                .background(CorporateColors.AccentPurple.copy(alpha = 0.15f), CircleShape)
        )

        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ProfileHeader(userName = state.userName, userRole = state.userRole, isShiftActive = state.isShiftActive, photoUrl = photoUrl, onStartShift = { viewModel.startShift() }, onEndShift = { showEndShiftDialog = true }, onPhotoClick = { imagePickerLauncher.launch("image/*") }) }

            item {
                AnimatedVisibility(visible = state.isShiftActive, enter = fadeIn(tween(300)) + expandVertically(tween(500)), exit = fadeOut(tween(300)) + shrinkVertically(tween(500))) {
                    ShiftProgressBar(shiftStartTime = state.shiftStartTime)
                }
            }

            item {
                if (isTechnic) {
                    TechnicFieldStatsCard(stats = state.fieldRepairStats, isLoading = state.fieldRepairLoading)
                } else {
                    AnimatedVisibility(visible = state.isShiftActive, enter = fadeIn(tween(400)) + expandVertically(tween(500)), exit = fadeOut(tween(300)) + shrinkVertically(tween(400))) {
                        TodayStatsCard(scansToday = state.scansToday, sessionsToday = state.sessionsToday)
                    }
                }
            }

            if (!isTechnic) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StreakCard(streakDays = state.streakDays, modifier = Modifier.weight(1f))
                        RecordCard(record = state.personalRecord, modifier = Modifier.weight(1f))
                    }
                }
                item { WeeklyChartCard(weeklyScans = state.weeklyScans) }
            }

            // Журнал смен
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(StardustGlassBg)
                        .drawBehind {
                            drawRoundRect(
                                color = CorporateColors.CardBorder,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
                            )
                        }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showHistorySheet = true }
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(CorporateColors.AccentPurple.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.History, null, tint = CorporateColors.AccentPurple, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Журнал смен", color = CorporateColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = CorporateColors.TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Общая статистика
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Общая статистика", color = CorporateColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.dp, bottom = 10.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .background(StardustGlassBg)
                            .drawBehind {
                                drawRoundRect(color = CorporateColors.CardBorder, cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        if (isTechnic) {
                            StatListItem(Icons.Default.TwoWheeler, "Самокатов завершено", state.fieldRepairStats.doneAllTime.toString(), Color(0xFF22C55E))
                            StatDivider()
                            StatListItem(Icons.Default.List, "Всего заданий", state.fieldRepairStats.totalAllTime.toString(), CorporateColors.AccentPurple)
                            StatDivider()
                            StatListItem(Icons.Default.Timer, "Среднее время", if (state.fieldRepairStats.avgMinutesPerScooter > 0) "${state.fieldRepairStats.avgMinutesPerScooter} мин" else "—", CorporateColors.AccentAmber)
                            StatDivider()
                            StatListItem(Icons.Outlined.Event, "В системе с", state.registrationDate, CorporateColors.TextSecondary)
                        } else {
                            StatListItem(Icons.Outlined.QrCodeScanner, "Всего сканирований", state.totalScans.toString(), CorporateColors.AccentPurple)
                            StatDivider()
                            StatListItem(Icons.Outlined.Inventory2, "Обработано партий", state.totalSessions.toString(), CorporateColors.AccentPurple)
                            StatDivider()
                            StatListItem(Icons.Outlined.Event, "В системе с", state.registrationDate, CorporateColors.TextSecondary)
                        }
                    }
                }
            }

            // Выход
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(CorporateColors.AccentRed.copy(alpha = 0.08f))
                        .drawBehind {
                            drawRoundRect(color = CorporateColors.AccentRed.copy(alpha = 0.25f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                        }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showLogoutDialog = true }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = CorporateColors.AccentRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Выйти из системы", color = CorporateColors.AccentRed, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    if (showHistorySheet) {
        ShiftHistoryBottomSheet(shifts = state.shiftHistory, onDismiss = { showHistorySheet = false })
    }
    if (showEndShiftDialog) {
        AccountConfirmDialog(
            title       = "Завершение смены",
            message     = "Статистика работы будет сохранена. Вы уверены?",
            confirmText = "Завершить",
            accentColor = CorporateColors.AccentRed,
            onDismiss   = { showEndShiftDialog = false },
            onConfirm   = { showEndShiftDialog = false; viewModel.endShift() }
        )
    }
    if (showLogoutDialog) {
        AccountConfirmDialog(
            title       = "Выход из аккаунта",
            message     = if (state.isShiftActive) "У вас активная смена — она будет завершена автоматически. Продолжить?" else "Выйти из корпоративной системы?",
            confirmText = "Выйти",
            accentColor = CorporateColors.AccentRed,
            onDismiss   = { showLogoutDialog = false },
            onConfirm   = { showLogoutDialog = false; scope.launch { if (state.isShiftActive) viewModel.endShiftOnLogout(); authManager.logout() } }
        )
    }
}

// =================================================================================
// ШАПКА ПРОФИЛЯ
// =================================================================================

@Composable
fun ProfileHeader(userName: String, userRole: String, isShiftActive: Boolean, photoUrl: String?, onStartShift: () -> Unit, onEndShift: () -> Unit, onPhotoClick: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    var isShiftLoading by remember { mutableStateOf(false) }
    LaunchedEffect(isShiftActive) { isShiftLoading = false }
    val hasPhoto = photoUrl != null

    // Пульс онлайн-индикатора
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        "pulse"
    )

    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(StardustGlassBg)
            .drawBehind {
                // Цветная линия сверху
                drawRect(
                    brush = Brush.horizontalGradient(listOf(Color.Transparent, CorporateColors.AccentPurple.copy(alpha = 0.7f), CorporateColors.AccentPurple.copy(alpha = 0.3f), Color.Transparent)),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width, 1.5.dp.toPx())
                )
                drawRoundRect(color = CorporateColors.CardBorder, cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Аватар
            Box(modifier = Modifier.size(72.dp)) {
                Box(
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                        .background(Color.Transparent)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.sweepGradient(listOf(CorporateColors.AccentPurple.copy(alpha = 0.6f), CorporateColors.AccentPurple.copy(alpha = 0.1f), CorporateColors.AccentPurple.copy(alpha = 0.6f))),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
                            )
                        }
                ) {
                    if (photoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(photoUrl).crossfade(true).build(),
                            contentDescription = "Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(36.dp).align(Alignment.Center), tint = CorporateColors.TextSecondary)
                    }
                }

                // Overlay: кнопка выбора фото (поверх аватара)
                if (onPhotoClick != null) {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                            .background(if (hasPhoto) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.25f))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPhotoClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Сменить фото",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(if (hasPhoto) 20.dp else 22.dp)
                        )
                    }
                }

                // Онлайн точка
                Box(
                    modifier = Modifier.size(16.dp).align(Alignment.BottomEnd)
                        .clip(CircleShape).background(StardustGlassBg).padding(2.dp)
                        .clip(CircleShape).background(if (isShiftActive) CorporateColors.AccentGreen.copy(alpha = pulseAlpha) else Color(0xFF3A3A4A))
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(userName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorporateColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CorporateColors.AccentPurple.copy(alpha = 0.12f)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                        Text(userRole, color = CorporateColors.AccentPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (isShiftActive) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CorporateColors.AccentGreen.copy(alpha = 0.1f)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                            Text("На смене", color = CorporateColors.AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = CorporateColors.AccentGreen, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Бестужевская 10", fontSize = 12.sp, color = CorporateColors.TextSecondary)
                }
                // Подпись «Изменить фото», если фото уже есть
                if (hasPhoto && onPhotoClick != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Изменить фото",
                        color = CorporateColors.AccentPurple.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPhotoClick() }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        if (!isShiftActive) {
            Box(
                modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(CorporateColors.AccentGreen, CorporateColors.AccentGreen.copy(alpha = 0.7f))))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { isShiftLoading = true; onStartShift() },
                contentAlignment = Alignment.Center
            ) {
                if (isShiftLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("Начать смену", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp))
                    .background(CorporateColors.AccentRed.copy(alpha = 0.1f))
                    .drawBehind { drawRoundRect(color = CorporateColors.AccentRed.copy(alpha = 0.3f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())) }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onEndShift() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Stop, null, tint = CorporateColors.AccentRed, modifier = Modifier.size(20.dp))
                    Text("Завершить смену", color = CorporateColors.AccentRed, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}

// =================================================================================
// ПРОГРЕСС СМЕНЫ
// =================================================================================

@Composable
fun ShiftProgressBar(modifier: Modifier = Modifier, shiftStartTime: Long) {
    val totalShiftDuration = 12 * 60 * 60 * 1000L
    var progress by remember { mutableFloatStateOf(0f) }
    var elapsedTimeText by remember { mutableStateOf("00:00:00") }

    LaunchedEffect(shiftStartTime) {
        if (shiftStartTime == 0L) { progress = 0f; elapsedTimeText = "00:00:00"; return@LaunchedEffect }
        while (true) {
            val elapsed = (System.currentTimeMillis() - shiftStartTime).coerceAtLeast(0)
            progress = (elapsed.toFloat() / totalShiftDuration).coerceIn(0f, 1f)
            val h = elapsed / 3600000; val m = (elapsed / 60000) % 60; val s = (elapsed / 1000) % 60
            elapsedTimeText = String.format("%02d:%02d:%02d", h, m, s)
            delay(1000)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(StardustGlassBg)
            .drawBehind { drawRoundRect(color = CorporateColors.CardBorder, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())) }
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Timer, null, tint = CorporateColors.AccentGreen, modifier = Modifier.size(14.dp))
                Text("Время на смене", color = CorporateColors.TextSecondary, fontSize = 12.sp)
            }
            Text(elapsedTimeText, color = CorporateColors.AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        AnimatedProgressBar(progress = progress, modifier = Modifier.fillMaxWidth(), fillColor = CorporateColors.AccentGreen, trackColor = StardustItemBg, cornerRadius = 3.dp)
        Spacer(Modifier.height(6.dp))
        Text("${(progress * 100).toInt()}% от 12 часов", color = CorporateColors.TextSecondary, fontSize = 11.sp)
    }
}

// =================================================================================
// КАРТОЧКИ СТАТИСТИКИ
// =================================================================================

@Composable
fun TodayStatsCard(scansToday: Int, sessionsToday: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TodayStatChip(value = scansToday, label = "СКАНОВ", icon = Icons.Outlined.QrCodeScanner, color = CorporateColors.AccentPurple, modifier = Modifier.weight(1f))
        TodayStatChip(value = sessionsToday, label = "ПАРТИЙ", icon = Icons.Outlined.Inventory2, color = Color(0xFF06B6D4), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TodayStatChip(value: Int, label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp))
            .background(StardustGlassBg)
            .drawBehind { drawRoundRect(color = CorporateColors.CardBorder, cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())) }
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.height(10.dp))
        AnimatedCounter(count = value, style = MaterialTheme.typography.headlineSmall.copy(color = CorporateColors.TextPrimary, fontWeight = FontWeight.Bold))
        Text(label, color = CorporateColors.TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
fun StreakCard(streakDays: Int, modifier: Modifier = Modifier) {
    CorporateStatCard(modifier, Icons.Default.TrendingUp, if (streakDays > 0) CorporateColors.AccentAmber else CorporateColors.TextSecondary, streakDays.toString(), "Дней подряд", "Текущий стрик")
}

@Composable
fun RecordCard(record: Int, modifier: Modifier = Modifier) {
    CorporateStatCard(modifier, Icons.Default.EmojiEvents, CorporateColors.AccentAmber, record.toString(), "Рекорд за день", "Макс. сканов")
}

@Composable
private fun CorporateStatCard(modifier: Modifier, icon: ImageVector, iconColor: Color, value: String, label: String, subLabel: String) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp))
            .background(StardustGlassBg)
            .drawBehind { drawRoundRect(color = CorporateColors.CardBorder, cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())) }
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CorporateColors.TextPrimary)
        Text(label, color = CorporateColors.TextSecondary, fontSize = 12.sp)
        Text(subLabel, color = CorporateColors.TextSecondary.copy(alpha = 0.5f), fontSize = 10.sp)
    }
}

// =================================================================================
// ГРАФИК НЕДЕЛИ
// =================================================================================

@Composable
fun WeeklyChartCard(weeklyScans: List<ChartDataPoint>) {
    if (weeklyScans.isEmpty()) return
    val maxVal = weeklyScans.maxOfOrNull { it.count }?.takeIf { it > 0 } ?: 1
    val todayIndex = weeklyScans.size - 1
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(weeklyScans) { animProgress.snapTo(0f); animProgress.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val progress by animProgress.asState()

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(StardustGlassBg)
            .drawBehind { drawRoundRect(color = CorporateColors.CardBorder, cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())) }
            .padding(18.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.BarChart, null, tint = CorporateColors.AccentPurple, modifier = Modifier.size(18.dp))
                Text("Активность за неделю", color = CorporateColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Text("${weeklyScans.sumOf { it.count }}", color = CorporateColors.TextSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            weeklyScans.forEachIndexed { index, point ->
                val isToday  = index == todayIndex
                val fraction = (point.count.toFloat() / maxVal) * progress
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                    if (point.count > 0) {
                        Text(if (point.count >= 1000) "${point.count / 1000}k" else point.count.toString(), color = if (isToday) CorporateColors.TextPrimary else CorporateColors.TextSecondary, fontSize = 9.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                        Spacer(Modifier.height(4.dp))
                    }
                    Box(modifier = Modifier.width(10.dp).height(76.dp).clip(RoundedCornerShape(50.dp)).background(Color.Transparent)) {
                        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction.coerceAtLeast(0.04f))
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isToday) CorporateColors.AccentPurple else CorporateColors.CardBorder)
                            .align(Alignment.BottomCenter))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(point.day.take(2), color = if (isToday) CorporateColors.AccentPurple else CorporateColors.TextSecondary, fontSize = 10.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

// =================================================================================
// КОМПОНЕНТ ТЕХНИКА
// =================================================================================

@Composable
fun TechnicFieldStatsCard(stats: FieldRepairStats, isLoading: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(StardustGlassBg)
            .drawBehind { drawRoundRect(color = CorporateColors.CardBorder, cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())) }
            .padding(18.dp)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                DotsLoader(color = CorporateColors.AccentPurple)
            }
        } else {
            Text("Полевой ремонт сегодня", color = CorporateColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(
                    Triple(stats.doneToday.toString(), "Завершено", Color(0xFF22C55E)),
                    Triple((stats.totalToday - stats.doneToday).toString(), "Осталось", CorporateColors.AccentAmber),
                    Triple(stats.totalToday.toString(), "Всего", CorporateColors.AccentPurple)
                ).forEach { (value, label, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
                        Text(label, fontSize = 12.sp, color = CorporateColors.TextSecondary)
                    }
                }
            }
        }
    }
}

// =================================================================================
// ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ
// =================================================================================

@Composable
fun StatListItem(icon: ImageVector, label: String, value: String, iconTint: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = CorporateColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = CorporateColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatDivider() {
    HorizontalDivider(color = CorporateColors.CardBorder, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun AnimatedCounter(count: Int, modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current) {
    val animatedCount by animateIntAsState(count, tween(1000, easing = FastOutSlowInEasing), label = "counter")
    Text(animatedCount.toString(), modifier = modifier, style = style)
}

// =================================================================================
// ШТОРКА ИСТОРИИ СМЕН
// =================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftHistoryBottomSheet(shifts: List<com.example.qrscannerapp.features.shift.domain.model.Shift>, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.Transparent, dragHandle = { BottomSheetDefaults.DragHandle(color = CorporateColors.CardBorder) }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Журнал работы", color = CorporateColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            if (shifts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text("История смен пока пуста", color = CorporateColors.TextSecondary) }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxHeight(0.8f)) {
                    items(shifts) { ShiftHistoryCard(shift = it) }
                }
            }
        }
    }
}

@Composable
fun ShiftHistoryCard(shift: com.example.qrscannerapp.features.shift.domain.model.Shift) {
    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("ru")) }
    val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale("ru")) }
    val dateStr     = dateFormat.format(java.util.Date(shift.startTime))
    val startStr    = timeFormat.format(java.util.Date(shift.startTime))
    val endStr      = shift.endTime?.let { timeFormat.format(java.util.Date(it)) } ?: "..."
    val h = shift.durationMinutes / 60; val m = shift.durationMinutes % 60
    val durStr      = if (h > 0) "${h}ч ${m}м" else "${m}м"
    val (statusColor, statusIcon, statusText) = when {
        shift.isActive -> Triple(CorporateColors.AccentGreen, Icons.Default.PlayCircle, "В процессе")
        shift.status == "COMPLETED" || shift.endReason == "manual" -> Triple(CorporateColors.AccentPurple, Icons.Default.CheckCircle, "Завершена")
        shift.endReason == "auto_12h" -> Triple(CorporateColors.AccentAmber, Icons.Default.Warning, "Авто-закрытие")
        shift.status == "FORCE_ENDED" || shift.endReason == "admin_force" -> Triple(CorporateColors.AccentRed, Icons.Default.GppBad, "Закрыто админом")
        else -> Triple(CorporateColors.TextSecondary, Icons.Default.Info, "Завершена")
    }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(StardustGlassBg)
            .drawBehind { drawRoundRect(color = CorporateColors.CardBorder, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())) }
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("$dateStr · $startStr – $endStr", color = CorporateColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(statusColor.copy(alpha = 0.12f)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(10.dp))
                    Text(statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = CorporateColors.CardBorder)
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(
                Triple(Icons.Outlined.QrCodeScanner, "${shift.totalScanCount}", "скан."),
                Triple(Icons.Outlined.Inventory2, "${shift.sessionsCreated}", "парт."),
                Triple(Icons.Outlined.Schedule, durStr, "")
            ).forEach { (icon, value, unit) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = CorporateColors.TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(value, color = CorporateColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (unit.isNotBlank()) Text(" $unit", color = CorporateColors.TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

// =================================================================================
// ДИАЛОГИ
// =================================================================================

@Composable
private fun AccountConfirmDialog(title: String, message: String, confirmText: String, accentColor: Color, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(if (visible) 1f else 0.90f, spring(0.75f, 400f), label = "s")
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(220), label = "a")
    fun handleDismiss() { visible = false; onDismiss() }

    Dialog(onDismissRequest = { handleDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f * alpha))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { handleDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(0.86f).wrapContentHeight()
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1C1830), Color(0xFF12102A)), Offset(0f,0f), Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
                    .drawBehind { drawRect(color = accentColor.copy(alpha = 0.45f), topLeft = Offset(size.width * 0.15f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 0.7f, 1.5.dp.toPx())) }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            ) {
                Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(if (accentColor == CorporateColors.AccentRed) Icons.Default.Warning else Icons.Default.Info, null, tint = accentColor, modifier = Modifier.size(24.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(title, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center)
                        Text(message, color = StardustTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp)).background(StardustGlassBg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { handleDismiss() }, contentAlignment = Alignment.Center) {
                            Text("Отмена", color = StardustTextSecondary, fontWeight = FontWeight.SemiBold)
                        }
                        Box(modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp)).background(accentColor).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onConfirm() }, contentAlignment = Alignment.Center) {
                            Text(confirmText, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// =================================================================================
// ПРИНУДИТЕЛЬНОЕ ОБНОВЛЕНИЕ
// =================================================================================

@Composable
fun ForceUpdateDialog(message: String, updateState: UpdateState, onUpdate: () -> Unit, onExit: () -> Unit) {
    Dialog(onDismissRequest = { }, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1C1830), Color(0xFF12102A)), Offset(0f,0f), Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
                .drawBehind { drawRect(color = CorporateColors.AccentAmber.copy(alpha = 0.5f), topLeft = Offset(size.width * 0.15f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 0.7f, 1.5.dp.toPx())) }
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(CorporateColors.AccentAmber.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.SystemUpdate, null, tint = CorporateColors.AccentAmber, modifier = Modifier.size(28.dp))
            }
            Text("Требуется обновление", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StardustTextPrimary, textAlign = TextAlign.Center)
            Text(message, fontSize = 13.sp, color = StardustTextSecondary, textAlign = TextAlign.Center, lineHeight = 18.sp)

            when (updateState) {
                is UpdateState.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(progress = { updateState.progress / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = CorporateColors.AccentPurple, trackColor = StardustGlassBg)
                        Text("Загрузка ${updateState.progress}%", color = StardustTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
                else -> {
                    val isChecking = updateState is UpdateState.Checking
                    Box(
                        modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(CorporateColors.AccentPurple, CorporateColors.AccentPurple.copy(alpha = 0.7f))))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !isChecking) { onUpdate() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecking) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                        else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("Обновить приложение", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(CorporateColors.AccentRed.copy(alpha = 0.08f))
                    .drawBehind { drawRoundRect(color = CorporateColors.AccentRed.copy(alpha = 0.25f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())) }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onExit() },
                contentAlignment = Alignment.Center
            ) {
                Text("Выйти из приложения", color = CorporateColors.AccentRed, fontWeight = FontWeight.Medium)
            }
        }
    }
}