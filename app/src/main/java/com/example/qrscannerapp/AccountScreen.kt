package com.example.qrscannerapp

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.qrscannerapp.features.street_doctor.domain.model.FieldRepairStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// =================================================================================
// КОРПОРАТИВНАЯ ЦВЕТОВАЯ ПАЛИТРА
// =================================================================================
object CorporateColors {
    val Background   = Color(0xFF09090E)
    val CardSurface  = Color(0xFF14141E)
    val CardBorder   = Color(0xFF2A2A3A)
    val TextPrimary  = Color(0xFFF3F4F6)
    val TextSecondary = Color(0xFF8E8E9F)
    val AccentGreen  = Color(0xFF10B981)
    val AccentPurple = Color(0xFF7C3AED)
    val AccentRed    = Color(0xFFEF4444)
    val AccentAmber  = Color(0xFFF59E0B)
}

private const val GITHUB_EMPLOYEES_URL =
    "https://raw.githubusercontent.com/Sixnyder6/QrScannerApp/master/images/employees/"

fun getEmployeePhotoUrl(userName: String): String? {
    val filename = when (userName) {
        "Николай Никасов"  -> "nikasov.png"
        "Михаил Ситников"  -> "sitnikov.png"
        "Соболев Владислав" -> "sobolev.png"
        else -> null
    }
    return filename?.let { GITHUB_EMPLOYEES_URL + it }
}

// =================================================================================
// ГЛАВНЫЙ ЭКРАН АККАУНТА
// =================================================================================

