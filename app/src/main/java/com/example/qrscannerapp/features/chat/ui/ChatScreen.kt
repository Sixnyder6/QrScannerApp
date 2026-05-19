package com.example.qrscannerapp.features.chat.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.qrscannerapp.*
import com.example.qrscannerapp.common.ui.AnimatedDialogWrapper
import com.example.qrscannerapp.features.chat.domain.model.ChatMessage
import com.example.qrscannerapp.features.chat.domain.model.ChatRoom
import com.example.qrscannerapp.features.chat.domain.model.ChatRoomInfo
import com.example.qrscannerapp.features.chat.domain.model.MessageType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================================
// ЦВЕТА
// ============================================================================================

private fun roomGradient(room: ChatRoom): List<Color> = when (room) {
    ChatRoom.GENERAL  -> listOf(Color(0xFF6A5AE0), Color(0xFF9B8FFF))
    ChatRoom.MUVERS   -> listOf(Color(0xFF00BCD4), Color(0xFF4DD0E1))
    ChatRoom.MANAGERS -> listOf(Color(0xFF4CAF50), Color(0xFF81C784))
    ChatRoom.SHIFTS   -> listOf(Color(0xFFFF8F00), Color(0xFFFFCA28))
    ChatRoom.ALERTS   -> listOf(Color(0xFFF44336), Color(0xFFEF9A9A))
}

private fun roomAccent(room: ChatRoom): Color = when (room) {
    ChatRoom.GENERAL  -> Color(0xFF6A5AE0)
    ChatRoom.MUVERS   -> Color(0xFF00BCD4)
    ChatRoom.MANAGERS -> Color(0xFF4CAF50)
    ChatRoom.SHIFTS   -> Color(0xFFFFA726)
    ChatRoom.ALERTS   -> Color(0xFFF44336)
}

private fun roleColor(role: String): Color = when (role) {
    "admin"             -> Color(0xFFEC407A)
    "inventory_manager" -> Color(0xFF4CAF50)
    "muver"             -> Color(0xFF29B6F6)
    "electrician"       -> Color(0xFFFFCA28)
    "technic"           -> Color(0xFFAB47BC)
    "system"            -> Color(0xFF78909C)
    else                -> StardustTextSecondary
}

private fun roleLabel(role: String): String = when (role) {
    "admin"             -> "Админ"
    "inventory_manager" -> "Кладовщик"
    "muver"             -> "Мувёр"
    "electrician"       -> "Электрик"
    "technic"           -> "Техник"
    else                -> ""
}

// Набор доступных реакций
private val REACTION_EMOJIS = listOf("👍", "❤️", "😂", "😮", "😢", "🔥", "👏", "✅")

// ============================================================================================
// ПОЛНОЭКРАННЫЙ ПРОСМОТР ФОТО
// ============================================================================================

@Composable
fun FullscreenImageViewer(imageUrl: String, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += panChange
    }
    AnimatedDialogWrapper(onDismiss = onDismiss) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
            AsyncImage(
                model = imageUrl, contentDescription = "Фото", contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }
                    .transformable(state = transformState).clickable { }
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f))) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
            }
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { scale = if (scale > 1.5f) 1f else 2.5f; offset = Offset.Zero }, onTap = { onDismiss() })
            })
        }
    }
}

// ============================================================================================
// НОВОЕ: ЭМОДЗИ-ПИКЕР
// ============================================================================================

