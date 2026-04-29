package com.example.qrscannerapp.features.street_doctor.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

// ── Колонки Excel ─────────────────────────────────────────────────────────────
private object ExcelColumns {
    const val SCOOTER_NUMBER = 1
    const val MODEL          = 2
    const val PROCESS        = 12
    const val PROCESS_STAGE  = 13
    const val LAT            = 14
    const val LON            = 15
    const val CHARGE         = 16
}

private const val BATCH_SIZE         = 400
private const val TAG                = "FieldRepairImport"
private const val MAX_SAFE_ROWS      = 500   // больше — это скорее всего не полевой ремонт
private const val MAX_ALLOWED_ROWS   = 2000  // жёсткий лимит

// ── Модели ────────────────────────────────────────────────────────────────────
data class TechnicianInfo(
    val id: String = "",
    val name: String = "",
    val warehouseId: String = ""
)

data class AssignmentEntry(
    val id: String = UUID.randomUUID().toString(),
    val technician: TechnicianInfo,
    val uri: Uri,
    val fileName: String,
    val scooterCount: Int = 0,
    val isParsed: Boolean = false,
    val tasks: List<FieldRepairTask> = emptyList()
)

data class ActiveSessionInfo(
    val id: String,
    val adminName: String,
    val date: String,
    val totalCount: Int,
    val doneCount: Int,
    val technicNames: List<String> = emptyList()
)

sealed class FieldImportStage(val message: String) {
    object Idle      : FieldImportStage("Готов к загрузке")
    object Opening   : FieldImportStage("Открываем файл...")
    object Reading   : FieldImportStage("Читаем строки...")
    class  Parsing(val count: Int) : FieldImportStage("Анализируем... $count самокатов")
    class  Saving(val batch: Int, val total: Int) : FieldImportStage("Сохраняем... $batch/$total")
    object Finishing : FieldImportStage("Финализируем...")
    class  Done(val count: Int, val sec: Long) : FieldImportStage("Готово: $count заданий за ${sec}с")
    class  Error(val reason: String) : FieldImportStage("Ошибка: $reason")
}