@Composable
fun AccountScreen(authManager: AuthManager) {
    val authState by authManager.authState.collectAsState()
    val context = LocalContext.current
    var showForceUpdateDialog by remember { mutableStateOf(false) }

    val updateManager: UpdateManager = androidx.hilt.navigation.compose.hiltViewModel()
    val updateState by updateManager.updateState.collectAsState()

    LaunchedEffect(authState.error) {
        authState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authManager.clearError()
        }
    }

    LaunchedEffect(authState.versionError) {
        if (authState.versionError) {
            showForceUpdateDialog = true
            updateManager.checkForUpdates()
        }
    }

    LaunchedEffect(updateState) {
        if (updateState is UpdateState.ReadyToInstall) {
            updateManager.installApk((updateState as UpdateState.ReadyToInstall).uri)
            updateManager.resetState()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(CorporateColors.Background)) {
        when {
            authState.isLoading -> {
                CircularProgressIndicator(
                    color = CorporateColors.AccentPurple,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            authState.isLoggedIn && !authState.versionError -> {
                val viewModel: AccountViewModel =
                    androidx.hilt.navigation.compose.hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()

                when {
                    uiState.isLoading && uiState.userName == "Загрузка..." -> {
                        CircularProgressIndicator(
                            color = CorporateColors.AccentPurple,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.error != null -> {
                        Text(
                            text = uiState.error!!,
                            color = CorporateColors.AccentRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(16.dp)
                        )
                    }
                    else -> {
                        PersonalProfileScreen(
                            viewModel = viewModel,
                            state = uiState,
                            authManager = authManager
                        )
                    }
                }
            }
            !authState.isLoggedIn && !authState.versionError -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LoginFormComponent(authManager = authManager)
                }
            }
        }
    }

    if (showForceUpdateDialog) {
        ForceUpdateDialog(
            message     = authState.error ?: "Ваша версия приложения устарела. Пожалуйста, обновитесь.",
            updateState = updateState,
            onUpdate    = {
                val info = (updateState as? UpdateState.UpdateAvailable)?.info
                if (info != null) updateManager.startUpdate(info)
                else updateManager.startCheckForUpdates()
            },
            onExit = {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        )
    }
}

// =================================================================================
// ДИАЛОГ ПРИНУДИТЕЛЬНОГО ОБНОВЛЕНИЯ
// =================================================================================
@Composable
fun ForceUpdateDialog(
    message: String,
    updateState: UpdateState,
    onUpdate: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CorporateColors.CardSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CorporateColors.CardBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = CorporateColors.AccentAmber,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Требуется обновление",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = CorporateColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = CorporateColors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (updateState) {
                    is UpdateState.Downloading -> {
                        val progress = updateState.progress
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = CorporateColors.AccentPurple,
                                trackColor = CorporateColors.CardBorder
                            )
                            Text(
                                "Загрузка... $progress%",
                                color = CorporateColors.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                    is UpdateState.Error -> {
                        Button(
                            onClick = onUpdate,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CorporateColors.AccentRed)
                        ) {
                            Icon(Icons.Filled.Refresh, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Повторить", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    else -> {
                        val isChecking = updateState is UpdateState.Checking
                        Button(
                            onClick = onUpdate,
                            enabled = !isChecking,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CorporateColors.AccentPurple)
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Download, null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Обновить приложение", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Выйти из приложения", color = CorporateColors.AccentRed)
                }
            }
        }
    }
}

// =================================================================================
// ПРОФИЛЬ
// =================================================================================

@Composable
fun PersonalProfileScreen(
    viewModel: AccountViewModel,
    state: AccountUiState,
    authManager: AuthManager
) {
    val scope = rememberCoroutineScope()
    val isTechnic = state.userRoleEnum == UserRole.TECHNIC

    var showEndShiftDialog by remember { mutableStateOf(false) }
    var showLogoutDialog   by remember { mutableStateOf(false) }
    var showHistorySheet   by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        item {
            ProfileHeader(
                userName      = state.userName,
                userRole      = state.userRole,
                isShiftActive = state.isShiftActive,
                onStartShift  = { viewModel.startShift() },
                onEndShift    = { showEndShiftDialog = true }
            )
        }

        item {
            AnimatedVisibility(
                visible = state.isShiftActive,
                enter = fadeIn(tween(300)) + expandVertically(tween(500)),
                exit  = fadeOut(tween(300)) + shrinkVertically(tween(500))
            ) {
                ShiftProgressBar(
                    modifier = Modifier.padding(top = 16.dp),
                    shiftStartTime = state.shiftStartTime
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            if (isTechnic) {
                TechnicFieldStatsCard(
                    stats     = state.fieldRepairStats,
                    isLoading = state.fieldRepairLoading
                )
            } else {
                AnimatedVisibility(
                    visible = state.isShiftActive,
                    enter = fadeIn(tween(400)) + expandVertically(tween(500)),
                    exit  = fadeOut(tween(300)) + shrinkVertically(tween(400))
                ) {
                    TodayStatsCard(
                        scansToday    = state.scansToday,
                        sessionsToday = state.sessionsToday
                    )
                }
            }
        }

        if (!isTechnic) {
            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StreakCard(streakDays = state.streakDays, modifier = Modifier.weight(1f))
                    RecordCard(record = state.personalRecord, modifier = Modifier.weight(1f))
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                WeeklyChartCard(weeklyScans = state.weeklyScans)
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { showHistorySheet = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CorporateColors.CardSurface)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.History, null, tint = CorporateColors.TextPrimary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Открыть журнал смен",
                        color = CorporateColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(32.dp))
            Text(
                "Общая статистика",
                color = CorporateColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 4.dp)
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CorporateColors.CardSurface)
                    .border(1.dp, CorporateColors.CardBorder, RoundedCornerShape(20.dp))
                    .padding(vertical = 8.dp)
            ) {
                if (isTechnic) {
                    StatListItem(
                        Icons.Default.TwoWheeler,
                        "Самокатов завершено",
                        state.fieldRepairStats.doneAllTime.toString(),
                        Color(0xFF22C55E)
                    )
                    HorizontalDivider(color = CorporateColors.CardBorder, modifier = Modifier.padding(horizontal = 20.dp))
                    StatListItem(
                        Icons.Default.List,
                        "Всего заданий",
                        state.fieldRepairStats.totalAllTime.toString(),
                        CorporateColors.AccentPurple
                    )
                    HorizontalDivider(color = CorporateColors.CardBorder, modifier = Modifier.padding(horizontal = 20.dp))
                    StatListItem(
                        Icons.Default.Timer,
                        "Среднее время",
                        if (state.fieldRepairStats.avgMinutesPerScooter > 0)
                            "${state.fieldRepairStats.avgMinutesPerScooter} мин"
                        else "—",
                        CorporateColors.AccentAmber
                    )
                    HorizontalDivider(color = CorporateColors.CardBorder, modifier = Modifier.padding(horizontal = 20.dp))
                    StatListItem(
                        Icons.Outlined.Event,
                        "В системе с",
                        state.registrationDate,
                        CorporateColors.TextSecondary
                    )
                } else {
                    StatListItem(
                        Icons.Outlined.QrCodeScanner,
                        "Всего сканирований",
                        state.totalScans.toString(),
                        CorporateColors.AccentPurple
                    )
                    HorizontalDivider(color = CorporateColors.CardBorder, modifier = Modifier.padding(horizontal = 20.dp))
                    StatListItem(
                        Icons.Outlined.Inventory2,
                        "Обработано партий",
                        state.totalSessions.toString(),
                        CorporateColors.AccentPurple
                    )
                    HorizontalDivider(color = CorporateColors.CardBorder, modifier = Modifier.padding(horizontal = 20.dp))
                    StatListItem(
                        Icons.Outlined.Event,
                        "В системе с",
                        state.registrationDate,
                        CorporateColors.TextSecondary
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(32.dp))
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CorporateColors.AccentRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, CorporateColors.AccentRed.copy(alpha = 0.5f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                Spacer(Modifier.width(8.dp))
                Text("Выйти из системы", fontWeight = FontWeight.Medium, fontSize = 16.sp)
            }
        }
    }

    if (showHistorySheet) {
        ShiftHistoryBottomSheet(
            shifts    = state.shiftHistory,
            onDismiss = { showHistorySheet = false }
        )
    }

    EndShiftDialog(
        showDialog = showEndShiftDialog,
        onDismiss  = { showEndShiftDialog = false },
        onConfirm  = { showEndShiftDialog = false; viewModel.endShift() }
    )

    LogoutDialog(
        showDialog    = showLogoutDialog,
        isShiftActive = state.isShiftActive,
        onDismiss     = { showLogoutDialog = false },
        onConfirm     = {
            showLogoutDialog = false
            scope.launch {
                if (state.isShiftActive) viewModel.endShiftOnLogout()
                authManager.logout()
            }
        }
    )
}

// =================================================================================
// ШТОРКА ИСТОРИИ СМЕН
// =================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftHistoryBottomSheet(
    shifts: List<com.example.qrscannerapp.features.shift.domain.model.Shift>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = CorporateColors.Background,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = CorporateColors.CardBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                "Журнал работы",
                color      = CorporateColors.TextPrimary,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(bottom = 16.dp)
            )

            if (shifts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("История смен пока пуста", color = CorporateColors.TextSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxHeight(0.8f)
                ) {
                    items(shifts) { shift ->
                        ShiftHistoryCard(shift = shift)
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftHistoryCard(shift: com.example.qrscannerapp.features.shift.domain.model.Shift) {
    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("ru")) }
    val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale("ru")) }

    val dateStr      = dateFormat.format(java.util.Date(shift.startTime))
    val startTimeStr = timeFormat.format(java.util.Date(shift.startTime))
    val endTimeStr   = shift.endTime?.let { timeFormat.format(java.util.Date(it)) } ?: "..."
    val hours        = shift.durationMinutes / 60
    val mins         = shift.durationMinutes % 60
    val durationStr  = if (hours > 0) "${hours}ч ${mins}м" else "${mins}м"

    val (statusColor, statusIcon, statusText) = when {
        shift.isActive -> Triple(CorporateColors.AccentGreen, Icons.Default.PlayCircle, "В процессе")
        shift.status == "COMPLETED" || shift.endReason == "manual" ->
            Triple(CorporateColors.AccentPurple, Icons.Default.CheckCircle, "Завершена")
        shift.endReason == "auto_12h" ->
            Triple(CorporateColors.AccentAmber, Icons.Default.Warning, "Авто-закрытие")
        shift.status == "FORCE_ENDED" || shift.endReason == "admin_force" ->
            Triple(CorporateColors.AccentRed, Icons.Default.GppBad, "Закрыто админом")
        else -> Triple(CorporateColors.TextSecondary, Icons.Default.Info, "Завершена")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = CorporateColors.CardSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CorporateColors.CardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$dateStr • $startTimeStr - $endTimeStr",
                    color = CorporateColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = CorporateColors.CardBorder)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.QrCodeScanner, null, tint = CorporateColors.TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${shift.totalScanCount}", color = CorporateColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(" скан.", color = CorporateColors.TextSecondary, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Inventory2, null, tint = CorporateColors.TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${shift.sessionsCreated}", color = CorporateColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(" парт.", color = CorporateColors.TextSecondary, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, tint = CorporateColors.TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(durationStr, color = CorporateColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =================================================================================
// КОМПОНЕНТЫ
// =================================================================================

@Composable
fun ProfileHeader(
    userName: String,
    userRole: String,
    isShiftActive: Boolean,
    onStartShift: () -> Unit,
    onEndShift: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoUrl = remember(userName) { getEmployeePhotoUrl(userName) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(containerColor = CorporateColors.CardSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, CorporateColors.CardBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(CorporateColors.Background)
                            .border(2.dp, CorporateColors.CardBorder, CircleShape)
                    ) {
                        if (photoUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(photoUrl).crossfade(true).build(),
                                contentDescription = "Profile Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.Person, null,
                                modifier = Modifier.size(40.dp).align(Alignment.Center),
                                tint = CorporateColors.TextSecondary
                            )
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(userName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CorporateColors.TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(8.dp).clip(CircleShape)
                                    .background(if (isShiftActive) CorporateColors.AccentGreen else CorporateColors.TextSecondary)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isShiftActive) "На смене • $userRole" else "Не в сети • $userRole",
                                fontSize = 13.sp, fontWeight = FontWeight.Medium, color = CorporateColors.TextSecondary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = CorporateColors.AccentGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Бестужевская 10",
                                fontSize = 12.sp,
                                color = CorporateColors.TextSecondary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                if (!isShiftActive) {
                    Button(
                        onClick = onStartShift,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CorporateColors.AccentGreen)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Начать смену", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                } else {
                    Button(
                        onClick = onEndShift,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CorporateColors.Background)
                    ) {
                        Icon(Icons.Default.Stop, null, tint = CorporateColors.AccentRed)
                        Spacer(Modifier.width(8.dp))
                        Text("Завершить смену", color = CorporateColors.AccentRed, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftProgressBar(modifier: Modifier = Modifier, shiftStartTime: Long) {
    val totalShiftDuration = 12 * 60 * 60 * 1000L
    var progress by remember { mutableFloatStateOf(0f) }
    var elapsedTimeText by remember { mutableStateOf("00:00:00") }

    LaunchedEffect(shiftStartTime) {
        if (shiftStartTime == 0L) { progress = 0f; elapsedTimeText = "00:00:00"; return@LaunchedEffect }
        while (true) {
            val elapsedMillis = (System.currentTimeMillis() - shiftStartTime).coerceAtLeast(0)
            progress = (elapsedMillis.toFloat() / totalShiftDuration).coerceIn(0f, 1f)
            val hours = elapsedMillis / 3600000; val minutes = (elapsedMillis / 60000) % 60; val seconds = (elapsedMillis / 1000) % 60
            elapsedTimeText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            delay(1000)
        }
    }

    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000, easing = LinearEasing), label = "progress")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Время на смене", color = CorporateColors.TextSecondary, fontSize = 13.sp)
            Text(elapsedTimeText, color = CorporateColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        }
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(CorporateColors.CardSurface)) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedProgress).clip(CircleShape).background(CorporateColors.AccentGreen))
        }
    }
}

