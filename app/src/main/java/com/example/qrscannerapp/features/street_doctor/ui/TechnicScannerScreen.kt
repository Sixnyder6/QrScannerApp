// TechnicScannerScreen.kt
// QR-сканер для техника полевого ремонта.
// Логика: сканируешь самокат → ищет в field_repair_tasks (assignedToId == myId)
// Если нашёл → PassportScreen. Нет в списке → предлагает добавить как незапланированный.
// Включает возможность ручного ввода поврежденного QR-кода с жестким форматом "H LNNNL".
package com.example.qrscannerapp.features.street_doctor.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrscannerapp.AuthManager
import com.example.qrscannerapp.TelemetryManager
import com.example.qrscannerapp.features.scanner.ui.components.CameraView
import com.example.qrscannerapp.features.street_doctor.domain.model.ScooterFieldStatus
import com.example.qrscannerapp.features.street_doctor.domain.model.StreetScooter
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.math.min

// ── Цвета (совпадают с TasksScreen) ──────────────────────────────────────────
private val ScBg         = Color(0xFF0F0F13)
private val ScCard       = Color(0xFF1C1C22)
private val ScPrimary    = Color(0xFF6C5CE7)
private val ScSuccess    = Color(0xFF10B981)
private val ScWarning    = Color(0xFFF59E0B)
private val ScDanger     = Color(0xFFEF4444)
private val ScTextMain   = Color(0xFFFFFFFF)
private val ScTextMuted  = Color(0xFF8E8E93)
private val ScDivider    = Color(0xFFFFFFFF).copy(alpha = 0.06f)
private val ScBorder     = Color(0xFF2A2A35)

// ── Состояние сканирования ────────────────────────────────────────────────────

sealed interface ScanResult {
    object Idle : ScanResult
    object Scanning : ScanResult
    /** Нашли в списке заданий */
    data class FoundInList(val scooter: StreetScooter) : ScanResult
    /** Не нашли в списке — предлагаем добавить незапланированным */
    data class NotInList(val code: String) : ScanResult
    /** Ошибка (нет сессии, сеть и т.д.) */
    data class Error(val message: String) : ScanResult
}

data class TechnicScannerUiState(
    val scanResult: ScanResult = ScanResult.Idle,
    val lastScannedCode: String = "",
    val sessionId: String? = null,
    val myTasks: List<StreetScooter> = emptyList(),   // кэш заданий текущего техника
    val isAddingUnplanned: Boolean = false,
    val scanEffect: ScanEffect = ScanEffect.None
)

