// File: features/scanner/ui/QrScannerViewModel.kt

package com.example.qrscannerapp

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.core.model.ActiveTab
import com.example.qrscannerapp.core.model.ScanEvent
import com.example.qrscannerapp.core.model.SessionType
import com.example.qrscannerapp.core.model.UiEffect

// --- ИМПОРТЫ ИЗ INVENTORY ---
import com.example.qrscannerapp.features.inventory.data.repository.StorageRepository
import com.example.qrscannerapp.features.inventory.domain.model.*
import com.example.qrscannerapp.features.inventory.ui.*
// -----------------------------

import com.example.qrscannerapp.features.scanner.data.local.entity.ScanSessionEntity
import com.example.qrscannerapp.features.scanner.data.repository.ScanSessionRepository
import com.example.qrscannerapp.features.scanner.domain.model.ScanItem
import com.example.qrscannerapp.features.scanner.domain.model.ScanSession
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "scanned_codes_store")

data class ActivityLogMetrics(
    val itemCount: Int,
    val manualEntryCount: Int,
    val durationSeconds: Long
)

// --- НОВЫЙ КЛАСС ДЛЯ ОТЧЕТА ---
data class DistributionReport(
    val successCount: Int,
    val errorCount: Int,
    val duplicateCount: Int,
    val excludedItems: List<ExcludedItem>, // Список исключенных с номерами
    val targetPalletNumber: Int,
    val targetManufacturer: String?
)

data class ExcludedItem(
    val indexInList: Int, // Номер по порядку (1, 2, 3...)
    val code: String,
    val reason: String // "Чужой производитель"
)
// ------------------------------

data class StorageUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val cells: List<StorageCell> = emptyList(),
    val activityLog: List<StorageActivityLogEntry> = emptyList(),
    val distributionResult: String? = null,
    val todayAddedCount: Int = 0
)

