package com.example.qrscannerapp.features.electrician.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.example.qrscannerapp.features.settings.ui.UnifiedSettingsScreen

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