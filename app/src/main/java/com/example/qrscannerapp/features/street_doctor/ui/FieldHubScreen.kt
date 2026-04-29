package com.example.qrscannerapp.features.street_doctor.ui

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.TelemetryManager
import com.example.qrscannerapp.features.street_doctor.domain.model.ScooterFieldStatus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

private val HubBg        = Color(0xFF0F0F13)
private val HubCard      = Color(0xFF1C1C22)
private val HubBorder    = Color(0xFF2A2A35)
private val HubDivider   = Color(0xFFFFFFFF).copy(alpha = 0.04f)
private val HubPrimary   = Color(0xFF6C5CE7)
private val HubTextMain  = Color(0xFFFFFFFF)
private val HubTextMuted = Color(0xFF8E8E93)
private val HubTextSec   = Color(0xFF5A5A70)

private val ColorNew       = Color(0xFF3B82F6)
private val ColorInProg    = Color(0xFFF59E0B)
private val ColorDone      = Color(0xFF22C55E)
private val ColorToStorage = Color(0xFFEF4444)
private val ColorNotFound  = Color(0xFF6B7280)

private fun statusColor(s: ScooterFieldStatus) = when (s) {
    ScooterFieldStatus.NEW         -> ColorNew
    ScooterFieldStatus.IN_PROGRESS -> ColorInProg
    ScooterFieldStatus.DONE        -> ColorDone
    ScooterFieldStatus.TO_STORAGE  -> ColorToStorage
    ScooterFieldStatus.NOT_FOUND   -> ColorNotFound
}

private fun statusLabel(s: ScooterFieldStatus) = when (s) {
    ScooterFieldStatus.NEW         -> "Новый"
    ScooterFieldStatus.IN_PROGRESS -> "В работе"
    ScooterFieldStatus.DONE        -> "Готово"
    ScooterFieldStatus.TO_STORAGE  -> "На склад"
    ScooterFieldStatus.NOT_FOUND   -> "Не найден"
}

// ── Модели ────────────────────────────────────────────────────────────────────

data class HubScooter(
    val id: String = "",
    val code: String = "",
    val status: ScooterFieldStatus = ScooterFieldStatus.NEW,
    val techId: String = "",
    val techName: String = "",
    val batteryPct: Int? = null,
    val batteryFromExcel: Int = 0,
    val workStartMs: Long? = null,
    val completedMs: Long? = null,
    val isProblematic: Boolean = false
)

data class HubTech(
    val id: String,
    val name: String,
    val initials: String,
    val total: Int,
    val done: Int,
    val inProgress: Int,
    val toStorage: Int,
    val notFound: Int,
    val scooters: List<HubScooter>
)

data class HubSession(val id: String = "", val totalCount: Int = 0)

enum class HubFilter { ALL, IN_PROGRESS, DONE, TO_STORAGE, NOT_FOUND, PROBLEMATIC }

