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
import com.example.qrscannerapp.features.inventory.data.repository.StorageRepository
import com.example.qrscannerapp.features.inventory.domain.model.*
import com.example.qrscannerapp.features.inventory.ui.*
import com.example.qrscannerapp.features.scanner.data.local.entity.ScanSessionEntity
import com.example.qrscannerapp.features.scanner.data.repository.ScanSessionRepository
import com.example.qrscannerapp.features.scanner.domain.model.ScanItem
import com.example.qrscannerapp.features.scanner.domain.model.ScanSession
import com.example.qrscannerapp.features.scanner.domain.util.ScannerCodeUtils
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
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

data class DistributionReport(
    val successCount: Int,
    val errorCount: Int,
    val duplicateCount: Int,
    val excludedItems: List<ExcludedItem>,
    val targetPalletNumber: Int,
    val targetManufacturer: String?
)

data class ExcludedItem(
    val indexInList: Int,
    val code: String,
    val reason: String
)

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

data class BatchConfig(
    val isActive: Boolean = false,
    val targetCount: Int = 50,
    val sessionType: ActiveTab = ActiveTab.BATTERIES,
    val batteryType: String = "FUJIAN"
)

sealed interface BatchEvent {
    object Completed : BatchEvent
    object NearlyDone : BatchEvent
}

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    private val application: Application,
    private val authManager: AuthManager,
    private val presenceManager: PresenceManager,
    private val sessionRepository: ScanSessionRepository,
    private val storageRepository: StorageRepository
) : AndroidViewModel(application) {
    private val firestore = Firebase.firestore
    private var storageActivityListener: ListenerRegistration? = null
    private var palletLogListener: ListenerRegistration? = null

    private val _scooterCodes = mutableStateListOf<ScanItem>()
    val scooterCodes: List<ScanItem> = _scooterCodes
    private val _batteryCodes = mutableStateListOf<ScanItem>()
    val batteryCodes: List<ScanItem> = _batteryCodes
    private val _newBatteryCodes = mutableStateListOf<ScanItem>()
    val newBatteryCodes: List<ScanItem> = _newBatteryCodes

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

    private val _duplicateCount = MutableStateFlow(0)
    val duplicateCount: StateFlow<Int> = _duplicateCount.asStateFlow()

    private val _scanRate = MutableStateFlow(0)
    val scanRate: StateFlow<Int> = _scanRate.asStateFlow()
    private val _lastScanTimestamps = ArrayDeque<Long>()

    private val _batchConfig = MutableStateFlow(BatchConfig())
    val batchConfig: StateFlow<BatchConfig> = _batchConfig.asStateFlow()

    private val _batchEventChannel = Channel<BatchEvent>(Channel.BUFFERED)
    val batchEvent = _batchEventChannel.receiveAsFlow()

    private val _palletDistributionState = MutableStateFlow(PalletDistributionUiState())
    val palletDistributionState = _palletDistributionState.asStateFlow()

    private val _distributionReport = MutableStateFlow<DistributionReport?>(null)
    val distributionReport = _distributionReport.asStateFlow()

    private val _todayAddedCount = MutableStateFlow(0)
    val todayAddedCount = _todayAddedCount.asStateFlow()

    private val _palletAnalysisResults = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val palletAnalysisResults = _palletAnalysisResults.asStateFlow()

    private val _showAnalysisReport = MutableStateFlow(false)
    val showAnalysisReport = _showAnalysisReport.asStateFlow()

    fun dismissAnalysisReport() { _showAnalysisReport.value = false }
    fun dismissDistributionReport() { _distributionReport.value = null }

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

    private val _isSearchingForScooter = MutableStateFlow(false)
    val isSearchingForScooter: StateFlow<Boolean> = _isSearchingForScooter.asStateFlow()

    private val _scooterSearchResult = MutableStateFlow<Pair<String, String>?>(null)
    val scooterSearchResult: StateFlow<Pair<String, String>?> = _scooterSearchResult.asStateFlow()

    private val _highlightedPalletId = MutableStateFlow<String?>(null)
    val highlightedPalletId = _highlightedPalletId.asStateFlow()

    @Volatile private var lastScannedValue: String = ""
    @Volatile private var lastProcessedTime: Long = 0L
    private val SCAN_DEBOUNCE_DELAY = 1500L
    private val SEARCH_DEBOUNCE_DELAY = 500L

    init {
        listenForSharedHistory()
        listenForLocalInventoryChanges()
    }

    // ============================================================================================
    // РЕЖИМ ПАРТИИ
    // ============================================================================================

    fun setBatchConfig(config: BatchConfig) {
        _batchConfig.value = config
        if (config.isActive) {
            _activeTab.value = config.sessionType
        }
    }

    fun clearBatchMode() {
        _batchConfig.value = BatchConfig(isActive = false)
    }

    fun getBatchProgress(): Pair<Int, Int> {
        val config = _batchConfig.value
        val current = when (config.sessionType) {
            ActiveTab.SCOOTERS -> _scooterCodes.size
            ActiveTab.BATTERIES -> _batteryCodes.size
            ActiveTab.NEW_BATTERIES -> _newBatteryCodes.size
            else -> 0
        }
        return current to config.targetCount
    }

    private suspend fun checkBatchProgress() {
        val config = _batchConfig.value
        if (!config.isActive) return
        val (current, target) = getBatchProgress()
        when {
            current >= target -> _batchEventChannel.send(BatchEvent.Completed)
            current == target - 5 -> _batchEventChannel.send(BatchEvent.NearlyDone)
        }
    }

    // ============================================================================================
    // СКОРОСТЬ СКАНИРОВАНИЯ
    // ============================================================================================

    private fun updateScanRate() {
        val now = System.currentTimeMillis()
        _lastScanTimestamps.addLast(now)
        while (_lastScanTimestamps.size > 30) _lastScanTimestamps.removeFirst()
        val windowStart = now - 60_000L
        val scansInWindow = _lastScanTimestamps.count { it >= windowStart }
        _scanRate.value = scansInWindow
    }

    // ============================================================================================
    // АНАЛИЗ ЯЧЕЕК (БЫВШИЙ АНАЛИЗ ПАЛЕТ)
    // ============================================================================================

    fun runPalletAnalysis() {
        viewModelScope.launch {
            _palletDistributionState.update { it.copy(isLoading = true) }
            delay(500)
            val currentPallets = _palletDistributionState.value.pallets
            val results = mutableMapOf<String, List<String>>()
            var totalErrors = 0
            currentPallets.forEach { pallet ->
                // Используем resolvedCellType — сначала cellType, fallback на manufacturer
                val cellTypeStr = pallet.cellType ?: pallet.manufacturer?.uppercase()
                if (cellTypeStr != null) {
                    val errorItems = pallet.items.filter { itemCode ->
                        isBatteryAlienToCell(itemCode, cellTypeStr)
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
                    it.copy(isLoading = false, distributionResult = "Анализ завершен. Ошибок не найдено.")
                }
            }
        }
    }

    /**
     * Проверяет, является ли АКБ "чужим" для данного типа ячейки.
     * Использует CellType enum для надёжного маппинга.
     */
    private fun isBatteryAlienToCell(code: String, cellTypeStr: String): Boolean {
        val cellType = CellType.entries.find { it.name.equals(cellTypeStr, ignoreCase = true) }
            ?: CellType.fromLegacyManufacturer(cellTypeStr)
            ?: return false // Тип не распознан — не считаем чужим

        val batteryType = CellType.fromBatteryCode(code) ?: return true // Код не распознан — чужой
        return batteryType != cellType
    }

    // ============================================================================================
    // ПОИСК
    // ============================================================================================

    fun setHighlightedPallet(palletId: String) {
        _highlightedPalletId.value = palletId
        viewModelScope.launch {
            delay(5000)
            if (_highlightedPalletId.value == palletId) _highlightedPalletId.value = null
        }
    }

    fun toggleSearchMode() {
        _isSearchMode.update { !it }
        if (!_isSearchMode.value) {
            _searchResult.value = null
            clearScooterSearchResult()
        }
        updateStatus(if (_isSearchMode.value) "РЕЖИМ ПОИСКА" else "Наведите камеру на QR-код")
    }

    fun clearSearchResult() { _searchResult.value = null }
    fun clearScooterSearchResult() { _scooterSearchResult.value = null }

    private fun searchBatteryInFirestore(code: String) {
        if (_isSearching.value) return
        _isSearching.value = true
        updateStatus("Поиск АКБ в базе...", isError = false)
        viewModelScope.launch {
            try {
                val querySnapshot = firestore.collection("storage_pallets")
                    .whereArrayContains("items", code).get().await()
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents.first()
                    val pallet = doc.toObject(StoragePallet::class.java)
                    if (pallet != null) {
                        val detectedType = CellType.fromBatteryCode(code)
                        val manufacturer = detectedType?.displayName ?: "Неизвестно"
                        _searchResult.value = BatterySearchResult(
                            batteryCode = code,
                            palletNumber = pallet.palletNumber,
                            palletId = pallet.id,
                            manufacturer = manufacturer,
                            creatorName = "Склад",
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

    private fun searchForScooter(code: String) {
        if (_isSearchingForScooter.value) return
        _isSearchingForScooter.value = true
        updateStatus("Поиск самоката на складе...", isError = false)
        viewModelScope.launch {
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

    private fun listenForLocalInventoryChanges() {
        storageRepository.getCellsFlow()
            .onEach { cells -> _storageState.update { it.copy(isLoading = false, cells = cells) } }
            .launchIn(viewModelScope)
        storageRepository.getPalletsFlow()
            .onEach { pallets -> _palletDistributionState.update { it.copy(isLoading = false, pallets = pallets) } }
            .launchIn(viewModelScope)
    }

    fun bulkAddScootersToCell(cellId: String, text: String) {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true) }
            storageRepository.bulkAddScootersToCell(cellId, text)
                .onSuccess { _storageState.update { it.copy(isLoading = false, distributionResult = "Номера добавлены.") } }
                .onFailure { error -> _storageState.update { it.copy(isLoading = false, error = "Ошибка добавления: ${error.message}") } }
        }
    }

    // ============================================================================================
    // УПРАВЛЕНИЕ ЯЧЕЙКАМИ (БЫВШЕЕ УПРАВЛЕНИЕ ПАЛЕТАМИ)
    // ============================================================================================

    /**
     * Устанавливает тип ячейки. Пишет в оба поля для обратной совместимости:
     * - cellType (новое) — имя CellType enum
     * - manufacturer (старое) — legacy значение
     */
    fun setCellType(palletId: String, cellType: CellType?) {
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any?>()
                if (cellType != null) {
                    updates["cellType"] = cellType.name
                    // Обратная совместимость: пишем и в manufacturer
                    updates["manufacturer"] = when (cellType) {
                        CellType.FUJIAN -> "FUJIAN"
                        CellType.BYD -> "BYD"
                        CellType.NINEBOT_NEW -> "NEW"
                        CellType.NINEBOT_OLD -> "OLD"
                    }
                } else {
                    updates["cellType"] = null
                    updates["manufacturer"] = null
                }
                firestore.collection("storage_pallets")
                    .document(palletId)
                    .update(updates)
                    .await()
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to set cell type", e)
            }
        }
    }

    /**
     * Обновляет ёмкость ячейки.
     */
    fun setCellCapacity(palletId: String, capacity: Int) {
        viewModelScope.launch {
            try {
                firestore.collection("storage_pallets")
                    .document(palletId)
                    .update("capacity", capacity)
                    .await()
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to set capacity", e)
            }
        }
    }

    /**
     * Обновляет статус ячейки.
     */
    fun setCellStatus(palletId: String, status: CellStatus) {
        viewModelScope.launch {
            try {
                firestore.collection("storage_pallets")
                    .document(palletId)
                    .update("status", status.name)
                    .await()
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to set cell status", e)
            }
        }
    }

    /**
     * Обновляет отображаемое имя ячейки.
     */
    fun setCellDisplayName(palletId: String, name: String?) {
        viewModelScope.launch {
            try {
                firestore.collection("storage_pallets")
                    .document(palletId)
                    .update("displayName", name)
                    .await()
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to set display name", e)
            }
        }
    }

    /**
     * Обновляет адрес ячейки.
     */
    fun setCellAddress(palletId: String, address: String?) {
        viewModelScope.launch {
            try {
                firestore.collection("storage_pallets")
                    .document(palletId)
                    .update("address", address)
                    .await()
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to set cell address", e)
            }
        }
    }

    // ============================================================================================
    // ИНДИВИДУАЛЬНЫЙ ЛОГ ЯЧЕЙКИ
    // ============================================================================================

    private val _cellActivityLog = MutableStateFlow<List<PalletActivityLogEntry>>(emptyList())
    val cellActivityLog: StateFlow<List<PalletActivityLogEntry>> = _cellActivityLog.asStateFlow()

    private val _isCellLogLoading = MutableStateFlow(false)
    val isCellLogLoading: StateFlow<Boolean> = _isCellLogLoading.asStateFlow()

    private var cellLogListener: ListenerRegistration? = null

    /**
     * Загружает лог операций для конкретной ячейки (по palletId).
     * Использует realtime listener для живого обновления.
     */
    fun loadCellActivityLog(palletId: String) {
        cellLogListener?.remove()
        _isCellLogLoading.value = true
        _cellActivityLog.value = emptyList()

        cellLogListener = firestore.collection("pallet_activity_log")
            .whereEqualTo("palletId", palletId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                _isCellLogLoading.value = false
                if (e != null) {
                    Log.e("CellLog", "Listen failed for $palletId", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _cellActivityLog.value = snapshot.documents.mapNotNull {
                        it.toObject(PalletActivityLogEntry::class.java)
                    }
                }
            }
    }

    /** Останавливает слушатель лога ячейки */
    fun stopCellActivityLog() {
        cellLogListener?.remove()
        cellLogListener = null
        _cellActivityLog.value = emptyList()
    }

    // ============================================================================================
    // РАСПОЗНАВАНИЕ ТИПА БАТАРЕИ ПО QR-КОДУ
    // ============================================================================================

    /**
     * Распознаёт тип батареи по QR-коду
     * Возвращает пару: (тип батареи, полный код)
     *
     * WIND 5.0:
     * - NEW: 5BB + 11 цифр (5BB32501113863) — формат: 1 цифра + 2 буквы + 11 цифр
     * - OLD: SF + 2 буквы + 11 символов (SFAEQ24A5A0175)
     *
     * WIND 4.0:
     * - FUJIAN: 4BB + 11 цифр
     * - BYD: 4BZ + 11 цифр
     */
    private fun processBatteryCodeType(rawCode: String): Pair<String, String>? {
        val code = rawCode.trim().uppercase()

        return when {
            // WIND 5.0 — НОВЫЕ АКБ: 5BB + 11 цифр
            code.startsWith("5BB") && code.length == 14 && code.substring(3).all { it.isDigit() } -> {
                "NEW" to code
            }

            // WIND 5.0 — СТАРЫЕ АКБ: SF + 2 буквы + 11 символов (буквы/цифры)
            code.startsWith("SF") && code.length >= 14 && code.length <= 16 -> {
                val afterSF = code.substring(2)
                if (afterSF.length >= 12 &&
                    afterSF.substring(0, 2).all { it.isLetter() } &&
                    afterSF.substring(2).all { it.isLetterOrDigit() }) {
                    "OLD" to code
                } else {
                    null
                }
            }

            // WIND 4.0 — FUJIAN: 4BB + 11 цифр
            code.startsWith("4BB") && code.length == 14 && code.substring(3).all { it.isDigit() } -> {
                "FUJIAN" to code
            }

            // WIND 4.0 — BYD: 4BZ + 11 цифр
            code.startsWith("4BZ") && code.length == 14 && code.substring(3).all { it.isDigit() } -> {
                "BYD" to code
            }

            else -> null
        }
    }

    // ============================================================================================
    // СКАНИРОВАНИЕ (ОБНОВЛЁННАЯ ЛОГИКА)
    // ============================================================================================

    fun onCodeScanned(rawCode: String) {
        val currentTime = System.currentTimeMillis()
        val debounceDelay = if (_isSearchMode.value) SEARCH_DEBOUNCE_DELAY else SCAN_DEBOUNCE_DELAY
        if (rawCode == lastScannedValue && (currentTime - lastProcessedTime) < debounceDelay) return

        lastScannedValue = rawCode
        lastProcessedTime = currentTime
        presenceManager.pingNow()

        viewModelScope.launch {
            // РЕЖИМ ПОИСКА
            if (_isSearchMode.value) {
                when (_activeTab.value) {
                    ActiveTab.SCOOTERS -> {
                        if (_isSearchingForScooter.value || _scooterSearchResult.value != null) return@launch
                        val extractedCode = ScannerCodeUtils.extractScooterCode(rawCode)
                        searchForScooter(extractedCode ?: rawCode)
                    }
                    ActiveTab.BATTERIES, ActiveTab.NEW_BATTERIES -> {
                        if (_searchResult.value != null || _isSearching.value) return@launch
                        searchBatteryInFirestore(rawCode)
                    }
                    ActiveTab.WAREHOUSE -> {}
                }
                return@launch
            }

            startSessionTimerIfNeeded()

            val batteryType = processBatteryCodeType(rawCode)
            if (batteryType != null) {
                val (type, fullCode) = batteryType
                processBatteryByType(type, fullCode)
                return@launch
            }

            val extractedCode = ScannerCodeUtils.extractScooterCode(rawCode)
            if (extractedCode != null) {
                processScooterCode(extractedCode)
            } else {
                updateStatus("❌ Неизвестный формат: $rawCode", isError = true)
            }
        }
    }

    // ============================================================================================
    // ОБРАБОТКА БАТАРЕЙ ПО ТИПУ
    // ============================================================================================

    private suspend fun processBatteryByType(type: String, code: String) {
        // WIND 5.0 (NEW + OLD) → NEW_BATTERIES tab
        // WIND 4.0 (FUJIAN + BYD) → BATTERIES tab
        val targetTab = when (type) {
            "NEW", "OLD" -> ActiveTab.NEW_BATTERIES
            "FUJIAN", "BYD" -> ActiveTab.BATTERIES
            else -> ActiveTab.BATTERIES
        }

        val isDuplicate = when (targetTab) {
            ActiveTab.NEW_BATTERIES -> _newBatteryCodes.any { it.code == code }
            ActiveTab.BATTERIES -> _batteryCodes.any { it.code == code }
            else -> false
        }

        if (isDuplicate) {
            _duplicateCount.update { it + 1 }
            updateStatus("⚠️ АКБ $code уже в списке!", isError = true)
            _scanEventChannel.send(ScanEvent.Duplicate)
            return
        }

        if (_activeTab.value != targetTab) {
            _activeTab.value = targetTab
        }

        val newItem = ScanItem(
            id = UUID.randomUUID().toString(),
            code = code,
            timestamp = System.currentTimeMillis()
        )

        when (targetTab) {
            ActiveTab.NEW_BATTERIES -> {
                _newBatteryCodes.add(0, newItem)
                _newItems.update { it + newItem.id }
            }
            ActiveTab.BATTERIES -> {
                _batteryCodes.add(0, newItem)
                _newItems.update { it + newItem.id }
            }
            else -> {}
        }

        _scanEffectChannel.send(UiEffect.ScanSuccess)
        _scanEventChannel.send(ScanEvent.Success)

        val label = when (type) {
            "NEW" -> "WIND 5.0 (Новый)"
            "OLD" -> "WIND 5.0 (Старый)"
            "FUJIAN" -> "WIND 4.0 FUJIAN"
            "BYD" -> "WIND 4.0 BYD"
            else -> "АКБ"
        }

        updateStatus("✓ $label: $code", isError = false)
        updateScanRate()
        checkBatchProgress()
    }

    private suspend fun processScooterCode(code: String) {
        if (_scooterCodes.any { it.code == code }) {
            _duplicateCount.update { it + 1 }
            updateStatus("Самокат $code уже в списке.", isError = true)
            _scanEventChannel.send(ScanEvent.Duplicate)
        } else {
            val newItem = ScanItem(code = code)
            _scooterCodes.add(0, newItem)
            _newItems.update { it + newItem.id }
            updateStatus("Самокат $code добавлен!")
            if (_activeTab.value == ActiveTab.BATTERIES || _activeTab.value == ActiveTab.NEW_BATTERIES) {
                _activeTab.value = ActiveTab.SCOOTERS
            }
            _scanEffectChannel.send(UiEffect.ScanSuccess)
            _scanEventChannel.send(ScanEvent.Success)
            updateScanRate()
            checkBatchProgress()
        }
    }

    // ============================================================================================
    // ИСТОРИЯ
    // ============================================================================================

    private fun listenForSharedHistory() {
        historyListener?.remove()
        historyListener = firestore.collection("scan_sessions")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.w("ViewModel", "History listen failed.", error); return@addSnapshotListener }
                if (snapshot != null) _scanSessions.value = snapshot.toObjects(ScanSession::class.java)
            }
    }

    // ============================================================================================
    // СОХРАНЕНИЕ СЕССИИ
    // ============================================================================================

    fun saveCurrentSession(name: String?) {
        if (_isSavingSession.value) return
        val currentUser = authManager.authState.value
        if (currentUser.userId == null) { updateStatus("Ошибка: Ты не залогинен, чувак", isError = true); return }

        viewModelScope.launch {
            _isSavingSession.value = true
            val (listToSave, sessionType) = when (_activeTab.value) {
                ActiveTab.SCOOTERS -> _scooterCodes.toList() to SessionType.SCOOTERS
                ActiveTab.BATTERIES -> _batteryCodes.toList() to SessionType.BATTERIES
                ActiveTab.NEW_BATTERIES -> _newBatteryCodes.toList() to SessionType.NEW_BATTERIES
                ActiveTab.WAREHOUSE -> emptyList<ScanItem>() to null
            }
            if (listToSave.isEmpty() || sessionType == null) {
                updateStatus("Сохранять нечего, список пустой.", isError = true)
                _isSavingSession.value = false
                return@launch
            }
            val sessionToUpload = ScanSession(
                id = UUID.randomUUID().toString(), name = name?.takeIf { it.isNotBlank() },
                type = sessionType, items = listToSave, timestamp = System.currentTimeMillis(),
                creatorId = currentUser.userId, creatorName = currentUser.userName ?: "Анонимус"
            )
            try {
                val itemMaps = sessionToUpload.items.map { item ->
                    buildMap<String, Any?> {
                        put("id", item.id)
                        put("code", item.code)
                        put("timestamp", item.timestamp)
                    }
                }
                val sessionMap = buildMap<String, Any?> {
                    put("id", sessionToUpload.id)
                    put("timestamp", sessionToUpload.timestamp)
                    put("type", sessionToUpload.type.name)
                    put("items", itemMaps)
                    sessionToUpload.name?.let { put("name", it) }
                    sessionToUpload.creatorId?.let { put("creatorId", it) }
                    sessionToUpload.creatorName
                        ?.takeIf { it.isNotBlank() }
                        ?.let { put("creatorName", it) }
                }
                firestore.collection("scan_sessions").document(sessionToUpload.id).set(sessionMap).await()
            } catch (e: Exception) {
                Log.w("ViewModel", "В облако не улетело. Сохраняем локально.", e)
                sessionRepository.saveSessionLocally(ScanSessionEntity(
                    id = sessionToUpload.id, name = sessionToUpload.name, type = sessionToUpload.type,
                    items = sessionToUpload.items, timestamp = sessionToUpload.timestamp,
                    creatorId = sessionToUpload.creatorId, creatorName = sessionToUpload.creatorName
                ))
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
            try { firestore.collection("scan_sessions").document(sessionId).delete().await() }
            catch (e: Exception) { Log.e("ViewModel", "Failed to delete session $sessionId", e); updateStatus("Ошибка удаления сессии из облака", isError = true) }
        }
    }

    fun deleteItemFromSession(sessionId: String, itemToDelete: ScanItem) {
        viewModelScope.launch {
            val sessionRef = firestore.collection("scan_sessions").document(sessionId)
            try {
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(sessionRef)
                    val session = snapshot.toObject(ScanSession::class.java) ?: throw Exception("Сессия не найдена")
                    val updatedItems = session.items.filter { it.id != itemToDelete.id }
                    if (updatedItems.isEmpty()) transaction.delete(sessionRef)
                    else transaction.update(sessionRef, "items", updatedItems)
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
        palletLogListener?.remove()
        cellLogListener?.remove()
        storageRepository.stopCellsRealtimeSync()
        storageRepository.stopPalletsRealtimeSync()
    }

    // ============================================================================================
    // РУЧНОЙ ВВОД (ОБНОВЛЁННАЯ ЛОГИКА)
    // ============================================================================================

    fun addManualCode(code: String) {
        if (_isSearchMode.value) {
            when (_activeTab.value) {
                ActiveTab.SCOOTERS -> searchForScooter(code)
                ActiveTab.BATTERIES, ActiveTab.NEW_BATTERIES -> searchBatteryInFirestore(code)
                ActiveTab.WAREHOUSE -> {}
            }
            return
        }

        startSessionTimerIfNeeded()
        _manualEntryCount++

        val cleanCode = code.trim().uppercase()
        val batteryType = processBatteryCodeType(cleanCode)

        viewModelScope.launch {
            when {
                batteryType != null -> {
                    val (type, fullCode) = batteryType
                    processBatteryByType(type, fullCode)
                }
                _activeTab.value == ActiveTab.SCOOTERS -> {
                    if (cleanCode.isBlank() || !cleanCode.all { it.isLetterOrDigit() }) {
                        updateStatus("Ошибка: Недопустимые символы.", isError = true)
                        return@launch
                    }
                    processScooterCode(cleanCode)
                }
                _activeTab.value == ActiveTab.BATTERIES -> {
                    if (cleanCode.isBlank()) {
                        updateStatus("Ошибка: Код не может быть пустым.", isError = true)
                        return@launch
                    }
                    if (_batteryCodes.any { it.code == cleanCode }) {
                        _duplicateCount.update { it + 1 }
                        updateStatus("АКБ $cleanCode уже в списке.", isError = true)
                        return@launch
                    }
                    val newItem = ScanItem(code = cleanCode)
                    _batteryCodes.add(0, newItem)
                    _newItems.update { it + newItem.id }
                    updateStatus("АКБ $cleanCode успешно добавлен!")
                    updateScanRate()
                    checkBatchProgress()
                }
                _activeTab.value == ActiveTab.NEW_BATTERIES -> {
                    if (cleanCode.isBlank()) {
                        updateStatus("Ошибка: Код не может быть пустым.", isError = true)
                        return@launch
                    }
                    if (_newBatteryCodes.any { it.code == cleanCode }) {
                        _duplicateCount.update { it + 1 }
                        updateStatus("АКБ $cleanCode уже в списке.", isError = true)
                        return@launch
                    }
                    val newItem = ScanItem(code = cleanCode)
                    _newBatteryCodes.add(0, newItem)
                    _newItems.update { it + newItem.id }
                    updateStatus("WIND 5.0 АКБ $cleanCode добавлен!")
                    updateScanRate()
                    checkBatchProgress()
                }
                else -> {
                    updateStatus("❌ Неизвестный формат: $cleanCode", isError = true)
                }
            }
        }
    }

    fun markAsOld(item: ScanItem) { _newItems.update { it - item.id } }

    fun clearList() {
        _sessionStartTimeMillis = 0L
        _manualEntryCount = 0
        _newItems.update { emptySet() }
        lastScannedValue = ""
        lastProcessedTime = 0L
        _duplicateCount.value = 0
        _scanRate.value = 0
        _lastScanTimestamps.clear()
        when (_activeTab.value) {
            ActiveTab.SCOOTERS -> _scooterCodes.clear()
            ActiveTab.BATTERIES -> _batteryCodes.clear()
            ActiveTab.NEW_BATTERIES -> _newBatteryCodes.clear()
            ActiveTab.WAREHOUSE -> {}
        }
        updateStatus("Список очищен")
    }

    fun removeCode(item: ScanItem) {
        _scooterCodes.remove(item)
        _batteryCodes.remove(item)
        _newBatteryCodes.remove(item)
    }

    fun sortCurrentList() {
        when (_activeTab.value) {
            ActiveTab.SCOOTERS -> _scooterCodes.sortBy { it.code.toLongOrNull() ?: Long.MAX_VALUE }
            ActiveTab.BATTERIES -> _batteryCodes.sortBy { it.code }
            ActiveTab.NEW_BATTERIES -> _newBatteryCodes.sortBy { it.code }
            ActiveTab.WAREHOUSE -> {}
        }
    }

    private fun startSessionTimerIfNeeded() {
        if (_scooterCodes.isEmpty() && _batteryCodes.isEmpty() && _newBatteryCodes.isEmpty()) {
            _sessionStartTimeMillis = System.currentTimeMillis()
            _manualEntryCount = 0
        }
    }

    fun onTabSelected(tab: ActiveTab) { _activeTab.value = tab }

    fun updateStatus(message: String, isError: Boolean = false) {
        viewModelScope.launch {
            _statusMessage.value = message
            delay(if (isError) 2000 else 1500)
            val defaultMessage = if (_isSearchMode.value) "РЕЖИМ ПОИСКА" else "Наведите камеру на QR-код"
            if (_statusMessage.value == message) _statusMessage.value = defaultMessage
        }
    }

    fun getCurrentMetrics(): ActivityLogMetrics {
        val currentList = when (_activeTab.value) {
            ActiveTab.SCOOTERS -> scooterCodes
            ActiveTab.BATTERIES -> batteryCodes
            ActiveTab.NEW_BATTERIES -> newBatteryCodes
            ActiveTab.WAREHOUSE -> return ActivityLogMetrics(0, 0, 0L)
        }
        val itemCount = currentList.size
        val durationSeconds = if (_sessionStartTimeMillis != 0L && itemCount > 0)
            (System.currentTimeMillis() - _sessionStartTimeMillis) / 1000 else 0L
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
                    "timestamp" to System.currentTimeMillis(), "activityType" to activityType,
                    "creatorId" to currentUserId, "creatorName" to (currentUserName ?: "Unknown"),
                    "itemCount" to activityMetrics.itemCount, "manualEntryCount" to activityMetrics.manualEntryCount,
                    "durationSeconds" to activityMetrics.durationSeconds, "networkPing" to networkPing
                )
                logData.putAll(deviceTelemetry)
                firestore.collection("activity_log").add(logData).await()
            } catch (e: Exception) {
                Log.e("Metrics", "Failed to log activity '$activityType'", e)
            }
        }
    }

    private fun <T> List<T>.chunkedSafe(size: Int): List<List<T>> = chunked(size).filter { it.isNotEmpty() }

    // ============================================================================================
    // РАСПРЕДЕЛЕНИЕ АКБ ПО ЯЧЕЙКАМ
    // ============================================================================================

    fun distributeBatteriesToPallet(pallet: StoragePallet) {
        val allItems = when (_activeTab.value) {
            ActiveTab.NEW_BATTERIES -> _newBatteryCodes.toList()
            else -> _batteryCodes.toList()
        }
        if (allItems.isEmpty()) return

        val cellTypeStr = pallet.cellType ?: pallet.manufacturer?.uppercase()
        val validItems = mutableListOf<ScanItem>()
        val excludedItemsList = mutableListOf<ExcludedItem>()

        allItems.forEachIndexed { index, item ->
            when {
                // Проверка ёмкости
                pallet.items.size + validItems.size >= pallet.capacity -> {
                    excludedItemsList.add(ExcludedItem(index + 1, item.code, "Ячейка заполнена"))
                }
                // Проверка типа
                cellTypeStr != null && isBatteryAlienToCell(item.code, cellTypeStr) -> {
                    excludedItemsList.add(ExcludedItem(index + 1, item.code, "Чужой тип АКБ"))
                }
                else -> validItems.add(item)
            }
        }

        if (validItems.isEmpty() && excludedItemsList.isNotEmpty()) {
            _distributionReport.value = DistributionReport(
                0, excludedItemsList.size, 0, excludedItemsList,
                pallet.palletNumber, cellTypeStr
            )
            return
        }
        executeBatchDistribution(pallet, validItems, excludedItemsList)
    }

    private fun executeBatchDistribution(pallet: StoragePallet, itemsToDistribute: List<ScanItem>, excludedItems: List<ExcludedItem>) {
        viewModelScope.launch {
            _palletDistributionState.update { it.copy(isDistributing = true, distributionResult = null) }
            val batteryCodesList = itemsToDistribute.map { it.code }
            val batteriesToAdd = mutableListOf<String>()
            var duplicateCount = 0
            var successfulCount = 0
            try {
                val allBatteryDocs = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()
                batteryCodesList.chunkedSafe(10).forEach { chunk ->
                    firestore.collection("batteries").whereIn(FieldPath.documentId(), chunk).get().await()
                        .documents.forEach { doc -> doc.id?.let { id -> allBatteryDocs[id] = doc } }
                }
                val palletRef = firestore.collection("storage_pallets").document(pallet.id)
                // Firestore: не больше 500 операций в одном batch. Один batch на тысячи АКБ → отказ записи.
                val batchChunkSize = 400
                val chunks = itemsToDistribute.chunked(batchChunkSize)
                val totalChunks = chunks.size.coerceAtLeast(1)
                chunks.forEachIndexed { idx, chunk ->
                    val batch = firestore.batch()
                    val newOnPallet = mutableListOf<String>()
                    for (item in chunk) {
                        val batteryRef = firestore.collection("batteries").document(item.code)
                        val snapshot = allBatteryDocs[item.code]
                        if (snapshot != null) {
                            if (snapshot.getString("status") == "on_storage") duplicateCount++
                            else {
                                batch.update(batteryRef, "status", "on_storage", "palletId", pallet.id)
                                newOnPallet.add(item.code)
                                successfulCount++
                            }
                        } else {
                            batch.set(
                                batteryRef,
                                mapOf(
                                    "id" to item.code,
                                    "status" to "on_storage",
                                    "palletId" to pallet.id,
                                    "createdAt" to FieldValue.serverTimestamp()
                                )
                            )
                            newOnPallet.add(item.code)
                            successfulCount++
                        }
                    }
                    if (newOnPallet.isNotEmpty()) {
                        batch.update(palletRef, "items", FieldValue.arrayUnion(*newOnPallet.toTypedArray()))
                        batteriesToAdd.addAll(newOnPallet)
                        Log.d(
                            "BatteryDistribution",
                            "storage_pallets: batch ${idx + 1}/$totalChunks, +${newOnPallet.size} АКБ → ячейка №${pallet.palletNumber} (${pallet.id})"
                        )
                        batch.commit().await()
                    }
                }
                val successCodes = batteriesToAdd.toSet()
                _batteryCodes.removeAll { successCodes.contains(it.code) }
                _newBatteryCodes.removeAll { successCodes.contains(it.code) }
                if (successfulCount > 0) logPalletActivity(action = "DISTRIBUTED", pallet = pallet, itemCount = successfulCount)
                val cellTypeStr = pallet.cellType ?: pallet.manufacturer?.uppercase()
                _distributionReport.value = DistributionReport(successfulCount, excludedItems.size, duplicateCount, excludedItems, pallet.palletNumber, cellTypeStr)
                _palletDistributionState.update { it.copy(isDistributing = false, undistributedItemCount = _batteryCodes.size + _newBatteryCodes.size) }
            } catch (e: Exception) {
                val msg = when (e) {
                    is FirebaseFirestoreException -> when (e.code) {
                        FirebaseFirestoreException.Code.INVALID_ARGUMENT ->
                            "Слишком большой документ или запрос (лимит Firestore ~1 МБ на документ / 500 операций в batch). Разбейте приёмку на части."
                        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                            "Нет прав на запись в Firestore."
                        FirebaseFirestoreException.Code.UNAVAILABLE ->
                            "Сеть недоступна. Повторите позже."
                        else -> e.message ?: e.code.toString()
                    }
                    else -> e.message
                }
                Log.e("BatteryDistribution", "executeBatchDistribution failed", e)
                _palletDistributionState.update { it.copy(isDistributing = false, error = "Ошибка записи: $msg") }
            }
        }
    }

    fun distributeSpecificItemToPallet(pallet: StoragePallet, batteryId: String) {
        viewModelScope.launch {
            _palletDistributionState.update { it.copy(isDistributing = true) }
            try {
                firestore.runTransaction { transaction ->
                    transaction.set(firestore.collection("batteries").document(batteryId), mapOf("id" to batteryId, "status" to "on_storage", "palletId" to pallet.id, "createdAt" to FieldValue.serverTimestamp()))
                    transaction.update(firestore.collection("storage_pallets").document(pallet.id), "items", FieldValue.arrayUnion(batteryId))
                }.await()
                _batteryCodes.removeAll { it.code == batteryId }
                _newBatteryCodes.removeAll { it.code == batteryId }
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
                firestore.runTransaction { transaction ->
                    transaction.update(firestore.collection("storage_pallets").document(palletId), "items", FieldValue.arrayRemove(batteryId))
                    transaction.update(firestore.collection("batteries").document(batteryId), "status", FieldValue.delete(), "palletId", FieldValue.delete())
                }.await()
                if (pallet != null) logPalletActivity(action = "REMOVED_ITEM", pallet = pallet, itemCount = 1)
                _palletDistributionState.update { it.copy(isDistributing = false, distributionResult = "АКБ $batteryId удален с ячейки и его статус сброшен.") }
            } catch (e: Exception) {
                _palletDistributionState.update { it.copy(isDistributing = false, error = "Не удалось удалить АКБ: ${e.message}") }
            }
        }
    }

    // ============================================================================================
    // СОЗДАНИЕ / УДАЛЕНИЕ ЯЧЕЕК
    // ============================================================================================

    /**
     * Создаёт новую ячейку для АКБ. Опционально принимает тип и ёмкость.
     * Пишет через HashMap чтобы Firestore гарантированно записал все поля.
     */
    fun createNewBatteryCell(
        cellType: CellType? = null,
        capacity: Int = 500,
        displayName: String? = null,
        address: String? = null
    ) {
        viewModelScope.launch {
            _palletDistributionState.update { it.copy(isLoading = true) }
            try {
                val newNumber = (_palletDistributionState.value.pallets
                    .maxOfOrNull { it.palletNumber } ?: 0) + 1
                val newId = UUID.randomUUID().toString()

                val data = hashMapOf<String, Any?>(
                    "id" to newId,
                    "palletNumber" to newNumber,
                    "items" to emptyList<String>(),
                    "isFull" to false,
                    // Новые поля
                    "cellType" to cellType?.name,
                    "manufacturer" to when (cellType) {
                        CellType.FUJIAN -> "FUJIAN"
                        CellType.BYD -> "BYD"
                        CellType.NINEBOT_NEW -> "NEW"
                        CellType.NINEBOT_OLD -> "OLD"
                        null -> null
                    },
                    "capacity" to capacity,
                    "displayName" to displayName,
                    "address" to address,
                    "status" to CellStatus.RECEIVING.name,
                    "creatorId" to authManager.currentUserId,
                    "creatorName" to authManager.authState.value.userName,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                firestore.collection("storage_pallets")
                    .document(newId)
                    .set(data)
                    .await()

                // Для лога создаём объект
                val newCell = StoragePallet(
                    id = newId,
                    palletNumber = newNumber,
                    cellType = cellType?.name,
                    capacity = capacity,
                    displayName = displayName,
                    address = address
                )
                logPalletActivity(action = "CREATED", pallet = newCell)
            } catch (e: Exception) {
                _palletDistributionState.update {
                    it.copy(isLoading = false, error = "Не удалось создать ячейку")
                }
            }
        }
    }

    fun deletePallet(pallet: StoragePallet) {
        viewModelScope.launch {
            _palletDistributionState.update { it.copy(isDistributing = true, distributionResult = null) }
            try {
                storageRepository.deletePallet(pallet)
                    .onSuccess { logPalletActivity(action = "DELETED", pallet = pallet, itemCount = pallet.items.size); _palletDistributionState.update { it.copy(isDistributing = false, distributionResult = "Ячейка №${pallet.palletNumber} удалена.") } }
                    .onFailure { e -> _palletDistributionState.update { it.copy(isDistributing = false, error = "Ошибка удаления ячейки: ${e.message}") } }
            } catch (e: Exception) {
                _palletDistributionState.update { it.copy(isDistributing = false, error = "Ошибка: ${e.message}") }
            }
        }
    }

    fun getPalletsDataForMasterExport(): Map<String, List<String>> =
        _palletDistributionState.value.pallets.associate { "Ячейка №${it.palletNumber}" to it.items.toList() }

    fun clearDistributionResult() { _palletDistributionState.update { it.copy(distributionResult = null) } }

    // ============================================================================================
    // ЛОГ ОПЕРАЦИЙ С ЯЧЕЙКАМИ
    // ============================================================================================

    private fun startPalletActivityLogListener() {
        palletLogListener?.remove()
        palletLogListener = firestore.collection("pallet_activity_log").orderBy("timestamp", Query.Direction.DESCENDING).limit(100)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { Log.e("PalletLog", "Listen failed.", e); return@addSnapshotListener }
                if (snapshot != null) {
                    val logEntries = snapshot.documents.mapNotNull { it.toObject(PalletActivityLogEntry::class.java) }
                    _palletDistributionState.update { it.copy(activityLog = logEntries) }
                    calculateTodayAddedCount(logEntries)
                }
            }
    }

    private fun calculateTodayAddedCount(logEntries: List<PalletActivityLogEntry>) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        _todayAddedCount.value = logEntries
            .filter { it.timestamp >= calendar.timeInMillis && it.action == "DISTRIBUTED" }
            .sumOf { it.itemCount ?: 0 }
    }

    private fun logPalletActivity(action: String, pallet: StoragePallet? = null, itemCount: Int = 0) {
        val currentUserId = authManager.currentUserId
        val currentUserName = authManager.authState.value.userName
        if (currentUserId == null) return
        val logEntry = PalletActivityLogEntry(
            userId = currentUserId, userName = currentUserName, action = action,
            palletNumber = pallet?.palletNumber, itemCount = itemCount, palletId = pallet?.id
        )
        viewModelScope.launch {
            try { firestore.collection("pallet_activity_log").add(logEntry).await() }
            catch (e: Exception) { Log.e("PalletLog", "Failed to write activity log", e) }
        }
    }

    fun clearPalletActivityLog() {
        viewModelScope.launch {
            if (!authManager.authState.value.isAdmin) { _palletDistributionState.update { it.copy(error = "У вас нет прав.") }; return@launch }
            _palletDistributionState.update { it.copy(isDistributing = true) }
            try {
                val snapshot = firestore.collection("pallet_activity_log").get().await()
                snapshot.documents.chunked(499).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
                _palletDistributionState.update { it.copy(isDistributing = false, distributionResult = "История операций успешно очищена.") }
            } catch (e: Exception) {
                _palletDistributionState.update { it.copy(isDistributing = false, error = "Не удалось очистить историю: ${e.message}") }
            }
        }
    }

    fun onNavigateToPalletDistribution() {
        _palletAnalysisResults.value = emptyMap()
        _showAnalysisReport.value = false
        _palletDistributionState.update { it.copy(undistributedItemCount = _batteryCodes.size + _newBatteryCodes.size) }
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

    // ============================================================================================
    // ХРАНЕНИЕ САМОКАТОВ (STORAGE)
    // ============================================================================================

    fun loadStorageCells() {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true, error = null) }
            startStorageActivityLogListener()
            storageRepository.startCellsRealtimeSync()
            storageRepository.refreshDataFromServer()
        }
    }

    fun createNewCell(description: String, capacity: Int) {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true) }
            storageRepository.createNewCell(description, capacity)
                .onSuccess { _storageState.update { it.copy(isLoading = false, distributionResult = "Ячейка создана") } }
                .onFailure { error -> _storageState.update { it.copy(isLoading = false, error = "Не удалось создать ячейку: ${error.message}") } }
        }
    }

    fun distributeScootersToCell(cell: StorageCell) {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true) }
            val scooterIds = scooterCodes.map { it.code }
            storageRepository.distributeScootersToCell(cell, scooterIds)
                .onSuccess { addedCount ->
                    val skipped = scooterIds.size - addedCount
                    var msg = "Успешно добавлено: $addedCount."
                    if (skipped > 0) msg += " Пропущено дубликатов: $skipped."
                    _storageState.update { it.copy(isLoading = false, distributionResult = msg) }
                    _scooterCodes.clear()
                }
                .onFailure { error -> _storageState.update { it.copy(isLoading = false, distributionResult = "Ошибка: ${error.message}") } }
        }
    }

    fun clearStorageDistributionResult() { _storageState.update { it.copy(distributionResult = null) } }

    fun updateCell(cellId: String, newDescription: String, newCapacity: Int) {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true) }
            storageRepository.updateCell(cellId, newDescription, newCapacity)
                .onSuccess { _storageState.update { it.copy(isLoading = false, distributionResult = "Ячейка обновлена") } }
                .onFailure { error -> _storageState.update { it.copy(isLoading = false, error = "Ошибка обновления: ${error.message}") } }
        }
    }

    fun removeItemFromCell(cell: StorageCell, scooterId: String) {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true) }
            storageRepository.removeItemFromCell(cell, scooterId)
                .onSuccess { _storageState.update { it.copy(isLoading = false, distributionResult = "Самокат удален из ячейки") } }
                .onFailure { error -> _storageState.update { it.copy(isLoading = false, error = "Ошибка удаления самоката: ${error.message}") } }
        }
    }

    fun deleteCell(cell: StorageCell) {
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true) }
            storageRepository.deleteCell(cell)
                .onSuccess { _storageState.update { it.copy(isLoading = false, distributionResult = "Ячейка удалена") } }
                .onFailure { error -> _storageState.update { it.copy(isLoading = false, error = "Ошибка удаления ячейки: ${error.message}") } }
        }
    }

    fun deleteCells(cells: List<StorageCell>) {
        if (cells.isEmpty()) return
        viewModelScope.launch {
            _storageState.update { it.copy(isLoading = true) }
            storageRepository.deleteMultipleCells(cells)
                .onSuccess { _storageState.update { it.copy(isLoading = false, distributionResult = "Успешно удалено ${cells.size} ячеек.") } }
                .onFailure { error -> _storageState.update { it.copy(isLoading = false, error = "Ошибка массового удаления: ${error.message}") } }
        }
    }

    private fun startStorageActivityLogListener() {
        storageActivityListener?.remove()
        storageActivityListener = firestore.collection("storage_activity_log")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { Log.w("ViewModel", "Storage activity log listen failed.", e); return@addSnapshotListener }
                if (snapshot != null) _storageState.update { it.copy(activityLog = snapshot.toObjects(StorageActivityLogEntry::class.java)) }
            }
    }

    fun clearStorageActivityLog() {
        viewModelScope.launch {
            if (!authManager.authState.value.isAdmin) { _storageState.update { it.copy(distributionResult = "У вас нет прав.") }; return@launch }
            _storageState.update { it.copy(isLoading = true) }
            try {
                val snapshot = firestore.collection("storage_activity_log").get().await()
                if (snapshot.isEmpty) { _storageState.update { it.copy(isLoading = false, distributionResult = "История операций уже пуста.") }; return@launch }
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
                _storageState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _storageState.update { it.copy(isLoading = false, distributionResult = "Не удалось очистить историю: ${e.message}") }
            }
        }
    }
}