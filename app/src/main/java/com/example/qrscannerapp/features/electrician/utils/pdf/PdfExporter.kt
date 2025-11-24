package com.example.qrscannerapp.features.electrician.utils.pdf

import android.content.Context
import android.content.Intent
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
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.graphics.withTranslation
import com.example.qrscannerapp.features.electrician.domain.model.BatteryRepairLog // Проверенный импорт
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {
    fun launchCreatePdfIntent(
        createDocumentLauncher: ActivityResultLauncher<Intent>,
        electricianName: String
    ) {
        val currentDate = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault()).format(Date())
        val fileName = "Отчет_${electricianName.replace(" ", "_")}_$currentDate.pdf"

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf" // <-- ИСПРАВЛЕНО
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        createDocumentLauncher.launch(intent)
    }

    fun createPdf(
        context: Context,
        uri: Uri,
        historyData: Map<String, List<BatteryRepairLog>>,
        electricianName: String
    ) {
        val pageHeight = 1120
        val pageWidth = 792
        val pageMargin = 40f

        val pdfDocument = PdfDocument()

        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 20f
            this.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)) // <-- ИСПРАВЛЕНО
            textAlign = Paint.Align.CENTER
        }
        val headerPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        val tableHeaderPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 10f
            this.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)) // <-- ИСПРАВЛЕНО
        }
        val tableCellPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 10f
        }
        val footerPaint = TextPaint().apply {
            color = Color.GRAY
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }

        try {
            context.contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                val fileOutputStream = FileOutputStream(pfd.fileDescriptor)
                var pageNumber = 1
                var currentPage: PdfDocument.Page? = null
                var canvas: Canvas? = null
                var yPosition = 0f

                fun startNewPage() {
                    if (currentPage != null) {
                        pdfDocument.finishPage(currentPage)
                    }
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    currentPage = pdfDocument.startPage(pageInfo)
                    canvas = currentPage!!.canvas
                    yPosition = pageMargin
                    drawHeader(canvas!!, electricianName, titlePaint, headerPaint, pageMargin, pageWidth.toFloat())
                    yPosition += 80f
                    pageNumber++
                }

                startNewPage()

                historyData.entries.forEachIndexed { index, (date, logs) ->
                    val blockHeight = estimateBlockHeight(logs, tableCellPaint)
                    if (yPosition + blockHeight > pageHeight - pageMargin) {
                        drawFooter(canvas!!, pageNumber - 1, footerPaint, pageWidth.toFloat(), pageHeight.toFloat())
                        startNewPage()
                    }

                    yPosition = drawDateBlock(canvas!!, date, logs, tableHeaderPaint, tableCellPaint, yPosition, pageMargin)
                }

                drawFooter(canvas!!, pageNumber - 1, footerPaint, pageWidth.toFloat(), pageHeight.toFloat())
                pdfDocument.finishPage(currentPage)
                pdfDocument.writeTo(fileOutputStream)
            }
            Toast.makeText(context, "PDF отчет успешно сохранен!", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Ошибка при создании PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawHeader(canvas: Canvas, electricianName: String, titlePaint: Paint, headerPaint: Paint, margin: Float, width: Float) {
        val reportDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Отчет о ремонтах АКБ", width / 2f, margin + 15f, titlePaint)
        canvas.drawText("Исполнитель: $electricianName", margin, margin + 40f, headerPaint)
        canvas.drawText("Дата отчета: $reportDate", width - margin - headerPaint.measureText("Дата отчета: $reportDate"), margin + 40f, headerPaint)
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, footerPaint: Paint, width: Float, height: Float) {
        canvas.drawText("Стр. $pageNumber", width / 2f, height - 20f, footerPaint)
    }

    private fun drawDateBlock(canvas: Canvas, date: String, logs: List<BatteryRepairLog>, headerPaint: Paint, cellPaint: Paint, startY: Float, margin: Float): Float {
        var y = startY
        // ИСПРАВЛЕНИЕ: TextPaint, а не просто Paint
        val dateTextPaint = TextPaint(headerPaint).apply { this.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)); textSize = 14f }
        canvas.drawText(date, margin, y, dateTextPaint)
        y += 30f

        canvas.withTranslation(margin, y) {
            drawText("Время", 0f, 0f, headerPaint)
            drawText("Номер АКБ", 60f, 0f, headerPaint)
            drawText("Произв.", 220f, 0f, headerPaint)
            drawText("Выполненные работы", 300f, 0f, headerPaint)
        }
        y += 20f

        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        logs.forEach { log ->
            val repairsText = log.repairs.joinToString(", ")
            val textPaint = TextPaint(cellPaint)
            // --- ИСПРАВЛЕНИЕ ЗДЕСЬ: Ширина передается как Int ---
            val staticLayoutWidth = 450
            val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(repairsText, 0, repairsText.length, textPaint, staticLayoutWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                (StaticLayout(
                    repairsText,
                    textPaint,
                    staticLayoutWidth,
                    Layout.Alignment.ALIGN_NORMAL,
                    1.0f,
                    0.0f,
                    false
                ))
            }

            canvas.withTranslation(margin, y) {
                drawText(sdfTime.format(Date(log.timestamp)), 0f, 0f, cellPaint)
                drawText(log.batteryId, 60f, 0f, cellPaint)
                drawText(log.manufacturer, 220f, 0f, cellPaint)
                withTranslation(300f, -5f) {
                    staticLayout.draw(this)
                }
            }
            y += staticLayout.height + 10f
        }
        return y
    }

    private fun estimateBlockHeight(logs: List<BatteryRepairLog>, cellPaint: Paint): Float {
        var height = 60f
        val textPaint = TextPaint(cellPaint)
        logs.forEach { log ->
            val repairsText = log.repairs.joinToString(", ")
            // --- ИСПРАВЛЕНИЕ ЗДЕСЬ: Ширина передается как Int ---
            val staticLayoutWidth = 450
            val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(repairsText, 0, repairsText.length, textPaint, staticLayoutWidth).build()
            } else {
                @Suppress("DEPRECATION")
                (StaticLayout(repairsText, textPaint, staticLayoutWidth, null, 1.0f, 0.0f, false))
            }
            height += staticLayout.height + 10f
        }
        return height
    }
}