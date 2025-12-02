// FILE: com/example/qrscannerapp/features/visual_repair/ui/VisualRepairScreen.kt
package com.example.qrscannerapp.features.visual_repair.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
// ЗАМЕНА: Icons.AutoMirrored.Filled.ThreeSixty УДАЛЕН
import androidx.compose.material.icons.filled.* // ИМПОРТИРУЕМ ВСЕ СТАНДАРТНЫЕ ИКОНКИ
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualRepairScreen(
    scooterId: String?,
    onNavigateBack: () -> Unit,
    viewModel: VisualRepairViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isAutoRotating by viewModel.isAutoRotating.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Создаем анимируемые значения для плавного вращения
    val rotationY = remember { Animatable(0f) }
    val rotationX = remember { Animatable(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мастерская · Самокат #${scooterId ?: "—"}", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF151515))
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            val engine = rememberEngine()
            val modelLoader = rememberModelLoader(engine)
            val rootNode = rememberNode(engine).apply {
                scale = Float3(0.8f)
            }

            // Применяем анимированное вращение к узлу
            LaunchedEffect(rotationX.value, rotationY.value) {
                rootNode.rotation = Rotation(x = rotationX.value, y = rotationY.value)
            }

            Scene(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                coroutineScope.launch {
                                    viewModel.stopAutoRotation() // Выключаем авто-вращение при касании
                                    rotationY.stop()
                                    rotationX.stop()
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    // ОКОНЧАТЕЛЬНО ИСПРАВЛЕННАЯ МАТЕМАТИКА
                                    rotationY.snapTo(rotationY.value + dragAmount.x * 0.5f)
                                    rotationX.snapTo(
                                        (rotationX.value + dragAmount.y * 0.5f).coerceIn(-60f, 60f)
                                    )
                                }
                            }
                        )
                    },
                engine = engine,
                modelLoader = modelLoader,
                childNodes = listOf(rootNode),
                cameraManipulator = null
            )

            LaunchedEffect(modelLoader) {
                loadModels(modelLoader, rootNode) { success ->
                    viewModel.setLoading(!success)
                }
            }

            // Исправленная логика авто-вращения: "возвращение домой"
            LaunchedEffect(isAutoRotating) {
                if (isAutoRotating) {
                    // Этап 1: Плавно возвращаемся в исходное положение
                    launch {
                        rotationX.animateTo(0f, animationSpec = tween(700))
                    }
                    launch {
                        rotationY.animateTo(
                            // Анимируем к ближайшему "нулю" (например, от 350 -> к 360, от 10 -> к 0)
                            targetValue = (rotationY.value / 360f).roundToInt() * 360f,
                            animationSpec = tween(700)
                        )
                    }.join() // Дожидаемся окончания возврата

                    // Этап 2: Запускаем бесконечную карусель
                    while (viewModel.isAutoRotating.value) { // Проверяем актуальное состояние из ViewModel
                        rotationY.snapTo(rotationY.value + 0.4f)
                        delay(16)
                    }
                } else {
                    if (rotationY.isRunning) {
                        rotationY.stop()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 110.dp)
                    .size(220.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFFFFD54F)
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HudBox("День", "30")
                HudBox("Время", "18:03")
                HudBox("Баланс", "52 981 ₽")
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xAA000000))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Вращайте модель пальцем, щипок — масштаб",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    RepairButton(text = "Диагностика", icon = Icons.Default.AutoAwesome, onClick = {})
                    RepairButton(text = "Ремонт", icon = Icons.Default.Build, onClick = {})
                    RepairButton(text = "Покраска", icon = Icons.Default.Palette, onClick = {})
                    RepairButton(text = "Инфо", icon = Icons.Default.Info, onClick = {})
                    RepairButton(
                        text = "Вращать",
                        icon = Icons.Default.Refresh,
                        onClick = { viewModel.toggleAutoRotation() },
                        tint = if (isAutoRotating) Color.Cyan else Color.White
                    )
                }
            }
        }
    }
}

private fun loadModels(modelLoader: ModelLoader, rootNode: Node, onResult: (Boolean) -> Unit) {
    try {
        modelLoader.createModelInstance("garage.glb")?.let { garageModel ->
            val garageNode = ModelNode(modelInstance = garageModel).apply {
                position = Position(y = -0.55f)
            }
            rootNode.addChildNode(garageNode)

            modelLoader.createModelInstance("scooter.glb")?.let { scooterModel ->
                val scooterNode = ModelNode(modelInstance = scooterModel, scaleToUnits = 0.95f).apply {
                    position = Position(y = 0.4f)
                }
                garageNode.addChildNode(scooterNode)
            }
            onResult(true)
        } ?: onResult(false)
    } catch (e: Exception) {
        e.printStackTrace()
        onResult(false)
    }
}

@Composable
fun HudBox(title: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color(0xAA1A1A1A), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun RepairButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = tint)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text.uppercase(),
            color = tint,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}