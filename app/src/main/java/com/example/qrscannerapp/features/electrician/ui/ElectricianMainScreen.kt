// --- ElectricianMainScreen.kt (ФИНАЛЬНАЯ ВЕРСИЯ С АРХИТЕКТУРОЙ "ФОН СНИЗУ" И ЛОГИКОЙ ОБНОВЛЕНИЙ) ---

package com.example.qrscannerapp.features.electrician.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.qrscannerapp.*
import com.example.qrscannerapp.common.ui.AppBackground
import com.example.qrscannerapp.features.electrician.ui.repair.ElectricianHistoryScreen
import com.example.qrscannerapp.features.electrician.ui.repair.RepairScreen
import com.example.qrscannerapp.features.electrician.ui.viewmodel.HistoryViewModel
import com.example.qrscannerapp.features.electrician.ui.viewmodel.HistoryViewModelFactory
import kotlinx.coroutines.launch

sealed class ElectricianSection(val route: String, val title: String, val icon: ImageVector) {
    object Repair : ElectricianSection("repair", "Ремонт", Icons.Default.Build)
    object Profile : ElectricianSection("profile", "Профиль", Icons.Default.AccountCircle)
    object Settings : ElectricianSection("settings", "Настройки", Icons.Default.Settings)
}
sealed class ProfileRoute(val route: String, val title: String) {
    object Overview : ProfileRoute("profile_overview", "Профиль")
    object History : ProfileRoute("profile_history", "История ремонтов")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElectricianMainScreen(authManager: AuthManager) {
    val sections = listOf(
        ElectricianSection.Repair,
        ElectricianSection.Profile,
        ElectricianSection.Settings,
    )
    var selectedSection by remember { mutableStateOf<ElectricianSection>(ElectricianSection.Repair) }
    val profileNavController = rememberNavController()
    val profileNavBackStackEntry by profileNavController.currentBackStackEntryAsState()
    val currentProfileRoute = profileNavBackStackEntry?.destination?.route

    val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(authManager))

    AppBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text( if (selectedSection == ElectricianSection.Profile && currentProfileRoute == ProfileRoute.History.route) "История ремонтов" else selectedSection.title) },
                    navigationIcon = {
                        if (selectedSection == ElectricianSection.Profile && currentProfileRoute == ProfileRoute.History.route) {
                            IconButton(onClick = { profileNavController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = StardustGlassBg,
                        titleContentColor = StardustTextPrimary,
                        navigationIconContentColor = StardustTextPrimary
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = StardustGlassBg) {
                    sections.forEach { section ->
                        NavigationBarItem(
                            icon = { Icon(section.icon, contentDescription = section.title) },
                            label = { Text(section.title) },
                            selected = selectedSection == section,
                            onClick = {
                                if (section == ElectricianSection.Profile && selectedSection == ElectricianSection.Profile) {
                                    profileNavController.popBackStack(ProfileRoute.Overview.route, inclusive = false)
                                }
                                selectedSection = section
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StardustPrimary,
                                unselectedIconColor = StardustTextSecondary,
                                selectedTextColor = StardustPrimary,
                                unselectedTextColor = StardustTextSecondary,
                                indicatorColor = StardustPrimary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                when (selectedSection) {
                    ElectricianSection.Repair -> {
                        val context = LocalContext.current
                        val settingsManager = remember { SettingsManager(context) }
                        val hapticManager = remember { HapticFeedbackManager(settingsManager) }
                        RepairScreen(
                            authManager = authManager,
                            hapticManager = hapticManager
                        )
                    }
                    ElectricianSection.Settings -> UnifiedSettingsScreen(authManager = authManager)
                    ElectricianSection.Profile -> {
                        ProfileNavHost(
                            navController = profileNavController,
                            authManager = authManager,
                            historyViewModel = historyViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileNavHost(
    navController: NavHostController,
    authManager: AuthManager,
    historyViewModel: HistoryViewModel
) {
    NavHost(navController = navController, startDestination = ProfileRoute.Overview.route) {
        composable(ProfileRoute.Overview.route) {
            ElectricianProfileScreen(
                authManager = authManager,
                historyViewModel = historyViewModel,
                onNavigateToHistory = {
                    navController.navigate(ProfileRoute.History.route)
                }
            )
        }
        composable(ProfileRoute.History.route) {
            ElectricianHistoryScreen(viewModel = historyViewModel)
        }
    }
}

@Composable
fun UnifiedSettingsScreen(authManager: AuthManager) {
    val authState by authManager.authState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val telemetryManager = remember { TelemetryManager(context) }
    // V-- НАЧАЛО ИЗМЕНЕНИЙ: Инициализируем UpdateManager как HiltViewModel --V
    val updateManager: UpdateManager = hiltViewModel()
    // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^
    val settingsManager = remember { SettingsManager(context) }

    val isSoundEnabled by settingsManager.isSoundEnabledFlow.collectAsState(initial = true)
    val isVibrationEnabled by settingsManager.isVibrationEnabledFlow.collectAsState(initial = true)

    // V-- НАЧАЛО ИЗМЕНЕНИЙ: Логика запроса разрешения на уведомления --V
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else { true } // Для старых версий разрешение не нужно
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, "Уведомления необходимы для отображения статуса загрузки.", Toast.LENGTH_LONG).show()
            }
        }
    )
    // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingsCategory(title = "Аккаунт")
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StardustGlassBg)) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                SettingsRow(icon = Icons.Default.Person, title = "Имя", value = authState.userName ?: "...")
                HorizontalDivider(color = StardustItemBg)
                SettingsRow(icon = Icons.Default.Shield, title = "Роль", value = authState.role?.replaceFirstChar { it.titlecase() } ?: "...")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (authState.role != "electrician") {
            SettingsCategory(title = "Эффекты при сканировании")
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StardustGlassBg)) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Default.VolumeUp,
                        title = "Звуковой сигнал",
                        isChecked = isSoundEnabled,
                        onCheckedChange = { scope.launch { settingsManager.setSoundEnabled(it) } }
                    )
                    HorizontalDivider(color = StardustItemBg)
                    SettingsToggleRow(
                        icon = Icons.Default.Vibration,
                        title = "Вибрация",
                        isChecked = isVibrationEnabled,
                        onCheckedChange = { scope.launch { settingsManager.setVibrationEnabled(it) } }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        SettingsCategory(title = "О приложении")
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StardustGlassBg)) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                SettingsRow(icon = Icons.Default.Info, title = "Версия приложения", value = telemetryManager.getAppVersion())
                HorizontalDivider(color = StardustItemBg)
                // V-- НАЧАЛО ИЗМЕНЕНИЙ: Передаем новую логику клика в UpdateChecker --V
                UpdateChecker(
                    updateManager = updateManager,
                    onCheckClick = {
                        if (hasNotificationPermission) {
                            scope.launch { updateManager.checkForUpdates() }
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }
                )
                // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        SettingsCategory(title = "Об авторе")
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StardustGlassBg)) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                SettingsRow(icon = Icons.Default.Code, title = "Разработчик", value = "Владислав С.")
                HorizontalDivider(color = StardustItemBg)
                SettingsRow(icon = Icons.AutoMirrored.Filled.Send, title = "Telegram", value = "@Cyberdyne_Industries", isClickable = true) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Cyberdyne_Industries"))
                    context.startActivity(intent)
                }
                HorizontalDivider(color = StardustItemBg)
                SettingsRow(icon = Icons.Default.Email, title = "Email", value = "pankratovvlad69@gmail.com", isClickable = true) {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:pankratovvlad69@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Обратная связь по приложению QR Scanner")
                    }
                    context.startActivity(Intent.createChooser(intent, "Отправить письмо..."))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { authManager.logout() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StardustError.copy(alpha = 0.2f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Выйти", tint = StardustError)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Выйти", color = StardustError, fontWeight = FontWeight.Bold)
        }
    }
}