data class BatterySearchResult(
    val batteryCode: String,
    val palletNumber: Int,
    val palletId: String,
    val manufacturer: String,
    val creatorName: String? = null,
    val timestamp: Long? = null
)

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    private val application: Application,
    private val authManager: AuthManager,
    private val sessionRepository: ScanSessionRepository,
    private val storageRepository: StorageRepository
) : AndroidViewModel(application) {
    private val firestore = Firebase.firestore
    private var storageActivityListener: ListenerRegistration? = null

    private val _scooterCodes = mutableStateListOf<ScanItem>()
    val scooterCodes: List<ScanItem> = _scooterCodes
    private val _batteryCodes = mutableStateListOf<ScanItem>()
    val batteryCodes: List<ScanItem> = _batteryCodes

    private val _scanSessions = MutableStateFlow<List<ScanSession>>(emptyList())
    val scanSessions: StateFlow<List<ScanSession>> = _scanSessions.asStateFlow()
    private var historyListener: ListenerRegistration? = null

    private val _newItems = MutableStateFlow<Set<String>>(emptySet())
    val newItems: StateFlow<Set<String>> = _newItems.asStateFlow()

    private val _activeTab = MutableStateFlow(ActiveTab.SCOOTERS)
    val activeTab = _activeTab.asStateFlow()
    private val _statusMessage = MutableStateFlow("Наведите камеру на QR-код")
    val statusMessage = _statusMessage.asStateFlow()
    private val _scanEffectChannel = Channel<UiEffect>()
    val scanEffect = _scanEffectChannel.receiveAsFlow()
    private val _scanEventChannel = Channel<ScanEvent>()
    val scanEvent = _scanEventChannel.receiveAsFlow()
    private var _sessionStartTimeMillis: Long = 0L
    private var _manualEntryCount: Int = 0

    private val _palletDistributionState = MutableStateFlow(PalletDistributionUiState())
    val palletDistributionState = _palletDistributionState.asStateFlow()

    // --- StateFlow для Диалога Отчета ---
    private val _distributionReport = MutableStateFlow<DistributionReport?>(null)
    val distributionReport = _distributionReport.asStateFlow()
    // ------------------------------------

    private val _todayAddedCount = MutableStateFlow(0)
    val todayAddedCount = _todayAddedCount.asStateFlow()

    // --- АНАЛИЗ ПАЛЕТ (QUALITY CONTROL) ---
    private val _palletAnalysisResults = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val palletAnalysisResults = _palletAnalysisResults.asStateFlow()

    private val _showAnalysisReport = MutableStateFlow(false)
    val showAnalysisReport = _showAnalysisReport.asStateFlow()

    fun dismissAnalysisReport() {
        _showAnalysisReport.value = false
    }

    fun dismissDistributionReport() {
        _distributionReport.value = null
    }
    // --------------------------------------

    private val _storageState = MutableStateFlow(StorageUiState())
    val storageState = _storageState.asStateFlow()

    private val _isSavingSession = MutableStateFlow(false)
    val isSavingSession: StateFlow<Boolean> = _isSavingSession.asStateFlow()

    private val _recentlySavedSession = MutableStateFlow<ScanSession?>(null)
    val recentlySavedSession: StateFlow<ScanSession?> = _recentlySavedSession.asStateFlow()

    private val _isSearchMode = MutableStateFlow(false)
    val isSearchMode: StateFlow<Boolean> = _isSearchMode.asStateFlow()

    private val _searchResult = MutableStateFlow<BatterySearchResult?>(null)
    val searchResult: StateFlow<BatterySearchResult?> = _searchResult.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // ==========================================
    // V-- НОВЫЕ ПЕРЕМЕННЫЕ ДЛЯ ПОИСКА САМОКАТОВ --V
    private val _isSearchingForScooter = MutableStateFlow(false)
    val isSearchingForScooter: StateFlow<Boolean> = _isSearchingForScooter.asStateFlow()

    private val _scooterSearchResult = MutableStateFlow<Pair<String, String>?>(null)
    val scooterSearchResult: StateFlow<Pair<String, String>?> = _scooterSearchResult.asStateFlow()
    // ==========================================

    private val _highlightedPalletId = MutableStateFlow<String?>(null)
    val highlightedPalletId = _highlightedPalletId.asStateFlow()

    private var lastScannedValue: String = ""
    private var lastProcessedTime: Long = 0L
    private val DEBOUNCE_DELAY = 1500L

    init {
        listenForSharedHistory()
        listenForLocalInventoryChanges()
    }

    // --- ЛОГИКА АНАЛИЗА ПАЛЕТ ---
    fun runPalletAnalysis() {
        viewModelScope.launch {
            _palletDistributionState.update { it.copy(isLoading = true) }
            delay(500)

            val currentPallets = _palletDistributionState.value.pallets
            val results = mutableMapOf<String, List<String>>()
            var totalErrors = 0

            currentPallets.forEach { pallet ->
                val manufacturer = pallet.manufacturer?.uppercase()
                if (manufacturer != null) {
                    val errorItems = pallet.items.filter { itemCode ->
                        isBatteryAlienToManufacturer(itemCode, manufacturer)
                    }
                    if (errorItems.isNotEmpty()) {
                        results[pallet.id] = errorItems
                        totalErrors += errorItems.size
                    }
                }
            }

            _palletAnalysisResults.value = results

            if (totalErrors > 0) {
                _showAnalysisReport.value = true
                _palletDistributionState.update { it.copy(isLoading = false) }
            } else {
                _palletDistributionState.update {
                    it.copy(
                        isLoading = false,
                        distributionResult = "Анализ завершен. Ошибок не найдено."
                    )
                }
            }
        }
    }

    private fun isBatteryAlienToManufacturer(code: String, manufacturer: String): Boolean {
        val cleanCode = code.uppercase().trim()
        val cleanManuf = manufacturer.uppercase().trim()
        return when (cleanManuf) {
            "FUJIAN" -> cleanCode.startsWith("4BZ")
            "BYD" -> cleanCode.startsWith("4BB")
            else -> false
        }
    }
    // ----------------------------

    fun setHighlightedPallet(palletId: String) {
        _highlightedPalletId.value = palletId
        viewModelScope.launch {
            delay(5000)
            if (_highlightedPalletId.value == palletId) {
                _highlightedPalletId.value = null
            }
        }
    }

    fun toggleSearchMode() {
        _isSearchMode.update { !it }
        if (!_isSearchMode.value) {
            _searchResult.value = null
            // ДОБАВЛЕНО: очистка поиска самокатов
            clearScooterSearchResult()
        }
        val modeMsg = if (_isSearchMode.value) "РЕЖИМ ПОИСКА" else "Наведите камеру на QR-код"
        updateStatus(modeMsg)
    }

    fun clearSearchResult() {
        _searchResult.value = null
    }

    // ДОБАВЛЕНО: Метод очистки поиска самокатов
    fun clearScooterSearchResult() {
        _scooterSearchResult.value = null
    }

    private fun searchBatteryInFirestore(code: String) {
        if (_isSearching.value) return
        _isSearching.value = true
        updateStatus("Поиск АКБ в базе...", isError = false)

        viewModelScope.launch {
            try {
                val querySnapshot = firestore.collection("storage_pallets")
                    .whereArrayContains("items", code)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents.first()
                    val pallet = doc.toObject(StoragePallet::class.java)

                    if (pallet != null) {
                        val manufacturer = when {
                            code.startsWith("4BB") -> "FUJIAN"
                            code.startsWith("4BZ") -> "BYD"
                            else -> "Неизвестно"
                        }
                        val creator = "Склад"

                        _searchResult.value = BatterySearchResult(
                            batteryCode = code,
                            palletNumber = pallet.palletNumber,
                            palletId = pallet.id,
                            manufacturer = manufacturer,
                            creatorName = creator,
                            timestamp = System.currentTimeMillis()
                        )
                        _scanEffectChannel.send(UiEffect.ScanSuccess)
                    }
                } else {
                    updateStatus("АКБ не найден на палетах", isError = true)
                    _scanEventChannel.send(ScanEvent.Duplicate)
                }
            } catch (e: Exception) {
                Log.e("Search", "Error searching battery", e)
                updateStatus("Ошибка поиска: ${e.message}", isError = true)
            } finally {
                _isSearching.value = false
            }
        }
    }

    // ==========================================
    // V-- НОВАЯ ФУНКЦИЯ ПОИСКА САМОКАТА --V
    private fun searchForScooter(code: String) {
        if (_isSearchingForScooter.value) return
        _isSearchingForScooter.value = true
        updateStatus("Поиск самоката на складе...", isError = false)
        viewModelScope.launch {
            // Предполагается, что метод findScooterInCell есть в StorageRepository (как обсуждали ранее)
            // Если его там нет, его нужно добавить в интерфейс репозитория
            val result = storageRepository.findScooterInCell(code)
            if (result != null) {
                _scooterSearchResult.value = result
                _scanEffectChannel.send(UiEffect.ScanSuccess)
            } else {
                updateStatus("Самокат не найден на хранении", isError = true)
                _scanEventChannel.send(ScanEvent.Duplicate)
                Toast.makeText(application, "Самокат $code не найден на хранении", Toast.LENGTH_SHORT).show()
            }
            _isSearchingForScooter.value = false
        }
    }
    // ==========================================

    private fun listenForLocalInventoryChanges() {
        storageRepository.getCellsFlow()
            .onEach { cells ->
                _storageState.update { it.copy(isLoading = false, cells = cells) }
            }
            .launchIn(viewModelScope)

        storageRepository.getPalletsFlow()
            .onEach { pallets ->
                _palletDistributionState.update { it.copy(isLoading = false, pallets = pallets) }
            }
            .launchIn(viewModelScope)
    }

    fun bulkAddScootersToCell(cellId: String, text: String) {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true) }
            storageRepository.bulkAddScootersToCell(cellId, text)
                .onSuccess {
                    _storageState.update { it.copy(
                        isLoading = false,
                        distributionResult = "Номера добавлены. Синхронизация с сервером начнется в фоне."
                    ) }
                }
                .onFailure { error ->
                    _storageState.update { it.copy(
                        isLoading = false,
                        error = "Ошибка добавления: ${error.message}"
                    ) }
                }
        }
    }

    fun setPalletManufacturer(palletId: String, manufacturer: String?) {
        viewModelScope.launch {
            storageRepository.setPalletManufacturer(palletId, manufacturer)
                .onFailure {
                    Log.e("ViewModel", "Failed to set manufacturer locally", it)
                }
        }
    }

    // --- ГЛАВНЫЙ МЕТОД СКАНИРОВАНИЯ (ИСПРАВЛЕН: ПАРСИНГ ПРИМЕНЯЕТСЯ К ПОИСКУ) ---
    fun onCodeScanned(rawCode: String) {
        val currentTime = System.currentTimeMillis()
        if (rawCode == lastScannedValue && (currentTime - lastProcessedTime) < DEBOUNCE_DELAY) {
            return
        }

        lastScannedValue = rawCode
        lastProcessedTime = currentTime

        viewModelScope.launch {
            // --- ПАРСИНГ КОДА (ВЫНЕСЕН НАВЕРХ ДЛЯ ПОИСКА) ---
            var extractedScooterCode: String? = null
            if (rawCode.contains("number=")) { try { extractedScooterCode = rawCode.substringAfter("number=").split('&', '?', '#').firstOrNull() } catch (e: Exception) { Log.e("VM_SCAN", "Parse error", e) } }
            if (extractedScooterCode == null && rawCode.contains('/')) { try { val segment = rawCode.split('/').lastOrNull { it.isNotEmpty() }; if (segment?.all { it.isDigit() } == true) { extractedScooterCode = segment } } catch (e: Exception) { Log.e("VM_SCAN", "Parse error", e) } }
            if (extractedScooterCode == null && rawCode.all { it.isDigit() }) { extractedScooterCode = rawCode }
            // ------------------------------------------------

            if (_isSearchMode.value) {
                // ИЗМЕНЕНО: Добавлена проверка вкладки для выбора типа поиска
                when (_activeTab.value) {
                    ActiveTab.SCOOTERS -> {
                        if (_isSearchingForScooter.value || _scooterSearchResult.value != null) return@launch
                        // ИСПОЛЬЗУЕМ ПАРСЕННЫЙ КОД (extractedScooterCode), ЕСЛИ ОН ЕСТЬ, ИНАЧЕ rawCode
                        searchForScooter(extractedScooterCode ?: rawCode)
                    }
                    ActiveTab.BATTERIES -> {
                        if (_searchResult.value != null) return@launch
                        if (_isSearching.value) return@launch
                        searchBatteryInFirestore(rawCode)
                    }
                    ActiveTab.WAREHOUSE -> { /* Логика для склада обрабатывается в StardustScreen, здесь ничего не делаем */ } // <-- ИЗМЕНЕНИЕ
                }
                return@launch
            }

            startSessionTimerIfNeeded()

            if (extractedScooterCode != null && extractedScooterCode.length < 10) {
                processScooterCode(extractedScooterCode)
            } else {
                processBatteryCode(rawCode)
            }
        }
    }

    private suspend fun processScooterCode(code: String) {
        if (_scooterCodes.any { it.code == code }) {
            updateStatus("Самокат $code уже в списке.", isError = true)
            _scanEventChannel.send(ScanEvent.Duplicate)
        } else {
            val newItem = ScanItem(code = code)
            _scooterCodes.add(0, newItem)
            _newItems.update { it + newItem.id }
            updateStatus("Самокат $code добавлен!")
            if (_activeTab.value == ActiveTab.BATTERIES) {
                _activeTab.value = ActiveTab.SCOOTERS
            }
            _scanEffectChannel.send(UiEffect.ScanSuccess)
            _scanEventChannel.send(ScanEvent.Success)
        }
    }

    private suspend fun processBatteryCode(code: String) {
        if (_batteryCodes.any { it.code == code }) {
            updateStatus("АКБ $code уже в списке.", isError = true)
            _scanEventChannel.send(ScanEvent.Duplicate)
        } else {
            val newItem = ScanItem(code = code)
            _batteryCodes.add(0, newItem)
            _newItems.update { it + newItem.id }
            updateStatus("АКБ $code добавлен!")
            if (_activeTab.value == ActiveTab.SCOOTERS) {
                _activeTab.value = ActiveTab.BATTERIES
            }
            _scanEffectChannel.send(UiEffect.ScanSuccess)
            _scanEventChannel.send(ScanEvent.Success)
        }
    }

    private fun listenForSharedHistory() {
        historyListener?.remove()

        historyListener = firestore.collection("scan_sessions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("ViewModel", "History listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val sessionsList = snapshot.toObjects(ScanSession::class.java)
                    _scanSessions.value = sessionsList
                }
            }
    }

    fun saveCurrentSession(name: String?) {
        if (_isSavingSession.value) return

        val currentUser = authManager.authState.value
        if (currentUser.userId == null) {
            updateStatus("Ошибка: Ты не залогинен, чувак", isError = true)
            return
        }

        viewModelScope.launch {
            _isSavingSession.value = true

            val (listToSave, sessionType) = when (_activeTab.value) {
                ActiveTab.SCOOTERS -> _scooterCodes.toList() to SessionType.SCOOTERS
                ActiveTab.BATTERIES -> _batteryCodes.toList() to SessionType.BATTERIES
                ActiveTab.WAREHOUSE -> emptyList<ScanItem>() to null // <-- ИЗМЕНЕНИЕ
            }

            if (listToSave.isEmpty() || sessionType == null) { // <-- ИЗМЕНЕНИЕ
                updateStatus("Сохранять нечего, список пустой.", isError = true)
                _isSavingSession.value = false
                return@launch
            }

            delay(1500)

            val sessionToUpload = ScanSession(
                id = UUID.randomUUID().toString(),
                name = name?.takeIf { it.isNotBlank() },
                type = sessionType,
                items = listToSave,
                timestamp = System.currentTimeMillis(),
                creatorId = currentUser.userId,
                creatorName = currentUser.userName ?: "Анонимус"
            )

            try {
                firestore.collection("scan_sessions").document(sessionToUpload.id).set(sessionToUpload).await()
                Log.d("ViewModel", "Сессия ${sessionToUpload.id} успешно улетела в облако.")
            } catch (e: Exception) {
                Log.w("ViewModel", "Пиздец, в облако не улетело. Сохраняем локально.", e)
                val newLocalSession = ScanSessionEntity(
                    id = sessionToUpload.id,
                    name = sessionToUpload.name,
                    type = sessionToUpload.type,
                    items = sessionToUpload.items,
                    timestamp = sessionToUpload.timestamp,
                    creatorId = sessionToUpload.creatorId,
                    creatorName = sessionToUpload.creatorName
                )
                sessionRepository.saveSessionLocally(newLocalSession)
            } finally {
                logActivity("SESSION_SAVED")
                _scanEffectChannel.send(UiEffect.SessionSaved)
                _recentlySavedSession.value = sessionToUpload
                _isSavingSession.value = false
            }
        }
    }

    fun onSessionSaveDialogDismissed() {
        clearList()
        _recentlySavedSession.value = null
    }


    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("scan_sessions").document(sessionId).delete().await()
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to delete session $sessionId from Firestore", e)
                updateStatus("Ошибка удаления сессии из облака", isError = true)
            }
        }
    }

    fun deleteItemFromSession(sessionId: String, itemToDelete: ScanItem) {
        viewModelScope.launch {
            val sessionRef = firestore.collection("scan_sessions").document(sessionId)
            try {
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(sessionRef)
                    val session = snapshot.toObject(ScanSession::class.java)
                        ?: throw Exception("Сессия не найдена")

                    val updatedItems = session.items.filter { it.id != itemToDelete.id }

                    if (updatedItems.isEmpty()) {
                        transaction.delete(sessionRef)
                    } else {
                        transaction.update(sessionRef, "items", updatedItems)
                    }
                }.await()
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to delete item from session $sessionId", e)
                updateStatus("Ошибка удаления элемента", isError = true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        historyListener?.remove()
        storageActivityListener?.remove()
        storageRepository.stopPalletsRealtimeSync()
    }

    fun addManualCode(code: String) {
        if (_isSearchMode.value) {
            when(_activeTab.value) {
                ActiveTab.SCOOTERS -> searchForScooter(code)
                ActiveTab.BATTERIES -> searchBatteryInFirestore(code)
                ActiveTab.WAREHOUSE -> { /* Аналогично onCodeScanned, обработка вне этого ViewModel */ } // <-- ИЗМЕНЕНИЕ
            }
            return
        }

        startSessionTimerIfNeeded()
        _manualEntryCount++
        Log.d("Metrics", "Manual entry count is now $_manualEntryCount")

        when (_activeTab.value) {
            ActiveTab.SCOOTERS -> {
                if (code.isBlank() || !code.all { it.isDigit() }) {
                    updateStatus("Ошибка: Код самоката должен состоять только из цифр.", isError = true)
                    return
                }
                if (_scooterCodes.any { it.code == code }) {
                    updateStatus("Самокат $code уже в списке.", isError = true)
                    return
                }
                val newItem = ScanItem(code = code)
                _scooterCodes.add(0, newItem)
                _newItems.update { it + newItem.id }
                updateStatus("Самокат $code успешно добавлен!")
            }
            ActiveTab.BATTERIES -> {
                if (code.isBlank()) {
                    updateStatus("Ошибка: Код АКБ не может быть пустым.", isError = true)
                    return
                }
                if (_batteryCodes.any { it.code == code }) {
                    updateStatus("АКБ $code уже в списке.", isError = true)
                    return
                }
                val newItem = ScanItem(code = code)
                _batteryCodes.add(0, newItem)
                _newItems.update { it + newItem.id }
                updateStatus("АКБ $code успешно добавлен!")
            }
            ActiveTab.WAREHOUSE -> { /* Список склада в WarehouseViewModel */ } // <-- ИЗМЕНЕНИЕ
        }
    }

    fun markAsOld(item: ScanItem) {
        _newItems.update { it - item.id }
    }

    fun clearList() {
        _sessionStartTimeMillis = 0L
        _manualEntryCount = 0
        _newItems.update { emptySet() }
        Log.d("Metrics", "Metrics reset.")
        when (_activeTab.value) {
            ActiveTab.SCOOTERS -> _scooterCodes.clear()
            ActiveTab.BATTERIES -> _batteryCodes.clear()
            ActiveTab.WAREHOUSE -> { /* Список склада в WarehouseViewModel */ } // <-- ИЗМЕНЕНИЕ
        }
        updateStatus("Список очищен")
    }

    fun removeCode(item: ScanItem) {
        _scooterCodes.remove(item)
        _batteryCodes.remove(item)
    }

    fun sortCurrentList() {
        when (_activeTab.value) {
            ActiveTab.SCOOTERS -> {
                _scooterCodes.sortBy { it.code.toLongOrNull() ?: Long.MAX_VALUE }
            }
            ActiveTab.BATTERIES -> {
                _batteryCodes.sortBy { it.code }
            }
            ActiveTab.WAREHOUSE -> { /* Список склада в WarehouseViewModel */ } // <-- ИЗМЕНЕНИЕ
        }
    }

    private fun startSessionTimerIfNeeded() {
        if (_scooterCodes.isEmpty() && _batteryCodes.isEmpty()) {
            _sessionStartTimeMillis = System.currentTimeMillis()
            _manualEntryCount = 0
        }
    }

    fun onTabSelected(tab: ActiveTab) { _activeTab.value = tab }

    fun updateStatus(message: String, isError: Boolean = false) {
        viewModelScope.launch {
            _statusMessage.value = message
            delay(if(isError) 2000 else 1500)
            val defaultMessage = if (_isSearchMode.value) "РЕЖИМ ПОИСКА" else "Наведите камеру на QR-код"
            if (_statusMessage.value == message) {
                _statusMessage.value = defaultMessage
            }
        }
    }


    fun getCurrentMetrics(): ActivityLogMetrics {
        val currentList = when (_activeTab.value) {
            ActiveTab.SCOOTERS -> scooterCodes
            ActiveTab.BATTERIES -> batteryCodes
            ActiveTab.WAREHOUSE -> return ActivityLogMetrics(0, 0, 0L) // <-- ИЗМЕНЕНИЕ
        }
        val itemCount = currentList.size
        val durationSeconds = if (_sessionStartTimeMillis != 0L && itemCount > 0) {
            (System.currentTimeMillis() - _sessionStartTimeMillis) / 1000
        } else {
            0L
        }
        return ActivityLogMetrics(itemCount, _manualEntryCount, durationSeconds)
    }

    fun logActivity(activityType: String) {
        val currentUserId = authManager.currentUserId
        val currentUserName = authManager.authState.value.userName
        if (currentUserId == null) return

        viewModelScope.launch {
            try {
                val activityMetrics = getCurrentMetrics()
                val telemetryManager = TelemetryManager(getApplication())
                val deviceTelemetry = telemetryManager.getAllTelemetry()
                val networkPing = telemetryManager.getNetworkPing()

                val logData = mutableMapOf<String, Any?>(
                    "timestamp" to System.currentTimeMillis(),
                    "activityType" to activityType,
                    "creatorId" to currentUserId,
                    "creatorName" to (currentUserName ?: "Unknown"),
                    "itemCount" to activityMetrics.itemCount,
                    "manualEntryCount" to activityMetrics.manualEntryCount,
                    "durationSeconds" to activityMetrics.durationSeconds,
                    "networkPing" to networkPing
                )
                logData.putAll(deviceTelemetry)
                firestore.collection("activity_log").add(logData).await()
                Log.d("Metrics", "Activity '$activityType' logged successfully.")

            } catch (e: Exception) {
                Log.e("Metrics", "Failed to log activity '$activityType'", e)
            }
        }
    }

    private fun <T> List<T>.chunkedSafe(size: Int): List<List<T>> {
        return this.chunked(size).filter { it.isNotEmpty() }
    }

    private fun startPalletActivityLogListener() {
        firestore.collection("pallet_activity_log").orderBy("timestamp", Query.Direction.DESCENDING).limit(100)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { Log.e("PalletLog", "Listen failed.", e); return@addSnapshotListener }
                if (snapshot != null) {
                    val logEntries = snapshot.documents.mapNotNull { it.toObject(
                        PalletActivityLogEntry::class.java) }

                    _palletDistributionState.update { it.copy(activityLog = logEntries) }

                    // --- ПЕРЕСЧЕТ "СЕГОДНЯ" ---
                    calculateTodayAddedCount(logEntries)
                }
            }
    }

    private fun calculateTodayAddedCount(logEntries: List<PalletActivityLogEntry>) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        val todayCount = logEntries
            .filter { it.timestamp >= startOfDay && it.action == "DISTRIBUTED" }
            .sumOf { it.itemCount ?: 0 }

        _todayAddedCount.value = todayCount
    }

    private fun logPalletActivity(action: String, pallet: StoragePallet? = null, itemCount: Int = 0) {
        val currentUserId = authManager.currentUserId; val currentUserName = authManager.authState.value.userName
        if (currentUserId == null) { return }
        val logEntry = PalletActivityLogEntry(
            userId = currentUserId,
            userName = currentUserName,
            action = action,
            palletNumber = pallet?.palletNumber,
            itemCount = itemCount,
            palletId = pallet?.id
        )
        viewModelScope.launch { try { firestore.collection("pallet_activity_log").add(logEntry).await() } catch (e: Exception) { Log.e("PalletLog", "Failed to write activity log", e) } }
    }

    fun clearPalletActivityLog() {
        viewModelScope.launch {
            if (!authManager.authState.value.isAdmin) {
                _palletDistributionState.update { it.copy(error = "У вас нет прав для выполнения этого действия.") }
                return@launch
            }

            _palletDistributionState.update { it.copy(isDistributing = true) }
            try {
                val snapshot = firestore.collection("pallet_activity_log").get().await()
                val batch = firestore.batch()
                for (document in snapshot.documents) {
                    batch.delete(document.reference)
                }
                batch.commit().await()
                Log.d("PalletLog", "Pallet activity log cleared by admin.")
                _palletDistributionState.update { it.copy(isDistributing = false, distributionResult = "История операций успешно очищена.") }
            } catch (e: Exception) {
                Log.e("PalletLog", "Failed to clear pallet activity log", e)
                _palletDistributionState.update { it.copy(isDistributing = false, error = "Не удалось очистить историю: ${e.message}") }
            }
        }
    }

    fun onNavigateToPalletDistribution() {
        _palletAnalysisResults.value = emptyMap()
        _showAnalysisReport.value = false
        _palletDistributionState.update { it.copy(undistributedItemCount = _batteryCodes.size) }

        storageRepository.startPalletsRealtimeSync()
        loadPallets()
        startPalletActivityLogListener()
    }

    private fun loadPallets() {
        viewModelScope.launch {
            _palletDistributionState.update { it.copy(isLoading = true, error = null) }
            storageRepository.refreshDataFromServer()
        }
    }

    fun loadStorageCells() {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true, error = null) }
            startStorageActivityLogListener()
            storageRepository.refreshDataFromServer()
        }
    }

    fun createNewPallet() {
        viewModelScope.launch {
            _palletDistributionState.update { it.copy(isLoading = true) }
            try {
                val newPalletNumber = (_palletDistributionState.value.pallets.maxOfOrNull { it.palletNumber } ?: 0) + 1
                val newPallet = StoragePallet(palletNumber = newPalletNumber)
                firestore.collection("storage_pallets").document(newPallet.id).set(newPallet).await()
                logPalletActivity(action = "CREATED", pallet = newPallet)
            } catch (e: Exception) {
                _palletDistributionState.update { it.copy(isLoading = false, error = "Не удалось создать палет") }
            }
        }
    }

    // --- ОБНОВЛЕННЫЙ УМНЫЙ МЕТОД: Формирует отчет и диалог ---
    fun distributeBatteriesToPallet(pallet: StoragePallet) {
        val allItems = _batteryCodes.toList()
        if (allItems.isEmpty()) return

        val manufacturer = pallet.manufacturer?.uppercase()
        val validItems = mutableListOf<ScanItem>()
        val excludedItemsList = mutableListOf<ExcludedItem>()

        // Индекс 0 - это последний отсканированный (верхний).
        // Пользователю удобнее считать с 1 (верхнего).
        allItems.forEachIndexed { index, item ->
            val displayIndex = index + 1 // 1, 2, 3...
            if (manufacturer != null && isBatteryAlienToManufacturer(item.code, manufacturer)) {
                excludedItemsList.add(ExcludedItem(displayIndex, item.code, "Чужой производитель"))
            } else {
                validItems.add(item)
            }
        }

        // Если есть ошибки, или если всё ок - мы в любом случае используем общую функцию записи
        // Но если ВСЕ ошибки - нет смысла писать в базу, сразу отчет
        if (validItems.isEmpty() && excludedItemsList.isNotEmpty()) {
            _distributionReport.value = DistributionReport(
                successCount = 0,
                errorCount = excludedItemsList.size,
                duplicateCount = 0,
                excludedItems = excludedItemsList,
                targetPalletNumber = pallet.palletNumber,
                targetManufacturer = manufacturer
            )
            return
        }

        executeBatchDistribution(pallet, validItems, excludedItemsList)
    }

    private fun executeBatchDistribution(
        pallet: StoragePallet,
        itemsToDistribute: List<ScanItem>,
        excludedItems: List<ExcludedItem>
    ) {
        viewModelScope.launch {
            _palletDistributionState.update { it.copy(isDistributing = true, distributionResult = null) }

            val batteryCodesList = itemsToDistribute.map { it.code }
            val batteriesToAdd = mutableListOf<String>()
            var duplicateCount = 0
            var successfulCount = 0

            try {
                // Проверка дубликатов (уже на складе)
                val allBatteryDocs = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()
                val chunks = batteryCodesList.chunkedSafe(10)
                for (chunk in chunks) {
                    val snapshots = firestore.collection("batteries").whereIn(FieldPath.documentId(), chunk).get().await()
                    snapshots.documents.forEach { doc -> doc.id?.let { id -> allBatteryDocs[id] = doc } }
                }

                val batch = firestore.batch()
                val palletRef = firestore.collection("storage_pallets").document(pallet.id)

                for (item in itemsToDistribute) {
                    val batteryId = item.code
                    val batteryRef = firestore.collection("batteries").document(batteryId)
                    val snapshot = allBatteryDocs[batteryId]

                    if (snapshot != null) {
                        // Если уже на складе - считаем дубликатом, не обновляем статус
                        if (snapshot.getString("status") == "on_storage") {
                            duplicateCount++
                        } else {
                            // Если был в другом месте - забираем
                            batch.update(batteryRef, "status", "on_storage", "palletId", pallet.id)
                            batteriesToAdd.add(batteryId); successfulCount++
                        }
                    } else {
                        val newBatteryData = mapOf("id" to batteryId, "status" to "on_storage", "palletId" to pallet.id, "createdAt" to FieldValue.serverTimestamp())
                        batch.set(batteryRef, newBatteryData); batteriesToAdd.add(batteryId); successfulCount++
                    }
                }

                if (batteriesToAdd.isNotEmpty()) {
                    batch.update(palletRef, "items", FieldValue.arrayUnion(*batteriesToAdd.toTypedArray()))
                }

                batch.commit().await()

                // Удаляем успешно добавленные из списка на телефоне
                val successCodes = batteriesToAdd.toSet()
                _batteryCodes.removeAll { item -> successCodes.contains(item.code) }

                if (successfulCount > 0) {
                    logPalletActivity(action = "DISTRIBUTED", pallet = pallet, itemCount = successfulCount)
                }

                // ПОКАЗЫВАЕМ ОТЧЕТ (ДИАЛОГ)
                _distributionReport.value = DistributionReport(
                    successCount = successfulCount,
                    errorCount = excludedItems.size,
                    duplicateCount = duplicateCount,
                    excludedItems = excludedItems,
                    targetPalletNumber = pallet.palletNumber,
                    targetManufacturer = pallet.manufacturer
                )

                _palletDistributionState.update {
                    it.copy(
                        isDistributing = false,
                        undistributedItemCount = _batteryCodes.size
                        // distributionResult больше не используется для Snackbara
                    )
                }

            } catch (e: Exception) {
                _palletDistributionState.update { it.copy(isDistributing = false, error = "Ошибка записи: ${e.message}") }
            }
        }
    }
    // -------------------------------------------------------------

    // --- ФУНКЦИЯ ДЛЯ ОТМЕНЫ УДАЛЕНИЯ (UNDO) ---
    fun distributeSpecificItemToPallet(pallet: StoragePallet, batteryId: String) {
        viewModelScope.launch {
            _palletDistributionState.update { it.copy(isDistributing = true) }
            try {
                val palletRef = firestore.collection("storage_pallets").document(pallet.id)
                val batteryRef = firestore.collection("batteries").document(batteryId)

                firestore.runTransaction { transaction ->
                    transaction.set(batteryRef, mapOf(
                        "id" to batteryId,
                        "status" to "on_storage",
                        "palletId" to pallet.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    ))
                    transaction.update(palletRef, "items", FieldValue.arrayUnion(batteryId))
                }.await()

                _batteryCodes.removeAll { it.code == batteryId }

                logPalletActivity(action = "RESTORED_ITEM", pallet = pallet, itemCount = 1)
                _palletDistributionState.update { it.copy(isDistributing = false, distributionResult = "АКБ $batteryId восстановлен.") }

            } catch (e: Exception) {
                _palletDistributionState.update { it.copy(isDistributing = false, error = "Не удалось восстановить АКБ: ${e.message}") }
            }
        }
    }

    fun removeItemFromPallet(palletId: String, batteryId: String) {
        viewModelScope.launch {
            _palletDistributionState.update { it.copy(isDistributing = true, distributionResult = null) }
            val pallet = _palletDistributionState.value.pallets.find { it.id == palletId }
            try {
                val palletRef = firestore.collection("storage_pallets").document(palletId)
                val batteryRef = firestore.collection("batteries").document(batteryId)
                firestore.runTransaction { transaction ->
                    transaction.update(palletRef, "items", FieldValue.arrayRemove(batteryId))
                    transaction.update(batteryRef, "status", FieldValue.delete(), "palletId", FieldValue.delete())
                }.await()
                if (pallet != null) { logPalletActivity(action = "REMOVED_ITEM", pallet = pallet, itemCount = 1) }
                _palletDistributionState.update { it.copy(isDistributing = false, distributionResult = "АКБ $batteryId удален с палета и его статус сброшен.") }
            } catch (e: Exception) {
                _palletDistributionState.update { it.copy(isDistributing = false, error = "Не удалось удалить АКБ: ${e.message}") }
            }
        }
    }

    fun deletePallet(pallet: StoragePallet) {
        viewModelScope.launch {
            _palletDistributionState.update { it.copy(isDistributing = true, distributionResult = null) }
            try {
                storageRepository.deletePallet(pallet)
                    .onSuccess {
                        logPalletActivity(action = "DELETED", pallet = pallet, itemCount = pallet.items.size)
                        _palletDistributionState.update {
                            it.copy(
                                isDistributing = false,
                                distributionResult = "Палет №${pallet.palletNumber} удален. Статусы АКБ сброшены."
                            )
                        }
                    }
                    .onFailure { e ->
                        _palletDistributionState.update { it.copy(isDistributing = false, error = "Ошибка удаления палета: ${e.message}") }
                    }
            } catch (e: Exception) {
                _palletDistributionState.update { it.copy(isDistributing = false, error = "Ошибка: ${e.message}") }
            }
        }
    }

    fun getPalletsDataForMasterExport(): Map<String, List<String>> {
        return _palletDistributionState.value.pallets.associate { "Палет №${it.palletNumber}" to it.items.toList() }
    }

    fun clearDistributionResult() {
        _palletDistributionState.update { it.copy(distributionResult = null) }
    }

    fun createNewCell(description: String, capacity: Int) {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true) }
            storageRepository.createNewCell(description, capacity)
                .onSuccess { }
                .onFailure { error ->
                    _storageState.update { it.copy(isLoading = false, error = "Не удалось создать ячейку: ${error.message}") }
                }
        }
    }

    fun distributeScootersToCell(cell: StorageCell) {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true) }
            val scooterIds = scooterCodes.map { it.code }

            storageRepository.distributeScootersToCell(cell, scooterIds)
                .onSuccess { addedCount ->
                    val totalCount = scooterIds.size
                    val duplicateCount = totalCount - addedCount
                    var resultMessage = "Успешно добавлено: $addedCount."
                    if (duplicateCount > 0) {
                        resultMessage += " Пропущено дубликатов: $duplicateCount."
                    }
                    _storageState.update { it.copy(isLoading = false, distributionResult = resultMessage) }
                    _scooterCodes.clear()
                }
                .onFailure { error ->
                    _storageState.update { it.copy(isLoading = false, distributionResult = "Ошибка: ${error.message}") }
                }
        }
    }

    fun clearStorageDistributionResult() {
        _storageState.update { it.copy(distributionResult = null) }
    }

    fun updateCell(cellId: String, newDescription: String, newCapacity: Int) {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true) }
            storageRepository.updateCell(cellId, newDescription, newCapacity)
                .onSuccess { }
                .onFailure { error ->
                    _storageState.update { it.copy(isLoading = false, error = "Ошибка обновления: ${error.message}") }
                }
        }
    }

    fun removeItemFromCell(cell: StorageCell, scooterId: String) {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true) }
            storageRepository.removeItemFromCell(cell, scooterId)
                .onSuccess { }
                .onFailure { error ->
                    _storageState.update { it.copy(isLoading = false, error = "Ошибка удаления самоката: ${error.message}") }
                }
        }
    }

    fun deleteCell(cell: StorageCell) {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true) }
            storageRepository.deleteCell(cell)
                .onSuccess { }
                .onFailure { error ->
                    _storageState.update { it.copy(isLoading = false, error = "Ошибка удаления ячейки: ${error.message}") }
                }
        }
    }

    private fun startStorageActivityLogListener() {
        storageActivityListener?.remove()
        storageActivityListener = firestore.collection("storage_activity_log")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ViewModel", "Storage activity log listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val logEntries = snapshot.toObjects(StorageActivityLogEntry::class.java)
                    _storageState.update { it.copy(activityLog = logEntries) }
                }
            }
    }

    fun clearStorageActivityLog() {
        viewModelScope.launch {
            if (!authManager.authState.value.isAdmin) {
                _storageState.update { it.copy(distributionResult = "У вас нет прав для выполнения этого действия.") }
                return@launch
            }

            _storageState.update { it.copy(isLoading = true) }
            try {
                val snapshot = firestore.collection("storage_activity_log").get().await()

                if (snapshot.isEmpty) {
                    _storageState.update { it.copy(isLoading = false, distributionResult = "История операций уже пуста.") }
                    return@launch
                }

                val batch = firestore.batch()
                for (document in snapshot.documents) {
                    batch.delete(document.reference)
                }

                batch.commit().await()
                Log.d("StorageLog", "Storage activity log cleared by admin.")
                _storageState.update { it.copy(isLoading = false) }

            } catch (e: Exception) {
                Log.e("StorageLog", "Failed to clear storage activity log", e)
                _storageState.update {
                    it.copy(
                        isLoading = false,
                        distributionResult = "Не удалось очистить историю: ${e.message}"
                    )
                }
            }
        }
    }
}