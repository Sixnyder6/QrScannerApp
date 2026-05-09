package com.example.qrscannerapp

import com.example.qrscannerapp.features.street_doctor.ui.StreetDoctorHost
import com.example.qrscannerapp.features.street_doctor.ui.FieldRepairAdminScreen
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.common.ui.ChatNotification
import com.example.qrscannerapp.common.ui.ChatNotificationBanner
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
import com.example.qrscannerapp.features.chat.ui.DirectMessage
import com.example.qrscannerapp.features.chat.ui.UserProfileScreen
import com.example.qrscannerapp.features.electrician.ui.UnifiedSettingsScreen
import com.example.qrscannerapp.features.inventory.ui.Warehouse.WarehouseAddItemScreen
import com.example.qrscannerapp.features.inventory.ui.Warehouse.WarehouseDashboardScreen
import com.example.qrscannerapp.features.inventory.ui.Warehouse.WarehouseViewModel
import com.example.qrscannerapp.features.inventory.ui.Warehouse.components.WarehouseCatalogScreen
import com.example.qrscannerapp.features.inventory.ui.distribution.PalletDistributionScreen
import com.example.qrscannerapp.features.inventory.ui.storage.StorageScreen
import com.example.qrscannerapp.features.profile.ui.EmployeeProfileScreen
import com.example.qrscannerapp.features.tasks.ui.MyTasksScreen
import com.example.qrscannerapp.features.tasks.ui.creation.TaskCreationScreen
import com.example.qrscannerapp.features.tasks.ui.viewmodel.MyTasksViewModel
import com.example.qrscannerapp.features.team.ui.TeamScreen
import com.example.qrscannerapp.features.team.ui.EmployeeDetailScreen
import com.example.qrscannerapp.features.vehicle_report.ui.VehicleReportAnalyticsScreen
import com.example.qrscannerapp.features.vehicle_report.ui.VehicleReportHistoryScreen
import com.example.qrscannerapp.features.vehicle_report.ui.VehicleReportScreen
import com.example.qrscannerapp.features.interaction.ui.InteractionScreen
import com.example.qrscannerapp.features.delivery.ui.DeliveryDashboardScreen
import com.example.qrscannerapp.features.delivery.ui.DeliveryFormScreen
import com.example.qrscannerapp.features.delivery.ui.DeliveryHistoryScreen
import com.example.qrscannerapp.features.delivery.ui.DeliveryViewModel
import com.example.qrscannerapp.features.security.ui.SecurityViewModel
import com.example.qrscannerapp.features.security.ui.SecurityScootersScreen
import com.example.qrscannerapp.features.security.ui.ScooterPassportScreen
import com.example.qrscannerapp.features.security.ui.SecurityBatteryScreen
import com.example.qrscannerapp.features.security.ui.SecurityStorageScreen
import com.example.qrscannerapp.features.security.ui.SecurityDashboardScreen
import com.example.qrscannerapp.features.security.ui.scanner.SecurityScannerScreen
import com.example.qrscannerapp.features.security.ui.scanner.SecurityScannerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset

// ============================================================================================
// VISIONOS PRO — ЦВЕТА АКЦЕНТОВ И GLOW
// ============================================================================================
private val IconColorScanner     = Color(0xFF8B7FFF)
private val IconColorInteraction = Color(0xFF22D3EE)
private val IconColorTasks       = Color(0xFFFBBF24)
private val IconColorPallet      = Color(0xFF34D399)
private val IconColorStorage     = Color(0xFF22D3EE)
private val IconColorWarehouse   = Color(0xFFA78BFA)
private val IconColorDelivery    = Color(0xFF38BDF8)
private val IconColorDashboard   = Color(0xFFF472B6)
private val IconColorVehicle     = Color(0xFFFB923C)
private val IconColorQr          = Color(0xFF4ADE80)
private val IconColorSettings    = Color(0xFF94A3B8)
private val IconColorHistory     = Color(0xFFC084FC)
private val IconColorChat        = Color(0xFF2DD4BF)
private val IconColorTeam        = Color(0xFF2DD4BF)
private val IconColorSecurity    = Color(0xFFF87171)
private val IconColorFieldRepair = Color(0xFF818CF8)

private val DrawerSurface       = Color(0xFF111114)
private val DrawerSurfaceTop    = Color(0xFF1A1A22)
private val DrawerStroke        = Color(0xFFFFFFFF)

private fun chatRoomAccent(room: ChatRoom): Color = when (room) {
    ChatRoom.GENERAL  -> Color(0xFF6A5AE0)
    ChatRoom.MUVERS   -> Color(0xFF00BCD4)
    ChatRoom.MANAGERS -> Color(0xFF4CAF50)
    ChatRoom.SHIFTS   -> Color(0xFFFFA726)
    ChatRoom.ALERTS   -> Color(0xFFF44336)
}

