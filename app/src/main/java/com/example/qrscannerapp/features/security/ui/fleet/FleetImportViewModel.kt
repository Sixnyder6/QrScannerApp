package com.example.qrscannerapp.features.security.ui.fleet

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.google.firebase.firestore.FirebaseFirestoreException
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
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToInt

// ============================================================================================
// РЕЖИМ ИМПОРТА
// ============================================================================================

enum class ImportMode {
    /** Полный каталог: 500+ строк — пишем fleet_catalog + fleet_vehicles */
    FULL_CATALOG,
    /** Оперативный: до 500 строк — пишем только fleet_vehicles, каталог не трогаем */
    OPERATIONAL
}

fun determineImportMode(count: Int): ImportMode =
    if (count >= 500) ImportMode.FULL_CATALOG else ImportMode.OPERATIONAL

// Кулдаун зависит от режима
fun cooldownMs(mode: ImportMode): Long = when (mode) {
    ImportMode.FULL_CATALOG -> 6 * 3_600_000L   // 6 часов для полного
    ImportMode.OPERATIONAL  -> 15 * 60_000L      // 15 минут для оперативного
}

// ============================================================================================
// ХЕЛПЕРЫ ЧТЕНИЯ ЯЧЕЕК
// ============================================================================================

private fun Row.safeString(col: Int): String {
    if (col < 0 || col >= this.cellCount) return ""
    return try {
        val cell = getCell(col) ?: return ""
        val text = cell.text
        if (!text.isNullOrBlank()) return text.trim()
        when (val value = cell.value) {
            is Number -> { val d = value.toDouble(); if (d % 1.0 == 0.0) d.toLong().toString() else d.toString() }
            else -> value?.toString()?.trim() ?: ""
        }
    } catch (e: Exception) { "" }
}

private fun Row.s(col: Int): String = safeString(col)
private fun Row.cellAsIdString(col: Int): String = safeString(col)