data class FieldHubUiState(
    val session: HubSession? = null,
    val techs: List<HubTech> = emptyList(),
    val allScooters: List<HubScooter> = emptyList(),
    val filter: HubFilter = HubFilter.ALL,
    val search: String = "",
    val isLoading: Boolean = true,
    val sheetDetails: ScooterSheetDetails? = null,
    val sheetLoading: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class FieldHubViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val telemetryManager: TelemetryManager
) : ViewModel() {

    private val db  = Firebase.firestore
    private val TAG = "FieldHubVM"

    private val _state = MutableStateFlow(FieldHubUiState())
    val state: StateFlow<FieldHubUiState> = _state.asStateFlow()

    private var sessionListener: ListenerRegistration? = null
    private var tasksListener:   ListenerRegistration? = null
    private var rebuildJob:      Job? = null
    private val nameCache = mutableMapOf<String, String>()

    init { subscribeSession() }

    private fun subscribeSession() {
        sessionListener?.remove()
        sessionListener = db.collection("field_repair_sessions")
            .whereEqualTo("status", "active").limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e(TAG, "session", err); return@addSnapshotListener }
                if (snap == null || snap.isEmpty) {
                    _state.update { it.copy(isLoading = false, session = null, allScooters = emptyList(), techs = emptyList()) }
                    return@addSnapshotListener
                }
                val doc = snap.documents.first()
                _state.update { it.copy(session = HubSession(id = doc.id, totalCount = (doc.getLong("totalCount") ?: 0L).toInt())) }
                subscribeTasks(doc.id)
            }
    }

    private fun subscribeTasks(sessionId: String) {
        tasksListener?.remove()
        tasksListener = db.collection("field_repair_tasks")
            .whereEqualTo("sessionId", sessionId)
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e(TAG, "tasks", err); return@addSnapshotListener }
                if (snap == null) return@addSnapshotListener
                val scooters = snap.documents.mapNotNull { parseScooter(it) }
                _state.update { it.copy(allScooters = scooters, isLoading = false) }
                scheduleRebuild()
            }
    }

    private fun scheduleRebuild() {
        rebuildJob?.cancel()
        rebuildJob = viewModelScope.launch { delay(300); rebuildTechs() }
    }

    private fun rebuildTechs() {
        viewModelScope.launch {
            val scooters = _state.value.allScooters
            val byTech = scooters.groupBy { it.techId }
            byTech.keys.forEach { techId ->
                if (techId.isBlank() || nameCache.containsKey(techId)) return@forEach
                try {
                    val doc = db.collection("internal_users").document(techId).get().await()
                    nameCache[techId] = doc.getString("displayName") ?: "Неизвестно"
                } catch (_: Exception) {}
            }
            // techs нужны только для статистики шапки, не для отображения секций
            val techs = byTech.mapNotNull { (techId, ts) ->
                if (techId.isBlank()) return@mapNotNull null
                val name = nameCache[techId] ?: ts.firstOrNull()?.techName ?: "Неизвестно"
                HubTech(
                    id         = techId,
                    name       = name,
                    initials   = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                    total      = ts.size,
                    done       = ts.count { it.status == ScooterFieldStatus.DONE },
                    inProgress = ts.count { it.status == ScooterFieldStatus.IN_PROGRESS },
                    toStorage  = ts.count { it.status == ScooterFieldStatus.TO_STORAGE },
                    notFound   = ts.count { it.status == ScooterFieldStatus.NOT_FOUND },
                    scooters   = ts
                )
            }
            _state.update { it.copy(techs = techs) }
        }
    }

    private fun parseScooter(doc: DocumentSnapshot): HubScooter? = try {
        val status = when (doc.getString("status") ?: "new") {
            "in_progress" -> ScooterFieldStatus.IN_PROGRESS
            "done"        -> ScooterFieldStatus.DONE
            "not_found"   -> ScooterFieldStatus.NOT_FOUND
            "to_storage"  -> ScooterFieldStatus.TO_STORAGE
            else          -> ScooterFieldStatus.NEW
        }
        val startMs = doc.getLong("updatedAt")
        val isProb  = status == ScooterFieldStatus.IN_PROGRESS &&
                startMs != null && (System.currentTimeMillis() - startMs) > 30 * 60_000L
        val battReal  = doc.getLong("batteryPctReal")?.toInt()
        val battExcel = when (val v = doc.get("charge")) {
            is Long   -> v.toInt()
            is Double -> v.toInt()
            is String -> v.toIntOrNull() ?: 0
            else      -> 0
        }
        HubScooter(
            id               = doc.id,
            code             = doc.getString("scooterNumber") ?: "",
            status           = status,
            techId           = doc.getString("assignedToId") ?: "",
            techName         = doc.getString("assignedToName") ?: "",
            batteryPct       = battReal,
            batteryFromExcel = battExcel,
            workStartMs      = if (status == ScooterFieldStatus.IN_PROGRESS) startMs else null,
            completedMs      = doc.getLong("completedAt"),
            isProblematic    = isProb
        )
    } catch (e: Exception) { Log.e(TAG, "parse ${doc.id}", e); null }

    fun setFilter(f: HubFilter) = _state.update { it.copy(filter = f) }
    fun setSearch(q: String)    = _state.update { it.copy(search = q) }

    // Плоский отфильтрованный список — без группировки по технику
    // Активные (IN_PROGRESS, NEW) сверху, завершённые снизу по времени завершения
    fun filtered(): List<HubScooter> {
        val s = _state.value
        var list = s.allScooters.filter { it.status != ScooterFieldStatus.NEW }
        if (s.search.isNotBlank()) list = list.filter { it.code.contains(s.search, ignoreCase = true) }
        list = when (s.filter) {
            HubFilter.ALL         -> list
            HubFilter.IN_PROGRESS -> list.filter { it.status == ScooterFieldStatus.IN_PROGRESS }
            HubFilter.DONE        -> list.filter { it.status == ScooterFieldStatus.DONE }
            HubFilter.TO_STORAGE  -> list.filter { it.status == ScooterFieldStatus.TO_STORAGE }
            HubFilter.NOT_FOUND   -> list.filter { it.status == ScooterFieldStatus.NOT_FOUND }
            HubFilter.PROBLEMATIC -> list.filter { it.isProblematic }
        }
        // Сортировка: активные сверху, завершённые снизу по убыванию времени
        return list.sortedWith(
            compareBy<HubScooter> {
                when (it.status) {
                    ScooterFieldStatus.IN_PROGRESS -> 0
                    else -> 1
                }
            }.thenByDescending { it.completedMs ?: 0L }
        )
    }

    // ── Детали для шита ───────────────────────────────────────────────────────
    fun loadScooterDetails(scooterId: String) {
        _state.update { it.copy(sheetLoading = true, sheetDetails = null) }
        viewModelScope.launch {
            try {
                val doc = db.collection("field_repair_tasks").document(scooterId).get().await()
                val status = when (doc.getString("status") ?: "new") {
                    "in_progress" -> ScooterFieldStatus.IN_PROGRESS
                    "done"        -> ScooterFieldStatus.DONE
                    "not_found"   -> ScooterFieldStatus.NOT_FOUND
                    "to_storage"  -> ScooterFieldStatus.TO_STORAGE
                    else          -> ScooterFieldStatus.NEW
                }
                val lat = when (val v = doc.get("lat")) {
                    is Double -> v; is String -> v.toDoubleOrNull() ?: 0.0
                    is Long   -> v.toDouble(); else -> 0.0
                }
                val lon = when (val v = doc.get("lon")) {
                    is Double -> v; is String -> v.toDoubleOrNull() ?: 0.0
                    is Long   -> v.toDouble(); else -> 0.0
                }
                val battExcel = when (val v = doc.get("charge")) {
                    is Long -> v.toInt(); is Double -> v.toInt()
                    is String -> v.toIntOrNull() ?: 0; else -> 0
                }
                @Suppress("UNCHECKED_CAST")
                val repairTypes = (doc.get("repairTypes") as? List<String>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val photoUrls = (doc.get("photoUrls") as? List<String>) ?: emptyList()

                // Считаем прогресс техника из уже загруженных данных в памяти
                val techId = doc.getString("assignedToId") ?: ""
                val techScooters = _state.value.allScooters.filter { it.techId == techId }
                val techDone      = techScooters.count { it.status == ScooterFieldStatus.DONE }
                val techToStorage = techScooters.count { it.status == ScooterFieldStatus.TO_STORAGE }
                val techNotFound  = techScooters.count { it.status == ScooterFieldStatus.NOT_FOUND }
                val techTotal     = techScooters.size

                // batteryPctReal: 0 считаем как null (техник не замерял)
                val battReal = doc.getLong("batteryPctReal")?.toInt()?.takeIf { it > 0 }

                _state.update {
                    it.copy(
                        sheetLoading = false,
                        sheetDetails = ScooterSheetDetails(
                            id               = doc.id,
                            code             = doc.getString("scooterNumber") ?: "",
                            status           = status,
                            technicianName   = doc.getString("assignedToName") ?: "",
                            batteryFromExcel = battExcel,
                            batteryReal      = battReal,
                            lat              = lat,
                            lon              = lon,
                            repairTypes      = repairTypes,
                            notes            = doc.getString("notes") ?: "",
                            photoUrls        = photoUrls,
                            model            = doc.getString("model") ?: "",
                            techDone         = techDone,
                            techToStorage    = techToStorage,
                            techNotFound     = techNotFound,
                            techTotal        = techTotal
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadScooterDetails error", e)
                _state.update { it.copy(sheetLoading = false) }
            }
        }
    }

    fun clearSheetDetails() {
        _state.update { it.copy(sheetDetails = null, sheetLoading = false) }
    }

    override fun onCleared() {
        super.onCleared()
        sessionListener?.remove()
        tasksListener?.remove()
        rebuildJob?.cancel()
    }
}

// ── Вспомогалки ───────────────────────────────────────────────────────────────

@Composable
private fun rememberElapsedStr(startMs: Long?): String {
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(startMs) {
        if (startMs == null) return@LaunchedEffect
        while (true) { elapsed = System.currentTimeMillis() - startMs; delay(1000) }
    }
    if (startMs == null) return ""
    val s = elapsed / 1000
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

// ── Экран ─────────────────────────────────────────────────────────────────────

@Composable
fun FieldHubScreen(viewModel: FieldHubViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val filtered = remember(state) { viewModel.filtered() }
    var showSheet by remember { mutableStateOf(false) }

    val activeScooters = remember(state.allScooters) {
        state.allScooters.filter { it.status != ScooterFieldStatus.NEW }
    }

    val total       = state.allScooters.size
    val done        = activeScooters.count { it.status == ScooterFieldStatus.DONE }
    val inProgress  = activeScooters.count { it.status == ScooterFieldStatus.IN_PROGRESS }
    val toStorage   = activeScooters.count { it.status == ScooterFieldStatus.TO_STORAGE }
    val notFound    = activeScooters.count { it.status == ScooterFieldStatus.NOT_FOUND }
    val problematic = activeScooters.count { it.isProblematic }

    if (showSheet) {
        ScooterInfoSheet(
            details   = state.sheetDetails,
            isLoading = state.sheetLoading,
            onDismiss = {
                showSheet = false
                viewModel.clearSheetDetails()
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(HubBg),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {

        // ── Шапка ──
        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 8.dp)
            ) {
                Text("ХРАНЕНИЕ", fontSize = 10.sp, color = HubPrimary, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Text("Общая доска", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = HubTextMain)
            }
        }

        // ── Сводка + прогресс-бар ──
        item(key = "summary") {
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (total > 0) {
                    val doneF = done.toFloat() / total
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Готово $done из $total", color = HubTextMuted, fontSize = 12.sp)
                        val newCntLabel = total - done - inProgress - toStorage - notFound
                        Text(
                            if (newCntLabel > 0) "${(doneF * 100).toInt()}% · ещё $newCntLabel не взяты"
                            else "${(doneF * 100).toInt()}%",
                            color = ColorDone, fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))) {
                        val t = total.toFloat()
                        if (done > 0)       Box(Modifier.weight(done / t).fillMaxHeight().background(ColorDone))
                        if (inProgress > 0) Box(Modifier.weight(inProgress / t).fillMaxHeight().background(ColorInProg))
                        if (toStorage > 0)  Box(Modifier.weight(toStorage / t).fillMaxHeight().background(ColorToStorage))
                        if (notFound > 0)   Box(Modifier.weight(notFound / t).fillMaxHeight().background(ColorNotFound))
                        val newCnt = total - done - inProgress - toStorage - notFound
                        if (newCnt > 0)     Box(Modifier.weight(newCnt / t).fillMaxHeight().background(HubBorder))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusCount("✓ $done",       ColorDone,      Modifier.weight(1f))
                    StatusCount("⟳ $inProgress", ColorInProg,    Modifier.weight(1f))
                    StatusCount("▲ $toStorage",  ColorToStorage, Modifier.weight(1f))
                    StatusCount("? $notFound",   ColorNotFound,  Modifier.weight(1f))
                }
            }
        }

        // ── Поиск ──
        item(key = "search") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HubCard)
                    .border(1.dp, HubBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, null, tint = HubTextMuted, modifier = Modifier.size(16.dp))
                BasicTextField(
                    value = state.search,
                    onValueChange = { viewModel.setSearch(it) },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = HubTextMain, fontSize = 14.sp),
                    decorationBox = { inner ->
                        if (state.search.isEmpty()) Text("Поиск по номеру...", color = HubTextMuted, fontSize = 14.sp)
                        inner()
                    },
                    singleLine = true
                )
                if (state.search.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearch("") }, modifier = Modifier.size(16.dp)) {
                        Icon(Icons.Default.Close, null, tint = HubTextMuted)
                    }
                }
            }
        }

        // ── Фильтры ──
        item(key = "filters") {
            LazyRow(
                modifier = Modifier.padding(vertical = 10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val activeCnt = done + inProgress + toStorage + notFound
                item { HubChip("Все · $activeCnt",  state.filter == HubFilter.ALL,         HubTextMain)    { viewModel.setFilter(HubFilter.ALL) } }
                item { HubChip("⟳ $inProgress",     state.filter == HubFilter.IN_PROGRESS, ColorInProg)    { viewModel.setFilter(HubFilter.IN_PROGRESS) } }
                item { HubChip("✓ $done",            state.filter == HubFilter.DONE,        ColorDone)      { viewModel.setFilter(HubFilter.DONE) } }
                item { HubChip("▲ $toStorage",       state.filter == HubFilter.TO_STORAGE,  ColorToStorage) { viewModel.setFilter(HubFilter.TO_STORAGE) } }
                item { HubChip("? $notFound",        state.filter == HubFilter.NOT_FOUND,   ColorNotFound)  { viewModel.setFilter(HubFilter.NOT_FOUND) } }
                if (problematic > 0) {
                    item { HubChip("⚠ $problematic", state.filter == HubFilter.PROBLEMATIC, ColorToStorage) { viewModel.setFilter(HubFilter.PROBLEMATIC) } }
                }
            }
        }

        // ── Состояния загрузки ──
        if (state.isLoading) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HubPrimary, modifier = Modifier.size(28.dp))
                }
            }
            return@LazyColumn
        }

        if (state.session == null) {
            item(key = "no_session") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.Inbox, null, tint = HubTextMuted, modifier = Modifier.size(44.dp))
                    Text("Нет активной сессии", color = HubTextMuted, fontSize = 15.sp)
                    Text("Загрузите задания через импорт", color = HubTextSec, fontSize = 12.sp)
                }
            }
            return@LazyColumn
        }

        if (filtered.isEmpty()) {
            item(key = "empty") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.HourglassEmpty, null, tint = HubTextMuted, modifier = Modifier.size(36.dp))
                    Text(
                        when {
                            state.search.isNotBlank() -> "Ничего не найдено"
                            state.allScooters.all { it.status == ScooterFieldStatus.NEW } -> "Техники ещё не начали работу"
                            else -> "Нет данных"
                        },
                        color = HubTextMuted, fontSize = 14.sp
                    )
                    if (state.allScooters.any { it.status == ScooterFieldStatus.NEW }) {
                        Text(
                            "Ждём первых действий (${state.allScooters.count { it.status == ScooterFieldStatus.NEW }} не взяты)",
                            color = HubTextSec, fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
            return@LazyColumn
        }

        // ── Плоский список всех номеров ──
        items(items = filtered, key = { "sc_${it.id}" }) { scooter ->
            ScooterRow(
                scooter = scooter,
                onClick = {
                    viewModel.loadScooterDetails(scooter.id)
                    showSheet = true
                }
            )
            HorizontalDivider(color = HubDivider, thickness = 0.5.dp, modifier = Modifier.padding(start = 20.dp))
        }

        item(key = "bottom") { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Компоненты ────────────────────────────────────────────────────────────────

@Composable
private fun ScooterRow(scooter: HubScooter, onClick: () -> Unit) {
    val color = statusColor(scooter.status)
    val elapsed = rememberElapsedStr(scooter.workStartMs)
    val rowBg by animateColorAsState(
        targetValue = when {
            scooter.isProblematic -> ColorToStorage.copy(alpha = 0.05f)
            scooter.status == ScooterFieldStatus.TO_STORAGE -> ColorToStorage.copy(alpha = 0.03f)
            else -> Color.Transparent
        },
        animationSpec = tween(400),
        label = "row_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Цветная точка
        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(color))

        // Номер
        Text(
            scooter.code,
            color = if (scooter.status == ScooterFieldStatus.NOT_FOUND) HubTextMuted else HubTextMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp,
            modifier = Modifier.width(90.dp),
            maxLines = 1
        )

        // Батарея
        val batt = scooter.batteryPct ?: scooter.batteryFromExcel.takeIf { it > 0 }
        if (batt != null) {
            val battColor = when { batt <= 10 -> ColorToStorage; batt <= 30 -> ColorInProg; else -> HubTextMuted }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.width(40.dp)
            ) {
                Icon(Icons.Default.BatteryStd, null, tint = battColor, modifier = Modifier.size(12.dp))
                Text("$batt%", color = battColor, fontSize = 11.sp)
            }
        } else {
            Spacer(Modifier.width(40.dp))
        }

        // Имя техника — сокращённо
        Text(
            scooter.techName.split(" ").let { p ->
                if (p.size >= 2) "${p[0]} ${p[1].firstOrNull() ?: ""}." else p.firstOrNull() ?: ""
            },
            color = HubTextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Статус + время
        Column(horizontalAlignment = Alignment.End) {
            Text(
                statusLabel(scooter.status),
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            when (scooter.status) {
                ScooterFieldStatus.IN_PROGRESS -> if (elapsed.isNotEmpty()) {
                    Text(
                        elapsed,
                        color = if (scooter.isProblematic) ColorToStorage else HubTextSec,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                ScooterFieldStatus.DONE,
                ScooterFieldStatus.TO_STORAGE,
                ScooterFieldStatus.NOT_FOUND -> scooter.completedMs?.let {
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)),
                        color = HubTextSec,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun StatusCount(label: String, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(0.07f))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HubChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) color.copy(0.15f) else HubCard)
            .border(1.dp, if (selected) color.copy(0.4f) else HubBorder, RoundedCornerShape(9.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (selected) color else HubTextMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}