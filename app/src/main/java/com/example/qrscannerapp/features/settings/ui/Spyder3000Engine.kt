package com.example.qrscannerapp.features.settings.ui

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

data class SpyderRenderConfig(
    val dampingMultiplier: Float = 1f,
    val stiffnessMultiplier: Float = 1f,
    val throttleLevel: Int = 0  // 0=норма, 1=тепло, 2=горячо, 3=критично
)

data class SpyderPerformanceProfile(
    val name: String,
    val targetFps: Int,
    val springDamping: Float,
    val springStiffness: Float,
    val shaderQuality: ShaderQuality,
    val description: String
)

enum class ShaderQuality {
    LOW, MEDIUM, HIGH, ULTRA
}

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

    fun setPerformanceProfile(profile: SpyderPerformanceProfile) {
        _currentProfile.update { profile }
    }

    fun updatePerformanceMetrics(fps: Float, frameTimeMs: Float, dropped: Int) {
        _fps.update { fps }
        _frameTime.update { frameTimeMs }
        _droppedFrames.update { dropped }
    }

    fun incrementAnimationCounter() {
        _totalAnimationsCount.update { it + 1 }
    }

    fun resetAnimationCounter() {
        _totalAnimationsCount.update { 0 }
    }

    fun getSpringSpec(
        dampingRatio: Float = _currentProfile.value.springDamping,
        stiffness: Float = _currentProfile.value.springStiffness
    ): SpringSpec<Float> {
        return spring(dampingRatio, stiffness)
    }

    fun getEasing(): CubicBezierEasing {
        return if (_currentProfile.value.targetFps <= 30) {
            CubicBezierEasing(0.55f, 0.0f, 0.45f, 1.0f)
        } else {
            CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
        }
    }

    fun getTargetFrameTimeMs(): Float = 1000f / _currentProfile.value.targetFps

    private val _batteryTemp = MutableStateFlow(0f)
    val batteryTemp: StateFlow<Float> = _batteryTemp.asStateFlow()

    private val _thermalStatus = MutableStateFlow(0)
    val thermalStatus: StateFlow<Int> = _thermalStatus.asStateFlow()

    private val _renderConfig = MutableStateFlow(SpyderRenderConfig())
    val renderConfig: StateFlow<SpyderRenderConfig> = _renderConfig.asStateFlow()

    fun updateThermalMetrics() {
        _batteryTemp.update { telemetryManager.getBatteryTemperatureCelsius() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(PowerManager::class.java)
            _thermalStatus.update { pm.currentThermalStatus }
        }
        applyAutoThrottle()
    }

    private fun applyAutoThrottle() {
        val temp = _batteryTemp.value
        val thermal = _thermalStatus.value
        val level = when {
            thermal >= 4 || temp > 52f -> 3
            thermal >= 3 || temp > 45f -> 2
            thermal >= 2 || temp > 40f -> 1
            else -> 0
        }
        val config = when (level) {
            1 -> SpyderRenderConfig(dampingMultiplier = 1.15f, stiffnessMultiplier = 0.80f, throttleLevel = 1)
            2 -> SpyderRenderConfig(dampingMultiplier = 1.35f, stiffnessMultiplier = 0.60f, throttleLevel = 2)
            3 -> SpyderRenderConfig(dampingMultiplier = 1.60f, stiffnessMultiplier = 0.45f, throttleLevel = 3)
            else -> SpyderRenderConfig()
        }
        _renderConfig.update { config }
    }
}