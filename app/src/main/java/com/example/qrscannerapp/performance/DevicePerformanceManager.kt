// Файл: performance/DevicePerformanceManager.kt
package com.example.qrscannerapp.performance

// V-- ДОБАВЛЕН ИМПОРТ ДЛЯ ЛОГГИРОВАНИЯ --V
import android.util.Log
import com.example.qrscannerapp.TelemetryManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Мозг" приложения, отвечающий за классификацию производительности устройства.
 * Этот менеджер анализирует телеметрию и выносит вердикт, который
 * используется для адаптации UI (например, включения/отключения анимаций).
 *
 * Является Singleton, так как производительность устройства - это глобальное
 * и редко изменяющееся состояние.
 */
@Singleton
class DevicePerformanceManager @Inject constructor(
    private val telemetryManager: TelemetryManager
) {

    /**
     * Текущий определенный класс производительности устройства.
     * Вычисляется один раз при инициализации для максимальной эффективности.
     */
    val currentPerformanceClass: PerformanceClass

    init {
        currentPerformanceClass = classifyDevicePerformance()
        // V-- ДОБАВЛЕНА СТРОКА ДЛЯ ДИАГНОСТИКИ --V
        // Этот лог покажет в Logcat, какое решение было принято и почему.
        Log.d("Cortex", "Device classified as: $currentPerformanceClass. Power Save Mode is: ${telemetryManager.isPowerSaveMode()}")
    }

    private fun classifyDevicePerformance(): PerformanceClass {
        // Эвристика №1: Режим энергосбережения имеет наивысший приоритет.
        if (telemetryManager.isPowerSaveMode()) {
            return PerformanceClass.NONE
        }

        // Эвристика №2: Классификация на основе общего объема RAM.
        val totalRamGb = telemetryManager.getTotalRamInGigabytes()

        // Поведение по умолчанию (fallback): Если не удалось получить RAM,
        // считаем устройство маломощным для безопасности.
        if (totalRamGb < 0) {
            return PerformanceClass.LOW
        }

        // Основное правило классификации.
        return if (totalRamGb > 4.0) {
            PerformanceClass.HIGH
        } else {
            PerformanceClass.LOW
        }
    }
}