@Composable
fun TodayStatsCard(scansToday: Int, sessionsToday: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CorporateColors.CardSurface)) {
        Box(modifier = Modifier.fillMaxWidth().border(1.dp, CorporateColors.CardBorder, RoundedCornerShape(20.dp)).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                TodayStatItem(value = scansToday, label = "СКАНИРОВАНИЙ", color = CorporateColors.TextPrimary)
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(CorporateColors.CardBorder))
                TodayStatItem(value = sessionsToday, label = "ПАРТИЙ", color = CorporateColors.AccentPurple)
            }
        }
    }
}

@Composable
private fun TodayStatItem(value: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedCounter(count = value, style = MaterialTheme.typography.headlineMedium.copy(color = color, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(2.dp))
        Text(label, color = CorporateColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
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
private fun CorporateStatCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color, value: String, label: String, subLabel: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CorporateColors.CardSurface)) {
        Column(modifier = Modifier.fillMaxWidth().border(1.dp, CorporateColors.CardBorder, RoundedCornerShape(20.dp)).padding(16.dp), horizontalAlignment = Alignment.Start) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CorporateColors.TextPrimary)
            Text(label, color = CorporateColors.TextSecondary, fontSize = 12.sp)
            Text(subLabel, color = CorporateColors.TextSecondary.copy(alpha = 0.5f), fontSize = 10.sp)
        }
    }
}