@Composable
fun EmojiPickerPopup(
    messageId: String,
    currentReactions: Map<String, List<String>>,
    currentUserId: String?,
    accent: Color,
    onReact: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 80.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = StardustModalBg,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Реакция",
                    color = StardustTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    REACTION_EMOJIS.forEach { emoji ->
                        val alreadyReacted = currentReactions[emoji]?.contains(currentUserId) == true
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (alreadyReacted) accent.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .border(
                                    width = if (alreadyReacted) 1.5.dp else 0.dp,
                                    color = if (alreadyReacted) accent.copy(alpha = 0.6f) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onReact(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================================
// ГЛАВНЫЙ ЭКРАН
// ============================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onOpenDirectChat: (peerId: String, peerName: String, peerRole: String) -> Unit = { _, _, _ -> },
    onOpenProfile: (userId: String, userName: String, userRole: String) -> Unit = { _, _, _ -> },
    onOpenInbox: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var messageToDelete by remember { mutableStateOf<ChatMessage?>(null) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var showAutoDeleteSheet by remember { mutableStateOf(false) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    val autoDeleteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentUserId = viewModel.currentUserId
    val isAdmin = viewModel.isAdmin
    val accent = roomAccent(uiState.activeRoom)

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.sendImageMessage(context, it) }
    }

    val isAtBottom by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index == 0 } }
    LaunchedEffect(isAtBottom) { if (isAtBottom) viewModel.onScrolledToBottom() else viewModel.onScrolledUp() }
    LaunchedEffect(uiState.isLoading) { if (!uiState.isLoading && uiState.messages.isNotEmpty()) listState.scrollToItem(0) }
    // ПАГИНАЦИЯ: триггер при скролле к старым сообщениям
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 5
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !uiState.isLoadingMore) viewModel.loadMoreMessages()
    }

    LaunchedEffect(uiState.currentSearchIndex, uiState.searchResults) {
        if (uiState.searchResults.isNotEmpty()) {
            val target = uiState.searchResults[uiState.currentSearchIndex]
            val idx = uiState.messages.indexOfFirst { it.id == target.id }
            if (idx >= 0) listState.animateScrollToItem(idx)
        }
    }

    fullscreenImageUrl?.let { url -> FullscreenImageViewer(imageUrl = url, onDismiss = { fullscreenImageUrl = null }) }

    messageToDelete?.let { msg ->
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("Удалить сообщение?", color = StardustTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                val preview = if (msg.type == MessageType.IMAGE) "📷 Фото" else "\"${msg.text.take(60)}${if (msg.text.length > 60) "..." else ""}\""
                Text(preview, color = StardustTextSecondary, fontSize = 14.sp, fontStyle = FontStyle.Italic)
            },
            confirmButton = {
                Button(onClick = { viewModel.deleteMessage(msg); messageToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = StardustError), shape = RoundedCornerShape(10.dp)) { Text("Удалить", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { messageToDelete = null }) { Text("Отмена", color = StardustTextSecondary) } },
            containerColor = StardustModalBg, shape = RoundedCornerShape(20.dp)
        )
    }

    if (showDeleteSelectedDialog) {
        val count = uiState.selectedMessageIds.size
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text("Удалить $count сообщений?", color = StardustTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Это действие нельзя отменить.", color = StardustTextSecondary, fontSize = 14.sp) },
            confirmButton = {
                Button(onClick = { viewModel.deleteSelectedMessages(); showDeleteSelectedDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = StardustError), shape = RoundedCornerShape(10.dp)) { Text("Удалить", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDeleteSelectedDialog = false }) { Text("Отмена", color = StardustTextSecondary) } },
            containerColor = StardustModalBg, shape = RoundedCornerShape(20.dp)
        )
    }

    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("Очистить весь чат?", color = StardustTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Все сообщения в «${uiState.activeRoom.displayName}» будут удалены безвозвратно.", color = StardustTextSecondary, fontSize = 14.sp) },
            confirmButton = {
                Button(onClick = { viewModel.clearEntireRoom(); showClearChatDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = StardustError), shape = RoundedCornerShape(10.dp)) { Text("Очистить", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showClearChatDialog = false }) { Text("Отмена", color = StardustTextSecondary) } },
            containerColor = StardustModalBg, shape = RoundedCornerShape(20.dp)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(StardustSolidBg)) {

            if (uiState.isSelectionMode) {
                SelectionTopBar(
                    selectedCount = uiState.selectedMessageIds.size,
                    totalDeletable = uiState.messages.count { it.senderId == currentUserId || isAdmin },
                    accent = accent, onClose = { viewModel.exitSelectionMode() },
                    onSelectAll = { viewModel.selectAllMessages() }, onDelete = { showDeleteSelectedDialog = true }
                )
            } else {
                ChatTopBar(
                    activeRoom = uiState.activeRoom, messageCount = uiState.messages.size,
                    accent = accent, isSearchActive = uiState.isSearchActive,
                    isAutoDeleteEnabled = uiState.isAutoDeleteEnabled, autoDeleteHours = uiState.autoDeleteIntervalHours,
                    isAdmin = isAdmin, isClearingChat = uiState.isClearingChat,
                    onBack = onBack,
                    onSearchClick = { viewModel.toggleSearch() }, onMembersClick = { viewModel.toggleRoomMenu() },
                    onAutoDeleteClick = { showAutoDeleteSheet = true }, onClearChatClick = { showClearChatDialog = true },
                    onOpenInbox = onOpenInbox
                )
            }

            RoomChipsRow(rooms = uiState.availableRooms, activeRoom = uiState.activeRoom,
                onRoomSelect = { viewModel.exitSelectionMode(); viewModel.selectRoom(it) })

            AnimatedVisibility(visible = uiState.isSearchActive && !uiState.isSelectionMode, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                SearchBar(query = uiState.searchQuery, resultCount = uiState.searchResults.size,
                    currentIndex = uiState.currentSearchIndex, accent = accent,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    onNext = { viewModel.searchNext() }, onPrev = { viewModel.searchPrev() }, onClose = { viewModel.clearSearch() })
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

            // НОВОЕ: баннер закреплённого сообщения
            uiState.pinnedMessage?.let { pinned ->
                PinnedMessageBanner(
                    message = pinned,
                    accent = accent,
                    isAdmin = isAdmin,
                    onUnpin = { viewModel.unpinMessage() },
                    onTap = {
                        // Скролл к закреплённому сообщению если оно есть в списке
                        val idx = uiState.messages.indexOfFirst { it.id == pinned.id }
                        if (idx >= 0) scope.launch { listState.animateScrollToItem(idx) }
                    }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = accent, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Загружаем сообщения...", color = StardustTextSecondary, fontSize = 13.sp)
                        }
                    }
                    uiState.isClearingChat -> {
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = StardustError, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Очищаем чат...", color = StardustTextSecondary, fontSize = 13.sp)
                        }
                    }
                    uiState.messages.isEmpty() -> EmptyChatPlaceholder(room = uiState.activeRoom, accent = accent)
                    else -> {
                        val groupedEntries = remember(uiState.messages) {
                            uiState.messages
                                .groupBy { msg -> msg.timestamp?.let { SimpleDateFormat("dd MMMM yyyy", Locale("ru")).format(it) } ?: "Сегодня" }
                                .entries.toList()
                                .sortedByDescending { (_, msgs) -> msgs.firstOrNull()?.timestamp?.time ?: 0L }
                        }
                        val highlightedIds = remember(uiState.searchResults) { uiState.searchResults.map { it.id }.toSet() }
                        val activeHighlightId = uiState.searchResults.getOrNull(uiState.currentSearchIndex)?.id

                        LazyColumn(
                            state = listState, modifier = Modifier.fillMaxSize(), reverseLayout = true,
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            groupedEntries.forEach { (date, msgs) ->
                                items(msgs, key = { it.id }) { message ->
                                    val canDelete = message.senderId == currentUserId || isAdmin
                                    AnimatedMessageBubble(
                                        message = message,
                                        isOwn = message.senderId == currentUserId,
                                        isAdmin = isAdmin,
                                        isSelectionMode = uiState.isSelectionMode,
                                        isSelected = message.id in uiState.selectedMessageIds,
                                        canSelect = canDelete,
                                        accent = accent, room = uiState.activeRoom,
                                        isHighlighted = message.id in highlightedIds,
                                        isActiveResult = message.id == activeHighlightId,
                                        searchQuery = uiState.searchQuery,
                                        currentUserId = currentUserId,
                                        onReply = { viewModel.setReplyTo(message) },
                                        onDelete = { messageToDelete = message },
                                        // НОВОЕ: лонгпресс → эмодзи-пикер (если не режим выделения)
                                        onLongPress = {
                                            if (!uiState.isSelectionMode) {
                                                viewModel.showEmojiPicker(message.id)
                                            } else if (canDelete) {
                                                viewModel.enterSelectionMode(message.id)
                                            }
                                        },
                                        onTapInSelectionMode = { if (canDelete) viewModel.toggleMessageSelection(message.id) },
                                        onImageClick = { url -> fullscreenImageUrl = url },
                                        onOpenProfile = onOpenProfile,
                                        // НОВОЕ: клик на реакцию = toggle
                                        onReactionClick = { emoji -> viewModel.toggleReaction(message.id, emoji) },
                                        onPin = { viewModel.pinMessage(message) }
                                    )
                                }
                                item(key = "date_$date") { DateSeparator(date = date) }
                            }
                            // ПАГИНАЦИЯ: индикатор внизу (самые старые сообщения)
                            if (uiState.isLoadingMore) {
                                item(key = "loading_more") {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                val uploadAlpha by animateFloatAsState(targetValue = if (uiState.isUploadingImage) 1f else 0f, animationSpec = tween(200), label = "upload_alpha")
                if (uiState.isUploadingImage || uploadAlpha > 0f) {
                    Surface(
                        color = StardustModalBg.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { alpha = uploadAlpha }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Text("Загружаем фото...", color = StardustTextSecondary, fontSize = 13.sp)
                        }
                    }
                }

                val scrollBtnAlpha by animateFloatAsState(targetValue = if (uiState.showScrollToBottom && !uiState.isSelectionMode) 1f else 0f, animationSpec = tween(200), label = "scroll_alpha")
                val scrollBtnScale by animateFloatAsState(targetValue = if (uiState.showScrollToBottom) 1f else 0.7f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scroll_scale")
                if (scrollBtnAlpha > 0f) {
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 8.dp).graphicsLayer { alpha = scrollBtnAlpha; scaleX = scrollBtnScale; scaleY = scrollBtnScale }) {
                        FloatingActionButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }, modifier = Modifier.size(42.dp), shape = CircleShape, containerColor = accent, contentColor = Color.White, elevation = FloatingActionButtonDefaults.elevation(4.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, "Вниз", modifier = Modifier.size(22.dp))
                        }
                        if (uiState.unreadBelowCount > 0) {
                            Badge(containerColor = StardustError, modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)) {
                                Text(if (uiState.unreadBelowCount > 99) "99+" else uiState.unreadBelowCount.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // НОВОЕ: эмодзи-пикер поверх списка
                val pickerMessageId = uiState.emojiPickerMessageId
                if (pickerMessageId != null) {
                    val pickerMessage = uiState.messages.find { it.id == pickerMessageId }
                    EmojiPickerPopup(
                        messageId = pickerMessageId,
                        currentReactions = pickerMessage?.reactions ?: emptyMap(),
                        currentUserId = currentUserId,
                        accent = accent,
                        onReact = { emoji -> viewModel.toggleReaction(pickerMessageId, emoji) },
                        onDismiss = { viewModel.hideEmojiPicker() }
                    )
                }
            }

            AnimatedVisibility(visible = uiState.typingUsers.isNotEmpty() && !uiState.isSelectionMode, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                TypingIndicator(names = uiState.typingUsers, accent = accent)
            }

            AnimatedVisibility(visible = uiState.replyTo != null && !uiState.isSelectionMode, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                uiState.replyTo?.let { reply -> ReplyPreviewBar(reply = reply, accent = accent, onDismiss = { viewModel.clearReply() }) }
            }

            AnimatedVisibility(visible = !uiState.isSelectionMode, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                ChatInputBar(
                    text = inputText,
                    onTextChange = { inputText = it; if (it.isNotBlank()) viewModel.onTyping() },
                    onSend = { if (inputText.isNotBlank()) { viewModel.sendMessage(inputText); inputText = ""; focusManager.clearFocus() } },
                    onImageClick = { imagePickerLauncher.launch("image/*") },
                    isUploadingImage = uiState.isUploadingImage, accent = accent,
                    placeholder = "Сообщение в ${uiState.activeRoom.displayName}..."
                )
            }
        }

        // Панель участников
        val menuAlpha by animateFloatAsState(targetValue = if (uiState.isRoomMenuOpen) 1f else 0f, animationSpec = tween(200), label = "menu_alpha")
        val menuOffset by animateFloatAsState(targetValue = if (uiState.isRoomMenuOpen) 0f else 1f, animationSpec = tween(250), label = "menu_offset")
        if (menuAlpha > 0f) {
            Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = menuAlpha }.background(Color.Black.copy(alpha = 0.5f)).clickable { viewModel.closeRoomMenu() })
            Box(modifier = Modifier.align(Alignment.CenterEnd).graphicsLayer { alpha = menuAlpha; translationX = menuOffset * 280.dp.toPx() }) {
                RoomMembersPanel(room = uiState.activeRoom, members = uiState.roomMembers, isLoading = uiState.isMembersLoading, accent = accent,
                    currentUserId = viewModel.currentUserId, onClose = { viewModel.closeRoomMenu() },
                    onOpenDm = { member -> viewModel.closeRoomMenu(); onOpenDirectChat(member.userId, member.displayName, member.role) })
            }
        }
    }

    if (showAutoDeleteSheet) {
        AutoDeleteSheet(sheetState = autoDeleteSheetState, isEnabled = uiState.isAutoDeleteEnabled, currentHours = uiState.autoDeleteIntervalHours,
            onDismiss = { scope.launch { autoDeleteSheetState.hide() }.invokeOnCompletion { showAutoDeleteSheet = false } },
            onSave = { enabled, hours -> viewModel.setAutoDelete(enabled, hours); scope.launch { autoDeleteSheetState.hide() }.invokeOnCompletion { showAutoDeleteSheet = false } },
            onDeleteNow = { viewModel.runAutoDeleteNow(); scope.launch { autoDeleteSheetState.hide() }.invokeOnCompletion { showAutoDeleteSheet = false } })
    }
}

