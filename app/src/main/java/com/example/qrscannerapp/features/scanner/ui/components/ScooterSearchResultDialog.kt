package com.example.qrscannerapp.features.scanner.ui.components

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.qrscannerapp.R
// Ваши импорты цветов
import com.example.qrscannerapp.StardustModalBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSecondary
import com.example.qrscannerapp.StardustSuccess
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ScooterSearchResultDialog(
    scooterNumber: String,
    locationName: String,
    lastUser: String,
    batteryLevel: Int = 85,
    modelName: String = "Wind 4.0",
    onDismiss: () -> Unit,
    onNavigate: () -> Unit,
    onOpen3D: () -> Unit // <--- ДОБАВЛЕН НОВЫЙ ПАРАМЕТР
) {
    // --- АНИМАЦИЯ ---
    var startAnimation by remember { mutableStateOf(false) }

    val scooterOffsetY by animateDpAsState(
        targetValue = if (startAnimation) 0.dp else (-400).dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "ScooterBounce"
    )

    val lightAlpha = remember { Animatable(0f) }

    val cardAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(400, delayMillis = 100), label = "CardAlpha"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(400, delayMillis = 100), label = "CardScale"
    )

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(300)
        lightAlpha.animateTo(0.8f, tween(500))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(750.dp)
        ) {
            // --- ГРУППА: САМОКАТ + СВЕТ ---
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = scooterOffsetY)
                    .offset(y = 80.dp)
                    .size(340.dp)
                    .zIndex(2f)
            ) {
                // 1. ПЕРЕДНИЙ СВЕТ (Ваши настройки)
                HeadlightBeam(
                    alpha = lightAlpha.value,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = 125.dp, y = 26.dp)
                )

                // 2. ЗАДНИЙ КРАСНЫЙ СВЕТ (НОВОЕ)
                RearLightGlow(
                    alpha = lightAlpha.value,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        // НАСТРОЙКА ПОЗИЦИИ ЗАДНЕГО ФОНАРЯ:
                        // x = отрицательное число (двигает влево к заднему колесу)
                        // y = большое положительное (двигает вниз к земле)
                        .offset(x = (-111).dp, y = 188.dp)
                )

                // 3. КАРТИНКА САМОКАТА
                Image(
                    painter = painterResource(id = R.drawable.scooter),
                    contentDescription = "Scooter",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // --- КАРТОЧКА ---
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = StardustModalBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .align(Alignment.BottomCenter)
                    .zIndex(1f)
                    .graphicsLayer {
                        alpha = cardAlpha
                        scaleX = cardScale
                        scaleY = cardScale
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 160.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Badge Модели
                    Surface(
                        color = StardustPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ElectricScooter, null, tint = StardustPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = modelName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = StardustPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "САМОКАТ НАЙДЕН",
                        style = MaterialTheme.typography.labelMedium,
                        color = StardustSuccess,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Номер и батарея
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(scooterNumber))
                                Toast
                                    .makeText(context, "Скопировано", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = scooterNumber,
                            style = MaterialTheme.typography.headlineLarge,
                            color = StardustTextPrimary,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        AnimatedBatteryIcon(
                            level = batteryLevel,
                            modifier = Modifier.size(width = 24.dp, height = 36.dp)
                        )
                    }

                    // Статусы
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusBadge(text = "ИСПРАВЕН", color = StardustSuccess)
                        StatusBadge(text = "GPS OK", color = StardustSecondary)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Инфо блок
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF232326), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, null, tint = StardustSecondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(locationName, color = StardustTextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                            }
                            Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, tint = StardustTextSecondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(lastUser, color = StardustTextSecondary, fontSize = 15.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // --- КНОПКА 3D ОСМОТРА (НОВАЯ) ---
                    OutlinedButton(
                        onClick = {
                            onOpen3D()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp) // Такая же высота как у основной кнопки
                            .padding(bottom = 12.dp), // Отступ снизу
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StardustPrimary.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StardustPrimary)
                    ) {
                        Text("3D Осмотр", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    // --- КНОПКА ПЕРЕЙТИ (СТАРАЯ) ---
                    Button(
                        onClick = onNavigate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(8.dp)
                    ) {
                        Text("Перейти к месту", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ПЕРЕДНЯЯ ФАРА
@Composable
fun HeadlightBeam(
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .width(180.dp)
                .height(100.dp)
                .rotate(-5f)
                .graphicsLayer {
                    this.alpha = alpha
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                }
        ) {
            val beamColor = Color(0xFFE3F2FD)
            val path = Path().apply {
                moveTo(0f, size.height / 2 - 2f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height / 2 + 2f)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        beamColor.copy(alpha = 0.9f),
                        beamColor.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    startX = 0f,
                    endX = size.width * 0.9f
                )
            )
        }
    }
}

// НОВАЯ ФУНКЦИЯ: ЗАДНИЙ КРАСНЫЙ ФОНАРЬ
@Composable
fun RearLightGlow(
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .width(80.dp)  // Короткий луч
                .height(50.dp)
                .graphicsLayer { this.alpha = alpha }
        ) {
            val lightColor = Color(0xFFFF1744) // Ярко-красный

            // Луч направлен ВЛЕВО (назад)
            val path = Path().apply {
                moveTo(size.width, size.height / 2) // Начало справа (у крыла)
                lineTo(0f, 0f)                      // Расширение влево верх
                lineTo(0f, size.height)             // Расширение влево низ
                close()
            }

            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,             // Вдали прозрачно
                        lightColor.copy(alpha = 0.2f), // Середина
                        lightColor.copy(alpha = 0.7f)  // У источника ярко
                    ),
                    startX = 0f,
                    endX = size.width
                )
            )

            // Маленькая яркая точка (сама лампочка)
            drawCircle(
                color = lightColor,
                radius = 3.dp.toPx(),
                center = Offset(size.width - 2.dp.toPx(), size.height / 2)
            )
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AnimatedBatteryIcon(level: Int, modifier: Modifier = Modifier) {
    val batteryColor = remember(level) {
        when {
            level <= 20 -> Color(0xFFF44336)
            level <= 50 -> Color(0xFFFF9800)
            level <= 70 -> Color(0xFFFFEB3B)
            else -> Color(0xFF4CAF50)
        }
    }
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    val animatedLevel by animateFloatAsState(
        targetValue = if (isVisible) level / 100f else 0f,
        animationSpec = tween(1000, 300),
        label = "Battery"
    )
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val capH = 3.dp.toPx()
        val bodyH = size.height - capH
        drawRoundRect(
            color = Color.White.copy(0.4f),
            topLeft = Offset(0f, capH),
            size = Size(size.width, bodyH),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
        )
        drawRect(
            color = Color.White.copy(0.4f),
            topLeft = Offset((size.width - size.width * 0.5f) / 2, 0f),
            size = Size(size.width * 0.5f, capH)
        )
        val fillH = (bodyH - stroke * 2) * animatedLevel
        if (animatedLevel > 0) {
            drawRoundRect(
                color = batteryColor,
                topLeft = Offset(stroke, capH + bodyH - stroke - fillH),
                size = Size(size.width - stroke * 2, fillH),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}