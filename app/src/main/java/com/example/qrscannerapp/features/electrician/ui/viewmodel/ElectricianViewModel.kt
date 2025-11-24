package com.example.qrscannerapp.features.electrician.ui.viewmodel // <-- ИСПРАВЛЕННЫЙ ПАКЕТ

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager // <-- ИСПРАВЛЕНО
import com.example.qrscannerapp.HapticFeedbackManager // <-- ИСПРАВЛЕНО
import com.example.qrscannerapp.features.electrician.domain.model.BatteryRepairLog
import com.example.qrscannerapp.features.electrician.domain.model.ElectricianUiState
import com.example.qrscannerapp.features.electrician.domain.model.Manufacturer
import com.example.qrscannerapp.features.electrician.domain.model.RepairType
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ElectricianViewModel(
    private val authManager: AuthManager,
    private val hapticManager: HapticFeedbackManager,
    // --- НАЧАЛО ИЗМЕНЕНИЙ: Принимаем hapticFeedback и scope ---
    private val hapticFeedback: HapticFeedback,
    private val scope: CoroutineScope
    // --- КОНЕЦ ИЗМЕНЕНИЙ ---
) : ViewModel() {
    private val db = Firebase.firestore
    private val _uiState = MutableStateFlow(ElectricianUiState())
    val uiState = _uiState.asStateFlow()

    companion object {
        private val VALID_ID_REGEX = Regex("[A-Z0-9]+")
        private const val MAX_ID_LENGTH = 14
    }

    private fun startBatteryCheck(batteryId: String) {
        if (_uiState.value.isCheckingHistory && _uiState.value.scannedBatteryId == batteryId) {
            return
        }
        // --- НАЧАЛО ИЗМЕНЕНИЙ: Обновляем вызов ---
        hapticManager.performConfirm(hapticFeedback, scope)
        // --- КОНЕЦ ИЗМЕНЕНИЙ ---
        _uiState.update {
            it.copy(
                scannedBatteryId = batteryId,
                isCheckingHistory = true,
                batteryHistory = null,
                error = null,
                showManualInputDialog = false,
                manualInputText = ""
            )
        }
        checkBatteryHistory(batteryId)
    }

    fun onQrCodeScanned(code: String) {
        startBatteryCheck(code)
    }
    private fun checkBatteryHistory(batteryId: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("battery_repair_log")
                    .whereEqualTo("batteryId", batteryId)
                    .get()
                    .await()
                val history = snapshot.toObjects<BatteryRepairLog>()
                _uiState.update { it.copy(batteryHistory = history, isCheckingHistory = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCheckingHistory = false, error = "Ошибка при проверке истории: ${e.message}") }
            }
        }
    }
    fun onStartRepair() {
        _uiState.update { it.copy(isRepairMode = true) }
    }
    fun cancelScan() {
        _uiState.value = ElectricianUiState()
    }

    fun onShowManualInputDialog() {
        _uiState.update { it.copy(showManualInputDialog = true) }
    }
    fun onDismissManualInputDialog() {
        _uiState.update { it.copy(showManualInputDialog = false, manualInputText = "") }
    }

    fun onManualInputTextChanged(newText: String) {
        val filteredText = newText
            .uppercase()
            .filter { it.isLetterOrDigit() }
            .take(MAX_ID_LENGTH)
        _uiState.update { it.copy(manualInputText = filteredText) }
    }

    fun onConfirmManualInput() {
        val batteryId = _uiState.value.manualInputText.trim()
        if (batteryId.length != MAX_ID_LENGTH || !batteryId.matches(VALID_ID_REGEX)) {
            _uiState.update {
                it.copy(error = "ID должен содержать ровно 14 заглавных букв и/или цифр.")
            }
            return
        }
        startBatteryCheck(batteryId)
    }

    fun toggleFlashlight() {
        _uiState.update { it.copy(isFlashlightOn = !it.isFlashlightOn) }
    }

    fun onRepairTypeToggle(repairType: RepairType) {
        _uiState.update { currentState ->
            val updatedRepairs = currentState.selectedRepairs.toMutableSet()
            var updatedCustomText = currentState.customRepairText
            if (updatedRepairs.contains(repairType)) {
                updatedRepairs.remove(repairType)
                if (repairType == RepairType.OTHER) {
                    updatedCustomText = ""
                }
            } else {
                updatedRepairs.add(repairType)
            }
            currentState.copy(
                selectedRepairs = updatedRepairs,
                customRepairText = updatedCustomText
            )
        }
    }
    fun onManufacturerSelected(manufacturer: Manufacturer) {
        _uiState.update { it.copy(selectedManufacturer = manufacturer) }
    }
    fun onCustomRepairTextChanged(newText: String) {
        _uiState.update { it.copy(customRepairText = newText) }
    }
    fun submitRepairLog() {
        val currentState = _uiState.value
        val currentUser = authManager.authState.value
        if (currentState.scannedBatteryId == null || currentUser.userId == null) {
            _uiState.update { it.copy(error = "ID аккумулятора должен быть указан") }
            return
        }

        if (currentState.selectedRepairs.isEmpty() ||
            (currentState.selectedRepairs.contains(RepairType.OTHER) && currentState.customRepairText.isBlank())
        ) {
            _uiState.update { it.copy(error = "Выберите тип ремонта (и опишите, если 'Другое')") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            val finalRepairsList = currentState.selectedRepairs.map { repairType ->
                if (repairType == RepairType.OTHER) {
                    currentState.customRepairText.trim()
                } else {
                    repairType.displayName
                }
            }

            val newLog = BatteryRepairLog(
                batteryId = currentState.scannedBatteryId,
                electricianId = currentUser.userId,
                electricianName = (currentUser.userName ?: "Unknown"),
                timestamp = System.currentTimeMillis(),
                repairs = finalRepairsList,
                manufacturer = currentState.selectedManufacturer.displayName
            )

            try {
                db.collection("battery_repair_log").document(newLog.id).set(newLog).await()
                _uiState.update { it.copy(saveCompleted = true) }
                resetStateAfterDelay()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Ошибка сохранения: ${e.message}") }
            }
        }
    }
    private fun resetStateAfterDelay() {
        viewModelScope.launch {
            delay(500)
            _uiState.value = ElectricianUiState()
        }
    }
}

class ElectricianViewModelFactory(
    private val authManager: AuthManager,
    private val hapticManager: HapticFeedbackManager,
    // --- НАЧАЛО ИЗМЕНЕНИЙ: Принимаем hapticFeedback и scope ---
    private val hapticFeedback: HapticFeedback,
    private val scope: CoroutineScope
    // --- КОНЕЦ ИЗМЕНЕНИЙ ---
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ElectricianViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // --- НАЧАЛО ИЗМЕНЕНИЙ: Передаем hapticFeedback и scope в ViewModel ---
            return ElectricianViewModel(authManager, hapticManager, hapticFeedback, scope) as T
            // --- КОНЕЦ ИЗМЕНЕНИЙ ---
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}