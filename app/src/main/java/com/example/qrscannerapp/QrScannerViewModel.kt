// File: features/scanner/ui/QrScannerViewModel.kt

package com.example.qrscannerapp

import android.app.Application
import android.content.Context
import android.util.Log
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
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "scanned_codes_store")

data class ActivityLogMetrics(
    val itemCount: Int,
    val manualEntryCount: Int,
    val durationSeconds: Long
)

data class StorageUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val cells: List<StorageCell> = emptyList(),
    val activityLog: List<StorageActivityLogEntry> = emptyList(),
    val distributionResult: String? = null
)

data class BatterySearchResult(
    val batteryCode: String,
    val palletNumber: Int,
    val palletId: String,
    val manufacturer: String,
    val creatorName: String? = null,
    val timestamp: Long? = null
)

enum class ScanMode { QR, TEXT }

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    application: Application,
    private val authManager: AuthManager,
    private val sessionRepository: ScanSessionRepository,
    private val storageRepository: StorageRepository
) : AndroidViewModel(application) {
    private val firestore = Firebase.firestore
    private var storageActivityListener: ListenerRegistration? = null

    private val _localScanSessions = mutableStateListOf<ScanSession>()

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

    private val _storageState = MutableStateFlow(StorageUiState())
    val storageState = _storageState.asStateFlow()

    private val _scanMode = MutableStateFlow(ScanMode.QR)
    val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

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

    // --- ID палета для подсветки ---
    private val _highlightedPalletId = MutableStateFlow<String?>(null)
    val highlightedPalletId = _highlightedPalletId.asStateFlow()

    init {
        listenForSharedHistory()
        listenForLocalInventoryChanges()
    }

    // --- Установка подсветки ---
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
        }
        val modeMsg = if (_isSearchMode.value) "РЕЖИМ ПОИСКА" else if (_scanMode.value == ScanMode.QR) "Режим сканирования QR" else "Режим чтения текста"
        updateStatus(modeMsg)
    }

    fun clearSearchResult() {
        _searchResult.value = null
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

    fun toggleScanMode() {
        _scanMode.update {
            if (it == ScanMode.QR) ScanMode.TEXT else ScanMode.QR
        }
        val message = if (_scanMode.value == ScanMode.TEXT) "Режим чтения текста" else "Режим сканирования QR"
        updateStatus(message)
    }

    fun onTextFound(text: String) {
        viewModelScope.launch {
            if (_isSearchMode.value) {
                if (_searchResult.value != null) return@launch
                if (_isSearching.value) return@launch

                if (text.matches(Regex("^\\d[A-Z]{2}\\d{11}$"))) {
                    searchBatteryInFirestore(text)
                }
                return@launch
            }

            startSessionTimerIfNeeded()
            when {
                text.matches(Regex("^00\\d{6}$")) -> {
                    processScooterCode(text)
                }
                text.matches(Regex("^\\d[A-Z]{2}\\d{11}$")) -> {
                    processBatteryCode(text)
                }
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
            }

            if (listToSave.isEmpty()) {
                updateStatus("Сохранять нехуй, список пустой.", isError = true)
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
    }

    fun onCodeScanned(rawCode: String) {
        viewModelScope.launch {
            if (_isSearchMode.value) {
                if (_searchResult.value != null) return@launch
                if (_isSearching.value) return@launch

                searchBatteryInFirestore(rawCode)
                return@launch
            }

            startSessionTimerIfNeeded()
            var extractedScooterCode: String? = null
            if (rawCode.contains("number=")) { try { extractedScooterCode = rawCode.substringAfter("number=").split('&', '?', '#').firstOrNull() } catch (e: Exception) { Log.e("VM_SCAN", "Parse error", e) } }
            if (extractedScooterCode == null && rawCode.contains('/')) { try { val segment = rawCode.split('/').lastOrNull { it.isNotEmpty() }; if (segment?.all { it.isDigit() } == true) { extractedScooterCode = segment } } catch (e: Exception) { Log.e("VM_SCAN", "Parse error", e) } }
            if (extractedScooterCode == null && rawCode.all { it.isDigit() }) { extractedScooterCode = rawCode }

            if (extractedScooterCode != null) {
                processScooterCode(extractedScooterCode)
            } else {
                processBatteryCode(rawCode)
            }
        }
    }

    fun addManualCode(code: String) {
        if (_isSearchMode.value) {
            searchBatteryInFirestore(code)
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
            val defaultMessage = if (_isSearchMode.value) {
                "РЕЖИМ ПОИСКА: Наведи на АКБ"
            } else {
                if (_scanMode.value == ScanMode.QR) "Наведите камеру на QR-код" else "Наведите камеру на текст"
            }
            if (_statusMessage.value == message) {
                _statusMessage.value = defaultMessage
            }
        }
    }


    fun getCurrentMetrics(): ActivityLogMetrics {
        val currentList = when (_activeTab.value) {
            ActiveTab.SCOOTERS -> scooterCodes
            ActiveTab.BATTERIES -> batteryCodes
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
        firestore.collection("pallet_activity_log").orderBy("timestamp", Query.Direction.DESCENDING).limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { Log.e("PalletLog", "Listen failed.", e); return@addSnapshotListener }
                if (snapshot != null) {
                    val logEntries = snapshot.documents.mapNotNull { it.toObject(
                        PalletActivityLogEntry::class.java) }
                    _palletDistributionState.update { it.copy(activityLog = logEntries) }
                }
            }
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
        _palletDistributionState.update { it.copy(undistributedItemCount = _batteryCodes.size) }
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
                loadPallets()
            } catch (e: Exception) {
                _palletDistributionState.update { it.copy(isLoading = false, error = "Не удалось создать палет") }
            }
        }
    }

    fun distributeBatteriesToPallet(pallet: StoragePallet) {
        val itemsToDistribute = _batteryCodes.toList()
        if (itemsToDistribute.isEmpty()) return
        executeBatchDistribution(pallet, itemsToDistribute)
    }

    private fun executeBatchDistribution(pallet: StoragePallet, itemsToDistribute: List<ScanItem>) {
        viewModelScope.launch {
            _palletDistributionState.update { it.copy(isDistributing = true, distributionResult = null) }
            val batteryCodesList = itemsToDistribute.map { it.code }
            val batteriesToAdd = mutableListOf<String>()
            var duplicateCount = 0; var successfulCount = 0
            try {
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
                        if (snapshot.getString("status") != "on_storage") {
                            batch.update(batteryRef, "status", "on_storage", "palletId", pallet.id)
                            batteriesToAdd.add(batteryId); successfulCount++
                        } else { duplicateCount++ }
                    } else {
                        val newBatteryData = mapOf("id" to batteryId, "status" to "on_storage", "palletId" to pallet.id, "createdAt" to FieldValue.serverTimestamp())
                        batch.set(batteryRef, newBatteryData); batteriesToAdd.add(batteryId); successfulCount++
                    }
                }
                if (batteriesToAdd.isNotEmpty()) { batch.update(palletRef, "items", FieldValue.arrayUnion(*batteriesToAdd.toTypedArray())) }
                batch.commit().await()
                _batteryCodes.removeAll { item -> batteryCodesList.contains(item.code) }
                if (successfulCount > 0) { logPalletActivity(action = "DISTRIBUTED", pallet = pallet, itemCount = successfulCount) }
            } catch (e: Exception) {
                _palletDistributionState.update { it.copy(isDistributing = false, error = "Ошибка пакетной записи: ${e.message}") }; loadPallets(); return@launch
            }
            val resultMessage = "Пакетная запись завершена. Успешно: $successfulCount. Уже на складе: $duplicateCount."
            _palletDistributionState.update { it.copy(isDistributing = false, undistributedItemCount = _batteryCodes.size, distributionResult = resultMessage) }
            loadPallets()
        }
    }

    // --- ФУНКЦИЯ ДЛЯ ОТМЕНЫ УДАЛЕНИЯ (UNDO) ---
    fun distributeSpecificItemToPallet(pallet: StoragePallet, batteryId: String) {
        viewModelScope.launch {
            _palletDistributionState.update { it.copy(isDistributing = true) }
            try {
                // Прямой возврат в базу, минуя буфер сканера
                val palletRef = firestore.collection("storage_pallets").document(pallet.id)
                val batteryRef = firestore.collection("batteries").document(batteryId)

                firestore.runTransaction { transaction ->
                    // 1. Восстанавливаем статус батареи
                    transaction.set(batteryRef, mapOf(
                        "id" to batteryId,
                        "status" to "on_storage",
                        "palletId" to pallet.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    ))
                    // 2. Добавляем обратно в массив палета
                    transaction.update(palletRef, "items", FieldValue.arrayUnion(batteryId))
                }.await()

                // Если вдруг сканер успел поймать его в буфер - убираем
                _batteryCodes.removeAll { it.code == batteryId }

                logPalletActivity(action = "RESTORED_ITEM", pallet = pallet, itemCount = 1)
                _palletDistributionState.update { it.copy(isDistributing = false, distributionResult = "АКБ $batteryId восстановлен.") }
                loadPallets()

            } catch (e: Exception) {
                _palletDistributionState.update { it.copy(isDistributing = false, error = "Не удалось восстановить АКБ: ${e.message}") }
            }
        }
    }
    // ------------------------------------------

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
                loadPallets()
            } catch (e: Exception) {
                _palletDistributionState.update { it.copy(isDistributing = false, error = "Не удалось удалить АКБ: ${e.message}") }
            }
        }
    }

    fun deletePallet(pallet: StoragePallet) {
        viewModelScope.launch {
            _palletDistributionState.update { it.copy(isDistributing = true, distributionResult = null) }
            try {
                val palletRef = firestore.collection("storage_pallets").document(pallet.id)
                val itemsToReset = pallet.items
                val batch = firestore.batch()
                itemsToReset.forEach { batteryId ->
                    val batteryRef = firestore.collection("batteries").document(batteryId)
                    batch.update(batteryRef, "status", FieldValue.delete(), "palletId", FieldValue.delete())
                }
                batch.commit().await()
                palletRef.delete().await()

                logPalletActivity(action = "DELETED", pallet = pallet, itemCount = itemsToReset.size)

                // ИСПРАВЛЕНИЕ: Немедленно обновляем UI, убирая палет из списка
                val currentPallets = _palletDistributionState.value.pallets
                _palletDistributionState.update {
                    it.copy(
                        pallets = currentPallets.filter { p -> p.id != pallet.id },
                        isDistributing = false,
                        distributionResult = "Палет №${pallet.palletNumber} удален. Статусы АКБ сброшены."
                    )
                }
            } catch (e: Exception) {
                _palletDistributionState.update { it.copy(isDistributing = false, error = "Ошибка удаления палета: ${e.message}") }
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
                .onSuccess {
                    // loadStorageCells() теперь не нужен, UI обновится сам
                }
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
                .onSuccess { /* UI обновится сам */ }
                .onFailure { error ->
                    _storageState.update { it.copy(isLoading = false, error = "Ошибка обновления: ${error.message}") }
                }
        }
    }

    fun removeItemFromCell(cell: StorageCell, scooterId: String) {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true) }
            storageRepository.removeItemFromCell(cell, scooterId)
                .onSuccess { /* UI обновится сам */ }
                .onFailure { error ->
                    _storageState.update { it.copy(isLoading = false, error = "Ошибка удаления самоката: ${error.message}") }
                }
        }
    }

    fun deleteCell(cell: StorageCell) {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true) }
            storageRepository.deleteCell(cell)
                .onSuccess { /* UI обновится сам */ }
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