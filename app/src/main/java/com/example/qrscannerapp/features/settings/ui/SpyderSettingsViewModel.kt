package com.example.qrscannerapp.features.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.DialogAnimation
import com.example.qrscannerapp.SettingsManager
import com.example.qrscannerapp.common.ui.Spyder3000Engine
import com.example.qrscannerapp.common.ui.SpyderConfig
import com.example.qrscannerapp.common.ui.SpyderPerformanceProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import android.view.Choreographer
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject

@HiltViewModel
class SpyderSettingsViewModel @Inject constructor(
    val spyderEngine: Spyder3000Engine,
    private val repository: SpyderAnimationRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpyderUiState())
    val uiState: StateFlow<SpyderUiState> = _uiState.asStateFlow()

    private val _autoUpdateEnabled = MutableStateFlow(true)
    val autoUpdateEnabled: StateFlow<Boolean> = _autoUpdateEnabled.asStateFlow()

    private val _currentDialogAnimation = MutableStateFlow(DialogAnimation.SCALE_BOUNCY)
    val currentDialogAnimation: StateFlow<DialogAnimation> = _currentDialogAnimation.asStateFlow()

    private val _showFpsOverlay = MutableStateFlow(false)
    val showFpsOverlay: StateFlow<Boolean> = _showFpsOverlay.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                spyderEngine.currentProfile,
                spyderEngine.fps,
                spyderEngine.frameTime,
                spyderEngine.droppedFrames,
                spyderEngine.totalAnimationsCount
            ) { profile, fps, frameTime, dropped, animCount ->
                SpyderUiState(
                    performanceProfile = profile,
                    currentFps = fps,
                    currentFrameTime = frameTime,
                    droppedFrames = dropped,
                    totalAnimations = animCount
                )
            }.collect { newState ->
                _uiState.update { current ->
                    newState.copy(
                        batteryTemp = current.batteryTemp,
                        thermalStatus = current.thermalStatus,
                        recentAnimations = current.recentAnimations
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getRecentAnimations().collect { animations ->
                _uiState.update { it.copy(recentAnimations = animations) }
            }
        }

        viewModelScope.launch {
            settingsManager.dialogAnimationFlow.collect { animation ->
                _currentDialogAnimation.update { animation }
            }
        }

        viewModelScope.launch {
            spyderEngine.batteryTemp.collect { temp ->
                _uiState.update { it.copy(batteryTemp = temp) }
            }
        }

        viewModelScope.launch {
            spyderEngine.thermalStatus.collect { status ->
                _uiState.update { it.copy(thermalStatus = status) }
            }
        }

        viewModelScope.launch {
            while (true) {
                spyderEngine.updateThermalMetrics()
                kotlinx.coroutines.delay(2000)
            }
        }

        startFpsSimulation()
    }

    private fun startFpsSimulation() {
        viewModelScope.launch(Dispatchers.Main) {
            val choreographer = Choreographer.getInstance()
            val frameTimestamps = ArrayDeque<Long>()
            val windowNanos = 500_000_000L // скользящее окно 500мс
            var prevFrameNanos = System.nanoTime()

            while (true) {
                prevFrameNanos = suspendCancellableCoroutine { cont ->
                    choreographer.postFrameCallback { frameTimeNanos ->
                        if (cont.isActive) {
                            val frameTimeMs = (frameTimeNanos - prevFrameNanos) / 1_000_000f

                            frameTimestamps.addLast(frameTimeNanos)
                            val cutoff = frameTimeNanos - windowNanos
                            while (frameTimestamps.isNotEmpty() && frameTimestamps.first() < cutoff) {
                                frameTimestamps.removeFirst()
                            }

                            val smoothFps = if (frameTimestamps.size >= 2) {
                                val span = (frameTimestamps.last() - frameTimestamps.first()) / 1_000_000_000f
                                if (span > 0f) (frameTimestamps.size - 1) / span else 0f
                            } else 0f

                            val dropped = if (smoothFps > 0f && smoothFps < spyderEngine.currentProfile.value.targetFps - 10) 1 else 0
                            spyderEngine.updatePerformanceMetrics(
                                fps = smoothFps.coerceIn(1f, 200f),
                                frameTimeMs = frameTimeMs,
                                dropped = _uiState.value.droppedFrames + dropped
                            )
                            cont.resume(frameTimeNanos)
                        }
                    }
                }
            }
        }
    }

    fun toggleAutoUpdate() {
        _autoUpdateEnabled.update { !it }
    }

    fun setDialogAnimation(animation: DialogAnimation) {
        _currentDialogAnimation.update { animation }
        viewModelScope.launch {
            settingsManager.setDialogAnimation(animation)
            repository.logAnimation(
                type = "dialog_${animation.name}",
                durationMs = 300,
                fps = _uiState.value.currentFps,
                frameTimeMs = _uiState.value.currentFrameTime
            )
        }
    }

    val animConfig: StateFlow<SpyderConfig> = spyderEngine.userConfig

    // renderConfig — что движок реально применяет (userConfig + тепловые поправки)
    val renderConfig: StateFlow<SpyderConfig> = spyderEngine.renderConfig

    fun updateAnimConfig(config: SpyderConfig) {
        spyderEngine.updateUserConfig(config)
    }

    fun setPerformanceProfile(profile: SpyderPerformanceProfile) {
        spyderEngine.setPerformanceProfile(profile)
        viewModelScope.launch {
            repository.logAnimation(
                type = "profile_${profile.name}",
                durationMs = 0,
                fps = _uiState.value.currentFps,
                frameTimeMs = _uiState.value.currentFrameTime
            )
        }
    }

    fun toggleFpsOverlay() {
        _showFpsOverlay.update { !it }
    }

    fun resetStatistics() {
        spyderEngine.resetAnimationCounter()
        viewModelScope.launch {
            repository.clearOldLogs(0)
        }
    }
}

data class SpyderUiState(
    val performanceProfile: SpyderPerformanceProfile? = null,
    val currentFps: Float = 0f,
    val currentFrameTime: Float = 0f,
    val droppedFrames: Int = 0,
    val totalAnimations: Int = 0,
    val recentAnimations: List<SpyderAnimationLog> = emptyList(),
    val batteryTemp: Float = 0f,
    val thermalStatus: Int = 0
)