// V-- НАЧАЛО ИЗМЕНЕНИЙ: UpdateChecker теперь принимает лямбду для клика --V
@Composable
private fun UpdateChecker(updateManager: UpdateManager, onCheckClick: () -> Unit) {
// ^-- КОНЕЦ ИЗМЕНЕНИЙ --^
    val updateState by updateManager.updateState.collectAsState()
    val scope = rememberCoroutineScope()
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(updateState) {
        if (updateState is UpdateState.UpdateAvailable) {
            showUpdateDialog = true
        }
    }

    if (showUpdateDialog && updateState is UpdateState.UpdateAvailable) {
        UpdateAvailableDialog(
            info = (updateState as UpdateState.UpdateAvailable).info,
            onDismiss = { showUpdateDialog = false },
            onConfirm = {
                // `startUpdate` теперь не suspend-функция
                updateManager.startUpdate((updateState as UpdateState.UpdateAvailable).info)
                showUpdateDialog = false
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading) {
                    onCheckClick() // Используем переданную лямбду
                }
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.SystemUpdate, contentDescription = "Обновления", tint = StardustTextSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Проверить обновления", color = StardustTextPrimary, fontSize = 16.sp)
            when (val state = updateState) {
                is UpdateState.Checking -> Text("Идет проверка...", color = StardustTextSecondary, fontSize = 12.sp)
                is UpdateState.UpdateAvailable -> Text("Доступна версия ${state.info.latestVersionName}!", color = StardustSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                is UpdateState.UpdateNotAvailable -> Text("У вас последняя версия", color = StardustTextSecondary, fontSize = 12.sp)
                is UpdateState.Error -> Text(state.message, color = StardustError, fontSize = 12.sp)
                is UpdateState.Downloading -> Text("Загрузка: ${state.progress}%", color = StardustPrimary, fontSize = 12.sp)
                is UpdateState.Idle -> Text("Нажмите, чтобы проверить", color = StardustTextSecondary, fontSize = 12.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = StardustTextSecondary)
    }
}
// ... (остальные функции остаются без изменений)
@Composable
private fun UpdateAvailableDialog(
    info: UpdateInfo,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)) {
                Text("Скачать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Позже", color = StardustTextSecondary)
            }
        },
        title = { Text("Доступно обновление!", color = StardustTextPrimary) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Новая версия: ${info.latestVersionName}",
                    color = StardustTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Что нового:",
                    color = StardustTextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = info.releaseNotes,
                    color = StardustTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        },
        containerColor = StardustModalBg
    )
}
@Composable
private fun SettingsCategory(title: String) {
    Text(
        text = title,
        color = StardustTextSecondary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
    )
}
@Composable
private fun SettingsRow(icon: ImageVector, title: String, value: String, isClickable: Boolean = false, onClick: () -> Unit = {}) {
    val modifier = if (isClickable) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = StardustTextSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, color = StardustTextPrimary, fontSize = 16.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, color = StardustTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (isClickable) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = StardustTextSecondary)
        }
    }
}
@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = StardustTextSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, color = StardustTextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = StardustPrimary,
                checkedTrackColor = StardustSecondary,
                uncheckedThumbColor = StardustTextSecondary,
                uncheckedTrackColor = StardustItemBg
            )
        )
    }
}