package com.example.qrscannerapp.features.interaction.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.core.model.ScanEvent
import com.example.qrscannerapp.features.interaction.data.repository.InteractionRepository
import com.example.qrscannerapp.features.interaction.domain.model.BatteryIssuance
import com.example.qrscannerapp.features.interaction.domain.model.BatteryReception
import com.example.qrscannerapp.features.interaction.domain.model.SbEmployee
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

enum class IssuanceStep { IDLE, CONFIRMING, DETAILS, SCANNING_RECEPTION, CONFIRMING_RECEPTION, DETAILS_RECEPTION }

@HiltViewModel
class InteractionViewModel @Inject constructor(
    private val repository: InteractionRepository,
    private val authManager: AuthManager
) : ViewModel() {

    val activeIssuances: StateFlow<List<BatteryIssuance>> = repository.getActiveIssuances()
        .map { list ->
            list.sortedWith(compareByDescending<BatteryIssuance> { it.isActive }.thenByDescending { it.timestamp })
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentReceptions: StateFlow<List<BatteryReception>> = repository.getRecentReceptions()
        .map { list -> list.sortedByDescending { it.timestamp } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _step = MutableStateFlow(IssuanceStep.IDLE)
    val step = _step.asStateFlow()

    private val _receptionScooterCodes = MutableStateFlow<List<String>>(emptyList())
    val receptionScooterCodes = _receptionScooterCodes.asStateFlow()

    private val _sbEmployees = MutableStateFlow<List<SbEmployee>>(emptyList())
    val sbEmployees = _sbEmployees.asStateFlow()

    private val _selectedIssuance = MutableStateFlow<BatteryIssuance?>(null)
    val selectedIssuance = _selectedIssuance.asStateFlow()

    private val _selectedReception = MutableStateFlow<BatteryReception?>(null)
    val selectedReception = _selectedReception.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _scanEventChannel = Channel<ScanEvent>()
    val scanEventFlow = _scanEventChannel.receiveAsFlow()

    fun startIssuance() {
        _step.value = IssuanceStep.CONFIRMING
        loadSbEmployees()
    }

    private fun loadSbEmployees() {
        viewModelScope.launch {
            _sbEmployees.value = repository.getSbEmployees()
        }
    }

    fun cancelIssuance() { _step.value = IssuanceStep.IDLE }

    fun startReceptionScanning() {
        _receptionScooterCodes.value = emptyList()
        _step.value = IssuanceStep.SCANNING_RECEPTION
        loadSbEmployees()
    }

    fun onReceptionCodeScanned(rawCode: String) {
        viewModelScope.launch {
            val scooterCode = parseCode(rawCode)
            if (!_receptionScooterCodes.value.contains(scooterCode)) {
                _receptionScooterCodes.value = listOf(scooterCode) + _receptionScooterCodes.value
                _scanEventChannel.send(ScanEvent.Success)
            } else {
                _scanEventChannel.send(ScanEvent.Duplicate)
            }
        }
    }

    fun removeReceptionScooterCode(code: String) { _receptionScooterCodes.value = _receptionScooterCodes.value.filter { it != code } }

    fun finishReceptionScanning() { _step.value = IssuanceStep.CONFIRMING_RECEPTION }

    fun cancelReception() {
        _step.value = IssuanceStep.IDLE
        _receptionScooterCodes.value = emptyList()
    }

    fun confirmReception(
        employeeId: String, employeeName: String, batteryCount: Int, reanimatorCount: Int,
        comment: String, photoUri: android.net.Uri?, context: android.content.Context,
        closedIssuanceId: String? = null, expectedBatteryCount: Int = 0
    ) {
        val user = authManager.authState.value
        val receptionId = UUID.randomUUID().toString()
        viewModelScope.launch {
            _isSaving.value = true
            val photoUrl = if (photoUri != null) savePhotoLocally(receptionId, photoUri, context) else null
            val reception = BatteryReception(
                id = receptionId,
                batteryCount = batteryCount,
                scooterCodes = _receptionScooterCodes.value,
                reanimatorCount = reanimatorCount,
                photoUrl = photoUrl,
                comment = comment,
                receivedById = user.userId ?: "",
                receivedByName = user.userName ?: "Неизвестно",
                receivedFromId = employeeId,
                receivedFromName = employeeName,
                timestamp = System.currentTimeMillis(),
                closedIssuanceId = closedIssuanceId,
                expectedBatteryCount = expectedBatteryCount
            )
            val result = repository.saveReception(reception)
            if (result.isSuccess) {
                if (closedIssuanceId != null) repository.closeIssuance(closedIssuanceId)
                _receptionScooterCodes.value = emptyList()
                _step.value = IssuanceStep.IDLE
            } else {
                Log.e("InteractionVM", "saveReception failed", result.exceptionOrNull())
            }
            _isSaving.value = false
        }
    }

    fun confirmIssuance(employeeId: String, employeeName: String, batteryCount: Int, reanimatorCount: Int, comment: String, photoUri: Uri?, context: Context) {
        val user = authManager.authState.value
        val issuanceId = UUID.randomUUID().toString()
        viewModelScope.launch {
            _isSaving.value = true
            val photoPath = if (photoUri != null) savePhotoLocally(issuanceId, photoUri, context) else null
            val issuance = BatteryIssuance(
                id = issuanceId,
                batteryCount = batteryCount,
                reanimatorCount = reanimatorCount,
                photoUrl = photoPath,
                comment = comment,
                issuedById = user.userId ?: "",
                issuedByName = user.userName ?: "Неизвестно",
                issuedByRole = user.role.displayName,
                issuedToId = employeeId,
                issuedToName = employeeName,
                timestamp = System.currentTimeMillis(),
                isActive = true
            )
            val result = repository.saveIssuance(issuance)
            if (result.isSuccess) {
                _step.value = IssuanceStep.IDLE
            } else {
                Log.e("InteractionVM", "saveIssuance failed", result.exceptionOrNull())
            }
            _isSaving.value = false
        }
    }

    fun openDetails(issuance: BatteryIssuance) { _selectedIssuance.value = issuance; _step.value = IssuanceStep.DETAILS }
    fun closeDetails() { _selectedIssuance.value = null; _step.value = IssuanceStep.IDLE }

    fun openReceptionDetails(reception: BatteryReception) { _selectedReception.value = reception; _step.value = IssuanceStep.DETAILS_RECEPTION }
    fun closeReceptionDetails() { _selectedReception.value = null; _step.value = IssuanceStep.IDLE }

    fun deleteIssuance(id: String) {
        viewModelScope.launch {
            repository.deleteIssuance(id)
            _selectedIssuance.value = null
            _step.value = IssuanceStep.IDLE
        }
    }

    fun deleteReception(id: String) {
        viewModelScope.launch {
            repository.deleteReception(id)
            _selectedReception.value = null
            _step.value = IssuanceStep.IDLE
        }
    }

    private suspend fun savePhotoLocally(id: String, uri: Uri, context: Context): String? {
        return try {
            val dir = File(context.filesDir, "photos").also { it.mkdirs() }
            val file = File(dir, "$id.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("InteractionVM", "savePhotoLocally", e)
            null
        }
    }

    private fun parseCode(raw: String): String {
        if (raw.contains("number=")) {
            raw.substringAfter("number=").split('&', '?', '#').firstOrNull()?.let { return it }
        }
        if (raw.contains('/')) {
            raw.split('/').lastOrNull { it.isNotEmpty() && it.all { c -> c.isDigit() } }?.let { return it }
        }
        return raw
    }
}