enum class ScanEffect { None, Success, NotFound, Error }

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class TechnicScannerViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val telemetryManager: TelemetryManager
) : ViewModel() {

    private val db = Firebase.firestore
    private val TAG = "TechnicScannerVM"

    private val _uiState = MutableStateFlow(TechnicScannerUiState())
    val uiState = _uiState.asStateFlow()

    private var sessionListener: ListenerRegistration? = null
    private var tasksListener: ListenerRegistration? = null

    // debounce: не обрабатываем один и тот же код дважды подряд
    private var lastCode = ""
    private var lastCodeTime = 0L
    private val DEBOUNCE_MS = 2500L

    private var effectResetJob: Job? = null

    init {
        subscribeToActiveSession()
    }

    // ── Подписка на активную сессию ───────────────────────────────────────────
    private fun subscribeToActiveSession() {
        sessionListener?.remove()
        sessionListener = db.collection("field_repair_sessions")
            .whereEqualTo("status", "active")
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e(TAG, "session error", err); return@addSnapshotListener }
                if (snap == null || snap.isEmpty) {
                    _uiState.update { it.copy(sessionId = null, myTasks = emptyList()) }
                    return@addSnapshotListener
                }
                val sessionId = snap.documents.first().id
                _uiState.update { it.copy(sessionId = sessionId) }
                subscribeToMyTasks(sessionId)
            }
    }

    // ── Подписка на свои задания ───────────────────────────────────────────────
    private fun subscribeToMyTasks(sessionId: String) {
        val myId = authManager.authState.value.userId ?: return
        tasksListener?.remove()
        tasksListener = db.collection("field_repair_tasks")
            .whereEqualTo("sessionId", sessionId)
            .whereEqualTo("assignedToId", myId)
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e(TAG, "tasks error", err); return@addSnapshotListener }
                if (snap == null) return@addSnapshotListener

                val scooters = snap.documents.mapNotNull { doc ->
                    try {
                        val status = when (doc.getString("status") ?: "new") {
                            "in_progress" -> ScooterFieldStatus.IN_PROGRESS
                            "done"        -> ScooterFieldStatus.DONE
                            "not_found"   -> ScooterFieldStatus.NOT_FOUND
                            "to_storage"  -> ScooterFieldStatus.TO_STORAGE
                            else          -> ScooterFieldStatus.NEW
                        }
                        val lat = when (val v = doc.get("lat")) {
                            is Double -> v; is Long -> v.toDouble()
                            is String -> v.toDoubleOrNull() ?: 0.0; else -> 0.0
                        }
                        val lon = when (val v = doc.get("lon")) {
                            is Double -> v; is Long -> v.toDouble()
                            is String -> v.toDoubleOrNull() ?: 0.0; else -> 0.0
                        }
                        StreetScooter(
                            id             = doc.id,
                            code           = doc.getString("scooterNumber") ?: "",
                            status         = status,
                            distanceMeters = 0,
                            batteryPct     = (doc.getLong("charge") ?: 0L).toInt(),
                            address        = doc.getString("processStage") ?: "",
                            lat            = lat, lon = lon,
                            model          = doc.getString("model") ?: "",
                            isMine         = true,
                            workerName     = doc.getString("assignedToName"),
                            workerInitials = doc.getString("assignedToName")
                                ?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString(""),
                            workerRole     = "Техник",
                            workStartTime  = if (status == ScooterFieldStatus.IN_PROGRESS)
                                doc.getLong("updatedAt") ?: doc.getLong("createdAt") else null
                        )
                    } catch (e: Exception) { Log.e(TAG, "parse error ${doc.id}", e); null }
                }
                _uiState.update { it.copy(myTasks = scooters) }
            }
    }

    // ── Обработка QR-кода ─────────────────────────────────────────────────────
    fun onCodeScanned(raw: String) {
        val code = extractScooterCode(raw)
        if (code.isBlank()) return

        val now = System.currentTimeMillis()
        if (code == lastCode && (now - lastCodeTime) < DEBOUNCE_MS) return
        lastCode = code
        lastCodeTime = now

        _uiState.update { it.copy(lastScannedCode = code, scanResult = ScanResult.Scanning) }

        viewModelScope.launch {
            val found = _uiState.value.myTasks.find {
                it.code.equals(code, ignoreCase = true)
            }

            if (found != null) {
                triggerEffect(ScanEffect.Success)
                _uiState.update { it.copy(scanResult = ScanResult.FoundInList(found)) }
            } else {
                if (_uiState.value.sessionId == null) {
                    triggerEffect(ScanEffect.Error)
                    _uiState.update { it.copy(scanResult = ScanResult.Error("Нет активной сессии полевого ремонта")) }
                } else {
                    triggerEffect(ScanEffect.NotFound)
                    _uiState.update { it.copy(scanResult = ScanResult.NotInList(code)) }
                }
            }
        }
    }

    // ── Добавление незапланированного самоката ────────────────────────────────
    fun addUnplannedScooter(
        code: String,
        onSuccess: (StreetScooter) -> Unit
    ) {
        val sessionId = _uiState.value.sessionId ?: return
        val auth = authManager.authState.value
        val myId = auth.userId ?: return
        val myName = auth.userName ?: "Техник"

        _uiState.update { it.copy(isAddingUnplanned = true) }

        viewModelScope.launch {
            try {
                val docId = db.collection("field_repair_tasks").document().id
                val now = System.currentTimeMillis()
                val gps = telemetryManager.getCurrentLocation()
                val lat = gps?.latitude ?: 0.0
                val lon = gps?.longitude ?: 0.0
                val data = hashMapOf(
                    "sessionId"      to sessionId,
                    "scooterNumber"  to code,
                    "assignedToId"   to myId,
                    "assignedToName" to myName,
                    "status"         to "in_progress",
                    "isFromList"     to false,
                    "isUnplanned"    to true,
                    "createdAt"      to now,
                    "updatedAt"      to now,
                    "lat"            to lat,
                    "lon"            to lon,
                    "charge"         to 0,
                    "model"          to "",
                    "processStage"   to "Незапланированный"
                )
                db.collection("field_repair_tasks").document(docId).set(data).await()

                val newScooter = StreetScooter(
                    id             = docId,
                    code           = code,
                    status         = ScooterFieldStatus.IN_PROGRESS,
                    distanceMeters = 0,
                    batteryPct     = 0,
                    address        = "Незапланированный",
                    lat            = lat, lon = lon,
                    model          = "",
                    isMine         = true,
                    workerName     = myName,
                    workerInitials = myName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                    workerRole     = "Техник",
                    workStartTime  = now
                )
                _uiState.update { it.copy(isAddingUnplanned = false, scanResult = ScanResult.Idle) }
                onSuccess(newScooter)
            } catch (e: Exception) {
                Log.e(TAG, "addUnplanned error", e)
                _uiState.update { it.copy(isAddingUnplanned = false, scanResult = ScanResult.Error("Не удалось добавить: ${e.message}")) }
            }
        }
    }

    fun resetScan() {
        lastCode = ""
        _uiState.update { it.copy(scanResult = ScanResult.Idle, lastScannedCode = "") }
    }

    private fun triggerEffect(effect: ScanEffect) {
        effectResetJob?.cancel()
        _uiState.update { it.copy(scanEffect = effect) }
        effectResetJob = viewModelScope.launch {
            delay(800)
            _uiState.update { it.copy(scanEffect = ScanEffect.None) }
        }
    }

    // ── Парсинг номера самоката из QR ─────────────────────────────────────────
    private fun extractScooterCode(raw: String): String {
        if (raw.contains("number=")) {
            val extracted = raw.substringAfter("number=")
                .split('&', '?', '#').firstOrNull()?.trim()
            if (!extracted.isNullOrBlank()) return extracted.uppercase()
        }
        if (raw.contains('/')) {
            val segment = raw.split('/').lastOrNull { it.isNotBlank() }
            if (!segment.isNullOrBlank() && segment.matches(Regex("[A-Za-z0-9]{2,12}"))) {
                return segment.uppercase()
            }
        }
        val cleaned = raw.trim()
        if (cleaned.matches(Regex("[A-Za-z0-9]{2,12}"))
            && !cleaned.startsWith("4BB") && !cleaned.startsWith("4BZ")
            && !cleaned.startsWith("5BB") && !cleaned.startsWith("SF")) {
            return cleaned.uppercase()
        }
        return ""
    }

    override fun onCleared() {
        super.onCleared()
        sessionListener?.remove()
        tasksListener?.remove()
        effectResetJob?.cancel()
    }
}

