package com.example.qrscannerapp.features.inventory.data.export

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.qrscannerapp.StorageCell // <-- Предполагаем, что StorageCell пока остается в корневом пакете
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StorageExportManager(private val context: Context) {

    fun exportAllCellsToExcel(cells: List<StorageCell>) {
        if (cells.isEmpty()) {
            Toast.makeText(context, "Нет ячеек для экспорта.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Хранение самокатов")

            // Создаем стили для заголовков
            val headerStyle = workbook.createCellStyle().apply {
                val font = workbook.createFont().apply {
                    // *** ИСПРАВЛЕНИЕ: Правильное свойство - 'bold', а не 'isBold' ***
                    this.bold = true
                }
                this.setFont(font)
            }

            // Определяем максимальное количество строк, которое нам понадобится
            val maxRows = cells.maxOfOrNull { it.items.size } ?: 0

            // Проходим по каждой ячейке, чтобы создать свой столбец
            cells.forEachIndexed { colIndex, cell ->
                // Устанавливаем ширину столбца
                sheet.setColumnWidth(colIndex, 20 * 256)

                // Строка 0: Имя ячейки (Ячейка 1, Ячейка 2...)
                val headerRow = sheet.getRow(0) ?: sheet.createRow(0)
                headerRow.createCell(colIndex).apply {
                    setCellValue(cell.name)
                    cellStyle = headerStyle
                }

                // Строка 1: Описание ячейки
                val descriptionRow = sheet.getRow(1) ?: sheet.createRow(1)
                descriptionRow.createCell(colIndex).setCellValue(cell.description)

                // Заполняем номера самокатов, начиная со 2-й строки
                cell.items.forEachIndexed { itemIndex, scooterId ->
                    val itemRowIndex = itemIndex + 2
                    val itemRow = sheet.getRow(itemRowIndex) ?: sheet.createRow(itemRowIndex)
                    itemRow.createCell(colIndex).setCellValue(scooterId)
                }
            }

            // Генерируем имя файла и сохраняем
            val sdf = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault())
            val timestamp = sdf.format(Date())
            val fileName = "storage_export_$timestamp.xlsx"

            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { workbook.write(it) }
            workbook.close()

            // Создаем Intent для шаринга файла
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // <-- ИСПРАВЛЕНО
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Экспорт склада"))

        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}