// ============================================================================================
// ТОПБАР ВЫДЕЛЕНИЯ
// ============================================================================================

@Composable
fun SelectionTopBar(selectedCount: Int, totalDeletable: Int, accent: Color, onClose: () -> Unit, onSelectAll: () -> Unit, onDelete: () -> Unit) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(StardustError))
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A22)).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, null, tint = StardustTextPrimary, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Выбрано: $selectedCount", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onSelectAll, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.SelectAll, null, tint = if (selectedCount == totalDeletable) accent else StardustTextSecondary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp), enabled = selectedCount > 0) {
                Icon(Icons.Default.Delete, null, tint = if (selectedCount > 0) StardustError else StardustTextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ============================================================================================
// ОБЫЧНЫЙ ТОПБАР
// ============================================================================================

@Composable
fun ChatTopBar(activeRoom: ChatRoom, messageCount: Int, accent: Color, isSearchActive: Boolean, isAutoDeleteEnabled: Boolean, autoDeleteHours: Int, isAdmin: Boolean, isClearingChat: Boolean, onBack: () -> Unit = {}, onSearchClick: () -> Unit, onMembersClick: () -> Unit, onAutoDeleteClick: () -> Unit, onClearChatClick: () -> Unit, onOpenInbox: () -> Unit = {}) {
    val gradient = roomGradient(activeRoom)
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Brush.horizontalGradient(gradient)))
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A22)).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = StardustTextSecondary, modifier = Modifier.size(20.dp)) }
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(gradient)).clickable { onMembersClick() }, contentAlignment = Alignment.Center) { Text(activeRoom.emoji, fontSize = 20.sp) }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(activeRoom.displayName, color = StardustTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val infiniteTransition = rememberInfiniteTransition(label = "live")
                    val liveAlpha by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "la")
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accent.copy(alpha = liveAlpha)))
                    Text(if (messageCount > 0) "$messageCount сообщений" else "нет сообщений", color = accent.copy(alpha = 0.8f), fontSize = 12.sp)
                    if (isAutoDeleteEnabled) { Icon(Icons.Default.Timer, null, tint = StardustWarning.copy(alpha = 0.8f), modifier = Modifier.size(11.dp)); Text("${autoDeleteHours}ч", color = StardustWarning.copy(alpha = 0.8f), fontSize = 10.sp) }
                }
            }
            if (isAdmin) { IconButton(onClick = onClearChatClick, modifier = Modifier.size(36.dp), enabled = !isClearingChat) { if (isClearingChat) CircularProgressIndicator(color = StardustError, strokeWidth = 2.dp, modifier = Modifier.size(16.dp)) else Icon(Icons.Default.DeleteSweep, null, tint = StardustError.copy(alpha = 0.7f), modifier = Modifier.size(19.dp)) } }
            IconButton(onClick = onAutoDeleteClick, modifier = Modifier.size(36.dp)) { Icon(if (isAutoDeleteEnabled) Icons.Default.Timer else Icons.Default.TimerOff, null, tint = if (isAutoDeleteEnabled) StardustWarning else StardustTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(19.dp)) }
            IconButton(onClick = onOpenInbox, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Inbox, null, tint = StardustTextSecondary.copy(alpha = 0.7f), modifier = Modifier.size(19.dp)) }
            IconButton(onClick = onSearchClick, modifier = Modifier.size(36.dp)) { Icon(if (isSearchActive) Icons.Default.SearchOff else Icons.Default.Search, null, tint = if (isSearchActive) accent else StardustTextSecondary.copy(alpha = 0.7f), modifier = Modifier.size(19.dp)) }
            IconButton(onClick = onMembersClick, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.PeopleAlt, null, tint = StardustTextSecondary.copy(alpha = 0.7f), modifier = Modifier.size(19.dp)) }
        }
    }
}

