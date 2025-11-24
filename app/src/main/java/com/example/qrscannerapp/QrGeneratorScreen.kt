// File: QrGeneratorScreen.kt

package com.example.qrscannerapp

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.common.ui.AppBackground // Убедитесь, что этот импорт есть
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QrGeneratorScreen() {
    var inputText by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val textToDisplay = inputText.padStart(8, '0')
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(inputText) {
        if (inputText.isNotBlank()) {
            qrBitmap = withContext(Dispatchers.Default) {
                generateQrCodeBitmap(textToDisplay)
            }
        } else {
            qrBitmap = null
        }
    }

    // V-- ИЗМЕНЕНИЕ: AppBackground теперь не имеет отступов --V
    AppBackground {
        Column(
            // V-- ИЗМЕНЕНИЕ: Отступы применяются здесь, к содержимому --V
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { newText ->
                    if (newText.length <= 8 && newText.all { it.isDigit() }) {
                        inputText = newText
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Введите 8 цифр") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StardustPrimary,
                    unfocusedBorderColor = StardustItemBg,
                    focusedLabelColor = StardustPrimary,
                    unfocusedLabelColor = StardustTextSecondary,
                    cursorColor = StardustPrimary,
                    focusedTextColor = StardustTextPrimary,
                    unfocusedTextColor = StardustTextPrimary,
                    focusedContainerColor = StardustGlassBg,
                    unfocusedContainerColor = StardustGlassBg,
                )
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (qrBitmap != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "Сгенерированный QR-код",
                        modifier = Modifier
                            .size(250.dp)
                            .background(Color.White, shape = RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = textToDisplay,
                        color = StardustTextSecondary,
                        fontSize = 18.sp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .background(StardustGlassBg, shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Введите цифры для\nгенерации QR-кода",
                        color = StardustTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        qrBitmap?.let { bitmap ->
                            scope.launch {
                                val fileName = "QR_код_$textToDisplay"
                                ImageUtils.saveBitmapToGallery(context, bitmap, fileName)
                            }
                        }
                    },
                    enabled = qrBitmap != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Сохранить")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сохранить")
                }
                Button(
                    onClick = {
                        qrBitmap?.let { bitmap ->
                            scope.launch {
                                val fileName = "QR_код_$textToDisplay"
                                ImageUtils.shareBitmap(context, bitmap, fileName)
                            }
                        }
                    },
                    enabled = qrBitmap != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StardustPrimary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Поделиться")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Поделиться")
                }
            }
        }
    }
}

private fun generateQrCodeBitmap(text: String): Bitmap? {
    if (text.isBlank()) return null
    return try {
        val size = 512
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size,
            null
        )
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
// ---
// На чем мы остановились: Предоставлена ПОЛНАЯ версия файла QrGeneratorScreen.kt для удаления из него локального фона.
// Следующий шаг: Ваша проверка, что после замены кода в ОБОИХ файлах (AppNavigation.kt и QrGeneratorScreen.kt) анимация стала видна.