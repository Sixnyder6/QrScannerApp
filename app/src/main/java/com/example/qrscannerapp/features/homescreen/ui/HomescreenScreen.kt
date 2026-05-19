package com.example.qrscannerapp.features.homescreen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.AccountViewModel
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.TelemetryManager
import com.example.qrscannerapp.UserRole
import com.example.qrscannerapp.features.chat.ui.ChatViewModel
import com.example.qrscannerapp.features.chat.ui.DirectInboxViewModel
import com.example.qrscannerapp.features.homescreen.ui.widgets.ChatWidget
import com.example.qrscannerapp.features.homescreen.ui.widgets.ShiftWidget
import com.example.qrscannerapp.features.homescreen.ui.widgets.TasksWidget
import com.example.qrscannerapp.features.homescreen.ui.widgets.TeamOnlineWidget
import com.example.qrscannerapp.features.homescreen.ui.widgets.TodayStatsWidget
import com.example.qrscannerapp.features.homescreen.ui.widgets.WeatherWidget
import com.example.qrscannerapp.features.homescreen.ui.widgets.ProvideShimmerPhase
import com.example.qrscannerapp.features.tasks.ui.viewmodel.MyTasksViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToInt

private const val PHI = 0.6180339887f
private val HomescreenBg = Color(0xFF02020A)
private const val DOT_SPACING = 28f
private const val DOT_RADIUS  = 1.1f
private val DOT_COLOR = Color(0xFF161625)

data class HomescreenActions(
    val openScanner: () -> Unit,
    val openInteraction: () -> Unit,
    val openTasks: () -> Unit,
    val openPalletDistribution: () -> Unit,
    val openStorage: () -> Unit,
    val openWarehouse: () -> Unit,
    val openDelivery: () -> Unit,
    val openDashboard: () -> Unit,
    val openVehicleReport: () -> Unit,
    val openFieldRepairAdmin: () -> Unit,
    val openTeam: () -> Unit,
    val openChat: () -> Unit,
    val openQrGenerator: () -> Unit,
    val openSettings: () -> Unit,
    val openHistory: () -> Unit,
    val openAccount: () -> Unit,
    val openSecurityDashboard: () -> Unit,
    val openSecurityScooters: () -> Unit,
    val openSecurityBattery: () -> Unit,
    val openSecurityStorage: () -> Unit
)

private data class IconEntry(
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit
)

