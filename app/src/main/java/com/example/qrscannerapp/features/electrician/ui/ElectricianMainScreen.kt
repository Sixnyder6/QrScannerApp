    package com.example.qrscannerapp.features.electrician.ui

    import android.Manifest
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.net.Uri
    import android.os.Build
    import android.widget.Toast
    import androidx.activity.compose.rememberLauncherForActivityResult
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.compose.animation.AnimatedContent
    import androidx.compose.animation.SizeTransform
    import androidx.compose.animation.animateColorAsState
    import androidx.compose.animation.animateContentSize
    import androidx.compose.animation.core.*
    import androidx.compose.animation.fadeIn
    import androidx.compose.animation.fadeOut
    import androidx.compose.animation.togetherWith
    import androidx.compose.foundation.ExperimentalFoundationApi
    import androidx.compose.foundation.background
    import androidx.compose.foundation.border
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.pager.HorizontalPager
    import androidx.compose.foundation.pager.rememberPagerState
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.automirrored.filled.ArrowBack
    import androidx.compose.material.icons.automirrored.filled.ExitToApp
    import androidx.compose.material.icons.automirrored.filled.Send
    import androidx.compose.material.icons.filled.*
    import androidx.compose.material.icons.rounded.LocationOn
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.draw.drawWithCache
    import androidx.compose.ui.geometry.Offset
    import androidx.compose.ui.graphics.Brush
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.graphicsLayer
    import androidx.compose.ui.graphics.vector.ImageVector
    import androidx.compose.ui.layout.ContentScale
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.compose.ui.window.Dialog
    import androidx.compose.ui.window.DialogProperties
    import androidx.core.content.ContextCompat
    import androidx.hilt.navigation.compose.hiltViewModel
    import androidx.lifecycle.viewmodel.compose.viewModel
    import androidx.navigation.NavHostController
    import androidx.navigation.compose.NavHost
    import androidx.navigation.compose.composable
    import androidx.navigation.compose.currentBackStackEntryAsState
    import androidx.navigation.compose.rememberNavController
    import coil.compose.AsyncImage
    import com.example.qrscannerapp.*
    import com.example.qrscannerapp.common.ui.AppBackground
    import com.example.qrscannerapp.features.electrician.ui.repair.ElectricianHistoryScreen
    import com.example.qrscannerapp.features.electrician.ui.repair.RepairScreen
    import com.example.qrscannerapp.features.electrician.ui.viewmodel.HistoryViewModel
    import com.example.qrscannerapp.features.electrician.ui.viewmodel.HistoryViewModelFactory
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.launch

    sealed class ElectricianSection(val route: String, val title: String, val icon: ImageVector) {
        object Repair   : ElectricianSection("repair",   "Ремонт",    Icons.Default.Build)
        object Profile  : ElectricianSection("profile",  "Профиль",   Icons.Default.AccountCircle)
        object Settings : ElectricianSection("settings", "Настройки", Icons.Default.Settings)
    }

    sealed class ProfileRoute(val route: String, val title: String) {
        object Overview : ProfileRoute("profile_overview", "Профиль")
        object History  : ProfileRoute("profile_history",  "История ремонтов")
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
                        title = {
                            Text(
                                if (selectedSection == ElectricianSection.Profile &&
                                    currentProfileRoute == ProfileRoute.History.route)
                                    "История ремонтов"
                                else selectedSection.title
                            )
                        },
                        navigationIcon = {
                            if (selectedSection == ElectricianSection.Profile &&
                                currentProfileRoute == ProfileRoute.History.route) {
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
                                    if (section == ElectricianSection.Profile &&
                                        selectedSection == ElectricianSection.Profile) {
                                        profileNavController.popBackStack(
                                            ProfileRoute.Overview.route, inclusive = false
                                        )
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
                            RepairScreen(authManager = authManager, hapticManager = hapticManager)
                        }
                        ElectricianSection.Settings ->
                            UnifiedSettingsScreen(authManager = authManager)
                        ElectricianSection.Profile ->
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
                    onNavigateToHistory = { navController.navigate(ProfileRoute.History.route) }
                )
            }
            composable(ProfileRoute.History.route) {
                ElectricianHistoryScreen(viewModel = historyViewModel)
            }
        }
    }

    // =================================================================================
    // НАСТРОЙКИ
    // =================================================================================

    @Composable
    fun UnifiedSettingsScreen(authManager: AuthManager) {
        val authState by authManager.authState.collectAsState()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val telemetryManager = remember { TelemetryManager(context) }
        val updateManager: UpdateManager = hiltViewModel()
        val settingsManager = remember { SettingsManager(context) }

        val isSoundEnabled     by settingsManager.isSoundEnabledFlow.collectAsState(initial = true)
        val isVibrationEnabled by settingsManager.isVibrationEnabledFlow.collectAsState(initial = true)
        // [НОВОЕ]
        val currentTheme by settingsManager.backgroundThemeFlow.collectAsState(initial = BackgroundTheme.ENGINE)

        var hasNotificationPermission by remember {
            mutableStateOf(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else true
            )
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                hasNotificationPermission = isGranted
                if (!isGranted) Toast.makeText(
                    context,
                    "Уведомления необходимы для отображения статуса загрузки.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- Аккаунт ---
            SettingsCategory(title = "Аккаунт")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SettingsRow(icon = Icons.Default.Person, title = "Имя",   value = authState.userName ?: "...")
                    HorizontalDivider(color = StardustItemBg)
                    SettingsRow(icon = Icons.Default.Shield, title = "Роль",  value = authState.role.displayName)
                    HorizontalDivider(color = StardustItemBg)
                    SettingsRow(icon = Icons.Rounded.LocationOn, title = "Склад", value = "Бестужевская 10")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // --- [НОВОЕ] Тема фона ---
            SettingsCategory(title = "Тема оформления")
            ThemePickerCard(
                currentTheme = currentTheme,
                onThemeSelected = { scope.launch { settingsManager.setBackgroundTheme(it) } }
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- Эффекты при сканировании ---
            if (authState.role != UserRole.ELECTRICIAN) {
                SettingsCategory(title = "Эффекты при сканировании")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
                ) {
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

            // --- О приложении ---
            SettingsCategory(title = "О приложении")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "Версия приложения",
                        value = telemetryManager.getAppVersion()
                    )
                    HorizontalDivider(color = StardustItemBg)
                    UpdateChecker(
                        updateManager = updateManager,
                        onCheckClick = {
                            if (hasNotificationPermission) {
                                scope.launch { updateManager.checkForUpdates() }
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // --- Об авторе ---
            SettingsCategory(title = "Об авторе")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SettingsRow(icon = Icons.Default.Code, title = "Разработчик", value = "Владислав С.")
                    HorizontalDivider(color = StardustItemBg)
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.Send,
                        title = "Telegram",
                        value = "@Cyberdyne_Industries",
                        isClickable = true
                    ) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Cyberdyne_Industries"))
                        )
                    }
                    HorizontalDivider(color = StardustItemBg)
                    SettingsRow(
                        icon = Icons.Default.Email,
                        title = "Email",
                        value = "pankratovvlad69@gmail.com",
                        isClickable = true
                    ) {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:pankratovvlad69@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Обратная связь по приложению QR Scanner")
                            }.let { Intent.createChooser(it, "Отправить письмо...") }
                        )
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

    // =================================================================================
    // [НОВОЕ] THEME PICKER
    // =================================================================================

    // Цвета превью для каждой темы — упрощённые, без шейдера
    private val themePreviewColors = mapOf(
        BackgroundTheme.ENGINE  to listOf(Color(0xFF0D0020), Color(0xFF4A00E0), Color(0xFF8E2DE2)),
        BackgroundTheme.NEBULA  to listOf(Color(0xFF000D1A), Color(0xFF1A4B8C), Color(0xFF5BB8FF)),
        BackgroundTheme.VORONOI to listOf(Color(0xFF010A04), Color(0xFF0D5C2E), Color(0xFF33E87A))
    )

    @Composable
    private fun ThemePickerCard(
        currentTheme: BackgroundTheme,
        onThemeSelected: (BackgroundTheme) -> Unit
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StardustGlassBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Фон приложения",
                    color = StardustTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BackgroundTheme.entries.forEach { theme ->
                        ThemePreviewCard(
                            theme = theme,
                            isSelected = theme == currentTheme,
                            onClick = { onThemeSelected(theme) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ThemePreviewCard(
        theme: BackgroundTheme,
        isSelected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val colors = themePreviewColors[theme] ?: listOf(Color.Black, Color.DarkGray, Color.Gray)

        // Анимация рамки при выборе
        val borderColor by animateColorAsState(
            targetValue = if (isSelected) StardustPrimary else Color.Transparent,
            animationSpec = tween(300),
            label = "border"
        )
        val scale by animateFloatAsState(
            targetValue = if (isSelected) 1.04f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "scale"
        )

        // Анимация движения градиента внутри превью
        val infiniteTransition = rememberInfiniteTransition(label = "preview_${theme.key}")
        val gradientOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(3000 + theme.ordinal * 700, easing = LinearEasing),
                RepeatMode.Reverse
            ),
            label = "gradient_offset"
        )

        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 2.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(onClick = onClick)
                    .drawWithCache {
                        onDrawBehind {
                            // Тёмный фон
                            drawRect(color = colors[0])

                            // Анимированное "пятно" света
                            val cx = size.width * (0.3f + gradientOffset * 0.4f)
                            val cy = size.height * (0.5f - gradientOffset * 0.2f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        colors[2].copy(alpha = 0.7f),
                                        colors[1].copy(alpha = 0.3f),
                                        Color.Transparent
                                    ),
                                    center = Offset(cx, cy),
                                    radius = size.width * 0.7f
                                ),
                                radius = size.width * 0.7f,
                                center = Offset(cx, cy)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Галочка если выбрана
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(StardustPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Название темы
            Text(
                text = "${theme.emoji} ${theme.displayName}",
                color = if (isSelected) StardustPrimary else StardustTextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }

    // =================================================================================
    // ОСТАЛЬНЫЕ КОМПОНЕНТЫ (без изменений)
    // =================================================================================

    @Composable
    private fun UpdateChecker(updateManager: UpdateManager, onCheckClick: () -> Unit) {
        val updateState by updateManager.updateState.collectAsState()
        var showUpdateDialog by remember { mutableStateOf(false) }

        LaunchedEffect(updateState) {
            if (updateState is UpdateState.UpdateAvailable) showUpdateDialog = true
        }

        if (showUpdateDialog) {
            val currentState = updateState
            if (currentState is UpdateState.UpdateAvailable) {
                UpdateAvailableDialog(
                    info = currentState.info,
                    updateState = updateState,
                    onDismiss = {
                        showUpdateDialog = false
                        if (updateState !is UpdateState.Downloading) updateManager.resetState()
                    },
                    onConfirm = { updateManager.startUpdate(currentState.info) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading)
                        onCheckClick()
                }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.SystemUpdate, contentDescription = "Обновления", tint = StardustTextSecondary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Проверить обновления", color = StardustTextPrimary, fontSize = 16.sp)
                when (val state = updateState) {
                    is UpdateState.Checking        -> Text("Идет проверка...",            color = StardustTextSecondary, fontSize = 12.sp)
                    is UpdateState.UpdateAvailable -> Text("Доступна версия ${state.info.latestVersionName}!", color = StardustSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    is UpdateState.UpdateNotAvailable -> Text("У вас последняя версия",   color = StardustTextSecondary, fontSize = 12.sp)
                    is UpdateState.Error           -> Text(state.message,                 color = StardustError,         fontSize = 12.sp)
                    is UpdateState.Downloading     -> Text("Загрузка: ${state.progress}%",color = StardustPrimary,       fontSize = 12.sp)
                    is UpdateState.Idle            -> Text("Нажмите, чтобы проверить",    color = StardustTextSecondary, fontSize = 12.sp)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = StardustTextSecondary)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun UpdateAvailableDialog(
        info: UpdateInfo,
        updateState: UpdateState,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        var fullScreenImageUri by remember { mutableStateOf<String?>(null) }

        if (fullScreenImageUri != null) {
            FullScreenImageViewerDialog(
                imageUrl = fullScreenImageUri!!,
                onDismiss = { fullScreenImageUri = null }
            )
        }

        AlertDialog(
            onDismissRequest = { if (updateState !is UpdateState.Downloading) onDismiss() },
            confirmButton = {
                if (updateState !is UpdateState.Downloading) {
                    val buttonText = "Скачать" + (info.apkSize?.let { " ($it)" } ?: "")
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
                    ) { Text(buttonText) }
                }
            },
            dismissButton = {
                if (updateState !is UpdateState.Downloading) {
                    TextButton(onClick = onDismiss) { Text("Позже", color = StardustTextSecondary) }
                }
            },
            title = { Text("Доступна новая версия ${info.latestVersionName}!", color = StardustTextPrimary) },
            text = {
                AnimatedContent(
                    targetState = updateState is UpdateState.Downloading,
                    label = "update_content_switcher"
                ) { isDownloading ->
                    if (isDownloading) {
                        val progress = (updateState as? UpdateState.Downloading)?.progress ?: 0
                        DownloadProgressIndicator(progress = progress)
                    } else {
                        UpdateInfoContent(info = info, onImageClick = { fullScreenImageUri = it })
                    }
                }
            },
            containerColor = StardustModalBg
        )
    }

    @Composable
    private fun UpdateInfoContent(info: UpdateInfo, onImageClick: (String) -> Unit) {
        var isExpanded by remember { mutableStateOf(false) }
        val itemsToShow = if (isExpanded) info.releaseItems else info.releaseItems?.take(2)

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            if (!info.imageUrls.isNullOrEmpty()) {
                ImageSlider(imageUrls = info.imageUrls, onImageClick = onImageClick)
                Spacer(modifier = Modifier.height(24.dp))
            }
            Column(modifier = Modifier.animateContentSize()) {
                if (!itemsToShow.isNullOrEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsToShow.forEach { item ->
                            Row(verticalAlignment = Alignment.Top) {
                                val (tagText, tagBaseColor) = when (item.type.lowercase()) {
                                    "new"  -> "New"  to Color(0xFF4CAF50)
                                    "fix"  -> "Fix"  to Color(0xFFFFC107)
                                    "beta" -> "Beta" to Color(0xFFE91E63)
                                    else   -> "Info" to Color.Gray
                                }
                                UpdateTag(text = tagText, baseColor = tagBaseColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = item.text, color = StardustTextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                    if ((info.releaseItems?.size ?: 0) > 2) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isExpanded) "Свернуть" else "Подробнее...",
                            color = StardustPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { isExpanded = !isExpanded }
                        )
                    }
                } else if (info.releaseNotes.isNotBlank()) {
                    Text(text = info.releaseNotes, color = StardustTextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
    }

    @Composable
    private fun DownloadProgressIndicator(progress: Int) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Идет загрузка...", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = StardustPrimary,
                trackColor = StardustItemBg
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("${progress}%", color = StardustTextSecondary, fontSize = 12.sp)
        }
    }

    @Composable
    fun FullScreenImageViewerDialog(imageUrl: String, onDismiss: () -> Unit) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Full screen image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }

    @Composable
    fun UpdateTag(text: String, baseColor: Color) {
        val shape = RoundedCornerShape(6.dp)
        Box(
            modifier = Modifier
                .border(width = 1.dp, color = baseColor, shape = shape)
                .background(color = baseColor.copy(alpha = 0.25f), shape = shape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = text, color = baseColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 12.sp)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ImageSlider(imageUrls: List<String>, onImageClick: (String) -> Unit) {
        val pagerState = rememberPagerState(pageCount = { imageUrls.size })

        LaunchedEffect(pagerState.pageCount) {
            while (true) {
                delay(4000)
                if (pagerState.pageCount > 0) {
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % pagerState.pageCount)
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) { page ->
                AsyncImage(
                    model = imageUrls[page],
                    contentDescription = "Изображение обновления ${page + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clickable { onImageClick(imageUrls[page]) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == iteration) StardustPrimary
                                else StardustTextSecondary.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
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
    private fun SettingsRow(
        icon: ImageVector,
        title: String,
        value: String,
        isClickable: Boolean = false,
        onClick: () -> Unit = {}
    ) {
        Row(
            modifier = (if (isClickable) Modifier.clickable(onClick = onClick) else Modifier)
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