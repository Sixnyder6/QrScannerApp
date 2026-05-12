package com.example.qrscannerapp.common.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.qrscannerapp.TelemetryManager
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.system.measureTimeMillis

/**
 * Мониторит батарею и пинг, показывает SystemAlertBanner.
 *
 * Добавь в MainApp.kt внутри AppBackground { Box { ... } } рядом с
 * ChatNotificationBanner:
 *
 * ```kotlin
 * var currentSystemAlert by remember { mutableStateOf<SystemAlert?>(null) }
 *
 * AlertsManager(
 *     telemetryManager = telemetryManager,
 *     onAlert = { alert ->
 *         // Не перебиваем существующий алерт если он не dismissed
 *         if (currentSystemAlert == null) currentSystemAlert = alert
 *     }
 * )
 *
 * SystemAlertBanner(
 *     alert     = currentSystemAlert,
 *     onDismiss = { currentSystemAlert = null },
 *     modifier  = Modifier.align(Alignment.BottomCenter)
 * )
 * ```
 */
@Composable
fun AlertsManager(
    telemetryManager: TelemetryManager,
    onAlert: (SystemAlert) -> Unit
) {
    val context = LocalContext.current

    // ── Батарея — проверяем каждые 2 минуты ──────────────────────────────
    LaunchedEffect(Unit) {
        var lastBatteryAlertLevel = -1
        while (true) {
            delay(120_000)
            val level = telemetryManager.getBatteryLevel()
            // Алертим только один раз при пересечении порога
            if (level in 1..15 && level != lastBatteryAlertLevel) {
                lastBatteryAlertLevel = level
                onAlert(SystemAlert.lowBattery(level))
            } else if (level > 20) {
                lastBatteryAlertLevel = -1 // сброс когда зарядили
            }
        }
    }

    // ── Пинг до Firebase — каждые 30 секунд ──────────────────────────────
    LaunchedEffect(Unit) {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        var consecutiveHighPing = 0

        while (true) {
            delay(30_000)
            try {
                val ms = measureTimeMillis {
                    val request = Request.Builder()
                        .url("https://firestore.googleapis.com/")
                        .head()
                        .build()
                    client.newCall(request).execute().close()
                }

                when {
                    ms > 1500 -> {
                        consecutiveHighPing++
                        // Показываем алерт только если пинг высокий 2 раза подряд
                        // чтобы не спамить при случайном скачке
                        if (consecutiveHighPing >= 2) {
                            onAlert(SystemAlert.highPing(ms))
                        }
                    }
                    else -> consecutiveHighPing = 0
                }
            } catch (e: Exception) {
                // Нет соединения
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val hasNet = cm.activeNetwork?.let {
                    cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                } ?: false

                if (!hasNet) {
                    onAlert(SystemAlert.noConnection())
                }
            }
        }
    }
}