// ============================================================================================
// СТРОКА КОМНАТ
// ============================================================================================

@Composable
fun RoomChipsRow(rooms: List<ChatRoomInfo>, activeRoom: ChatRoom, onRoomSelect: (ChatRoom) -> Unit) {
    LazyRow(modifier = Modifier.fillMaxWidth().background(Color(0xFF16161E)).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rooms, key = { it.room.id }) { roomInfo ->
            val isSelected = activeRoom == roomInfo.room
            val accent = roomAccent(roomInfo.room)
            val gradient = roomGradient(roomInfo.room)
            val animBg by animateColorAsState(if (isSelected) accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f), tween(200), label = "bg")
            val animText by animateColorAsState(if (isSelected) accent else StardustTextSecondary, tween(200), label = "tx")
            val chipScale by animateFloatAsState(if (isSelected) 1f else 0.96f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "sc")
            Box(modifier = Modifier.scale(chipScale)) {
                Surface(onClick = { onRoomSelect(roomInfo.room) }, shape = RoundedCornerShape(20.dp), color = animBg, border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.6f)) else null, modifier = Modifier.height(32.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(roomInfo.room.emoji, fontSize = 13.sp)
                        Text(roomInfo.room.displayName, color = animText, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp)
                        if (roomInfo.unreadCount > 0) {
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(if (roomInfo.hasUnreadMention) Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))) else Brush.linearGradient(gradient)), contentAlignment = Alignment.Center) {
                                Text(if (roomInfo.unreadCount > 9) "9+" else roomInfo.unreadCount.toString(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================================
// ШТОРКА АВТОУДАЛЕНИЯ
// ============================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDeleteSheet(sheetState: SheetState, isEnabled: Boolean, currentHours: Int, onDismiss: () -> Unit, onSave: (Boolean, Int) -> Unit, onDeleteNow: () -> Unit) {
    var enabled by remember { mutableStateOf(isEnabled) }
    var selectedHours by remember { mutableIntStateOf(currentHours) }
    val options = listOf(1 to "1 час", 6 to "6 часов", 12 to "12 часов", 24 to "24 часа", 48 to "2 дня", 168 to "7 дней")
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = StardustModalBg, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = StardustWarning, modifier = Modifier.size(22.dp)); Spacer(modifier = Modifier.width(10.dp))
                Text("Автоудаление сообщений", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StardustWarning))
            }
            Text("Сообщения старше указанного времени будут автоматически удалены во всех комнатах к которым у вас есть доступ.", color = StardustTextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
            AnimatedVisibility(visible = enabled) {
                Column {
                    Text("Удалять сообщения старше:", color = StardustTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp); Spacer(modifier = Modifier.height(10.dp))
                    options.chunked(3).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (hours, label) ->
                                val isSelected = selectedHours == hours
                                Surface(onClick = { selectedHours = hours }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp), color = if (isSelected) StardustWarning.copy(alpha = 0.2f) else StardustItemBg, border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, StardustWarning.copy(alpha = 0.7f)) else null) {
                                    Box(contentAlignment = Alignment.Center) { Text(label, color = if (isSelected) StardustWarning else StardustTextSecondary, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp) }
                                }
                            }
                            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                    OutlinedButton(onClick = onDeleteNow, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = StardustError), border = androidx.compose.foundation.BorderStroke(1.dp, StardustError.copy(alpha = 0.4f))) {
                        Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Удалить старые сейчас", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            Button(onClick = { onSave(enabled, selectedHours) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = if (enabled) StardustWarning else StardustPrimary)) {
                Text(if (enabled) "Включить на $selectedHours ч" else "Сохранить", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (enabled) Color.Black else Color.White)
            }
        }
    }
}

