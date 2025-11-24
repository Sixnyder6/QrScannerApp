package com.example.qrscannerapp.features.inventory.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import com.example.qrscannerapp.R
import com.example.qrscannerapp.features.inventory.domain.model.StoragePallet
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PalletSummaryPdfGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    // --- ЦВЕТОВАЯ ПАЛИТРА ---
    private val colorPrimary = Color.parseColor("#6A5AE0") // Основной фиолетовый
    private val colorLightBg = Color.parseColor("#F3F1FF") // Фон заголовков
    private val colorDarkText = Color.parseColor("#1A1A1D")
    private val colorGrayText = Color.parseColor("#666666")
    private val colorZebra = Color.parseColor("#FAFAFA")

    // Цвета для диаграммы
    private val colorFujian = Color.parseColor("#FF8A65") // Мягкий оранжевый
    private val colorByd = Color.parseColor("#4FC3F7")    // Мягкий голубой
    private val colorEmpty = Color.parseColor("#E0E0E0")  // Серый для пустого

    // --- ШРИФТЫ ---
    private val titlePaint = TextPaint().apply {
        color = colorPrimary
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    private val subtitlePaint = TextPaint().apply {
        color = colorGrayText
        textSize = 10f
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.LEFT
    }

    private val statLabelPaint = TextPaint().apply {
        color = colorGrayText
        textSize = 9f
        textAlign = Paint.Align.LEFT // Выравнивание по левому краю для аккуратности
    }

    private val statValuePaint = TextPaint().apply {
        color = colorDarkText
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    private val tableHeaderPaint = TextPaint().apply {
        color = colorPrimary
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val rowTextPaint = TextPaint().apply {
        color = colorDarkText
        textSize = 10f
    }

    private val manufacturerPaint = TextPaint().apply {
        color = colorDarkText
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val footerPaint = TextPaint().apply {
        color = Color.LTGRAY
        textSize = 8f
        textAlign = Paint.Align.CENTER
    }

    // --- PUBLIC API ---

    fun writePdfToStream(context: Context, pallets: List<StoragePallet>, adminName: String, outputStream: OutputStream) {
        val pdfDocument = createPopulatedPdf(context, pallets, adminName)
        try {
            outputStream.use { pdfDocument.writeTo(it) }
        } finally {
            pdfDocument.close()
        }
    }

    fun generateAndShare(context: Context, pallets: List<StoragePallet>, adminName: String) {
        val pdfDocument = createPopulatedPdf(context, pallets, adminName)
        try {
            val fileName = "Otchet_Sklad_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { pdfDocument.writeTo(it) }
            pdfDocument.close()
            sharePdfFile(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Ошибка создания PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // --- ЛОГИКА ---

    private fun createPopulatedPdf(context: Context, pallets: List<StoragePallet>, adminName: String): PdfDocument {
        val pdfDocument = PdfDocument()
        var pageNumber = 1

        // Данные
        val totalItems = pallets.sumOf { it.items.size }
        val fujianCount = pallets.filter { it.manufacturer == "FUJIAN" }.sumOf { it.items.size }
        val bydCount = pallets.filter { it.manufacturer == "BYD" }.sumOf { it.items.size }
        val currentDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())

        val logoBitmap = try {
            ContextCompat.getDrawable(context, R.drawable.ic_logo_report)?.toBitmap(40, 40)
        } catch (e: Exception) { null }

        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var yPosition = MARGIN

        fun startPage() {
            if (currentPage != null) {
                drawFooter(canvas!!, pageNumber)
                pdfDocument.finishPage(currentPage)
                pageNumber++
            }
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage!!.canvas
            yPosition = MARGIN
        }

        startPage()

        // 1. ЗАГОЛОВОК
        var headerOffset = 0f
        if (logoBitmap != null) {
            canvas?.drawBitmap(logoBitmap, MARGIN, yPosition, null)
            headerOffset = 50f
        }

        canvas?.drawText("ОТЧЕТ ПРИЕМКИ", MARGIN + headerOffset, yPosition + 25f, titlePaint)
        yPosition += 45f

        canvas?.drawText("Сформирован: $currentDate", MARGIN, yPosition, subtitlePaint)
        canvas?.drawText("Ответственный: $adminName", MARGIN, yPosition + 12f, subtitlePaint)

        yPosition += 30f

        // 2. ДАШБОРД (Диаграмма + Цифры)
        val statBoxHeight = 70f
        // Фон дашборда
        val bgPaint = Paint().apply { color = colorZebra; style = Paint.Style.FILL }
        canvas?.drawRoundRect(RectF(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition + statBoxHeight), 10f, 10f, bgPaint)

        // Рисуем диаграмму (Donut Chart)
        val chartSize = 50f
        val chartX = MARGIN + 40f
        val chartY = yPosition + (statBoxHeight - chartSize) / 2
        drawDonutChart(canvas!!, chartX, chartY, chartSize, fujianCount, bydCount, totalItems)

        // Рисуем цифры (сдвигаем правее от диаграммы)
        val textStartX = MARGIN + 120f
        val colWidth = 130f

        // Всего
        drawStatItem(canvas!!, "ВСЕГО АКБ", "$totalItems", textStartX, yPosition + 45f, colorDarkText)

        // Fujian (Оранжевый)
        drawStatItem(canvas!!, "FUJIAN", "$fujianCount", textStartX + colWidth, yPosition + 45f, colorFujian)

        // BYD (Голубой)
        drawStatItem(canvas!!, "BYD", "$bydCount", textStartX + colWidth * 2, yPosition + 45f, colorByd)

        yPosition += statBoxHeight + 30f

        // 3. ТАБЛИЦА
        drawTableHeader(canvas!!, yPosition)
        yPosition += 25f

        val rowHeight = 25f
        val sortedPallets = pallets.sortedByDescending { it.palletNumber }

        sortedPallets.forEachIndexed { index, pallet ->
            if (yPosition + rowHeight > PAGE_HEIGHT - MARGIN - 60f) { // Оставляем место под подписи в конце
                startPage()
                yPosition += 40f
                drawTableHeader(canvas!!, yPosition)
                yPosition += 25f
            }

            if (index % 2 == 0) {
                val zebraPaint = Paint().apply { color = colorZebra }
                canvas?.drawRect(MARGIN, yPosition - 18f, PAGE_WIDTH - MARGIN, yPosition + 7f, zebraPaint)
            }

            val creationDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
            drawRow(
                canvas!!, yPosition,
                "№ ${pallet.palletNumber}",
                pallet.manufacturer ?: "-",
                "${pallet.items.size} шт.",
                creationDate,
                adminName
            )
            yPosition += rowHeight
        }

        // Черта конца таблицы
        canvas?.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, Paint().apply { color = colorPrimary; strokeWidth = 2f })
        yPosition += 40f

        // 4. ПОДПИСИ (Юридический блок)
        // Если места мало, переносим на новую страницу
        if (yPosition + 60f > PAGE_HEIGHT - MARGIN) {
            startPage()
            yPosition += 40f
        }

        drawSignatures(canvas!!, yPosition)

        drawFooter(canvas!!, pageNumber)
        pdfDocument.finishPage(currentPage)

        return pdfDocument
    }

    private fun drawDonutChart(canvas: Canvas, x: Float, y: Float, size: Float, fujian: Int, byd: Int, total: Int) {
        val rect = RectF(x, y, x + size, y + size)
        val paint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }

        if (total == 0) {
            paint.color = colorEmpty
            canvas.drawArc(rect, 0f, 360f, true, paint)
        } else {
            // Угол Fujian
            val fujianAngle = (fujian.toFloat() / total) * 360f
            // Угол BYD (остаток)
            val bydAngle = 360f - fujianAngle

            // Рисуем Fujian
            paint.color = colorFujian
            canvas.drawArc(rect, -90f, fujianAngle, true, paint)

            // Рисуем BYD
            paint.color = colorByd
            canvas.drawArc(rect, -90f + fujianAngle, bydAngle, true, paint)
        }

        // Рисуем центр (чтобы получился бублик)
        val holeSize = size * 0.5f
        val holeOffset = (size - holeSize) / 2
        val holeRect = RectF(x + holeOffset, y + holeOffset, x + holeOffset + holeSize, y + holeOffset + holeSize)
        val holePaint = Paint().apply { color = colorZebra; style = Paint.Style.FILL; isAntiAlias = true } // Цвет фона дашборда
        canvas.drawOval(holeRect, holePaint)
    }

    private fun drawStatItem(canvas: Canvas, label: String, value: String, x: Float, y: Float, color: Int) {
        val paintVal = TextPaint(statValuePaint).apply { this.color = color }
        canvas.drawText(value, x, y, paintVal)
        canvas.drawText(label, x, y - 20f, statLabelPaint)
    }

    private fun drawTableHeader(canvas: Canvas, y: Float) {
        val bgPaint = Paint().apply { color = colorLightBg }
        canvas.drawRect(MARGIN, y - 18f, PAGE_WIDTH - MARGIN, y + 8f, bgPaint)

        val x = getColumnXCoords()
        canvas.drawText("Палет", x[0], y, tableHeaderPaint)
        canvas.drawText("Производитель", x[1], y, tableHeaderPaint)
        canvas.drawText("Кол-во", x[2], y, tableHeaderPaint)
        canvas.drawText("Дата", x[3], y, tableHeaderPaint)
        canvas.drawText("Ответственный", x[4], y, tableHeaderPaint)
    }

    private fun drawRow(canvas: Canvas, y: Float, c1: String, c2: String, c3: String, c4: String, c5: String) {
        val x = getColumnXCoords()
        canvas.drawText(c1, x[0], y, rowTextPaint)
        canvas.drawText(c2, x[1], y, manufacturerPaint)
        canvas.drawText(c3, x[2], y, rowTextPaint)
        canvas.drawText(c4, x[3], y, rowTextPaint)
        canvas.drawText(c5, x[4], y, rowTextPaint)
    }

    private fun drawSignatures(canvas: Canvas, y: Float) {
        val linePaint = Paint().apply { color = colorDarkText; strokeWidth = 1f }
        val textPaint = TextPaint().apply { color = colorGrayText; textSize = 10f }

        // Левая подпись (Сдал)
        val xLeft = MARGIN
        val width = 200f
        canvas.drawLine(xLeft, y, xLeft + width, y, linePaint)
        canvas.drawText("Сдал (Подпись ответственного)", xLeft, y + 15f, textPaint)

        // Правая подпись (Принял)
        val xRight = PAGE_WIDTH - MARGIN - width
        canvas.drawLine(xRight, y, xRight + width, y, linePaint)
        canvas.drawText("Принял (Подпись кладовщика)", xRight, y + 15f, textPaint)
    }

    private fun getColumnXCoords() = floatArrayOf(
        MARGIN + 10f, MARGIN + 80f, MARGIN + 200f, MARGIN + 300f, MARGIN + 400f
    )

    private fun drawFooter(canvas: Canvas, pageNumber: Int) {
        canvas.drawText("- $pageNumber -", PAGE_WIDTH / 2f, PAGE_HEIGHT - 20f, footerPaint)
    }

    private fun sharePdfFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Отправить отчет"))
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка при отправке", Toast.LENGTH_SHORT).show()
        }
    }
}