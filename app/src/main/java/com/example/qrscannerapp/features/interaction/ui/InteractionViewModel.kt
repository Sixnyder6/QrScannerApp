package com.example.qrscannerapp.features.interaction.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.core.model.ScanEvent
import com.example.qrscannerapp.features.interaction.data.repository.InteractionRepository
import com.example.qrscannerapp.features.interaction.domain.model.InteractionSession
import com.example.qrscannerapp.features.interaction.domain.model.OperationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class InteractionViewModel @Inject constructor(
    private val repository: InteractionRepository,
    private val authManager: AuthManager
) : ViewModel() {

    // Текущая выбранная операция (если null - показываем меню выбора)
    private val _currentOperation = MutableStateFlow<OperationType?>(null)
    val currentOperation = _currentOperation.asStateFlow()

    // Список отсканированных кодов в текущей сессии
    private val _scannedCodes = MutableStateFlow<List<String>>(emptyList())
    val scannedCodes = _scannedCodes.asStateFlow()

    // Состояние загрузки (при сохранении)
    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    // Канал для анимаций рамки камеры (Успех/Дубликат)
    private val _scanEventChannel = Channel<ScanEvent>()
    val scanEventFlow = _scanEventChannel.receiveAsFlow()

    // История сессий (напрямую из БД, обновляется сама)
    val history: StateFlow<List<InteractionSession>> = repository.getSessionsHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- ДЕЙСТВИЯ (INTENTS) ---

    fun selectOperation(operation: OperationType) {
        _currentOperation.value = operation
        _scannedCodes.value = emptyList() // Очищаем список при новой сессии
    }

    fun cancelSession() {
        _currentOperation.value = null
        _scannedCodes.value = emptyList()
    }

    fun onCodeScanned(rawCode: String) {
        viewModelScope.launch {
            // ПАРСИНГ КОДА (чистим ссылки, достаем только номер)
            var extractedCode: String? = null
            if (rawCode.contains("number=")) { try { extractedCode = rawCode.substringAfter("number=").split('&', '?', '#').firstOrNull() } catch (e: Exception) { Log.e("VM_SCAN", "Parse error", e) } }
            if (extractedCode == null && rawCode.contains('/')) { try { val segment = rawCode.split('/').lastOrNull { it.isNotEmpty() }; if (segment?.all { it.isDigit() } == true) { extractedCode = segment } } catch (e: Exception) { Log.e("VM_SCAN", "Parse error", e) } }
            if (extractedCode == null && rawCode.all { it.isDigit() }) { extractedCode = rawCode }

            val finalCode = extractedCode ?: rawCode
            val currentList = _scannedCodes.value

            // Проверка на дубликат в ТЕКУЩЕЙ сессии
            if (!currentList.contains(finalCode)) {
                // Добавляем в начало списка, чтобы последний скан был сверху
                _scannedCodes.value = listOf(finalCode) + currentList
                _scanEventChannel.send(ScanEvent.Success) // Запускаем зеленую рамку и звук
            } else {
                _scanEventChannel.send(ScanEvent.Duplicate) // Запускаем красную рамку
            }
        }
    }

    fun removeCode(code: String) {
        _scannedCodes.value = _scannedCodes.value.filter { it != code }
    }

    fun finishAndSaveSession() {
        val operation = _currentOperation.value ?: return
        val codes = _scannedCodes.value
        if (codes.isEmpty()) return

        val user = authManager.authState.value

        viewModelScope.launch {
            _isSaving.value = true

            val session = InteractionSession(
                id = UUID.randomUUID().toString(),
                operationType = operation,
                creatorId = user.userId ?: "unknown_id",
                creatorName = user.userName ?: "Неизвестный сотрудник",
                timestamp = System.currentTimeMillis(),
                scooterCount = codes.size,
                scooterCodes = codes
            )

            // Сохраняем через репозиторий (он сам разберется с Room и Firebase)
            val result = repository.saveSession(session)

            if (result.isSuccess) {
                // Возвращаемся в главное меню взаимодействий
                _currentOperation.value = null
                _scannedCodes.value = emptyList()
            }
            // TODO: Можно добавить отправку UiEvent для показа Snackbar об ошибке/успехе

            _isSaving.value = false
        }
    }
}