// Полная, измененная версия файла AppNavigation.kt

package com.example.qrscannerapp
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.qrscannerapp.features.electrician.ui.UnifiedSettingsScreen
import com.example.qrscannerapp.features.inventory.ui.distribution.PalletDistributionScreen
import com.example.qrscannerapp.features.inventory.ui.storage.StorageScreen
import com.example.qrscannerapp.features.profile.ui.EmployeeProfileScreen
import com.example.qrscannerapp.features.tasks.ui.MyTasksScreen
import com.example.qrscannerapp.features.tasks.ui.creation.TaskCreationScreen
import com.example.qrscannerapp.features.tasks.ui.viewmodel.MyTasksViewModel
import com.example.qrscannerapp.features.vehicle_report.ui.VehicleReportAnalyticsScreen
import com.example.qrscannerapp.features.vehicle_report.ui.VehicleReportHistoryScreen
import com.example.qrscannerapp.features.vehicle_report.ui.VehicleReportScreen
import kotlinx.coroutines.launch


sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Scanner : Screen("scanner", "Сканер", Icons.Outlined.DocumentScanner)
    object MyTasks : Screen("my_tasks", "Мои Задачи", Icons.Outlined.TaskAlt)
    object QrGenerator : Screen("qr_generator", "Генератор QR", Icons.Outlined.QrCode2)
    object Settings : Screen("settings", "Настройки", Icons.Outlined.Settings)
    object Account : Screen("account", "Аккаунт", Icons.Outlined.AccountCircle)
    object Dashboard : Screen("dashboard", "Дашборд", Icons.Outlined.Analytics)
    object VehicleReport : Screen("vehicle_report", "Сводка самокатов", Icons.Outlined.Summarize)
    object VehicleReportHistory : Screen("vehicle_report_history", "История отчетов", Icons.Outlined.History)
    object VehicleReportAnalytics : Screen("vehicle_report_analytics", "Аналитика", Icons.Outlined.Analytics)
    object History : Screen("history", "История", Icons.Outlined.History)
    object SessionDetail : Screen("session_detail/{sessionId}", "Детали сессии", Icons.Outlined.ReceiptLong)
    object EmployeeProfile : Screen("employee_profile/{userId}", "Профиль сотрудника", Icons.Outlined.Badge)
    object AdminRepairLog : Screen("admin_repair_log", "Журнал ремонтов", Icons.Outlined.Build)
    object PalletDistribution : Screen("pallet_distribution", "Приемка", Icons.Outlined.Inventory)
    object Storage : Screen("storage", "Хранение", Icons.Outlined.Inventory2)
    object TaskCreation : Screen("task_creation", "Создать Задачу", Icons.Default.Add)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val myTasksViewModel: MyTasksViewModel = hiltViewModel()
    val myTasksUiState by myTasksViewModel.uiState.collectAsState()

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
        } catch (e: Exception) {
            "v?" to "v? (?)"
        }
    }

    val updateManager = remember { UpdateManager(context) }
    val updateState by updateManager.updateState.collectAsState()
    LaunchedEffect(Unit) {
        updateManager.checkForUpdates()
    }

    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    val settingsManager = remember { SettingsManager(context) }
    val hapticManager = remember { HapticFeedbackManager(settingsManager) }

    val authManager = remember { AuthManager(context) }
    val authState by authManager.authState.collectAsState()

    // V-- НАЧАЛО ИЗМЕНЕНИЙ (Блок 1 из 3) --V
    // 1. Создаем состояние, которое будет хранить Composable-блок с кнопками для TopAppBar.
    // По умолчанию оно пустое.
    var topBarActions: @Composable RowScope.() -> Unit by remember { mutableStateOf({}) }

    // 2. При смене экрана (currentRoute) мы сбрасываем это состояние до пустого,
    // чтобы кнопки со старого экрана не "переезжали" на новый.
    LaunchedEffect(currentRoute) {
        topBarActions = {}
    }
    // ^-- КОНЕЦ ИЗМЕНЕНИЙ (Блок 1 из 3) --^


    val menuItems = remember(authState.isAdmin) {
        val itemsList = mutableListOf<Screen>()
        itemsList.add(Screen.Scanner)
        itemsList.add(Screen.MyTasks)
        itemsList.add(Screen.PalletDistribution)
        itemsList.add(Screen.Storage)

        if (authState.isAdmin) {
            itemsList.add(Screen.Dashboard)
            itemsList.add(Screen.VehicleReport)
        }

        itemsList.add(Screen.QrGenerator)
        itemsList.add(Screen.Settings)
        itemsList.add(Screen.History)

        itemsList.toList()
            .distinctBy { it.route }
            .sortedBy {
                when(it.route) {
                    Screen.Scanner.route -> 0
                    Screen.MyTasks.route -> 1
                    Screen.PalletDistribution.route -> 2
                    Screen.Storage.route -> 3
                    Screen.Dashboard.route -> 4
                    Screen.VehicleReport.route -> 5
                    Screen.QrGenerator.route -> 6
                    else -> 10
                }
            }
            .filter { authState.isAdmin || (it.route != Screen.Dashboard.route && it.route != Screen.VehicleReport.route) }
    }

    AppBackground {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color.Transparent,
                    modifier = Modifier.background(Brush.verticalGradient(
                        colors = listOf(Color(0xFF181818), Color(0xFF383838))
                    ))
                ) {
                    Column(modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.height(12.dp))
                        menuItems.forEach { screen ->
                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        screen.icon,
                                        contentDescription = screen.title,
                                        tint = if (currentRoute == screen.route) StardustTextPrimary else StardustTextSecondary
                                    )
                                },
                                label = {
                                    Text(
                                        screen.title,
                                        color = if (currentRoute == screen.route) StardustTextPrimary else StardustTextSecondary
                                    )
                                },
                                badge = {
                                    if (screen is Screen.Settings && updateState is UpdateState.UpdateAvailable) {
                                        Badge(containerColor = Color.Red)
                                    }
                                    if (screen is Screen.MyTasks && myTasksUiState.activeTaskCount > 0) {
                                        Badge(containerColor = StardustError) {
                                            Text(
                                                text = myTasksUiState.activeTaskCount.toString(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                },
                                selected = currentRoute == screen.route,
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
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = StardustPrimary,
                                    unselectedContainerColor = Color.Transparent
                                )
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        DrawerFooter(
                            authState = authState,
                            appVersionName = appVersionName,
                            onNavigateToAccount = {
                                scope.launch { drawerState.close() }
                                navController.navigate(Screen.Account.route)
                            }
                        )
                    }
                }
            }
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    val allScreens = menuItems + listOf(
                        Screen.Account, Screen.SessionDetail, Screen.Dashboard,
                        Screen.EmployeeProfile, Screen.AdminRepairLog, Screen.PalletDistribution,
                        Screen.Storage, Screen.MyTasks, Screen.TaskCreation,
                        Screen.VehicleReport, Screen.VehicleReportHistory, Screen.VehicleReportAnalytics
                    )
                    val currentScreen = allScreens.find { it.route == currentRoute }
                    val topBarTitle = currentScreen?.title ?: ""

                    if (currentRoute != Screen.Scanner.route) {
                        TopAppBar(
                            title = { Text(topBarTitle) },
                            navigationIcon = {
                                val backButtonScreens = listOf(
                                    Screen.EmployeeProfile.route, Screen.AdminRepairLog.route,
                                    Screen.SessionDetail.route, Screen.Storage.route,
                                    Screen.TaskCreation.route, Screen.VehicleReportHistory.route,
                                    Screen.VehicleReportAnalytics.route
                                )
                                if (currentRoute in backButtonScreens) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                    }
                                } else {
                                    IconButton(onClick = {
                                        hapticManager.performClick(hapticFeedback, scope)
                                        scope.launch { drawerState.open() }
                                    }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                                    }
                                }
                            },
                            // V-- НАЧАЛО ИЗМЕНЕНИЙ (Блок 2 из 3) --V
                            // 3. Используем наше состояние для отображения кнопок.
                            // Если состояние пустое, ничего не отобразится.
                            // Если StorageScreen положит туда свою кнопку, она отобразится.
                            actions = { topBarActions() },
                            // ^-- КОНЕЦ ИЗМЕНЕНИЙ (Блок 2 из 3) --^
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Black.copy(alpha = 0.3f),
                                titleContentColor = StardustTextPrimary,
                                navigationIconContentColor = StardustTextPrimary,
                                actionIconContentColor = StardustTextPrimary // Добавлено для консистентности цвета иконки
                            )
                        )
                    }
                }
            ) { innerPadding ->
                AppNavHost(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding),
                    onMenuClick = {
                        hapticManager.performClick(hapticFeedback, scope)
                        scope.launch { drawerState.open() }
                    },
                    updateManager = updateManager,
                    appVersion = detailedAppVersion,
                    hapticManager = hapticManager,
                    view = view,
                    authManager = authManager,
                    // V-- НАЧАЛО ИЗМЕНЕНИЙ (Блок 3 из 3) --V
                    // 4. Передаем функцию для *установки* состояния в AppNavHost.
                    setTopBarActions = { actions ->
                        topBarActions = actions
                    }
                    // ^-- КОНЕЦ ИЗМЕНЕНИЙ (Блок 3 из 3) --^
                )
            }
        }
    }
}

