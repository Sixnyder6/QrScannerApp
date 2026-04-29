package com.example.qrscannerapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

private val ColorOldBattery = Color(0xFFE53935)
private val ColorNewBattery = Color(0xFF43A047)
private val ColorScooter = Color(0xFFFFD54F) // Желтый цвет для самокатов
private val ColorScooterBg = Color(0xFFFFE082) // Светло-желтый для заливки самого QR-кода

@Composable
fun QrGeneratorScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Старый АКБ", "Новый АКБ", "Самокат")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StardustSolidBg)
            .padding(16.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = StardustItemBg,
            contentColor = StardustTextPrimary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 14.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> OldBatteryGenerator()
            1 -> NewBatteryGenerator()
            2 -> ScooterGenerator()
        }
    }
}

@Composable
fun OldBatteryGenerator() {
    var letters by remember { mutableStateOf("") }
    var digits by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val fullCode = if (letters.length == 2 && digits.length == 11) {
        "SF$letters$digits"
    } else ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StardustItemBg),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Формат: SF + 2 буквы + 11 цифр",
                    color = ColorOldBattery,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Пример: SFAEQ24A5A0175",
                    color = StardustTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = letters,
            onValueChange = { v -> letters = v.filter { it.isLetter() }.uppercase().take(2) },
            label = { Text("2 буквы (AE, BQ, XY...)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Characters
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColorOldBattery,
                cursorColor = ColorOldBattery,
                focusedTextColor = StardustTextPrimary,
                unfocusedTextColor = StardustTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = digits,
            onValueChange = { v -> digits = v.filter { it.isDigit() || it.isLetter() }.uppercase().take(11) },
            label = { Text("11 символов (буквы/цифры)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Например: Q24A5A0175") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Characters
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColorOldBattery,
                cursorColor = ColorOldBattery,
                focusedTextColor = StardustTextPrimary,
                unfocusedTextColor = StardustTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (fullCode.isNotEmpty()) {
                    qrBitmap = generateQrCode(fullCode)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = fullCode.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = ColorOldBattery),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.QrCode2, null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Сгенерировать QR", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (fullCode.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Полный код: $fullCode",
                color = ColorOldBattery,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        qrBitmap?.let { bitmap ->
            Card(
                modifier = Modifier.size(300.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(280.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NewBatteryGenerator() {
    var digits by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val fullCode = if (digits.length == 11) "5BB$digits" else ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StardustItemBg),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Формат: 5BB + 11 цифр",
                    color = ColorNewBattery,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Пример: 5BB32501113863",
                    color = StardustTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = digits,
            onValueChange = { v -> digits = v.filter { it.isDigit() }.take(11) },
            label = { Text("11 цифр") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("32501113863") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColorNewBattery,
                cursorColor = ColorNewBattery,
                focusedTextColor = StardustTextPrimary,
                unfocusedTextColor = StardustTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (fullCode.isNotEmpty()) {
                    qrBitmap = generateQrCode(fullCode)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = fullCode.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = ColorNewBattery),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.QrCode2, null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Сгенерировать QR", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (fullCode.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Полный код: $fullCode",
                color = ColorNewBattery,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        qrBitmap?.let { bitmap ->
            Card(
                modifier = Modifier.size(300.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(280.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================================
// ГЕНЕРАТОР САМОКАТОВ (Исправлен ввод и фон картинки)
// ============================================================================================
@Composable
fun ScooterGenerator() {
    val context = LocalContext.current
    var scooterCode by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Проверяем, введено ли ровно 6 символов (так как ввод теперь строго фильтруется,
    // если длина 6, значит код 100% правильный)
    val isCodeComplete = scooterCode.length == 6

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StardustItemBg),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Формат: 2 буквы + 3 цифры + 1 буква",
                    color = ColorScooter,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Пример: HB127B (вводите слитно)",
                    color = StardustTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = scooterCode,
            onValueChange = { input ->
                // ЗАЩИТА ОТ ДУРАКА: Проверяем каждый символ на лету
                var isValid = true
                if (input.length > 6) {
                    isValid = false // Блокируем ввод больше 6 символов
                } else {
                    for (i in input.indices) {
                        val char = input[i]
                        // Индексы 0 и 1 (первые два символа) - только буквы
                        if (i == 0 || i == 1) {
                            if (!char.isLetter()) isValid = false
                        }
                        // Индексы 2, 3, 4 (три символа) - только цифры
                        else if (i == 2 || i == 3 || i == 4) {
                            if (!char.isDigit()) isValid = false
                        }
                        // Индекс 5 (последний символ) - только буква
                        else if (i == 5) {
                            if (!char.isLetter()) isValid = false
                        }
                    }
                }

                // Если строка прошла проверку, обновляем значение
                if (isValid) {
                    scooterCode = input.uppercase()
                    qrBitmap = null // Прячем старый QR при вводе
                }
            },
            label = { Text("Номер самоката") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("HB127B") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Characters
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColorScooter,
                cursorColor = ColorScooter,
                focusedTextColor = StardustTextPrimary,
                unfocusedTextColor = StardustTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (isCodeComplete) {
                    // Передаем желтый цвет (toArgb переводит Compose Color в системный Int)
                    qrBitmap = generateQrCode(
                        text = scooterCode,
                        bgColorInt = ColorScooterBg.toArgb()
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isCodeComplete,
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorScooter,
                disabledContainerColor = Color.DarkGray
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.QrCode2, null, tint = if (isCodeComplete) Color.Black else Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Сгенерировать QR",
                color = if (isCodeComplete) Color.Black else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }

        if (qrBitmap != null) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Сгенерировано для: $scooterCode",
                color = ColorScooter,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Карточка теперь просто служит оберткой (тень/скругление).
            // Сам Bitmap уже сгенерирован с желтым фоном.
            Card(
                modifier = Modifier.size(300.dp),
                colors = CardDefaults.cardColors(containerColor = ColorScooterBg),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize() // Растягиваем на всю карточку
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { shareQrCode(context, qrBitmap!!, scooterCode) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorScooter),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Поделиться")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Поделиться QR-кодом", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ============================================================================================
// ФУНКЦИЯ ГЕНЕРАЦИИ (Добавлен параметр bgColorInt)
// ============================================================================================
private fun generateQrCode(text: String, size: Int = 512, bgColorInt: Int = AndroidColor.WHITE): Bitmap {
    val writer = QRCodeWriter()
    // Отступ вокруг QR (margin) можно убрать, передав map параметров, но пока оставим стандартный
    val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
    val width = bitMatrix.width
    val height = bitMatrix.height

    val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)

    for (x in 0 until width) {
        for (y in 0 until height) {
            // Если бит черный -> рисуем черный. Иначе -> рисуем переданный цвет фона (желтый)
            bitmap[x, y] = if (bitMatrix[x, y]) AndroidColor.BLACK else bgColorInt
        }
    }

    return bitmap
}

// Функция отправки QR-кода
private fun shareQrCode(context: Context, bitmap: Bitmap, codeTitle: String) {
    try {
        val path = android.provider.MediaStore.Images.Media.insertImage(
            context.contentResolver, bitmap, "QR_$codeTitle", "QR-код для $codeTitle"
        )
        if (path != null) {
            val imageUri = Uri.parse(path)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, "Сгенерирован QR-код: $codeTitle")
            }
            context.startActivity(Intent.createChooser(intent, "Поделиться QR-кодом"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}