// Squircle — настоящая iOS-форма (smoother чем RoundedCornerShape)
private val Squircle12 = androidx.compose.foundation.shape.GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = 12f * (size.minDimension / 100f).coerceAtLeast(0.3f)
    moveTo(0f, h * 0.5f)
    cubicTo(0f, r, r, 0f, w * 0.5f, 0f)
    cubicTo(w - r, 0f, w, r, w, h * 0.5f)
    cubicTo(w, h - r, w - r, h, w * 0.5f, h)
    cubicTo(r, h, 0f, h - r, 0f, h * 0.5f)
    close()
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Scanner           : Screen("scanner",                  "Сканер",            Icons.Outlined.DocumentScanner)
    object Interaction       : Screen("interaction",              "Взаимодействие",    Icons.Outlined.Handyman)
    object MyTasks           : Screen("my_tasks",                 "Мои Задачи",        Icons.Outlined.TaskAlt)
    object QrGenerator       : Screen("qr_generator",            "Генератор QR",      Icons.Outlined.QrCode2)
    object Settings          : Screen("settings",                 "Настройки",         Icons.Outlined.Settings)
    object Account           : Screen("account",                  "Аккаунт",           Icons.Outlined.AccountCircle)
    object Dashboard         : Screen("dashboard",                "Дашборд",           Icons.Outlined.Analytics)
    object VehicleReport     : Screen("vehicle_report",           "Сводка самокатов",  Icons.Outlined.Summarize)
    object VehicleReportHistory   : Screen("vehicle_report_history",    "История отчетов",  Icons.Outlined.History)
    object VehicleReportAnalytics : Screen("vehicle_report_analytics",  "Аналитика",        Icons.Outlined.Analytics)
    object History           : Screen("history",                  "История",           Icons.Outlined.History)
    object SessionDetail     : Screen("session_detail/{sessionId}","Детали сессии",    Icons.Outlined.ReceiptLong)
    object EmployeeProfile   : Screen("employee_profile/{userId}", "Профиль сотрудника",Icons.Outlined.Badge)
    object AdminRepairLog    : Screen("admin_repair_log",         "Журнал ремонтов",   Icons.Outlined.Build)
    object PalletDistribution: Screen("pallet_distribution",      "АКБ",               Icons.Outlined.Inventory)
    object Storage           : Screen("storage",                  "Самокаты",          Icons.Outlined.Inventory2)
    object Warehouse         : Screen("warehouse",                "Склад",             Icons.Outlined.Warehouse)
    object WarehouseCatalog  : Screen("warehouse_catalog",        "Каталог запчастей", Icons.AutoMirrored.Outlined.MenuBook)
    object WarehouseAddItem  : Screen("warehouse_add_item",       "Новая запчасть",    Icons.Default.Add)
    object TaskCreation      : Screen("task_creation",            "Создать Задачу",    Icons.Default.Add)
    object VisualRepair      : Screen("visual_repair/{scooterId}","3D Осмотр",         Icons.Outlined.Build)
    object Chat              : Screen("chat",                     "Чат",               Icons.AutoMirrored.Filled.Chat)
    object DirectChat        : Screen("direct_chat/{peerId}/{peerName}/{peerRole}", "Личные сообщения", Icons.AutoMirrored.Filled.Chat)
    object DirectInbox       : Screen("direct_inbox", "Личные сообщения", Icons.AutoMirrored.Filled.Chat)
    object UserProfile       : Screen("user_profile/{userId}/{userName}/{userRole}", "Профиль", Icons.Default.Person)
    object DeliveryDashboard : Screen("delivery_dashboard",       "Логистика",         Icons.Outlined.LocalShipping)
    object DeliveryForm      : Screen("delivery_form/{type}",     "Оформление",        Icons.Default.Add)
    object DeliveryHistory   : Screen("delivery_history",         "История доставок",  Icons.Outlined.History)
    object Team              : Screen("team",                     "Команда",           Icons.Outlined.Groups)
    object EmployeeDetail    : Screen("employee_detail/{userId}/{userName}/{userRole}", "Профиль сотрудника", Icons.Outlined.Badge)
    object SecurityDashboard : Screen("security_dashboard",       "Дашборд СБ",        Icons.Outlined.Shield)
    object SecurityScooters  : Screen("security_scooters",        "Самокаты СБ",       Icons.Outlined.DirectionsBike)
    object ScooterPassport   : Screen("scooter_passport/{scooterId}", "Паспорт",       Icons.Outlined.Badge)
    object SecurityBattery   : Screen("security_battery",         "АКБ СБ",            Icons.Outlined.BatteryAlert)
    object SecurityStorage   : Screen("security_storage",         "Склад СБ",          Icons.Outlined.Inventory)
    object SecurityScanner   : Screen("security_scanner",         "Сканер СБ",         Icons.Outlined.QrCodeScanner)
    object StreetDoctorTasks    : Screen("street_doctor_tasks",    "Задания",  Icons.Outlined.Build)
    object StreetDoctorPassport : Screen("street_doctor_passport", "Паспорт",  Icons.Outlined.Badge)
    object FieldRepairAdmin  : Screen("field_repair_admin",       "Полевой ремонт",    Icons.Outlined.DirectionsBike)
}

private fun iconColorFor(route: String): Color = when (route) {
    Screen.Scanner.route            -> IconColorScanner
    Screen.Interaction.route        -> IconColorInteraction
    Screen.MyTasks.route            -> IconColorTasks
    Screen.PalletDistribution.route -> IconColorPallet
    Screen.Storage.route            -> IconColorStorage
    Screen.Warehouse.route          -> IconColorWarehouse
    Screen.DeliveryDashboard.route  -> IconColorDelivery
    Screen.Dashboard.route          -> IconColorDashboard
    Screen.VehicleReport.route      -> IconColorVehicle
    Screen.QrGenerator.route        -> IconColorQr
    Screen.Settings.route           -> IconColorSettings
    Screen.History.route            -> IconColorHistory
    Screen.Chat.route               -> IconColorChat
    Screen.Team.route               -> IconColorTeam
    Screen.FieldRepairAdmin.route   -> IconColorFieldRepair
    Screen.SecurityDashboard.route  -> IconColorSecurity
    Screen.SecurityScooters.route   -> IconColorSecurity
    Screen.SecurityBattery.route    -> IconColorSecurity
    Screen.SecurityStorage.route    -> IconColorSecurity
    else                            -> StardustTextSecondary
}

private data class MenuGroup(val label: String, val screens: List<Screen>)

private fun buildSupervisorMenu(): List<Screen> {
    return listOf(
        Screen.Scanner,
        Screen.PalletDistribution,
        Screen.QrGenerator,
        Screen.Settings
    )
}

