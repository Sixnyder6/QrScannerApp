package com.example.qrscannerapp.features.scanner.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.util.Size
import android.graphics.Rect
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.qrscannerapp.StardustError
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustSuccess
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.StardustWarning
import com.example.qrscannerapp.core.model.ScanEvent
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraView(
    isSearchMode: Boolean,
    hasPermission: Boolean,
    scanEventFlow: Flow<ScanEvent>,
    isTorchOn: Boolean,
    onTorchChange: (Boolean) -> Unit,
    onCodeScanned: (String) -> Unit,
    onStatusUpdate: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val density = LocalDensity.current

    var camera by remember { mutableStateOf<Camera?>(null) }
    var zoomLevel by rememberSaveable { mutableStateOf(0f) }
    var focusRingPosition by remember { mutableStateOf<Offset?>(null) }
    var focusRingAlpha by remember { mutableStateOf(0f) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    var borderColorTarget by remember { mutableStateOf(Color.White.copy(alpha = 0.8f)) }
    var scaleTarget by remember { mutableStateOf(1f) }
    val targetBorderColor = if (isSearchMode) StardustWarning else borderColorTarget

    val borderColor by animateColorAsState(targetValue = targetBorderColor, animationSpec = tween(400), label = "BorderColor")
    val scale by animateFloatAsState(targetValue = scaleTarget, animationSpec = tween(200), label = "BorderScale")

    LaunchedEffect(Unit) {
        scanEventFlow.collect { event ->
            launch {
                val (color, newScale) = when (event) {
                    ScanEvent.Success -> StardustSuccess to 1.05f
                    ScanEvent.Duplicate -> StardustError to 1.05f
                }
                borderColorTarget = color; scaleTarget = newScale
                delay(250)
                borderColorTarget = Color.White.copy(alpha = 0.8f); scaleTarget = 1f
            }
        }
    }

    LaunchedEffect(focusRingPosition) {
        if (focusRingPosition != null) {
            focusRingAlpha = 1f; delay(800); focusRingAlpha = 0f; delay(200); focusRingPosition = null
        }
    }

    LaunchedEffect(isTorchOn, camera) {
        try {
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                camera?.cameraControl?.enableTorch(isTorchOn)
            }
        } catch (e: Exception) { Log.e("CameraView", "Torch error", e) }
    }

    LaunchedEffect(zoomLevel, camera) {
        try { camera?.cameraControl?.setLinearZoom(zoomLevel) }
        catch (e: Exception) { Log.e("CameraView", "Zoom error", e) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                    val scaleGestureDetector = ScaleGestureDetector(ctx,
                        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScale(detector: ScaleGestureDetector): Boolean {
                                val currentZoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                                camera?.cameraControl?.setZoomRatio(currentZoomRatio * detector.scaleFactor)
                                zoomLevel = camera?.cameraInfo?.zoomState?.value?.linearZoom ?: 0f
                                return true
                            }
                        })
                    previewView.setOnTouchListener { v, event ->
                        scaleGestureDetector.onTouchEvent(event)
                        if (event.action == MotionEvent.ACTION_UP && !scaleGestureDetector.isInProgress) {
                            val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                                .setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                            camera?.cameraControl?.startFocusAndMetering(action)
                            focusRingPosition = Offset(event.x, event.y)
                            v.performClick()
                        }
                        true
                    }
                    previewView
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    val cameraProvider = cameraProviderFuture.get()
                    try {
                        cameraError = null
                        cameraProvider.unbindAll()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(cameraExecutor, QrCodeAnalyzer(onCodeScanned = { code -> onCodeScanned(code) }, onStatusUpdate = { msg -> onStatusUpdate(msg, true) })) }
                        camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
                        camera?.cameraControl?.setLinearZoom(zoomLevel)
                        if (camera!!.cameraInfo.hasFlashUnit()) camera!!.cameraControl.enableTorch(isTorchOn)
                    } catch (e: Exception) {
                        Log.e("CameraView", "Camera bind error", e)
                        cameraError = "Не удалось запустить камеру"
                    }
                }
            )

            cameraError?.let { error ->
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, null, tint = StardustError, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(error, color = StardustTextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Попробуйте перезапустить приложение", color = StardustTextSecondary, fontSize = 13.sp)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

            Box(
                modifier = Modifier.size(240.dp).align(Alignment.Center).scale(scale)
                    .border(3.dp, borderColor, RoundedCornerShape(24.dp)).background(Color.Transparent)
            )

            focusRingPosition?.let { offset ->
                Box(
                    modifier = Modifier
                        .offset(x = with(density) { offset.x.toDp() - 25.dp }, y = with(density) { offset.y.toDp() - 25.dp })
                        .size(50.dp).graphicsLayer { this.alpha = focusRingAlpha }
                        .border(2.dp, StardustPrimary.copy(alpha = 0.8f), CircleShape)
                )
            }

            if (isSearchMode) {
                Box(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
                        .background(StardustWarning.copy(0.9f), RoundedCornerShape(8.dp)).padding(16.dp, 6.dp)
                ) { Text("РЕЖИМ ПОИСКА", color = Color.Black, fontWeight = FontWeight.Bold) }
            }

            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).height(200.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val zoomState = camera?.cameraInfo?.zoomState?.value
                Text(
                    text = if (zoomState != null) "%.1fx".format(zoomState.zoomRatio) else "1.0x",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = zoomLevel,
                    onValueChange = { zoomLevel = it },
                    modifier = Modifier
                        .graphicsLayer { rotationZ = 270f; transformOrigin = TransformOrigin(0.5f, 0.5f) }
                        .width(150.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = StardustPrimary, activeTrackColor = StardustPrimary.copy(alpha = 0.8f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет доступа к камере", color = StardustTextPrimary)
            }
        }
    }
}

class QrCodeAnalyzer(
    private val onCodeScanned: (String) -> Unit,
    private val onStatusUpdate: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private var isProcessing = false
    private var lastAnalyzedTimestamp = 0L
    private val THROTTLE_DURATION = 300L
    private var cachedImageWidth = 0
    private var cachedImageHeight = 0
    private var cachedSafeZone = Rect(0, 0, 0, 0)

    private fun getSafeZone(width: Int, height: Int): Rect {
        if (width != cachedImageWidth || height != cachedImageHeight) {
            cachedImageWidth = width; cachedImageHeight = height
            cachedSafeZone = Rect(
                (width * 0.25).toInt(), (height * 0.25).toInt(),
                (width * 0.75).toInt(), (height * 0.75).toInt()
            )
        }
        return cachedSafeZone
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (isProcessing || (currentTimestamp - lastAnalyzedTimestamp < THROTTLE_DURATION)) {
            imageProxy.close(); return
        }
        isProcessing = true
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            ).process(image)
                .addOnSuccessListener { barcodes ->
                    val safeZone = getSafeZone(imageProxy.width, imageProxy.height)
                    barcodes.firstOrNull { barcode ->
                        barcode.boundingBox?.let {
                            safeZone.contains(it.centerX(), it.centerY())
                        } == true
                    }?.rawValue?.let {
                        onCodeScanned(it)
                        lastAnalyzedTimestamp = System.currentTimeMillis()
                    }
                }
                .addOnFailureListener { Log.e("QrCodeAnalyzer", "Error", it) }
                .addOnCompleteListener { isProcessing = false; imageProxy.close() }
        } else {
            isProcessing = false; imageProxy.close()
        }
    }
}