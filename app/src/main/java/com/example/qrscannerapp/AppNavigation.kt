package com.example.qrscannerapp

import androidx.compose.ui.Alignment
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.common.ui.ChatNotification
import com.example.qrscannerapp.common.ui.ChatNotificationBanner
import com.example.qrscannerapp.common.ui.ShiftRequestBanner
import com.example.qrscannerapp.common.ui.ShiftRequestNotification
import com.example.qrscannerapp.common.ui.macZoomIn
import com.example.qrscannerapp.common.ui.macZoomExitShrink
import com.example.qrscannerapp.common.ui.macZoomPopEnter
import com.example.qrscannerapp.common.ui.macZoomPopExit
import com.example.qrscannerapp.common.ui.smoothFadeIn
import com.example.qrscannerapp.common.ui.smoothFadeOut
import com.example.qrscannerapp.features.chat.domain.model.ChatRoom
import com.example.qrscannerapp.features.chat.domain.model.MessageType
import com.example.qrscannerapp.features.chat.ui.ChatScreen
import com.example.qrscannerapp.features.chat.ui.ChatViewModel
import com.example.qrscannerapp.features.chat.ui.DirectChatScreen
import com.example.qrscannerapp.features.chat.ui.DirectInboxScreen
import com.example.qrscannerapp.features.chat.ui.DirectInboxViewModel
import com.example.qrscannerapp.features.chat.ui.UserProfileScreen
import com.example.qrscannerapp.features.delivery.ui.DeliveryDashboardScreen
import com.example.qrscannerapp.features.delivery.ui.DeliveryFormScreen
import com.example.qrscannerapp.features.delivery.ui.DeliveryHistoryScreen
import com.example.qrscannerapp.features.delivery.ui.DeliveryViewModel
import com.example.qrscannerapp.features.settings.ui.UnifiedSettingsScreen
import com.example.qrscannerapp.features.homescreen.ui.HomescreenActions
import com.example.qrscannerapp.features.homescreen.ui.HomescreenScreen
import com.example.qrscannerapp.features.interaction.ui.InteractionScreen
import com.example.qrscannerapp.features.inventory.ui.Warehouse.WarehouseAddItemScreen
import com.example.qrscannerapp.features.inventory.ui.Warehouse.WarehouseDashboardScreen
import com.example.qrscannerapp.features.inventory.ui.Warehouse.WarehouseViewModel
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.WarehouseCatalogScreen
import com.example.qrscannerapp.features.inventory.ui.distribution.PalletDistributionScreen
import com.example.qrscannerapp.features.inventory.ui.storage.StorageScreen
import com.example.qrscannerapp.features.profile.ui.EmployeeProfileScreen
import com.example.qrscannerapp.features.security.ui.ScooterPassportScreen
import com.example.qrscannerapp.features.security.ui.SecurityBatteryScreen
import com.example.qrscannerapp.features.security.ui.SecurityDashboardScreen
import com.example.qrscannerapp.features.security.ui.SecurityScootersScreen
import com.example.qrscannerapp.features.security.ui.SecurityStorageScreen
import com.example.qrscannerapp.features.security.ui.SecurityViewModel
import com.example.qrscannerapp.features.security.ui.scanner.SecurityScannerScreen
import com.example.qrscannerapp.features.security.ui.scanner.SecurityScannerViewModel
import com.example.qrscannerapp.features.street_doctor.ui.FieldRepairAdminScreen
import com.example.qrscannerapp.features.street_doctor.ui.StreetDoctorHost
import com.example.qrscannerapp.features.tasks.ui.MyTasksScreen
import com.example.qrscannerapp.features.tasks.ui.creation.TaskCreationScreen
import com.example.qrscannerapp.features.tasks.ui.viewmodel.MyTasksViewModel
import com.example.qrscannerapp.features.team.ui.EmployeeDetailScreen
import com.example.qrscannerapp.features.team.ui.TeamScreen
import com.example.qrscannerapp.features.vehicle_report.ui.VehicleReportAnalyticsScreen
import com.example.qrscannerapp.features.vehicle_report.ui.VehicleReportHistoryScreen
import com.example.qrscannerapp.features.vehicle_report.ui.VehicleReportScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

// =============================================================================
// SCREEN ROUTES
// =============================================================================
// Same routes as before, plus a new Screen.Homescreen which is now the start
// destination for non-technic roles. The drawer is gone — the Home button on
// the top bar (and the back stack) bring users back here.