// ============================================================================================
// ПОИСК
// ============================================================================================

@Composable
fun SearchBar(query: String, resultCount: Int, currentIndex: Int, accent: Color, onQueryChange: (String) -> Unit, onNext: () -> Unit, onPrev: () -> Unit, onClose: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(modifier = Modifier.fillMaxWidth().background(StardustSolidBg).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Search, null, tint = accent, modifier = Modifier.size(18.dp))
        OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.weight(1f).focusRequester(focusRequester), placeholder = { Text("Поиск в чате...", fontSize = 13.sp, color = StardustTextSecondary.copy(alpha = 0.5f)) }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onNext() }), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent.copy(alpha = 0.5f), unfocusedBorderColor = Color.White.copy(alpha = 0.1f), focusedContainerColor = Color.White.copy(alpha = 0.05f), unfocusedContainerColor = Color.White.copy(alpha = 0.03f), cursorColor = accent, focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextPrimary), shape = RoundedCornerShape(12.dp), textStyle = TextStyle(fontSize = 13.sp))
        if (resultCount > 0) {
            Text("${currentIndex + 1}/$resultCount", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 36.dp), textAlign = TextAlign.Center)
            IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.KeyboardArrowUp, null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp)) }
            IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.KeyboardArrowDown, null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp)) }
        } else if (query.isNotBlank()) { Text("0", color = StardustTextSecondary, fontSize = 12.sp, modifier = Modifier.widthIn(min = 36.dp), textAlign = TextAlign.Center) }
        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp)) }
    }
    HorizontalDivider(color = accent.copy(alpha = 0.2f))
}

// ============================================================================================
// TYPING
// ============================================================================================

@Composable
fun TypingIndicator(names: List<String>, accent: Color) {
    if (names.isEmpty()) return
    val typingText = when (names.size) { 1 -> "${names[0]} печатает"; 2 -> "${names[0]} и ${names[1]} печатают"; else -> "${names[0]} и ещё ${names.size - 1} печатают" }
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dot1 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse, StartOffset(0)), label = "d1")
    val dot2 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse, StartOffset(150)), label = "d2")
    val dot3 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse, StartOffset(300)), label = "d3")
    Row(modifier = Modifier.fillMaxWidth().background(StardustSolidBg).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) { listOf(dot1, dot2, dot3).forEach { alpha -> Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accent.copy(alpha = alpha))) } }
        Text(typingText, color = StardustTextSecondary, fontSize = 12.sp, fontStyle = FontStyle.Italic)
    }
}

// ============================================================================================
// ПАНЕЛЬ УЧАСТНИКОВ
// ============================================================================================

@Composable
fun RoomMembersPanel(room: ChatRoom, members: List<RoomMember>, isLoading: Boolean, accent: Color, currentUserId: String?, onClose: () -> Unit, onOpenDm: (RoomMember) -> Unit) {
    val gradient = roomGradient(room)
    val onlineCount = members.count { it.isOnline }
    Box(modifier = Modifier.fillMaxHeight().width(280.dp).background(Brush.verticalGradient(listOf(Color(0xFF1E1E28), Color(0xFF16161E)))).border(1.dp, Brush.verticalGradient(listOf(accent.copy(alpha = 0.3f), Color.Transparent)), RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)).clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(gradient.map { it.copy(alpha = 0.2f) })).padding(16.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Участники", color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp)) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        val pulseTransition = rememberInfiniteTransition(label = "online_pulse")
                        val pulseAlpha by pulseTransition.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pa")
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) { Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF4CAF50).copy(alpha = pulseAlpha))); Text("$onlineCount онлайн", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        Text("·", color = StardustTextSecondary.copy(alpha = 0.4f)); Text("${members.size} всего", color = StardustTextSecondary, fontSize = 12.sp)
                    }
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            if (isLoading) { Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp)) } }
            else {
                val online = members.filter { it.isOnline }; val offline = members.filter { !it.isOnline }
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                    if (online.isNotEmpty()) { Text("ОНЛАЙН — ${online.size}", color = Color(0xFF4CAF50).copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)); online.forEach { member -> MemberRow(member = member, accent = accent, isSelf = member.userId == currentUserId, onOpenDm = { onOpenDm(member) }) } }
                    if (offline.isNotEmpty()) { Text("ОФФЛАЙН — ${offline.size}", color = StardustTextSecondary.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(start = 16.dp, top = if (online.isNotEmpty()) 12.dp else 8.dp, bottom = 4.dp)); offline.forEach { member -> MemberRow(member = member, accent = accent, isSelf = member.userId == currentUserId, onOpenDm = { onOpenDm(member) }) } }
                    if (members.isEmpty()) { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Нет участников", color = StardustTextSecondary, fontSize = 13.sp) } }
                }
            }
        }
    }
}

// ============================================================================================
// СТРОКА УЧАСТНИКА
// ============================================================================================

@Composable
fun MemberRow(member: RoomMember, accent: Color, isSelf: Boolean = false, onOpenDm: () -> Unit = {}) {
    val roleClr = roleColor(member.role); val label = roleLabel(member.role); val now = System.currentTimeMillis()
    Row(modifier = Modifier.fillMaxWidth().then(if (!isSelf) Modifier.clickable { onOpenDm() } else Modifier).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.size(40.dp)) {
            if (!member.avatarUrl.isNullOrBlank()) { AsyncImage(model = member.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(36.dp).clip(CircleShape)) }
            else { val initials = member.displayName.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }.joinToString("").ifBlank { "?" }; Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Brush.linearGradient(listOf(roleClr.copy(alpha = 0.35f), roleClr.copy(alpha = 0.15f)))), contentAlignment = Alignment.Center) { Text(initials, color = roleClr, fontSize = if (initials.length > 1) 12.sp else 15.sp, fontWeight = FontWeight.ExtraBold) } }
            Box(modifier = Modifier.size(11.dp).align(Alignment.BottomEnd).clip(CircleShape).background(StardustSolidBg).padding(1.5.dp).clip(CircleShape).background(if (member.isOnline) Color(0xFF4CAF50) else Color(0xFF4A4A58)))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(member.displayName, color = if (member.isOnline) StardustTextPrimary else StardustTextSecondary, fontSize = 13.sp, fontWeight = if (member.isOnline) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (isSelf) { Surface(color = accent.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) { Text("вы", color = accent.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)) } }
            }
            if (label.isNotBlank()) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(roleClr.copy(alpha = 0.6f))); Text(label, color = roleClr.copy(alpha = 0.8f), fontSize = 10.sp) } }
        }
        if (!member.isOnline && member.lastSeen > 0) { val diff = now - member.lastSeen; Text(when { diff < 60_000L -> "сейчас"; diff < 3_600_000L -> "${diff / 60_000} мин"; diff < 86_400_000L -> "${diff / 3_600_000} ч"; else -> "${diff / 86_400_000} дн" }, color = StardustTextSecondary.copy(alpha = 0.45f), fontSize = 10.sp) }
        else if (member.isOnline) { val pt = rememberInfiniteTransition(label = "mp_${member.userId}"); val pAlpha by pt.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "mpa"); Text("онлайн", color = Color(0xFF4CAF50).copy(alpha = pAlpha), fontSize = 10.sp, fontWeight = FontWeight.Medium) }
        if (!isSelf) { Icon(Icons.Default.Chat, contentDescription = "Написать", tint = accent.copy(alpha = 0.45f), modifier = Modifier.size(15.dp)) }
    }
}

