// Файл: common/ui/AppBackgroundViewModel.kt
package com.example.qrscannerapp.common.ui

import androidx.lifecycle.ViewModel
import com.example.qrscannerapp.performance.DevicePerformanceManager
import com.example.qrscannerapp.performance.PerformanceClass
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Специализированная ViewModel-прослойка для AppBackground.
 * Ее единственная задача - предоставить доступ к DevicePerformanceManager
 * в рамках, совместимых с функцией hiltViewModel().
 */
@HiltViewModel
class AppBackgroundViewModel @Inject constructor(
    // Hilt автоматически внедрит сюда наш Singleton
    private val performanceManager: DevicePerformanceManager
) : ViewModel() {

    /**
     * Предоставляет решение "мозга" для UI.
     * Значение вычисляется один раз и кэшируется.
     */
    val performanceClass: PerformanceClass = performanceManager.currentPerformanceClass
}