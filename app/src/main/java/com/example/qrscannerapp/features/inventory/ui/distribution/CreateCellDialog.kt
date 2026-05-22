package com.example.qrscannerapp.features.inventory.ui.distribution

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.qrscannerapp.*
import com.example.qrscannerapp.features.inventory.domain.model.CellGroup
import com.example.qrscannerapp.features.inventory.domain.model.CellType

data class CreateCellData(
    val displayName: String?,
    val cellType: CellType?,
    val capacity: Int,
    val address: String?
)

@Composable
fun CreateCellDialog(
    onDismiss: () -> Unit,
    onCreate: (CreateCellData) -> Unit
) {
    var displayName       by remember { mutableStateOf("") }
    var selectedCellType  by remember { mutableStateOf<CellType?>(null) }
    var capacityText      by remember { mutableStateOf("500") }
    var address           by remember { mutableStateOf("") }
    var showCapacityError by remember { mutableStateOf(false) }

    val capacity        = capacityText.toIntOrNull()
    val isCapacityValid = capacity != null && capacity in 1..9999

    // ── Анимация появления ───────────────────────────────────────────────
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val sheetScale by animateFloatAsState(
        targetValue   = if (visible) 1f else 0.90f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label         = "scale"
    )
    val sheetAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(220),
        label         = "alpha"
    )

    fun handleDismiss() { visible = false; onDismiss() }

    Dialog(
        onDismissRequest = { handleDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f * sheetAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null
                ) { handleDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.93f)
                    .wrapContentHeight()
                    .graphicsLayer { scaleX = sheetScale; scaleY = sheetScale; alpha = sheetAlpha }
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF1C1830), Color(0xFF12102A)),
                            start  = Offset(0f, 0f),
                            end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .drawBehind {
                        drawRect(
                            color   = StardustPrimary.copy(alpha = 0.45f),
                            topLeft = Offset(size.width * 0.15f, 0f),
                            size    = androidx.compose.ui.geometry.Size(size.width * 0.7f, 1.5.dp.toPx())
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null
                    ) { }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {

                    // ── Хэндл ───────────────────────────────────────
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp).height(4.dp)
                                .clip(CircleShape)
                                .background(StardustTextSecondary.copy(alpha = 0.22f))
                        )
                    }

                    // ── Заголовок ────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StardustPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Inventory2, null,
                                    tint     = StardustPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Новая ячейка",
                                color      = StardustTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 18.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(StardustTextSecondary.copy(alpha = 0.1f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null
                                ) { handleDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Close, null, tint = StardustTextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }

                    HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 22.dp))

                    Column(
                        modifier            = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {

                        // ── Название ─────────────────────────────────
                        CellSection(icon = Icons.Outlined.Label, title = "Название", subtitle = "необязательно") {
                            CellInputField(
                                value       = displayName,
                                onChange    = { displayName = it },
                                placeholder = "Напр. «Основная FUJIAN»",
                                icon        = Icons.Outlined.Label
                            )
                        }

                        HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.06f))

                        // ── Тип АКБ ──────────────────────────────────
                        CellSection(icon = Icons.Default.Factory, title = "Тип АКБ", subtitle = "только выбранный тип") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // WIND 4.0
                                CellGroupLabel("WIND 4.0")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CellType.entries.filter { it.group == CellGroup.WIND_40 }.forEach { type ->
                                        CellTypeChipSelectable(
                                            type       = type,
                                            isSelected = selectedCellType == type,
                                            onClick    = { selectedCellType = if (selectedCellType == type) null else type },
                                            modifier   = Modifier.weight(1f)
                                        )
                                    }
                                }
                                // WIND 5.0
                                CellGroupLabel("WIND 5.0 / Ninebot")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CellType.entries.filter { it.group == CellGroup.WIND_50 }.forEach { type ->
                                        CellTypeChipSelectable(
                                            type       = type,
                                            isSelected = selectedCellType == type,
                                            onClick    = { selectedCellType = if (selectedCellType == type) null else type },
                                            modifier   = Modifier.weight(1f)
                                        )
                                    }
                                }
                                // Универсальная
                                AnimatedVisibility(
                                    visible = selectedCellType == null,
                                    enter   = fadeIn(tween(200)) + expandVertically(),
                                    exit    = fadeOut(tween(150)) + shrinkVertically()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(StardustTextSecondary.copy(alpha = 0.07f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Info, null,
                                                tint     = StardustTextSecondary.copy(alpha = 0.5f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "Тип не выбран — ячейка будет универсальной",
                                                color    = StardustTextSecondary.copy(alpha = 0.6f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.06f))

                        // ── Ёмкость ───────────────────────────────────
                        CellSection(icon = Icons.Default.Inventory2, title = "Ёмкость", subtitle = "шт.") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CellInputField(
                                    value          = capacityText,
                                    onChange       = {
                                        capacityText      = it.filter { c -> c.isDigit() }.take(4)
                                        showCapacityError = false
                                    },
                                    placeholder    = "500",
                                    icon           = Icons.Default.Numbers,
                                    keyboardType   = KeyboardType.Number,
                                    isError        = showCapacityError,
                                    errorText      = "Укажите число от 1 до 9999"
                                )
                                // Пресеты
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(100, 250, 500, 1000).forEach { preset ->
                                        val isActive = capacityText == preset.toString()
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isActive) StardustPrimary.copy(alpha = 0.18f)
                                                    else StardustGlassBg
                                                )
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication        = null
                                                ) { capacityText = preset.toString() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                preset.toString(),
                                                color      = if (isActive) StardustPrimary else StardustTextSecondary.copy(alpha = 0.6f),
                                                fontSize   = 13.sp,
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.06f))

                        // ── Адрес ─────────────────────────────────────
                        CellSection(icon = Icons.Default.LocationOn, title = "Адрес склада", subtitle = "необязательно") {
                            CellInputField(
                                value       = address,
                                onChange    = { address = it },
                                placeholder = "Напр. «Бестужевская 10»",
                                icon        = Icons.Default.LocationOn
                            )
                        }
                    }

                    HorizontalDivider(color = StardustTextSecondary.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 22.dp))

                    // ── Кнопка создать ────────────────────────────────
                    Box(modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) {
                        val btnAlpha by animateFloatAsState(
                            targetValue   = if (isCapacityValid) 1f else 0.45f,
                            animationSpec = tween(200),
                            label         = "btn_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            StardustPrimary.copy(alpha = btnAlpha),
                                            StardustPrimary.copy(alpha = btnAlpha * 0.7f)
                                        )
                                    )
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null
                                ) {
                                    if (!isCapacityValid) { showCapacityError = true; return@clickable }
                                    onCreate(CreateCellData(
                                        displayName = displayName.takeIf { it.isNotBlank() },
                                        cellType    = selectedCellType,
                                        capacity    = capacity!!,
                                        address     = address.takeIf { it.isNotBlank() }
                                    ))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add, null,
                                    tint     = Color.White.copy(alpha = btnAlpha),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Создать ячейку",
                                    color      = Color.White.copy(alpha = btnAlpha),
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================================
// ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ
// ============================================================================================

@Composable
private fun CellSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StardustPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = StardustPrimary, modifier = Modifier.size(14.dp))
            }
            Text(title, color = StardustTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("· $subtitle", color = StardustTextSecondary.copy(alpha = 0.45f), fontSize = 12.sp)
        }
        content()
    }
}

