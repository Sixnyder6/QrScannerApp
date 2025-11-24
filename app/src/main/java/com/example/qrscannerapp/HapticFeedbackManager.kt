package com.example.qrscannerapp

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Менеджер для управления тактильной обратной связью (вибрацией).
 * Учитывает системные настройки и предоставляет простые методы для вызова
 * стандартных тактильных эффектов.
 *
 * @param settingsManager Менеджер настроек, чтобы проверять, включена ли вибрация.
 */
class HapticFeedbackManager(private val settingsManager: SettingsManager) {

    /**
     * Вызывает короткую вибрацию, соответствующую простому клику.
     * Эффект сработает только если вибрация включена в настройках приложения.
     *
     * @param hapticFeedback Экземпляр HapticFeedback, получаемый из LocalHapticFeedback.current.
     * @param scope CoroutineScope для асинхронного чтения настроек.
     */
    fun performClick(hapticFeedback: HapticFeedback, scope: CoroutineScope) {
        scope.launch {
            if (settingsManager.isVibrationEnabledFlow.first()) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    /**
     * Вызывает более сильную вибрацию, соответствующую подтверждению действия
     * (например, успешное сохранение).
     * Эффект сработает только если вибрация включена в настройках приложения.
     *
     * @param hapticFeedback Экземпляр HapticFeedback, получаемый из LocalHapticFeedback.current.
     * @param scope CoroutineScope для асинхронного чтения настроек.
     */
    fun performConfirm(hapticFeedback: HapticFeedback, scope: CoroutineScope) {
        scope.launch {
            if (settingsManager.isVibrationEnabledFlow.first()) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
}