// ============================================================================================
// РАЗДЕЛИТЕЛЬ ДАТ
// ============================================================================================

@Composable
fun DateSeparator(date: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.08f)); Spacer(modifier = Modifier.width(12.dp))
        Surface(color = StardustItemBg, shape = RoundedCornerShape(10.dp)) { Text(date, color = StardustTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)) }
        Spacer(modifier = Modifier.width(12.dp)); HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.08f))
    }
}

// ============================================================================================
// НОВОЕ: ПОЛОСКА РЕАКЦИЙ ПОД ПУЗЫРЁМ
// ============================================================================================

@Composable
fun ReactionsRow(
    reactions: Map<String, List<String>>,
    currentUserId: String?,
    accent: Color,
    onReactionClick: (String) -> Unit
) {
    if (reactions.isEmpty()) return
    // Показываем только эмодзи у которых есть хотя бы один голос
    val nonEmpty = reactions.filter { it.value.isNotEmpty() }
    if (nonEmpty.isEmpty()) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        nonEmpty.forEach { (emoji, userIds) ->
            val isMine = userIds.contains(currentUserId)
            Surface(
                onClick = { onReactionClick(emoji) },
                shape = RoundedCornerShape(12.dp),
                color = if (isMine) accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                border = if (isMine) androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f)) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(emoji, fontSize = 13.sp)
                    if (userIds.size > 1) {
                        Text(
                            userIds.size.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMine) accent else StardustTextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================================
// ПУЗЫРЬ СООБЩЕНИЯ
// ============================================================================================

@Composable
fun AnimatedMessageBubble(
    message: ChatMessage, isOwn: Boolean, isAdmin: Boolean = false,
    isSelectionMode: Boolean = false, isSelected: Boolean = false, canSelect: Boolean = false,
    accent: Color, room: ChatRoom, isHighlighted: Boolean = false, isActiveResult: Boolean = false,
    searchQuery: String = "",
    currentUserId: String?,
    onReply: () -> Unit, onDelete: () -> Unit,
    onLongPress: () -> Unit = {}, onTapInSelectionMode: () -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onOpenProfile: (userId: String, userName: String, userRole: String) -> Unit = { _, _, _ -> },
    onReactionClick: (String) -> Unit = {},
    onPin: () -> Unit = {}
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val selectedBg by animateColorAsState(if (isSelected) accent.copy(alpha = 0.15f) else Color.Transparent, tween(150), label = "sel_bg")

    AnimatedVisibility(visible = appeared, enter = if (isOwn) slideInHorizontally { it / 3 } + fadeIn(tween(200)) else slideInHorizontally { -it / 3 } + fadeIn(tween(200))) {
        Box(modifier = Modifier.fillMaxWidth().background(selectedBg).then(if (isSelectionMode && canSelect) Modifier.clickable { onTapInSelectionMode() } else Modifier)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (isSelectionMode && canSelect) {
                    Box(modifier = Modifier.padding(start = 8.dp).size(22.dp).clip(CircleShape).background(if (isSelected) accent else Color.White.copy(alpha = 0.15f)).border(1.5.dp, if (isSelected) accent else Color.White.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                        if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    MessageBubble(
                        message = message, isOwn = isOwn, isAdmin = isAdmin, accent = accent, room = room,
                        isHighlighted = isHighlighted, isActiveResult = isActiveResult, searchQuery = searchQuery,
                        currentUserId = currentUserId,
                        onReply = if (isSelectionMode) ({}) else onReply,
                        onDelete = if (isSelectionMode) ({}) else onDelete,
                        onLongPress = onLongPress, isSelectionMode = isSelectionMode,
                        onImageClick = onImageClick, onOpenProfile = onOpenProfile,
                        onReactionClick = onReactionClick,
                        onPin = onPin
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage, isOwn: Boolean, isAdmin: Boolean = false, accent: Color, room: ChatRoom,
    isHighlighted: Boolean = false, isActiveResult: Boolean = false, searchQuery: String = "",
    isSelectionMode: Boolean = false,
    currentUserId: String?,
    onReply: () -> Unit, onDelete: () -> Unit,
    onLongPress: () -> Unit = {}, onImageClick: (String) -> Unit = {},
    onOpenProfile: (userId: String, userName: String, userRole: String) -> Unit = { _, _, _ -> },
    onReactionClick: (String) -> Unit = {},
    onPin: () -> Unit = {}
) {
    val isSystem = message.type == MessageType.SYSTEM || message.type == MessageType.ALERT
    val timeStr = message.timestamp?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: ""

    if (isSystem) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            val isAlert = message.type == MessageType.ALERT
            Surface(color = if (isAlert) StardustError.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.06f), shape = RoundedCornerShape(14.dp), border = if (isAlert) androidx.compose.foundation.BorderStroke(1.dp, StardustError.copy(alpha = 0.3f)) else null) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (isAlert) Icons.Default.Warning else Icons.Default.Info, null, tint = if (isAlert) StardustError else StardustTextSecondary, modifier = Modifier.size(15.dp))
                    Text(message.text, color = if (isAlert) StardustError.copy(alpha = 0.9f) else StardustTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(timeStr, color = StardustTextSecondary.copy(alpha = 0.4f), fontSize = 10.sp)
                }
            }
        }
        return
    }

    var showActions by remember { mutableStateOf(false) }
    val roleClr = roleColor(message.senderRole)
    val gradient = roomGradient(room)
    val highlightAlpha by animateFloatAsState(if (isActiveResult) 1f else if (isHighlighted) 0.4f else 0f, label = "hl")

    Column(
        modifier = Modifier.fillMaxWidth()
            .then(if (highlightAlpha > 0f) Modifier.background(accent.copy(alpha = highlightAlpha * 0.12f), RoundedCornerShape(12.dp)) else Modifier)
            .padding(vertical = 3.dp),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        if (!isOwn) {
            Row(modifier = Modifier.padding(start = 6.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(Brush.linearGradient(listOf(roleClr.copy(alpha = 0.3f), roleClr.copy(alpha = 0.1f)))).then(if (!isSelectionMode && message.senderId.isNotBlank()) Modifier.clickable { onOpenProfile(message.senderId, message.senderName, message.senderRole) } else Modifier), contentAlignment = Alignment.Center) {
                    Text(message.senderName.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = roleClr, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(text = message.senderName, color = roleClr, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = if (!isSelectionMode && message.senderId.isNotBlank()) Modifier.clickable { onOpenProfile(message.senderId, message.senderName, message.senderRole) } else Modifier)
                val label = roleLabel(message.senderRole)
                if (label.isNotBlank()) { Surface(color = roleClr.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) { Text(label, color = roleClr.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)) } }
            }
        }

        val topStart = if (isOwn) 20.dp else 4.dp
        val topEnd = if (isOwn) 4.dp else 20.dp
        val bubbleShape = RoundedCornerShape(topStart = topStart, topEnd = topEnd, bottomStart = 20.dp, bottomEnd = 20.dp)

        if (message.type == MessageType.IMAGE && !message.imageUrl.isNullOrBlank()) {
            Box(modifier = Modifier.widthIn(min = 120.dp, max = 260.dp).clip(bubbleShape).then(if (isActiveResult) Modifier.border(1.5.dp, accent, bubbleShape) else Modifier).then(if (!isSelectionMode) Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onImageClick(message.imageUrl) }, onLongPress = { onLongPress() }) } else Modifier)) {
                Column {
                    AsyncImage(model = message.imageThumbnailUrl ?: message.imageUrl, contentDescription = "Фото", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 220.dp).then(if (isOwn) Modifier.background(Brush.linearGradient(gradient)) else Modifier.background(Color.White.copy(alpha = 0.08f))))
                    Box(modifier = Modifier.fillMaxWidth().then(if (isOwn) Modifier.background(Brush.linearGradient(gradient)) else Modifier.background(Color.White.copy(alpha = 0.08f))).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        if (message.text.isNotBlank()) { Text(message.text, fontSize = 13.sp, color = if (isOwn) Color.White else StardustTextPrimary, modifier = Modifier.align(Alignment.CenterStart).padding(end = 44.dp)) }
                        Text(timeStr, fontSize = 10.sp, color = if (isOwn) Color.White.copy(alpha = 0.5f) else StardustTextSecondary.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.BottomEnd))
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.widthIn(min = 80.dp, max = 290.dp).clip(bubbleShape)
                    .then(if (isOwn) Modifier.background(Brush.linearGradient(gradient)) else Modifier.background(Color.White.copy(alpha = 0.08f)))
                    .then(if (isActiveResult) Modifier.border(1.5.dp, accent, bubbleShape) else Modifier)
                    .then(if (!isSelectionMode) Modifier.pointerInput(Unit) { detectTapGestures(onTap = { showActions = !showActions }, onLongPress = { onLongPress() }) } else Modifier)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    if (!message.replyToText.isNullOrBlank()) {
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.2f)).padding(start = 0.dp, top = 6.dp, end = 8.dp, bottom = 6.dp)) {
                            Box(modifier = Modifier.width(3.dp).fillMaxHeight().clip(CircleShape).background(if (isOwn) Color.White.copy(alpha = 0.6f) else accent))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column { Text(message.replyToSenderName ?: "", color = if (isOwn) Color.White.copy(alpha = 0.9f) else accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold); Text(message.replyToText, color = if (isOwn) Color.White.copy(alpha = 0.65f) else StardustTextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    val annotated = buildAnnotatedString {
                        val parts = message.text.split(Regex("(?=@\\S)"))
                        parts.forEach { part ->
                            when {
                                part.startsWith("@") -> { pushStyle(SpanStyle(color = if (isOwn) Color.White else StardustWarning, fontWeight = FontWeight.ExtraBold, background = if (isOwn) Color.White.copy(alpha = 0.2f) else StardustWarning.copy(alpha = 0.15f))); append(part); pop() }
                                searchQuery.isNotBlank() && part.contains(searchQuery, ignoreCase = true) -> {
                                    val lp = part.lowercase(); val lq = searchQuery.lowercase(); var start = 0
                                    while (true) {
                                        val idx = lp.indexOf(lq, start); if (idx == -1) { pushStyle(SpanStyle(color = if (isOwn) Color.White else StardustTextPrimary)); append(part.substring(start)); pop(); break }
                                        if (idx > start) { pushStyle(SpanStyle(color = if (isOwn) Color.White else StardustTextPrimary)); append(part.substring(start, idx)); pop() }
                                        pushStyle(SpanStyle(color = Color(0xFF1A1A1A), background = Color(0xFFFFEB3B), fontWeight = FontWeight.Bold)); append(part.substring(idx, idx + searchQuery.length)); pop()
                                        start = idx + searchQuery.length
                                    }
                                }
                                else -> { pushStyle(SpanStyle(color = if (isOwn) Color.White else StardustTextPrimary)); append(part); pop() }
                            }
                        }
                    }
                    Text(annotated, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(timeStr, color = if (isOwn) Color.White.copy(alpha = 0.5f) else StardustTextSecondary.copy(alpha = 0.5f), fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
                }
            }
        }

        // НОВОЕ: полоска реакций под пузырём
        if (message.reactions.isNotEmpty()) {
            ReactionsRow(
                reactions = message.reactions,
                currentUserId = currentUserId,
                accent = accent,
                onReactionClick = onReactionClick
            )
        }

        if (!isSelectionMode) {
            AnimatedVisibility(visible = showActions, enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(), exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()) {
                Row(modifier = Modifier.padding(top = 4.dp, start = 6.dp, end = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (message.type != MessageType.IMAGE) {
                        Surface(onClick = { onReply(); showActions = false }, color = StardustItemBg, shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) { Icon(Icons.Default.Reply, null, tint = StardustTextSecondary, modifier = Modifier.size(13.dp)); Text("Ответить", color = StardustTextSecondary, fontSize = 12.sp) }
                        }
                    }
                    // НОВОЕ: закрепить — только для админа
                    if (isAdmin) {
                        Surface(onClick = { onPin(); showActions = false }, color = StardustItemBg, shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) { Icon(Icons.Default.PushPin, null, tint = accent, modifier = Modifier.size(13.dp)); Text("Закрепить", color = accent, fontSize = 12.sp) }
                        }
                    }
                    if (isOwn || isAdmin) {
                        Surface(onClick = { onDelete(); showActions = false }, color = StardustError.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, StardustError.copy(alpha = 0.3f))) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) { Icon(Icons.Default.DeleteOutline, null, tint = StardustError, modifier = Modifier.size(13.dp)); Text("Удалить", color = StardustError, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================================
// ПАНЕЛЬ ОТВЕТА
// ============================================================================================

@Composable
fun ReplyPreviewBar(reply: ChatMessage, accent: Color, onDismiss: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(accent.copy(alpha = 0.08f)).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(3.dp).height(38.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.3f)))))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("↩ ${reply.senderName}", color = accent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            val preview = if (reply.type == MessageType.IMAGE) "📷 Фото" else reply.text.take(70)
            Text(preview, color = StardustTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f))) { Icon(Icons.Default.Close, null, tint = StardustTextSecondary, modifier = Modifier.size(16.dp)) }
    }
}

// ============================================================================================
// ПОЛЕ ВВОДА
// ============================================================================================

@Composable
fun ChatInputBar(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit, onImageClick: () -> Unit, isUploadingImage: Boolean, accent: Color, placeholder: String) {
    val canSend = text.isNotBlank() && !isUploadingImage
    val sendScale by animateFloatAsState(if (canSend) 1f else 0.85f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "send")
    Row(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, StardustSolidBg))).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(StardustItemBg).clickable(enabled = !isUploadingImage) { onImageClick() }, contentAlignment = Alignment.Center) {
            if (isUploadingImage) CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            else Icon(Icons.Default.AttachFile, "Прикрепить фото", tint = StardustTextSecondary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        }
        OutlinedTextField(value = text, onValueChange = onTextChange, modifier = Modifier.weight(1f), placeholder = { Text(placeholder, color = StardustTextSecondary.copy(alpha = 0.5f), fontSize = 14.sp) }, maxLines = 5, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default), shape = RoundedCornerShape(24.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent.copy(alpha = 0.7f), unfocusedBorderColor = Color.White.copy(alpha = 0.1f), focusedContainerColor = Color.White.copy(alpha = 0.07f), unfocusedContainerColor = Color.White.copy(alpha = 0.04f), cursorColor = accent, focusedTextColor = StardustTextPrimary, unfocusedTextColor = StardustTextPrimary), textStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp))
        Box(modifier = Modifier.size(48.dp).scale(sendScale).clip(CircleShape).background(if (canSend) Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.6f))) else Brush.linearGradient(listOf(StardustItemBg, StardustItemBg))).clickable(enabled = canSend) { onSend() }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.Send, "Отправить", tint = if (canSend) Color.White else StardustTextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
    }
}