private fun buildMenuGroups(
    menuItems: List<Screen>,
    isUserManager: Boolean,
    isSupervisor: Boolean,
    isSecurity: Boolean,
    isStrictSecurity: Boolean,
    isTechnic: Boolean = false
): List<MenuGroup> {
    if (isTechnic) {
        return listOf(
            MenuGroup("Работа", listOf(Screen.StreetDoctorTasks)),
            MenuGroup("Склад", listOf(Screen.Warehouse)),
            MenuGroup("Прочее", listOf(Screen.Chat, Screen.Settings))
        )
    }
    if (isStrictSecurity) {
        val security = menuItems.filter { it.route in listOf(Screen.SecurityDashboard.route, Screen.SecurityScooters.route, Screen.SecurityBattery.route, Screen.SecurityStorage.route) }
        val team = menuItems.filter { it.route in listOf(Screen.Team.route, Screen.Chat.route) }
        val other = menuItems.filter { it.route in listOf(Screen.Settings.route) }
        return buildList {
            if (security.isNotEmpty()) add(MenuGroup("Служба безопасности", security))
            if (team.isNotEmpty()) add(MenuGroup("Команда", team))
            if (other.isNotEmpty()) add(MenuGroup("Прочее", other))
        }
    }
    if (isSupervisor) {
        return listOf(
            MenuGroup("Работа", listOf(Screen.Scanner, Screen.PalletDistribution)),
            MenuGroup("Инструменты", listOf(Screen.QrGenerator, Screen.Settings))
        )
    }

    val work       = menuItems.filter { it.route in listOf(Screen.Scanner.route, Screen.Interaction.route, Screen.MyTasks.route) }
    val warehouse  = menuItems.filter { it.route in listOf(Screen.PalletDistribution.route, Screen.Storage.route, Screen.Warehouse.route, Screen.DeliveryDashboard.route) }
    val management = menuItems.filter { it.route in listOf(Screen.Dashboard.route, Screen.VehicleReport.route, Screen.FieldRepairAdmin.route) }
    val team       = menuItems.filter { it.route in listOf(Screen.Team.route, Screen.Chat.route) }
    val security   = menuItems.filter { it.route in listOf(Screen.SecurityDashboard.route, Screen.SecurityScooters.route, Screen.SecurityBattery.route, Screen.SecurityStorage.route) }
    val other      = menuItems.filter { it.route in listOf(Screen.QrGenerator.route, Screen.Settings.route, Screen.History.route) }

    return buildList {
        if (work.isNotEmpty()) add(MenuGroup("Работа", work))
        if (warehouse.isNotEmpty()) add(MenuGroup("Склад", warehouse))
        if (management.isNotEmpty() && isUserManager) add(MenuGroup("Управление", management))
        if (team.isNotEmpty()) add(MenuGroup("Команда", team))
        if (security.isNotEmpty()) add(MenuGroup("Служба безопасности", security))
        if (other.isNotEmpty()) add(MenuGroup("Прочее", other))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    authManager: AuthManager,               // ← Hilt singleton из MainActivity
    telemetryManager: TelemetryManager,     // ← Hilt singleton из MainActivity
    initialRoute: String? = null,
    initialChatRoomId: String? = null
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(initialRoute) {
        if (initialRoute != null) {
            delay(400)
            when (initialRoute) {
                "chat"     -> navController.navigate(Screen.Chat.route) { launchSingleTop = true }
                "my_tasks" -> navController.navigate(Screen.MyTasks.route) { launchSingleTop = true }
                "scanner"  -> { }
            }
        }
    }

    val myTasksViewModel: MyTasksViewModel = hiltViewModel()
    val myTasksUiState by myTasksViewModel.uiState.collectAsState()
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val chatViewModel: ChatViewModel = hiltViewModel()
    val totalUnreadChat by chatViewModel.totalUnread.collectAsState()
    val chatLastMessage by chatViewModel.lastMessagePreview.collectAsState()
    val dmInboxViewModel: DirectInboxViewModel = hiltViewModel()
    val dmInboxState by dmInboxViewModel.uiState.collectAsState()

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
            currentChatNotification = ChatNotification(senderName = lastMsg.senderName, message = lastMsg.text.take(120), roomName = chatUiState.activeRoom.displayName, roomEmoji = chatUiState.activeRoom.emoji, accentColor = accent, isMention = isMention, id = lastMsg.timestamp?.time ?: System.currentTimeMillis())
        }
    }

    var currentDmNotification by remember { mutableStateOf<ChatNotification?>(null) }
    val lastSeenDmId = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(dmInboxState.items, currentRoute) {
        if (currentRoute == Screen.DirectChat.route || currentRoute == Screen.DirectInbox.route) { currentDmNotification = null; return@LaunchedEffect }
        val freshItem = dmInboxState.items.firstOrNull { it.unreadCount > 0 }
        if (freshItem != null) {
            val notifId = "${freshItem.conversationId}_${freshItem.lastTimestamp?.time}"
            if (notifId != lastSeenDmId.value) {
                lastSeenDmId.value = notifId
                currentDmNotification = ChatNotification(senderName = freshItem.peerName, message = freshItem.lastMessage.take(120), roomName = "Личное сообщение", roomEmoji = "💬", accentColor = when (freshItem.peerRole) { "admin" -> Color(0xFFEC407A); "inventory_manager" -> Color(0xFF4CAF50); "muver" -> Color(0xFF29B6F6); "electrician" -> Color(0xFFFFCA28); "technic" -> Color(0xFFAB47BC); else -> Color(0xFF78909C) }, isMention = false, id = freshItem.lastTimestamp?.time ?: System.currentTimeMillis())
            }
        }
    }

    LaunchedEffect(Unit) {
        dashboardViewModel.notificationEvent.collect { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
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
    var updateDismissed by remember { mutableStateOf(false) }
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

    val isSupervisor = authState.role == UserRole.SUPERVISOR
    val isSecurity   = authState.role == UserRole.SECURITY || authState.isAdmin
    val isStrictSecurity = authState.role == UserRole.SECURITY
    val isTechnic = authState.role == UserRole.TECHNIC

    val isUserManager = remember(authState) {
        val currentName = authState.userName?.lowercase() ?: ""
        authState.isAdmin ||
                authState.role == UserRole.INVENTORY_MANAGER ||
                currentName.contains("ситников") ||
                currentName.contains("miha.sklad") ||
                currentName.contains("nikasov")
    }

    val menuItems = remember(isUserManager, isSupervisor, isStrictSecurity, isSecurity, isTechnic) {
        if (isStrictSecurity) {
            listOf(Screen.SecurityDashboard, Screen.SecurityScooters, Screen.SecurityBattery, Screen.SecurityStorage, Screen.Team, Screen.Chat, Screen.Settings)
        } else if (isSupervisor) {
            buildSupervisorMenu()
        } else if (isTechnic) {
            listOf(Screen.StreetDoctorTasks, Screen.Warehouse, Screen.Chat, Screen.Settings)
        } else {
            mutableListOf<Screen>().apply {
                add(Screen.Scanner); add(Screen.Interaction); add(Screen.MyTasks)
                add(Screen.PalletDistribution); add(Screen.Storage); add(Screen.Warehouse)
                add(Screen.DeliveryDashboard)
                if (isUserManager) {
                    add(Screen.Dashboard)
                    add(Screen.VehicleReport)
                    add(Screen.FieldRepairAdmin)
                }
                if (isUserManager) add(Screen.Team)
                add(Screen.Chat)
                add(Screen.QrGenerator); add(Screen.Settings); add(Screen.History)
                if (isSecurity) {
                    add(Screen.SecurityDashboard); add(Screen.SecurityScooters)
                    add(Screen.SecurityBattery); add(Screen.SecurityStorage)
                }
            }.distinctBy { it.route }.sortedBy {
                when (it.route) {
                    Screen.Scanner.route -> 0; Screen.Interaction.route -> 1
                    Screen.MyTasks.route -> 2; Screen.PalletDistribution.route -> 3
                    Screen.Storage.route -> 4; Screen.Warehouse.route -> 5
                    Screen.DeliveryDashboard.route -> 6
                    Screen.Dashboard.route -> 7; Screen.VehicleReport.route -> 8
                    Screen.FieldRepairAdmin.route -> 9
                    Screen.Team.route -> 10
                    Screen.Chat.route -> 11
                    Screen.QrGenerator.route -> 12; Screen.Settings.route -> 13
                    Screen.History.route -> 14
                    Screen.SecurityDashboard.route -> 20
                    Screen.SecurityScooters.route  -> 21
                    Screen.SecurityBattery.route   -> 22
                    Screen.SecurityStorage.route   -> 23
                    else -> 15
                }
            }.filter {
                isUserManager || (it.route != Screen.Dashboard.route && it.route != Screen.VehicleReport.route && it.route != Screen.FieldRepairAdmin.route && it.route != Screen.Team.route)
            }
        }
    }

    val menuGroups = remember(menuItems, isUserManager, isSupervisor, isSecurity, isStrictSecurity, isTechnic) {
        buildMenuGroups(menuItems, isUserManager, isSupervisor, isSecurity, isStrictSecurity, isTechnic)
    }

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = currentRoute != Screen.VisualRepair.route,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = Color.Transparent,
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        DrawerSurfaceTop,
                                        DrawerSurface
                                    )
                                )
                            )
                            .drawBehind {
                                drawLine(
                                    color = DrawerStroke.copy(alpha = 0.06f),
                                    start = Offset(size.width - 0.5f, 0f),
                                    end = Offset(size.width - 0.5f, size.height),
                                    strokeWidth = 1f
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(IconColorScanner.copy(alpha = 0.12f), Color.Transparent),
                                        center = Offset(size.width, 0f),
                                        radius = size.width * 0.8f
                                    )
                                )
                            }
                    ) {
                        Column(modifier = Modifier.fillMaxHeight()) {
                            DrawerHeader(authState = authState, warehouseName = "Бестужевская 10")
                            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                menuGroups.forEach { group ->
                                    Text(text = group.label.uppercase(), color = StardustTextSecondary.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 4.dp))
                                    group.screens.forEach { screen ->
                                        val isSelected = currentRoute == screen.route
                                        val accentColor = iconColorFor(screen.route)
                                        NavigationDrawerItem(
                                            icon = {
                                                Box(
                                                    modifier = Modifier.size(40.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .matchParentSize()
                                                                .drawBehind {
                                                                    drawCircle(
                                                                        brush = Brush.radialGradient(
                                                                            listOf(
                                                                                accentColor.copy(alpha = 0.5f),
                                                                                accentColor.copy(alpha = 0.15f),
                                                                                Color.Transparent
                                                                            ),
                                                                            radius = size.maxDimension
                                                                        )
                                                                    )
                                                                }
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(Squircle12)
                                                            .background(
                                                                Brush.verticalGradient(
                                                                    if (isSelected) listOf(
                                                                        accentColor.copy(alpha = 0.45f),
                                                                        accentColor.copy(alpha = 0.2f)
                                                                    ) else listOf(
                                                                        accentColor.copy(alpha = 0.18f),
                                                                        accentColor.copy(alpha = 0.08f)
                                                                    )
                                                                )
                                                            )
                                                            .drawBehind {
                                                                drawLine(
                                                                    color = Color.White.copy(alpha = if (isSelected) 0.25f else 0.1f),
                                                                    start = Offset(size.width * 0.2f, 1.5f),
                                                                    end = Offset(size.width * 0.8f, 1.5f),
                                                                    strokeWidth = 1f
                                                                )
                                                                drawLine(
                                                                    color = accentColor.copy(alpha = if (isSelected) 0.7f else 0.25f),
                                                                    start = Offset(0f, size.height - 0.5f),
                                                                    end = Offset(size.width, size.height - 0.5f),
                                                                    strokeWidth = 1f
                                                                )
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            screen.icon,
                                                            contentDescription = screen.title,
                                                            tint = if (isSelected) Color.White else accentColor,
                                                            modifier = Modifier.size(19.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            label = {
                                                if (screen is Screen.Chat) {
                                                    Column {
                                                        Text(screen.title, color = if (isSelected) Color.White else StardustTextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                                                        if (chatLastMessage.isNotBlank()) { Text(chatLastMessage, color = StardustTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                                    }
                                                } else {
                                                    Text(screen.title, color = if (isSelected) Color.White else StardustTextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                                                }
                                            },
                                            badge = {
                                                if (screen is Screen.Chat && totalUnreadChat > 0) { Badge(containerColor = IconColorChat) { Text(if (totalUnreadChat > 99) "99+" else totalUnreadChat.toString(), color = Color.White, fontWeight = FontWeight.Bold) } }
                                                if (screen is Screen.Settings && updateState is UpdateState.UpdateAvailable) { Badge(containerColor = Color(0xFFFF5252)) { Text("!", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
                                                if (screen is Screen.MyTasks && myTasksUiState.activeTaskCount > 0) { Badge(containerColor = IconColorTasks) { Text(myTasksUiState.activeTaskCount.toString(), color = Color.White, fontWeight = FontWeight.Bold) } }
                                            },
                                            selected = isSelected,
                                            onClick = {
                                                hapticManager.performClick(hapticFeedback, scope)
                                                scope.launch { drawerState.close() }
                                                if (currentRoute != screen.route) {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.startDestinationId)
                                                        launchSingleTop = true
                                                    }
                                                }
                                            },
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 1.dp),
                                            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = iconColorFor(screen.route).copy(alpha = 0.25f), unselectedContainerColor = Color.Transparent)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            // ← telemetryManager передаётся как параметр, не создаётся заново
                            DrawerFooter(
                                authState = authState,
                                appVersionName = appVersionName,
                                telemetryManager = telemetryManager,
                                onNavigateToAccount = { scope.launch { drawerState.close() }; navController.navigate(Screen.Account.route) }
                            )
                        }
                    }
                }
            ) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = Color.Transparent,
                    topBar = {
                        val allScreens = menuItems + listOf(
                            Screen.Account, Screen.SessionDetail, Screen.Dashboard,
                            Screen.EmployeeProfile, Screen.AdminRepairLog, Screen.PalletDistribution,
                            Screen.Storage, Screen.MyTasks, Screen.TaskCreation,
                            Screen.VehicleReport, Screen.VehicleReportHistory, Screen.VehicleReportAnalytics,
                            Screen.VisualRepair, Screen.Warehouse, Screen.WarehouseCatalog,
                            Screen.WarehouseAddItem, Screen.Interaction, Screen.Chat,
                            Screen.DeliveryDashboard, Screen.DeliveryForm, Screen.DeliveryHistory,
                            Screen.Team, Screen.EmployeeDetail,
                            Screen.SecurityDashboard, Screen.SecurityScooters,
                            Screen.SecurityBattery, Screen.SecurityStorage, Screen.ScooterPassport,
                            Screen.FieldRepairAdmin
                        )
                        val currentScreen = allScreens.find { it.route == currentRoute }
                        val topBarTitle = currentScreen?.title ?: ""

                        val screensWithoutMainTopBar = listOf(
                            Screen.Scanner.route, Screen.VisualRepair.route,
                            Screen.WarehouseAddItem.route, Screen.WarehouseCatalog.route,
                            Screen.StreetDoctorTasks.route,
                            Screen.Storage.route, Screen.Interaction.route,
                            Screen.Chat.route,
                            Screen.DirectChat.route,
                            Screen.UserProfile.route,
                            Screen.DeliveryDashboard.route, Screen.DeliveryForm.route,
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

                        if (currentRoute !in screensWithoutMainTopBar) {
                            TopAppBar(
                                title = { Text(topBarTitle) },
                                navigationIcon = {
                                    val backButtonScreens = listOf(
                                        Screen.EmployeeProfile.route, Screen.AdminRepairLog.route,
                                        Screen.SessionDetail.route, Screen.TaskCreation.route,
                                        Screen.VehicleReportHistory.route, Screen.VehicleReportAnalytics.route
                                    )
                                    if (currentRoute in backButtonScreens) {
                                        IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
                                    } else {
                                        IconButton(onClick = { hapticManager.performClick(hapticFeedback, scope); scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, contentDescription = "Меню") }
                                    }
                                },
                                actions = { topBarActions() },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.3f), titleContentColor = StardustTextPrimary, navigationIconContentColor = StardustTextPrimary, actionIconContentColor = StardustTextPrimary)
                            )
                        }
                    }
                ) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        onMenuClick = { if (!isTechnic) { hapticManager.performClick(hapticFeedback, scope); scope.launch { drawerState.open() } } },
                        updateManager = updateManager,
                        appVersion = detailedAppVersion,
                        hapticManager = hapticManager,
                        view = view,
                        authManager = authManager,
                        setTopBarActions = { actions -> topBarActions = actions },
                        isUserManager = isUserManager,
                        chatViewModel = chatViewModel,
                        isSupervisor = isSupervisor,
                        isSecurity = isSecurity,
                        isStrictSecurity = isStrictSecurity,
                        isTechnic = isTechnic
                    )
                }
            }

            ChatNotificationBanner(notification = currentChatNotification, onDismiss = { currentChatNotification = null }, onTap = { navController.navigate(Screen.Chat.route) { launchSingleTop = true } }, modifier = Modifier.align(Alignment.TopCenter))

            if (currentChatNotification == null) {
                ChatNotificationBanner(
                    notification = currentDmNotification,
                    onDismiss = { currentDmNotification = null },
                    onTap = {
                        val item = dmInboxState.items.firstOrNull { it.unreadCount > 0 }
                        if (item != null) {
                            val encodedName = java.net.URLEncoder.encode(item.peerName, "UTF-8")
                            val encodedRole = java.net.URLEncoder.encode(item.peerRole, "UTF-8")
                            navController.navigate("direct_chat/${item.peerId}/$encodedName/$encodedRole") { launchSingleTop = true }
                        }
                        currentDmNotification = null
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            // --- Оверлей обновления (блокирует интерфейс) ---
            if (updateState is UpdateState.UpdateAvailable && !updateDismissed) {
                val info = (updateState as UpdateState.UpdateAvailable).info
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(200f)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                        border = BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(StardustPrimary.copy(alpha = 0.7f), StardustPrimary.copy(alpha = 0.15f))
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(StardustPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = StardustPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Доступно обновление",
                                color = StardustTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                info.latestVersionName,
                                color = StardustTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            )
                            if (!info.releaseNotes.isNullOrBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    info.releaseNotes,
                                    color = StardustTextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { updateManager.startUpdate(info) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
                            ) {
                                Icon(
                                    Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = info.apkSize?.let { "Обновить ($it)" } ?: "Обновить",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            TextButton(
                                onClick = { updateDismissed = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Позже",
                                    color = StardustTextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // --- Баннер прогресса загрузки ---
            if (updateState is UpdateState.Downloading) {
                val progress = (updateState as UpdateState.Downloading).progress
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(200f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                        border = BorderStroke(1.dp, StardustPrimary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { progress / 100f },
                                    modifier = Modifier.size(32.dp),
                                    color = StardustPrimary,
                                    strokeWidth = 3.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Загрузка обновления...",
                                        color = StardustTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "$progress%",
                                        color = StardustPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = StardustPrimary,
                                trackColor = StardustItemBg
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerHeader(authState: AuthState, warehouseName: String) {
    var currentDate by remember { mutableStateOf("") }
    var weather by remember { mutableStateOf<WeatherData?>(null) }

    LaunchedEffect(Unit) {
        val dateFmt = SimpleDateFormat("EEE, d MMM", Locale("ru"))
        while (true) {
            currentDate = dateFmt.format(Date())
            delay(60_000)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            weather = WeatherRepo.load()
            delay(30 * 60 * 1000L) // обновление раз в 30 минут
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E1B4B).copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(IconColorScanner.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.7f),
                        radius = size.maxDimension * 0.6f
                    )
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1f
                )
            }
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column {
            // Склад
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = StardustPrimary.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = warehouseName, color = StardustPrimary.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Погода
            if (weather != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = weather!!.emoji, fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${weather!!.tempC}°C",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1).sp,
                                modifier = Modifier.drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            listOf(StardustPrimary.copy(alpha = 0.25f), Color.Transparent),
                                            radius = size.maxDimension
                                        )
                                    )
                                }
                            )
                            Text(
                                text = weather!!.description,
                                color = StardustTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Text(
                        text = currentDate,
                        color = StardustTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            } else {
                // Пока грузится — показываем дату
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "—°C",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = currentDate,
                        color = StardustTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            // Роль
            if (authState.isLoggedIn) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = StardustPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = authState.role.displayName.uppercase(),
                        color = StardustPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun DrawerFooter(authState: AuthState, appVersionName: String, telemetryManager: TelemetryManager, onNavigateToAccount: () -> Unit) {
    val userName = authState.userName ?: "Пользователь"
    val photoUrl = remember(userName) { getEmployeePhotoUrl(userName) }
    var pingText by remember { mutableStateOf("...") }
    var batteryLevel by remember { mutableStateOf(0) }
    var isCharging by remember { mutableStateOf(false) }
    var networkState by remember { mutableStateOf("...") }
    LaunchedEffect(Unit) { batteryLevel = telemetryManager.getBatteryLevel(); isCharging = telemetryManager.isCharging(); networkState = telemetryManager.getNetworkState(); pingText = telemetryManager.getNetworkPing() }
    LaunchedEffect(Unit) { while (true) { delay(30_000); pingText = telemetryManager.getNetworkPing(); batteryLevel = telemetryManager.getBatteryLevel(); isCharging = telemetryManager.isCharging() } }
    val pingColor = remember(pingText) { when { pingText == "..." || pingText == "Offline" || pingText == "Timeout" -> Color(0xFFF44336); pingText.replace(" ms", "").toIntOrNull()?.let { it < 100 } == true -> Color(0xFF4CAF50); pingText.replace(" ms", "").toIntOrNull()?.let { it < 300 } == true -> Color(0xFFFFC107); else -> Color(0xFFF44336) } }
    val batteryColor = when { isCharging -> Color(0xFF4CAF50); batteryLevel > 50 -> Color(0xFF4CAF50); batteryLevel > 20 -> Color(0xFFFFC107); else -> Color(0xFFF44336) }
    val batteryIcon = when { isCharging -> Icons.Default.BatteryChargingFull; batteryLevel > 80 -> Icons.Default.BatteryFull; batteryLevel > 50 -> Icons.Default.Battery5Bar; batteryLevel > 20 -> Icons.Default.Battery3Bar; else -> Icons.Default.Battery1Bar }
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.03f)
                        )
                    )
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isOnline = pingText != "Offline" && pingText != "Timeout" && pingText != "..."
                    if (isOnline) { val infiniteTransition = rememberInfiniteTransition(label = "ping_pulse"); val alpha by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulse"); Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(pingColor.copy(alpha = alpha))) } else { Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFF44336))) }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = if (pingText == "...") "—" else pingText, color = pingColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("·", color = StardustTextSecondary.copy(alpha = 0.4f), fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(batteryIcon, null, tint = batteryColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = if (batteryLevel > 0) "$batteryLevel%" else "—", color = batteryColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("·", color = StardustTextSecondary.copy(alpha = 0.4f), fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = when { networkState.contains("WiFi") -> Icons.Default.Wifi; networkState.contains("Cellular") -> Icons.Default.SignalCellularAlt; else -> Icons.Default.WifiOff }, contentDescription = null, tint = if (networkState == "Offline") Color(0xFFF44336) else StardustTextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = networkState.replace("Cellular ", "").take(6), color = StardustTextSecondary, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = appVersionName, color = StardustTextSecondary.copy(alpha = 0.4f), fontSize = 11.sp)
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
        if (authState.isLoggedIn) {
            Surface(onClick = onNavigateToAccount, color = Color.Transparent, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), shape = RoundedCornerShape(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).border(1.5.dp, Brush.linearGradient(listOf(StardustPrimary, StardustPrimary.copy(alpha = 0.3f))), CircleShape)) {
                        if (photoUrl != null) { AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(photoUrl).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                        else { Icon(Icons.Default.Person, null, modifier = Modifier.size(22.dp).align(Alignment.Center), tint = StardustTextSecondary) }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = userName, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        Text(text = authState.role.displayName, color = StardustTextSecondary, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = StardustTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                }
            }
        } else {
            TextButton(onClick = onNavigateToAccount, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.AccountCircle, null, tint = StardustTextSecondary); Spacer(Modifier.width(8.dp)); Text("Войти в аккаунт", color = StardustTextSecondary) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit,
    updateManager: UpdateManager,
    appVersion: String,
    hapticManager: HapticFeedbackManager,
    view: View,
    authManager: AuthManager,
    setTopBarActions: (@Composable RowScope.() -> Unit) -> Unit,
    isUserManager: Boolean,
    chatViewModel: ChatViewModel,
    isSupervisor: Boolean,
    isSecurity: Boolean,
    isStrictSecurity: Boolean,
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

    NavHost(
        navController = navController,
        startDestination = if (isTechnic) Screen.StreetDoctorTasks.route else Screen.Scanner.route,
        modifier = modifier
    ) {

        composable(route = Screen.Scanner.route, enterTransition = modalEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = modalExit) {
            if (isStrictSecurity) {
                LaunchedEffect(Unit) { navController.navigate(Screen.SecurityDashboard.route) { popUpTo(Screen.Scanner.route) { inclusive = true }; launchSingleTop = true } }
            } else if (isTechnic) {
                LaunchedEffect(Unit) { navController.navigate(Screen.StreetDoctorTasks.route) { popUpTo(Screen.Scanner.route) { inclusive = true }; launchSingleTop = true } }
            } else {
                StardustScreen(viewModel = viewModel, onMenuClick = onMenuClick, hapticManager = hapticManager, view = view,
                    onNavigateToPalletDistribution = { viewModel.onNavigateToPalletDistribution(); navController.navigate(Screen.PalletDistribution.route) },
                    onNavigateToStorage = { if (!isSupervisor) navController.navigate(Screen.Storage.route) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) { launchSingleTop = true } },
                    onNavigateToVisualRepair = { scooterId -> navController.navigate(Screen.VisualRepair.route.replace("{scooterId}", scooterId)) })
            }
        }

        composable(route = Screen.PalletDistribution.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
            PalletDistributionScreen(viewModel = viewModel, authManager = authManager, onNavigateBack = { navController.popBackStack() })
        }

        composable(route = Screen.QrGenerator.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { QrGeneratorScreen() }
        composable(route = Screen.Settings.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { UnifiedSettingsScreen(authManager = authManager) }
        composable(route = Screen.Account.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { AccountScreen(authManager = authManager) }

        if (isTechnic) {
            composable(route = Screen.StreetDoctorTasks.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
                val currentUserName = authState.userName ?: "Техник"
                val currentUserInitials = authState.userName?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString("") ?: "ТХ"
                StreetDoctorHost(
                    currentUserName     = currentUserName,
                    currentUserInitials = currentUserInitials,
                    isOnShift           = authState.isShiftActive,
                    userRole            = authState.role.displayName,
                    onLogout            = { authManager.logout() }
                )
            }
        }

        if (!isSupervisor) {

            composable(route = Screen.Interaction.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { InteractionScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(route = Screen.MyTasks.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { MyTasksScreen(onMenuClick = onMenuClick, onTaskClick = { }) }

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

            composable(
                route = Screen.FieldRepairAdmin.route,
                enterTransition = iosEnter, exitTransition = iosExit,
                popEnterTransition = iosPopEnter, popExitTransition = iosPopExit
            ) {
                FieldRepairAdminScreen(
                    onBack = { navController.popBackStack() },
                    onOpenImport = { }
                )
            }

            composable(route = Screen.Team.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
                TeamScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProfile = { userId, userName, userRole -> val encodedName = URLEncoder.encode(userName, "UTF-8"); val encodedRole = URLEncoder.encode(userRole, "UTF-8"); navController.navigate("employee_detail/$userId/$encodedName/$encodedRole") },
                    onNavigateToDirectChat = { peerId, peerName, peerRole -> val encodedName = URLEncoder.encode(peerName, "UTF-8"); val encodedRole = URLEncoder.encode(peerRole, "UTF-8"); navController.navigate("direct_chat/$peerId/$encodedName/$encodedRole") },
                    isAdmin = isUserManager
                )
            }

            composable(route = Screen.EmployeeDetail.route, arguments = listOf(navArgument("userId") { type = NavType.StringType }, navArgument("userName") { type = NavType.StringType }, navArgument("userRole") { type = NavType.StringType }), enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val userName = URLDecoder.decode(backStackEntry.arguments?.getString("userName") ?: "", "UTF-8")
                val userRole = URLDecoder.decode(backStackEntry.arguments?.getString("userRole") ?: "", "UTF-8")
                EmployeeDetailScreen(userId = userId, userName = userName, userRole = userRole, isAdmin = isUserManager, onBack = { navController.popBackStack() }, onWriteDm = { navController.popBackStack(); val encodedName = URLEncoder.encode(userName, "UTF-8"); val encodedRole = URLEncoder.encode(userRole, "UTF-8"); navController.navigate("direct_chat/$userId/$encodedName/$encodedRole") { launchSingleTop = true } })
            }

            composable(route = Screen.VehicleReport.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { VehicleReportScreen(onNavigateToHistory = { navController.navigate(Screen.VehicleReportHistory.route) }) }
            composable(route = Screen.VehicleReportHistory.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { VehicleReportHistoryScreen(onNavigateToAnalytics = { navController.navigate(Screen.VehicleReportAnalytics.route) }) }
            composable(route = Screen.VehicleReportAnalytics.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { VehicleReportAnalyticsScreen() }
            composable(route = Screen.History.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { HistoryScreen(viewModel = viewModel, navController = navController) }
            composable(route = Screen.SessionDetail.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { backStackEntry -> val sessionId = backStackEntry.arguments?.getString("sessionId"); if (sessionId != null) SessionDetailScreen(sessionId = sessionId, viewModel = viewModel, navController = navController) else PlaceholderScreen(text = "Ошибка: ID сессии не найден") }
            composable(route = Screen.EmployeeProfile.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { EmployeeProfileScreen() }
            composable(route = Screen.AdminRepairLog.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { AdminRepairLogScreen() }

            composable(route = Screen.Storage.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { StorageScreen(viewModel = viewModel, authManager = authManager, onNavigateBack = { navController.popBackStack() }, setTopBarActions = setTopBarActions) }

            composable(route = Screen.Warehouse.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { WarehouseDashboardScreen(navController = navController, isAdmin = isUserManager, userRole = authState.role, userId = authState.userId ?: "") }

            composable(route = Screen.WarehouseCatalog.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
                val warehouseViewModel: WarehouseViewModel = viewModel()
                val items by warehouseViewModel.items.collectAsState()
                val cart by warehouseViewModel.cart.collectAsState()
                val currentUserName = authState.userName ?: "Сотрудник"
                val currentUserId = authState.userId ?: ""
                val currentUserRole = authState.role.name
                val context = LocalContext.current
                LaunchedEffect(Unit) { warehouseViewModel.uiEvents.collect { event -> when (event) { is WarehouseViewModel.UiEvent.ShowSnackbar -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show(); is WarehouseViewModel.UiEvent.NavigateBack -> navController.popBackStack() } } }
                WarehouseCatalogScreen(items = items, cart = cart, onAddToCart = { item, qty -> warehouseViewModel.onAddToCart(item, qty) }, onRemoveFromCart = { id -> warehouseViewModel.onRemoveFromCart(id) }, onSubmitOrder = { warehouseViewModel.onSubmitOrder(currentUserId, currentUserName, currentUserRole) }, onNavigateToAddItem = { navController.navigate(Screen.WarehouseAddItem.route) }, onTakeItem = { item, quantity -> warehouseViewModel.onTakeItem(item, quantity, currentUserName) }, isAdmin = isUserManager, userRole = authState.role, onNavigateBack = { navController.popBackStack() })
            }

            composable(route = Screen.WarehouseAddItem.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
                val warehouseViewModel: WarehouseViewModel = viewModel()
                val items by warehouseViewModel.items.collectAsState()
                val dynamicCategories = remember(items) { (items.map { it.category } + listOf("Электроника", "Ходовая", "Механика", "Колеса", "Расходники")).distinct() }
                val context = LocalContext.current
                LaunchedEffect(Unit) { warehouseViewModel.uiEvents.collect { event -> when (event) { is WarehouseViewModel.UiEvent.NavigateBack -> navController.popBackStack(); is WarehouseViewModel.UiEvent.ShowSnackbar -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show() } } }
                WarehouseAddItemScreen(existingCategories = dynamicCategories, onNavigateUp = { navController.popBackStack() }, onItemCreated = { newItem -> warehouseViewModel.onAddNewItem(fullName = newItem.fullName, shortName = newItem.shortName, sku = newItem.sku, category = newItem.category, unit = newItem.unit, totalStock = newItem.totalStock) })
            }

            composable(route = Screen.TaskCreation.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { TaskCreationScreen(navController = navController) }

            composable(route = Screen.Chat.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
                ChatScreen(viewModel = chatViewModel, onBack = { navController.popBackStack() }, onOpenDirectChat = { peerId, peerName, peerRole -> val encodedName = URLEncoder.encode(peerName, "UTF-8"); val encodedRole = URLEncoder.encode(peerRole, "UTF-8"); navController.navigate("direct_chat/$peerId/$encodedName/$encodedRole") }, onOpenProfile = { uid, name, role -> val encodedName = URLEncoder.encode(name, "UTF-8"); val encodedRole = URLEncoder.encode(role, "UTF-8"); navController.navigate("user_profile/$uid/$encodedName/$encodedRole") }, onOpenInbox = { navController.navigate(Screen.DirectInbox.route) { launchSingleTop = true } })
            }

            composable(route = Screen.DirectInbox.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { DirectInboxScreen(onBack = { navController.popBackStack() }, onOpenChat = { peerId, peerName, peerRole -> val encodedName = URLEncoder.encode(peerName, "UTF-8"); val encodedRole = URLEncoder.encode(peerRole, "UTF-8"); navController.navigate("direct_chat/$peerId/$encodedName/$encodedRole") }) }

            composable(route = Screen.DirectChat.route, arguments = listOf(navArgument("peerId") { type = NavType.StringType }, navArgument("peerName") { type = NavType.StringType }, navArgument("peerRole") { type = NavType.StringType }), enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { backStackEntry ->
                val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
                val peerName = URLDecoder.decode(backStackEntry.arguments?.getString("peerName") ?: "", "UTF-8")
                val peerRole = URLDecoder.decode(backStackEntry.arguments?.getString("peerRole") ?: "", "UTF-8")
                DirectChatScreen(peerId = peerId, peerName = peerName, peerRole = peerRole, onBack = { navController.popBackStack() }, onOpenProfile = { uid, name, role -> val encodedName = URLEncoder.encode(name, "UTF-8"); val encodedRole = URLEncoder.encode(role, "UTF-8"); navController.navigate("user_profile/$uid/$encodedName/$encodedRole") })
            }

            composable(route = Screen.UserProfile.route, arguments = listOf(navArgument("userId") { type = NavType.StringType }, navArgument("userName") { type = NavType.StringType }, navArgument("userRole") { type = NavType.StringType }), enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val userName = URLDecoder.decode(backStackEntry.arguments?.getString("userName") ?: "", "UTF-8")
                val userRole = URLDecoder.decode(backStackEntry.arguments?.getString("userRole") ?: "", "UTF-8")
                UserProfileScreen(userId = userId, peerName = userName, peerRole = userRole, onBack = { navController.popBackStack() }, onWriteDm = { navController.popBackStack(); val encodedName = URLEncoder.encode(userName, "UTF-8"); val encodedRole = URLEncoder.encode(userRole, "UTF-8"); navController.navigate("direct_chat/$userId/$encodedName/$encodedRole") { launchSingleTop = true } })
            }

            composable(route = Screen.DeliveryDashboard.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { DeliveryDashboardScreen(navController = navController, onMenuClick = onMenuClick) }

            composable(route = Screen.DeliveryForm.route, arguments = listOf(navArgument("type") { type = NavType.StringType }), enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { backStackEntry ->
                val deliveryType = backStackEntry.arguments?.getString("type") ?: "receive"
                val deliveryViewModel: DeliveryViewModel = hiltViewModel()
                val currentUserName = authState.userName ?: "Неизвестный сотрудник"
                DeliveryFormScreen(type = deliveryType, viewModel = deliveryViewModel, currentUserName = currentUserName, onNavigateBack = { navController.popBackStack() })
            }

            composable(route = Screen.DeliveryHistory.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
                val deliveryViewModel: DeliveryViewModel = hiltViewModel()
                DeliveryHistoryScreen(viewModel = deliveryViewModel, onNavigateBack = { navController.popBackStack() })
            }

            if (isSecurity) {
                composable(route = Screen.SecurityDashboard.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
                    val securityViewModel: SecurityViewModel = hiltViewModel()
                    SecurityDashboardScreen(viewModel = securityViewModel, onNavigateToScooters = { navController.navigate(Screen.SecurityScooters.route) { launchSingleTop = true } }, onNavigateToBattery = { navController.navigate(Screen.SecurityBattery.route) { launchSingleTop = true } }, onNavigateToStorage = { navController.navigate(Screen.SecurityStorage.route) { launchSingleTop = true } }, onMenuClick = onMenuClick, onNavigateToScanner = { navController.navigate(Screen.SecurityScanner.route) { launchSingleTop = true } })
                }

                composable(route = Screen.SecurityScanner.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
                    val securityScannerViewModel: SecurityScannerViewModel = hiltViewModel()
                    SecurityScannerScreen(viewModel = securityScannerViewModel, onNavigateBack = { navController.popBackStack() }, onNavigateToPassport = { scooterId -> navController.popBackStack(); val encoded = java.net.URLEncoder.encode(scooterId, "UTF-8"); navController.navigate("scooter_passport/$encoded") })
                }

                composable(route = Screen.SecurityScooters.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) {
                    val securityViewModel: SecurityViewModel = hiltViewModel()
                    SecurityScootersScreen(viewModel = securityViewModel, onMenuClick = onMenuClick, onOpenPassport = { scooterId -> val encoded = java.net.URLEncoder.encode(scooterId, "UTF-8"); navController.navigate("scooter_passport/$encoded") })
                }

                composable(route = Screen.ScooterPassport.route, arguments = listOf(navArgument("scooterId") { type = NavType.StringType }), enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { backStackEntry ->
                    val securityViewModel: SecurityViewModel = hiltViewModel()
                    val scooterId = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("scooterId") ?: "", "UTF-8")
                    ScooterPassportScreen(scooterId = scooterId, viewModel = securityViewModel, onBack = { navController.popBackStack() })
                }

                composable(route = Screen.SecurityBattery.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { val securityViewModel: SecurityViewModel = hiltViewModel(); SecurityBatteryScreen(viewModel = securityViewModel, onMenuClick = onMenuClick) }
                composable(route = Screen.SecurityStorage.route, enterTransition = iosEnter, exitTransition = iosExit, popEnterTransition = iosPopEnter, popExitTransition = iosPopExit) { val securityViewModel: SecurityViewModel = hiltViewModel(); SecurityStorageScreen(viewModel = securityViewModel, onMenuClick = onMenuClick) }
            }
        }
    }
}