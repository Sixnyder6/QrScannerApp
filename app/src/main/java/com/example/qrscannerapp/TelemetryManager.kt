// Файл: TelemetryManager.kt
package com.example.qrscannerapp

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import com.example.qrscannerapp.features.profile.domain.model.PerformanceClass
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Собирает информацию об устройстве и версии ОС.
     * @return Строка вида "Google Pixel 8, Android 14"
     */
    fun getDeviceInfo(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val osVersion = Build.VERSION.RELEASE
        return "$manufacturer $model, Android $osVersion"
    }

    /**
     * Получает версию текущего приложения.
     * @return Строка вида "1.1.0"
     */
    fun getAppVersion(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Получает текущий уровень заряда батареи в процентах.
     * @return Целое число от 0 до 100, или -1 в случае ошибки.
     */
    fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    /**
     * Получает информацию о текущем состоянии сети.
     * @return Строка вида "WiFi", "Cellular (4G)", "Offline".
     */
    fun getNetworkState(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return "Offline"
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "Offline"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val generation = when (capabilities.linkDownstreamBandwidthKbps) {
                    in 1..100 -> "2G"
                    in 101..1000 -> "3G"
                    in 1001..50000 -> "4G"
                    in 50001..Int.MAX_VALUE -> "5G"
                    else -> ""
                }
                "Cellular ${if (generation.isNotEmpty()) "($generation)" else ""}".trim()
            }
            else -> "Unknown"
        }
    }

    /**
     * Получает информацию о свободной оперативной памяти (RAM).
     * @return Строка вида "1.2 GB free".
     */
    fun getFreeRam(): String {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val freeRamBytes = memoryInfo.availMem
        return if (freeRamBytes > 1024 * 1024 * 1024) {
            String.format("%.1f GB free", freeRamBytes / (1024.0 * 1024.0 * 1024.0))
        } else {
            String.format("%d MB free", freeRamBytes / (1024 * 1024))
        }
    }

    /**
     * Получает информацию о свободном месте во внутреннем хранилище.
     * @return Строка вида "25.6 GB free".
     */
    fun getFreeStorage(): String {
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeStorageBytes = stat.availableBlocksLong * stat.blockSizeLong
        return String.format("%.1f GB free", freeStorageBytes / (1024.0 * 1024.0 * 1024.0))
    }

    /**
     * Получает время работы устройства с последней перезагрузки.
     * @return Строка вида "3 days, 14 hours".
     */
    fun getDeviceUptime(): String {
        val uptimeMillis = SystemClock.elapsedRealtime()
        val days = TimeUnit.MILLISECONDS.toDays(uptimeMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(uptimeMillis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMillis) % 60

        return when {
            days > 0 -> "$days days, $hours hours"
            hours > 0 -> "$hours hours, $minutes min"
            else -> "$minutes min"
        }
    }

    /**
     * Проверяет, заряжается ли устройство в данный момент.
     * @return true, если устройство подключено к зарядному устройству, иначе false.
     */
    fun isCharging(): Boolean {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    /**
     * Получает температуру и состояние "здоровья" батареи.
     * @return Строка вида "35.5°C (Good)".
     */
    fun getBatteryHealth(): String {
        val intent: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }

        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)

        val tempCelsius = if (temp != null && temp != -1) "%.1f°C".format(temp / 10.0) else "N/A"

        val healthStatus = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            else -> "Unknown"
        }

        return "$tempCelsius ($healthStatus)"
    }

    /**
     * Проверяет, включен ли на устройстве режим энергосбережения.
     * @return true, если режим включен, иначе false.
     */
    fun isPowerSaveMode(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isPowerSaveMode
    }

    /**
     * Асинхронно проверяет время отклика (ping) до DNS сервера Google.
     * Эту функцию нужно вызывать внутри корутины.
     * @return Строка вида "25 ms" или "Timeout" / "Offline".
     */
    suspend fun getNetworkPing(): String {
        if (getNetworkState() == "Offline") return "Offline"

        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val reachable = InetAddress.getByName("8.8.8.8").isReachable(1000)
                val endTime = System.currentTimeMillis()

                if (reachable) {
                    "${endTime - startTime} ms"
                } else {
                    "Timeout"
                }
            } catch (e: IOException) {
                "Host Unreachable"
            }
        }
    }

    /**
     * Получает общий объем оперативной памяти (RAM) устройства.
     * Эта метрика стабильна и идеально подходит для классификации производительности.
     * @return Общий объем RAM в гигабайтах (GB), или -1.0 в случае ошибки.
     */
    fun getTotalRamInGigabytes(): Double {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            val totalRamBytes = memoryInfo.totalMem
            // 1 гигабайт = 1024 * 1024 * 1024 байт
            totalRamBytes / (1024.0 * 1024.0 * 1024.0)
        } catch (e: Exception) {
            // В случае ошибки возвращаем значение, которое легко обработать.
            -1.0
        }
    }

    /**
     * Определяет класс производительности устройства на основе общего объема RAM.
     * @return Enum PerformanceClass (LOW, MEDIUM, HIGH).
     */
    fun getPerformanceClass(): PerformanceClass {
        val totalRamGb = getTotalRamInGigabytes()
        return when {
            totalRamGb <= 0.0 -> PerformanceClass.UNKNOWN // В случае ошибки
            totalRamGb < 3.5 -> PerformanceClass.LOW     // < 3.5 GB RAM - ведро
            totalRamGb < 7.5 -> PerformanceClass.MEDIUM  // от 3.5 до 7.5 GB RAM - рабочая лошадка
            else -> PerformanceClass.HIGH                // > 7.5 GB RAM - ракета
        }
    }

    /**
     * Собирает ВСЮ телеметрию (кроме пинга) в удобный для записи в Firestore формат.
     * @return Map<String, Any> с расширенным набором ключей.
     */
    fun getAllTelemetry(): Map<String, Any> {
        return mapOf(
            "deviceInfo" to getDeviceInfo(),
            "appVersion" to getAppVersion(),
            "lastBatteryLevel" to getBatteryLevel(),
            "networkState" to getNetworkState(),
            "freeRam" to getFreeRam(),
            "freeStorage" to getFreeStorage(),
            "deviceUptime" to getDeviceUptime(),
            "isCharging" to isCharging(),
            "batteryHealth" to getBatteryHealth(),
            "isPowerSaveMode" to isPowerSaveMode(),
            // Добавляем общий RAM для будущей аналитики
            "totalRamInGb" to getTotalRamInGigabytes()
        )
    }
}