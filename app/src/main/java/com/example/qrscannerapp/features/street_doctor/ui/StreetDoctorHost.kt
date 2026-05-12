package com.example.qrscannerapp.features.street_doctor.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.qrscannerapp.features.street_doctor.domain.model.StreetScooter

enum class StreetDoctorScreen {
    MAIN, PASSPORT, ACCOUNT, SETTINGS, WAREHOUSE, CHAT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreetDoctorHost(
    currentUserName: String = "Техник",
    currentUserInitials: String = "ТХ",
    isOnShift: Boolean = true,
    userRole: String = "Техник",
    onLogout: () -> Unit = {}
) {
    val viewModel: StreetDoctorViewModel = hiltViewModel()

    var currentTab      by remember { mutableStateOf(StreetDoctorTab.TASKS) }
    var selectedScooter by remember { mutableStateOf<StreetScooter?>(null) }
    var currentScreen   by remember { mutableStateOf(StreetDoctorScreen.MAIN) }
    var showMenu        by remember { mutableStateOf(false) }

    if (currentScreen == StreetDoctorScreen.ACCOUNT) {
        com.example.qrscannerapp.common.ui.AppBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                com.example.qrscannerapp.AccountScreen(
                    authManager = hiltViewModel<com.example.qrscannerapp.AuthViewModel>().authManager
                )
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 4.dp, top = 4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { currentScreen = StreetDoctorScreen.MAIN },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Назад",
                        tint = Color.White, modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        return
    }

    if (currentScreen == StreetDoctorScreen.SETTINGS) {
        com.example.qrscannerapp.common.ui.AppBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                com.example.qrscannerapp.features.settings.ui.UnifiedSettingsScreen(
                    authManager = hiltViewModel<com.example.qrscannerapp.AuthViewModel>().authManager
                )
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 4.dp, top = 4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { currentScreen = StreetDoctorScreen.MAIN },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Назад",
                        tint = Color.White, modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        return
    }

    // ── Склад — собственный мини-NavHost чтобы работала навигация внутри ──
    if (currentScreen == StreetDoctorScreen.WAREHOUSE) {
        val authVm     = hiltViewModel<com.example.qrscannerapp.AuthViewModel>()
        val authState  = authVm.authManager.authState.collectAsState()
        val userId     = authState.value.userId ?: ""
        val context = LocalContext.current

        val warehouseNav = rememberNavController()

        com.example.qrscannerapp.common.ui.AppBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController    = warehouseNav,
                    startDestination = "wh_dashboard",
                    modifier         = Modifier.fillMaxSize()
                ) {
                    // dashboard
                    composable("wh_dashboard") {
                        com.example.qrscannerapp.features.inventory.ui.Warehouse.WarehouseDashboardScreen(
                            navController = warehouseNav,
                            isAdmin       = false,
                            userRole      = com.example.qrscannerapp.UserRole.TECHNIC,
                            userId        = userId
                        )
                    }
                    // каталог — WarehouseDashboardScreen делает navigate(Screen.WarehouseCatalog.route)
                    // Screen.WarehouseCatalog.route = "warehouse_catalog"
                    composable("warehouse_catalog") {
                        val warehouseVm: com.example.qrscannerapp.features.inventory.ui.Warehouse.WarehouseViewModel =
                            hiltViewModel()
                        val items by warehouseVm.items.collectAsState()
                        val cart  by warehouseVm.cart.collectAsState()
                        val currentUserName = authState.value.userName ?: "Техник"
                        val currentUserId   = authState.value.userId ?: ""
                        val currentUserRole = com.example.qrscannerapp.UserRole.TECHNIC.name
                        LaunchedEffect(Unit) {
                            warehouseVm.uiEvents.collect { event ->
                                when (event) {
                                    is com.example.qrscannerapp.features.inventory.ui.Warehouse.WarehouseViewModel.UiEvent.ShowSnackbar ->
                                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                                    is com.example.qrscannerapp.features.inventory.ui.Warehouse.WarehouseViewModel.UiEvent.NavigateBack ->
                                        warehouseNav.popBackStack()
                                }
                            }
                        }
                        com.example.qrscannerapp.features.inventory.ui.Warehouse.components.WarehouseCatalogScreen(
                            items               = items,
                            cart                = cart,
                            onAddToCart         = { item, qty -> warehouseVm.onAddToCart(item, qty) },
                            onRemoveFromCart    = { id -> warehouseVm.onRemoveFromCart(id) },
                            onSubmitOrder       = { warehouseVm.onSubmitOrder(currentUserId, currentUserName, currentUserRole) },
                            onNavigateToAddItem = { },
                            onTakeItem          = { item, qty -> warehouseVm.onTakeItem(item, qty, currentUserName) },
                            isAdmin             = false,
                            userRole            = com.example.qrscannerapp.UserRole.TECHNIC,
                            onNavigateBack      = { warehouseNav.popBackStack() }
                        )
                    }
                }

                // Кнопка назад: popBackStack внутри склада, или выход в MAIN
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 8.dp, top = 8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable {
                            if (!warehouseNav.popBackStack()) {
                                currentScreen = StreetDoctorScreen.MAIN
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "Назад",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        return
    }

    // ── Чат — рендерим внутри StreetDoctorHost ──
    if (currentScreen == StreetDoctorScreen.CHAT) {
        com.example.qrscannerapp.common.ui.AppBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                com.example.qrscannerapp.features.chat.ui.ChatScreen(
                    viewModel        = hiltViewModel(),
                    onOpenDirectChat = { _, _, _ -> },
                    onOpenProfile    = { _, _, _ -> },
                    onOpenInbox      = {}
                )
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 4.dp, top = 4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { currentScreen = StreetDoctorScreen.MAIN },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        return
    }

    Scaffold(
        containerColor = Color(0xFF0F0F13),
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            StreetDoctorBottomNav(
                currentTab    = currentTab,
                userName      = currentUserName,
                isOnShift     = isOnShift,
                onTabSelected = { tab ->
                    currentScreen   = StreetDoctorScreen.MAIN
                    selectedScooter = null
                    currentTab      = tab
                },
                onMenuClick = { showMenu = true }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (currentScreen == StreetDoctorScreen.PASSPORT && selectedScooter != null) {
                PassportScreen(
                    scooter   = selectedScooter!!,
                    viewModel = viewModel,
                    onBack    = {
                        currentScreen   = StreetDoctorScreen.MAIN
                        selectedScooter = null
                    }
                )
            } else {
                when (currentTab) {
                    StreetDoctorTab.TASKS -> {
                        TasksScreen(
                            viewModel           = viewModel,
                            currentUserName     = currentUserName,
                            currentUserInitials = currentUserInitials,
                            onMenuClick         = { showMenu = true },
                            onOpenPassport      = { scooter ->
                                selectedScooter = scooter
                                currentScreen   = StreetDoctorScreen.PASSPORT
                            }
                        )
                    }
                    StreetDoctorTab.SCANNER -> {
                        TechnicScannerScreen(
                            onMenuClick    = { showMenu = true },
                            onOpenPassport = { scooter ->
                                selectedScooter = scooter
                                currentScreen   = StreetDoctorScreen.PASSPORT
                            }
                        )
                    }
                    StreetDoctorTab.STORAGE -> {
                        FieldHubScreen()
                    }
                }
            }
        }
    }

    if (showMenu) {
        StreetDoctorMenuSheet(
            userName            = currentUserName,
            userRole            = userRole,
            isOnShift           = isOnShift,
            onDismiss           = { showMenu = false },
            onNavigateToAccount = {
                showMenu      = false
                currentScreen = StreetDoctorScreen.ACCOUNT
            },
            onNavigate = { key ->
                showMenu = false
                when (key) {
                    "chat"      -> currentScreen = StreetDoctorScreen.CHAT
                    "warehouse" -> currentScreen = StreetDoctorScreen.WAREHOUSE
                    "settings"  -> currentScreen = StreetDoctorScreen.SETTINGS
                }
            },
            onLogout = {
                showMenu = false
                onLogout()
            }
        )
    }
}