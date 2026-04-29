package com.example.qrscannerapp

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.qrscannerapp.features.profile.domain.model.PerformanceClass
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ── Константы (убираем магические числа) ────────────────────────────────
private object TelemetryConstants {
    const val TAG = "TelemetryManager"

    // Пороги классификации RAM (в ГБ)
    const val RAM_LOW_THRESHOLD_GB = 3.5
    const val RAM_MEDIUM_THRESHOLD_GB = 7.5

    // Таймауты
    const val NETWORK_PING_TIMEOUT_MS = 1500L
    const val LOCATION_CACHE_TTL_MS = 5 * 60 * 1000L // 5 минут

    // Сеть
    const val PING_HOST = "8.8.8.8"
    const val PING_PORT = 53

    // Пороги генерации сети (Kbps)
    const val NETWORK_2G_MAX_KBPS = 100
    const val NETWORK_3G_MAX_KBPS = 1000
    const val NETWORK_4G_MAX_KBPS = 50000
}

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class TelemetryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var collectionTrace: Trace? = null

    // ✅ Кэш для GPS-координат (экономия батареи)
    private var cachedLocation: DeviceLocation? = null
    private var lastLocationTimestamp: Long = 0L

    // ============================================================================================
    // НОВОЕ: ПОЛУЧЕНИЕ КООРДИНАТ ДЛЯ СБ (GPS) С КЭШИРОВАНИЕМ
    // ============================================================================================
    suspend fun getCurrentLocation(): DeviceLocation? {
        // ✅ Проверяем кэш — если координаты свежие (< 5 мин), возвращаем их
        val now = System.currentTimeMillis()
        if (cachedLocation != null &&
            (now - lastLocationTimestamp) < TelemetryConstants.LOCATION_CACHE_TTL_MS) {
            Log.d(TelemetryConstants.TAG, "Используем закэшированные координаты")
            return cachedLocation
        }

        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // ✅ Логирование отсутствия разрешений
        if (!hasFineLocation && !hasCoarseLocation) {
            Log.w(TelemetryConstants.TAG, "Нет разрешений на геолокацию")
            return null
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            Log.w(TelemetryConstants.TAG, "Геолокация отключена на устройстве")
            return null
        }

        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val location = withTimeout(TelemetryConstants.NETWORK_PING_TIMEOUT_MS * 2) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()
            }

            if (location != null) {
                val deviceLocation = DeviceLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = now
                )
                // ✅ Сохраняем в кэш
                cachedLocation = deviceLocation
                lastLocationTimestamp = now
                Log.d(TelemetryConstants.TAG, "Получены новые координаты: ${location.latitude}, ${location.longitude}")
                deviceLocation
            } else {
                Log.w(TelemetryConstants.TAG, "Не удалось получить координаты")
                null
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TelemetryConstants.TAG, "Таймаут при получении координат", e)
            null
        } catch (e: SecurityException) {
            Log.w(TelemetryConstants.TAG, "Разрешение на геолокацию отозвано", e)
            null
        } catch (e: Exception) {
            Log.e(TelemetryConstants.TAG, "Ошибка получения координат", e)
            null
        }
    }

    /**
     * Очищает кэш локаций (вызывать при изменении разрешений или явном запросе)
     */
    fun clearLocationCache() {
        cachedLocation = null
        lastLocationTimestamp = 0L
        Log.d(TelemetryConstants.TAG, "Кэш локаций очищен")
    }

    // ✅ Оптимизация: получаем BatteryIntent один раз
    private fun getBatteryIntent(): Intent? {
        return context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    fun getDeviceInfo(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val osVersion = Build.VERSION.RELEASE
        return "$manufacturer $model, Android $osVersion"
    }

    fun getAppVersion(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            Log.e(TelemetryConstants.TAG, "Ошибка получения версии приложения", e)
            "unknown"
        }
    }

    fun getBatteryLevel(): Int {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            Log.e(TelemetryConstants.TAG, "Ошибка получения уровня батареи", e)
            -1
        }
    }

    fun getNetworkState(): String {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return "Offline"
            val capabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "Offline"

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    val bandwidth = capabilities.linkDownstreamBandwidthKbps
                    val generation = when (bandwidth) {
                        in 1..TelemetryConstants.NETWORK_2G_MAX_KBPS -> "2G"
                        in (TelemetryConstants.NETWORK_2G_MAX_KBPS + 1)..TelemetryConstants.NETWORK_3G_MAX_KBPS -> "3G"
                        in (TelemetryConstants.NETWORK_3G_MAX_KBPS + 1)..TelemetryConstants.NETWORK_4G_MAX_KBPS -> "4G"
                        else -> "5G"
                    }
                    "Cellular ($generation)"
                }
                else -> "Unknown"
            }
        } catch (e: Exception) {
            Log.e(TelemetryConstants.TAG, "Ошибка определения состояния сети", e)
            "Error"
        }
    }

    fun getFreeRam(): String {
        return try {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            val freeRamBytes = memoryInfo.availMem
            if (freeRamBytes > 1024 * 1024 * 1024) {
                String.format("%.1f GB free", freeRamBytes / (1024.0 * 1024.0 * 1024.0))
            } else {
                String.format("%d MB free", freeRamBytes / (1024 * 1024))
            }
        } catch (e: Exception) {
            Log.e(TelemetryConstants.TAG, "Ошибка получения свободной RAM", e)
            "N/A"
        }
    }

    fun getFreeStorage(): String {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val freeStorageBytes = stat.availableBlocksLong * stat.blockSizeLong
            String.format("%.1f GB free", freeStorageBytes / (1024.0 * 1024.0 * 1024.0))
        } catch (e: Exception) {
            Log.e(TelemetryConstants.TAG, "Ошибка получения свободного хранилища", e)
            "N/A"
        }
    }

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

    fun isCharging(): Boolean {
        val status: Int = getBatteryIntent()
            ?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun getBatteryHealth(): String {
        val intent = getBatteryIntent()
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)

        val tempCelsius =
            if (temp != null && temp != -1) "%.1f°C".format(temp / 10.0) else "N/A"

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

    fun isPowerSaveMode(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isPowerSaveMode
    }

    suspend fun getNetworkPing(): String {
        if (getNetworkState() == "Offline") return "Offline"

        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                Socket().use { socket ->
                    socket.connect(
                        InetSocketAddress(TelemetryConstants.PING_HOST, TelemetryConstants.PING_PORT),
                        TelemetryConstants.NETWORK_PING_TIMEOUT_MS.toInt()
                    )
                }
                val elapsed = System.currentTimeMillis() - startTime
                "$elapsed ms"
            } catch (e: Exception) {
                Log.w(TelemetryConstants.TAG, "Таймаут ping: ${e.message}", e)
                "Timeout"
            }
        }
    }

    fun getTotalRamInGigabytes(): Double {
        return try {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        } catch (e: Exception) {
            Log.e(TelemetryConstants.TAG, "Ошибка получения общей RAM", e)
            -1.0
        }
    }

    fun getPerformanceClass(): PerformanceClass {
        val totalRamGb = getTotalRamInGigabytes()
        return when {
            totalRamGb <= 0.0 -> PerformanceClass.UNKNOWN
            totalRamGb < TelemetryConstants.RAM_LOW_THRESHOLD_GB -> PerformanceClass.LOW
            totalRamGb < TelemetryConstants.RAM_MEDIUM_THRESHOLD_GB -> PerformanceClass.MEDIUM
            else -> PerformanceClass.HIGH
        }
    }

    fun getCpuAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
    }

    fun getAndroidSdkVersion(): Int {
        return Build.VERSION.SDK_INT
    }

    fun isLowRamDevice(): Boolean {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.isLowRamDevice
    }

    fun getBatterySnapshot(): String {
        val level = getBatteryLevel()
        val charging = isCharging()
        val powerSave = isPowerSaveMode()
        val flags = buildList {
            if (charging) add("charging")
            if (powerSave) add("power-save")
        }
        return if (flags.isEmpty()) "$level%" else "$level% (${flags.joinToString(", ")})"
    }

    suspend fun getAllTelemetry(
        includePing: Boolean = false,
        includeLocation: Boolean = false
    ): Map<String, Any> {
        collectionTrace = FirebasePerformance.getInstance().newTrace("telemetry_collection")
        collectionTrace?.start()

        val telemetry = try {
            val baseMap = mutableMapOf<String, Any>(
                "deviceInfo" to getDeviceInfo(),
                "appVersion" to getAppVersion(),
                "cpuAbi" to getCpuAbi(),
                "androidSdk" to getAndroidSdkVersion(),
                "isLowRamDevice" to isLowRamDevice(),
                "lastBatteryLevel" to getBatteryLevel(),
                "isCharging" to isCharging(),
                "batteryHealth" to getBatteryHealth(),
                "isPowerSaveMode" to isPowerSaveMode(),
                "batterySnapshot" to getBatterySnapshot(),
                "freeRam" to getFreeRam(),
                "freeStorage" to getFreeStorage(),
                "totalRamInGb" to getTotalRamInGigabytes(),
                "networkState" to getNetworkState(),
                "networkPing" to if (includePing) getNetworkPing() else "N/A",
                "deviceUptime" to getDeviceUptime(),
                "timestamp" to System.currentTimeMillis()
            )

            if (includeLocation) {
                val loc = getCurrentLocation()
                if (loc != null) {
                    baseMap["locationLat"] = loc.latitude
                    baseMap["locationLng"] = loc.longitude
                    baseMap["locationTimestamp"] = loc.timestamp
                } else {
                    Log.w(TelemetryConstants.TAG, "Координаты недоступны для телеметрии")
                }
            }

            baseMap
        } finally {
            collectionTrace?.stop()
        }

        return telemetry
    }

    fun startCustomTrace(name: String): Trace? {
        return try {
            FirebasePerformance.getInstance().newTrace(name).apply { start() }
        } catch (e: Exception) {
            Log.e(TelemetryConstants.TAG, "Ошибка создания трейса: $name", e)
            null
        }
    }

    fun stopTrace(trace: Trace?) {
        trace?.let {
            try {
                it.stop()
            } catch (e: Exception) {
                Log.e(TelemetryConstants.TAG, "Ошибка остановки трейса", e)
            }
        }
    }
}