@Composable
fun WeeklyChartCard(weeklyScans: List<ChartDataPoint>) {
    if (weeklyScans.isEmpty()) return
    val maxVal = weeklyScans.maxOfOrNull { it.count }?.takeIf { it > 0 } ?: 1
    val todayIndex = weeklyScans.size - 1
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(weeklyScans) { animProgress.snapTo(0f); animProgress.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
    val progress by animProgress.asState()

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CorporateColors.CardSurface)) {
        Column(modifier = Modifier.border(1.dp, CorporateColors.CardBorder, RoundedCornerShape(20.dp)).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.BarChart, null, tint = CorporateColors.AccentPurple, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Активность за неделю", color = CorporateColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Text("${weeklyScans.sumOf { it.count }}", color = CorporateColors.TextSecondary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                weeklyScans.forEachIndexed { index, point ->
                    val isToday  = index == todayIndex
                    val fraction = (point.count.toFloat() / maxVal) * progress
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                        if (point.count > 0) {
                            Text(if (point.count >= 1000) "${point.count / 1000}k" else point.count.toString(), color = if (isToday) CorporateColors.TextPrimary else CorporateColors.TextSecondary, fontSize = 10.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                        }
                        Box(modifier = Modifier.width(12.dp).height(80.dp).clip(RoundedCornerShape(50)).background(CorporateColors.Background)) {
                            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction.coerceAtLeast(0.05f)).clip(RoundedCornerShape(50)).background(if (isToday) CorporateColors.AccentPurple else CorporateColors.CardBorder).align(Alignment.BottomCenter))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(point.day.take(2), color = if (isToday) CorporateColors.AccentPurple else CorporateColors.TextSecondary, fontSize = 11.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun StatListItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, iconTint: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(CorporateColors.Background), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(label, color = CorporateColors.TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = CorporateColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AnimatedCounter(count: Int, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current) {
    val animatedCount by animateIntAsState(targetValue = count, animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "counter")
    Text(animatedCount.toString(), modifier = modifier, style = style)
}

@Composable
private fun EndShiftDialog(showDialog: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = CorporateColors.CardSurface,
            titleContentColor = CorporateColors.TextPrimary,
            textContentColor = CorporateColors.TextSecondary,
            title = { Text("Завершение смены", fontWeight = FontWeight.Bold) },
            text = { Text("Статистика работы будет сохранена. Вы уверены?") },
            confirmButton = { TextButton(onClick = onConfirm) { Text("Завершить", color = CorporateColors.AccentRed, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = CorporateColors.TextSecondary) } }
        )
    }
}

@Composable
private fun LogoutDialog(showDialog: Boolean, isShiftActive: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = CorporateColors.CardSurface,
            titleContentColor = CorporateColors.TextPrimary,
            textContentColor = CorporateColors.TextSecondary,
            title = { Text("Выход из аккаунта", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (isShiftActive) Text("У вас есть активная смена! Она будет завершена автоматически.", color = CorporateColors.AccentAmber, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
                    Text("Продолжить выход из корпоративной системы?")
                }
            },
            confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = CorporateColors.AccentRed)) { Text("Выйти", color = Color.White) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = CorporateColors.TextSecondary) } }
        )
    }
}