@Composable
private fun CellGroupLabel(text: String) {
    Text(
        text.uppercase(),
        color         = StardustTextSecondary.copy(alpha = 0.4f),
        fontSize      = 10.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun CellInputField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorText: String = ""
) {
    var focused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue   = when {
            isError -> StardustError.copy(alpha = 0.7f)
            focused -> StardustPrimary.copy(alpha = 0.65f)
            else    -> Color.Transparent
        },
        animationSpec = tween(180),
        label         = "border"
    )
    val iconTint by animateColorAsState(
        targetValue   = if (focused) StardustPrimary else StardustTextSecondary.copy(alpha = 0.4f),
        animationSpec = tween(180),
        label         = "icon"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(StardustGlassBg)
                .drawBehind {
                    drawRoundRect(
                        color        = borderColor,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(13.dp.toPx()),
                        style        = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )
                }
        ) {
            Row(
                modifier          = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value         = value,
                    onValueChange = onChange,
                    singleLine    = true,
                    modifier      = Modifier
                        .weight(1f)
                        .onFocusChanged { focused = it.isFocused },
                    textStyle     = androidx.compose.ui.text.TextStyle(
                        color    = StardustTextPrimary,
                        fontSize = 15.sp
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    cursorBrush   = Brush.verticalGradient(listOf(StardustPrimary, StardustPrimary)),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) Text(placeholder, color = StardustTextSecondary.copy(alpha = 0.38f), fontSize = 15.sp)
                            inner()
                        }
                    }
                )
            }
        }
        AnimatedVisibility(
            visible = isError && errorText.isNotBlank(),
            enter   = fadeIn(tween(150)) + expandVertically(),
            exit    = fadeOut(tween(100)) + shrinkVertically()
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier              = Modifier.padding(start = 4.dp)
            ) {
                Icon(Icons.Default.ErrorOutline, null, tint = StardustError, modifier = Modifier.size(12.dp))
                Text(errorText, color = StardustError, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CellTypeChipSelectable(
    type: CellType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color    = colorForCellType(type)
    val bgAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label         = "chip_bg"
    )
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label         = "chip_scale"
    )

    Box(
        modifier = modifier
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.22f + bgAlpha * 0.03f)
                else StardustGlassBg
            )
            .drawBehind {
                if (isSelected) {
                    drawRoundRect(
                        color        = color.copy(alpha = 0.55f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                        style        = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                type.shortLabel,
                color      = if (isSelected) color else StardustTextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp
            )
            Text(
                type.group.displayName,
                color    = (if (isSelected) color else StardustTextSecondary).copy(alpha = 0.55f),
                fontSize = 9.sp
            )
        }
    }
}