// ── Главный composable ────────────────────────────────────────────────────────

@Composable
fun TechnicScannerScreen(
    viewModel: TechnicScannerViewModel = hiltViewModel(),
    onMenuClick: () -> Unit = {},
    onOpenPassport: (StreetScooter) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var pendingUnplannedCode by remember { mutableStateOf<String?>(null) }
    var showManualInputDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.scanResult) {
        when (val r = uiState.scanResult) {
            is ScanResult.FoundInList -> {
                delay(400)
                onOpenPassport(r.scooter)
                viewModel.resetScan()
            }
            is ScanResult.NotInList -> {
                pendingUnplannedCode = r.code
            }
            else -> {}
        }
    }

    if (showManualInputDialog) {
        ManualInputDialog(
            onDismiss = { showManualInputDialog = false },
            onConfirm = { code ->
                showManualInputDialog = false
                viewModel.onCodeScanned(code)
            }
        )
    }

    if (pendingUnplannedCode != null) {
        UnplannedScooterDialog(
            code = pendingUnplannedCode!!,
            isAdding = uiState.isAddingUnplanned,
            onAdd = { code ->
                viewModel.addUnplannedScooter(code) { newScooter ->
                    pendingUnplannedCode = null
                    onOpenPassport(newScooter)
                }
            },
            onDismiss = {
                pendingUnplannedCode = null
                viewModel.resetScan()
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(ScBg)) {
        CameraView(
            isSearchMode = false,
            hasPermission = hasCameraPermission,
            scanEventFlow = kotlinx.coroutines.flow.emptyFlow(),
            isTorchOn = false,
            onTorchChange = {},
            onCodeScanned = { code -> viewModel.onCodeScanned(code) },
            onStatusUpdate = { _, _ -> }
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ScannerTopBar(onMenuClick = onMenuClick)

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ScanFrame(effect = uiState.scanEffect)
            }

            ScannerBottomPanel(
                uiState = uiState,
                onRetry = { viewModel.resetScan() },
                onManualInputClick = { showManualInputDialog = true }
            )
        }
    }
}

// ── Шапка ─────────────────────────────────────────────────────────────────────

