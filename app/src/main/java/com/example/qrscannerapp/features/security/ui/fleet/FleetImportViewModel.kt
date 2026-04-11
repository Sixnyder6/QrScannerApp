package com.example.qrscannerapp.features.security.ui.fleet

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.dhatim.fastexcel.reader.Row
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import kotlin.math.roundToInt

// ============================================================================================
// ХЕЛПЕРЫ ДЛЯ ЧТЕНИЯ ЯЧЕЕК (top-level — видны везде)
// ============================================================================================

private fun Row.s(col: Int): String =
    getCellAsString(col).orElse("").trim()

private fun Row.n(col: Int): Double =
    getCellAsNumber(col).map { it.toDouble() }.orElse(0.0)

private fun Row.i(col: Int): Int {
    val d = n(col)
    if (d.isNaN() || d.isInfinite()) return 0
    return d.roundToInt().coerceIn(Int.MIN_VALUE, Int.MAX_VALUE)
}

/** Firestore document IDs cannot contain `/`. Поле `id` в данных остаётся исходным. */
private fun firestoreFleetDocId(raw: String): String = raw.trim().replace("/", "_")

private fun humanizeImportError(e: Throwable): String {
    if (e is FirebaseFirestoreException) {
        return when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Нет прав на запись в базу. Проверьте правила Firestore."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Сеть недоступна. Повторите позже."
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "Превышено время ожидания сервера."
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                "Слишком много запросов. Подождите и повторите."
            else -> e.message?.takeIf { it.isNotBlank() } ?: "Ошибка Firestore: ${e.code}"
        }
    }
    val msg = e.message.orEmpty()
    if (msg.contains("XML", ignoreCase = true) ||
        msg.contains("StAX", ignoreCase = true) ||
        msg.contains("AsyncXML", ignoreCase = true) ||
        msg.contains("OPC", ignoreCase = true)
    ) {
        return "Не удалось разобрать Excel. Сохраните файл как .xlsx (не .xls) и откройте снова."
    }
    return msg.ifBlank { "Неизвестная ошибка" }
}

// ============================================================================================
// МОДЕЛЬ ДАННЫХ
// ============================================================================================

data class FleetVehicle(
    val id: String = "",
    val number: String = "",
    val model: String = "",
    val iotType: String = "",
    val vin: String = "",
    val imei: String = "",
    val park: String = "",
    val region: String = "",
    val year: String = "",
    val status: String = "",
    val statusUpdatedDate: String = "",
    val statusUpdatedTime: String = "",
    val process: String = "",
    val processStage: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val charge: Int = 0,
    val mileage: Double = 0.0,
    val firmwareIot: String = "",
    val firmwareMcu: String = "",
    val firmwareEcu: String = "",
    val firmwareGps: String = "",
    val iccid: String = "",
    val iotSerial: String = "",
    val errorCode: String = "",
    val isUnlocked: Boolean = false,
    val isDeckOpen: Boolean = false,
    val heartbeatLag: String = "",
    val importedAt: Long = System.currentTimeMillis()
)

private fun FleetVehicle.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "number" to number, "model" to model, "iotType" to iotType,
    "vin" to vin, "imei" to imei, "park" to park, "region" to region,
    "year" to year, "status" to status,
    "statusUpdatedDate" to statusUpdatedDate, "statusUpdatedTime" to statusUpdatedTime,
    "process" to process, "processStage" to processStage,
    "lat" to lat, "lon" to lon, "charge" to charge, "mileage" to mileage,
    "firmwareIot" to firmwareIot, "firmwareMcu" to firmwareMcu,
    "firmwareEcu" to firmwareEcu, "firmwareGps" to firmwareGps,
    "iccid" to iccid, "iotSerial" to iotSerial, "errorCode" to errorCode,
    "isUnlocked" to isUnlocked, "isDeckOpen" to isDeckOpen,
    "heartbeatLag" to heartbeatLag, "importedAt" to importedAt
)

// ============================================================================================
// СТАДИИ ИМПОРТА
// ============================================================================================

