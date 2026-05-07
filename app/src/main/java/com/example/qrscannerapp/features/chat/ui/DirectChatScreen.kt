package com.example.qrscannerapp.features.chat.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.qrscannerapp.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private fun dmRoleColor(role: String): Color = when (role) {
    "admin"             -> Color(0xFFEC407A)
    "inventory_manager" -> Color(0xFF4CAF50)
    "muver"             -> Color(0xFF29B6F6)
    "electrician"       -> Color(0xFFFFCA28)
    "technic"           -> Color(0xFFAB47BC)
    else                -> Color(0xFF78909C)
}

private fun dmRoleLabel(role: String): String = when (role) {
    "admin"             -> "Админ"
    "inventory_manager" -> "Кладовщик"
    "muver"             -> "Мувёр"
    "electrician"       -> "Электрик"
    "technic"           -> "Техник"
    else                -> ""
}

@Composable
fun DirectChatScreen(
    peerId: String,
    peerName: String,
    peerRole: String,
    onBack: () -> Unit,
    onOpenProfile: (peerId: String, peerName: String, peerRole: String) -> Unit = { _, _, _ -> },
    viewModel: DirectChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var inputText by remember { mutableStateOf("") }
    var messageToDelete by remember { mutableStateOf<DirectMessage?>(null) }
    val currentUserId = viewModel.currentUserId
    val accentColor = dmRoleColor(peerRole)
    val isAtBottom by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index == 0 } }
    val showScrollToBottom by remember { derivedStateOf { !isAtBottom && listState.layoutInfo.totalItemsCount > 0 } }

    LaunchedEffect(peerId) {
        viewModel.openConversation(peerId, peerName, peerRole)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    // Диалог удаления
    messageToDelete?.let { msg ->
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("Удалить сообщение?", color = StardustTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${msg.text.take(60)}${if (msg.text.length > 60) "..." else ""}\"",
                    color = StardustTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteMessage(msg.id); messageToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = StardustError),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Удалить", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) {
                    Text("Отмена", color = StardustTextSecondary)
                }
            },
            containerColor = StardustModalBg,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StardustSolidBg)
    ) {
        // ── Цветная полоска сверху ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.3f)))
                )
        )

        // ── Топбар ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A22))
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    null,
                    tint = StardustTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Аватар с онлайн/typing индикатором
            val initials = peerName.split(" ").take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                .joinToString("").ifBlank { "?" }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clickable { onOpenProfile(peerId, peerName, peerRole) }
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(accentColor.copy(alpha = 0.35f), accentColor.copy(alpha = 0.15f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initials,
                        color = accentColor,
                        fontSize = if (initials.length > 1) 13.sp else 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                // НОВОЕ: индикатор "печатает" на аватаре — пульсирующие точки
                if (uiState.isPeerTyping) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(StardustSolidBg)
                            .padding(1.5.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Имя + статус "печатает" под именем
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenProfile(peerId, peerName, peerRole) },
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = peerName,
                    color = StardustTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // НОВОЕ: анимированный статус под именем
                AnimatedContent(
                    targetState = Pair(uiState.isPeerTyping, uiState.isPeerOnline),
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    },
                    label = "typing_status"
                ) { (isTyping, isOnline) ->
                    if (isTyping) {
                        DmTypingDots(accentColor = accentColor)
                    } else if (isOnline) {
                        val pulseTransition = rememberInfiniteTransition(label = "dm_online_pulse")
                        val pAlpha by pulseTransition.animateFloat(
                            0.5f, 1f,
                            infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                            label = "pa"
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50).copy(alpha = pAlpha))
                            )
                            Text(
                                "онлайн",
                                color = Color(0xFF4CAF50),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        val label = dmRoleLabel(peerRole)
                        if (label.isNotBlank()) {
                            Text(
                                text = label,
                                color = accentColor.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Icon(
                Icons.Default.Lock,
                null,
                tint = StardustTextSecondary.copy(alpha = 0.35f),
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(15.dp)
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

        // ── Контент ─────────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = accentColor, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Загружаем переписку...", color = StardustTextSecondary, fontSize = 13.sp)
                    }
                }
                uiState.messages.isEmpty() -> {
                    DmEmptyState(peerName = peerName, accentColor = accentColor)
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            val isOwn = message.senderId == currentUserId
                            DmMessageBubble(
                                message = message,
                                isOwn = isOwn,
                                accentColor = accentColor,
                                canDelete = isOwn,
                                onDelete = { messageToDelete = message }
                            )
                        }
                    }
                }
            }

            // Баннер "печатает" снизу контентной зоны
            if (uiState.isPeerTyping) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DmTypingDotsRow(accentColor = accentColor)
                    Text(
                        "$peerName печатает...",
                        color = StardustTextSecondary,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            // Кнопка "прокрутить вниз"
            val scrollBtnAlpha by animateFloatAsState(
                targetValue = if (showScrollToBottom) 1f else 0f,
                animationSpec = tween(200),
                label = "dm_scroll_alpha"
            )
            val scrollBtnScale by animateFloatAsState(
                targetValue = if (showScrollToBottom) 1f else 0.7f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "dm_scroll_scale"
            )
            if (scrollBtnAlpha > 0f) {
                FloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = if (uiState.isPeerTyping) 52.dp else 8.dp)
                        .size(42.dp)
                        .alpha(scrollBtnAlpha)
                        .scale(scrollBtnScale),
                    shape = CircleShape,
                    containerColor = accentColor,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "Вниз", modifier = Modifier.size(22.dp))
                }
            }
        }

        // ── Поле ввода ───────────────────────────────────────────────────────
        val canSend = inputText.isNotBlank() && !uiState.isSending

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, StardustSolidBg)))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                // НОВОЕ: при каждом изменении текста сообщаем ViewModel
                onValueChange = {
                    inputText = it
                    if (it.isNotBlank()) viewModel.onTyping()
                },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Сообщение для $peerName...",
                        color = StardustTextSecondary.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                },
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default
                ),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor.copy(alpha = 0.7f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color.White.copy(alpha = 0.07f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                    cursorColor = accentColor,
                    focusedTextColor = StardustTextPrimary,
                    unfocusedTextColor = StardustTextPrimary
                ),
                textStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend)
                            Brush.linearGradient(listOf(accentColor, accentColor.copy(alpha = 0.6f)))
                        else
                            Brush.linearGradient(listOf(StardustItemBg, StardustItemBg))
                    )
                    .clickable(enabled = canSend) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                        focusManager.clearFocus()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isSending) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        "Отправить",
                        tint = if (canSend) Color.White else StardustTextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================================
