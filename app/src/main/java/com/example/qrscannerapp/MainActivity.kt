package com.example.qrscannerapp

import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.common.ui.ShiftLockScreen
import com.example.qrscannerapp.features.electrician.ui.ElectricianMainScreen
import com.example.qrscannerapp.features.street_doctor.ui.StreetDoctorHost
import com.example.qrscannerapp.ui.theme.QrScannerAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var appSecurityGuard: AppSecurityGuard

    @Inject
    lateinit var telemetryManager: TelemetryManager  // ← Hilt singleton

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            appSecurityGuard.runSecurityCheckAsync()
        }

        val navigateTo = intent?.getStringExtra("navigate_to")
        val chatRoomId = intent?.getStringExtra("chat_room_id")

        setContent {
            val systemDensity = LocalDensity.current
            val customScale = 0.92f

            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = systemDensity.density * customScale,
                    fontScale = 1.0f
                )
            ) {
                QrScannerAppTheme {
                    val authState by authManager.authState.collectAsState()
                    var showSplashScreen by remember { mutableStateOf(true) }

                    Crossfade(
                        targetState = when {
                            showSplashScreen      -> AppScreen.SPLASH
                            authState.isLoading   -> AppScreen.SPLASH
                            !authState.isLoggedIn -> AppScreen.LOGIN
                            authState.isAdmin || authState.isShiftActive -> AppScreen.MAIN
                            else                  -> AppScreen.LOCK
                        },
                        animationSpec = tween(300),
                        label = "app_screen_transition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.SPLASH -> {
                                SplashScreen(onAnimationFinished = {
                                    showSplashScreen = false
                                })
                            }

                            AppScreen.LOADING -> {
                                LoadingScreen()
                            }

                            AppScreen.LOGIN -> {
                                AccountScreen(authManager = authManager)
                            }

                            AppScreen.MAIN -> {
                                when (authState.role) {
                                    UserRole.ELECTRICIAN -> {
                                        ElectricianMainScreen(authManager = authManager)
                                    }
                                    UserRole.TECHNIC -> {
                                        val currentUserName = authState.userName ?: "Техник"
                                        val currentUserInitials = currentUserName
                                            .split(" ")
                                            .mapNotNull { it.firstOrNull()?.toString() }
                                            .take(2)
                                            .joinToString("")
                                            .ifBlank { "ТХ" }
                                        StreetDoctorHost(
                                            currentUserName     = currentUserName,
                                            currentUserInitials = currentUserInitials,
                                            isOnShift           = authState.isShiftActive,
                                            userRole            = authState.role.displayName,
                                            onLogout            = { authManager.logout() }
                                        )
                                    }
                                    else -> {
                                        MainApp(
                                            authManager       = authManager,       // ← inject
                                            telemetryManager  = telemetryManager,  // ← inject
                                            initialRoute      = navigateTo,
                                            initialChatRoomId = chatRoomId
                                        )
                                    }
                                }
                            }

                            AppScreen.LOCK -> {
                                LockScreenContent(
                                    authManager = authManager,
                                    authState   = authState
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockScreenContent(
    authManager: AuthManager,
    authState: AuthState
) {
    val accountViewModel: AccountViewModel = hiltViewModel()
    val uiState by accountViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val updateManager: UpdateManager = hiltViewModel()
    val updateState by updateManager.updateState.collectAsState()

    LaunchedEffect(updateState) {
        if (updateState is UpdateState.ReadyToInstall) {
            updateManager.installApk((updateState as UpdateState.ReadyToInstall).uri)
            updateManager.resetState()
        }
    }

    LaunchedEffect(Unit) {
        updateManager.checkForUpdates()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    ShiftLockScreen(
        userName           = authState.userName ?: "Сотрудник",
        isAllowedToWork    = uiState.isAllowedToWork,
        shiftRequestStatus = uiState.shiftRequestStatus,
        isLoading          = uiState.isLoading,
        updateState        = updateState,
        onStartUpdate      = {
            if (updateState is UpdateState.UpdateAvailable) {
                updateManager.startUpdate((updateState as UpdateState.UpdateAvailable).info)
            }
        },
        onDismissUpdate    = { updateManager.resetState() },
        onStartShift       = { accountViewModel.startShift() },
        onRequestAccess    = { accountViewModel.requestShiftAccess() },
        onLogout           = {
            scope.launch {
                if (authState.isShiftActive) {
                    accountViewModel.endShiftOnLogout()
                }
                authManager.logout()
            }
        }
    )
}

private enum class AppScreen {
    SPLASH, LOADING, LOGIN, MAIN, LOCK
}