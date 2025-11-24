package com.example.qrscannerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.qrscannerapp.features.electrician.ui.ElectricianMainScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject // <-- НОВЫЙ ИМПОРТ

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject // <-- НОВАЯ АННОТАЦИЯ
    lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // AuthManager теперь инжектируется, а не создается вручную
                val authState by authManager.authState.collectAsState()

                var showSplashScreen by remember { mutableStateOf(true) }

                if (showSplashScreen) {
                    SplashScreen(onAnimationFinished = {
                        showSplashScreen = false
                    })
                } else {
                    when {
                        authState.isLoading -> {
                            LoadingScreen()
                        }
                        authState.isLoggedIn -> {
                            when (authState.role) {
                                "electrician" -> {
                                    ElectricianMainScreen(authManager = authManager)
                                }
                                else -> {
                                    MainApp()
                                }
                            }
                        }
                        else -> {
                            AccountScreen(authManager = authManager)
                        }
                    }
                }
            }
        }
    }
}