sealed class Screen(val route: String, val title: String) {
    object Homescreen        : Screen("homescreen",                "Главная")
    object Scanner           : Screen("scanner",                   "Сканер")
    object Interaction       : Screen("interaction",               "Взаимодействие")
    object MyTasks           : Screen("my_tasks",                  "Мои Задачи")
    object QrGenerator       : Screen("qr_generator",              "Генератор QR")
    object Settings          : Screen("settings",                  "Настройки")
    object Account           : Screen("account",                   "Аккаунт")
    object Dashboard         : Screen("dashboard",                 "Дашборд")
    object VehicleReport     : Screen("vehicle_report",            "Сводка самокатов")
    object VehicleReportHistory   : Screen("vehicle_report_history",   "История отчетов")
    object VehicleReportAnalytics : Screen("vehicle_report_analytics", "Аналитика")
    object History           : Screen("history",                   "История")
    object SessionDetail     : Screen("session_detail/{sessionId}","Детали сессии")
    object EmployeeProfile   : Screen("employee_profile/{userId}", "Профиль сотрудника")
    object AdminRepairLog    : Screen("admin_repair_log",          "Журнал ремонтов")
    object PalletDistribution: Screen("pallet_distribution",       "АКБ")
    object Storage           : Screen("storage",                   "Самокаты")
    object Warehouse         : Screen("warehouse",                 "Склад")
    object WarehouseCatalog  : Screen("warehouse_catalog",         "Каталог запчастей")
    object WarehouseAddItem  : Screen("warehouse_add_item",        "Новая запчасть")
    object TaskCreation      : Screen("task_creation",             "Создать Задачу")
    object VisualRepair      : Screen("visual_repair/{scooterId}", "3D Осмотр")
    object Chat              : Screen("chat",                      "Чат")
    object DirectChat        : Screen("direct_chat/{peerId}/{peerName}/{peerRole}", "Личные сообщения")
    object DirectInbox       : Screen("direct_inbox",              "Личные сообщения")
    object UserProfile       : Screen("user_profile/{userId}/{userName}/{userRole}", "Профиль")
    object DeliveryDashboard : Screen("delivery_dashboard",        "Логистика")
    object DeliveryForm      : Screen("delivery_form/{type}",      "Оформление")
    object DeliveryHistory   : Screen("delivery_history",          "История доставок")
    object Team              : Screen("team",                      "Команда")
    object EmployeeDetail    : Screen("employee_detail/{userId}/{userName}/{userRole}", "Профиль сотрудника")
    object SecurityDashboard : Screen("security_dashboard",        "Дашборд СБ")
    object SecurityScooters  : Screen("security_scooters",         "Самокаты СБ")
    object ScooterPassport   : Screen("scooter_passport/{scooterId}", "Паспорт")
    object SecurityBattery   : Screen("security_battery",          "АКБ СБ")
    object SecurityStorage   : Screen("security_storage",          "Склад СБ")
    object SecurityScanner   : Screen("security_scanner",          "Сканер СБ")
    object StreetDoctorTasks : Screen("street_doctor_tasks",       "Задания")
    object FieldRepairAdmin  : Screen("field_repair_admin",        "Полевой ремонт")
}

private fun chatRoomAccent(room: ChatRoom): Color = when (room) {
    ChatRoom.GENERAL  -> Color(0xFF6A5AE0)
    ChatRoom.MUVERS   -> Color(0xFF00BCD4)
    ChatRoom.MANAGERS -> Color(0xFF4CAF50)
    ChatRoom.SHIFTS   -> Color(0xFFFFA726)
    ChatRoom.ALERTS   -> Color(0xFFF44336)
}

