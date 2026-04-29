package com.example.qrscannerapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.core.image.ImagePreloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashScreenViewModel @Inject constructor(
    private val imagePreloader: ImagePreloader,
    private val settingsManager: SettingsManager,
    private val authManager: AuthManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _loadingStatus = MutableStateFlow("Загрузка...")
    val loadingStatus = _loadingStatus.asStateFlow()

    companion object {
        private const val MIN_SPLASH_DURATION_MS = 5000L
    }

    init {
        startLoading()
    }

    private fun startLoading() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            val needsPreloading = !settingsManager.isCatalogPrecached()

            if (needsPreloading) {
                _loadingStatus.value = "Подготовка каталога..."
                try {
                    imagePreloader.preloadCatalogImages()
                    settingsManager.setCatalogPrecached(true)
                    _loadingStatus.value = "Почти готово..."
                } catch (e: Exception) {
                    _loadingStatus.value = "Загрузка..."
                    e.printStackTrace()
                }
            } else {
                _loadingStatus.value = "Все готово!"
            }

            // Ждём минимальное время анимации
            val elapsed = System.currentTimeMillis() - startTime
            val remainingDelay = MIN_SPLASH_DURATION_MS - elapsed
            if (remainingDelay > 0) {
                delay(remainingDelay)
            }

            // Ждём пока authState загрузится — чтобы NavHost знал роль
            // и стартовал сразу с правильного экрана
            authManager.authState.first { !it.isLoading }

            _isLoading.value = false
        }
    }
}