// ============================================================================================
// EMPTY STATE
// ============================================================================================

@Composable
fun EmptyChatPlaceholder(room: ChatRoom, accent: Color) {
    val gradient = roomGradient(room)
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatY by infiniteTransition.animateFloat(-8f, 8f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "fy")
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(100.dp).offset(y = floatY.dp).clip(RoundedCornerShape(32.dp)).background(Brush.linearGradient(gradient.map { it.copy(alpha = 0.2f) })).border(1.dp, Brush.linearGradient(gradient.map { it.copy(alpha = 0.4f) }), RoundedCornerShape(32.dp)), contentAlignment = Alignment.Center) { Text(room.emoji, fontSize = 48.sp) }
            Spacer(modifier = Modifier.height(28.dp))
            Text(room.displayName, color = StardustTextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp); Spacer(modifier = Modifier.height(8.dp))
            Text("Здесь пока тихо", color = StardustTextSecondary, fontSize = 15.sp); Spacer(modifier = Modifier.height(4.dp))
            Text("Напиши первым 👇", color = accent.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Medium); Spacer(modifier = Modifier.height(32.dp))
            Surface(color = accent.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HintRow("↩", "Нажми на сообщение чтобы ответить", accent)
                    HintRow("🔍", "Поиск по иконке лупы в топбаре", accent)
                    HintRow("👥", "Нажми на участника — написать лично", accent)
                    HintRow("@", "Упомяни коллегу через @Имя", accent)
                    HintRow("📎", "Скрепка — отправить фото", accent)
                    HintRow("☑", "Зажми сообщение чтобы выделить несколько", accent)
                    HintRow("😊", "Зажми сообщение чтобы поставить реакцию", accent)
                }
            }
        }
    }
}