// =============================================================================
// ENTRY POINT
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    authManager: AuthManager,
    telemetryManager: TelemetryManager,
    initialRoute: String? = null,
    initialChatRoomId: String? = null
) {
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Push notifications / external intents land here. Wait a moment so the
    // homescreen can render first — feels more native than a blank cold-start
    // jump straight into Chat.
    LaunchedEffect(initialRoute) {
        if (initialRoute != null) {
            delay(400)
            when (initialRoute) {
                "chat"     -> navController.navigate(Screen.Chat.route) { launchSingleTop = true }
                "my_tasks" -> navController.navigate(Screen.MyTasks.route) { launchSingleTop = true }
                "scanner"  -> navController.navigate(Screen.Scanner.route) { launchSingleTop = true }
            }
        }
    }

    val myTasksViewModel: MyTasksViewModel = hiltViewModel()
    val myTasksUiState by myTasksViewModel.uiState.collectAsState()
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val chatViewModel: ChatViewModel = hiltViewModel()
    val totalUnreadChat by chatViewModel.totalUnread.collectAsState()
    val dmInboxViewModel: DirectInboxViewModel = hiltViewModel()
    val dmInboxState by dmInboxViewModel.uiState.collectAsState()

    // ── Notification banners (chat + DM) — unchanged from the previous version
    var currentChatNotification by remember { mutableStateOf<ChatNotification?>(null) }
    val chatUiState by chatViewModel.uiState.collectAsState()
    val lastSeenMessageId = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(chatUiState.messages, currentRoute) {
        if (currentRoute == Screen.Chat.route) { currentChatNotification = null; return@LaunchedEffect }
        val lastMsg = chatUiState.messages.firstOrNull()
        if (lastMsg != null && lastMsg.senderId != chatViewModel.currentUserId && lastMsg.id != lastSeenMessageId.value && lastMsg.type == MessageType.TEXT) {
            lastSeenMessageId.value = lastMsg.id
            val accent = chatRoomAccent(chatUiState.activeRoom)
            val myFirstName = chatViewModel.currentUserName.split(" ").firstOrNull()?.lowercase().orEmpty()
            val isMention = myFirstName.isNotBlank() && lastMsg.mentionedNames.any { it.lowercase().contains(myFirstName) }
            currentChatNotification = ChatNotification(
                senderName = lastMsg.senderName,
                message = lastMsg.text.take(120),
                roomName = chatUiState.activeRoom.displayName,
                roomEmoji = chatUiState.activeRoom.emoji,
                accentColor = accent,
                isMention = isMention,
                id = lastMsg.timestamp?.time ?: System.currentTimeMillis()
            )
        }
    }

    var currentDmNotification by remember { mutableStateOf<ChatNotification?>(null) }
    var currentShiftRequestNotification by remember { mutableStateOf<ShiftRequestNotification?>(null) }
    val lastSeenDmId = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(dmInboxState.items, currentRoute) {
        if (currentRoute == Screen.DirectChat.route || currentRoute == Screen.DirectInbox.route) { currentDmNotification = null; return@LaunchedEffect }
        val freshItem = dmInboxState.items.firstOrNull { it.unreadCount > 0 }
        if (freshItem != null) {
            val notifId = "${freshItem.conversationId}_${freshItem.lastTimestamp?.time}"
            if (notifId != lastSeenDmId.value) {
                lastSeenDmId.value = notifId
                currentDmNotification = ChatNotification(
                    senderName = freshItem.peerName,
                    message = freshItem.lastMessage.take(120),
                    roomName = "Личное сообщение",
                    roomEmoji = "💬",
                    accentColor = when (freshItem.peerRole) {
                        "admin" -> Color(0xFFEC407A)
                        "inventory_manager" -> Color(0xFF4CAF50)
                        "muver" -> Color(0xFF29B6F6)
                        "electrician" -> Color(0xFFFFCA28)
                        "technic" -> Color(0xFFAB47BC)
                        else -> Color(0xFF78909C)
                    },
                    isMention = false,
                    id = freshItem.lastTimestamp?.time ?: System.currentTimeMillis()
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        dashboardViewModel.notificationEvent.collect { message ->
            if (message.contains("запросил доступ к смене")) {
                val name = message.removePrefix("🔔 ").removeSuffix(" запросил доступ к смене").trim()
                currentShiftRequestNotification = ShiftRequestNotification(employeeName = name)
            } else {
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            }
        }
    }

    val context = LocalContext.current
    val (appVersionName, detailedAppVersion) = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val name = "v${packageInfo.versionName}"
            val code = packageInfo.versionCode
            name to "$name ($code)"
        } catch (e: Exception) { "v?" to "v? (?)" }
    }

    val updateManager = remember { UpdateManager(context) }
    val updateState by updateManager.updateState.collectAsState()
    LaunchedEffect(Unit) { updateManager.checkForUpdates() }
    LaunchedEffect(updateState) {
        if (updateState is UpdateState.ReadyToInstall) {
            updateManager.installApk((updateState as UpdateState.ReadyToInstall).uri)
            updateManager.resetState()
        }
    }

    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    val settingsManager = remember { SettingsManager(context) }
    val hapticManager = remember { HapticFeedbackManager(settingsManager) }
    val authState by authManager.authState.collectAsState()

    var topBarActions: @Composable RowScope.() -> Unit by remember { mutableStateOf({}) }
    LaunchedEffect(currentRoute) { topBarActions = {} }

    val isTechnic = authState.role == UserRole.TECHNIC

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = Color.Transparent,
                topBar = {
                    val screensWithoutTopBar = listOf(
                        Screen.Homescreen.route,    // homescreen has its own status bar
                        Screen.Scanner.route,
                        Screen.VisualRepair.route,
                        Screen.WarehouseAddItem.route,
                        Screen.WarehouseCatalog.route,
                        Screen.StreetDoctorTasks.route,
                        Screen.Storage.route,
                        Screen.Interaction.route,
                        Screen.Chat.route,
                        Screen.DirectChat.route,
                        Screen.UserProfile.route,
                        Screen.DeliveryDashboard.route,
                        Screen.DeliveryForm.route,
                        Screen.DeliveryHistory.route,
                        Screen.Team.route,
                        Screen.EmployeeDetail.route,
                        Screen.SecurityDashboard.route,
                        Screen.SecurityScooters.route,
                        Screen.SecurityBattery.route,
                        Screen.SecurityStorage.route,
                        Screen.ScooterPassport.route,
                        Screen.FieldRepairAdmin.route
                    )

                    if (currentRoute !in screensWithoutTopBar) {
                        val currentTitle = when (currentRoute) {
                            Screen.Account.route -> "Аккаунт"
                            Screen.Dashboard.route -> "Дашборд"
                            Screen.MyTasks.route -> "Мои Задачи"
                            Screen.QrGenerator.route -> "Генератор QR"
                            Screen.Settings.route -> "Настройки"
                            Screen.History.route -> "История"
                            Screen.PalletDistribution.route -> "АКБ"
                            Screen.Warehouse.route -> "Склад"
                            Screen.VehicleReport.route -> "Сводка"
                            Screen.VehicleReportHistory.route -> "История отчетов"
                            Screen.VehicleReportAnalytics.route -> "Аналитика"
                            Screen.SessionDetail.route -> "Детали сессии"
                            Screen.EmployeeProfile.route -> "Профиль"
                            Screen.AdminRepairLog.route -> "Журнал ремонтов"
                            Screen.TaskCreation.route -> "Создать задачу"
                            Screen.SecurityScanner.route -> "Сканер СБ"
                            else -> ""
                        }

                        TopAppBar(
                            title = { Text(currentTitle) },
                            navigationIcon = {
                                val backButtonScreens = listOf(
                                    Screen.EmployeeProfile.route,
                                    Screen.AdminRepairLog.route,
                                    Screen.SessionDetail.route,
                                    Screen.TaskCreation.route,
                                    Screen.VehicleReportHistory.route,
                                    Screen.VehicleReportAnalytics.route
                                )
                                if (currentRoute in backButtonScreens) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                    }
                                } else {
                                    // Home button takes the place of the old menu button.
                                    // It always pops back to the homescreen, never opens a drawer.
                                    IconButton(onClick = {
                                        hapticManager.performClick(hapticFeedback, scope)
                                        navController.navigate(Screen.Homescreen.route) {
                                            popUpTo(Screen.Homescreen.route) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }) {
                                        Icon(Icons.Outlined.GridView, contentDescription = "На главную")
                                    }
                                }
                            },
                            actions = { topBarActions() },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Black.copy(alpha = 0.3f),
                                titleContentColor = Color.White,
                                navigationIconContentColor = Color.White,
                                actionIconContentColor = Color.White
                            )
                        )
                    }
                }
            ) { innerPadding ->
                AppNavHost(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding),
                    onHomeClick = {
                        hapticManager.performClick(hapticFeedback, scope)
                        navController.navigate(Screen.Homescreen.route) {
                            popUpTo(Screen.Homescreen.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    updateManager = updateManager,
                    appVersion = detailedAppVersion,
                    hapticManager = hapticManager,
                    view = view,
                    authManager = authManager,
                    telemetryManager = telemetryManager,
                    setTopBarActions = { actions -> topBarActions = actions },
                    chatViewModel = chatViewModel,
                    isTechnic = isTechnic
                )
            }

            ChatNotificationBanner(
                notification = currentChatNotification,
                onDismiss = { currentChatNotification = null },
                onTap = { navController.navigate(Screen.Chat.route) { launchSingleTop = true } },
                modifier = Modifier.align(Alignment.TopCenter)
            )
            if (currentChatNotification == null) {
                ChatNotificationBanner(
                    notification = currentDmNotification,
                    onDismiss = { currentDmNotification = null },
                    onTap = {
                        val item = dmInboxState.items.firstOrNull { it.unreadCount > 0 }
                        if (item != null) {
                            val encodedName = URLEncoder.encode(item.peerName, "UTF-8")
                            val encodedRole = URLEncoder.encode(item.peerRole, "UTF-8")
                            navController.navigate("direct_chat/${item.peerId}/$encodedName/$encodedRole") { launchSingleTop = true }
                        }
                        currentDmNotification = null
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            if (authState.isAdmin && currentChatNotification == null && currentDmNotification == null) {
                ShiftRequestBanner(
                    notification = currentShiftRequestNotification,
                    onDismiss    = { currentShiftRequestNotification = null },
                    onTap        = {
                        currentShiftRequestNotification = null
                        navController.navigate(Screen.Team.route) { launchSingleTop = true }
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

// =============================================================================
// NAVIGATION GRAPH
// =============================================================================

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit,
    updateManager: UpdateManager,
    appVersion: String,
    hapticManager: HapticFeedbackManager,
    view: View,
    authManager: AuthManager,
    telemetryManager: TelemetryManager,
    setTopBarActions: (@Composable RowScope.() -> Unit) -> Unit,
    chatViewModel: ChatViewModel,
    isTechnic: Boolean
) {
    val viewModel: QrScannerViewModel = hiltViewModel()
    val authState by authManager.authState.collectAsState()
    if (authState.isLoading) { LoadingScreen(); return }

    val iosEnter    = macZoomIn()
    val iosExit     = macZoomExitShrink()
    val iosPopEnter = macZoomPopEnter()
    val iosPopExit  = macZoomPopExit()
    val modalEnter  = smoothFadeIn()
    val modalExit   = smoothFadeOut()

    // For the homescreen we use a softer fade — it's the "base layer" that other
    // screens zoom out from, so a fade reads more like "returning to desktop".
    val homeEnter = smoothFadeIn()
    val homeExit  = smoothFadeOut()

    NavHost(
        navController = navController,
        startDestination = if (isTechnic) Screen.StreetDoctorTasks.route else Screen.Homescreen.route,
        modifier = modifier
    ) {

        // ── HOMESCREEN ────────────────────────────────────────────────────────
        composable(
            route = Screen.Homescreen.route,
            enterTransition = homeEnter,
            exitTransition = homeExit,
            popEnterTransition = homeEnter,
            popExitTransition = homeExit
        ) {
            // Double-press-back to exit, classic Android pattern. First press shows
            // a Toast, second within 2s closes the app via finish(). Saves users
            // from accidentally swiping back out of Stardust.
            val ctx = LocalContext.current
            var lastBackPress by remember { mutableStateOf(0L) }
            BackHandler {
                val now = System.currentTimeMillis()
                if (now - lastBackPress < 2000) {
                    (ctx as? android.app.Activity)?.finish()
                } else {
                    lastBackPress = now
                    Toast.makeText(ctx, "Нажмите ещё раз для выхода", Toast.LENGTH_SHORT).show()
                }
            }

            HomescreenScreen(
                authManager = authManager,
                telemetryManager = telemetryManager,
                chatViewModel = chatViewModel,
                actions = HomescreenActions(
                    openScanner            = { navController.navigate(Screen.Scanner.route) { launchSingleTop = true } },
                    openInteraction        = { navController.navigate(Screen.Interaction.route) { launchSingleTop = true } },
                    openTasks              = { navController.navigate(Screen.MyTasks.route) { launchSingleTop = true } },
                    openPalletDistribution = { viewModel.onNavigateToPalletDistribution(); navController.navigate(Screen.PalletDistribution.route) { launchSingleTop = true } },
                    openStorage            = { navController.navigate(Screen.Storage.route) { launchSingleTop = true } },
                    openWarehouse          = { navController.navigate(Screen.Warehouse.route) { launchSingleTop = true } },
                    openDelivery           = { navController.navigate(Screen.DeliveryDashboard.route) { launchSingleTop = true } },
                    openDashboard          = { navController.navigate(Screen.Dashboard.route) { launchSingleTop = true } },
                    openVehicleReport      = { navController.navigate(Screen.VehicleReport.route) { launchSingleTop = true } },
                    openFieldRepairAdmin   = { navController.navigate(Screen.FieldRepairAdmin.route) { launchSingleTop = true } },
                    openTeam               = { navController.navigate(Screen.Team.route) { launchSingleTop = true } },
                    openChat               = { navController.navigate(Screen.Chat.route) { launchSingleTop = true } },
                    openQrGenerator        = { navController.navigate(Screen.QrGenerator.route) { launchSingleTop = true } },
                    openSettings           = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } },
                    openHistory            = { navController.navigate(Screen.History.route) { launchSingleTop = true } },
                    openAccount            = { navController.navigate(Screen.Account.route) { launchSingleTop = true } },
                    openSecurityDashboard  = { navController.navigate(Screen.SecurityDashboard.route) { launchSingleTop = true } },
                    openSecurityScooters   = { navController.navigate(Screen.SecurityScooters.route) { launchSingleTop = true } },
                    openSecurityBattery    = { navController.navigate(Screen.SecurityBattery.route) { launchSingleTop = true } },
                    openSecurityStorage    = { navController.navigate(Screen.SecurityStorage.route) { launchSingleTop = true } }
                )
            )
        }

        // ── TECHNIC ROOT ──────────────────────────────────────────────────────
        if (isTechnic) {
            composable(route = Screen.StreetDoctorTasks.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
                val currentUserName = authState.userName ?: "Техник"
                val currentUserInitials = authState.userName
                    ?.split(" ")
                    ?.mapNotNull { it.firstOrNull()?.toString() }
                    ?.take(2)
                    ?.joinToString("") ?: "ТХ"
                StreetDoctorHost(
                    currentUserName     = currentUserName,
                    currentUserInitials = currentUserInitials,
                    isOnShift           = authState.isShiftActive,
                    userRole            = authState.role.displayName,
                    onLogout            = { authManager.logout() }
                )
            }
        }

        // ── REGULAR SCREENS ───────────────────────────────────────────────────
        composable(route = Screen.Scanner.route, enterTransition = modalEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = modalExit) {
            StardustScreen(
                viewModel = viewModel,
                onMenuClick = onHomeClick,   // every old onMenuClick now goes home
                hapticManager = hapticManager,
                view = view,
                onNavigateToPalletDistribution = { viewModel.onNavigateToPalletDistribution(); navController.navigate(Screen.PalletDistribution.route) },
                onNavigateToStorage = { navController.navigate(Screen.Storage.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) { launchSingleTop = true } },
                onNavigateToVisualRepair = { scooterId -> navController.navigate(Screen.VisualRepair.route.replace("{scooterId}", scooterId)) }
            )
        }

        composable(route = Screen.PalletDistribution.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            PalletDistributionScreen(viewModel = viewModel, authManager = authManager, onNavigateBack = { navController.popBackStack() })
        }

        composable(route = Screen.QrGenerator.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { QrGeneratorScreen() }
        composable(route = Screen.Settings.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { UnifiedSettingsScreen(authManager = authManager) }
        composable(route = Screen.Account.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { AccountScreen(authManager = authManager) }

        composable(route = Screen.Interaction.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { InteractionScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(route = Screen.MyTasks.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { MyTasksScreen(onMenuClick = onHomeClick, onTaskClick = { }) }

        composable(route = Screen.Dashboard.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            DashboardScreen(
                onNavigateToEmployeeProfile = { userId -> navController.navigate("employee_profile/$userId") },
                onNavigateToAdminRepairLog  = { navController.navigate(Screen.AdminRepairLog.route) },
                onNavigateToTaskCreation    = { navController.navigate(Screen.TaskCreation.route) },
                onNavigateToVehicleReport   = { navController.navigate(Screen.VehicleReport.route) },
                onNavigateToTeam            = { navController.navigate(Screen.Team.route) { launchSingleTop = true } },
                onNavigateToFieldRepairAdmin = { navController.navigate(Screen.FieldRepairAdmin.route) { launchSingleTop = true } },
                onNavigateToEmployeeDetail  = { userId, userName, userRole ->
                    val encodedName = URLEncoder.encode(userName, "UTF-8")
                    val encodedRole = URLEncoder.encode(userRole, "UTF-8")
                    navController.navigate("employee_detail/$userId/$encodedName/$encodedRole")
                }
            )
        }

        composable(route = Screen.FieldRepairAdmin.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            FieldRepairAdminScreen(onBack = { navController.popBackStack() }, onOpenImport = { })
        }

        composable(route = Screen.Team.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            val isUserManager = remember(authState) {
                val name = authState.userName?.lowercase().orEmpty()
                authState.isAdmin || authState.role == UserRole.INVENTORY_MANAGER ||
                        name.contains("ситников") || name.contains("miha.sklad") || name.contains("nikasov")
            }
            TeamScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId, userName, userRole ->
                    val encodedName = URLEncoder.encode(userName, "UTF-8")
                    val encodedRole = URLEncoder.encode(userRole, "UTF-8")
                    navController.navigate("employee_detail/$userId/$encodedName/$encodedRole")
                },
                onNavigateToDirectChat = { peerId, peerName, peerRole ->
                    val encodedName = URLEncoder.encode(peerName, "UTF-8")
                    val encodedRole = URLEncoder.encode(peerRole, "UTF-8")
                    navController.navigate("direct_chat/$peerId/$encodedName/$encodedRole")
                },
                isAdmin = isUserManager
            )
        }

        composable(
            route = Screen.EmployeeDetail.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }, navArgument("userName") { type = NavType.StringType }, navArgument("userRole") { type = NavType.StringType }),
            enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val userName = URLDecoder.decode(backStackEntry.arguments?.getString("userName") ?: "", "UTF-8")
            val userRole = URLDecoder.decode(backStackEntry.arguments?.getString("userRole") ?: "", "UTF-8")
            val isAdminForDetail = remember(authState) {
                val name = authState.userName?.lowercase().orEmpty()
                authState.isAdmin || authState.role == UserRole.INVENTORY_MANAGER ||
                        name.contains("ситников") || name.contains("miha.sklad") || name.contains("nikasov")
            }
            EmployeeDetailScreen(
                userId = userId, userName = userName, userRole = userRole,
                isAdmin = isAdminForDetail,
                onBack = { navController.popBackStack() },
                onWriteDm = {
                    navController.popBackStack()
                    val encodedName = URLEncoder.encode(userName, "UTF-8")
                    val encodedRole = URLEncoder.encode(userRole, "UTF-8")
                    navController.navigate("direct_chat/$userId/$encodedName/$encodedRole") { launchSingleTop = true }
                }
            )
        }

        composable(route = Screen.VehicleReport.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { VehicleReportScreen(onNavigateToHistory = { navController.navigate(Screen.VehicleReportHistory.route) }) }
        composable(route = Screen.VehicleReportHistory.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { VehicleReportHistoryScreen(onNavigateToAnalytics = { navController.navigate(Screen.VehicleReportAnalytics.route) }) }
        composable(route = Screen.VehicleReportAnalytics.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { VehicleReportAnalyticsScreen() }
        composable(route = Screen.History.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { HistoryScreen(viewModel = viewModel, navController = navController) }

        composable(route = Screen.SessionDetail.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
            if (sessionId != null) SessionDetailScreen(sessionId = sessionId, viewModel = viewModel, navController = navController)
            else PlaceholderScreen(text = "Ошибка: ID сессии не найден")
        }

        composable(route = Screen.EmployeeProfile.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { EmployeeProfileScreen() }
        composable(route = Screen.AdminRepairLog.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { AdminRepairLogScreen() }
        composable(route = Screen.Storage.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { StorageScreen(viewModel = viewModel, authManager = authManager, onNavigateBack = { navController.popBackStack() }, setTopBarActions = setTopBarActions) }

        composable(route = Screen.Warehouse.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            val isUserManager = remember(authState) {
                val name = authState.userName?.lowercase().orEmpty()
                authState.isAdmin || authState.role == UserRole.INVENTORY_MANAGER ||
                        name.contains("ситников") || name.contains("miha.sklad") || name.contains("nikasov")
            }
            WarehouseDashboardScreen(navController = navController, isAdmin = isUserManager, userRole = authState.role, userId = authState.userId ?: "")
        }

        composable(route = Screen.WarehouseCatalog.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            val warehouseViewModel: WarehouseViewModel = viewModel()
            val items by warehouseViewModel.items.collectAsState()
            val cart by warehouseViewModel.cart.collectAsState()
            val currentUserName = authState.userName ?: "Сотрудник"
            val currentUserId = authState.userId ?: ""
            val currentUserRole = authState.role.name
            val context = LocalContext.current
            val isUserManager = remember(authState) {
                val name = authState.userName?.lowercase().orEmpty()
                authState.isAdmin || authState.role == UserRole.INVENTORY_MANAGER ||
                        name.contains("ситников") || name.contains("miha.sklad") || name.contains("nikasov")
            }
            LaunchedEffect(Unit) {
                warehouseViewModel.uiEvents.collect { event ->
                    when (event) {
                        is WarehouseViewModel.UiEvent.ShowSnackbar -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                        is WarehouseViewModel.UiEvent.NavigateBack -> navController.popBackStack()
                    }
                }
            }
            WarehouseCatalogScreen(
                items = items, cart = cart,
                onAddToCart = { item, qty -> warehouseViewModel.onAddToCart(item, qty) },
                onRemoveFromCart = { id -> warehouseViewModel.onRemoveFromCart(id) },
                onSubmitOrder = { warehouseViewModel.onSubmitOrder(currentUserId, currentUserName, currentUserRole) },
                onNavigateToAddItem = { navController.navigate(Screen.WarehouseAddItem.route) },
                onTakeItem = { item, quantity -> warehouseViewModel.onTakeItem(item, quantity, currentUserName) },
                isAdmin = isUserManager,
                userRole = authState.role,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.WarehouseAddItem.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            val warehouseViewModel: WarehouseViewModel = viewModel()
            val items by warehouseViewModel.items.collectAsState()
            val dynamicCategories = remember(items) { (items.map { it.category } + listOf("Электроника", "Ходовая", "Механика", "Колеса", "Расходники")).distinct() }
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                warehouseViewModel.uiEvents.collect { event ->
                    when (event) {
                        is WarehouseViewModel.UiEvent.NavigateBack -> navController.popBackStack()
                        is WarehouseViewModel.UiEvent.ShowSnackbar -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            WarehouseAddItemScreen(
                existingCategories = dynamicCategories,
                onNavigateUp = { navController.popBackStack() },
                onItemCreated = { newItem ->
                    warehouseViewModel.onAddNewItem(
                        fullName = newItem.fullName, shortName = newItem.shortName,
                        sku = newItem.sku, category = newItem.category, unit = newItem.unit,
                        totalStock = newItem.totalStock
                    )
                }
            )
        }

        composable(route = Screen.TaskCreation.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { TaskCreationScreen(navController = navController) }

        composable(route = Screen.Chat.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            ChatScreen(
                viewModel = chatViewModel,
                onOpenDirectChat = { peerId, peerName, peerRole ->
                    val encodedName = URLEncoder.encode(peerName, "UTF-8")
                    val encodedRole = URLEncoder.encode(peerRole, "UTF-8")
                    navController.navigate("direct_chat/$peerId/$encodedName/$encodedRole")
                },
                onOpenProfile = { uid, name, role ->
                    val encodedName = URLEncoder.encode(name, "UTF-8")
                    val encodedRole = URLEncoder.encode(role, "UTF-8")
                    navController.navigate("user_profile/$uid/$encodedName/$encodedRole")
                },
                onOpenInbox = { navController.navigate(Screen.DirectInbox.route) { launchSingleTop = true } }
            )
        }

        composable(route = Screen.DirectInbox.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            DirectInboxScreen(
                onBack = { navController.popBackStack() },
                onOpenChat = { peerId, peerName, peerRole ->
                    val encodedName = URLEncoder.encode(peerName, "UTF-8")
                    val encodedRole = URLEncoder.encode(peerRole, "UTF-8")
                    navController.navigate("direct_chat/$peerId/$encodedName/$encodedRole")
                }
            )
        }

        composable(
            route = Screen.DirectChat.route,
            arguments = listOf(navArgument("peerId") { type = NavType.StringType }, navArgument("peerName") { type = NavType.StringType }, navArgument("peerRole") { type = NavType.StringType }),
            enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit
        ) { backStackEntry ->
            val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
            val peerName = URLDecoder.decode(backStackEntry.arguments?.getString("peerName") ?: "", "UTF-8")
            val peerRole = URLDecoder.decode(backStackEntry.arguments?.getString("peerRole") ?: "", "UTF-8")
            DirectChatScreen(
                peerId = peerId, peerName = peerName, peerRole = peerRole,
                onBack = { navController.popBackStack() },
                onOpenProfile = { uid, name, role ->
                    val encodedName = URLEncoder.encode(name, "UTF-8")
                    val encodedRole = URLEncoder.encode(role, "UTF-8")
                    navController.navigate("user_profile/$uid/$encodedName/$encodedRole")
                }
            )
        }

        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }, navArgument("userName") { type = NavType.StringType }, navArgument("userRole") { type = NavType.StringType }),
            enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val userName = URLDecoder.decode(backStackEntry.arguments?.getString("userName") ?: "", "UTF-8")
            val userRole = URLDecoder.decode(backStackEntry.arguments?.getString("userRole") ?: "", "UTF-8")
            UserProfileScreen(
                userId = userId, peerName = userName, peerRole = userRole,
                onBack = { navController.popBackStack() },
                onWriteDm = {
                    navController.popBackStack()
                    val encodedName = URLEncoder.encode(userName, "UTF-8")
                    val encodedRole = URLEncoder.encode(userRole, "UTF-8")
                    navController.navigate("direct_chat/$userId/$encodedName/$encodedRole") { launchSingleTop = true }
                }
            )
        }

        composable(route = Screen.DeliveryDashboard.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            DeliveryDashboardScreen(navController = navController, onMenuClick = onHomeClick)
        }

        composable(
            route = Screen.DeliveryForm.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType }),
            enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit
        ) { backStackEntry ->
            val deliveryType = backStackEntry.arguments?.getString("type") ?: "receive"
            val deliveryViewModel: DeliveryViewModel = hiltViewModel()
            val currentUserName = authState.userName ?: "Неизвестный сотрудник"
            DeliveryFormScreen(type = deliveryType, viewModel = deliveryViewModel, currentUserName = currentUserName, onNavigateBack = { navController.popBackStack() })
        }

        composable(route = Screen.DeliveryHistory.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            val deliveryViewModel: DeliveryViewModel = hiltViewModel()
            DeliveryHistoryScreen(viewModel = deliveryViewModel, onNavigateBack = { navController.popBackStack() })
        }

        // ── SECURITY ──────────────────────────────────────────────────────────
        composable(route = Screen.SecurityDashboard.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            val securityViewModel: SecurityViewModel = hiltViewModel()
            SecurityDashboardScreen(
                viewModel = securityViewModel,
                onNavigateToScooters = { navController.navigate(Screen.SecurityScooters.route) { launchSingleTop = true } },
                onNavigateToBattery = { navController.navigate(Screen.SecurityBattery.route) { launchSingleTop = true } },
                onNavigateToStorage = { navController.navigate(Screen.SecurityStorage.route) { launchSingleTop = true } },
                onMenuClick = onHomeClick,
                onNavigateToScanner = { navController.navigate(Screen.SecurityScanner.route) { launchSingleTop = true } }
            )
        }

        composable(route = Screen.SecurityScanner.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            val securityScannerViewModel: SecurityScannerViewModel = hiltViewModel()
            SecurityScannerScreen(
                viewModel = securityScannerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPassport = { scooterId ->
                    navController.popBackStack()
                    val encoded = URLEncoder.encode(scooterId, "UTF-8")
                    navController.navigate("scooter_passport/$encoded")
                }
            )
        }

        composable(route = Screen.SecurityScooters.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            val securityViewModel: SecurityViewModel = hiltViewModel()
            SecurityScootersScreen(
                viewModel = securityViewModel,
                onMenuClick = onHomeClick,
                onOpenPassport = { scooterId ->
                    val encoded = URLEncoder.encode(scooterId, "UTF-8")
                    navController.navigate("scooter_passport/$encoded")
                }
            )
        }

        composable(
            route = Screen.ScooterPassport.route,
            arguments = listOf(navArgument("scooterId") { type = NavType.StringType }),
            enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit
        ) { backStackEntry ->
            val securityViewModel: SecurityViewModel = hiltViewModel()
            val scooterId = URLDecoder.decode(backStackEntry.arguments?.getString("scooterId") ?: "", "UTF-8")
            ScooterPassportScreen(scooterId = scooterId, viewModel = securityViewModel, onBack = { navController.popBackStack() })
        }

        composable(route = Screen.SecurityBattery.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            val securityViewModel: SecurityViewModel = hiltViewModel()
            SecurityBatteryScreen(viewModel = securityViewModel, onMenuClick = onHomeClick)
        }
        composable(route = Screen.SecurityStorage.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            val securityViewModel: SecurityViewModel = hiltViewModel()
            SecurityStorageScreen(viewModel = securityViewModel, onMenuClick = onHomeClick)
        }
    }
}