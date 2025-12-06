// Полное содержимое для НОВОГО файла SplashScreenViewModel.kt

package com.example.qrscannerapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.core.image.ImagePreloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashScreenViewModel @Inject constructor(
    private val imagePreloader: ImagePreloader,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _loadingStatus = MutableStateFlow("Загрузка...")
    val loadingStatus = _loadingStatus.asStateFlow()

    init {
        startLoading()
    }

    private fun startLoading() {
        viewModelScope.launch {
            val needsPreloading = !settingsManager.isCatalogPrecached()

            if (needsPreloading) {
                _loadingStatus.value = "Подготовка каталога..."
                try {
                    imagePreloader.preloadCatalogImages()
                    settingsManager.setCatalogPrecached(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                _loadingStatus.value = "Все готово!"
            }
            _isLoading.value = false
        }
    }
}