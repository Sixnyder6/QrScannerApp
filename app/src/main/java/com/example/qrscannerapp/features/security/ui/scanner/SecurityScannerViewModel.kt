package com.example.qrscannerapp.features.security.ui.scanner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.DeviceLocation
import com.example.qrscannerapp.PresenceManager
import com.example.qrscannerapp.TelemetryManager
import com.example.qrscannerapp.features.security.ui.ScooterCoords
import com.example.qrscannerapp.features.security.ui.ScooterHistoryEntry
import com.example.qrscannerapp.features.security.ui.ScooterPassport
import com.example.qrscannerapp.features.security.ui.ScooterTag
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// ============================================================================================
// РЕЖИМ СКАНЕРА
// ============================================================================================

enum class ScannerMode(val label: String, val emoji: String) {
    SCOOTER("Самокат", "🛴"),
    BATTERY("АКБ", "🔋")
}

// ============================================================================================
// ДАННЫЕ САМОКАТА ИЗ ФЛИТА (умный поиск)
// ============================================================================================

data class FleetScooterInfo(
    val number: String = "",
    val model: String = "",
    val status: String = "",
    val process: String = "",
    val processStage: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val charge: Int = 0,
    val heartbeatLag: String = "",
    val statusUpdatedDate: String = "",
    val statusUpdatedTime: String = "",
    val vin: String = "",
    val errorCode: String = ""
) {
    // Живой если заряд > 0
    val isAlive: Boolean get() = charge > 0

    // Парсим heartbeat lag в часы для определения "мёртвости"
    val heartbeatHours: Int get() {
        if (heartbeatLag.isBlank()) return 0
        return try {
            heartbeatLag.split(":").firstOrNull()?.toIntOrNull() ?: 0
        } catch (e: Exception) { 0 }
    }

    // Подозрительный если не пингует больше 24 часов
    val isSuspicious: Boolean get() = heartbeatHours > 24

    val hasCoords: Boolean get() = lat != 0.0 && lon != 0.0
}

// ============================================================================================
// РЕЗУЛЬТАТ ПОИСКА АКБ
// ============================================================================================

data class BatteryLookupResult(
    val code: String,
    val palletNumber: Int,
    val palletId: String,
    val cellType: String?,
    val hub: String = "Неизвестно",
    val creatorName: String? = null
)

// ============================================================================================
// СТЕЙТ СКАНЕРА
// ============================================================================================

data class SecurityScannerState(
    val isProcessing: Boolean = false,
    val scannedCode: String? = null,
    val itemType: String = "Самокат",
    val mode: ScannerMode = ScannerMode.SCOOTER,

    // Диалог нового паспорта
    val showNewPassportDialog: Boolean = false,
    val currentLocation: DeviceLocation? = null,

    // Данные из флита (умный поиск)
    val fleetInfo: FleetScooterInfo? = null,
    val showFleetInfoDialog: Boolean = false,

    // АКБ
    val batteryLookupResult: BatteryLookupResult? = null,
    val showBatteryResultDialog: Boolean = false,
    val batteryNotFound: Boolean = false,

    val error: String? = null
)

// ============================================================================================
// СОБЫТИЯ
// ============================================================================================

sealed class SecurityScannerEvent {
    data class NavigateToPassport(val scooterId: String) : SecurityScannerEvent()
    object PlaySuccessBeep : SecurityScannerEvent()
    object PlayErrorBeep   : SecurityScannerEvent()
    object PlayWarningBeep : SecurityScannerEvent()
}

// ============================================================================================
// VIEWMODEL
// ============================================================================================

