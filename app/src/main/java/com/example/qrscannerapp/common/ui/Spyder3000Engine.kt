package com.example.qrscannerapp.common.ui

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.*
import com.example.qrscannerapp.TelemetryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

// =============================================================================
// SPYDER PERFORMANCE ENGINE
// Мониторинг FPS, температуры, профили рендеринга.
// Живёт в common/ui рядом с TransitionEngine и AppBackground.
// Испускает SpyderConfig напрямую — мост в MainApp не нужен.
// =============================================================================

data class SpyderPerformanceProfile(
    val name: String,
    val targetFps: Int,
    val springDamping: Float,
    val springStiffness: Float,
    val shaderQuality: ShaderQuality,
    val description: String
)

enum class ShaderQuality { LOW, MEDIUM, HIGH, ULTRA }

// =============================================================================
// HYSTERESIS CONFIG — предотвращает осцилляцию на границах профилей
// =============================================================================

data class HysteresisRange(
    val name: String,
    val enterBelowFps: Float,    // входим в профиль когда FPS падает ниже
    val exitAboveFps: Float,     // выходим из профиля когда FPS стабильно выше
    val enterAboveTemp: Float,   // входим при температуре выше
    val exitBelowTemp: Float,    // выходим при температуре ниже
    val tempRiseRateThreshold: Float, // °C/сек — упреждающий вход
    val stabilityWindowMs: Long = 2000L  // сколько держать стабильный FPS перед выходом
)