sealed class ImportStage(val message: String) {
    object Idle      : ImportStage("Готов к загрузке")
    object Opening   : ImportStage("Открываем файл...")
    object Reading   : ImportStage("Читаем структуру...")
    class  Parsing(val count: Int) : ImportStage("Анализируем данные... $count строк")
    class  Saving(val batch: Int, val total: Int) : ImportStage("Сохраняем... пакет $batch / $total")
    object Finishing : ImportStage("Финализируем...")
    class  Done(val count: Int, val sec: Long) : ImportStage("Готово: $count самокатов за ${sec}с")
    class  Error(val reason: String) : ImportStage("Ошибка: $reason")
}

data class FleetImportUiState(
    val stage: ImportStage = ImportStage.Idle,
    val progress: Float = 0f,
    val lastImportedAt: Long? = null,
    val lastImportCount: Int = 0,
    val canImport: Boolean = true
)

// ============================================================================================
// VIEWMODEL
// ============================================================================================

@HiltViewModel
class FleetImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager
) : ViewModel() {

    private val firestore = Firebase.firestore
    private val TAG = "FleetImport"

    private val _uiState = MutableStateFlow(FleetImportUiState())
    val uiState: StateFlow<FleetImportUiState> = _uiState.asStateFlow()

    init { loadMeta() }

    private fun loadMeta() {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("fleet_import_meta")
                    .document("last").get().await()
                if (doc.exists()) {
                    val ts    = doc.getLong("importedAt") ?: 0L
                    val count = (doc.getLong("count") ?: 0L).toInt()
                    _uiState.update {
                        it.copy(
                            lastImportedAt  = ts,
                            lastImportCount = count,
                            canImport       = (System.currentTimeMillis() - ts) > 6 * 3_600_000L
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "meta load failed", e)
            }
        }
    }

    // ============================================================================================
    // ИМПОРТ
    // ============================================================================================

    fun importFromUri(uri: Uri) {
        val cur = _uiState.value.stage
        if (cur !is ImportStage.Idle &&
            cur !is ImportStage.Done &&
            cur !is ImportStage.Error) return

        viewModelScope.launch {
            val t0 = System.currentTimeMillis()
            try {
                // 1. Открываем
                setStage(ImportStage.Opening, 0.02f)
                delay(200)

                // 2. Читаем через FastExcel — streaming, ~5MB RAM
                setStage(ImportStage.Reading, 0.05f)

                val vehicles = mutableListOf<FleetVehicle>()

                withContext(Dispatchers.IO) {
                    // content:// часто отдаёт одноразовый поток — копируем в файл, иначе ZIP/XLSX может
                    // «ломаться» ближе к концу или при закрытии (прогресс ~70% = конец чтения листа).
                    val tmp = File.createTempFile("fleet_import_", ".xlsx", context.cacheDir)
                    try {
                        context.contentResolver.openInputStream(uri).use { src ->
                            if (src == null) throw Exception("Не удалось открыть файл")
                            tmp.outputStream().use { dst -> src.copyTo(dst) }
                        }

                        FileInputStream(tmp).use { fis ->
                            ReadableWorkbook(fis).use { wb ->
                                val sheet = wb.firstSheet
                                sheet.openStream().use { rows ->
                                    var isHeader = true
                                    var rowNum   = 0

                                    rows.forEach { row ->
                                        if (isHeader) {
                                            isHeader = false
                                            return@forEach
                                        }

                                        rowNum++
                                        try {
                                            val id = row.s(0)
                                            if (id.length < 8) return@forEach

                                            vehicles.add(
                                                FleetVehicle(
                                                    id                = id,
                                                    number            = row.s(1),
                                                    model             = row.s(2),
                                                    iotType           = row.s(3),
                                                    vin               = row.s(4),
                                                    imei              = row.s(5),
                                                    park              = row.s(6),
                                                    region            = row.s(7),
                                                    year              = row.s(8),
                                                    status            = row.s(9),
                                                    statusUpdatedDate = row.s(10),
                                                    statusUpdatedTime = row.s(11),
                                                    process           = row.s(12),
                                                    processStage      = row.s(13),
                                                    lat               = row.n(14),
                                                    lon               = row.n(15),
                                                    charge            = row.i(16),
                                                    mileage           = row.n(17),
                                                    firmwareIot       = row.s(18),
                                                    firmwareMcu       = row.s(19),
                                                    firmwareEcu       = row.s(20),
                                                    firmwareGps       = row.s(21),
                                                    iccid             = row.s(22),
                                                    iotSerial         = row.s(23),
                                                    errorCode         = row.s(24),
                                                    isUnlocked        = row.n(25) > 0,
                                                    isDeckOpen        = row.n(26) > 0,
                                                    heartbeatLag      = row.s(27),
                                                    importedAt        = System.currentTimeMillis()
                                                )
                                            )
                                        } catch (e: Exception) {
                                            Log.w(TAG, "skip row $rowNum: ${e.message}", e)
                                        }

                                        if (rowNum % 500 == 0) {
                                            val prog =
                                                0.05f + (rowNum.toFloat() / 20_000f).coerceAtMost(0.65f)
                                            viewModelScope.launch(Dispatchers.Main) {
                                                setStage(ImportStage.Parsing(vehicles.size), prog)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        if (!tmp.delete()) Log.w(TAG, "temp xlsx not deleted: ${tmp.absolutePath}")
                    }
                }

                if (vehicles.isEmpty()) {
                    throw Exception("Данные не распознаны. Проверьте формат файла.")
                }

                // Конец разбора листа — явно показываем 70%, дальше только Firestore.
                setStage(ImportStage.Parsing(vehicles.size), 0.70f)

                // 3. Сохраняем батчами по 400
                val batches = vehicles.chunked(400)
                batches.forEachIndexed { idx, batch ->
                    val total = batches.size.coerceAtLeast(1)
                    setStage(
                        ImportStage.Saving(idx + 1, total),
                        0.70f + (idx.toFloat() / total) * 0.27f
                    )
                    withContext(Dispatchers.IO) {
                        val fb = firestore.batch()
                        batch.forEach { v ->
                            fb.set(
                                firestore.collection("fleet_vehicles")
                                    .document(firestoreFleetDocId(v.id)),
                                v.toMap()
                            )
                        }
                        fb.commit().await()
                    }
                }

                // 4. Финал
                setStage(ImportStage.Finishing, 0.98f)
                val sec = (System.currentTimeMillis() - t0) / 1000L

                withContext(Dispatchers.IO) {
                    firestore.collection("fleet_import_meta").document("last").set(
                        mapOf(
                            "importedAt"  to System.currentTimeMillis(),
                            "count"       to vehicles.size,
                            "importedBy"  to (authManager.authState.value.userName ?: "СБ"),
                            "durationSec" to sec
                        )
                    ).await()
                }

                setStage(ImportStage.Done(vehicles.size, sec), 1f)
                _uiState.update {
                    it.copy(
                        lastImportedAt  = System.currentTimeMillis(),
                        lastImportCount = vehicles.size,
                        canImport       = false
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "import failed", e)
                setStage(ImportStage.Error(humanizeImportError(e)), 0f)
            }
        }
    }

    // ============================================================================================
    // СБРОС
    // ============================================================================================

    fun resetToIdle() {
        _uiState.update { it.copy(stage = ImportStage.Idle, progress = 0f) }
    }

    // ============================================================================================
    // ПОИСК ПО НОМЕРУ (для сканера)
    // ============================================================================================

    suspend fun findVehicleByNumber(number: String): FleetVehicle? {
        return try {
            val q = firestore.collection("fleet_vehicles")
                .whereEqualTo("number", number)
                .limit(1).get().await()
            if (q.isEmpty) return null
            val d = q.documents.first()
            FleetVehicle(
                id                = d.getString("id") ?: d.id,
                number            = d.getString("number") ?: "",
                model             = d.getString("model") ?: "",
                status            = d.getString("status") ?: "",
                process           = d.getString("process") ?: "",
                processStage      = d.getString("processStage") ?: "",
                lat               = d.getDouble("lat") ?: 0.0,
                lon               = d.getDouble("lon") ?: 0.0,
                charge            = (d.getLong("charge") ?: 0L).toInt(),
                mileage           = d.getDouble("mileage") ?: 0.0,
                errorCode         = d.getString("errorCode") ?: "",
                isDeckOpen        = d.getBoolean("isDeckOpen") ?: false,
                heartbeatLag      = d.getString("heartbeatLag") ?: "",
                statusUpdatedDate = d.getString("statusUpdatedDate") ?: "",
                statusUpdatedTime = d.getString("statusUpdatedTime") ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "findVehicle error", e)
            null
        }
    }

    private fun setStage(stage: ImportStage, progress: Float) {
        _uiState.update { it.copy(stage = stage, progress = progress) }
    }
}