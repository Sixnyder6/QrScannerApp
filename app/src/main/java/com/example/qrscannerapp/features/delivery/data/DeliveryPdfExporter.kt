package com.example.qrscannerapp.features.delivery.data

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.qrscannerapp.features.delivery.domain.model.DeliveryLog
import com.example.qrscannerapp.features.delivery.domain.model.DeliveryType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DeliveryPdfExporter(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())

    /**
     * Генерация PDF отчёта по списку доставок
     */
    fun exportDeliveriesToPdf(deliveries: List<DeliveryLog>): Result<File> {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 размер
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Настройки шрифтов и цветов
            val titlePaint = android.graphics.Paint().apply {
                textSize = 24f
                isFakeBoldText = true
                color = android.graphics.Color.parseColor("#6A5AE0")
            }

            val headerPaint = android.graphics.Paint().apply {
                textSize = 14f
                isFakeBoldText = true
                color = android.graphics.Color.BLACK
            }

            val bodyPaint = android.graphics.Paint().apply {
                textSize = 12f
                color = android.graphics.Color.DKGRAY
            }

            val dividerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.LTGRAY
                strokeWidth = 2f
            }

            // Заголовок
            canvas.drawText("ОТЧЁТ ПО ДОСТАВКАМ", 40f, 50f, titlePaint)
            canvas.drawText(
                "Сгенерирован: ${dateFormat.format(Date())}",
                40f,
                80f,
                bodyPaint
            )
            canvas.drawText(
                "Всего записей: ${deliveries.size}",
                40f,
                100f,
                bodyPaint
            )

            canvas.drawLine(40f, 110f, 555f, 110f, dividerPaint)

            var yPos = 140f
            val leftMargin = 40f
            val rightMargin = 555f

            // Таблица записей
            deliveries.forEachIndexed { index, delivery ->
                // Проверка на новую страницу
                if (yPos > 780f) {
                    document.finishPage(page)
                    val newPage = document.startPage(pageInfo)
                    yPos = 40f
                }

                // Тип доставки (цветной бейдж)
                val typeText = when (delivery.type) {
                    DeliveryType.EXPECTED -> "[ОЖИДАЕТСЯ]"
                    DeliveryType.RECEIVE -> "[ПРИНЯТО]"
                    DeliveryType.SEND -> "[ОТПРАВЛЕНО]"
                }
                val typeColor = when (delivery.type) {
                    DeliveryType.EXPECTED -> android.graphics.Color.parseColor("#FFA726")
                    DeliveryType.RECEIVE -> android.graphics.Color.parseColor("#4CAF50")
                    DeliveryType.SEND -> android.graphics.Color.parseColor("#29B6F6")
                }

                val typePaint = android.graphics.Paint().apply {
                    textSize = 11f
                    isFakeBoldText = true
                    color = typeColor
                }
                canvas.drawText(typeText, leftMargin, yPos, typePaint)

                // Дата и сотрудник
                canvas.drawText(
                    dateFormat.format(Date(delivery.timestamp)),
                    leftMargin + 120f,
                    yPos,
                    bodyPaint
                )
                canvas.drawText(
                    delivery.employeeName,
                    rightMargin - 120f,
                    yPos,
                    bodyPaint
                )

                yPos += 25f

                // Номер машины
                canvas.drawText("🚗 ${delivery.licensePlate}", leftMargin, yPos, headerPaint)

                yPos += 25f

                // Количество и описание
                canvas.drawText("📦 Единиц: ${delivery.itemCount}", leftMargin, yPos, bodyPaint)
                if (delivery.description.isNotBlank()) {
                    canvas.drawText("📝 ${delivery.description}", leftMargin + 200f, yPos, bodyPaint)
                }

                yPos += 25f

                // Фото (если есть)
                if (delivery.photoUrls.isNotEmpty()) {
                    canvas.drawText("📸 Фото: ${delivery.photoUrls.size}", leftMargin, yPos, bodyPaint)
                }

                yPos += 35f

                // Разделитель
                canvas.drawLine(leftMargin, yPos, rightMargin, yPos, dividerPaint)
                yPos += 20f
            }

            document.finishPage(page)

            // Сохранение файла
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "delivery_report_${fileNameFormat.format(Date())}.pdf"
            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Генерация PDF накладной для одной доставки
     */
    fun exportDeliveryReceipt(delivery: DeliveryLog): Result<File> {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = android.graphics.Paint().apply {
                textSize = 28f
                isFakeBoldText = true
                color = android.graphics.Color.parseColor("#6A5AE0")
            }

            val headerPaint = android.graphics.Paint().apply {
                textSize = 16f
                isFakeBoldText = true
                color = android.graphics.Color.BLACK
            }

            val bodyPaint = android.graphics.Paint().apply {
                textSize = 14f
                color = android.graphics.Color.DKGRAY
            }

            // Заголовок
            canvas.drawText("НАКЛАДНАЯ", 40f, 80f, titlePaint)
            canvas.drawText(
                "№ ${delivery.id.take(8).uppercase()}",
                40f,
                110f,
                bodyPaint
            )

            var yPos = 180f

            // Тип операции
            val typeText = when (delivery.type) {
                DeliveryType.EXPECTED -> "ОЖИДАЕТСЯ ПРИБЫТИЕ"
                DeliveryType.RECEIVE -> "ПРИЁМКА ГРУЗА"
                DeliveryType.SEND -> "ОТПРАВКА ГРУЗА"
            }
            canvas.drawText(typeText, 40f, yPos, headerPaint)
            yPos += 50f

            // Информация о доставке
            canvas.drawText("🚗 Государственный номер:", 40f, yPos, headerPaint)
            yPos += 25f
            canvas.drawText(delivery.licensePlate, 60f, yPos, bodyPaint)
            yPos += 40f

            canvas.drawText("📦 Количество единиц:", 40f, yPos, headerPaint)
            yPos += 25f
            canvas.drawText("${delivery.itemCount} шт.", 60f, yPos, bodyPaint)
            yPos += 40f

            if (delivery.description.isNotBlank()) {
                canvas.drawText("📝 Описание груза:", 40f, yPos, headerPaint)
                yPos += 25f
                canvas.drawText(delivery.description, 60f, yPos, bodyPaint)
                yPos += 40f
            }

            canvas.drawText("👤 Сотрудник:", 40f, yPos, headerPaint)
            yPos += 25f
            canvas.drawText(delivery.employeeName, 60f, yPos, bodyPaint)
            yPos += 40f

            canvas.drawText("🕐 Дата и время:", 40f, yPos, headerPaint)
            yPos += 25f
            canvas.drawText(dateFormat.format(Date(delivery.timestamp)), 60f, yPos, bodyPaint)

            if (delivery.plannedDate != null) {
                yPos += 30f
                canvas.drawText("📅 Плановая дата:", 40f, yPos, headerPaint)
                yPos += 25f
                canvas.drawText(dateFormat.format(Date(delivery.plannedDate)), 60f, yPos, bodyPaint)
            }

            // Подпись
            yPos += 80f
            canvas.drawText("____________________", 40f, yPos, bodyPaint)
            yPos += 25f
            canvas.drawText("(Подпись сотрудника)", 40f, yPos, bodyPaint)

            document.finishPage(page)

            // Сохранение
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "receipt_${delivery.licensePlate}_${fileNameFormat.format(Date(delivery.timestamp))}.pdf"
            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}