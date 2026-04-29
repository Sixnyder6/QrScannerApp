package com.example.qrscannerapp.features.scanner.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.BatchConfig
import com.example.qrscannerapp.ColorByd
import com.example.qrscannerapp.ColorFujian
import com.example.qrscannerapp.ColorWind40
import com.example.qrscannerapp.ColorWind50
import com.example.qrscannerapp.StardustError
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.core.model.ActiveTab

// ============================================================================================
// GLASSMORPHISM BATCH SETUP SHEET
// ============================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchSetupSheet(
    currentConfig: BatchConfig,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onConfirm: (BatchConfig) -> Unit
) {
    var selectedTab by remember {
        mutableStateOf(
            currentConfig.sessionType.takeIf { it != ActiveTab.WAREHOUSE } ?: ActiveTab.BATTERIES
        )
    }
    var selectedBatteryType by remember { mutableStateOf(currentConfig.batteryType) }
    var targetCount by remember { mutableIntStateOf(currentConfig.targetCount) }
    var inputText by remember { mutableStateOf(currentConfig.targetCount.toString()) }

    val presets = listOf(10, 25, 50, 100)

    // Акцентный цвет зависит от выбранного типа
    val accentColor by animateColorAsState(
        targetValue = when (selectedTab) {
            ActiveTab.SCOOTERS -> StardustPrimary
            ActiveTab.BATTERIES -> ColorWind40
            ActiveTab.NEW_BATTERIES -> ColorWind50
            else -> StardustPrimary
        },
        animationSpec = tween(400),
        label = "accentColor"
    )

    // Пульсирующий glow
    val infiniteTransition = rememberInfiniteTransition(label = "glow_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0D0D0F),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // === AMBIENT GLOW BACKGROUND ===
            // Рассеянное свечение акцентного цвета — "утекает" за границы
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-40).dp)
                    .size(300.dp, 200.dp)
                    .blur(80.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = glowAlpha),
                                accentColor.copy(alpha = glowAlpha * 0.3f),
                                Color.Transparent
                            ),
                            radius = 400f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                // === ЗАГОЛОВОК ===
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            "Режим партии",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp,
                            lineHeight = 36.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Настройте параметры",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 15.sp,
                            letterSpacing = (-0.3).sp
                        )
                    }
                    if (currentConfig.isActive) {
                        Surface(
                            onClick = { onConfirm(BatchConfig(isActive = false)) },
                            shape = CircleShape,
                            color = StardustError.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, StardustError.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Отключить",
                                tint = StardustError,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(20.dp)
                            )
                        }
                    }
                }

                // === ТИП СКАНИРОВАНИЯ — Glass Cards ===
                Text(
                    "ТИП",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TypeCard(
                        label = "Самокаты",
                        icon = Icons.Default.ElectricScooter,
                        color = StardustPrimary,
                        isSelected = selectedTab == ActiveTab.SCOOTERS,
                        onClick = { selectedTab = ActiveTab.SCOOTERS },
                        modifier = Modifier.weight(1f)
                    )
                    TypeCard(
                        label = "WIND 4.0",
                        icon = Icons.Outlined.BatteryFull,
                        color = ColorWind40,
                        isSelected = selectedTab == ActiveTab.BATTERIES,
                        onClick = { selectedTab = ActiveTab.BATTERIES },
                        modifier = Modifier.weight(1f)
                    )
                    TypeCard(
                        label = "WIND 5.0",
                        icon = Icons.Outlined.BatteryChargingFull,
                        color = ColorWind50,
                        isSelected = selectedTab == ActiveTab.NEW_BATTERIES,
                        onClick = { selectedTab = ActiveTab.NEW_BATTERIES },
                        modifier = Modifier.weight(1f)
                    )
                }

                // === ПРОИЗВОДИТЕЛЬ (WIND 4.0) ===
                AnimatedVisibility(
                    visible = selectedTab == ActiveTab.BATTERIES,
                    enter = expandVertically(
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
                    ) + fadeIn(),
                    exit = shrinkVertically(
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
                    ) + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        Text(
                            "ПРОИЗВОДИТЕЛЬ",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ManufacturerChip(
                                brand = "FUJIAN",
                                color = ColorFujian,
                                isSelected = selectedBatteryType == "FUJIAN",
                                onClick = { selectedBatteryType = "FUJIAN" },
                                modifier = Modifier.weight(1f)
                            )
                            ManufacturerChip(
                                brand = "BYD",
                                color = ColorByd,
                                isSelected = selectedBatteryType == "BYD",
                                onClick = { selectedBatteryType = "BYD" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // === КОЛИЧЕСТВО ===
                Text(
                    "КОЛИЧЕСТВО",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Пресеты — Glass Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = targetCount == preset
                        GlassPresetButton(
                            value = preset,
                            accentColor = accentColor,
                            isSelected = isSelected,
                            onClick = {
                                targetCount = preset
                                inputText = preset.toString()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Поле ввода — Glass Style
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { v: String ->
                        inputText = v.filter { c: Char -> c.isDigit() }
                        inputText.toIntOrNull()?.let { parsed: Int ->
                            if (parsed > 0) targetCount = parsed
                        }
                    },
                    label = {
                        Text(
                            "Своё количество",
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.06f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = accentColor,
                        focusedContainerColor = Color.White.copy(alpha = 0.04f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // === КНОПКА СТАРТ — с glow ===
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow под кнопкой
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(56.dp)
                            .offset(y = 8.dp)
                            .blur(24.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .background(
                                accentColor.copy(alpha = 0.4f),
                                RoundedCornerShape(20.dp)
                            )
                    )
                    Button(
                        onClick = {
                            val finalType = selectedTab
                            if (finalType == ActiveTab.NEW_BATTERIES) {
                                selectedBatteryType = "NEW"
                            }
                            onConfirm(
                                BatchConfig(
                                    isActive = true,
                                    targetCount = targetCount.coerceAtLeast(1),
                                    sessionType = finalType,
                                    batteryType = selectedBatteryType
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text(
                            "Начать  ·  $targetCount шт.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.Black,
                            letterSpacing = (-0.3).sp
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================================
// GLASS TYPE CARD — Карточка выбора типа с эффектом стекла
// ============================================================================================

@Composable
private fun TypeCard(
    label: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBorderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.5f else 0.08f,
        animationSpec = tween(300),
        label = "borderAlpha"
    )
    val animatedBgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.12f else 0.04f,
        animationSpec = tween(300),
        label = "bgAlpha"
    )

    Box(modifier = modifier) {
        // Glow за карточкой (только для выбранного)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 4.dp)
                    .blur(16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        color.copy(alpha = 0.25f),
                        RoundedCornerShape(16.dp)
                    )
            )
        }

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = animatedBgAlpha),
            border = BorderStroke(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = color.copy(alpha = animatedBorderAlpha)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (isSelected) color else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = label,
                    color = if (isSelected) color else Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// ============================================================================================
// MANUFACTURER CHIP — Чип производителя с мягким свечением
// ============================================================================================

@Composable
private fun ManufacturerChip(
    brand: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.15f else 0.04f,
        animationSpec = tween(300),
        label = "manufBg"
    )

    Box(modifier = modifier) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 3.dp)
                    .blur(12.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        color.copy(alpha = 0.2f),
                        RoundedCornerShape(14.dp)
                    )
            )
        }

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            color = color.copy(alpha = animatedBgAlpha),
            border = BorderStroke(
                width = 1.dp,
                color = if (isSelected) color.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .then(
                            if (isSelected) {
                                Modifier.drawBehind {
                                    drawCircle(
                                        color = color.copy(alpha = 0.6f),
                                        radius = size.minDimension * 1.5f,
                                        style = Fill
                                    )
                                }
                            } else Modifier
                        )
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    brand,
                    color = if (isSelected) color else Color.White.copy(alpha = 0.5f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ============================================================================================
// GLASS PRESET BUTTON — Кнопка пресета с glow
// ============================================================================================

@Composable
private fun GlassPresetButton(
    value: Int,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color.White.copy(alpha = 0.04f),
        animationSpec = tween(300),
        label = "presetBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.06f),
        animationSpec = tween(300),
        label = "presetBorder"
    )

    Box(modifier = modifier) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 4.dp)
                    .blur(14.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        accentColor.copy(alpha = 0.35f),
                        RoundedCornerShape(12.dp)
                    )
            )
        }

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = animatedBg,
            border = BorderStroke(1.dp, animatedBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    value.toString(),
                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}