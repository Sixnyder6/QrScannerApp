// FILE: com/example/qrscannerapp/features/visual_repair/ui/VisualRepairViewModel.kt
package com.example.qrscannerapp.features.visual_repair.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class VisualRepairViewModel @Inject constructor() : ViewModel() {

    // ViewModel теперь отвечает только за состояние, а не за создание 3D-объектов.
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isAutoRotating = MutableStateFlow(false)
    val isAutoRotating = _isAutoRotating.asStateFlow()

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    fun toggleAutoRotation() {
        _isAutoRotating.value = !_isAutoRotating.value
    }

    fun stopAutoRotation() {
        _isAutoRotating.value = false
    }
}