@Composable
private fun LoginFormComponent(authManager: AuthManager) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CorporateColors.CardSurface)) {
        Column(modifier = Modifier.border(1.dp, CorporateColors.CardBorder, RoundedCornerShape(24.dp)).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Lock, null, tint = CorporateColors.AccentPurple, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Вход в систему", fontSize = 22.sp, color = CorporateColors.TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(
                value = username, onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Логин") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorporateColors.AccentPurple, unfocusedBorderColor = CorporateColors.CardBorder, focusedLabelColor = CorporateColors.AccentPurple, unfocusedLabelColor = CorporateColors.TextSecondary, focusedTextColor = CorporateColors.TextPrimary, unfocusedTextColor = CorporateColors.TextPrimary, focusedContainerColor = CorporateColors.Background, unfocusedContainerColor = CorporateColors.Background)
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth().focusRequester(passwordFocusRequester),
                label = { Text("Пароль") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CorporateColors.AccentPurple, unfocusedBorderColor = CorporateColors.CardBorder, focusedLabelColor = CorporateColors.AccentPurple, unfocusedLabelColor = CorporateColors.TextSecondary, focusedTextColor = CorporateColors.TextPrimary, unfocusedTextColor = CorporateColors.TextPrimary, focusedContainerColor = CorporateColors.Background, unfocusedContainerColor = CorporateColors.Background)
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { focusManager.clearFocus(); scope.launch { authManager.login(username.trim(), password.trim()) } },
                enabled = username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CorporateColors.AccentPurple, disabledContainerColor = CorporateColors.CardBorder)
            ) { Text("Войти", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
        }
    }
}

// =================================================================================
// КОМПОНЕНТ ДЛЯ ТЕХНИКА
// =================================================================================
@Composable
fun TechnicFieldStatsCard(
    stats: FieldRepairStats,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CorporateColors.CardSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CorporateColors.CardBorder, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CorporateColors.AccentPurple, modifier = Modifier.size(32.dp))
                }
            } else {
                Column {
                    Text(
                        "Полевой ремонт сегодня",
                        color = CorporateColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stats.doneToday.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = CorporateColors.AccentGreen
                            )
                            Text("Завершено", fontSize = 12.sp, color = CorporateColors.TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                (stats.totalToday - stats.doneToday).toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = CorporateColors.AccentAmber
                            )
                            Text("Осталось", fontSize = 12.sp, color = CorporateColors.TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stats.totalToday.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = CorporateColors.AccentPurple
                            )
                            Text("Всего", fontSize = 12.sp, color = CorporateColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}