@Composable
fun DrawerFooter(
    authState: AuthState,
    appVersionName: String,
    onNavigateToAccount: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalDivider(color = StardustItemBg.copy(alpha = 0.5f))
        if (authState.isLoggedIn) {
            Column(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToAccount).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(authState.userName ?: "Пользователь", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Перейти в профиль", color = StardustTextSecondary.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        } else {
            TextButton(onClick = onNavigateToAccount, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = "Войти", tint = StardustTextSecondary)
                    Spacer(Modifier.width(8.dp))
                    Text("Войти в аккаунт", color = StardustTextSecondary)
                }
            }
        }
        Text(appVersionName, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), textAlign = TextAlign.Center, color = StardustTextSecondary.copy(alpha = 0.5f), fontSize = 12.sp)
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
    setTopBarActions: (@Composable RowScope.() -> Unit) -> Unit // Принимаем функцию
) {
    val viewModel: QrScannerViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Scanner.route,
        modifier = modifier
    ) {
        val fadeEnter = fadeIn(animationSpec = tween(300))
        val fadeExit = fadeOut(animationSpec = tween(300))

        composable(route = Screen.Scanner.route, enterTransition = { fadeEnter }, exitTransition = { fadeExit }, popEnterTransition = { fadeEnter }, popExitTransition = { fadeExit }) {
            StardustScreen(
                viewModel = viewModel,
                onMenuClick = onMenuClick,
                hapticManager = hapticManager,
                view = view,
                onNavigateToPalletDistribution = {
                    viewModel.onNavigateToPalletDistribution()
                    navController.navigate(Screen.PalletDistribution.route)
                },
                onNavigateToStorage = {
                    navController.navigate(Screen.Storage.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route) { launchSingleTop = true }
                }
            )
        }

        // ... (остальные composable без изменений) ...

        composable(route = Screen.MyTasks.route, enterTransition = { fadeEnter }, exitTransition = { fadeExit }, popEnterTransition = { fadeEnter }, popExitTransition = { fadeExit }) {
            MyTasksScreen(
                onMenuClick = onMenuClick,
                onTaskClick = { taskId ->
                }
            )
        }

        composable(route = Screen.QrGenerator.route, enterTransition = { fadeEnter }, exitTransition = { fadeExit }, popEnterTransition = { fadeEnter }, popExitTransition = { fadeExit }) {
            QrGeneratorScreen()
        }
        composable(route = Screen.Settings.route, enterTransition = { fadeEnter }, exitTransition = { fadeExit }, popEnterTransition = { fadeEnter }, popExitTransition = { fadeExit }) {
            UnifiedSettingsScreen(authManager = authManager)
        }
        composable(route = Screen.Account.route, enterTransition = { fadeEnter }, exitTransition = { fadeExit }, popEnterTransition = { fadeEnter }, popExitTransition = { fadeExit }) {
            AccountScreen(authManager = authManager)
        }
        composable(route = Screen.Dashboard.route, enterTransition = { fadeEnter }, exitTransition = { fadeExit }, popEnterTransition = { fadeEnter }, popExitTransition = { fadeExit }) {
            DashboardScreen(
                onNavigateToEmployeeProfile = { userId ->
                    navController.navigate("employee_profile/$userId")
                },
                onNavigateToAdminRepairLog = {
                    navController.navigate(Screen.AdminRepairLog.route)
                },
                onNavigateToTaskCreation = {
                    navController.navigate(Screen.TaskCreation.route)
                },
                onNavigateToVehicleReport = {
                    navController.navigate(Screen.VehicleReport.route)
                }
            )
        }

        composable(route = Screen.VehicleReport.route, enterTransition = { fadeEnter }, exitTransition = { fadeExit }, popEnterTransition = { fadeEnter }, popExitTransition = { fadeExit }) {
            VehicleReportScreen(
                onNavigateToHistory = {
                    navController.navigate(Screen.VehicleReportHistory.route)
                }
            )
        }

        composable(route = Screen.VehicleReportHistory.route, enterTransition = { fadeEnter }, exitTransition = { fadeExit }, popEnterTransition = { fadeEnter }, popExitTransition = { fadeExit }) {
            VehicleReportHistoryScreen(
                onNavigateToAnalytics = {
                    navController.navigate(Screen.VehicleReportAnalytics.route)
                }
            )
        }

        composable(route = Screen.VehicleReportAnalytics.route, enterTransition = { fadeEnter }, exitTransition = { fadeExit }, popEnterTransition = { fadeEnter }, popExitTransition = { fadeExit }) {
            VehicleReportAnalyticsScreen()
        }

        composable(route = Screen.History.route, enterTransition = { fadeEnter }, exitTransition = { fadeExit }, popEnterTransition = { fadeEnter }, popExitTransition = { fadeExit }) {
            HistoryScreen(viewModel = viewModel, navController = navController)
        }
        composable(route = Screen.SessionDetail.route, enterTransition = { fadeEnter }, exitTransition = { fadeExit }, popEnterTransition = { fadeEnter }, popExitTransition = { fadeExit }) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
            if (sessionId != null) {
                SessionDetailScreen(sessionId = sessionId, viewModel = viewModel, navController = navController)
            } else {
                PlaceholderScreen(text = "Ошибка: ID сессии не найден")
            }
        }
        composable(route = Screen.EmployeeProfile.route, enterTransition = { fadeEnter }, exitTransition = { fadeExit }, popEnterTransition = { fadeEnter }, popExitTransition = { fadeExit }) {
            EmployeeProfileScreen()
        }
        composable(route = Screen.AdminRepairLog.route, enterTransition = { fadeEnter }, exitTransition = { fadeExit }, popEnterTransition = { fadeEnter }, popExitTransition = { fadeExit }) {
            AdminRepairLogScreen()
        }
        composable(route = Screen.PalletDistribution.route) {
            PalletDistributionScreen(viewModel = viewModel, authManager = authManager, onNavigateBack = { navController.popBackStack() })
        }
        composable(route = Screen.Storage.route) {
            StorageScreen(
                viewModel = viewModel,
                authManager = authManager,
                onNavigateBack = { navController.popBackStack() },
                setTopBarActions = setTopBarActions // Передаем функцию экрану
            )
        }
        composable(route = Screen.TaskCreation.route, enterTransition = { fadeEnter }, exitTransition = { fadeExit }, popEnterTransition = { fadeEnter }, popExitTransition = { fadeExit }) {
            TaskCreationScreen(navController = navController)
        }
    }
}