@Singleton
class Spyder3000Engine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telemetryManager: TelemetryManager
) {

    private val _currentProfile = MutableStateFlow(
        SpyderPerformanceProfile(
            name = "Balanced",
            targetFps = 60,
            springDamping = 0.52f,
            springStiffness = 520f,
            shaderQuality = ShaderQuality.MEDIUM,
            description = "Оптимальный баланс производительности"
        )
    )
    val currentProfile: StateFlow<SpyderPerformanceProfile> = _currentProfile.asStateFlow()

    private val _fps = MutableStateFlow(60f)
    val fps: StateFlow<Float> = _fps.asStateFlow()

    private val _totalAnimationsCount = MutableStateFlow(0)
    val totalAnimationsCount: StateFlow<Int> = _totalAnimationsCount.asStateFlow()

    private val _frameTime = MutableStateFlow(16.6f)
    val frameTime: StateFlow<Float> = _frameTime.asStateFlow()

    private val _droppedFrames = MutableStateFlow(0)
    val droppedFrames: StateFlow<Int> = _droppedFrames.asStateFlow()

    val availableProfiles = listOf(
        SpyderPerformanceProfile(
            name = "Power Saving",
            targetFps = 30,
            springDamping = 0.70f,
            springStiffness = 380f,
            shaderQuality = ShaderQuality.LOW,
            description = "Экономия батареи"
        ),
        SpyderPerformanceProfile(
            name = "Balanced",
            targetFps = 60,
            springDamping = 0.52f,
            springStiffness = 520f,
            shaderQuality = ShaderQuality.MEDIUM,
            description = "Оптимальный баланс"
        ),
        SpyderPerformanceProfile(
            name = "Performance",
            targetFps = 90,
            springDamping = 0.45f,
            springStiffness = 650f,
            shaderQuality = ShaderQuality.HIGH,
            description = "Максимальная частота"
        ),
        SpyderPerformanceProfile(
            name = "Spyder Max",
            targetFps = 120,
            springDamping = 0.38f,
            springStiffness = 780f,
            shaderQuality = ShaderQuality.ULTRA,
            description = "Для устройств с 120Hz"
        )
    )

    // =============================================================================
    // ГИСТЕРЕЗИСНЫЕ ДИАПАЗОНЫ
    // =============================================================================

    private val hysteresisRanges = mapOf(
        "Power Saving" to HysteresisRange(
            name = "Power Saving",
            enterBelowFps = 42f,
            exitAboveFps = 52f,
            enterAboveTemp = 48f,
            exitBelowTemp = 43f,
            tempRiseRateThreshold = 0.25f
        ),
        "Balanced" to HysteresisRange(
            name = "Balanced",
            enterBelowFps = 55f,
            exitAboveFps = 65f,
            enterAboveTemp = 42f,
            exitBelowTemp = 38f,
            tempRiseRateThreshold = 0.30f
        ),
        "Performance" to HysteresisRange(
            name = "Performance",
            enterBelowFps = 78f,
            exitAboveFps = 92f,
            enterAboveTemp = 38f,
            exitBelowTemp = 34f,
            tempRiseRateThreshold = 0.35f
        ),
        "Spyder Max" to HysteresisRange(
            name = "Spyder Max",
            enterBelowFps = 105f,
            exitAboveFps = 118f,
            enterAboveTemp = 34f,
            exitBelowTemp = 30f,
            tempRiseRateThreshold = 0.40f
        )
    )

    // История температур для расчёта тренда
    private val tempHistory = mutableListOf<Pair<Long, Float>>() // timestamp to temp
    private val fpsHistory = mutableListOf<Pair<Long, Float>>()
    private var currentStableFps = 60f
    private var stableSince = System.currentTimeMillis()

    // =============================================================================
    // УПРАВЛЕНИЕ ПРОФИЛЯМИ
    // =============================================================================

    fun setPerformanceProfile(profile: SpyderPerformanceProfile) {
        _currentProfile.update { profile }
        // Обновляем лимиты анимаций в зависимости от профиля
        val tier = when (profile.targetFps) {
            30 -> 0; 60 -> 1; 90 -> 2; else -> 3
        }
        ActiveAnimationTracker.updateLimits(tier)
    }

    fun updatePerformanceMetrics(fps: Float, frameTimeMs: Float, dropped: Int) {
        _fps.update { fps }
        _frameTime.update { frameTimeMs }
        _droppedFrames.update { dropped }
        _totalAnimationsCount.update { ActiveAnimationTracker.getTotalCount() }

        // Отслеживаем стабильность FPS
        val now = System.currentTimeMillis()
        fpsHistory.add(now to fps)
        if (fpsHistory.size > 100) fpsHistory.removeAt(0)

        // Проверяем смену профиля по гистерезису
        checkProfileHysteresis(fps, now)
    }

    private fun checkProfileHysteresis(fps: Float, now: Long) {
        val current = _currentProfile.value
        val range = hysteresisRanges[current.name] ?: return

        // Стабильность: если FPS стабильно выше порога — запоминаем
        if (fps >= range.exitAboveFps) {
            if (currentStableFps < range.exitAboveFps) {
                currentStableFps = fps
                stableSince = now
            }
        } else {
            currentStableFps = fps
            stableSince = now
        }

        val stableDuration = now - stableSince

        // Проверяем выход в более высокий профиль
        val higherProfile = getHigherProfile(current)
        if (higherProfile != null &&
            stableDuration >= range.stabilityWindowMs &&
            currentStableFps >= range.exitAboveFps
        ) {
            setPerformanceProfile(higherProfile)
            return
        }

        // Проверяем падение в более низкий профиль
        val lowerProfile = getLowerProfile(current)
        if (lowerProfile != null && fps <= range.enterBelowFps) {
            setPerformanceProfile(lowerProfile)
        }
    }

    private fun getHigherProfile(current: SpyderPerformanceProfile): SpyderPerformanceProfile? {
        val profiles = listOf("Power Saving", "Balanced", "Performance", "Spyder Max")
        val idx = profiles.indexOf(current.name)
        if (idx < profiles.size - 1) {
            return availableProfiles.find { it.name == profiles[idx + 1] }
        }
        return null
    }

    private fun getLowerProfile(current: SpyderPerformanceProfile): SpyderPerformanceProfile? {
        val profiles = listOf("Power Saving", "Balanced", "Performance", "Spyder Max")
        val idx = profiles.indexOf(current.name)
        if (idx > 0) {
            return availableProfiles.find { it.name == profiles[idx - 1] }
        }
        return null
    }

    fun incrementAnimationCounter() {
        _totalAnimationsCount.update { ActiveAnimationTracker.getTotalCount() }
    }

    fun resetAnimationCounter() {
        ActiveAnimationTracker.resetAll()
        _totalAnimationsCount.update { 0 }
    }

    fun getSpringSpec(
        dampingRatio: Float = _currentProfile.value.springDamping,
        stiffness: Float = _currentProfile.value.springStiffness
    ): SpringSpec<Float> = spring(dampingRatio, stiffness)

    fun getEasing(): CubicBezierEasing =
        if (_currentProfile.value.targetFps <= 30)
            CubicBezierEasing(0.55f, 0.0f, 0.45f, 1.0f)
        else
            CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)

    fun getTargetFrameTimeMs(): Float = 1000f / _currentProfile.value.targetFps

    private val _batteryTemp = MutableStateFlow(0f)
    val batteryTemp: StateFlow<Float> = _batteryTemp.asStateFlow()

    private val _thermalStatus = MutableStateFlow(0)
    val thermalStatus: StateFlow<Int> = _thermalStatus.asStateFlow()

    // Пользовательские настройки анимации
    private val _userConfig = MutableStateFlow(SpyderConfig())
    val userConfig: StateFlow<SpyderConfig> = _userConfig.asStateFlow()

    fun updateUserConfig(config: SpyderConfig) {
        _userConfig.update { config }
        applyAutoThrottle()
    }

    // Итоговый конфиг = пользовательский + тепловые поправки
    private val _renderConfig = MutableStateFlow(SpyderConfig())
    val renderConfig: StateFlow<SpyderConfig> = _renderConfig.asStateFlow()

    // =============================================================================
    // УПРЕЖДАЮЩИЙ ТРОТТЛИНГ ПО ТРЕНДУ ТЕМПЕРАТУРЫ
    // =============================================================================

    private var lastTempCheck = System.currentTimeMillis()
    private var lastTemp = 0f

    fun updateThermalMetrics() {
        val temp = telemetryManager.getBatteryTemperatureCelsius()
        _batteryTemp.update { temp }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(PowerManager::class.java)
            _thermalStatus.update { pm.currentThermalStatus }
        }

        // Отслеживаем тренд температуры
        val now = System.currentTimeMillis()
        tempHistory.add(now to temp)
        if (tempHistory.size > 30) tempHistory.removeAt(0)

        // Рассчитываем скорость роста (°C/сек)
        val tempRiseRate = calculateTempRiseRate()

        lastTemp = temp
        lastTempCheck = now

        applyAutoThrottle(tempRiseRate)
    }

    private fun calculateTempRiseRate(): Float {
        if (tempHistory.size < 5) return 0f
        val recent = tempHistory.takeLast(5)
        val first = recent.first()
        val last = recent.last()
        val durationSec = (last.first - first.first) / 1000f
        if (durationSec <= 0f) return 0f
        return (last.second - first.second) / durationSec
    }

    private fun applyAutoThrottle(tempRiseRate: Float = 0f) {
        val temp = _batteryTemp.value
        val thermal = _thermalStatus.value
        val range = hysteresisRanges[_currentProfile.value.name]

        // Упреждающий throttle: если температура растёт быстро — реагируем заранее
        val preemptiveLevel = if (range != null && tempRiseRate > range.tempRiseRateThreshold) {
            1 // мягкий throttle, не дожидаясь достижения порога
        } else 0

        val level = when {
            thermal >= 4 || temp > 52f -> 3
            thermal >= 3 || temp > 45f -> maxOf(2, preemptiveLevel)
            thermal >= 2 || temp > 40f -> maxOf(1, preemptiveLevel)
            preemptiveLevel > 0 -> 1
            else -> 0
        }

        val base = _userConfig.value

        // Рассчитываем интервал тика шейдеров в зависимости от уровня
        val shaderTick = when (level) {
            0 -> 66L   // ~15fps
            1 -> 80L   // ~12fps
            2 -> 100L  // ~10fps
            3 -> 132L  // ~7.5fps
            else -> 66L
        }

        // Лимиты анимаций в зависимости от уровня
        val maxContent = when (level) {
            0 -> 24; 1 -> 16; 2 -> 10; else -> 6
        }
        val maxDecorative = when (level) {
            0 -> 12; 1 -> 6; 2 -> 0; else -> 0
        }

        val config = when (level) {
            1 -> base.copy(
                dampingMul = base.dampingMul * 1.15f,
                stiffnessMul = base.stiffnessMul * 0.80f,
                throttleLevel = 1,
                decorativeEnabled = true,
                shaderTickIntervalMs = shaderTick,
                maxContentAnimations = maxContent,
                maxDecorativeAnimations = maxDecorative
            )
            2 -> base.copy(
                dampingMul = base.dampingMul * 1.35f,
                stiffnessMul = base.stiffnessMul * 0.60f,
                throttleLevel = 2,
                decorativeEnabled = false,
                shaderTickIntervalMs = shaderTick,
                maxContentAnimations = maxContent,
                maxDecorativeAnimations = maxDecorative
            )
            3 -> base.copy(
                dampingMul = base.dampingMul * 1.60f,
                stiffnessMul = base.stiffnessMul * 0.45f,
                throttleLevel = 3,
                decorativeEnabled = false,
                shaderTickIntervalMs = shaderTick,
                maxContentAnimations = maxContent,
                maxDecorativeAnimations = maxDecorative
            )
            else -> base.copy(
                throttleLevel = 0,
                decorativeEnabled = true,
                shaderTickIntervalMs = 66L,
                maxContentAnimations = 24,
                maxDecorativeAnimations = 12
            )
        }

        _renderConfig.update { config }
        ActiveAnimationTracker.updateLimits(
            when {
                maxDecorative <= 3 -> 0
                maxDecorative <= 8 -> 1
                maxDecorative <= 12 -> 2
                else -> 3
            }
        )
    }

    // =============================================================================
    // ДИАГНОСТИКА
    // =============================================================================

    fun getDiagnostics(): Map<String, Any> {
        return mapOf(
            "profile" to _currentProfile.value.name,
            "fps" to _fps.value,
            "frameTimeMs" to _frameTime.value,
            "droppedFrames" to _droppedFrames.value,
            "batteryTemp" to _batteryTemp.value,
            "thermalStatus" to _thermalStatus.value,
            "throttleLevel" to _renderConfig.value.throttleLevel,
            "decorativeEnabled" to _renderConfig.value.decorativeEnabled,
            "activeAnimations" to ActiveAnimationTracker.getTotalCount(),
            "activeContent" to ActiveAnimationTracker.getCount(AnimationTier.CONTENT),
            "activeDecorative" to ActiveAnimationTracker.getCount(AnimationTier.DECORATIVE),
            "activeCritical" to ActiveAnimationTracker.getCount(AnimationTier.CRITICAL),
            "tempRiseRate" to calculateTempRiseRate(),
            "shaderTickMs" to _renderConfig.value.shaderTickIntervalMs
        )
    }
}