@Composable
private fun HintRow(icon: String, text: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(icon, fontSize = 14.sp, color = accent, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
        Text(text, color = StardustTextSecondary, fontSize = 12.sp)
    }
}

// ============================================================================================
// НОВОЕ: БАННЕР ЗАКРЕПЛЁННОГО СООБЩЕНИЯ
// ============================================================================================

@Composable
fun PinnedMessageBanner(
    message: ChatMessage,
    accent: Color,
    isAdmin: Boolean,
    onUnpin: () -> Unit,
    onTap: () -> Unit
) {
    val preview = when {
        message.type == MessageType.IMAGE -> "📷 Фото"
        message.text.isNotBlank() -> message.text.take(80)
        else -> "Сообщение"
    }

    Surface(
        onClick = onTap,
        color = accent.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Вертикальная цветная полоска
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.4f))))
            )

            // Иконка булавки
            Icon(
                Icons.Default.PushPin,
                contentDescription = null,
                tint = accent.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )

            // Текст
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Закреплено",
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    preview,
                    color = StardustTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Кнопка снять закреп — только для админа
            if (isAdmin) {
                IconButton(
                    onClick = onUnpin,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Снять закреп",
                        tint = StardustTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
    HorizontalDivider(color = accent.copy(alpha = 0.15f))
}