@Composable
fun HomescreenScreen(
    authManager: AuthManager,
    telemetryManager: TelemetryManager,
    chatViewModel: ChatViewModel,
    actions: HomescreenActions,
    modifier: Modifier = Modifier
) {
    val authState by authManager.authState.collectAsState()
    val isUserManager = remember(authState) {
        val name = authState.userName?.lowercase().orEmpty()
        authState.isAdmin || authState.role == UserRole.INVENTORY_MANAGER ||
                name.contains("ситников") || name.contains("miha.sklad") || name.contains("nikasov")
    }
    val isStrictSecurity = authState.role == UserRole.SECURITY
    val isSecurity = isStrictSecurity || authState.isAdmin

    val hazeState = rememberHazeState()


    val tasksViewModel: MyTasksViewModel = hiltViewModel()
    val dmInboxViewModel: DirectInboxViewModel = hiltViewModel()
    val accountViewModel: AccountViewModel = hiltViewModel()

    // ── Dialog state (заменяет BottomSheet) ──────────────────────────────
    var showTaskDialog     by remember { mutableStateOf(false) }
    var showShiftDialog    by remember { mutableStateOf(false) }

    val allIcons = remember(actions, isUserManager, isStrictSecurity, isSecurity) {
        buildList {
            if (isStrictSecurity) {
                add(IconEntry("Дашборд",   Icons.Outlined.Shield,                      Color(0xFFF87171), actions.openSecurityDashboard))
                add(IconEntry("Самокаты",  Icons.AutoMirrored.Outlined.DirectionsBike, Color(0xFFF87171), actions.openSecurityScooters))
                add(IconEntry("АКБ СБ",    Icons.Outlined.BatteryAlert,                Color(0xFFF87171), actions.openSecurityBattery))
                add(IconEntry("Склад СБ",  Icons.Outlined.Inventory,                   Color(0xFFF87171), actions.openSecurityStorage))
            } else {
                add(IconEntry("Сканер",    Icons.Outlined.DocumentScanner,             Color(0xFF8B7FFF), actions.openScanner))
                add(IconEntry("Взаимод.",  Icons.Outlined.Handyman,                    Color(0xFF22D3EE), actions.openInteraction))
                add(IconEntry("Задачи",    Icons.Outlined.TaskAlt,                     Color(0xFFFBBF24)) { showTaskDialog = true })
                add(IconEntry("АКБ",       Icons.Outlined.Inventory,                   Color(0xFF34D399), actions.openPalletDistribution))
                add(IconEntry("Самокаты",  Icons.Outlined.Inventory2,                  Color(0xFF22D3EE), actions.openStorage))
                add(IconEntry("Склад",     Icons.Outlined.Warehouse,                   Color(0xFFA78BFA), actions.openWarehouse))
                add(IconEntry("Доставка",  Icons.Outlined.LocalShipping,               Color(0xFF38BDF8), actions.openDelivery))
                if (isUserManager) {
                    add(IconEntry("Дашборд", Icons.Outlined.Analytics,  Color(0xFFF472B6), actions.openDashboard))
                    add(IconEntry("Сводка",  Icons.Outlined.Summarize,  Color(0xFFFB923C), actions.openVehicleReport))
                    add(IconEntry("Поле",    Icons.Outlined.Build,       Color(0xFF818CF8), actions.openFieldRepairAdmin))
                }
                add(IconEntry("Команда",   Icons.Outlined.Groups,                      Color(0xFF2DD4BF), actions.openTeam))
                add(IconEntry("Чат",       Icons.AutoMirrored.Filled.Chat,             Color(0xFF2DD4BF), actions.openChat))
                if (isSecurity) add(IconEntry("СБ", Icons.Outlined.Shield,             Color(0xFFF87171), actions.openSecurityDashboard))
                add(IconEntry("QR",        Icons.Outlined.QrCode2,                     Color(0xFF4ADE80), actions.openQrGenerator))
                add(IconEntry("История",   Icons.Outlined.History,                     Color(0xFFC084FC), actions.openHistory))
                add(IconEntry("Настройки", Icons.Outlined.Settings,                    Color(0xFF94A3B8), actions.openSettings))
            }
        }
    }

    ProvideShimmerPhase {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomescreenBg)
    ) {
        // ── ФОН: dot grid + vignette ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .drawWithCache {
                    val cols = (size.width  / DOT_SPACING).roundToInt() + 1
                    val rows = (size.height / DOT_SPACING).roundToInt() + 1
                    val vignetteBrush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            HomescreenBg.copy(alpha = 0.60f),
                            HomescreenBg.copy(alpha = 0.92f)
                        ),
                        center = Offset(size.width / 2f, size.height * 0.40f),
                        radius = size.maxDimension * 0.70f
                    )
                    onDrawBehind {
                        for (row in 0..rows) {
                            for (col in 0..cols) {
                                drawCircle(
                                    color  = DOT_COLOR,
                                    radius = DOT_RADIUS,
                                    center = Offset(col * DOT_SPACING, row * DOT_SPACING)
                                )
                            }
                        }
                        drawRect(brush = vignetteBrush)
                    }
                }
        )

        // ── КОНТЕНТ ───────────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {
            HomescreenStatusBar(telemetryManager = telemetryManager)

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp,
                    top = 4.dp, bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(span = { GridItemSpan(2) }) {
                    ShiftWidget(
                        authManager      = authManager,
                        hazeState        = hazeState,
                        modifier         = Modifier.height(160.dp),
                        sheenPhaseOffset = 0f,
                        onTap            = { showShiftDialog = true }
                    )
                }
                item(span = { GridItemSpan(2) }) {
                    WeatherWidget(
                        hazeState        = hazeState,
                        modifier         = Modifier.height(160.dp),
                        sheenPhaseOffset = PHI * 1
                    )
                }
                item(span = { GridItemSpan(2) }) {
                    TasksWidget(
                        viewModel        = tasksViewModel,
                        hazeState        = hazeState,
                        modifier         = Modifier.height(110.dp),
                        sheenPhaseOffset = (PHI * 2) % 1f,
                        onTap            = { showTaskDialog = true },
                        onLongPress      = actions.openTasks
                    )
                }
                item(span = { GridItemSpan(2) }) {
                    ChatWidget(
                        chatViewModel    = chatViewModel,
                        dmInboxViewModel = dmInboxViewModel,
                        hazeState        = hazeState,
                        modifier         = Modifier.height(110.dp),
                        sheenPhaseOffset = (PHI * 3) % 1f,
                        onTap            = actions.openChat
                    )
                }
                item(span = { GridItemSpan(3) }) {
                    TodayStatsWidget(
                        viewModel        = accountViewModel,
                        hazeState        = hazeState,
                        modifier         = Modifier.height(110.dp),
                        sheenPhaseOffset = (PHI * 4) % 1f,
                        onTap            = actions.openHistory
                    )
                }
                item(span = { GridItemSpan(1) }) {
                    TeamOnlineWidget(
                        hazeState        = hazeState,
                        modifier         = Modifier.height(110.dp),
                        sheenPhaseOffset = (PHI * 5) % 1f,
                        onTap            = actions.openTeam
                    )
                }
                items(
                    count = allIcons.size,
                    span  = { GridItemSpan(1) }
                ) { idx ->
                    val entry = allIcons[idx]
                    AppIconTile(
                        label       = entry.label,
                        icon        = entry.icon,
                        accentColor = entry.accent,
                        phaseOffset = (idx * PHI) % 1f,
                        onClick     = entry.onClick
                    )
                }
            }
        }

        HomescreenUserCard(
            authManager = authManager,
            hazeState   = hazeState,
            modifier    = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            onTap = actions.openAccount
        )
    }

    // ── TASK DIALOG — центральное окно вместо BottomSheet ────────────────
    if (showTaskDialog) {
        HomescreenDialog(
            title       = "Задачи",
            accentColor = Color(0xFFFBBF24),
            icon        = Icons.Outlined.TaskAlt,
            onDismiss   = { showTaskDialog = false },
            onExpand    = actions.openTasks,
            expandLabel = "Все задачи"
        ) {
            TaskDialogContent(viewModel = tasksViewModel)
        }
    }

    // ── SHIFT DIALOG — информация о текущей смене ────────────────────────
    if (showShiftDialog) {
        HomescreenDialog(
            title       = "Смена",
            accentColor = Color(0xFF22D380),
            icon        = Icons.Outlined.Schedule,
            onDismiss   = { showShiftDialog = false },
            onExpand    = actions.openAccount,
            expandLabel = "Аккаунт"
        ) {
            ShiftDialogContent(authManager = authManager)
        }
    }
    } // ProvideShimmerPhase

}