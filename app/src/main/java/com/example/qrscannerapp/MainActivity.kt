package com.example.qrscannerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.qrscannerapp.features.electrician.ui.ElectricianMainScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            // --- НАЧАЛО НАСТРОЙКИ ЭКРАНА ---

            // 1. Берем текущие параметры экрана телефона
            val systemDensity = LocalDensity.current

            // 2. Настраиваем Масштаб "2K" (Balanced Pro Mode)
            // 0.85f было слишком мелко (4K).
            // 0.92f — это идеальная середина. Четко, плотно, но пальцем попадать легче.
            val customScale = 0.92f

            // 3. Создаем "Купол", который держит интерфейс в рамках
            CompositionLocalProvider(
                LocalDensity provides Density(
                    // Уменьшаем интерфейс чуть меньше, чем в прошлый раз
                    density = systemDensity.density * customScale,

                    // Жестко блокируем увеличение шрифта (игнорируем настройки "для слепых")
                    fontScale = 1.0f
                )
            ) {
                // --- ВНУТРИ УЖЕ ТВОЕ ПРИЛОЖЕНИЕ ---

                MaterialTheme {
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
                                // --- ИСПРАВЛЕНИЕ: Используем Enum вместо строки ---
                                when (authState.role) {
                                    UserRole.ELECTRICIAN -> {
                                        ElectricianMainScreen(authManager = authManager)
                                    }
                                    else -> {
                                        MainApp()
                                    }
                                }
                                // --------------------------------------------------
                            }
                            else -> {
                                AccountScreen(authManager = authManager)
                            }
                        }
                    }
                }
            }
            // --- КОНЕЦ НАСТРОЙКИ ЭКРАНА ---
        }
    }
}