data class FieldRepairTask(
    val id: String = "",
    val sessionId: String = "",
    val scooterNumber: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val charge: Int = 0,
    val model: String = "",
    val process: String = "",
    val processStage: String = "",
    val status: String = "new",
    val assignedToId: String = "",
    val assignedToName: String = "",
    val isFromList: Boolean = true,
    val repairTypes: List<String> = emptyList(),
    val batteryPctReal: Int = 0,
    val finalStatus: String = "",
    val completedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

data class FieldRepairSession(
    val id: String = "",
    val adminId: String = "",
    val adminName: String = "",
    val date: String = "",
    val totalCount: Int = 0,
    val status: String = "active",
    val createdAt: Long = System.currentTimeMillis()
)

// ── Предупреждения ────────────────────────────────────────────────────────────
data class ImportWarning(
    val type: WarningType,
    val message: String,
    val isBlocker: Boolean = false  // true = нельзя отправить без подтверждения
)

enum class WarningType {
    ACTIVE_SESSION,      // уже есть активная сессия
    TOO_MANY_ROWS,       // файл подозрительно большой
    DUPLICATE_SCOOTERS,  // один самокат у двух техников
    PARTIAL_UPLOAD,      // предыдущая загрузка оборвалась
    FIRED_TECHNICIAN     // техник неактивен
}

// ── UI State ──────────────────────────────────────────────────────────────────
data class FieldRepairImportUiState(
    val technicians: List<TechnicianInfo> = emptyList(),
    val isLoadingTechnicians: Boolean = true,
    val selectedTechnician: TechnicianInfo? = null,
    val queue: List<AssignmentEntry> = emptyList(),
    val stage: FieldImportStage = FieldImportStage.Idle,
    val progress: Float = 0f,
    val currentSendingName: String = "",
    val lastImportedAt: Long? = null,
    val lastImportCount: Int = 0,
    val lastSessionId: String? = null,
    // Активная сессия
    val activeSession: ActiveSessionInfo? = null,
    val isCheckingSession: Boolean = true,
    // Предупреждения
    val warnings: List<ImportWarning> = emptyList(),
    val showWarningDialog: Boolean = false,
    val pendingConfirmAction: (() -> Unit)? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────
@HiltViewModel
class FieldRepairImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager
) : ViewModel() {

    private val firestore = Firebase.firestore
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.forLanguageTag("ru"))
    private val displayDateFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru"))

    private val _uiState = MutableStateFlow(FieldRepairImportUiState())
    val uiState: StateFlow<FieldRepairImportUiState> = _uiState.asStateFlow()

    private var sendJob: Job? = null

    init {
        loadTechnicians()
        loadHistory()
        checkActiveSession()
    }

    // ── Загрузка техников ─────────────────────────────────────────────────────
    private fun loadTechnicians() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("internal_users")
                    .whereEqualTo("role", "technic")
                    .get().await()

                val technicians = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("displayName") ?: return@mapNotNull null
                    TechnicianInfo(
                        id          = doc.id,
                        name        = name,
                        warehouseId = doc.getString("warehouseId") ?: ""
                    )
                }.sortedBy { it.name }

                _uiState.update { it.copy(technicians = technicians, isLoadingTechnicians = false) }
            } catch (e: Exception) {
                Log.e(TAG, "loadTechnicians error", e)
                _uiState.update { it.copy(isLoadingTechnicians = false) }
            }
        }
    }

    // ── История ───────────────────────────────────────────────────────────────
    private fun loadHistory() {
        viewModelScope.launch {
            try {
                val savedTs    = ImportPreferencesManager.getLastImportedAt(context).first()
                val savedCount = ImportPreferencesManager.getLastImportCount(context).first()
                val savedSid   = ImportPreferencesManager.getLastSessionId(context).first()
                if (savedTs != null && savedCount > 0) {
                    _uiState.update {
                        it.copy(
                            lastImportedAt  = savedTs,
                            lastImportCount = savedCount,
                            lastSessionId   = savedSid
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadHistory error", e)
            }
        }
    }

    // ── Проверка активной сессии ──────────────────────────────────────────────
    private fun checkActiveSession() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("field_repair_sessions")
                    .whereEqualTo("status", "active")
                    .get().await()

                if (snapshot.isEmpty) {
                    _uiState.update { it.copy(isCheckingSession = false, activeSession = null) }
                    return@launch
                }

                // Если несколько активных — это проблема (предыдущий баг)
                if (snapshot.documents.size > 1) {
                    Log.w(TAG, "Multiple active sessions found: ${snapshot.documents.size}")
                }

                val doc = snapshot.documents.first()
                val technicIds = (doc.get("technicIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                // Загружаем имена техников
                val techNames = mutableListOf<String>()
                technicIds.forEach { techId ->
                    try {
                        val techDoc = firestore.collection("internal_users").document(techId).get().await()
                        techDoc.getString("displayName")?.let { techNames.add(it) }
                    } catch (_: Exception) {}
                }

                val activeSession = ActiveSessionInfo(
                    id          = doc.id,
                    adminName   = doc.getString("adminName") ?: "Администратор",
                    date        = doc.getString("date") ?: "",
                    totalCount  = (doc.getLong("totalCount") ?: 0L).toInt(),
                    doneCount   = (doc.getLong("doneCount") ?: 0L).toInt(),
                    technicNames = techNames
                )

                // Проверяем — не оборванная ли загрузка
                val tasksCount = firestore.collection("field_repair_tasks")
                    .whereEqualTo("sessionId", doc.id)
                    .get().await().documents.size

                val warnings = mutableListOf<ImportWarning>()

                warnings.add(
                    ImportWarning(
                        type      = WarningType.ACTIVE_SESSION,
                        message   = "Активный выезд от ${activeSession.date}: ${activeSession.totalCount} самокатов (${activeSession.doneCount} выполнено). Техники: ${techNames.joinToString(", ")}",
                        isBlocker = false
                    )
                )

                if (tasksCount < activeSession.totalCount * 0.9) {
                    warnings.add(
                        ImportWarning(
                            type      = WarningType.PARTIAL_UPLOAD,
                            message   = "⚠️ Предыдущая загрузка оборвалась! В базе только $tasksCount из ${activeSession.totalCount} заданий. Рекомендуем завершить сессию и загрузить заново.",
                            isBlocker = true
                        )
                    )
                }

                _uiState.update {
                    it.copy(
                        isCheckingSession = false,
                        activeSession     = activeSession,
                        warnings          = warnings
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "checkActiveSession error", e)
                _uiState.update { it.copy(isCheckingSession = false) }
            }
        }
    }

    // ── Завершить текущую сессию ──────────────────────────────────────────────
    fun closeActiveSession() {
        val sessionId = _uiState.value.activeSession?.id ?: return
        viewModelScope.launch {
            try {
                firestore.collection("field_repair_sessions")
                    .document(sessionId)
                    .update("status", "completed").await()

                _uiState.update {
                    it.copy(
                        activeSession = null,
                        warnings      = it.warnings.filter { w -> w.type != WarningType.ACTIVE_SESSION && w.type != WarningType.PARTIAL_UPLOAD }
                    )
                }
                Log.d(TAG, "Session $sessionId closed")
            } catch (e: Exception) {
                Log.e(TAG, "closeActiveSession error", e)
            }
        }
    }

    // ── Выбор техника ─────────────────────────────────────────────────────────
    fun selectTechnician(technician: TechnicianInfo) {
        _uiState.update { it.copy(selectedTechnician = technician) }
    }

    // ── Добавить файл в очередь ───────────────────────────────────────────────
    fun addToQueue(uri: Uri, fileName: String) {
        val tech = _uiState.value.selectedTechnician ?: return

        viewModelScope.launch {
            try {
                val tasks = parseExcel(uri, technicianId = tech.id, technicianName = tech.name, sessionId = "preview")

                // Проверяем дубли с уже добавленными в очередь
                val existingNumbers = _uiState.value.queue.flatMap { e -> e.tasks.map { it.scooterNumber } }.toSet()
                val newNumbers = tasks.map { it.scooterNumber }.toSet()
                val duplicates = newNumbers.intersect(existingNumbers)

                val entry = AssignmentEntry(
                    technician   = tech,
                    uri          = uri,
                    fileName     = fileName,
                    scooterCount = tasks.size,
                    isParsed     = true,
                    tasks        = tasks
                )

                val newWarnings = _uiState.value.warnings.toMutableList()

                // Предупреждение о больших файлах
                if (tasks.size > MAX_SAFE_ROWS) {
                    newWarnings.removeAll { it.type == WarningType.TOO_MANY_ROWS }
                    newWarnings.add(
                        ImportWarning(
                            type      = WarningType.TOO_MANY_ROWS,
                            message   = "Файл для «${tech.name}» содержит ${tasks.size} самокатов. Это много для одного техника — убедитесь что загружен правильный файл.",
                            isBlocker = false
                        )
                    )
                }

                // Предупреждение о дублях
                if (duplicates.isNotEmpty()) {
                    newWarnings.removeAll { it.type == WarningType.DUPLICATE_SCOOTERS }
                    newWarnings.add(
                        ImportWarning(
                            type      = WarningType.DUPLICATE_SCOOTERS,
                            message   = "Дубли! ${duplicates.size} самокатов уже назначены другому технику: ${duplicates.take(5).joinToString(", ")}${if (duplicates.size > 5) "..." else ""}",
                            isBlocker = true
                        )
                    )
                }

                _uiState.update { state ->
                    state.copy(
                        queue              = state.queue + entry,
                        selectedTechnician = null,
                        warnings           = newWarnings
                    )
                }

                Log.d(TAG, "Queue: ${tech.name} → ${tasks.size} scooters, dupes: ${duplicates.size}")
            } catch (e: Exception) {
                Log.e(TAG, "addToQueue error", e)
                _uiState.update { it.copy(stage = FieldImportStage.Error("Ошибка чтения файла: ${e.message}")) }
            }
        }
    }

    // ── Удалить из очереди ────────────────────────────────────────────────────
    fun removeFromQueue(entryId: String) {
        _uiState.update { state ->
            val newQueue = state.queue.filter { it.id != entryId }
            // Пересчитываем дубли
            val allNumbers = newQueue.flatMap { e -> e.tasks.map { it.scooterNumber } }
            val dupes = allNumbers.groupBy { it }.filter { it.value.size > 1 }.keys
            val newWarnings = state.warnings.toMutableList()
            if (dupes.isEmpty()) newWarnings.removeAll { it.type == WarningType.DUPLICATE_SCOOTERS }
            // Убираем предупреждение о большом файле если тот техник удалён
            state.queue.find { it.id == entryId }?.let { removed ->
                if (removed.scooterCount > MAX_SAFE_ROWS) {
                    newWarnings.removeAll { it.type == WarningType.TOO_MANY_ROWS }
                }
            }
            state.copy(queue = newQueue, warnings = newWarnings)
        }
    }

    // ── Отправить всё ─────────────────────────────────────────────────────────
    fun sendAll(forceOverride: Boolean = false) {
        val state = _uiState.value
        if (state.queue.isEmpty()) return

        // Блокирующие предупреждения — требуют подтверждения
        val blockers = state.warnings.filter { it.isBlocker }
        if (blockers.isNotEmpty() && !forceOverride) {
            _uiState.update {
                it.copy(
                    showWarningDialog   = true,
                    pendingConfirmAction = { sendAll(forceOverride = true) }
                )
            }
            return
        }

        _uiState.update { it.copy(showWarningDialog = false, pendingConfirmAction = null) }

        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            val t0 = System.currentTimeMillis()
            try {
                val auth      = authManager.authState.value
                val adminId   = auth.userId ?: ""
                val adminName = auth.userName ?: "Администратор"
                val sessionId = UUID.randomUUID().toString()
                val now       = System.currentTimeMillis()
                val dateStr   = LocalDate.now().format(dateFormatter)

                // ── Шаг 1: Закрываем все старые активные сессии ──────────────
                _uiState.update { it.copy(stage = FieldImportStage.Opening, progress = 0.02f) }
                try {
                    val oldSessions = firestore.collection("field_repair_sessions")
                        .whereEqualTo("status", "active")
                        .get().await()

                    if (oldSessions.documents.isNotEmpty()) {
                        val batch = firestore.batch()
                        oldSessions.documents.forEach { doc ->
                            batch.update(doc.reference, "status", "completed")
                        }
                        batch.commit().await()
                        Log.d(TAG, "Closed ${oldSessions.documents.size} old sessions")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not close old sessions", e)
                }

                // ── Шаг 2: Сохраняем задания батчами ─────────────────────────
                var totalSaved = 0
                val queue = state.queue

                queue.forEachIndexed { index, entry ->
                    _uiState.update {
                        it.copy(
                            stage              = FieldImportStage.Saving(index + 1, queue.size),
                            progress           = 0.05f + index.toFloat() / queue.size * 0.85f,
                            currentSendingName = entry.technician.name
                        )
                    }

                    val tasks = entry.tasks.map { task ->
                        task.copy(
                            sessionId      = sessionId,
                            id             = "${sessionId}_${task.scooterNumber}",
                            assignedToId   = entry.technician.id,
                            assignedToName = entry.technician.name,
                            createdAt      = now
                        )
                    }

                    val batches = tasks.chunked(BATCH_SIZE)
                    batches.forEachIndexed { bIdx, batch ->
                        saveBatch(batch)
                        val overall = 0.05f + (index.toFloat() + (bIdx + 1f) / batches.size) / queue.size * 0.85f
                        _uiState.update { it.copy(progress = overall) }
                    }

                    totalSaved += tasks.size
                    Log.d(TAG, "✓ ${entry.technician.name}: ${tasks.size} tasks")
                }

                // ── Шаг 3: Создаём сессию ПОСЛЕ заданий ──────────────────────
                _uiState.update { it.copy(stage = FieldImportStage.Finishing, progress = 0.95f) }

                firestore.collection("field_repair_sessions").document(sessionId).set(
                    mapOf(
                        "id"          to sessionId,
                        "adminId"     to adminId,
                        "adminName"   to adminName,
                        "date"        to dateStr,
                        "totalCount"  to totalSaved,
                        "doneCount"   to 0,
                        "status"      to "active",
                        "technicIds"  to queue.map { it.technician.id }.distinct(),
                        "createdAt"   to now
                    )
                ).await()

                val sec = (System.currentTimeMillis() - t0) / 1000L
                ImportPreferencesManager.saveLastImportInfo(context, now, totalSaved, sessionId)

                _uiState.update {
                    it.copy(
                        stage              = FieldImportStage.Done(totalSaved, sec),
                        progress           = 1f,
                        queue              = emptyList(),
                        warnings           = emptyList(),
                        activeSession      = null,
                        lastImportedAt     = now,
                        lastImportCount    = totalSaved,
                        lastSessionId      = sessionId
                    )
                }

                Log.d(TAG, "=== DONE: $totalSaved tasks in ${sec}s ===")

            } catch (e: Exception) {
                if (e is CancellationException) {
                    _uiState.update { it.copy(stage = FieldImportStage.Idle, progress = 0f) }
                } else {
                    Log.e(TAG, "sendAll error", e)
                    _uiState.update { it.copy(stage = FieldImportStage.Error(e.message ?: "Неизвестная ошибка")) }
                }
            }
        }
    }

    fun dismissWarningDialog() {
        _uiState.update { it.copy(showWarningDialog = false, pendingConfirmAction = null) }
    }

    fun confirmWarningAndSend() {
        val action = _uiState.value.pendingConfirmAction
        _uiState.update { it.copy(showWarningDialog = false, pendingConfirmAction = null) }
        action?.invoke()
    }

    // ── Парсинг Excel ─────────────────────────────────────────────────────────
    private suspend fun parseExcel(
        uri: Uri,
        technicianId: String,
        technicianName: String,
        sessionId: String
    ): List<FieldRepairTask> = withContext(Dispatchers.IO) {
        val tasks = mutableListOf<FieldRepairTask>()
        val tmp = File.createTempFile("field_repair_", ".xlsx", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri).use { src ->
                if (src == null) throw Exception("Не удалось открыть файл")
                tmp.outputStream().use { dst -> src.copyTo(dst) }
            }
            FileInputStream(tmp).use { fis ->
                ReadableWorkbook(fis).use { wb ->
                    val sheet = wb.firstSheet
                    sheet.openStream().use { rows ->
                        val iter = rows.iterator()
                        if (!iter.hasNext()) throw Exception("Пустой файл")
                        iter.next() // пропускаем заголовок
                        var rowCount = 0
                        while (iter.hasNext()) {
                            val row = iter.next()
                            rowCount++

                            if (rowCount > MAX_ALLOWED_ROWS) {
                                throw Exception("Файл содержит более $MAX_ALLOWED_ROWS строк. Убедитесь что это правильный файл с фильтром «Полевой ремонт».")
                            }

                            try {
                                val number = safeString(row, ExcelColumns.SCOOTER_NUMBER)
                                if (number.isBlank() || number.length < 3) continue
                                tasks.add(
                                    FieldRepairTask(
                                        id             = "${sessionId}_${number}",
                                        sessionId      = sessionId,
                                        scooterNumber  = number,
                                        lat            = safeDouble(row, ExcelColumns.LAT),
                                        lon            = safeDouble(row, ExcelColumns.LON),
                                        charge         = safeInt(row, ExcelColumns.CHARGE),
                                        model          = safeString(row, ExcelColumns.MODEL),
                                        process        = safeString(row, ExcelColumns.PROCESS),
                                        processStage   = safeString(row, ExcelColumns.PROCESS_STAGE),
                                        status         = "new",
                                        assignedToId   = technicianId,
                                        assignedToName = technicianName,
                                        isFromList     = true
                                    )
                                )
                            } catch (e: Exception) {
                                Log.w(TAG, "Skip row $rowCount: ${e.message}")
                            }
                        }
                    }
                }
            }
        } finally {
            tmp.delete()
        }

        if (tasks.isEmpty()) throw Exception("Не найдено валидных самокатов в файле")
        tasks
    }

    // ── Сохранение батча ─────────────────────────────────────────────────────
    private suspend fun saveBatch(batch: List<FieldRepairTask>) {
        withContext(Dispatchers.IO) {
            val fb = firestore.batch()
            batch.forEach { task ->
                val docRef = firestore.collection("field_repair_tasks").document(task.id)
                fb.set(docRef, mapOf(
                    "id"             to task.id,
                    "sessionId"      to task.sessionId,
                    "scooterNumber"  to task.scooterNumber,
                    "lat"            to task.lat,
                    "lon"            to task.lon,
                    "charge"         to task.charge,
                    "model"          to task.model,
                    "process"        to task.process,
                    "processStage"   to task.processStage,
                    "status"         to task.status,
                    "assignedToId"   to task.assignedToId,
                    "assignedToName" to task.assignedToName,
                    "isFromList"     to task.isFromList,
                    "repairTypes"    to task.repairTypes,
                    "batteryPctReal" to task.batteryPctReal,
                    "finalStatus"    to task.finalStatus,
                    "completedAt"    to task.completedAt,
                    "createdAt"      to task.createdAt
                ))
            }
            fb.commit().await()
        }
    }

    fun cancelImport() { sendJob?.cancel() }

    fun resetToIdle() {
        sendJob?.cancel()
        _uiState.update { it.copy(stage = FieldImportStage.Idle, progress = 0f) }
    }

    // ── Хелперы ячеек ────────────────────────────────────────────────────────
    private fun safeString(row: org.dhatim.fastexcel.reader.Row, col: Int): String {
        if (col >= row.cellCount) return ""
        return try {
            val cell = row.getCell(col) ?: return ""
            val text = cell.text
            if (!text.isNullOrBlank()) return text.trim()
            when (val v = cell.value) {
                is Number -> if (v.toDouble() % 1.0 == 0.0) v.toLong().toString() else v.toString()
                else -> v?.toString()?.trim() ?: ""
            }
        } catch (e: Exception) { "" }
    }

    private fun safeDouble(row: org.dhatim.fastexcel.reader.Row, col: Int): Double {
        if (col >= row.cellCount) return 0.0
        return try {
            val cell = row.getCell(col) ?: return 0.0
            when (val v = cell.value) {
                is Number -> v.toDouble()
                is String -> v.replace(",", ".").toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
        } catch (e: Exception) { 0.0 }
    }

    private fun safeInt(row: org.dhatim.fastexcel.reader.Row, col: Int): Int =
        safeDouble(row, col).toInt()
}