private fun Row.n(col: Int): Double {
    if (col < 0 || col >= this.cellCount) return 0.0
    return try {
        val cell = getCell(col) ?: return 0.0
        when (val v = cell.value) {
            is Number -> v.toDouble()
            is String -> v.replace(",", ".").replace(Regex("[^0-9.-]"), "").toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    } catch (e: Exception) { 0.0 }
}

private fun Row.i(col: Int): Int {
    val d = n(col); if (d.isNaN() || d.isInfinite()) return 0
    return d.roundToInt().coerceIn(Int.MIN_VALUE, Int.MAX_VALUE)
}

private fun Row.nAt(cols: IntArray, idx: Int): Double = n(cols[idx])
private fun Row.sAt(cols: IntArray, idx: Int): String = s(cols[idx])
private fun Row.iAt(cols: IntArray, idx: Int): Int = i(cols[idx])

private fun defaultFleetCols(): IntArray =
    intArrayOf(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27)

private fun shiftFleetCols(delta: Int): IntArray {
    val base = defaultFleetCols(); return IntArray(base.size) { base[it] + delta }
}

private fun normalizeFleetHeader(raw: String): String =
    raw.trim().lowercase().replace("ё","е").replace(Regex("\\s+")," ")

private fun isFleetIdHeaderCell(raw: String): Boolean {
    val n = normalizeFleetHeader(raw)
    if (n.isEmpty() || n.length > 80) return false
    if (n == "id" || n == "uuid" || n == "uid") return true
    if ("идентификатор" in n || "идентиф" in n) return true
    if (n.contains("vehicle") && n.contains("id")) return true
    if (n.contains("scooter") && n.contains("id")) return true
    if (n.matches(Regex("^id\\s*#?\\d*$")) || n.matches(Regex("^код\\s*#?\\d*$"))) return true
    return false
}

private fun detectFleetIdColumn(header: Row): Int? {
    for (col in 0 until min(15, header.cellCount)) {
        if (isFleetIdHeaderCell(header.s(col))) { Log.d("FleetImport","✓ ID col $col"); return col }
    }
    return null
}

private fun firestoreFleetDocId(raw: String): String = raw.trim().replace("/","_")

private fun humanizeImportError(e: Throwable): String {
    if (e is FirebaseFirestoreException) return when (e.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED  -> "Нет прав на запись."
        FirebaseFirestoreException.Code.UNAVAILABLE        -> "Сеть недоступна."
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED  -> "Превышено время ожидания."
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> "Слишком много запросов."
        else -> e.message?.takeIf { it.isNotBlank() } ?: "Ошибка Firestore"
    }
    val msg = e.message.orEmpty()
    if (msg.contains("XML",true) || msg.contains("StAX",true)) return "Неверный формат. Сохраните как .xlsx"
    return msg.ifBlank { "Неизвестная ошибка" }
}

// ============================================================================================
// ФИЛЬТР ИСТОРИИ — только СБ / Скаут / пустой процесс
// ============================================================================================

private fun shouldTrackHistory(process: String): Boolean {
    val p = process.trim()
    return p.isBlank() || p.equals("СБ", ignoreCase = true) || p.equals("Скаут", ignoreCase = true)
}

// ============================================================================================
// МОДЕЛИ
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

/** Каталожная запись — только постоянные поля */
data class FleetCatalogEntry(
    val id: String,
    val number: String,
    val model: String,
    val vin: String,
    val imei: String,
    val year: String
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

private fun FleetCatalogEntry.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "number" to number, "model" to model,
    "vin" to vin, "imei" to imei, "year" to year
)

private fun FleetVehicle.toHistoryMap(importedAt: Long, importDate: String): Map<String, Any?> = mapOf(
    "vehicleId" to id, "number" to number, "model" to model,
    "process" to process, "processStage" to processStage,
    "lat" to lat, "lon" to lon, "charge" to charge,
    "heartbeatLag" to heartbeatLag, "status" to status,
    "errorCode" to errorCode, "importedAt" to importedAt, "importDate" to importDate
)

// ============================================================================================
// СТАДИИ
// ============================================================================================

sealed class ImportStage(val message: String) {
    object Idle      : ImportStage("Готов к загрузке")
    object Opening   : ImportStage("Открываем файл...")
    object Reading   : ImportStage("Читаем структуру...")
    class  Parsing(val count: Int) : ImportStage("Анализируем... $count строк")
    class  Saving(val batch: Int, val total: Int) : ImportStage("Сохраняем... $batch/$total")
    object Catalog   : ImportStage("Обновляем каталог самокатов...")
    object History   : ImportStage("Записываем историю СБ/Скаут...")
    object Finishing : ImportStage("Финализируем...")
    class  Done(val count: Int, val sec: Long, val historyCount: Int = 0, val mode: ImportMode = ImportMode.FULL_CATALOG)
        : ImportStage("Готово: $count самокатов за ${sec}с")
    class  Error(val reason: String) : ImportStage("Ошибка: $reason")
}

data class FleetImportUiState(
    val stage: ImportStage = ImportStage.Idle,
    val progress: Float = 0f,
    val lastImportedAt: Long? = null,
    val lastImportCount: Int = 0,
    val lastImportMode: ImportMode = ImportMode.FULL_CATALOG,
    val canImport: Boolean = true,
    // Статистика каталога
    val catalogCount: Int = 0
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
                val doc = firestore.collection("fleet_import_meta").document("last").get().await()
                if (doc.exists()) {
                    val ts       = doc.getLong("importedAt") ?: 0L
                    val count    = (doc.getLong("count") ?: 0L).toInt()
                    val modeStr  = doc.getString("mode") ?: "FULL_CATALOG"
                    val mode     = try { ImportMode.valueOf(modeStr) } catch (e: Exception) { ImportMode.FULL_CATALOG }
                    val cooldown = cooldownMs(mode)
                    val catalog  = (doc.getLong("catalogCount") ?: 0L).toInt()
                    _uiState.update {
                        it.copy(
                            lastImportedAt  = ts,
                            lastImportCount = count,
                            lastImportMode  = mode,
                            catalogCount    = catalog,
                            canImport       = (System.currentTimeMillis() - ts) > cooldown
                        )
                    }
                }
            } catch (e: Exception) { Log.w(TAG, "meta load failed", e) }
        }
    }

    fun importFromUri(uri: Uri) {
        val cur = _uiState.value.stage
        if (cur !is ImportStage.Idle && cur !is ImportStage.Done && cur !is ImportStage.Error) return

        viewModelScope.launch {
            val t0 = System.currentTimeMillis()
            try {
                setStage(ImportStage.Opening, 0.02f)
                delay(200)
                setStage(ImportStage.Reading, 0.05f)

                val vehicles          = mutableListOf<FleetVehicle>()
                var rowsProcessed     = 0
                var rowsSkipped       = 0
                val firstErrors       = mutableListOf<String>()
                var idColumnIndex     = 0

                withContext(Dispatchers.IO) {
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
                                    val it = rows.iterator()
                                    if (!it.hasNext()) throw Exception("Пустой лист")

                                    var cols = defaultFleetCols()
                                    val headerRow = it.next()
                                    val idCol = detectFleetIdColumn(headerRow)
                                    idColumnIndex = idCol ?: 0
                                    cols = if (idCol != null) shiftFleetCols(idCol) else defaultFleetCols()

                                    fun processRow(row: Row) {
                                        val rowNum = ++rowsProcessed
                                        try {
                                            val id = row.cellAsIdString(cols[0])
                                            if (id.isBlank() || id.length < 3) { rowsSkipped++; return }

                                            vehicles.add(FleetVehicle(
                                                id                = id,
                                                number            = row.sAt(cols, 1),
                                                model             = row.sAt(cols, 2),
                                                iotType           = row.sAt(cols, 3),
                                                vin               = row.sAt(cols, 4),
                                                imei              = row.sAt(cols, 5),
                                                park              = row.sAt(cols, 6),
                                                region            = row.sAt(cols, 7),
                                                year              = row.sAt(cols, 8),
                                                status            = row.sAt(cols, 9),
                                                statusUpdatedDate = row.sAt(cols, 10),
                                                statusUpdatedTime = row.sAt(cols, 11),
                                                process           = row.sAt(cols, 12),
                                                processStage      = row.sAt(cols, 13),
                                                lat               = row.nAt(cols, 14),
                                                lon               = row.nAt(cols, 15),
                                                charge            = row.iAt(cols, 16),
                                                mileage           = row.nAt(cols, 17),
                                                firmwareIot       = row.sAt(cols, 18),
                                                firmwareMcu       = row.sAt(cols, 19),
                                                firmwareEcu       = row.sAt(cols, 20),
                                                firmwareGps       = row.sAt(cols, 21),
                                                iccid             = row.sAt(cols, 22),
                                                iotSerial         = row.sAt(cols, 23),
                                                errorCode         = row.sAt(cols, 24),
                                                isUnlocked        = row.nAt(cols, 25) > 0,
                                                isDeckOpen        = row.nAt(cols, 26) > 0,
                                                heartbeatLag      = row.sAt(cols, 27),
                                                importedAt        = System.currentTimeMillis()
                                            ))
                                        } catch (e: Exception) {
                                            rowsSkipped++
                                            if (rowNum <= 5) firstErrors.add("Row $rowNum: ${e.message}")
                                        }
                                        if (rowNum % 500 == 0) {
                                            val prog = 0.05f + (rowNum.toFloat() / 20_000f).coerceAtMost(0.50f)
                                            viewModelScope.launch(Dispatchers.Main) { setStage(ImportStage.Parsing(vehicles.size), prog) }
                                        }
                                    }

                                    while (it.hasNext()) processRow(it.next())
                                }
                            }
                        }
                    } finally { tmp.delete() }
                }

                if (vehicles.isEmpty()) throw Exception(
                    "Не найдено валидных записей.\nОбработано: $rowsProcessed\nПропущено: $rowsSkipped"
                            + if (firstErrors.isNotEmpty()) "\n${firstErrors.take(3).joinToString("\n")}" else ""
                )

                // ── Определяем режим по количеству строк ─────────────────
                val mode = determineImportMode(vehicles.size)
                Log.d(TAG, "Import mode: $mode (${vehicles.size} vehicles)")

                // ── 1. Сохраняем fleet_vehicles (всегда) ─────────────────
                setStage(ImportStage.Parsing(vehicles.size), 0.55f)
                val batches = vehicles.chunked(400)
                batches.forEachIndexed { idx, batch ->
                    val total = batches.size.coerceAtLeast(1)
                    setStage(ImportStage.Saving(idx + 1, total), 0.55f + (idx.toFloat() / total) * 0.20f)
                    withContext(Dispatchers.IO) {
                        val fb = firestore.batch()
                        batch.forEach { v ->
                            fb.set(firestore.collection("fleet_vehicles").document(firestoreFleetDocId(v.id)), v.toMap())
                        }
                        fb.commit().await()
                    }
                }

                // ── 2. Если полный каталог — пишем fleet_catalog ─────────
                if (mode == ImportMode.FULL_CATALOG) {
                    setStage(ImportStage.Catalog, 0.76f)
                    Log.d(TAG, "Writing catalog: ${vehicles.size} entries")
                    val catalogBatches = vehicles.chunked(400)
                    catalogBatches.forEach { batch ->
                        withContext(Dispatchers.IO) {
                            val fb = firestore.batch()
                            batch.forEach { v ->
                                val entry = FleetCatalogEntry(
                                    id     = v.id,
                                    number = v.number,
                                    model  = v.model,
                                    vin    = v.vin,
                                    imei   = v.imei,
                                    year   = v.year
                                )
                                fb.set(
                                    firestore.collection("fleet_catalog").document(firestoreFleetDocId(v.id)),
                                    entry.toMap()
                                )
                            }
                            fb.commit().await()
                        }
                    }
                    Log.d(TAG, "Catalog written: ${vehicles.size} entries")
                }

                // ── 3. История для СБ / Скаут / пустой ───────────────────
                val now        = System.currentTimeMillis()
                val importDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
                val tracked    = vehicles.filter { shouldTrackHistory(it.process) && (it.lat != 0.0 || it.lon != 0.0) }

                if (tracked.isNotEmpty()) {
                    setStage(ImportStage.History, 0.88f)
                    Log.d(TAG, "Writing history: ${tracked.size} tracked vehicles")
                    tracked.chunked(400).forEach { batch ->
                        withContext(Dispatchers.IO) {
                            val fb = firestore.batch()
                            batch.forEach { v ->
                                // Один документ в день — предотвращает дублирование
                                val docId = "${firestoreFleetDocId(v.id)}_${importDate}"
                                fb.set(firestore.collection("fleet_history").document(docId), v.toHistoryMap(now, importDate))
                            }
                            fb.commit().await()
                        }
                    }
                }

                // ── 4. Финализация ────────────────────────────────────────
                setStage(ImportStage.Finishing, 0.97f)
                val sec = (System.currentTimeMillis() - t0) / 1000L

                withContext(Dispatchers.IO) {
                    firestore.collection("fleet_import_meta").document("last").set(
                        mapOf(
                            "importedAt"   to now,
                            "count"        to vehicles.size,
                            "historyCount" to tracked.size,
                            "catalogCount" to if (mode == ImportMode.FULL_CATALOG) vehicles.size else _uiState.value.catalogCount,
                            "importDate"   to importDate,
                            "importedBy"   to (authManager.authState.value.userName ?: "СБ"),
                            "durationSec"  to sec,
                            "mode"         to mode.name
                        )
                    ).await()
                }

                setStage(ImportStage.Done(vehicles.size, sec, tracked.size, mode), 1f)
                _uiState.update {
                    it.copy(
                        lastImportedAt  = now,
                        lastImportCount = vehicles.size,
                        lastImportMode  = mode,
                        catalogCount    = if (mode == ImportMode.FULL_CATALOG) vehicles.size else it.catalogCount,
                        canImport       = false
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "import failed", e)
                setStage(ImportStage.Error(humanizeImportError(e)), 0f)
            }
        }
    }

    fun resetToIdle() {
        // Сбрасываем cooldown правильно с учётом режима
        val ts   = _uiState.value.lastImportedAt ?: 0L
        val mode = _uiState.value.lastImportMode
        _uiState.update {
            it.copy(
                stage     = ImportStage.Idle,
                progress  = 0f,
                canImport = (System.currentTimeMillis() - ts) > cooldownMs(mode)
            )
        }
    }

    // ============================================================================================
    // ИСТОРИЯ И ПОИСК
    // ============================================================================================

    suspend fun getVehicleHistory(vehicleId: String): List<Map<String, Any>> {
        return try {
            firestore.collection("fleet_history")
                .whereEqualTo("vehicleId", vehicleId)
                .orderBy("importedAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get().await().documents.mapNotNull { it.data }
        } catch (e: Exception) { Log.e(TAG, "getVehicleHistory error", e); emptyList() }
    }

    suspend fun getVehicleHistoryByNumber(number: String): List<Map<String, Any>> {
        return try {
            firestore.collection("fleet_history")
                .whereEqualTo("number", number)
                .orderBy("importedAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get().await().documents.mapNotNull { it.data }
        } catch (e: Exception) { Log.e(TAG, "getVehicleHistoryByNumber error", e); emptyList() }
    }

    suspend fun findVehicleByNumber(number: String): FleetVehicle? {
        return try {
            val q = firestore.collection("fleet_vehicles")
                .whereEqualTo("number", number).limit(1).get().await()
            if (q.isEmpty) return null
            val d = q.documents.first()
            FleetVehicle(
                id = d.getString("id") ?: d.id, number = d.getString("number") ?: "",
                model = d.getString("model") ?: "", status = d.getString("status") ?: "",
                process = d.getString("process") ?: "", processStage = d.getString("processStage") ?: "",
                lat = d.getDouble("lat") ?: 0.0, lon = d.getDouble("lon") ?: 0.0,
                charge = (d.getLong("charge") ?: 0L).toInt(), mileage = d.getDouble("mileage") ?: 0.0,
                errorCode = d.getString("errorCode") ?: "", isDeckOpen = d.getBoolean("isDeckOpen") ?: false,
                heartbeatLag = d.getString("heartbeatLag") ?: "",
                statusUpdatedDate = d.getString("statusUpdatedDate") ?: "",
                statusUpdatedTime = d.getString("statusUpdatedTime") ?: ""
            )
        } catch (e: Exception) { Log.e(TAG, "findVehicle error", e); null }
    }

    private fun setStage(stage: ImportStage, progress: Float) {
        _uiState.update { it.copy(stage = stage, progress = progress) }
    }
}