@Composable
private fun ScannerTopBar(onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Menu, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "STREET DOCTOR",
                fontSize = 9.sp,
                color = ScPrimary,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Text(
                "Сканер самоката",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ── Рамка сканирования с эффектами ───────────────────────────────────────────

@Composable
private fun ScanFrame(effect: ScanEffect) {
    val frameSize = 220.dp
    val cornerLen = 28.dp
    val cornerThick = 3.dp

    val frameColor by animateColorAsState(
        targetValue = when (effect) {
            ScanEffect.Success  -> ScSuccess
            ScanEffect.NotFound -> ScWarning
            ScanEffect.Error    -> ScDanger
            ScanEffect.None     -> Color.White.copy(alpha = 0.8f)
        },
        animationSpec = tween(250),
        label = "frame_color"
    )

    val pulse = rememberInfiniteTransition(label = "scan_pulse")
    val scanLine by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "scan_line")

    Box(
        modifier = Modifier.size(frameSize),
        contentAlignment = Alignment.Center
    ) {
        if (effect == ScanEffect.None) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(2.dp)
                    .offset(y = (frameSize * (scanLine - 0.5f)))
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color.Transparent, ScPrimary.copy(0.8f), Color.Transparent)
                        ),
                        RoundedCornerShape(1.dp)
                    )
            )
        }

        CornerBracket(Alignment.TopStart, frameSize, cornerLen, cornerThick, frameColor)
        CornerBracket(Alignment.TopEnd, frameSize, cornerLen, cornerThick, frameColor)
        CornerBracket(Alignment.BottomStart, frameSize, cornerLen, cornerThick, frameColor)
        CornerBracket(Alignment.BottomEnd, frameSize, cornerLen, cornerThick, frameColor)

        AnimatedVisibility(
            visible = effect != ScanEffect.None,
            enter = scaleIn(spring(0.5f)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(when (effect) {
                        ScanEffect.Success  -> ScSuccess.copy(0.9f)
                        ScanEffect.NotFound -> ScWarning.copy(0.9f)
                        ScanEffect.Error    -> ScDanger.copy(0.9f)
                        else               -> Color.Transparent
                    }),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (effect) {
                        ScanEffect.Success  -> Icons.Default.CheckCircle
                        ScanEffect.NotFound -> Icons.Default.Help
                        ScanEffect.Error    -> Icons.Default.ErrorOutline
                        else               -> Icons.Default.QrCodeScanner
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun CornerBracket(
    alignment: Alignment,
    frameSize: androidx.compose.ui.unit.Dp,
    cornerLen: androidx.compose.ui.unit.Dp,
    thickness: androidx.compose.ui.unit.Dp,
    color: Color
) {
    Box(modifier = Modifier.size(frameSize)) {
        Box(
            modifier = Modifier
                .width(cornerLen)
                .height(thickness)
                .background(color, CircleShape)
                .align(
                    when (alignment) {
                        Alignment.TopStart    -> Alignment.TopStart
                        Alignment.TopEnd      -> Alignment.TopEnd
                        Alignment.BottomStart -> Alignment.BottomStart
                        else                  -> Alignment.BottomEnd
                    }
                )
        )
        Box(
            modifier = Modifier
                .width(thickness)
                .height(cornerLen)
                .background(color, CircleShape)
                .align(
                    when (alignment) {
                        Alignment.TopStart    -> Alignment.TopStart
                        Alignment.TopEnd      -> Alignment.TopEnd
                        Alignment.BottomStart -> Alignment.BottomStart
                        else                  -> Alignment.BottomEnd
                    }
                )
        )
    }
}

// ── Нижняя панель ─────────────────────────────────────────────────────────────

@Composable
private fun ScannerBottomPanel(
    uiState: TechnicScannerUiState,
    onRetry: () -> Unit,
    onManualInputClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                )
            )
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (val r = uiState.scanResult) {
            is ScanResult.Idle -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.size(44.dp))
                    StatusHint(
                        icon = Icons.Outlined.QrCodeScanner,
                        text = if (uiState.sessionId != null)
                            "Наведите камеру на QR-код самоката"
                        else
                            "Нет активной сессии ремонта",
                        color = if (uiState.sessionId != null) ScTextMuted else ScDanger,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onManualInputClick,
                        enabled = uiState.sessionId != null,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Keyboard,
                            contentDescription = "Ручной ввод",
                            tint = if (uiState.sessionId != null) ScTextMuted else ScTextMuted.copy(alpha = 0.4f)
                        )
                    }
                }

                if (uiState.sessionId != null && uiState.myTasks.isNotEmpty()) {
                    val done = uiState.myTasks.count {
                        it.status == ScooterFieldStatus.DONE ||
                                it.status == ScooterFieldStatus.TO_STORAGE
                    }
                    val total = uiState.myTasks.size
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ScSuccess))
                        Text(
                            "Заданий: $done/$total выполнено",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            is ScanResult.Scanning -> {
                StatusHint(
                    icon = Icons.Default.Search,
                    text = "Поиск «${uiState.lastScannedCode}»...",
                    color = ScPrimary
                )
                CircularProgressIndicator(
                    color = ScPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
            is ScanResult.FoundInList -> {
                StatusHint(
                    icon = Icons.Default.CheckCircle,
                    text = "Найден · открываем паспорт...",
                    color = ScSuccess
                )
            }
            is ScanResult.NotInList -> { /* диалог открыт */ }
            is ScanResult.Error -> {
                StatusHint(
                    icon = Icons.Default.ErrorOutline,
                    text = r.message,
                    color = ScDanger
                )
                OutlinedButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ScDanger),
                    border = BorderStroke(1.dp, ScDanger.copy(0.4f))
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Повторить", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusHint(icon: ImageVector, text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

// ── Диалог: незапланированный самокат ─────────────────────────────────────────

@Composable
private fun UnplannedScooterDialog(
    code: String,
    isAdding: Boolean,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { if (!isAdding) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ScWarning.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TwoWheeler, null, tint = ScWarning, modifier = Modifier.size(30.dp))
                }
                Text(
                    "Самокат вне списка",
                    color = ScTextMain,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        code,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }
                Text(
                    "Этого самоката нет в ваших заданиях. Хотите добавить его как незапланированный ремонт?",
                    color = ScTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ScWarning.copy(0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.AddTask, null, tint = ScWarning, modifier = Modifier.size(14.dp))
                    Text(
                        "Будет создан как незапланированный",
                        color = ScWarning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isAdding,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ScTextMuted),
                        border = BorderStroke(1.dp, ScDivider)
                    ) {
                        Text("Отмена", fontSize = 14.sp)
                    }
                    Button(
                        onClick = { onAdd(code) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isAdding,
                        colors = ButtonDefaults.buttonColors(containerColor = ScPrimary)
                    ) {
                        if (isAdding) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Добавить", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Диалог ручного ввода с форматированием и валидацией ─────────────────────

/**
 * Визуальное преобразование вводимого текста в формат "H XNNN X".
 */
private class ScooterCodeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val inputText = text.text
        val formattedText = buildAnnotatedString {
            append("H ")

            if (inputText.isNotEmpty()) {
                append(inputText[0])
            }
            if (inputText.length > 1) {
                append(inputText.substring(1, min(inputText.length, 4)))
            }
            if (inputText.length > 3) {
                append(" ")
            }
            if (inputText.length > 4) {
                append(inputText[4])
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset == 0 -> 2
                    offset <= 3 -> offset + 2
                    offset == 4 -> offset + 3
                    offset >= 5 -> offset + 3
                    else -> formattedText.length
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 2 -> 0
                    offset <= 5 -> offset - 2
                    offset == 6 -> 3
                    offset >= 7 -> offset - 3
                    else -> inputText.length
                }
            }
        }
        return TransformedText(formattedText, offsetMapping)
    }
}

@Composable
private fun ManualInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // Пользователь вводит только 5 символов после 'H'
    var userInput by remember { mutableStateOf("") }
    val maxLen = 5

    // Переключение клавиатуры в зависимости от того, что ожидаем ввести
    val keyboardType = when (userInput.length) {
        0, 4 -> KeyboardType.Ascii // Ожидаем букву
        1, 2, 3 -> KeyboardType.Number // Ожидаем цифру
        else -> KeyboardType.Ascii
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Ручной ввод номера",
                    color = ScTextMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "Введите 1 букву, 3 цифры и 1 букву. Формат: H LNNN L",
                    color = ScTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { newText ->
                        if (newText.length <= maxLen) {
                            val lastChar = newText.lastOrNull()
                            val isValid = when (newText.length) {
                                1, 5 -> lastChar?.isLetter() ?: true
                                2, 3, 4 -> lastChar?.isDigit() ?: true
                                else -> true
                            }
                            if (isValid) {
                                userInput = newText.uppercase()
                            }
                        }
                    },
                    label = { Text("Номер самоката") },
                    placeholder = { Text("H ... ...", fontFamily = FontFamily.Monospace, color = ScTextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = ScooterCodeVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrect = false,
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Done
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ScCard,
                        unfocusedContainerColor = ScCard,
                        focusedIndicatorColor = ScPrimary,
                        unfocusedIndicatorColor = ScBorder,
                        focusedTextColor = ScTextMain,
                        unfocusedTextColor = ScTextMain,
                        cursorColor = ScPrimary,
                        focusedLabelColor = ScPrimary,
                        unfocusedLabelColor = ScTextMuted,
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ScTextMuted),
                        border = BorderStroke(1.dp, ScDivider)
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = { onConfirm("H${userInput}") },
                        enabled = userInput.length == maxLen,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ScPrimary)
                    ) {
                        Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Найти", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}