// НОВОЕ: Анимированные точки "печатает" — компактная версия для топбара
// ============================================================================================

@Composable
private fun DmTypingDots(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "dm_typing")
    val dot1 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse, StartOffset(0)), label = "d1")
    val dot2 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse, StartOffset(140)), label = "d2")
    val dot3 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse, StartOffset(280)), label = "d3")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        listOf(dot1, dot2, dot3).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = alpha))
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "печатает",
            color = accentColor.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontStyle = FontStyle.Italic
        )
    }
}

// НОВОЕ: только точки — для баннера внизу
@Composable
private fun DmTypingDotsRow(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "dm_dots")
    val dot1 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse, StartOffset(0)), label = "d1")
    val dot2 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse, StartOffset(140)), label = "d2")
    val dot3 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse, StartOffset(280)), label = "d3")

    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(dot1, dot2, dot3).forEach { alpha ->
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accentColor.copy(alpha = alpha)))
        }
    }
}

// ============================================================================================
// ПУСТОЙ ЭКРАН
// ============================================================================================

@Composable
private fun DmEmptyState(peerName: String, accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "dm_float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fy"
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .offset(y = floatY.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.12f))
                .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, null, tint = accentColor.copy(alpha = 0.5f), modifier = Modifier.size(34.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Личный чат с $peerName", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Сообщения видны только вам двоим", color = StardustTextSecondary, fontSize = 13.sp)
    }
}

// ============================================================================================
// ПУЗЫРЬ СООБЩЕНИЯ ЛС
// ============================================================================================

@Composable
fun DmMessageBubble(
    message: DirectMessage,
    isOwn: Boolean,
    accentColor: Color,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }
    val timeStr = message.timestamp?.let {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
    } ?: ""

    val topStart = if (isOwn) 20.dp else 4.dp
    val topEnd   = if (isOwn) 4.dp else 20.dp
    val bubbleShape = RoundedCornerShape(topStart = topStart, topEnd = topEnd, bottomStart = 20.dp, bottomEnd = 20.dp)

    val bubbleBg: Brush = if (isOwn) {
        Brush.linearGradient(listOf(accentColor, accentColor.copy(alpha = 0.7f)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f)))
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
            if (canDelete) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 80.dp, max = 280.dp)
                        .clip(bubbleShape)
                        .background(brush = bubbleBg)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { showDelete = !showDelete },
                                onLongPress = { showDelete = true }
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    BubbleContent(text = message.text, timeStr = timeStr, isOwn = isOwn, readBy = message.readBy)
                }
            } else {
                Box(
                    modifier = Modifier
                        .widthIn(min = 80.dp, max = 280.dp)
                        .clip(bubbleShape)
                        .background(brush = bubbleBg)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    BubbleContent(text = message.text, timeStr = timeStr, isOwn = isOwn, readBy = message.readBy)
                }
            }

            AnimatedVisibility(
                visible = showDelete && canDelete,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit  = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Surface(
                    onClick = { onDelete(); showDelete = false },
                    color = StardustError.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StardustError.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, null, tint = StardustError, modifier = Modifier.size(13.dp))
                        Text("Удалить", color = StardustError, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleContent(
    text: String,
    timeStr: String,
    isOwn: Boolean,
    readBy: List<String>
) {
    Column {
        Text(text = text, fontSize = 14.sp, lineHeight = 20.sp, color = if (isOwn) Color.White else StardustTextPrimary)
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            modifier = Modifier.align(Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = timeStr,
                fontSize = 10.sp,
                color = if (isOwn) Color.White.copy(alpha = 0.5f) else StardustTextSecondary.copy(alpha = 0.5f)
            )
            if (isOwn) {
                val isRead = readBy.size > 1
                Icon(
                    imageVector = if (isRead) Icons.Default.DoneAll else Icons.Default.Done,
                    contentDescription = null,
                    tint = if (isRead) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}