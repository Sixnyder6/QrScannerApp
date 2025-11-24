package com.example.qrscannerapp.features.electrician.utils.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withTranslation
import com.example.qrscannerapp.R
import com.example.qrscannerapp.features.electrician.domain.model.BatteryRepairLog
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private var currentPage: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var yPosition = 0f
    private var pageNumber = 1

    // Цвета (лучше вынести в ресурсы, но для утилиты сойдет и так)
    private val primaryColor = Color.parseColor("#6A5AE0") // Stardust Primary
    private val secondaryColor = Color.parseColor("#2a2a2e")
    private val lightGrayColor = Color.parseColor("#A0A0A5")
    private val zebraColor = Color.parseColor("#F5F5F5")

    // Кисти (Paints)
    private val titlePaint = TextPaint().apply {
        color = primaryColor; textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val subHeaderPaint = TextPaint().apply { color = secondaryColor; textSize = 10f }
    private val sectionTitlePaint = TextPaint().apply {
        color = primaryColor; textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isUnderlineText = true
    }
    private val normalTextPaint = TextPaint().apply { color = secondaryColor; textSize = 11f }
    private val boldTextPaint = TextPaint().apply {
        color = secondaryColor; textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val tableCellPaint = TextPaint().apply { color = secondaryColor; textSize = 9f }
    private val footerPaint = TextPaint().apply { color = lightGrayColor; textSize = 8f; textAlign = Paint.Align.CENTER }

    // Запуск интента для создания файла
    fun launchCreateReportIntent(launcher: ActivityResultLauncher<Intent>, adminName: String) {
        val currentDate = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault()).format(Date())
        val fileName = "Аналитика_${adminName.replace(" ", "_")}_$currentDate.pdf"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        launcher.launch(intent)
    }

    // Основная функция генерации
    fun createAnalyticsReport(
        context: Context,
        uri: Uri,
        logs: List<BatteryRepairLog>,
        startDate: Long,
        endDate: Long,
        adminName: String
    ) {
        val pdfDocument = PdfDocument()
        pageNumber = 1

        // Безопасная загрузка логотипа
        val logoBitmap = try {
            ContextCompat.getDrawable(context, R.drawable.ic_logo_report)?.toBitmap(50, 50)
        } catch (e: Exception) {
            null // Если логотип не найден или ошибка, просто не рисуем его
        }

        try {
            context.contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                val fileOutputStream = FileOutputStream(pfd.fileDescriptor)

                // 1. Подготовка данных (Аналитика)
                val totalRepairs = logs.size
                val repairsByEmployee = logs.groupBy { it.electricianName }
                // Считаем типы ремонтов (плоский список всех работ)
                val repairsByType = logs.flatMap { it.repairs }.groupingBy { it }.eachCount()
                // Считаем производителей
                val repairsByManufacturer = logs.groupingBy { it.manufacturer }.eachCount()

                // 2. Рисование страниц

                // Стр 1: Сводка (Дашборд)
                startNewPage(pdfDocument, logoBitmap)
                drawSummaryPage(adminName, startDate, endDate, totalRepairs, repairsByEmployee, repairsByType)

                // Стр 2+: Детализация по сотрудникам (если есть данные)
                if (repairsByEmployee.isNotEmpty()) {
                    // Если на первой странице мало места, начинаем новую, иначе продолжаем
                    if (yPosition > PAGE_HEIGHT - 200) startNewPage(pdfDocument, logoBitmap)
                    else {
                        yPosition += 30f
                        canvas?.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, footerPaint)
                        yPosition += 30f
                    }
                    drawEmployeeDetailsPage(pdfDocument, logoBitmap, repairsByEmployee)
                }

                drawFooter() // Футер последней страницы
                pdfDocument.finishPage(currentPage)
                pdfDocument.writeTo(fileOutputStream)
            }
            Toast.makeText(context, "Аналитический отчет сохранен!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun startNewPage(pdfDocument: PdfDocument, logo: Bitmap?) {
        // Если была предыдущая страница, закрываем её
        if (currentPage != null) {
            drawFooter()
            pdfDocument.finishPage(currentPage)
        }
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        currentPage = pdfDocument.startPage(pageInfo)
        canvas = currentPage!!.canvas
        yPosition = MARGIN

        logo?.let { canvas?.drawBitmap(it, MARGIN, MARGIN - 10f, null) }

        pageNumber++
    }

    private fun drawFooter() {
        canvas?.drawText("Стр. ${pageNumber}", PAGE_WIDTH / 2f, PAGE_HEIGHT - MARGIN / 2, footerPaint)
    }

    private fun drawHeader(adminName: String, startDate: Long, endDate: Long) {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val period = "Период: с ${sdf.format(Date(startDate))} по ${sdf.format(Date(endDate))}"
        val generatedBy = "Сформирован: ${sdf.format(Date())}, $adminName"

        yPosition = MARGIN + 25f
        canvas?.drawText("АНАЛИТИЧЕСКИЙ ОТЧЕТ О РЕМОНТАХ", PAGE_WIDTH / 2f, yPosition, titlePaint)
        yPosition += 35f

        canvas?.drawText(period, MARGIN, yPosition, subHeaderPaint)
        canvas?.drawText(generatedBy, PAGE_WIDTH - MARGIN, yPosition, subHeaderPaint.apply { textAlign = Paint.Align.RIGHT })
        subHeaderPaint.textAlign = Paint.Align.LEFT // Возвращаем выравнивание

        yPosition += 15f
        canvas?.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, Paint().apply { color = lightGrayColor; strokeWidth = 1f })
        yPosition += 30f
    }

    private fun drawSummaryPage(
        adminName: String,
        startDate: Long,
        endDate: Long,
        totalRepairs: Int,
        repairsByEmployee: Map<String, List<BatteryRepairLog>>,
        repairsByType: Map<String, Int>
    ) {
        drawHeader(adminName, startDate, endDate)

        // Блок: Ключевые показатели
        canvas?.drawText("КЛЮЧЕВЫЕ ПОКАЗАТЕЛИ", MARGIN, yPosition, sectionTitlePaint)
        yPosition += 25f
        drawKeyValueRow("Всего отремонтировано АКБ:", "$totalRepairs шт.")
        drawKeyValueRow("Активных сотрудников:", "${repairsByEmployee.keys.size} чел.")
        yPosition += 30f

        // Блок: Рейтинг
        canvas?.drawText("РЕЙТИНГ ПРОДУКТИВНОСТИ", MARGIN, yPosition, sectionTitlePaint)
        yPosition += 25f

        val sortedEmployees = repairsByEmployee.entries.sortedByDescending { it.value.size }
        sortedEmployees.forEachIndexed { index, (name, logs) ->
            if (totalRepairs > 0) {
                val percentage = (logs.size.toFloat() / totalRepairs * 100).toInt()
                val paintToUse = if (index == 0) boldTextPaint else normalTextPaint // Лидер жирным

                canvas?.drawText("${index + 1}. $name", MARGIN, yPosition, paintToUse)
                // Рисуем полоску прогресса
                val barWidth = 200f * (logs.size.toFloat() / sortedEmployees.first().value.size) // Нормализация по лидеру
                val barPaint = Paint().apply { color = if(index == 0) primaryColor else lightGrayColor }
                canvas?.drawRect(PAGE_WIDTH - MARGIN - 250f, yPosition - 8f, PAGE_WIDTH - MARGIN - 250f + barWidth, yPosition, barPaint)

                canvas?.drawText("${logs.size} шт. ($percentage%)", PAGE_WIDTH - MARGIN, yPosition, boldTextPaint.apply { textAlign = Paint.Align.RIGHT })
                boldTextPaint.textAlign = Paint.Align.LEFT
                yPosition += 20f
            }
        }
        yPosition += 30f

        // Блок: Неисправности
        canvas?.drawText("ТОП НЕИСПРАВНОСТЕЙ", MARGIN, yPosition, sectionTitlePaint)
        yPosition += 25f
        repairsByType.entries.sortedByDescending { it.value }.take(10).forEach { (type, count) ->
            canvas?.drawText("• $type", MARGIN, yPosition, normalTextPaint)
            canvas?.drawText("$count шт.", PAGE_WIDTH - MARGIN, yPosition, boldTextPaint.apply { textAlign = Paint.Align.RIGHT })
            boldTextPaint.textAlign = Paint.Align.LEFT
            yPosition += 18f
        }
    }

    private fun drawKeyValueRow(key: String, value: String) {
        canvas?.drawText(key, MARGIN, yPosition, normalTextPaint)
        canvas?.drawText(value, PAGE_WIDTH - MARGIN, yPosition, boldTextPaint.apply { textAlign = Paint.Align.RIGHT })
        boldTextPaint.textAlign = Paint.Align.LEFT
        yPosition += 20f
    }

    private fun drawEmployeeDetailsPage(
        pdfDocument: PdfDocument,
        logo: Bitmap?,
        repairsByEmployee: Map<String, List<BatteryRepairLog>>
    ) {
        canvas?.drawText("ДЕТАЛИЗАЦИЯ ПО СОТРУДНИКАМ", PAGE_WIDTH / 2f, yPosition, titlePaint)
        yPosition += 40f

        repairsByEmployee.entries.sortedByDescending { it.value.size }.forEach { (name, logs) ->
            // Группируем работы конкретного сотрудника
            val personalStats = logs.flatMap { it.repairs }.groupingBy { it }.eachCount()
            val blockHeight = 40f + personalStats.size * 20f

            // Проверка на конец страницы
            if (yPosition + blockHeight > PAGE_HEIGHT - MARGIN) {
                startNewPage(pdfDocument, logo)
                canvas?.drawText("ДЕТАЛИЗАЦИЯ (продолжение)", PAGE_WIDTH / 2f, yPosition, titlePaint)
                yPosition += 40f
            }

            // Заголовок сотрудника с фоном
            val bgPaint = Paint().apply { color = zebraColor }
            canvas?.drawRect(MARGIN, yPosition - 15f, PAGE_WIDTH - MARGIN, yPosition + 10f, bgPaint)
            canvas?.drawText("$name (Всего: ${logs.size})", MARGIN + 10f, yPosition, sectionTitlePaint.apply { textSize = 12f; isUnderlineText = false })
            yPosition += 25f

            // Список работ сотрудника
            personalStats.entries.sortedByDescending { it.value }.forEach { (type, count) ->
                canvas?.drawText("- $type", MARGIN + 20f, yPosition, normalTextPaint)
                canvas?.drawText("$count", PAGE_WIDTH - MARGIN - 20f, yPosition, boldTextPaint.apply { textAlign = Paint.Align.RIGHT })
                boldTextPaint.textAlign = Paint.Align.LEFT
                yPosition += 18f
            }
            yPosition += 20f
        }
    }

    // --- УТИЛИТЫ ---

    // Правильная реализация StaticLayout для разных версий Android
    private fun createStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
        }
    }
}