@HiltViewModel
class SecurityScannerViewModel @Inject constructor(
    private val telemetryManager: TelemetryManager,
    private val authManager: AuthManager,
    private val presenceManager: PresenceManager
) : ViewModel() {

    private val firestore = Firebase.firestore
    private val TAG = "SecScanner"

    private val _state = MutableStateFlow(SecurityScannerState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<SecurityScannerEvent>()
    val events = _events.asSharedFlow()

    private var lastScannedCode: String? = null
    private var lastScanTime: Long = 0L

    // ============================================================================================
    // РЕЖИМ
    // ============================================================================================

    fun setMode(mode: ScannerMode) {
        _state.update {
            it.copy(
                mode                    = mode,
                error                   = null,
                batteryLookupResult     = null,
                batteryNotFound         = false,
                showBatteryResultDialog = false,
                showFleetInfoDialog     = false,
                fleetInfo               = null,
                scannedCode             = null
            )
        }
        lastScannedCode = null
        lastScanTime = 0L
    }

    // ============================================================================================
    // УТИЛИТЫ
    // ============================================================================================

    private fun determineItemType(code: String): String = when {
        code.startsWith("5BB") && code.length == 14     -> "АКБ (WIND 5.0 Новый)"
        code.startsWith("SF")  && code.length in 14..16 -> "АКБ (WIND 5.0 Старый)"
        code.startsWith("4BB") && code.length == 14     -> "АКБ (WIND 4.0 FUJIAN)"
        code.startsWith("4BZ") && code.length == 14     -> "АКБ (WIND 4.0 BYD)"
        else                                             -> "Самокат"
    }

    private fun extractCleanCode(rawCode: String): String {
        var code = rawCode.trim().uppercase()
        when {
            code.contains("number=") ->
                code = code.substringAfter("number=").split('&', '?', '#').firstOrNull() ?: code
            code.contains('/') ->
                code = code.split('/').lastOrNull { it.isNotEmpty() } ?: code
        }
        if (code.startsWith("SCOOTERSNUMBER")) code = code.removePrefix("SCOOTERSNUMBER")
        return code.filter { it.isLetterOrDigit() }
    }

    // ============================================================================================
    // ТОЧКИ ВХОДА
    // ============================================================================================

    fun onCodeScanned(rawCode: String) {
        val code = extractCleanCode(rawCode)
        if (code.isBlank()) return
        val now = System.currentTimeMillis()
        if (code == lastScannedCode && (now - lastScanTime) < 2000L) return
        lastScannedCode = code
        lastScanTime = now
        presenceManager.pingNow()
        dispatch(code)
    }

    fun onManualInput(rawCode: String) {
        val code = rawCode.trim().uppercase().filter { it.isLetterOrDigit() }
        if (code.isBlank()) return
        lastScannedCode = null
        dispatch(code)
    }

    private fun dispatch(code: String) {
        when (_state.value.mode) {
            ScannerMode.SCOOTER -> handleScooterScan(code)
            ScannerMode.BATTERY -> handleBatteryScan(code)
        }
    }

    // ============================================================================================
    // САМОКАТ — УМНЫЙ ПОИСК
    // ============================================================================================

    private fun handleScooterScan(code: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isProcessing = true,
                    scannedCode  = code,
                    itemType     = determineItemType(code),
                    error        = null,
                    fleetInfo    = null
                )
            }
            try {
                // 1. Проверяем паспорт СБ
                val passportDoc = firestore.collection("scooter_passports")
                    .document(code).get().await()

                // 2. Параллельно ищем в fleet_vehicles по номеру
                val fleetInfo = searchFleet(code)

                if (passportDoc.exists()) {
                    // Паспорт есть — сразу открываем, флит инфо передаётся через passportState
                    _events.emit(SecurityScannerEvent.PlaySuccessBeep)
                    _state.update { it.copy(isProcessing = false, fleetInfo = fleetInfo) }
                    _events.emit(SecurityScannerEvent.NavigateToPassport(code))
                } else {
                    // Паспорта нет
                    _events.emit(SecurityScannerEvent.PlayWarningBeep)
                    val location = telemetryManager.getCurrentLocation()

                    if (fleetInfo != null) {
                        // Нашли в флите — показываем инфо-диалог с данными флита
                        // перед созданием паспорта
                        _state.update {
                            it.copy(
                                isProcessing        = false,
                                fleetInfo           = fleetInfo,
                                currentLocation     = location,
                                showFleetInfoDialog = true
                            )
                        }
                    } else {
                        // Нет ни паспорта ни в флите — сразу диалог создания
                        _state.update {
                            it.copy(
                                isProcessing        = false,
                                currentLocation     = location,
                                showNewPassportDialog = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка поиска самоката", e)
                _state.update { it.copy(isProcessing = false, error = "Ошибка проверки базы") }
                _events.emit(SecurityScannerEvent.PlayErrorBeep)
            }
        }
    }

    // Поиск в fleet_vehicles по номеру самоката
    private suspend fun searchFleet(number: String): FleetScooterInfo? {
        return try {
            // Ищем по полю number (колонка B в Excel)
            val query = firestore.collection("fleet_vehicles")
                .whereEqualTo("number", number)
                .limit(1)
                .get()
                .await()

            if (query.isEmpty) return null

            val doc = query.documents.first()
            FleetScooterInfo(
                number            = doc.getString("number") ?: number,
                model             = doc.getString("model") ?: "",
                status            = doc.getString("status") ?: "",
                process           = doc.getString("process") ?: "",
                processStage      = doc.getString("processStage") ?: "",
                lat               = doc.getDouble("lat") ?: 0.0,
                lon               = doc.getDouble("lon") ?: 0.0,
                charge            = (doc.getLong("charge") ?: 0L).toInt(),
                heartbeatLag      = doc.getString("heartbeatLag") ?: "",
                statusUpdatedDate = doc.getString("statusUpdatedDate") ?: "",
                statusUpdatedTime = doc.getString("statusUpdatedTime") ?: "",
                vin               = doc.getString("vin") ?: "",
                errorCode         = doc.getString("errorCode") ?: ""
            )
        } catch (e: Exception) {
            Log.w(TAG, "Fleet search failed", e)
            null
        }
    }

    // Пользователь ознакомился с данными флита и хочет создать паспорт
    fun proceedToCreatePassport() {
        _state.update {
            it.copy(
                showFleetInfoDialog   = false,
                showNewPassportDialog = true
            )
        }
    }

    // ============================================================================================
    // АКБ
    // ============================================================================================

    private fun handleBatteryScan(code: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isProcessing            = true,
                    scannedCode             = code,
                    itemType                = determineItemType(code),
                    error                   = null,
                    batteryLookupResult     = null,
                    batteryNotFound         = false,
                    showBatteryResultDialog = false
                )
            }
            try {
                searchBatteryInFirestore(code)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка поиска АКБ", e)
                _state.update { it.copy(isProcessing = false, error = "Ошибка поиска: ${e.message}") }
                _events.emit(SecurityScannerEvent.PlayErrorBeep)
            }
        }
    }

    private suspend fun searchBatteryInFirestore(code: String) {
        // 1. Ищем в storage_pallets
        val palletQuery = firestore.collection("storage_pallets")
            .whereArrayContains("items", code)
            .limit(1).get().await()

        if (!palletQuery.isEmpty) {
            val doc = palletQuery.documents.first()
            val result = BatteryLookupResult(
                code         = code,
                palletNumber = (doc.getLong("palletNumber") ?: 0L).toInt(),
                palletId     = doc.id,
                cellType     = doc.getString("cellType") ?: doc.getString("manufacturer"),
                creatorName  = doc.getString("creatorName")
            )
            _events.emit(SecurityScannerEvent.PlaySuccessBeep)
            _state.update { it.copy(isProcessing = false, batteryLookupResult = result, showBatteryResultDialog = true) }
            return
        }

        // 2. Ищем в batteries
        val batteryDoc = firestore.collection("batteries").document(code).get().await()
        if (batteryDoc.exists()) {
            val palletId = batteryDoc.getString("palletId") ?: ""
            var palletNumber = 0
            var cellType: String? = null
            if (palletId.isNotBlank()) {
                val palletDoc = firestore.collection("storage_pallets").document(palletId).get().await()
                palletNumber = (palletDoc.getLong("palletNumber") ?: 0L).toInt()
                cellType = palletDoc.getString("cellType") ?: palletDoc.getString("manufacturer")
            }
            _events.emit(SecurityScannerEvent.PlaySuccessBeep)
            _state.update {
                it.copy(
                    isProcessing            = false,
                    batteryLookupResult     = BatteryLookupResult(code, palletNumber, palletId, cellType),
                    showBatteryResultDialog = true
                )
            }
            return
        }

        // 3. Нигде не нашли
        _events.emit(SecurityScannerEvent.PlayErrorBeep)
        _state.update { it.copy(isProcessing = false, batteryNotFound = true, showBatteryResultDialog = true) }
    }

    // ============================================================================================
    // СОЗДАНИЕ ПАСПОРТА В ПОЛЕ
    // ============================================================================================

    fun createNewPassport(tags: List<ScooterTag>, notes: String, isLost: Boolean) {
        val code  = _state.value.scannedCode ?: return
        val loc   = _state.value.currentLocation
        val user  = authManager.authState.value
        val type  = _state.value.itemType
        val fleet = _state.value.fleetInfo

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }
            try {
                val now    = System.currentTimeMillis()
                val coords = loc?.let { ScooterCoords(lat = it.latitude, lng = it.longitude) }

                // Добавляем данные флита в заметки если нашли
                val fleetNote = fleet?.let {
                    "Модель: ${it.model}. Заряд: ${it.charge}%. Статус флита: ${it.processStage}."
                } ?: ""
                val fullNote = listOf("Тип: $type", fleetNote, if (notes.isNotBlank()) notes else "")
                    .filter { it.isNotBlank() }.joinToString(" ")

                val passport = ScooterPassport(
                    scooterId   = code,
                    tags        = tags.map { it.key },
                    status      = if (isLost) "lost" else "found",
                    foundAt     = if (loc != null) "GPS: ${"%.6f".format(loc.latitude)}, ${"%.6f".format(loc.longitude)}" else "Неизвестно",
                    coords      = coords,
                    foundBy     = user.userId,
                    foundByName = user.userName,
                    createdAt   = now,
                    updatedAt   = now,
                    notes       = fullNote,
                    historyLog  = listOf(
                        ScooterHistoryEntry(
                            action     = "PASSPORT_CREATED",
                            byUserId   = user.userId ?: "",
                            byUserName = user.userName ?: "СБ",
                            timestamp  = now,
                            note       = "Полевое сканирование ($type)"
                        )
                    )
                )

                firestore.collection("scooter_passports").document(code).set(passport).await()

                if (isLost) {
                    firestore.collection("security_lost_list").document(code).set(
                        mapOf(
                            "scooterId"      to code,
                            "addedAt"        to now,
                            "addedBy"        to user.userId,
                            "addedByName"    to (user.userName ?: "СБ"),
                            "searchAttempts" to emptyList<String>()
                        )
                    ).await()
                }

                _state.update { it.copy(showNewPassportDialog = false, isProcessing = false) }
                _events.emit(SecurityScannerEvent.PlaySuccessBeep)
                _events.emit(SecurityScannerEvent.NavigateToPassport(code))

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка сохранения", e)
                _state.update { it.copy(isProcessing = false, error = "Ошибка сохранения: ${e.message}") }
            }
        }
    }

    // ============================================================================================
    // ЗАКРЫТИЕ ДИАЛОГОВ
    // ============================================================================================

    fun dismissDialog() {
        _state.update {
            it.copy(
                showNewPassportDialog    = false,
                showFleetInfoDialog      = false,
                showBatteryResultDialog  = false,
                batteryNotFound          = false,
                batteryLookupResult      = null,
                fleetInfo                = null
            )
        }
        lastScannedCode = null
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}